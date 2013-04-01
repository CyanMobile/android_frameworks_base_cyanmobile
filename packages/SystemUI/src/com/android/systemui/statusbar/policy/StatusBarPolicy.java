/*
 * Copyright (C) 2008 The Android Open Source Project
 * Patched by Sven Dawitz; Copyright (C) 2011 CyanogenMod Project
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

import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHid;
import android.bluetooth.BluetoothPbap;
import android.bluetooth.BluetoothPan;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.telephony.SmsMessage;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Intents;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Slog;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.statusbar.cmcustom.SmsHelper;
import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.telephony.cdma.TtyIntent;
import com.android.server.am.BatteryStatsService;
import com.android.systemui.R;
import android.net.wimax.WimaxManagerConstants;

/**
 * This class contains all of the policy about which icons are installed in the status
 * bar at boot time.  It goes through the normal API for icons, even though it probably
 * strictly doesn't need to.
 */
public class StatusBarPolicy {
    private static final String TAG = "StatusBarPolicy";

    // message codes for the handler
    private static final int EVENT_BATTERY_CLOSE = 4;

    private static final int AM_PM_STYLE_NORMAL  = 0;
    private static final int AM_PM_STYLE_SMALL   = 1;
    private static final int AM_PM_STYLE_GONE    = 2;

    private static int AM_PM_STYLE = AM_PM_STYLE_GONE;

    private static final int INET_CONDITION_THRESHOLD = 50;

    private final Context mContext;
    private final StatusBarManager mService;
    private final Handler mHandler = new StatusBarHandler();
    private final IBatteryStats mBatteryStats;

    private int mStatusBarColor;
    private int mNotificationBackgroundColor;

    // headset
    private boolean mHeadsetPlugged = false;

    // storage
    private StorageManager mStorageManager;

    // battery
    private boolean mBatteryFirst = true;
    private boolean mBatteryPlugged;
    private int mBatteryLevel;
    private AlertDialog mLowBatteryDialog;
    private AlertDialog mFullBatteryDialog;
    private AlertDialog mSmsDialog;
    private TextView mBatteryLevelTextView;
    private View mBatteryView;
    private int mBatteryViewSequence;
    
    private View mSmsView;
    private ImageView mContactPicture;
    private TextView mContactName;
    private TextView mSmsBody;
    private TextView mTimeStamp;
    private Button mCallsButton;
    private String callNumber;
    private String callerName;
    private String inboxMessage;
    private Bitmap contactImage;
    private String inboxDate;
    private int smsCount;
    private long messageId;

    private boolean mBatteryShowLowOnEndCall = false;
    private boolean mBatteryShowFullOnEndCall = false;
    private static final boolean SHOW_LOW_BATTERY_WARNING = true;
    private static final boolean SHOW_FULL_BATTERY_WARNING = true;
    private static final boolean SHOW_BATTERY_WARNINGS_IN_CALL = true;
    public static final String SMS_CHANGED = "android.provider.Telephony.SMS_RECEIVED";

    //alarm
    private boolean mAlarmSet = false;

    // phone
    private TelephonyManager mPhone;
    private int mPhoneSignalIconId;
    public static final int PHONE_SIGNAL_IS_AIRPLANE_MODE = 1;
    public static final int PHONE_SIGNAL_IS_NULL = 2;
    public static final int PHONE_SIGNAL_IS_NORMAL = 0;

    //***** Data connection icons
    private int[] mDataIconList = TelephonyIcons.sDataNetType_g[0];

    // Assume it's all good unless we hear otherwise.  We don't always seem
    // to get broadcasts that it *is* there.
    private IccCard.State mSimState = IccCard.State.READY;
    private int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
    private int mDataState = TelephonyManager.DATA_DISCONNECTED;
    private int mDataActivity = TelephonyManager.DATA_ACTIVITY_NONE;
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;

    // flag for signal strength behavior
    private boolean mAlwaysUseCdmaRssi;

    // data connection
    private boolean mDataIconVisible;
    private boolean mHspaDataDistinguishable;

    // ringer volume
    private boolean mVolumeVisible;

    // bluetooth device status
    private int mBluetoothHeadsetState;
    private boolean mBluetoothA2dpConnected;
    private int mBluetoothHidState;
    private int mBluetoothPbapState;
    private boolean mBluetoothPanConnected;
    private boolean mBluetoothEnabled;

    private static final int sWifiTemporarilyNotConnectedImage =
            R.drawable.stat_sys_wifi_signal_0;

    private int mLastWifiSignalLevel = -1;
    private boolean mIsWifiConnected = false;

    private static final int sWimaxDisconnectedImg =
            R.drawable.stat_sys_data_wimax_signal_disconnected;
    private static final int sWimaxIdleImg = R.drawable.stat_sys_data_wimax_signal_idle;
    private boolean mIsWimaxEnabled = false;
    private int mWimaxSignal = 0;
    private int mWimaxState = 0;
    private int mWimaxExtraState = 0;

    // state of inet connection - 0 not connected, 100 connected
    private int mInetCondition = 0;

