/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.systemui.statusbar.carrierlabels;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.CmSystem;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.TypedValue;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.TextView;

import com.android.internal.R;
import com.android.internal.telephony.TelephonyProperties;

/**
 * This widget display the current network status or registered PLMN, and/or
 * SPN if available.
 */
public class CarrierLabelBottom extends TextView {
    private boolean mAttached;

    private boolean mShowSpn;
    private String mSpn;
    private boolean mShowPlmn;
    private boolean mAirplaneOn;
    private String mPlmn;
    private int mCarrierColor;
    private int mCarrierSize;

    private int mCarrierLabelType;
    private String mCarrierLabelCustom;

    private static final int TYPE_DEFAULT = 0;

    private static final int TYPE_SPN = 1;

    private static final int TYPE_PLMN = 2;

    private static final int TYPE_CUSTOM = 3;

    private Handler mHandler;
    private Context mContext;
    private SettingsObserver mSettingsObserver;

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.CARRIER_LABEL_TYPE),
                    false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.CARRIER_LABEL_CUSTOM_STRING),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CARRIERCOLOR), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_CARRIER_FONT_SIZE), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.AIRPLANE_MODE_ON), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
            updateNetworkName(mShowSpn, mSpn, mShowPlmn, mPlmn);
        }
    }

    public CarrierLabelBottom(Context context) {
        this(context, null);
    }

    public CarrierLabelBottom(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarrierLabelBottom(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler);
        updateSettings();
        updateNetworkName(false, null, false, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Telephony.Intents.SPN_STRINGS_UPDATED_ACTION);
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

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Telephony.Intents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
                updateNetworkName(intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_SPN, false),
                        intent.getStringExtra(Telephony.Intents.EXTRA_SPN),
                        intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_PLMN, false),
                        intent.getStringExtra(Telephony.Intents.EXTRA_PLMN));
            }
        }
    };

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        int defValuesColor = mContext.getResources().getInteger(com.android.internal.R.color.color_default_cyanmobile);
        int defValuesFontSize = mContext.getResources().getInteger(com.android.internal.R.integer.config_fontsize_default_cyanmobile);
        mCarrierLabelType = Settings.System.getInt(resolver,
                Settings.System.CARRIER_LABEL_TYPE, TYPE_DEFAULT);
        mCarrierLabelCustom = Settings.System.getString(resolver,
                Settings.System.CARRIER_LABEL_CUSTOM_STRING);
        mAirplaneOn = (Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) == 1);
        mCarrierColor = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CARRIERCOLOR, defValuesColor));
        float mCarrierSizeval = (float) Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CARRIER_FONT_SIZE, defValuesFontSize);
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int CarrierSizepx = (int) (metrics.density * mCarrierSizeval);
        mCarrierSize = CarrierSizepx;
    }

    private void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
        if (false) {
            Slog.d("CarrierLabelBottom", "updateNetworkName showSpn=" + showSpn + " spn=" + spn
                    + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }

        mShowSpn = showSpn;
        mSpn = spn;
        mShowPlmn = showPlmn;
        mPlmn = plmn;
        setTextColor(mCarrierColor);
        setTextSize(mCarrierSize);

        boolean haveSignal = (showPlmn && plmn != null) || (showSpn && spn != null);
        if (!haveSignal) {
            if (mAirplaneOn) {
                setText("Airplane Mode");
                return;
            } else {
                setText(com.android.internal.R.string.lockscreen_carrier_default);
                return;
            }
        }

        String realPlmn = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ALPHA);
        int carrierLabelType = mCarrierLabelType;

        if (plmn != null && !(plmn.equals(realPlmn))) {
            carrierLabelType = TYPE_DEFAULT;
        }

        switch (carrierLabelType) {
            default:
            case TYPE_DEFAULT:
                StringBuilder str = new StringBuilder();
                if (showPlmn) {
                    if (plmn != null) {
                        str.append(plmn);
                    } else {
                        str.append(mContext.getText(R.string.lockscreen_carrier_default));
                    }
                }
                if (showSpn && spn != null) {
                    if (showPlmn) {
                        str.append('\n');
                    }
                    str.append(spn);
                }
                setText(str.toString());
                break;

            case TYPE_SPN:
                setText(spn);
                break;

            case TYPE_PLMN:
                setText(plmn);
                break;

            case TYPE_CUSTOM:
                setText(mCarrierLabelCustom);
                break;
        }
    }

}
