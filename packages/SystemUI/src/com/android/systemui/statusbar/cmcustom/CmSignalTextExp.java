/*
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

package com.android.systemui.statusbar.cmcustom;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.graphics.Color;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.util.TypedValue;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.TextView;
import com.android.systemui.statusbar.policy.StatusBarPolicy;
import com.android.systemui.R;

import java.util.Calendar;
import java.util.TimeZone;

public class CmSignalTextExp extends TextView {

    private int dBm = 0;
    private int ASU = 0;

    private SignalStrength signal;

    private boolean mAttached;

    private static final int STYLE_HIDE = 0;
    private static final int STYLE_SHOW = 1;
    private static final int STYLE_SHOW_DBM = 2;
    private static final int STYLE_SHOW_WITH_COLOR = 3;
    private static final int STYLE_HIDDEN = 4;

    private int mClockColor;

    private static int style;
    private int mCarrierSize;

    private int mPhoneState;

    private Handler mHandler;
    private Context mContext;
    private SettingsObserver mSettingsObserver;

    public CmSignalTextExp(Context context) {
        this(context, null);

    }

    private boolean mPhoneSignalHidden;

    public CmSignalTextExp(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler);

        // load config to determine if CmSignalText should be hidden
        try {
            mPhoneSignalHidden = context.getResources().getBoolean(
                R.bool.config_statusbar_hide_phone_signal);
        } catch (Exception e) {
            mPhoneSignalHidden = false;
        }

        // hide phone_signal icon if hidden
        if (mPhoneSignalHidden) {
            this.setVisibility(GONE);
        } else {
            this.setVisibility(VISIBLE);
        }

        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SIGNAL_DBM_CHANGED);
            mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
            mSettingsObserver.observe();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mAttached = false;
            mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SIGNAL_DBM_CHANGED)) {
                dBm = intent.getIntExtra("dbm", 0);
                mPhoneState = intent.getIntExtra("signal_status", StatusBarPolicy.PHONE_SIGNAL_IS_NORMAL);
            }
            updateSettings();
        }
    };

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_CM_SIGNAL_TEXT), false,
                    this);
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

    private String getSignalLevelString(int dBm) {
        if (mPhoneState == StatusBarPolicy.PHONE_SIGNAL_IS_NULL || dBm == 0) {
            return "-\u221e"; // -oo ('minus infinity')
        }
        return Integer.toString(dBm);
    }

    private void updateSettings() {
        updateSignalText();
    }

    private final void updateSignalText() {
        style = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CM_SIGNAL_TEXT, STYLE_HIDE);
        int defValuesColor = mContext.getResources().getInteger(com.android.internal.R.color.color_default_cyanmobile);
        mClockColor = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CLOCKCOLOR, defValuesColor));
        int defValuesFontSize = mContext.getResources().getInteger(com.android.internal.R.integer.config_fontsize_default_cyanmobile);
        float mCarrierSizeval = (float) Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUSBAR_ICON_FONT_SIZE, defValuesFontSize);
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int CarrierSizepx = (int) (metrics.density * mCarrierSizeval);
        mCarrierSize = CarrierSizepx;

        if (mPhoneState == StatusBarPolicy.PHONE_SIGNAL_IS_AIRPLANE_MODE) {
            setVisibility(View.GONE);
        } else if (style == STYLE_SHOW) {
            setVisibility(View.VISIBLE);
            setText(getSignalLevelString(dBm) + " ");
            setTextColor(mClockColor);
            setTextSize(mCarrierSize);
        } else if (style == STYLE_SHOW_DBM) {
            String result = getSignalLevelString(dBm) + " dBm";
            SpannableStringBuilder formatted = new SpannableStringBuilder(result);
            int start = result.indexOf("d");
            CharacterStyle style = new RelativeSizeSpan(0.7f);
            formatted.setSpan(style, start, start + 3, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

            setVisibility(View.VISIBLE);
            setText(formatted);
            setTextColor(mClockColor);
            setTextSize(mCarrierSize);
        } else if (style == STYLE_SHOW_WITH_COLOR) {
            setVisibility(View.VISIBLE);
            setText(getSignalLevelString(dBm) + " ");
            setTextSize(mCarrierSize);
            if (ASU <= 2 || ASU == 99)
                setTextColor(Color.RED);
            else if (ASU >= 12)
                setTextColor(Color.GREEN);
            else if (ASU >= 8)
                setTextColor(Color.BLUE);
            else if (ASU >= 5)
                setTextColor(Color.YELLOW);
            else
                setTextColor(Color.MAGENTA);
        } else if (style == STYLE_HIDDEN) {
            setVisibility(View.GONE);
        } else {
            setVisibility(View.GONE);
        }
    }

    /*
     * Phone listener to update signal information
     */
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (signalStrength != null) {
                ASU = signalStrength.getGsmSignalStrength();
                dBm = -113 + (2 * ASU);
            } else {
                // When signal strenth is null, let's set the values below to zero,
                // this showns then -oo in the status bar display
                ASU = 0;
                dBm = 0;
            }

            // update text if it's visible
            if (mAttached) {
                updateSignalText();
            }
        }
    };
}
