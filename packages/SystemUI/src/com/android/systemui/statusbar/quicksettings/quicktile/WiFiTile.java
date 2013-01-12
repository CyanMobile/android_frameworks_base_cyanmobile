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
    public void onWifiSignalChanged(boolean mIsWifiConnected, int mWifiSignalIconId, String wifiDesc) {
        WifiManager wfmg = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (mIsWifiConnected) {
            mDrawable = mWifiSignalIconId;
            if (wifiDesc != null) {
                mLabel = wifiDesc;
            } else {
                mLabel = mContext.getString(R.string.quick_settings_wifi_label_connected);
            }
        } else if (wfmg.isWifiEnabled()) {
            mDrawable = R.drawable.ic_qs_wifi_4;
            mLabel = mContext.getString(R.string.quick_settings_wifi_label);
        } else if (wfmg.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
            mDrawable = R.drawable.ic_qs_wifi_4;
            mLabel = mContext.getString(R.string.quick_settings_wifi_label_turnon);
        } else if (wfmg.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
            mDrawable = R.drawable.ic_qs_wifi_0;
            mLabel = mContext.getString(R.string.quick_settings_wifi_label_turnoff);
        } else if (wfmg.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN) {
            mDrawable = R.drawable.ic_qs_wifi_0;
            mLabel = mContext.getString(R.string.quick_settings_wifi_label_error);
        } else {
            mDrawable = R.drawable.ic_qs_wifi_0;
            mLabel = mContext.getString(R.string.quick_settings_wifi_off_label);
        }
        updateQuickSettings();
        flipTile();
    }

    @Override
    public void onMobileDataSignalChanged(boolean mMobileDataEnable, int mPhoneSignalIconId, int mDataDirection, int mDataSignalIconId) {
        // TODO Auto-generated method stub
    }
}
