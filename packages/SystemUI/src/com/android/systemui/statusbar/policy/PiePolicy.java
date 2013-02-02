/*
 * Copyright (C) 2013 ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.os.BatteryManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.text.format.DateFormat;
import android.net.ConnectivityManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.provider.Telephony;

import com.android.systemui.R;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PiePolicy {

    public static int LOW_BATTERY_LEVEL;
    public static int CRITICAL_BATTERY_LEVEL;

    private static Context mContext;
    private static int mBatteryLevel = 0;
    private static boolean mTelephony;
    private static boolean mCharging;
    private static int dBm = 0;
    private static int ASU = 0;
    private static int mRssi;
    private static int mPhoneState;
    private static String mDataString;
    private OnClockChangedListener mClockChangedListener;
    private static final int INET_CONDITION_THRESHOLD = 50;
    private static String mMemInfo;
    private static double totalMemory;
    private static double availableMemory;
    private ActivityManager activityManager;

    private static final int MIN_RSSI = -100;
    private static final int MAX_RSSI = -55;

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            mCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        }
    };

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                mRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI,MIN_RSSI); 
                updateWifiSignal();
            }
        }
    };

    private final BroadcastReceiver mSignalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SIGNAL_DBM_CHANGED)) {
                dBm = intent.getIntExtra("dbm", 0);
                mPhoneState = intent.getIntExtra("signal_status", StatusBarPolicy.PHONE_SIGNAL_IS_NORMAL);
                getMemInfo();
            }
        }
    };

    private final BroadcastReceiver mDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                     action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
                updateConnectivity(intent);
            }
        }
    };

    private final BroadcastReceiver mClockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                if (mClockChangedListener != null) {
                    mClockChangedListener.onChange(getSimpleTime());
                }
            }
        }
    };

    public interface OnClockChangedListener {
        public void onChange(String s);
    }

    public PiePolicy(Context context) {
        mContext = context;
        mContext.registerReceiver(mSignalReceiver, 
                new IntentFilter(Intent.ACTION_SIGNAL_DBM_CHANGED));
        mContext.registerReceiver(mBatteryReceiver, 
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        mContext.registerReceiver(mClockReceiver, filter);
        IntentFilter filters = new IntentFilter();
        filters.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filters.addAction(ConnectivityManager.INET_CONDITION_ACTION);
        mContext.registerReceiver(mDataReceiver, filters);
        mContext.registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
        LOW_BATTERY_LEVEL = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryCloseWarningLevel);
        CRITICAL_BATTERY_LEVEL = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
        mTelephony = mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        ((TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE))
                .listen(mPhoneStateListener,
                          PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
	activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        totalMemory = 0;
        availableMemory = 0;
        getMemInfo();
    }

    public void setOnClockChangedListener(OnClockChangedListener l){
        mClockChangedListener = l;
    }
    
    public boolean supportsTelephony() {
        return mTelephony;
    }

    public static String getWifiSsid() {
        String ssid = mContext.getString(R.string.quick_settings_wifi_not_connected);
        ConnectivityManager connManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) mContext
                    .getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            ssid = huntForSsid(wifiManager, connectionInfo) + "(" + mRssi + " dBm)";
        } else if (connManager.getMobileDataEnabled() && !networkInfo.isConnected()) {
            ssid = mContext.getString(R.string.widget_mobile_data_title);
        }
        return ssid.toUpperCase();
    }

    private void updateWifiSignal() {
        float max = Math.abs(MAX_RSSI);
        float min = Math.abs(MIN_RSSI);
        float signal = 0f;
        signal = min - Math.abs(mRssi);
        signal = ((signal / (min - max)) * 100f);
        mRssi = (signal > 100f ? 100 : Math.round(signal));
    }

    public static String getSignalText() {
        String result = getSignalLevelString(dBm) + " dBm";
        if (mPhoneState == StatusBarPolicy.PHONE_SIGNAL_IS_AIRPLANE_MODE) {
             return "Airplane Mode";
        }
        return result.toUpperCase();
    }

    private static String huntForSsid(WifiManager wifiManager, WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        // OK, it's not in the connectionInfo; we have to go hunting for it
        List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration net : networks) {
            if (net.networkId == info.getNetworkId()) {
                return net.SSID;
            }
        }
        return null;
    }

    private static String getSignalLevelString(int dBm) {
        if (mPhoneState == StatusBarPolicy.PHONE_SIGNAL_IS_NULL || dBm == 0) {
            return "-\u221e"; // -oo ('minus infinity')
        }
        return Integer.toString(dBm);
    }

    /*
     * Phone listener to update signal information
     */
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (signalStrength != null) {
                ASU = signalStrength.getGsmSignalStrength();
                dBm = -113 + (2 * ASU);
            } else {
                // When signal strenth is null, let's set the values below to zero,
                // this showns then -oo in the status bar display
                ASU = 0;
                dBm = 0;
            }
            getMemInfo();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            updateDataNetType(networkType);
            getMemInfo();
        }

    };

    private void updateConnectivity(Intent intent) {
        NetworkInfo info = (NetworkInfo)(intent.getParcelableExtra(
                ConnectivityManager.EXTRA_NETWORK_INFO));
        int connectionStatus = intent.getIntExtra(ConnectivityManager.EXTRA_INET_CONDITION, 0);

        int inetCondition = (connectionStatus > INET_CONDITION_THRESHOLD ? 1 : 0);

        if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
            updateDataNetType(info.getSubtype());
        }
    }

    public static String getDataType() {
        return mDataString.toUpperCase();
    }

    private final void updateDataNetType(int net) {
        boolean mShowFourG = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_FOURG, 0) == 1);
        boolean mHspaDataDistinguishable;
        try {
            mHspaDataDistinguishable = mContext.getResources().getBoolean(
                    R.bool.config_hspa_data_distinguishable);
        } catch (Exception e) {
            mHspaDataDistinguishable = false;
        }

        switch (net) {
        case TelephonyManager.NETWORK_TYPE_EDGE:
            mDataString = "2G";
            break;
        case TelephonyManager.NETWORK_TYPE_UMTS:
            if (mShowFourG) {
                mDataString = "4G";
            } else {
                mDataString = "3G";
            }
            break;
        case TelephonyManager.NETWORK_TYPE_HSDPA:
        case TelephonyManager.NETWORK_TYPE_HSUPA:
        case TelephonyManager.NETWORK_TYPE_HSPA:
            if (mHspaDataDistinguishable) {
                if (mShowFourG) {
                    mDataString = "4G";
                } else {
                    mDataString = "3G";
                }
            } else {
                if (mShowFourG) {
                    mDataString = "4G";
                } else {
                    mDataString = "3G";
                }
            }
            break;
        case TelephonyManager.NETWORK_TYPE_CDMA:
            // display 1xRTT for IS95A/B
            mDataString = "CDMA";
            break;
        case TelephonyManager.NETWORK_TYPE_1xRTT:
            mDataString = "CDMA 1x";
            break;
        case TelephonyManager.NETWORK_TYPE_EVDO_0: //fall through
        case TelephonyManager.NETWORK_TYPE_EVDO_A:
        case TelephonyManager.NETWORK_TYPE_EVDO_B:
        case TelephonyManager.NETWORK_TYPE_EHRPD:
            if (mShowFourG) {
                mDataString = "CDMA 4G";
            } else {
                mDataString = "CDMA 3G";
            }
            break;
        case TelephonyManager.NETWORK_TYPE_LTE:
        case TelephonyManager.NETWORK_TYPE_HSPAP:
            mDataString = "4G";
            break;
        default:
            mDataString = "GPRS";
        break;
        }
    }

    public static String getMemoryInfo() {
        return mMemInfo.toUpperCase();
    }

    private void getMemInfo() {

	if (totalMemory == 0) {
	   try {
	      RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
	      String load = reader.readLine();
	      String[] memInfo = load.split(" ");
	      totalMemory = Double.parseDouble(memInfo[9])/1024;
           } catch (IOException ex) {
	      ex.printStackTrace();
	   }
	}

	MemoryInfo mi = new MemoryInfo();
	activityManager.getMemoryInfo(mi);
	availableMemory = mi.availMem / 1048576L;
	mMemInfo = "Free: "+(int)(availableMemory)+" MB | Total: "+(int)totalMemory+" MB";
    }

    public static String getNetworkProvider() {
        String operatorName = mContext.getString(R.string.quick_settings_wifi_no_network);
        TelephonyManager telephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        operatorName = telephonyManager.getNetworkOperatorName();
        if(operatorName == null) {
            operatorName = telephonyManager.getSimOperatorName();
        }
        return operatorName.toUpperCase();
    }

    public static String getSimpleDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                mContext.getString(R.string.pie_date_format));
        String date = sdf.format(new Date());
        return date.toUpperCase();
    }

    public static boolean is24Hours() {
        return DateFormat.is24HourFormat(mContext);
    }

    public static String getSimpleTime() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                mContext.getString(is24Hours() ? R.string.pie_hour_format_24 :
                R.string.pie_hour_format_12));
        String amPm = sdf.format(new Date());
        return amPm.toUpperCase();
    }

    public static String getAmPm() {
        String amPm = "";
        if(!is24Hours()) {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    mContext.getString(R.string.pie_am_pm));
            amPm = sdf.format(new Date()).toUpperCase();
        }
        return amPm;
    }

    public static int getBatteryLevel() {
        return mBatteryLevel;
    }

    public static String getBatteryLevelReadable() {
        if (mBatteryLevel == 100) {
            return mContext.getString(R.string.battery_fulls_percent_format, mBatteryLevel)
                .toUpperCase();
        }
        return mCharging ? mContext.getString(R.string.battery_charging_format, mBatteryLevel)
                .toUpperCase() : mContext.getString(R.string.battery_low_percent_format, mBatteryLevel)
                .toUpperCase();
    }
}
