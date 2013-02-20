/*
 * Created by Sven Dawitz; Copyright (C) 2011 CyanogenMod Project
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

package com.android.systemui.statusbar.batteries;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.util.TypedValue;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.TextView;

/**
 * This widget displays the percentage of the battery as a number
 */
public class CmBatteryTextExp extends TextView {
    private boolean mAttached;

    // battery style preferences
    private static final int BATTERY_STYLE_PERCENT   = 1;
    private static final int BATTERY_STYLE_PERCENTS   = 0;
    private static final int BATTERY_STYLE_PERCENT_WITH   = 1;
    private static final int BATTERY_STYLE_COLOR   = 2;
    private int mStatusBarBattery;
    private int mClockColor;
    private int mCarrierSize;

    private static int style;

    private Handler mHandler;
    private Context mContext;
    private SettingsObserver mSettingsObserver;

    // tracks changes to settings, so status bar is auto updated the moment the
    // setting is toggled
    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_BATTERY), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_BATTERY_STYLE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCKCOLOR), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_ICON_FONT_SIZE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public CmBatteryTextExp(Context context) {
        this(context, null);
    }

    public CmBatteryTextExp(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CmBatteryTextExp(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler);
        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
            mSettingsObserver.observe();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mContext.unregisterReceiver(mIntentReceiver);
            mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
            mAttached = false;
        }
    }

    /**
     * Handles changes ins battery level and charger connection
     */
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                updateCmBatteryText(intent);
            }
        }
    };

    /**
     * Sets the output text. Kind of onDraw of canvas based classes
     *
     * @param intent
     */
    private final void updateCmBatteryText(Intent intent) {
        style = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, 0);
        int defValuesColor = mContext.getResources().getInteger(com.android.internal.R.color.color_default_cyanmobile);
        mClockColor = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CLOCKCOLOR, defValuesColor));
        int defValuesFontSize = mContext.getResources().getInteger(com.android.internal.R.integer.config_fontsize_default_cyanmobile);
        float mCarrierSizeval = (float) Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUSBAR_ICON_FONT_SIZE, defValuesFontSize);
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int CarrierSizepx = (int) (metrics.density * mCarrierSizeval);
        mCarrierSize = CarrierSizepx;

        int level = intent.getIntExtra("level", 0);
        if (style == BATTERY_STYLE_PERCENTS) {
            setText(Integer.toString(level));
            setTextSize(mCarrierSize);
            if (level <= 15){
               setTextColor(Color.RED);
            } else {
               setTextColor(mClockColor);
            }
        } else if (style == BATTERY_STYLE_PERCENT_WITH) {
            String result = Integer.toString(level) + "% ";

            SpannableStringBuilder formatted = new SpannableStringBuilder(result);
            int start = result.indexOf("%");

            CharacterStyle style = new RelativeSizeSpan(0.7f);
            formatted.setSpan(style, start, start + 1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

            setText(formatted);
            setTextSize(mCarrierSize);
            if (level <= 15) {
               setTextColor(Color.RED);
            } else {
               setTextColor(mClockColor);
            }
        } else if (style == BATTERY_STYLE_COLOR) {
            setText(Integer.toString(level));
            setTextSize(mCarrierSize);
            if (level >= 90) {
                setTextColor(Color.GREEN);
            } else if (level >= 65 && level <= 90) {
                setTextColor(Color.BLUE);
            } else if (level >= 45 && level < 65) {
                setTextColor(Color.YELLOW);
            } else if (level > 15 && level < 45) {
                setTextColor(Color.GRAY);
            } else if (level <= 15) {
                setTextColor(Color.RED);
            } else {
                setTextColor(Color.MAGENTA);
            }
        }
    }

    /**
     * Invoked by SettingsObserver, this method keeps track of just changed
     * settings. Also does the initial call from constructor
     */
    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        int statusBarBattery = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY, 2));
        mStatusBarBattery = Integer.valueOf(statusBarBattery);

        if (mStatusBarBattery == BATTERY_STYLE_PERCENT) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }
}
