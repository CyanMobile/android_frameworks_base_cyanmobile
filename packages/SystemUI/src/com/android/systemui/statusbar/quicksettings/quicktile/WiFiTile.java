package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;

public class WiFiTile extends QuickSettingsTile implements NetworkSignalChangedCallback {

    public WiFiTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);
        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiManager wfm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                wfm.setWifiEnabled(!wfm.isWifiEnabled());
            }
        };
        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity(android.provider.Settings.ACTION_WIFI_SETTINGS);
                return true;
            }
        };
    }

    @Override
    void onPostCreate() {
        NetworkController controller = new NetworkController(mContext);
        controller.addNetworkSignalChangedCallback(this);
        super.onPostCreate();
    }

    @Override
    public void onWifiSignalChanged(boolean mIsWifiConnected, int mWifiSignalIconId) {
        WifiManager wfmg = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (mIsWifiConnected) {
            mDrawable = mWifiSignalIconId;
            mLabel = mContext.getString(R.string.quick_settings_wifi_label_connected);
        } else if (wfmg.isWifiEnabled()) {
            mDrawable = R.drawable.stat_wifi_on;
            mLabel = mContext.getString(R.string.quick_settings_wifi_label);
        } else {
            mDrawable = R.drawable.stat_wifi_off;
            mLabel = mContext.getString(R.string.quick_settings_wifi_off_label);
        }
        updateQuickSettings();
    }

    @Override
    public void onMobileDataSignalChanged(boolean mMobileDataEnable, int mPhoneSignalIconId, int mDataSignalIconId, String description) {
        // TODO Auto-generated method stub
    }
}
