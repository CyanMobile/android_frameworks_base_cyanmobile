package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.CmStatusBarView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;

public class WifiAPTile extends QuickSettingsTile {

    private static WifiManager mWifiManager;

    public WifiAPTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        updateTileState();
        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int state = mWifiManager.getWifiApState();
                switch (state) {
                    case WifiManager.WIFI_AP_STATE_ENABLING:
                    case WifiManager.WIFI_AP_STATE_ENABLED:
                        setSoftapEnabled(false);
                        break;
                    case WifiManager.WIFI_AP_STATE_DISABLING:
                    case WifiManager.WIFI_AP_STATE_DISABLED:
                        setSoftapEnabled(true);
                        break;
                }
            }
        };
        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                CmStatusBarView.runSettings("com.android.settings.TetherSettings", mContext);
                startCollapseActivity();
                return true;
            }
        };
        qsc.registerAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION, this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        updateTileState();
        updateQuickSettings();
    }

    private void updateTileState() {
        int state = mWifiManager.getWifiApState();
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                mLabel = mContext.getString(R.string.quick_settings_wifi_label_turnon);
                mDrawable = R.drawable.ic_qs_wifi_ap_off;
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                mLabel = mContext.getString(R.string.quick_settings_wifiap_label);
                mDrawable = R.drawable.ic_qs_wifi_ap_on;
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                mLabel = mContext.getString(R.string.quick_settings_wifi_label_turnoff);
                mDrawable = R.drawable.ic_qs_wifi_ap_on;
            case WifiManager.WIFI_AP_STATE_DISABLED:
            default:
                mLabel = mContext.getString(R.string.quick_settings_wifiap_off_label);
                mDrawable = R.drawable.ic_qs_wifi_ap_off;
                break;
        }
    }

    private void setSoftapEnabled(boolean enable) {
        final ContentResolver cr = mContext.getContentResolver();
        /**
         * Disable Wifi if enabling tethering
         */
        int wifiState = mWifiManager.getWifiState();
        if (enable && ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                    (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
            mWifiManager.setWifiEnabled(false);
            Settings.Secure.putInt(cr, Settings.Secure.WIFI_SAVED_STATE, 1);
        }

        // Turn on the Wifi AP
        mWifiManager.setWifiApEnabled(null, enable);

        /**
         *  If needed, restore Wifi on tether disable
         */
        if (!enable) {
            int wifiSavedState = 0;
            try {
                wifiSavedState = Settings.Secure.getInt(cr, Settings.Secure.WIFI_SAVED_STATE);
            } catch (Settings.SettingNotFoundException e) {
                // Do nothing here
            }
            if (wifiSavedState == 1) {
                mWifiManager.setWifiEnabled(true);
                Settings.System.putInt(cr, Settings.Secure.WIFI_SAVED_STATE, 0);
            }
        }
    }

}
