/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.ContentResolver;
import android.provider.Settings;
import android.os.Handler;
import android.database.ContentObserver;
import android.content.BroadcastReceiver;

import com.android.systemui.R;

public class QuickTileView extends LinearLayout {
    private static final boolean DEBUG = false;
    private static final String TAG = "QuickTileView";

    private ImageView mAirplane;
    private ImageView mAlarm;
    private ImageView mBluetooth;
    private ImageView mBattery;
    private ImageView mBrightness;
    private ImageView mLocation;
    private ImageView mRotation;
    private ImageView mSignal;
    private ImageView mSignalOverlay;
    private ImageView mSetting;
    private ImageView mTorch;
    private ImageView mWifi;

    private TextView mAirplaneText;
    private TextView mAlarmText;
    private TextView mBluetoothText;
    private TextView mBatteryText;
    private TextView mBrightnessText;
    private TextView mLocationText;
    private TextView mRotationText;
    private TextView mSignalText;
    private TextView mSettingText;
    private TextView mTorchText;
    private TextView mWifiText;

    private boolean mAirplaneEnable;
    private boolean mRotationEnable;
    Handler mHandler;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            // resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.SHOW_TILE_VIEW), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.AIRPLANE_MODE_ON), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            ContentResolver resolver = mContext.getContentResolver();
            // mTileShow = (Settings.System.getInt(resolver, 
            //        Settings.System.SHOW_TILE_VIEW, 1) == 1);
            mAirplaneEnable = (Settings.System.getInt(resolver, 
                    Settings.System.AIRPLANE_MODE_ON, 0) == 1);
            mRotationEnable = (Settings.System.getInt(resolver, 
                    Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
            updateTileView();
            updateAirplaneView();
            updateBluetoothView();
            updateRotationView();
            updateSettingView();
        }
    }

    public QuickTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHandler=new Handler();

        mAirplane = (ImageView) findViewById(R.id.airplane_image);
        mAirplaneText = (TextView) findViewById(R.id.airplane_textview);

        mAlarm = (ImageView) findViewById(R.id.alarm_image);
        mAlarmText = (TextView) findViewById(R.id.alarm_textview);

        mBluetooth = (ImageView) findViewById(R.id.bluetooth_image);
        mBluetoothText = (TextView) findViewById(R.id.bluetooth_textview);

        mBattery = (ImageView) findViewById(R.id.battery_image);
        mBatteryText = (TextView) findViewById(R.id.battery_textview);

        mBrightness = (ImageView) findViewById(R.id.brightness_image);
        mBrightnessText = (TextView) findViewById(R.id.brightness_textview);

        mLocation = (ImageView) findViewById(R.id.location_image);
        mLocationText = (TextView) findViewById(R.id.location_textview);

        mRotation = (ImageView) findViewById(R.id.rotation_image);
        mRotationText = (TextView) findViewById(R.id.rotation_textview);

        mSignal = (ImageView) findViewById(R.id.signal_image);
        mSignalOverlay = (ImageView) findViewById(R.id.signal_overlay_image);
        mSignalText = (TextView) findViewById(R.id.signal_textview);

        mSetting = (ImageView) findViewById(R.id.setting_image);
        mSettingText = (TextView) findViewById(R.id.setting_textview);

        mTorch = (ImageView) findViewById(R.id.torch_image);
        mTorchText = (TextView) findViewById(R.id.torch_textview);

        mWifi = (ImageView) findViewById(R.id.wifi_image);
        mWifiText = (TextView) findViewById(R.id.wifi_textview);

        // set up settings observer
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    private void updateTileView() {
      // todo update tile view
    }

    private void updateAirplaneView() {
      if (mAirplaneEnable) {
          mAirplane.setImageResource(R.drawable.stat_airplane_on);
          mAirplaneText.setText("Airplane On");
      } else {
          mAirplane.setImageResource(R.drawable.stat_airplane_off);
          mAirplaneText.setText("Airplane Off");
      }
    }

    private void updateAlarmView() {
      mAlarmText.setText("Alarm");
    }

    private void updateBluetoothView() {
      //if (mBluetoothEnable) {
      //    mBluetooth.setImageResource(R.drawable.stat_bluetooth_on);
      //    mBluetoothText.setText("Bluetooth On");
      //} else {
          mBluetooth.setImageResource(R.drawable.stat_bluetooth_off);
          mBluetoothText.setText("Bluetooth Off");
      //}
    }

    private void updateBrightnessView() {
      mBrightnessText.setText("Brightness");
    }

    private void updateLocationView() {
      mLocationText.setText("GPS");
    }

    private void updateRotationView() {
      if (mRotationEnable) {
          mRotation.setImageResource(R.drawable.stat_orientation_on);
          mRotationText.setText("Rotation On");
      } else {
          mRotation.setImageResource(R.drawable.stat_orientation_off);
          mRotationText.setText("Rotation Off");
      }
    }

    private void updateSettingView() {
      mSetting.setImageResource(R.drawable.ic_sysbar_set);
      mSettingText.setText("Settings");
    }

    public void updateBattery(int dwb) {
      mBattery.setImageResource(dwb);
    }

    public void updateSignal(int dwb) {
      mSignal.setImageResource(dwb);
    }

    public void updateSignalOverlay(int dwb) {
      mSignalOverlay.setImageResource(dwb);
    }

    public void updateWifi(int dwb) {
      mWifi.setImageResource(dwb);
    }
}
