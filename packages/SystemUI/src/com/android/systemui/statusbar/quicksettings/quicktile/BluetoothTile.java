package com.android.systemui.statusbar.quicksettings.quicktile;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;

public class BluetoothTile extends QuickSettingsTile {

    private boolean enabled = false;
    private boolean connected = false;
    private BluetoothAdapter mBluetoothAdapter;

    public BluetoothTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        enabled = mBluetoothAdapter.isEnabled();

        mOnClick = new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(enabled){
                    mBluetoothAdapter.disable();
                }else{
                    mBluetoothAdapter.enable();
                }
            }
        };

        mOnLongClick = new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                return true;
            }
        };
        qsc.registerAction(BluetoothAdapter.ACTION_STATE_CHANGED, this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            enabled = (state == BluetoothAdapter.STATE_ON);
        }
        applyBluetoothChanges();
    }

    void checkBluetoothState() {
        enabled = mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private void applyBluetoothChanges(){
        if(enabled){
            mDrawable = R.drawable.stat_bluetooth_on;
            mLabel = mContext.getString(R.string.quick_settings_bluetooth_label);
        }else{
            mDrawable = R.drawable.stat_bluetooth_off;
            mLabel = mContext.getString(R.string.quick_settings_bluetooth_off_label);
        }
        updateQuickSettings();
    }

    @Override
    void onPostCreate() {
        checkBluetoothState();
        applyBluetoothChanges();
        super.onPostCreate();
    }

}
