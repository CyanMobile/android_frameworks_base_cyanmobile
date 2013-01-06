package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;

public class MobileNetworkTile extends QuickSettingsTile implements NetworkSignalChangedCallback {

    private int mDataTypeIconId;
    private int mDataDirectIconId;
    private boolean dataOn = false;

    public MobileNetworkTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);
        mTileLayout = R.layout.quick_settings_tile_rssi;
        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
                ConnectivityManager conMan = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                if(tm.getDataState() == TelephonyManager.DATA_DISCONNECTED){
                    conMan.setMobileDataEnabled(true);
                }else{
                    conMan.setMobileDataEnabled(false);
                }
            }
        };
        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.phone", "com.android.phone.Settings");
                startSettingsActivity(intent);
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
        // TODO Auto-generated method stub
    }

    @Override
    public void onMobileDataSignalChanged(boolean mMobileDataEnable, int mPhoneSignalIconId, int mDataDirection, int mDataSignalIconId) {
        // TODO: If view is in awaiting state, disable
        dataOn = mMobileDataEnable;
        mDrawable = mPhoneSignalIconId;
        mDataDirectIconId = mDataDirection;
        mDataTypeIconId = mDataSignalIconId;
        updateQuickSettings();
    }

    @Override
    void updateQuickSettings() {
        ImageView iv = (ImageView) mTile.findViewById(R.id.rssi_image);
        ImageView ivv = (ImageView) mTile.findViewById(R.id.rssi_inout_image);
        ImageView iov = (ImageView) mTile.findViewById(R.id.rssi_overlay_image);
        iv.setBackgroundResource(mDrawable);
        if (dataOn) {
            if (mDataDirectIconId != 0) {
                ivv.setVisibility(View.VISIBLE);
                ivv.setBackgroundResource(mDataDirectIconId);
            } else {
                ivv.setVisibility(View.GONE);
            }
            iov.setVisibility(View.VISIBLE);
            iov.setBackgroundResource(mDataTypeIconId);
        } else {
            ivv.setVisibility(View.GONE);
            iov.setVisibility(View.GONE);
        }
        flipTile();
    }
}
