package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;

public class WiFiDisplayTile extends QuickSettingsTile{

    private boolean enabled = false;
    private boolean connected = false;

    public WiFiDisplayTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container,
            QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mOnClick = new OnClickListener() {

            @Override
            public void onClick(View v) {
                startSettingsActivity(android.provider.Settings.ACTION_WIFI_SETTINGS);
            }
        };
        applyWiFiDisplayChanges();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final WifiManager wifiManager = (WifiManager) context
            .getSystemService(Context.WIFI_SERVICE);
        enabled = wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
        connected = wifiManager.getWifiState() == WifiManager.WIFI_AP_STATE_ENABLING;
        applyWiFiDisplayChanges();
    }

    private void applyWiFiDisplayChanges() {
        if(enabled || connected) {
            mLabel = mContext.getString(R.string.quick_settings_wifi_display_label);
            mDrawable = R.drawable.stat_wifi_on;
        }else{
            mLabel = mContext.getString(R.string.quick_settings_wifi_display_no_connection_label);
            mDrawable = R.drawable.stat_wifi_off;
        }
        if(mTile != null) {
            updateQuickSettings();
        }
    }

    @Override
    void updateQuickSettings() {
        mTile.setVisibility(enabled ? View.VISIBLE : View.GONE);
        super.updateQuickSettings();
    }
}
