/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Slog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.telephony.cdma.TtyIntent;
import com.android.internal.telephony.TelephonyProperties;
import com.android.systemui.R;
import android.net.wimax.WimaxManagerConstants;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class NetworkController {
    // debug
    static final String TAG = "StatusBar.NetworkController";

    // telephony
    final TelephonyManager mPhone;
    private int mPhoneSignalIconId;
    public static final int PHONE_SIGNAL_IS_AIRPLANE_MODE = 1;
    public static final int PHONE_SIGNAL_IS_NULL = 2;
    public static final int PHONE_SIGNAL_IS_NORMAL = 0;

    // Assume it's all good unless we hear otherwise.  We don't always seem
    // to get broadcasts that it *is* there.
    IccCard.State mSimState = IccCard.State.READY;
    int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
    int mDataState = TelephonyManager.DATA_DISCONNECTED;
    int mDataActivity = TelephonyManager.DATA_ACTIVITY_NONE;
    ServiceState mServiceState;
    SignalStrength mSignalStrength;

    // flag for signal strength behavior
    boolean mAlwaysUseCdmaRssi;

    // data connection
    private int mDataSignalIconId;
    boolean mHspaDataDistinguishable;

    int mLastWifiSignalLevel = -1;
    boolean mIsWifiConnected = false;

    private boolean mIsWimaxEnabled = false;
    private int mWifiIconId;
    boolean mWifiEnabled;
    private int mWimaxSignal = 0;
    private int mWimaxState = 0;
    private int mWimaxExtraState = 0;
    private int mInetCondition = 0;
    private static final int INET_CONDITION_THRESHOLD = 50;

    private boolean mAirplaneMode = false;
    private boolean mLastAirplaneMode = true;

    private int[] mDataIconList = TelephonyIcons.sDataNetType_g[0];

    boolean mMobileDataEnable;
    private boolean mShowSpn;
    private String mSpn;
    private boolean mShowPlmn;
    private String mPlmn;
    private int mCarrierLabelType;
    private String mCarrierLabelCustom;
    String mNetworkName;
    String mWifiSsid;
    private boolean mShowFourG;
    private static final int TYPE_DEFAULT = 0;
    private static final int TYPE_SPN = 1;
    private static final int TYPE_PLMN = 2;
    private static final int TYPE_CUSTOM = 3;
    private boolean mWimaxSupported;
    private final Handler mHandler;

    // our ui
    Context mContext;
    ArrayList<NetworkSignalChangedCallback> mSignalsChangedCallbacks =
            new ArrayList<NetworkSignalChangedCallback>();

    public interface SignalCluster {
        void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon);
        void setMobileDataIndicators(boolean visible, int strengthIcon, int activityIcon,
                int typeIcon);
        void setIsAirplaneMode(boolean is, int airplaneIcon);
    }

    public interface NetworkSignalChangedCallback {
        void onWifiSignalChanged(boolean enabled, int wifiSignalIconId, String description);
        void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId, int dataTypeIconId, String description);
        void onAirplaneModeChanged(boolean enabled);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_FOURG), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.CARRIER_LABEL_TYPE),
                    false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.CARRIER_LABEL_CUSTOM_STRING),
                    false, this);
        }

        @Override 
        public void onChange(boolean selfChange) {
           ContentResolver resolver = mContext.getContentResolver();
           mShowFourG = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_FOURG, 0) == 1);
           mCarrierLabelType = Settings.System.getInt(resolver,
                Settings.System.CARRIER_LABEL_TYPE, TYPE_DEFAULT);
           mCarrierLabelCustom = Settings.System.getString(resolver,
                Settings.System.CARRIER_LABEL_CUSTOM_STRING);
           updateAirplaneMode();
        }
    }

    /**
     * Construct this controller object and register for updates.
     */
    public NetworkController(Context context) {
        mContext = context;

        mHandler = new Handler();
        mSignalStrength = new SignalStrength();

        // settings observer for cm-battery change
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();

        // telephony
        mPhone = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mPhone.listen(mPhoneStateListener,
                          PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY);

        mAlwaysUseCdmaRssi = mContext.getResources().getBoolean(
            com.android.internal.R.bool.config_alwaysUseCdmaRssi);

        // broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(Telephony.Intents.SPN_STRINGS_UPDATED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(ConnectivityManager.INET_CONDITION_ACTION);
        filter.addAction(WimaxManagerConstants.WIMAX_STATE_CHANGED_ACTION);
        filter.addAction(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION);
        filter.addAction(WimaxManagerConstants.WIMAX_ENABLED_STATUS_CHANGED);
        filter.addAction(WimaxManagerConstants.WIMAX_ENABLED_CHANGED_ACTION);
        filter.addAction(WimaxManagerConstants.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WimaxManagerConstants.RSSI_CHANGED_ACTION);
        mWimaxSupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);

        mContext.registerReceiver(mIntentReceiver, filter, null, mHandler);

        try {
            mHspaDataDistinguishable = mContext.getResources().getBoolean(
                    R.bool.config_hspa_data_distinguishable);
        } catch (Exception e) {
            mHspaDataDistinguishable = false;
        }

        // AIRPLANE_MODE_CHANGED is sent at boot; we've probably already missed it
        updateAirplaneMode();
        mMobileDataEnable = false;
    }

    public boolean isEmergencyOnly() {
        return (mServiceState != null && mServiceState.isEmergencyOnly());
    }

    public void addNetworkSignalChangedCallback(NetworkSignalChangedCallback cb) {
        mSignalsChangedCallbacks.add(cb);
        notifySignalsChangedCallbacks(cb);
    }

    void notifySignalsChangedCallbacks(NetworkSignalChangedCallback cb) {
        // only show wifi in the cluster if connected or if wifi-only
        String wifiDesc = mWifiEnabled ?
                mWifiSsid : null;
        cb.onWifiSignalChanged(mWifiEnabled, mWifiIconId, wifiDesc);

        if (isEmergencyOnly()) {
            cb.onMobileDataSignalChanged(false, mPhoneSignalIconId, mDataSignalIconId, null);
        } else {
            cb.onMobileDataSignalChanged(mWifiEnabled ? false : mMobileDataEnable, mPhoneSignalIconId, mDataSignalIconId, mNetworkName);
        }
        cb.onAirplaneModeChanged(mAirplaneMode);
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) ||
                    action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION) ||
                    action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            updateWifiState(intent);
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
            updateSimState(intent);
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                     action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
            updateConnectivity(intent);
        } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
            updateAirplaneMode();
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            updateAirplaneMode();
        } else if (action.equals(WimaxManagerConstants.WIMAX_ENABLED_STATUS_CHANGED) ||
                     action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION) ||
                     action.equals(WimaxManagerConstants.WIMAX_STATE_CHANGED_ACTION) ||
                     action.equals(WimaxManagerConstants.NETWORK_STATE_CHANGED_ACTION) ||
                     action.equals(WimaxManagerConstants.WIMAX_ENABLED_CHANGED_ACTION) ||
                     action.equals(WimaxManagerConstants.RSSI_CHANGED_ACTION)) {
            updateWiMAX(intent);
        } else if (Telephony.Intents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
                updateNetworkName(intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_SPN, false),
                        intent.getStringExtra(Telephony.Intents.EXTRA_SPN),
                        intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_PLMN, false),
                        intent.getStringExtra(Telephony.Intents.EXTRA_PLMN));
        }
       }
    };


    // ===== Telephony ==============================================================

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mSignalStrength = signalStrength;
            updateTelephonySignalStrength();
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            mServiceState = state;
            updateTelephonySignalStrength();
            updateDataIcon();
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            // In cdma, if a voice call is made, RSSI should switch to 1x.
            if (isCdma()) {
                updateTelephonySignalStrength();
            }
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            mDataState = state;
            updateDataNetType(networkType);
            updateDataIcon();
        }

        @Override
        public void onDataActivity(int direction) {
            mDataActivity = direction;
            updateDataIcon();
        }
    };

    private final void updateSimState(Intent intent) {
        String stateExtra = intent.getStringExtra(IccCard.INTENT_KEY_ICC_STATE);
        if (IccCard.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
            mSimState = IccCard.State.ABSENT;
        }
        else if (IccCard.INTENT_VALUE_ICC_READY.equals(stateExtra)) {
            mSimState = IccCard.State.READY;
        }
        else if (IccCard.INTENT_VALUE_ICC_LOCKED.equals(stateExtra)) {
            final String lockedReason = intent.getStringExtra(IccCard.INTENT_KEY_LOCKED_REASON);
            if (IccCard.INTENT_VALUE_LOCKED_ON_PIN.equals(lockedReason)) {
                mSimState = IccCard.State.PIN_REQUIRED;
            }
            else if (IccCard.INTENT_VALUE_LOCKED_ON_PUK.equals(lockedReason)) {
                mSimState = IccCard.State.PUK_REQUIRED;
            }
            else {
                mSimState = IccCard.State.NETWORK_LOCKED;
            }
        } else {
            mSimState = IccCard.State.UNKNOWN;
        }
    }

    private boolean isCdma() {
        return (mSignalStrength != null) && !mSignalStrength.isGsm();
    }

    private boolean isEvdo() {
        return ( (mServiceState != null)
                 && ((mServiceState.getRadioTechnology()
                        == ServiceState.RADIO_TECHNOLOGY_EVDO_0)
                     || (mServiceState.getRadioTechnology()
                        == ServiceState.RADIO_TECHNOLOGY_EVDO_A)
                     || (mServiceState.getRadioTechnology()
                        == ServiceState.RADIO_TECHNOLOGY_EVDO_B)));
    }

    private boolean hasService() {
        if (mServiceState != null) {
            switch (mServiceState.getState()) {
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_POWER_OFF:
                    return false;
                default:
                    return true;
            }
        } else {
            return false;
        }
    }

    private void updateAirplaneMode() {
        mAirplaneMode = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.AIRPLANE_MODE_ON, 0) == 1);
    }

    private final void updateTelephonySignalStrength() {
        int iconLevel = -1;
        int[] iconList;

        // Display signal strength while in "emergency calls only" mode
        if (mServiceState == null || (!hasService() && !mServiceState.isEmergencyOnly())) {
            //Slog.d(TAG, "updateSignalStrength: no service");
            if (mAirplaneMode) {
                mPhoneSignalIconId = R.drawable.stat_sys_signal_flightmode;
            } else {
                mPhoneSignalIconId = R.drawable.stat_sys_signal_null;
            }
            return;
        }

        if (!isCdma()) {
            int asu = mSignalStrength.getGsmSignalStrength();

            // ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
            // asu = 0 (-113dB or less) is very weak
            // signal, its better to show 0 bars to the user in such cases.
            // asu = 99 is a special case, where the signal strength is unknown.
            if (asu <= 2 || asu == 99) iconLevel = 0;
            else if (asu >= 12) iconLevel = 4;
            else if (asu >= 8)  iconLevel = 3;
            else if (asu >= 5)  iconLevel = 2;
            else iconLevel = 1;

            // Though mPhone is a Manager, this call is not an IPC
            if (mPhone.isNetworkRoaming()) {
                iconList = TelephonyIcons.sSignalImages_r[mInetCondition];
            } else {
                iconList = TelephonyIcons.sSignalImages[mInetCondition];
            }
        } else {
            iconList = TelephonyIcons.sSignalImages[mInetCondition];

            // If 3G(EV) and 1x network are available than 3G should be
            // displayed, displayed RSSI should be from the EV side.
            // If a voice call is made then RSSI should switch to 1x.

            // Samsung CDMA devices handle signal strength display differently
            // relying only on cdmaDbm - thanks Adr0it for the assistance here
            if (SystemProperties.get("ro.ril.samsung_cdma").equals("true")) {
                final int cdmaDbm = mSignalStrength.getCdmaDbm();
                if (cdmaDbm >= -75) iconLevel = 4;
                else if (cdmaDbm >= -85) iconLevel = 3;
                else if (cdmaDbm >= -95) iconLevel = 2;
                else if (cdmaDbm >= -100) iconLevel = 1;
                else iconLevel = 0;
            } else {
                if ((mPhoneState == TelephonyManager.CALL_STATE_IDLE) && isEvdo()
                    && !mAlwaysUseCdmaRssi) {
                    iconLevel = getEvdoLevel();
                    if (false) {
                        Slog.d(TAG, "use Evdo level=" + iconLevel + " to replace Cdma Level=" + getCdmaLevel());
                    }
                } else {
                    if ((mPhoneState == TelephonyManager.CALL_STATE_IDLE) && isEvdo()){
                        iconLevel = getEvdoLevel();
                        if (false) {
                            Slog.d(TAG, "use Evdo level=" + iconLevel + " to replace Cdma Level=" + getCdmaLevel());
                        }
                    } else {
                        iconLevel = getCdmaLevel();
                    }
                }
            }
        }
        mPhoneSignalIconId = iconList[iconLevel];
    }

    // ===== Full or limited Internet connectivity ==================================

    private void updateConnectivity(Intent intent) {
        NetworkInfo info = (NetworkInfo)(intent.getParcelableExtra(
                ConnectivityManager.EXTRA_NETWORK_INFO));
        int connectionStatus = intent.getIntExtra(ConnectivityManager.EXTRA_INET_CONDITION, 0);

        int inetCondition = (connectionStatus > INET_CONDITION_THRESHOLD ? 1 : 0);

        switch (info.getType()) {
        case ConnectivityManager.TYPE_MOBILE:
            mInetCondition = inetCondition;
            updateDataNetType(info.getSubtype());
            updateDataIcon();
            updateTelephonySignalStrength(); // apply any change in connectionStatus
            break;
        case ConnectivityManager.TYPE_WIFI:
            mInetCondition = inetCondition;
            if (info.isConnected()) {
                mIsWifiConnected = true;
                int iconId = WifiIcons.sWifiSignalImages[mInetCondition][0];
                if (mLastWifiSignalLevel == -1) {
                    iconId = WifiIcons.sWifiSignalImages[mInetCondition][0];
                } else {
                    iconId = WifiIcons.sWifiSignalImages[mInetCondition][mLastWifiSignalLevel];
                }
                mWifiIconId = iconId;
            } else {
                mLastWifiSignalLevel = -1;
                mIsWifiConnected = false;
                mWifiIconId = WifiIcons.sWifiSignalImages[0][0];
            }
            updateTelephonySignalStrength(); // apply any change in mInetCondition
            break;
        case ConnectivityManager.TYPE_WIMAX:
            mInetCondition = inetCondition;
            updateWiMAX(intent);
            break;
        }
    }

    private int getCdmaLevel() {
        final int cdmaDbm = mSignalStrength.getCdmaDbm();
        final int cdmaEcio = mSignalStrength.getCdmaEcio();
        int levelDbm = 0;
        int levelEcio = 0;

        if (cdmaDbm >= -75) levelDbm = 4;
        else if (cdmaDbm >= -85) levelDbm = 3;
        else if (cdmaDbm >= -95) levelDbm = 2;
        else if (cdmaDbm >= -100) levelDbm = 1;
        else levelDbm = 0;

        // Ec/Io are in dB*10
        if (cdmaEcio >= -90) levelEcio = 4;
        else if (cdmaEcio >= -110) levelEcio = 3;
        else if (cdmaEcio >= -130) levelEcio = 2;
        else if (cdmaEcio >= -150) levelEcio = 1;
        else levelEcio = 0;

        return (levelDbm < levelEcio) ? levelDbm : levelEcio;
    }

    private int getEvdoLevel() {
        int evdoDbm = mSignalStrength.getEvdoDbm();
        int evdoSnr = mSignalStrength.getEvdoSnr();
        int levelEvdoDbm = 0;
        int levelEvdoSnr = 0;

        if (evdoDbm >= -65) levelEvdoDbm = 4;
        else if (evdoDbm >= -75) levelEvdoDbm = 3;
        else if (evdoDbm >= -90) levelEvdoDbm = 2;
        else if (evdoDbm >= -105) levelEvdoDbm = 1;
        else levelEvdoDbm = 0;

        if (evdoSnr >= 7) levelEvdoSnr = 4;
        else if (evdoSnr >= 5) levelEvdoSnr = 3;
        else if (evdoSnr >= 3) levelEvdoSnr = 2;
        else if (evdoSnr >= 1) levelEvdoSnr = 1;
        else levelEvdoSnr = 0;

        return (levelEvdoDbm < levelEvdoSnr) ? levelEvdoDbm : levelEvdoSnr;
    }

    private final void updateDataNetType(int net) {
        ContentResolver resolver = mContext.getContentResolver();

        mShowFourG = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_FOURG, 0) == 1);

        switch (net) {
        case TelephonyManager.NETWORK_TYPE_EDGE:
            mDataIconList = TelephonyIcons.sDataNetType_e[mInetCondition];
            break;
        case TelephonyManager.NETWORK_TYPE_UMTS:
            if (mShowFourG) {
                mDataIconList = TelephonyIcons.sDataNetType_4g[mInetCondition];
            } else {
                mDataIconList = TelephonyIcons.sDataNetType_3g[mInetCondition];
            }
            break;
        case TelephonyManager.NETWORK_TYPE_HSDPA:
        case TelephonyManager.NETWORK_TYPE_HSUPA:
        case TelephonyManager.NETWORK_TYPE_HSPA:
            if (mHspaDataDistinguishable) {
                if (mShowFourG) {
                    mDataIconList = TelephonyIcons.sDataNetType_4g[mInetCondition];
                } else {
                    mDataIconList = TelephonyIcons.sDataNetType_h[mInetCondition];
                }
            } else {
                if (mShowFourG) {
                    mDataIconList = TelephonyIcons.sDataNetType_4g[mInetCondition];
                } else {
                    mDataIconList = TelephonyIcons.sDataNetType_3g[mInetCondition];
                }
            }
            break;
        case TelephonyManager.NETWORK_TYPE_CDMA:
            // display 1xRTT for IS95A/B
            mDataIconList = TelephonyIcons.sDataNetType_1x[mInetCondition];
            break;
        case TelephonyManager.NETWORK_TYPE_1xRTT:
            mDataIconList = TelephonyIcons.sDataNetType_1x[mInetCondition];
            break;
        case TelephonyManager.NETWORK_TYPE_EVDO_0: //fall through
        case TelephonyManager.NETWORK_TYPE_EVDO_A:
        case TelephonyManager.NETWORK_TYPE_EVDO_B:
        case TelephonyManager.NETWORK_TYPE_EHRPD:
            if (mShowFourG) {
                mDataIconList = TelephonyIcons.sDataNetType_4g[mInetCondition];
            } else {
                mDataIconList = TelephonyIcons.sDataNetType_3g[mInetCondition];
            }
            break;
        case TelephonyManager.NETWORK_TYPE_LTE:
        case TelephonyManager.NETWORK_TYPE_HSPAP:
            mDataIconList = TelephonyIcons.sDataNetType_4g[mInetCondition];
            break;
        default:
            mDataIconList = TelephonyIcons.sDataNetType_g[mInetCondition];
        break;
        }
    }

    private final void updateDataIcon() {
        if (!isCdma()) {
            // GSM case, we have to check also the sim state
            if (mSimState == IccCard.State.READY || mSimState == IccCard.State.UNKNOWN) {
                if (hasService() && mDataState == TelephonyManager.DATA_CONNECTED) {
                    switch (mDataActivity) {
                        case TelephonyManager.DATA_ACTIVITY_IN:
                            mDataSignalIconId = mDataIconList[1];
                            break;
                        case TelephonyManager.DATA_ACTIVITY_OUT:
                            mDataSignalIconId = mDataIconList[2];
                            break;
                        case TelephonyManager.DATA_ACTIVITY_INOUT:
                            mDataSignalIconId = mDataIconList[3];
                            break;
                        default:
                            mDataSignalIconId = mDataIconList[0];
                            break;
                    }
                    mMobileDataEnable = true;
                }
            } else {
                mDataSignalIconId = R.drawable.stat_sys_no_sim;
                mMobileDataEnable = false;
            }
        } else {
            // CDMA case, mDataActivity can be also DATA_ACTIVITY_DORMANT
            if (hasService() && mDataState == TelephonyManager.DATA_CONNECTED) {
                switch (mDataActivity) {
                    case TelephonyManager.DATA_ACTIVITY_IN:
                        mDataSignalIconId = mDataIconList[1];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_OUT:
                        mDataSignalIconId = mDataIconList[2];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_INOUT:
                        mDataSignalIconId = mDataIconList[3];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_DORMANT:
                    default:
                        mDataSignalIconId = mDataIconList[0];
                        break;
                }
                mMobileDataEnable = true;
            }
        }
    }

    void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
        if (false) {
            Slog.d("CarrierLabel", "updateNetworkName showSpn=" + showSpn + " spn=" + spn
                    + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }

        mShowSpn = showSpn;
        mSpn = spn;
        mShowPlmn = showPlmn;
        mPlmn = plmn;

        boolean haveSignal = (showPlmn && plmn != null) || (showSpn && spn != null);
        if (!haveSignal) {
            if (mAirplaneMode) {
                mNetworkName = "Airplane Mode";
                return;
            } else {
                mNetworkName = mContext.getString(com.android.internal.R.string.lockscreen_carrier_default);
                return;
            }
        }

        String realPlmn = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ALPHA);
        int carrierLabelType = mCarrierLabelType;

        if (plmn != null && !(plmn.equals(realPlmn))) {
            carrierLabelType = TYPE_DEFAULT;
        }

        switch (carrierLabelType) {
            default:
            case TYPE_DEFAULT:
                StringBuilder str = new StringBuilder();
                if (showPlmn) {
                    if (plmn != null) {
                        str.append(plmn);
                    } else {
                        str.append(mContext.getText(com.android.internal.R.string.lockscreen_carrier_default));
                    }
                }
                if (showSpn && spn != null) {
                    if (showPlmn) {
                        str.append('\n');
                    }
                    str.append(spn);
                }
                mNetworkName = str.toString();
                break;

            case TYPE_SPN:
                mNetworkName = spn;
                break;

            case TYPE_PLMN:
                mNetworkName = plmn;
                break;

            case TYPE_CUSTOM:
                mNetworkName = mCarrierLabelCustom;
                break;
        }
    }


    // ===== Wifi ===================================================================

    private void updateWifiState(Intent intent) {

        final String action = intent.getAction();
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            mWifiEnabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;
        } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            mWifiEnabled = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED,
                                                           false);
        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            int iconId;
            final int newRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
            int newSignalLevel = WifiManager.calculateSignalLevel(newRssi,
                                                                  WifiIcons.sWifiSignalImages[0].length);
            if (newSignalLevel != mLastWifiSignalLevel) {
                mLastWifiSignalLevel = newSignalLevel;
                if (mIsWifiConnected) {
                    iconId = WifiIcons.sWifiSignalImages[mInetCondition][newSignalLevel];
                } else {
                    iconId = WifiIcons.sWifiTemporarilyNotConnectedImage;
                }
                mWifiIconId = iconId;
            }
        }
    }

    private String huntForSsid(WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        return null;
    }


    // ===== Wimax ===================================================================
    private final void updateWiMAX(Intent intent) {
        final String action = intent.getAction();
        int iconId = WimaxIcons.sWimaxDisconnectedImg;

        if (action.equals(WimaxManagerConstants.WIMAX_ENABLED_STATUS_CHANGED)) {
            int wimaxStatus = intent.getIntExtra(WimaxManagerConstants.EXTRA_WIMAX_STATUS,
                    WimaxManagerConstants.WIMAX_STATUS_DISABLED);
            switch(wimaxStatus) {
                case WimaxManagerConstants.WIMAX_STATUS_ENABLED:
                    mIsWimaxEnabled = true;
                    break;
                case WimaxManagerConstants.WIMAX_STATUS_DISABLED:
                    mIsWimaxEnabled = false;
                    break;
            }
            //mService.setIconVisibility("wimax", mIsWimaxEnabled);
        } else if (action.equals(WimaxManagerConstants.WIMAX_ENABLED_CHANGED_ACTION)) {
            int wimaxStatus = intent.getIntExtra(WimaxManagerConstants.CURRENT_WIMAX_ENABLED_STATE,
                    WimaxManagerConstants.WIMAX_ENABLED_STATE_UNKNOWN);
            mIsWimaxEnabled = (wimaxStatus == WimaxManagerConstants.WIMAX_ENABLED_STATE_ENABLED);
            //mService.setIconVisibility("wimax", mIsWimaxEnabled);
        } else if (action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION)) {
            mWimaxSignal = intent.getIntExtra(WimaxManagerConstants.EXTRA_NEW_SIGNAL_LEVEL, 0);
        } else if (action.equals(WimaxManagerConstants.RSSI_CHANGED_ACTION)) {
            int rssi = intent.getIntExtra(WimaxManagerConstants.EXTRA_NEW_RSSI_LEVEL, -200);
            Slog.d(TAG, "WiMAX RSSI: " + rssi);
            if (rssi >= 3) {
                mWimaxSignal = 3;
            } else if (rssi <= 0) {
                mWimaxSignal = 0;
            } else {
                mWimaxSignal = rssi;
            }
        } else if (action.equals(WimaxManagerConstants.WIMAX_STATE_CHANGED_ACTION)) {
            mWimaxState = intent.getIntExtra(WimaxManagerConstants.EXTRA_WIMAX_STATE,
                    WimaxManagerConstants.WIMAX_STATE_UNKNOWN);
            mWimaxExtraState = intent.getIntExtra(
                    WimaxManagerConstants.EXTRA_WIMAX_STATE_DETAIL,
                    WimaxManagerConstants.WIMAX_DEREGISTRATION);
        } else if (action.equals(WimaxManagerConstants.NETWORK_STATE_CHANGED_ACTION)) {
            final NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WimaxManagerConstants.EXTRA_NETWORK_INFO);
            if (networkInfo != null && networkInfo.isConnected()) {
                mWimaxState = WimaxManagerConstants.WIMAX_STATE_CONNECTED;
                mWimaxExtraState = WimaxManagerConstants.WIMAX_STATE_UNKNOWN;
            } else if (networkInfo != null && networkInfo.isAvailable()) {
                mWimaxState = WimaxManagerConstants.WIMAX_STATE_CONNECTED;
                mWimaxExtraState = WimaxManagerConstants.WIMAX_IDLE;
            } else {
                mWimaxState = WimaxManagerConstants.WIMAX_STATE_DISCONNECTED;
                mWimaxExtraState = WimaxManagerConstants.WIMAX_STATE_UNKNOWN;
            }
        }
        switch(mWimaxState) {
            case WimaxManagerConstants.WIMAX_STATE_DISCONNECTED:
                iconId = WimaxIcons.sWimaxDisconnectedImg;
                break;
            case WimaxManagerConstants.WIMAX_STATE_CONNECTED:
                if(mWimaxExtraState == WimaxManagerConstants.WIMAX_IDLE) {
                    iconId = WimaxIcons.sWimaxIdleImg;
                } else {
                    iconId = WimaxIcons.sWimaxSignalImages[mInetCondition][mWimaxSignal];
                }
                break;
        }
        //if (mIsWimaxEnabled) mService.setIcon("wimax", iconId, 0);
    }
}
