/*
 * Copyright (C) 2012 Sven Dawitz for the CyanogenMod Project
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
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.android.internal.R;

/***
 * Note about CircleBattery Implementation:
 *
 * Unfortunately, we cannot use BatteryController or DockBatteryController here,
 * since communication between controller and this view is not possible without
 * huge changes. As a result, this Class is doing everything by itself,
 * monitoring battery level and battery settings.
 */

public class CircleBattery extends ImageView {
    private Handler mHandler;
    private BatteryReceiver mBatteryReceiver = null;

    // state variables
    private boolean mAttached;      // whether or not attached to a window
    private boolean mActivated;     // whether or not activated due to system settings
    private boolean mPercentage;    // whether or not to show percentage number
    private boolean mIsCharging;    // whether or not device is currently charging
    private int     mLevel;         // current battery level
    private int     mAnimOffset;    // current level of charging animation
    private boolean mIsAnimating;   // stores charge-animation status to reliably remove callbacks

    private int     mCircleSize;    // draw size of circle. read rather complicated from
                                    // another status bar icon, so it fits the icon size
                                    // no matter the dps and resolution
    private RectF   mCircleRect;    // contains the precalculated rect used in drawArc(), derived from mCircleSize
    private Float   mPercentX;      // precalculated x position for drawText() to appear centered
    private Float   mPercentY;      // precalculated y position for drawText() to appear vertical-centered

    // quiet a lot of paint variables. helps to move cpu-usage from actual drawing to initialization
    private Paint   mPaintFont;
    private Paint   mPaintGray;
    private Paint   mPaintSystem;
    private Paint   mPaintRed;
    private Context mContext;
    private SettingsObserver mSettingsObserver;

    // runnable to invalidate view via mHandler.postDelayed() call
    private final Runnable mInvalidate = new Runnable() {
        @Override
        public void run() {
            if(mActivated && mAttached) {
                invalidate();
            }
        }
    };

