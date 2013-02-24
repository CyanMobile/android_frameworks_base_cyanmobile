package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import android.provider.Settings;
import com.android.internal.telephony.Phone;

import com.android.systemui.R;
import com.android.systemui.statusbar.CmStatusBarView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;

public class MobileNetworkTile extends QuickSettingsTile implements NetworkSignalChangedCallback {

    public static final String ACTION_MODIFY_NETWORK_MODE = "com.android.internal.telephony.MODIFY_NETWORK_MODE";
    public static final String ACTION_MOBILE_DATA_CHANGED = "com.android.internal.telephony.MOBILE_DATA_CHANGED";
    public static final String EXTRA_NETWORK_MODE = "networkMode";

    private int mDataTypeIconId;
    private int mDataDirectIconId;
    private ConnectivityManager mConnM;
    private boolean dataOn = false;

    public MobileNetworkTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);
        mTileLayout = R.layout.quick_settings_tile_rssi;
        mConnM = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dataOn) {
                    if (dataState()) {
                        Intent intent = new Intent(ACTION_MODIFY_NETWORK_MODE);
                        intent.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_GSM_ONLY);
                        mContext.sendBroadcast(intent);
                    }
                    mConnM.setMobileDataEnabled(false);
                } else {
                    if (dataState()) {
                        Intent intent = new Intent(ACTION_MODIFY_NETWORK_MODE);
                        intent.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_WCDMA_PREF);
                        mContext.sendBroadcast(intent);
                    }
                    mConnM.setMobileDataEnabled(true);
                }
            }
        };
        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                CmStatusBarView.runPhoneSettings("com.android.phone.Settings", mContext);
                startCollapseActivity();
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

    private boolean dataState() {
       return (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.EXPANDED_MOBILEDATANETWORK_MODE, 0) == 1);
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