    // sync state
    // If sync is active the SyncActive icon is displayed. If sync is not active but
    // sync is failing the SyncFailing icon is displayed. Otherwise neither are displayed.
    private boolean SyncIsActive = false;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                updateBattery(intent);
            }
            else if (action.equals(Intent.ACTION_ALARM_CHANGED)) {
                updateAlarm(intent);
            }
            else if (action.equals(Intent.ACTION_SYNC_STATE_CHANGED)) {
                updateSyncState(intent);
            }
            else if (action.equals(Intent.ACTION_BATTERY_LOW)) {
                onBatteryLow(intent);
            }
            else if (action.equals(Intent.ACTION_BATTERY_FULL)) {
                onBatteryFull(intent);
            }
            else if (action.equals(Intent.ACTION_BATTERY_OKAY)
                    || action.equals(Intent.ACTION_POWER_CONNECTED)) {
                onBatteryOkay(intent);
            }
            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) ||
                    action.equals(BluetoothHeadset.ACTION_STATE_CHANGED) ||
                    action.equals(BluetoothHid.HID_DEVICE_STATE_CHANGED_ACTION) ||
                    action.equals(BluetoothA2dp.ACTION_SINK_STATE_CHANGED) ||
                    action.equals(BluetoothPan.INTERFACE_ADDED) ||
                    action.equals(BluetoothPan.ALL_DISCONNECTED) ||
                    action.equals(BluetoothHid.HID_DEVICE_STATE_CHANGED_ACTION) ||
                    action.equals(BluetoothPbap.PBAP_STATE_CHANGED_ACTION)) {
                updateBluetooth(intent);
            }
            else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) ||
                    action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION) ||
                    action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                updateWifi(intent);
            }
            else if (action.equals(LocationManager.GPS_ENABLED_CHANGE_ACTION) ||
                    action.equals(LocationManager.GPS_FIX_CHANGE_ACTION)) {
                updateGps(intent);
            }
            else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION) ||
                    action.equals(AudioManager.VIBRATE_SETTING_CHANGED_ACTION)) {
                updateVolume();
            }
            else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                updateHeadset(intent);
            }
            else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                updateSimState(intent);
            }
            else if (action.equals(SMS_CHANGED)) {
                onSmsDialog(intent);
            }
            else if (action.equals(TtyIntent.TTY_ENABLED_CHANGE_ACTION)) {
                updateTTY(intent);
            }
            else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                     action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
                // TODO - stop using other means to get wifi/mobile info
                updateConnectivity(intent);
            }
            else if (action.equals(WimaxManagerConstants.WIMAX_ENABLED_STATUS_CHANGED) ||
                     action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION) ||
                     action.equals(WimaxManagerConstants.WIMAX_STATE_CHANGED_ACTION) ||
                     action.equals(WimaxManagerConstants.NETWORK_STATE_CHANGED_ACTION) ||
                     action.equals(WimaxManagerConstants.WIMAX_ENABLED_CHANGED_ACTION) ||
                     action.equals(WimaxManagerConstants.RSSI_CHANGED_ACTION)) {
                updateWiMAX(intent);
            }
        }
    };

    private boolean mStatusBarBattery;

    // need another var that superceding mPhoneSignalHidden
    private boolean mShowCmSignal;

    private boolean mShowHeadset;

    private boolean mShowFourG;

    private boolean mShowAlarmIcon;

    private boolean mShowWifiIcon;

    private boolean mShowWifiText;

    private boolean mShowBluetoothIcon;

    private boolean mShow3gIcon;

    private boolean mShowGPSIcon;

    private boolean GPSenabled;

    private boolean mShowSyncIcon;

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_HEADSET), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_ALARM), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_WIFI), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_CM_WIFI_TEXT), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_BLUETOOTH), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_3G), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_GPS), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_SYNC), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_FOURG), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_COLOR), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.NOTIFICATION_BACKGROUND_COLOR), false, this);
            onChange(true);
        }

        @Override 
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private class SettingsSignalObserver extends ContentObserver {
        SettingsSignalObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_CM_SIGNAL_TEXT), false, this);
            onChange(true);
        }

        @Override 
        public void onChange(boolean selfChange) {
            updateSignalSettings();
        }
    }

    // phone_signal visibility
    private boolean mPhoneSignalHidden;
    public StatusBarPolicy(Context context) {
        mContext = context;
        mService = (StatusBarManager)context.getSystemService(Context.STATUS_BAR_SERVICE);
        mSignalStrength = new SignalStrength();
        mBatteryStats = BatteryStatsService.getService();

        // settings observer for cm-battery change
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();

        SettingsSignalObserver settingsSignalObserver = new SettingsSignalObserver(mHandler);
        settingsSignalObserver.observe();

        updateSettings();

        updateSignalSettings();

        // storage
        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        mStorageManager.registerListener(
                new com.android.systemui.usb.StorageNotification(context));

        // battery
        mService.setIcon("battery", com.android.internal.R.drawable.stat_sys_battery_unknown, 0);

        // phone_signal
        mPhone = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneSignalIconId = R.drawable.stat_sys_signal_null;
        mService.setIcon("phone_signal", mPhoneSignalIconId, 0);
        mAlwaysUseCdmaRssi = mContext.getResources().getBoolean(
            com.android.internal.R.bool.config_alwaysUseCdmaRssi);

        try { 
            mPhoneSignalHidden = mContext.getResources().getBoolean( 
                R.bool.config_statusbar_hide_phone_signal); 
        } catch (Exception e) { 
            mPhoneSignalHidden = false; 
        }

        // hide phone_signal icon if hidden
        mService.setIconVisibility("phone_signal", !mPhoneSignalHidden && !mShowCmSignal);

        // register for phone state notifications.
        ((TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE))
                .listen(mPhoneStateListener,
                          PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY);

        // data_connection
        mService.setIcon("data_connection", R.drawable.stat_sys_data_connected_g, 0);
        mService.setIconVisibility("data_connection", false);

        // wifi
        mService.setIcon("wifi", WifiIcons.sWifiSignalImages[0][0], 0);
        mService.setIconVisibility("wifi", false);
        // wifi will get updated by the sticky intents

        // wimax
        //enable/disable wimax depending on the value in config.xml
        boolean isWimaxEnabled = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if (isWimaxEnabled) {
            mService.setIcon("wimax", sWimaxDisconnectedImg, 0);
            mService.setIconVisibility("wimax", false);
        }

        // TTY status
        mService.setIcon("tty",  R.drawable.stat_sys_tty_mode, 0);
        mService.setIconVisibility("tty", false);

        // Cdma Roaming Indicator, ERI
        mService.setIcon("cdma_eri", R.drawable.stat_sys_roaming_cdma_0, 0);
        mService.setIconVisibility("cdma_eri", false);

        // bluetooth status
        mService.setIcon("bluetooth", R.drawable.stat_sys_data_bluetooth, 0);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            mBluetoothEnabled = adapter.isEnabled();
        } else {
            mBluetoothEnabled = false;
        }
        mBluetoothA2dpConnected = false;
        mBluetoothHeadsetState = BluetoothHeadset.STATE_DISCONNECTED;
        mBluetoothPbapState = BluetoothPbap.STATE_DISCONNECTED;
        mBluetoothPanConnected = false;
        mService.setIconVisibility("bluetooth", (mBluetoothEnabled && mShowBluetoothIcon));

        // Gps status
        mService.setIcon("gps", R.drawable.stat_sys_gps_acquiring_anim, 0);
        mService.setIconVisibility("gps", false);

        // Alarm clock
        mService.setIcon("alarm_clock", R.drawable.stat_notify_alarm, 0);
        mService.setIconVisibility("alarm_clock", false);

        // Sync state
        mService.setIcon("sync_active", com.android.internal.R.drawable.stat_notify_sync_anim0, 0);
        mService.setIcon("sync_failing", com.android.internal.R.drawable.stat_notify_sync_error, 0);
        mService.setIconVisibility("sync_active", false);
        mService.setIconVisibility("sync_failing", false);

        // volume
        mService.setIcon("volume", R.drawable.stat_sys_ringer_silent, 0);
        mService.setIconVisibility("volume", false);
        updateVolume();

        // headset
        mService.setIcon("headset", com.android.internal.R.drawable.stat_sys_headset, 0);
        mService.setIconVisibility("headset", false);

        IntentFilter filter = new IntentFilter();

        // Register for Intent broadcasts for...
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_FULL);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_ALARM_CHANGED);
        filter.addAction(Intent.ACTION_SYNC_STATE_CHANGED);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filter.addAction(AudioManager.VIBRATE_SETTING_CHANGED_ACTION);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothHid.HID_DEVICE_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothA2dp.ACTION_SINK_STATE_CHANGED);
        filter.addAction(BluetoothPbap.PBAP_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothPan.INTERFACE_ADDED);
        filter.addAction(BluetoothPan.ALL_DISCONNECTED);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(LocationManager.GPS_ENABLED_CHANGE_ACTION);
        filter.addAction(LocationManager.GPS_FIX_CHANGE_ACTION);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(TtyIntent.TTY_ENABLED_CHANGE_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(ConnectivityManager.INET_CONDITION_ACTION);
        filter.addAction(WimaxManagerConstants.WIMAX_STATE_CHANGED_ACTION);
        filter.addAction(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION);
        filter.addAction(WimaxManagerConstants.WIMAX_ENABLED_STATUS_CHANGED);
        filter.addAction(WimaxManagerConstants.WIMAX_ENABLED_CHANGED_ACTION);
        filter.addAction(WimaxManagerConstants.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WimaxManagerConstants.RSSI_CHANGED_ACTION);
        filter.addAction(SMS_CHANGED);
        mContext.registerReceiver(mIntentReceiver, filter, null, mHandler);

        // load config to determine if to distinguish Hspa data icon
        try {
            mHspaDataDistinguishable = mContext.getResources().getBoolean(
                    R.bool.config_hspa_data_distinguishable);
        } catch (Exception e) {
            mHspaDataDistinguishable = false;
        }
    }

    private final void updateAlarm(Intent intent) {
        mAlarmSet = intent.getBooleanExtra("alarmSet", false);
        mShowAlarmIcon = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_ALARM, 1) == 1);
        mService.setIconVisibility("alarm_clock", mAlarmSet && mShowAlarmIcon);
    }

    private final void updateSyncState(Intent intent) {
        SyncIsActive = intent.getBooleanExtra("active", false);
        boolean isFailing = intent.getBooleanExtra("failing", false);
        mShowSyncIcon = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_SYNC, 1) == 1);
        mService.setIconVisibility("sync_active", SyncIsActive && mShowSyncIcon);
        // Don't display sync failing icon: BUG 1297963 Set sync error timeout to "never"
        //mService.setIconVisibility("sync_failing", isFailing && !isActive);
    }

    private final void updateBattery(Intent intent) {
        final int id = intent.getIntExtra("icon-small", 0);
        int level = intent.getIntExtra("level", 0);
        mService.setIcon("battery", id, level);
        mService.setIconVisibility("battery", mStatusBarBattery);

        boolean plugged = intent.getIntExtra("plugged", 0) != 0;
        level = intent.getIntExtra("level", -1);
        if (false) {
            Slog.d(TAG, "updateBattery level=" + level
                    + " plugged=" + plugged
                    + " mBatteryPlugged=" + mBatteryPlugged
                    + " mBatteryLevel=" + mBatteryLevel
                    + " mBatteryFirst=" + mBatteryFirst);
        }

        boolean oldPlugged = mBatteryPlugged;

        mBatteryPlugged = plugged;
        mBatteryLevel = level;

        if (mBatteryFirst) {
            mBatteryFirst = false;
        }
        /*
         * No longer showing the battery view because it draws attention away
         * from the USB storage notification. We could still show it when
         * connected to a brick, but that could lead to the user into thinking
         * the device does not charge when plugged into USB (since he/she would
         * not see the same battery screen on USB as he sees on brick).
         */
        if (false) {
            Slog.d(TAG, "plugged=" + plugged + " oldPlugged=" + oldPlugged + " level=" + level);
        }
    }

    private void onBatteryLow(Intent intent) {
        if (SHOW_LOW_BATTERY_WARNING) {
            if (false) {
                Slog.d(TAG, "mPhoneState=" + mPhoneState
                      + " mLowBatteryDialog=" + mLowBatteryDialog
                      + " mBatteryShowLowOnEndCall=" + mBatteryShowLowOnEndCall);
            }

            if (SHOW_BATTERY_WARNINGS_IN_CALL || mPhoneState == TelephonyManager.CALL_STATE_IDLE) {
                      final ContentResolver cr = mContext.getContentResolver();
                      if (Settings.System.getInt(cr,
                            Settings.System.POWER_BATTERYLOW_ENABLED, 1) == 1)
                      {
                         showLowBatteryWarning();
                      }
            } else {
                mBatteryShowLowOnEndCall = true;
            }
        }
    }

    private void onSmsDialog(Intent intent) {
        if (mPhoneState != TelephonyManager.CALL_STATE_IDLE) return;

	if (intent != null) {
            String smsBody;
            SmsMessage[] message = Intents.getMessagesFromIntent(intent);
            SmsMessage sms = message[0];
            int pduCount = message.length;
            if (pduCount == 1) {
               smsBody = sms.getDisplayMessageBody();
            } else {
	       StringBuilder body = new StringBuilder(); // sms content
               for (int i = 0; i < pduCount; i++) {
                   sms = message[i];
                   body.append(sms.getDisplayMessageBody());
               }
               smsBody = body.toString();
            }
            if ((Settings.System.getInt(mContext.getContentResolver(), Settings.System.USE_POPUP_SMS, 1) == 1)
                       && mPhoneState == TelephonyManager.CALL_STATE_IDLE) {
               setSmsInfo(mContext, sms.getDisplayOriginatingAddress(), smsBody, System.currentTimeMillis());
            }
        }
    }

    private void onBatteryFull(Intent intent) {
        if (SHOW_FULL_BATTERY_WARNING) {
            if (false) {
                Slog.d(TAG, "mPhoneState=" + mPhoneState
                      + " mFullBatteryDialog=" + mFullBatteryDialog
                      + " mBatteryShowFullOnEndCall=" + mBatteryShowFullOnEndCall);
            }

            if (SHOW_BATTERY_WARNINGS_IN_CALL || mPhoneState == TelephonyManager.CALL_STATE_IDLE) {
                      final ContentResolver cr = mContext.getContentResolver();
                      if (Settings.System.getInt(cr,
                            Settings.System.POWER_BATTERYFULL_ENABLED, 1) == 1)
                      {
                         showFullBatteryWarning();
                      }
            } else {
                mBatteryShowFullOnEndCall = true;
            }
        }
    }

    private void onBatteryOkay(Intent intent) {
        if (mLowBatteryDialog != null
                && SHOW_LOW_BATTERY_WARNING) {
            mLowBatteryDialog.dismiss();
            mBatteryShowLowOnEndCall = false;
        } else if (mFullBatteryDialog != null
                && SHOW_FULL_BATTERY_WARNING) {
            mFullBatteryDialog.dismiss();
            mBatteryShowFullOnEndCall = false;
        }
    }

    private void setBatteryLevel(View parent, int id, int height, int background, int level) {
        ImageView v = (ImageView)parent.findViewById(id);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)v.getLayoutParams();
        lp.weight = height;
        if (background != 0) {
            v.setBackgroundResource(background);
            Drawable bkg = v.getBackground();
            bkg.setLevel(level);
        }
    }

    private void showLowBatteryWarning() {
        closeLastBatteryView();

        // Show exact battery level.
        CharSequence levelText = mContext.getString(
                    R.string.battery_low_percent_format, mBatteryLevel);

        if (mBatteryLevelTextView != null) {
            mBatteryLevelTextView.setText(levelText);
        } else {
            View v = View.inflate(mContext, R.layout.battery_low, null);
            mBatteryLevelTextView=(TextView)v.findViewById(R.id.level_percent);

            mBatteryLevelTextView.setText(levelText);

            AlertDialog.Builder b = new AlertDialog.Builder(mContext);
                b.setCancelable(true);
                b.setTitle(R.string.battery_low_title);
                b.setView(v);
                b.setIcon(android.R.drawable.ic_dialog_alert);
                b.setPositiveButton(android.R.string.ok, null);

                final Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_HISTORY);
                if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                    b.setNegativeButton(R.string.battery_low_why,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mContext.startActivity(intent);
                            if (mLowBatteryDialog != null) {
                                mLowBatteryDialog.dismiss();
                            }
                        }
                    });
                }

            AlertDialog d = b.create();
            d.setOnDismissListener(mLowBatteryListener);
            d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            d.show();
            mLowBatteryDialog = d;
        }

        final ContentResolver cr = mContext.getContentResolver();
        if (Settings.System.getInt(cr,
                Settings.System.POWER_SOUNDS_ENABLED, 1) == 1)
        {
            final String soundPath = Settings.System.getString(cr,
                Settings.System.LOW_BATTERY_SOUND);
            if (soundPath != null) {
                final Uri soundUri = Uri.parse("file://" + soundPath);
                if (soundUri != null) {
                    final Ringtone sfx = RingtoneManager.getRingtone(mContext, soundUri);
                    if (sfx != null) {
                        sfx.setStreamType(AudioManager.STREAM_SYSTEM);
                        sfx.play();
                    }
                }
            }
        }
    }

    private void showFullBatteryWarning() {
        closeLastBatteryView();

        // Show exact battery level.
        CharSequence levelText = mContext.getString(
                    R.string.battery_full_percent_format, mBatteryLevel);

        if (mBatteryLevelTextView != null) {
            mBatteryLevelTextView.setText(levelText);
        } else {
            View v = View.inflate(mContext, R.layout.battery_full, null);
            mBatteryLevelTextView=(TextView)v.findViewById(R.id.level_percent);

            mBatteryLevelTextView.setText(levelText);
            AlertDialog.Builder b = new AlertDialog.Builder(mContext);
                b.setCancelable(true);
                b.setTitle(R.string.battery_full_title);
                b.setView(v);
                b.setIcon(android.R.drawable.ic_dialog_alert);
                b.setPositiveButton(android.R.string.ok, null);

                final Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_HISTORY);
                if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                    b.setNegativeButton(R.string.battery_low_why,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mContext.startActivity(intent);
                            if (mFullBatteryDialog != null) {
                                mFullBatteryDialog.dismiss();
                            }
                        }
                    });
                }

            AlertDialog d = b.create();
            d.setOnDismissListener(mLowBatteryListener);
            d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            d.show();
            mFullBatteryDialog = d;
        }

        final ContentResolver cr = mContext.getContentResolver();
        if (Settings.System.getInt(cr,
                Settings.System.POWER_SOUNDS_ENABLED, 1) == 1)
        {
            final String soundPath = Settings.System.getString(cr,
                Settings.System.FULL_BATTERY_SOUND);
            if (soundPath != null) {
                final Uri soundUri = Uri.parse("file://" + soundPath);
                if (soundUri != null) {
                    final Ringtone sfx = RingtoneManager.getRingtone(mContext, soundUri);
                    if (sfx != null) {
                        sfx.setStreamType(AudioManager.STREAM_SYSTEM);
                        sfx.play();
                    }
                }
            }
        }
    }

    private void setSmsInfo(Context context, String smsNumber, String smsBody, long dateTaken) {
        if (mSmsDialog != null) mSmsDialog.dismiss();

        smsCount = SmsHelper.getUnreadSmsCount(context);
        callNumber = smsNumber;
        callerName = SmsHelper.getName(context, callNumber);
        inboxMessage = smsBody;
        inboxDate = SmsHelper.getDate(context, dateTaken);
        messageId = SmsHelper.getSmsId(context);
        contactImage = SmsHelper.getContactPicture(
                context, callNumber);
        showSmsInfo();
    }

    private void resetSmsInfo() {
        smsCount = 0;
        callNumber = null;
        callerName = null;
        inboxMessage = null;
        inboxDate = null;
        messageId = 0;
        contactImage = null;
    }

    private void showSmsInfo() {
        closeLastSmsView();

        View v = View.inflate(mContext, R.layout.smscall_widget, null);

        mContactPicture = (ImageView) v.findViewById(R.id.contactpicture);
        mContactName = (TextView) v.findViewById(R.id.contactname);
        mSmsBody = (TextView) v.findViewById(R.id.smsmessage);
        mTimeStamp = (TextView) v.findViewById(R.id.smstime);
        mCallsButton = (Button) v.findViewById(R.id.calls_button);
        mCallsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + callNumber));
               dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               mContext.startActivity(dialIntent);
               if (mSmsDialog != null) {
                   mSmsDialog.dismiss();
               }
            }
        });

        if (contactImage != null) {
            mContactPicture.setImageBitmap(contactImage);
        }
        mContactName.setText(callerName);
        mSmsBody.setText(SmsHelper.replaceWithEmotes(inboxMessage, mContext));
        mTimeStamp.setText(inboxDate);

            AlertDialog.Builder b = new AlertDialog.Builder(mContext);
                b.setCancelable(false);
                b.setView(v);
                b.setPositiveButton("QuickReply",
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                               Intent isms = new Intent(Intent.ACTION_MAIN);
                               isms.setClassName("com.android.mms",
                                          "com.android.mms.ui.QuickReplyBox");
                               isms.putExtra("avatars", contactImage);
                               isms.putExtra("numbers", callNumber);
                               isms.putExtra("names", callerName);
                               isms.putExtra("inmessage", inboxMessage);
                               isms.putExtra("indate", inboxDate);
                               isms.putExtra("id", messageId);
                               isms.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(isms);
                            if (mSmsDialog != null) {
                                mSmsDialog.dismiss();
                            }
                        }
                    });
                b.setNegativeButton("Inbox",
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                               Intent ioinbox = new Intent(Intent.ACTION_MAIN);  
                               ioinbox.addCategory(Intent.CATEGORY_DEFAULT);  
                               ioinbox.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
                               ioinbox.setType("vnd.android-dir/mms-sms");  
                            mContext.startActivity(ioinbox);
                            if (mSmsDialog != null) {
                                mSmsDialog.dismiss();
                            }
                        }
                    });
                b.setNeutralButton("Cancel",
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                             Intent ismsread = new Intent(Intent.ACTION_MAIN);
                             ismsread.setClassName("com.android.mms",
                                     "com.android.mms.ui.QuickReader");
                             ismsread.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                             mContext.startActivity(ismsread);
                             if (mSmsDialog != null) {
                                 mSmsDialog.dismiss();
                             }
                        }
                    });
            AlertDialog d = b.create();
            d.setOnDismissListener(mSmsListener);
            d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            d.show();
            mSmsDialog = d;
    }

    private final void updateCallState(int state) {
        mPhoneState = state;
        if (false) {
            Slog.d(TAG, "mPhoneState=" + mPhoneState
                    + " mLowBatteryDialog=" + mLowBatteryDialog
                    + " mBatteryShowLowOnEndCall=" + mBatteryShowLowOnEndCall
                    + " mFullBatteryDialog=" + mFullBatteryDialog
                    + " mBatteryShowFullOnEndCall=" + mBatteryShowFullOnEndCall);
        }
        if (mPhoneState == TelephonyManager.CALL_STATE_IDLE) {
            if (mBatteryShowLowOnEndCall) {
                if (!mBatteryPlugged) {
                      final ContentResolver cr = mContext.getContentResolver();
                      if (Settings.System.getInt(cr,
                            Settings.System.POWER_BATTERYLOW_ENABLED, 1) == 1)
                      {
                          showLowBatteryWarning();
                      }
                }
                mBatteryShowLowOnEndCall = false;
            } else if (mBatteryShowFullOnEndCall) {
                if (mBatteryPlugged) {
                      final ContentResolver cr = mContext.getContentResolver();
                      if (Settings.System.getInt(cr,
                            Settings.System.POWER_BATTERYFULL_ENABLED, 1) == 1)
                      {
                             showFullBatteryWarning();
                      }
                }
                mBatteryShowFullOnEndCall = false;
            }
        } else {
            if (mLowBatteryDialog != null) {
                mLowBatteryDialog.dismiss();
                mBatteryShowLowOnEndCall = true;
            } else if (mFullBatteryDialog != null) {
                mFullBatteryDialog.dismiss();
                mBatteryShowFullOnEndCall = true;
            }
        }
    }

    private DialogInterface.OnDismissListener mLowBatteryListener
            = new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            mLowBatteryDialog = null;
            mBatteryLevelTextView = null;
        }
    };

    private DialogInterface.OnDismissListener mSmsListener
            = new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            mSmsDialog = null;
            resetSmsInfo();
        }
    };

    private DialogInterface.OnDismissListener mFullBatteryListener
            = new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            mFullBatteryDialog = null;
            mBatteryLevelTextView = null;
        }
    };

    private void scheduleCloseBatteryView() {
        Message m = mHandler.obtainMessage(EVENT_BATTERY_CLOSE);
        m.arg1 = (++mBatteryViewSequence);
        mHandler.sendMessageDelayed(m, 3000);
    }

    private void closeLastBatteryView() {
        if (mBatteryView != null) {
            //mBatteryView.debug();
            WindowManagerImpl.getDefault().removeView(mBatteryView);
            mBatteryView = null;
        }
    }

    private void closeLastSmsView() {
        if (mSmsView != null) {
            WindowManagerImpl.getDefault().removeView(mSmsView);
            mSmsView = null;
        }
    }

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
            updateSignalStrength(); // apply any change in connectionStatus
            break;
        case ConnectivityManager.TYPE_WIFI:
            mInetCondition = inetCondition;
            mShowWifiIcon = (Settings.System.getInt(mContext.getContentResolver(),
                             Settings.System.STATUS_BAR_WIFI, 1) == 1);
            mShowWifiText = (Settings.System.getInt(mContext.getContentResolver(),
                             Settings.System.STATUS_BAR_CM_WIFI_TEXT, 0) == 1);
            if (info.isConnected()) {
                mIsWifiConnected = true;
                int iconId = WifiIcons.sWifiSignalImages[mInetCondition][0];
                if (mShowWifiIcon && !mShowWifiText) {
                  if (mLastWifiSignalLevel == -1) {
                      iconId = WifiIcons.sWifiSignalImages[mInetCondition][0];
                  } else {
                      iconId = WifiIcons.sWifiSignalImages[mInetCondition][mLastWifiSignalLevel];
                  }
                }
                mService.setIcon("wifi", iconId, 0);
                if (mShowWifiIcon && !mShowWifiText) {
                    // Show the icon since wi-fi is connected
                    mService.setIconVisibility("wifi", true);
                } else {
                    mService.setIconVisibility("wifi", false);
                }
            } else {
                mLastWifiSignalLevel = -1;
                mIsWifiConnected = false;
                int iconId = WifiIcons.sWifiSignalImages[0][0];

                mService.setIcon("wifi", iconId, 0);
                // Hide the icon since we're not connected
                mService.setIconVisibility("wifi", false);
            }
            updateSignalStrength(); // apply any change in mInetCondition
            break;
        case ConnectivityManager.TYPE_WIMAX:
            mInetCondition = inetCondition;
            updateWiMAX(intent);
            break;
        }
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mSignalStrength = signalStrength;
            updateSignalStrength();
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            mServiceState = state;
            updateSignalStrength();
            updateCdmaRoamingIcon(state);
            updateDataIcon();
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            updateCallState(state);
            // In cdma, if a voice call is made, RSSI should switch to 1x.
            if (isCdma()) {
                updateSignalStrength();
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
        updateDataIcon();
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

    private final void updateSignalStrength() {
        int iconLevel = -1;
        int[] iconList;

        // Display signal strength while in "emergency calls only" mode
        if (mServiceState == null || (!hasService() && !mServiceState.isEmergencyOnly())) {
            boolean isVisible;
            //Slog.d(TAG, "updateSignalStrength: no service");
            if (Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) == 1) {
                mPhoneSignalIconId = R.drawable.stat_sys_signal_flightmode;
                updateSignalStrengthDbm(PHONE_SIGNAL_IS_AIRPLANE_MODE);
                // show the icon depening on mPhoneSignalHidden (and regardless of
                // the value of CmShowCmSignal)
                isVisible = !mPhoneSignalHidden;
            } else {
                mPhoneSignalIconId = R.drawable.stat_sys_signal_null;
                updateSignalStrengthDbm(PHONE_SIGNAL_IS_NULL);
                // set phone_signal visibility false if hidden
                // and hide it if CmSignalText is used
                isVisible = !mPhoneSignalHidden && !mShowCmSignal;
            }
            mService.setIcon("phone_signal", mPhoneSignalIconId, 0);
            mService.setIconVisibility("phone_signal", isVisible);
            return;
        }

        // calculate and update the dBm value of the signal strength
        updateSignalStrengthDbm(PHONE_SIGNAL_IS_NORMAL);
        if (mShowCmSignal && !mPhoneSignalHidden) {
            // if we show the dBm value, hide the standard icon and quit this method.
            mService.setIconVisibility("phone_signal", false);
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
        mService.setIcon("phone_signal", mPhoneSignalIconId, 0);
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

    public void updateSignalStrengthDbm(int phoneSignalStatus) {
        int dBm = -1;

        if(!mSignalStrength.isGsm()) {
            dBm = mSignalStrength.getCdmaDbm();
        } else {
            int gsmSignalStrength = mSignalStrength.getGsmSignalStrength();
            int asu = (gsmSignalStrength == 99 ? -1 : gsmSignalStrength);
            if (asu != -1) {
                dBm = -113 + 2*asu;
            }
        }

        Intent dbmIntent = new Intent(Intent.ACTION_SIGNAL_DBM_CHANGED);
        dbmIntent.putExtra("dbm", dBm);
        dbmIntent.putExtra("signal_status", phoneSignalStatus);
        mContext.sendBroadcast(dbmIntent);
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
        int iconId;
        boolean visible = true;
        mShow3gIcon = (Settings.System.getInt(mContext.getContentResolver(),
                       Settings.System.STATUS_BAR_3G, 1) == 1);
        if (!isCdma()) {
            // GSM case, we have to check also the sim state
            if (mSimState == IccCard.State.READY || mSimState == IccCard.State.UNKNOWN) {
                if (hasService() && mDataState == TelephonyManager.DATA_CONNECTED) {
                    switch (mDataActivity) {
                        case TelephonyManager.DATA_ACTIVITY_IN:
                            iconId = mDataIconList[1];
                            break;
                        case TelephonyManager.DATA_ACTIVITY_OUT:
                            iconId = mDataIconList[2];
                            break;
                        case TelephonyManager.DATA_ACTIVITY_INOUT:
                            iconId = mDataIconList[3];
                            break;
                        default:
                            iconId = mDataIconList[0];
                            break;
                    }
                    mService.setIcon("data_connection", iconId, 0);
                } else {
                    visible = false;
                }
            } else {
                iconId = R.drawable.stat_sys_no_sim;
                mService.setIcon("data_connection", iconId, 0);
            }
        } else {
            // CDMA case, mDataActivity can be also DATA_ACTIVITY_DORMANT
            if (hasService() && mDataState == TelephonyManager.DATA_CONNECTED) {
                switch (mDataActivity) {
                    case TelephonyManager.DATA_ACTIVITY_IN:
                        iconId = mDataIconList[1];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_OUT:
                        iconId = mDataIconList[2];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_INOUT:
                        iconId = mDataIconList[3];
                        break;
                    case TelephonyManager.DATA_ACTIVITY_DORMANT:
                    default:
                        iconId = mDataIconList[0];
                        break;
                }
                mService.setIcon("data_connection", iconId, 0);
            } else {
                visible = false;
            }
        }

        long ident = Binder.clearCallingIdentity();
        try {
            mBatteryStats.notePhoneDataConnectionState(mPhone.getNetworkType(), visible);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }

        if (mShow3gIcon) {
            mService.setIconVisibility("data_connection", visible);
        } else {
            mService.setIconVisibility("data_connection", false);
        }
        mDataIconVisible = visible;
    }

    private final void updateVolume() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        final int ringerMode = audioManager.getRingerMode();
        final boolean visible = ringerMode == AudioManager.RINGER_MODE_SILENT ||
                ringerMode == AudioManager.RINGER_MODE_VIBRATE;
        final int iconId = audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_RINGER)
                ? R.drawable.stat_sys_ringer_vibrate
                : R.drawable.stat_sys_ringer_silent;

        if (visible) {
            mService.setIcon("volume", iconId, 0);
        }
        if (visible != mVolumeVisible) {
            mService.setIconVisibility("volume", visible);
            mVolumeVisible = visible;
        }
    }

    private final void updateHeadset(Intent intent) {
        mHeadsetPlugged = intent.getIntExtra("state", 0) == 1;

        if (mHeadsetPlugged) {
            final boolean hasMicrophone = intent.getIntExtra("microphone", 1) == 1;
            final int iconId = hasMicrophone
                    ? com.android.internal.R.drawable.stat_sys_headset
                    : R.drawable.stat_sys_headset_no_mic;
            mService.setIcon("headset", iconId, 0);
        }
        mService.setIconVisibility("headset", mShowHeadset && mHeadsetPlugged);
    }

    private final void updateBluetooth(Intent intent) {
        int iconId = R.drawable.stat_sys_data_bluetooth;
        String action = intent.getAction();
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            mBluetoothEnabled = state == BluetoothAdapter.STATE_ON;
        } else if (action.equals(BluetoothHeadset.ACTION_STATE_CHANGED)) {
            mBluetoothHeadsetState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                    BluetoothHeadset.STATE_ERROR);
        } else if (action.equals(BluetoothA2dp.ACTION_SINK_STATE_CHANGED)) {
            BluetoothA2dp a2dp = new BluetoothA2dp(mContext);
            if (a2dp.getConnectedSinks().size() != 0) {
                mBluetoothA2dpConnected = true;
            } else {
                mBluetoothA2dpConnected = false;
            }
        } else if (action.equals(BluetoothPbap.PBAP_STATE_CHANGED_ACTION)) {
            mBluetoothPbapState = intent.getIntExtra(BluetoothPbap.PBAP_STATE,
                    BluetoothPbap.STATE_DISCONNECTED);
        } else if (action.equals(BluetoothHid.HID_DEVICE_STATE_CHANGED_ACTION)) {
            mBluetoothHidState = intent.getIntExtra(BluetoothHid.HID_DEVICE_STATE,
                    BluetoothHid.STATE_DISCONNECTED);
        } else if (action.equals(BluetoothPan.INTERFACE_ADDED)) {
            mBluetoothPanConnected = true;
        } else if (action.equals(BluetoothPan.ALL_DISCONNECTED)) {
            mBluetoothPanConnected = false;
        } else {
            return;
        }

        if (mBluetoothHeadsetState == BluetoothHeadset.STATE_CONNECTED || mBluetoothA2dpConnected ||
                mBluetoothHidState == BluetoothHid.STATE_CONNECTED ||
                mBluetoothPbapState == BluetoothPbap.STATE_CONNECTED || mBluetoothPanConnected) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected;
        }

        mService.setIcon("bluetooth", iconId, 0);
        if (mShowBluetoothIcon) {
            mService.setIconVisibility("bluetooth", mBluetoothEnabled);
        } else {
            mService.setIconVisibility("bluetooth", false);
        }
    }

    private final void updateWifi(Intent intent) {

        final String action = intent.getAction();
        mShowWifiIcon = (Settings.System.getInt(mContext.getContentResolver(),
                         Settings.System.STATUS_BAR_WIFI, 1) == 1);
            mShowWifiText = (Settings.System.getInt(mContext.getContentResolver(),
                             Settings.System.STATUS_BAR_CM_WIFI_TEXT, 0) == 1);

        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {

            final boolean enabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;

            if (!enabled) {
                // If disabled, hide the icon. (We show icon when connected.)
                mService.setIconVisibility("wifi", false);
            }

        } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            final boolean enabled = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED,
                                                           false);
            if (!enabled) {
                mService.setIconVisibility("wifi", false);
            }
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
                    iconId = sWifiTemporarilyNotConnectedImage;
                }
                mService.setIcon("wifi", iconId, 0);
            }
        }
        if (!mShowWifiIcon && mShowWifiText) {
            mService.setIconVisibility("wifi", false);
        }
    }

    private final void updateWiMAX(Intent intent) {
        final String action = intent.getAction();
        int iconId = sWimaxDisconnectedImg;

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
            mService.setIconVisibility("wimax", mIsWimaxEnabled);
        } else if (action.equals(WimaxManagerConstants.WIMAX_ENABLED_CHANGED_ACTION)) {
            int wimaxStatus = intent.getIntExtra(WimaxManagerConstants.CURRENT_WIMAX_ENABLED_STATE,
                    WimaxManagerConstants.WIMAX_ENABLED_STATE_UNKNOWN);
            mIsWimaxEnabled = (wimaxStatus == WimaxManagerConstants.WIMAX_ENABLED_STATE_ENABLED);
            mService.setIconVisibility("wimax", mIsWimaxEnabled);
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
                iconId = sWimaxDisconnectedImg;
                break;
            case WimaxManagerConstants.WIMAX_STATE_CONNECTED:
                if(mWimaxExtraState == WimaxManagerConstants.WIMAX_IDLE) {
                    iconId = sWimaxIdleImg;
                } else {
                    iconId = WifiIcons.sWimaxSignalImages[mInetCondition][mWimaxSignal];
                }
                break;
        }
        if (mIsWimaxEnabled) mService.setIcon("wimax", iconId, 0);
    }

    private final void updateGps(Intent intent) {
        final String action = intent.getAction();
        GPSenabled = intent.getBooleanExtra(LocationManager.EXTRA_GPS_ENABLED, false);
        mShowGPSIcon = (Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.STATUS_BAR_GPS, 1) == 1);

        if (action.equals(LocationManager.GPS_FIX_CHANGE_ACTION) && GPSenabled && mShowGPSIcon) {
            // GPS is getting fixes
            mService.setIcon("gps", com.android.internal.R.drawable.stat_sys_gps_on, 0);
            mService.setIconVisibility("gps", true);
        } else if ((action.equals(LocationManager.GPS_ENABLED_CHANGE_ACTION) && !GPSenabled) || !mShowGPSIcon) {
            // GPS is off
            mService.setIconVisibility("gps", false);
        } else {
            // GPS is on, but not receiving fixes
            mService.setIcon("gps", R.drawable.stat_sys_gps_acquiring_anim, 0);
            mService.setIconVisibility("gps", true);
        }
    }

    private final void updateTTY(Intent intent) {
        final String action = intent.getAction();
        final boolean enabled = intent.getBooleanExtra(TtyIntent.TTY_ENABLED, false);

        if (false) Slog.v(TAG, "updateTTY: enabled: " + enabled);

        if (enabled) {
            // TTY is on
            if (false) Slog.v(TAG, "updateTTY: set TTY on");
            mService.setIcon("tty", R.drawable.stat_sys_tty_mode, 0);
            mService.setIconVisibility("tty", true);
        } else {
            // TTY is off
            if (false) Slog.v(TAG, "updateTTY: set TTY off");
            mService.setIconVisibility("tty", false);
        }
    }

    private final void updateCdmaRoamingIcon(ServiceState state) {
        if (!hasService()) {
            mService.setIconVisibility("cdma_eri", false);
            return;
        }

        if (!isCdma()) {
            mService.setIconVisibility("cdma_eri", false);
            return;
        }

        int[] iconList = TelephonyIcons.sRoamingIndicatorImages_cdma;
        int iconIndex = state.getCdmaEriIconIndex();
        int iconMode = state.getCdmaEriIconMode();

        if (iconIndex == -1) {
            Slog.e(TAG, "getCdmaEriIconIndex returned null, skipping ERI icon update");
            return;
        }

        if (iconMode == -1) {
            Slog.e(TAG, "getCdmeEriIconMode returned null, skipping ERI icon update");
            return;
        }

        if (iconIndex == EriInfo.ROAMING_INDICATOR_OFF) {
            if (false) Slog.v(TAG, "Cdma ROAMING_INDICATOR_OFF, removing ERI icon");
            mService.setIconVisibility("cdma_eri", false);
            return;
        }

        switch (iconMode) {
            case EriInfo.ROAMING_ICON_MODE_NORMAL:
                if (iconIndex >= iconList.length) {
                    Slog.e(TAG, "unknown iconIndex " + iconIndex + ", skipping ERI icon update");
                    return;
                }
                mService.setIcon("cdma_eri", iconList[iconIndex], 0);
                mService.setIconVisibility("cdma_eri", true);
                break;
            case EriInfo.ROAMING_ICON_MODE_FLASH:
                mService.setIcon("cdma_eri", R.drawable.stat_sys_roaming_cdma_flash, 0);
                mService.setIconVisibility("cdma_eri", true);
                break;

        }
        mService.setIcon("phone_signal", mPhoneSignalIconId, 0);
    }

    private class StatusBarHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_BATTERY_CLOSE:
                if (msg.arg1 == mBatteryViewSequence) {
                    closeLastBatteryView();
                }
                break;
            }
        }
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        // check for changes to status bar color and update accordingly
        int mSBColor = mStatusBarColor;

        mStatusBarColor = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_COLOR, 1));

        // check for changes to notification background color and update accordingly
        int mNBColor = mNotificationBackgroundColor;

        mNotificationBackgroundColor = (Settings.System.getInt(resolver,
                Settings.System.NOTIFICATION_BACKGROUND_COLOR, 1));

        mStatusBarBattery = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY, 0) == 0);
        mService.setIconVisibility("battery", mStatusBarBattery);

        mShowAlarmIcon = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_ALARM, 1) == 1);
        mService.setIconVisibility("alarm_clock", mAlarmSet && mShowAlarmIcon);

        mShowWifiText = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CM_WIFI_TEXT, 0) == 1);
        mShowWifiIcon = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_WIFI, 1) == 1);
        mService.setIconVisibility("wifi", mShowWifiIcon && mIsWifiConnected && !mShowWifiText);

        mShowBluetoothIcon = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BLUETOOTH, 1) == 1);
        mService.setIconVisibility("bluetooth", mBluetoothEnabled && mShowBluetoothIcon);

        mShow3gIcon = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_3G, 1) == 1);
        mService.setIconVisibility("data_connection", mShow3gIcon && mDataIconVisible);

        mShowGPSIcon = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_GPS, 1) == 1);
        mService.setIconVisibility("gps", mShowGPSIcon && GPSenabled);

        mShowSyncIcon = (Settings.System.getInt(resolver,
             Settings.System.STATUS_BAR_SYNC, 1) == 1);
        mService.setIconVisibility("sync_active", mShowSyncIcon && SyncIsActive);

        mShowHeadset = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_HEADSET, 1) == 1);
        mService.setIconVisibility("headset", mShowHeadset && mHeadsetPlugged);
    }

    private void updateSignalSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mPhoneSignalHidden = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CM_SIGNAL_TEXT, 0) != 4);

        // 0 will hide the cmsignaltext and show the signal bars
        mShowCmSignal = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CM_SIGNAL_TEXT, 0) != 0);
        mService.setIconVisibility("phone_signal", !mPhoneSignalHidden && !mShowCmSignal);
    }
}
