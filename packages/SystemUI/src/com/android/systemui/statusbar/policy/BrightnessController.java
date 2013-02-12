/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Power;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Slog;
import android.view.IWindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;

import java.util.ArrayList;
import com.android.systemui.statusbar.popups.BrightnessPanel;

public class BrightnessController implements ToggleSlider.Listener {
    private static final String TAG = "StatusBar.BrightnessController";

    private final int mMinimumBacklight = Power.BRIGHTNESS_DIM + 8;
    private final int mMaximumBacklight = Power.BRIGHTNESS_ON;

    private final Context mContext;
    private final ImageView mIcon;
    private final ToggleSlider mControl;
    private final boolean mAutomaticAvailable;
    private final IPowerManager mPower;
    private BrightnessPanel mBrightnessPanel = null;
    private final Handler mHandler = new Handler();

    private ArrayList<BrightnessStateChangeCallback> mChangeCallbacks =
            new ArrayList<BrightnessStateChangeCallback>();

    public interface BrightnessStateChangeCallback {
        public void onBrightnessLevelChanged();
    }

    public BrightnessController(Context context, ImageView icon, ToggleSlider control) {
        mContext = context;
        mIcon = icon;
        mControl = control;

        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));

        control.setOnChangedListener(this);
    }

    public void addStateChangedCallback(BrightnessStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    @Override
    public void onInit(ToggleSlider control) {
        if (mAutomaticAvailable) {
            int automatic = Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
            control.setChecked(automatic != 0);
            updateIcon(automatic != 0);
        } else {
            control.setChecked(false);
            updateIcon(false /*automatic*/);
            //control.hideToggle();
        }
        
        int value = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, mMaximumBacklight);

        control.setMax(mMaximumBacklight - mMinimumBacklight);
        control.setValue(value - mMinimumBacklight);
    }

    public void onChanged(ToggleSlider view, boolean tracking, boolean automatic, int value) {
        setMode(automatic ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        updateIcon(automatic);
        if (!automatic) {
            final int val = value + mMinimumBacklight;
            setBrightness(val);
            if (mBrightnessPanel == null) {
                mBrightnessPanel = new BrightnessPanel(mContext);
            }
            mBrightnessPanel.postBrightnessChanged(val, Power.BRIGHTNESS_ON);
            if (!tracking) {
                mHandler.postDelayed(new Runnable() {
                        public void run() {
                            Settings.System.putInt(mContext.getContentResolver(),
                                    Settings.System.SCREEN_BRIGHTNESS, val);
                        }
                    }, 1000);
            }
        }

        for (BrightnessStateChangeCallback cb : mChangeCallbacks) {
            cb.onBrightnessLevelChanged();
        }
    }

    private void setMode(int mode) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }
    
    private void setBrightness(int brightness) {
        try {
            mPower.setBacklightBrightness(brightness);
        } catch (RemoteException ex) {
        }        
    }

    private void updateIcon(boolean automatic) {
        if (mIcon != null) {
            mIcon.setImageResource(automatic ?
                    com.android.systemui.R.drawable.ic_qs_brightness_auto_on :
                    com.android.systemui.R.drawable.ic_qs_brightness_auto_off);
        }
    }
}
