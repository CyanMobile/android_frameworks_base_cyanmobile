package com.android.systemui.statusbar.quicksettings.quicktile;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHid;
import android.bluetooth.BluetoothPbap;
import android.bluetooth.BluetoothPan;
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

    private boolean enabled;
    private boolean turningOn;
    private boolean turningOff;
    private boolean errored;
    private boolean connected;
    private int mHeadsetState;
    private boolean mA2dpConnected;
    private int mHidState;
    private int mPbapState;
    private boolean mPanConnected;
    private BluetoothAdapter mBluetoothAdapter;

    public BluetoothTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        enabled = mBluetoothAdapter.isEnabled();
        turningOn = false;
        turningOff = false;
        errored = false;
        connected = false;
        mA2dpConnected = false;
        mHeadsetState = BluetoothHeadset.STATE_DISCONNECTED;
        mPbapState = BluetoothPbap.STATE_DISCONNECTED;
        mPanConnected = false;

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
        qsc.registerAction(BluetoothHeadset.ACTION_STATE_CHANGED, this);
        qsc.registerAction(BluetoothA2dp.ACTION_SINK_STATE_CHANGED, this);
        qsc.registerAction(BluetoothPbap.PBAP_STATE_CHANGED_ACTION, this);
        qsc.registerAction(BluetoothHid.HID_DEVICE_STATE_CHANGED_ACTION, this);
        qsc.registerAction(BluetoothPan.INTERFACE_ADDED, this);
        qsc.registerAction(BluetoothPan.ALL_DISCONNECTED, this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            enabled = (state == BluetoothAdapter.STATE_ON);
            turningOn = (state == BluetoothAdapter.STATE_TURNING_ON);
            turningOff = (state == BluetoothAdapter.STATE_TURNING_OFF);
            errored = (state == BluetoothAdapter.ERROR);
        } else if (action.equals(BluetoothHeadset.ACTION_STATE_CHANGED)) {
            mHeadsetState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                    BluetoothHeadset.STATE_ERROR);
        } else if (action.equals(BluetoothA2dp.ACTION_SINK_STATE_CHANGED)) {
            BluetoothA2dp a2dp = new BluetoothA2dp(mContext);
            if (a2dp.getConnectedSinks().size() != 0) {
                mA2dpConnected = true;
            } else {
                mA2dpConnected = false;
            }
        } else if (action.equals(BluetoothPbap.PBAP_STATE_CHANGED_ACTION)) {
            mPbapState = intent.getIntExtra(BluetoothPbap.PBAP_STATE,
                    BluetoothPbap.STATE_DISCONNECTED);
        } else if (action.equals(BluetoothHid.HID_DEVICE_STATE_CHANGED_ACTION)) {
            mHidState = intent.getIntExtra(BluetoothHid.HID_DEVICE_STATE,
                    BluetoothHid.STATE_DISCONNECTED);
        } else if (action.equals(BluetoothPan.INTERFACE_ADDED)) {
            mPanConnected = true;
        } else if (action.equals(BluetoothPan.ALL_DISCONNECTED)) {
            mPanConnected = false;
        }

        if (mHeadsetState == BluetoothHeadset.STATE_CONNECTED || mA2dpConnected ||
                mHidState == BluetoothHid.STATE_CONNECTED ||
                mPbapState == BluetoothPbap.STATE_CONNECTED || mPanConnected) {
            connected = true;
        }
        applyBluetoothChanges();
    }

    void checkBluetoothState() {
        enabled = mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private void applyBluetoothChanges(){
        if (enabled) {
            mDrawable = R.drawable.ic_qs_bluetooth_not_connected;
            mLabel = mContext.getString(R.string.quick_settings_bluetooth_label);
        } else if (turningOn) {
            mDrawable = R.drawable.ic_qs_bluetooth_off;
            mLabel = mContext.getString(R.string.quick_settings_bluetooth_label_turnon);
        } else if (turningOff) {
            mDrawable = R.drawable.ic_qs_bluetooth_not_connected;
            mLabel = mContext.getString(R.string.quick_settings_bluetooth_label_turnoff);
        } else if (errored) {
            mDrawable = R.drawable.ic_qs_bluetooth_neutral;
            mLabel = mContext.getString(R.string.quick_settings_bluetooth_label_error);
        } else if (enabled && connected) {
            mDrawable = R.drawable.ic_qs_bluetooth_on;
            mLabel = mContext.getString(R.string.quick_settings_bluetooth_label_connected);
        } else {
            mDrawable = R.drawable.ic_qs_bluetooth_off;
            mLabel = mContext.getString(R.string.quick_settings_bluetooth_off_label);
        }
        updateQuickSettings();
        flipTile();
    }

    @Override
    void onPostCreate() {
        checkBluetoothState();
        applyBluetoothChanges();
        super.onPostCreate();
    }

}
