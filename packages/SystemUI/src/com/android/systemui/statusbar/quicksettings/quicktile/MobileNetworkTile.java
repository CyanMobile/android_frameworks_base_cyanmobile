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

public class MobileNetworkTile extends QuickSettingsTile implements NetworkSignalChangedCallback{

    private int mDataTypeIconId;
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
    public void onWifiSignalChanged(boolean enabled, int wifiSignalIconId, String description) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onMobileDataSignalChanged(boolean enabled,
            int mobileSignalIconId, int dataTypeIconId, String description) {
        // TODO: If view is in awaiting state, disable
        dataOn = enabled;
        mDrawable = mobileSignalIconId;
        mDataTypeIconId = dataTypeIconId;
        mLabel = description;
        updateQuickSettings();
    }

    @Override
    public void onAirplaneModeChanged(boolean enabled) {
        // TODO Auto-generated method stub
    }

    @Override
    void updateQuickSettings() {
        TextView tv = (TextView) mTile.findViewById(R.id.rssi_textview);
        tv.setText(mLabel);
        ImageView iv = (ImageView) mTile.findViewById(R.id.rssi_image);
        ImageView iov = (ImageView) mTile.findViewById(R.id.rssi_overlay_image);
        iv.setImageResource(mDrawable);
        if (dataOn) {
            iov.setImageResource(mDataTypeIconId);
        } else {
            iov.setImageDrawable(null);
        }
    }
}