    // observes changes in system battery settings and enables/disables view accordingly
    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY), false, this);
	    resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY_COLOR), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUSBAR_ICONS_SIZE), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            int batteryStyle = (Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, 0));

            mActivated = (batteryStyle == 8 || batteryStyle == 9);
            mPercentage = (batteryStyle == 9);

            setVisibility(mActivated ? View.VISIBLE : View.GONE);
            if (mBatteryReceiver != null) {
                mBatteryReceiver.updateRegistration();
            }

            if (mActivated && mAttached) {
                invalidate();
            }

             initSizeMeasureIconHeight();
        }
    }

    // keeps track of current battery level and charger-plugged-state
    private class BatteryReceiver extends BroadcastReceiver {
        private boolean mIsRegistered = false;

        public BatteryReceiver(Context context) {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                mLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                mIsCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;

                if (mActivated && mAttached) {
                    invalidate();
                }
            }
        }

        private void registerSelf() {
            if (!mIsRegistered) {
                mIsRegistered = true;

                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                mContext.registerReceiver(mBatteryReceiver, filter);
            }
        }

        private void unregisterSelf() {
            if (mIsRegistered) {
                mIsRegistered = false;
                mContext.unregisterReceiver(this);
            }
        }

        private void updateRegistration() {
            if (mActivated && mAttached) {
                registerSelf();
            } else {
                unregisterSelf();
            }
        }
    }

    /***
     * Start of CircleBattery implementation
     */
    public CircleBattery(Context context) {
        this(context, null);
    }

    public CircleBattery(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleBattery(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mHandler = new Handler();

        mSettingsObserver = new SettingsObserver(mHandler);
        mBatteryReceiver = new BatteryReceiver(context);

        // initialize and setup all paint variables
        // stroke width is later set in initSizeBasedStuff()
        Resources res = getResources();
        int defValuesColor = context.getResources().getInteger(R.color.color_default_cyanmobile);
        String colores = Settings.System.getString(context.getContentResolver(), Settings.System.STATUS_BAR_BATTERY_COLOR);
        Integer barColor = null;
        if (!TextUtils.isEmpty(colores)) {
           try {
                barColor = Color.parseColor(colores);
           } catch (IllegalArgumentException e) {
           }
        }

        mPaintFont = new Paint();
        mPaintFont.setAntiAlias(true);
        mPaintFont.setDither(true);
        mPaintFont.setStyle(Paint.Style.STROKE);

        mPaintGray = new Paint(mPaintFont);
        mPaintSystem = new Paint(mPaintFont);
        mPaintRed = new Paint(mPaintFont);

        if (barColor != null) {
            mPaintFont.setColor(barColor);
            mPaintSystem.setColor(barColor);
        } else {
            mPaintFont.setColor(defValuesColor);
            mPaintSystem.setColor(defValuesColor);
        }
        // could not find the darker definition anywhere in resources
        // do not want to use static 0x404040 color value. would break theming.
        mPaintGray.setColor(Color.GRAY);
        mPaintRed.setColor(Color.RED);

        // font needs some extra settings
        mPaintFont.setTextAlign(Align.CENTER);
        mPaintFont.setFakeBoldText(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            mBatteryReceiver.updateRegistration();
            mSettingsObserver.observe();
            mHandler.postDelayed(mInvalidate, 250);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mAttached = false;
            mBatteryReceiver.updateRegistration();
            mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
            mCircleRect = null; // makes sure, size based variables get
                                // recalculated on next attach
            mCircleSize = 0;    // makes sure, mCircleSize is reread from icons on
                                // next attach
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mCircleSize == 0) {
            initSizeMeasureIconHeight();
        }
        setMeasuredDimension(mCircleSize + getPaddingLeft(), mCircleSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCircleRect == null) {
            initSizeBasedStuff();
        }

        updateChargeAnim();

        int defValuesColor = mContext.getResources().getInteger(R.color.color_default_cyanmobile);
        String colores = Settings.System.getString(mContext.getContentResolver(), Settings.System.STATUS_BAR_BATTERY_COLOR);
        Integer barColor = null;
        if (!TextUtils.isEmpty(colores)) {
           try {
                barColor = Color.parseColor(colores);
           } catch (IllegalArgumentException e) {
           }
        }

        if (barColor != null) {
            mPaintSystem.setColor(barColor);
        } else {
            mPaintSystem.setColor(defValuesColor);
        }

        Paint usePaint = mPaintSystem;
        // turn red at 14% - same level android battery warning appears
        if (mLevel <= 14) {
            usePaint = mPaintRed;
        }

        // pad circle percentage to 100% once it reaches 97%
        // for one, the circle looks odd with a too small gap,
        // for another, some phones never reach 100% due to hardware design
        int padLevel = mLevel;
        if (mLevel >= 97) {
            padLevel=100;
        }

        // draw thin gray ring first
        canvas.drawArc(mCircleRect, 270, 360, false, mPaintGray);
        // draw thin colored ring-level last
        canvas.drawArc(mCircleRect, 270+mAnimOffset, 3.6f * padLevel, false, usePaint);
        // if chosen by options, draw percentage text in the middle
        // always skip percentage when 100, so layout doesnt break
        if (mLevel < 100 && mPercentage){
            mPaintFont.setColor(usePaint.getColor());
            canvas.drawText(Integer.toString(mLevel), mPercentX, mPercentY, mPaintFont);
        }
    }

    /***
     * updates the animation counter
     * cares for timed callbacks to continue animation cycles
     * uses mInvalidate for delayed invalidate() callbacks
     */
    private void updateChargeAnim() {
        if (!mIsCharging || mLevel >= 97) {
            if (mIsAnimating) {
                mIsAnimating = false;
                mAnimOffset = 0;
                mHandler.removeCallbacks(mInvalidate);
            }
            return;
        }

        mIsAnimating = true;

        if (mAnimOffset > 360) {
            mAnimOffset = 0;
        } else {
            mAnimOffset += 3;
        }

        mHandler.removeCallbacks(mInvalidate);
        mHandler.postDelayed(mInvalidate, 50);
    }

    /***
     * initializes all size dependent variables
     * sets stroke width and text size of all involved paints
     * YES! i think the method name is appropriate
     */
    private void initSizeBasedStuff() {
        if (mCircleSize == 0) {
            initSizeMeasureIconHeight();
        }

        mPaintFont.setTextSize(mCircleSize / 2f);

        float strokeWidth = mCircleSize / 6.5f;
        mPaintRed.setStrokeWidth(strokeWidth);
        mPaintSystem.setStrokeWidth(strokeWidth);
        mPaintGray.setStrokeWidth(strokeWidth / 3.5f);

        // calculate rectangle for drawArc calls
        int pLeft = getPaddingLeft();
        mCircleRect = new RectF(pLeft + strokeWidth / 2.0f, 0 + strokeWidth / 2.0f, mCircleSize
                - strokeWidth / 2.0f + pLeft, mCircleSize - strokeWidth / 2.0f);

        // calculate Y position for text
        Rect bounds = new Rect();
        mPaintFont.getTextBounds("99", 0, "99".length(), bounds);
        mPercentX = mCircleSize / 2.0f + getPaddingLeft();
        // the +1 at end of formular balances out rounding issues. works out on all resolutions
        mPercentY = mCircleSize / 2.0f + (bounds.bottom - bounds.top) / 2.0f - strokeWidth / 2.0f + 1;

        // force new measurement for wrap-content xml tag
        onMeasure(0, 0);
    }

    /***
     * we need to measure the size of the circle battery by checking another
     * resource. unfortunately, those resources have transparent/empty borders
     * so we have to count the used pixel manually and deduct the size from
     * it. quiet complicated, but the only way to fit properly into the
     * statusbar for all resolutions
     */
    private void initSizeMeasureIconHeight() {
        int defValuesFontSize = mContext.getResources().getInteger(com.android.internal.R.integer.config_fontsize_default_cyanmobile);
        float mCarrierSizeval = (float) Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUSBAR_ICONS_SIZE, defValuesFontSize);
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int CarrierSizepx = (int) (metrics.density * (mCarrierSizeval * 1.8));
        mCircleSize = CarrierSizepx;
    }
}
