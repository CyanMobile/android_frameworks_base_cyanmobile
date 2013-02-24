package com.android.systemui.statusbar.powerwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import com.android.systemui.statusbar.CmStatusBarView;

public class UsbTetherButton extends PowerButton {

    public UsbTetherButton() { mType = BUTTON_USBAP;}

    @Override
    protected boolean handleLongClick(Context context) {
        CmStatusBarView.runSettings("com.android.settings.TetherSettings", context);
        return true;
    }

    @Override
    protected void toggleState(Context context) {
        CmStatusBarView.runSettings("com.android.settings.TetherSettings", context);
    }

    @Override
    protected void updateState(Context context) {
       mIcon = R.drawable.stat_usb_tether_icon_off;
       mState = STATE_DISABLED;
    }
}
