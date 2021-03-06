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

package com.android.internal.policy.impl;

import com.android.internal.widget.DigitalClock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.os.SystemProperties;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.text.format.DateFormat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import android.view.ViewGroup;
import java.util.List;
import java.util.Date;

/**
 * This is the screen that shows the 9 circle unlock widget and instructs
 * the user how to unlock their device, or make an emergency call.
 */
class PatternUnlockScreen extends LinearLayoutWithDefaultTouchRecepient
        implements KeyguardScreen, KeyguardUpdateMonitor.InfoCallback,
        KeyguardUpdateMonitor.SimStateCallback {

    private static final boolean DBG = false;
    private static final boolean DEBUG = false;
    private static final String TAG = "UnlockScreen";

    // how long before we clear the wrong pattern
    private static final int PATTERN_CLEAR_TIMEOUT_MS = 2000;

    // how long we stay awake after each key beyond MIN_PATTERN_BEFORE_POKE_WAKELOCK
    private static final int UNLOCK_PATTERN_WAKE_INTERVAL_MS = 7000;

    // how long we stay awake after the user hits the first dot.
    private static final int UNLOCK_PATTERN_WAKE_INTERVAL_FIRST_DOTS_MS = 2000;

    // how many cells the user has to cross before we poke the wakelock
    private static final int MIN_PATTERN_BEFORE_POKE_WAKELOCK = 2;

    private int mFailedPatternAttemptsSinceLastTimeout = 0;
    private int mTotalFailedPatternAttempts = 0;
    private CountDownTimer mCountdownTimer = null;

    private LockPatternUtils mLockPatternUtils;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private KeyguardScreenCallback mCallback;
    static final int CARRIER_TYPE_DEFAULT = 0;
    static final int CARRIER_TYPE_SPN = 1;
    static final int CARRIER_TYPE_PLMN = 2;
    static final int CARRIER_TYPE_CUSTOM = 3;

    private Status mStatus = Status.Normal;

    /**
     * whether there is a fallback option available when the pattern is forgotten.
     */
    private boolean mEnableFallback;

    private String mDateFormatString;

    private TextView mCarrier;
    private DigitalClock mClock;
    private TextView mDate;
    private TextView mTime;
    private TextView mAmPm;

    // are we showing battery information?
    private boolean mShowingBatteryInfo = false;

    // always showing battery information?
    private boolean mLockAlwaysBattery = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_ALWAYS_BATTERY, 0) == 1);

    // last known plugged in state
    private boolean mPluggedIn = false;

    // last known battery level
    private int mBatteryLevel = 100;
    private String mCharging = null;

    private int defValuesColor = mContext.getResources().getInteger(com.android.internal.R.color.color_default_cyanmobile);

    private int mClockColor = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_LOCKSCREENCOLOR, defValuesColor));

    private int mCarrierColor = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CARRIERCOLOR, defValuesColor));

    private String mNextAlarm = null;

    private String mInstructions = null;
    private TextView mStatus1;
    private TextView mStatusSep;
    private TextView mStatus2;
    private TextView mCustomMsg;

    private LockPatternView mLockPatternView;

    private ViewGroup mFooterNormal;
    private ViewGroup mFooterForgotPattern;

    private int mCarrierLabelType = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.CARRIER_LABEL_LOCKSCREEN_TYPE, CARRIER_TYPE_DEFAULT));

    private String mCarrierLabelCustom = (Settings.System.getString(mContext.getContentResolver(),
            Settings.System.CARRIER_LABEL_LOCKSCREEN_CUSTOM_STRING));

    /**
     * Keeps track of the last time we poked the wake lock during dispatching
     * of the touch event, initalized to something gauranteed to make us
     * poke it when the user starts drawing the pattern.
     * @see #dispatchTouchEvent(android.view.MotionEvent)
     */
    private long mLastPokeTime = -UNLOCK_PATTERN_WAKE_INTERVAL_MS;

    /**
     * Useful for clearing out the wrong pattern after a delay
     */
    private Runnable mCancelPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
        }
    };

    private Button mForgotPatternButton;
    private Button mEmergencyAlone;
    private Button mEmergencyTogether;
    private int mCreationOrientation;

    /**
     * The status of this lock screen.
     */
    enum Status {
        /**
         * Normal case (sim card present, it's not locked)
         */
        Normal(true),

        /**
         * The sim card is 'network locked'.
         */
        NetworkLocked(true),

        /**
         * The sim card is missing.
         */
        SimMissing(false),

        /**
         * The sim card is missing, and this is the device isn't provisioned, so we don't let
         * them get past the screen.
         */
        SimMissingLocked(false),

        /**
         * The sim card is PUK locked, meaning they've entered the wrong sim unlock code too many
         * times.
         */
        SimPukLocked(false),

        /**
         * The sim card is locked.
         */
        SimLocked(true);

        private final boolean mShowStatusLines;

        Status(boolean mShowStatusLines) {
            this.mShowStatusLines = mShowStatusLines;
        }

        /**
         * @return Whether the status lines (battery level and / or next alarm) are shown while
         *         in this state.  Mostly dictated by whether this is room for them.
         */
        public boolean showStatusLines() {
            return mShowStatusLines;
        }
    }

    enum FooterMode {
        Normal,
        ForgotLockPattern,
        VerifyUnlocked
    }

    private void updateFooter(FooterMode mode) {
        switch (mode) {
            case Normal:
                mFooterNormal.setVisibility(View.VISIBLE);
                mFooterForgotPattern.setVisibility(View.GONE);
                break;
            case ForgotLockPattern:
                mFooterNormal.setVisibility(View.GONE);
                mFooterForgotPattern.setVisibility(View.VISIBLE);
                mForgotPatternButton.setVisibility(View.VISIBLE);
                break;
            case VerifyUnlocked:
                mFooterNormal.setVisibility(View.GONE);
                mFooterForgotPattern.setVisibility(View.GONE);
        }
    }

    /**
     * @param context The context.
     * @param configuration
     * @param lockPatternUtils Used to lookup lock pattern settings.
     * @param updateMonitor Used to lookup state affecting keyguard.
     * @param callback Used to notify the manager when we're done, etc.
     * @param totalFailedAttempts The current number of failed attempts.
     * @param enableFallback True if a backup unlock option is available when the user has forgotten
     *        their pattern (e.g they have a google account so we can show them the account based
     *        backup option).
     */
    PatternUnlockScreen(Context context,
                 Configuration configuration, LockPatternUtils lockPatternUtils,
                 KeyguardUpdateMonitor updateMonitor,
                 KeyguardScreenCallback callback,
                 int totalFailedAttempts) {
        super(context);
        mLockPatternUtils = lockPatternUtils;
        mUpdateMonitor = updateMonitor;
        mCallback = callback;
        int CColours = mClockColor;
        mTotalFailedPatternAttempts = totalFailedAttempts;
        mFailedPatternAttemptsSinceLastTimeout =
            totalFailedAttempts % LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT;

        if (DEBUG) Log.d(TAG,
            "UnlockScreen() ctor: totalFailedAttempts="
                 + totalFailedAttempts + ", mFailedPat...="
                 + mFailedPatternAttemptsSinceLastTimeout
                 );

        mCreationOrientation = configuration.orientation;

        LayoutInflater inflater = LayoutInflater.from(context);
        if (mCreationOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            inflater.inflate(R.layout.keyguard_screen_unlock_portrait, this, true);
        } else {
            inflater.inflate(R.layout.keyguard_screen_unlock_landscape, this, true);
        }
        ViewGroup lockWallpaper = (ViewGroup) findViewById(R.id.pattern);
        LockScreen.setBackground(getContext(), lockWallpaper);

        mCarrier = (TextView) findViewById(R.id.carrier);
        mClock = (DigitalClock) findViewById(R.id.time);
        mTime = (TextView) findViewById(R.id.timeDisplay);
        mTime.setTextColor(CColours);
        mAmPm = (TextView) findViewById(R.id.am_pm);
        mAmPm.setTextColor(CColours);
        mDate = (TextView) findViewById(R.id.date);
        mDate.setTextColor(CColours);

        mDateFormatString = getContext().getString(R.string.full_wday_month_day_no_year);
        refreshTimeAndDateDisplay();

        mStatus1 = (TextView) findViewById(R.id.status1);
        mStatus1.setTextColor(CColours);
        mStatusSep = (TextView) findViewById(R.id.statusSep);
        mStatusSep.setTextColor(CColours);
        mStatus2 = (TextView) findViewById(R.id.status2);
        mStatus2.setTextColor(CColours);

        mLockPatternView = (LockPatternView) findViewById(R.id.lockPattern);

        mFooterNormal = (ViewGroup) findViewById(R.id.footerNormal);
        mFooterForgotPattern = (ViewGroup) findViewById(R.id.footerForgotPattern);

        // emergency call buttons
        final OnClickListener emergencyClick = new OnClickListener() {
            public void onClick(View v) {
                mCallback.takeEmergencyCallAction();
            }
        };

        mEmergencyAlone = (Button) findViewById(R.id.emergencyCallAlone);
        mEmergencyAlone.setFocusable(false); // touch only!
        mEmergencyAlone.setOnClickListener(emergencyClick);
        mEmergencyTogether = (Button) findViewById(R.id.emergencyCallTogether);
        mEmergencyTogether.setFocusable(false);
        mEmergencyTogether.setOnClickListener(emergencyClick);
        refreshEmergencyButtonText();

        mForgotPatternButton = (Button) findViewById(R.id.forgotPattern);
        mForgotPatternButton.setText(R.string.lockscreen_forgot_pattern_button_text);
        mForgotPatternButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                mCallback.forgotPattern(true);
            }
        });

        // make it so unhandled touch events within the unlock screen go to the
        // lock pattern view.
        setDefaultTouchRecepient(mLockPatternView);

        mLockPatternView.setSaveEnabled(false);
        mLockPatternView.setFocusable(false);
        mLockPatternView.setOnPatternListener(new UnlockPatternListener());

        mLockPatternView.setVisibleDots(mLockPatternUtils.isVisibleDotsEnabled());
        mLockPatternView.setShowErrorPath(mLockPatternUtils.isShowErrorPath());
        mLockPatternView.setIncorrectDelay(mLockPatternUtils.getIncorrectDelay());

        // stealth mode will be the same for the life of this screen
        mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled());

        // vibrate mode will be the same for the life of this screen
        mLockPatternView.setTactileFeedbackEnabled(mLockPatternUtils.isTactileFeedbackEnabled());
 
        mLockPatternView.setLockPatternSize(mLockPatternUtils.getLockPatternSize());

        // assume normal footer mode for now
        updateFooter(FooterMode.Normal);

        mUpdateMonitor.registerInfoCallback(this);
        mUpdateMonitor.registerSimStateCallback(this);
        setFocusableInTouchMode(true);

        mLockPatternUtils.updateLockPatternSize();

        // Required to get Marquee to work.
        mCarrier.setSelected(true);
        mCarrier.setTextColor(mCarrierColor);

        resetStatusInfo(updateMonitor);

        int widgetLayout = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCKSCREEN_WIDGETS_LAYOUT, 0);

        switch (widgetLayout) {
            case 2:
                centerWidgets();
                break;
            case 3:
                alignWidgetsToRight();
                break;
        }
    }

    private void centerWidgets() {
        if (mCreationOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            RelativeLayout.LayoutParams layoutParams =
                    (RelativeLayout.LayoutParams) mCarrier.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.addRule(RelativeLayout.RIGHT_OF, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            mCarrier.setLayoutParams(layoutParams);
            mCarrier.setGravity(Gravity.CENTER_HORIZONTAL);
            layoutParams = (RelativeLayout.LayoutParams) mDate.getLayoutParams();
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, 1);
            mDate.setLayoutParams(layoutParams);
            layoutParams = (RelativeLayout.LayoutParams) mClock.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, 1);
            layoutParams.addRule(RelativeLayout.BELOW, R.id.carrier);
            mClock.setLayoutParams(layoutParams);
        } else {
            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) mCarrier.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mCarrier.setLayoutParams(layoutParams);
            mCarrier.setGravity(Gravity.CENTER_HORIZONTAL);
            layoutParams = (LinearLayout.LayoutParams) mDate.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mDate.setLayoutParams(layoutParams);
            mDate.setGravity(Gravity.CENTER_HORIZONTAL);
            layoutParams = (LinearLayout.LayoutParams) mClock.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mClock.setGravity(Gravity.CENTER_HORIZONTAL);
            mClock.setLayoutParams(layoutParams);
        }
    }

    private void alignWidgetsToRight() {
        if (mCreationOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            RelativeLayout.LayoutParams layoutParams =
                    (RelativeLayout.LayoutParams) mCarrier.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.addRule(RelativeLayout.RIGHT_OF, 0);
            layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.time);
            mCarrier.setLayoutParams(layoutParams);
            mCarrier.setGravity(Gravity.LEFT);
            layoutParams = (RelativeLayout.LayoutParams) mDate.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
            mDate.setLayoutParams(layoutParams);
            layoutParams = (RelativeLayout.LayoutParams) mClock.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
            mClock.setLayoutParams(layoutParams);
        } else {
            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) mCarrier.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mCarrier.setLayoutParams(layoutParams);
            mCarrier.setGravity(Gravity.RIGHT);
            layoutParams = (LinearLayout.LayoutParams) mDate.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mDate.setLayoutParams(layoutParams);
            mDate.setGravity(Gravity.RIGHT);
            layoutParams = (LinearLayout.LayoutParams) mClock.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mClock.setGravity(Gravity.RIGHT);
            mClock.setLayoutParams(layoutParams);
        }
    }

    private void refreshEmergencyButtonText() {
        mLockPatternUtils.updateEmergencyCallButtonState(mEmergencyAlone);
        mLockPatternUtils.updateEmergencyCallButtonState(mEmergencyTogether);
    }

    public void setEnableFallback(boolean state) {
        if (DEBUG) Log.d(TAG, "setEnableFallback(" + state + ")");
        mEnableFallback = state;
    }

    private void resetStatusInfo(KeyguardUpdateMonitor updateMonitor) {
        mInstructions = null;
        mShowingBatteryInfo = updateMonitor.shouldShowBatteryInfo();
        mPluggedIn = updateMonitor.isDevicePluggedIn();
        mBatteryLevel = updateMonitor.getBatteryLevel();
        mNextAlarm = mLockPatternUtils.getNextAlarm();
        mStatus = getCurrentStatus(updateMonitor.getSimState());
        updateLayout(mStatus);
        updateStatusLines();
        refreshBatteryString();
    }

    private void refreshBatteryString() {
        if (!mShowingBatteryInfo && !mLockAlwaysBattery) {
            mCharging = null;
            return;
        }

        if (mPluggedIn) {
            if (mUpdateMonitor.isDeviceCharged()) {
                mCharging = getContext().getString(R.string.lockscreen_charged, mBatteryLevel);
            } else {
                mCharging = getContext().getString(R.string.lockscreen_plugged_in, mBatteryLevel);
            }
        } else {
            if (mBatteryLevel <= 20) {
                mCharging = getContext().getString(R.string.lockscreen_low_battery, mBatteryLevel);
            } else {
                mCharging = getContext().getString(R.string.lockscreen_discharging, mBatteryLevel);
            }
        }
    }

    private void updateStatusLines() {
        if (mInstructions != null) {
            // instructions only
            mStatus1.setText(mInstructions);
            if (TextUtils.isEmpty(mInstructions)) {
                mStatus1.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            } else {
                mStatus1.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_lock_idle_lock, 0, 0, 0);
            }

            if (mInstructions.equals(getContext().getString(R.string.lockscreen_pattern_wrong))
                    && !mLockPatternUtils.isShowUnlockErrMsg()) {
                mStatus1.setVisibility(View.GONE);
            } else {
                mStatus1.setVisibility(View.VISIBLE);
            }

            mStatusSep.setVisibility(View.GONE);
            mStatus2.setVisibility(View.GONE);
        } else if ((mShowingBatteryInfo || mLockAlwaysBattery) && mNextAlarm == null) {
            // battery only
            if (mPluggedIn) {
                mStatus1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_idle_charging, 0, 0, 0);
            } else if (mBatteryLevel <= 20){
                mStatus1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_idle_low_battery, 0, 0, 0);
            } else {
                mStatus1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_idle_discharging, 0, 0, 0);
            }
            if (mCharging != null) {
                mStatus1.setText(mCharging);
            }
            mStatus1.setVisibility(View.VISIBLE);
            mStatusSep.setVisibility(View.GONE);
            mStatus2.setVisibility(View.GONE);

        } else if (mNextAlarm != null && !(mShowingBatteryInfo || mLockAlwaysBattery)) {
            // alarm only
            mStatus1.setText(mNextAlarm);
            mStatus1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_idle_alarm, 0, 0, 0);

            mStatus1.setVisibility(View.VISIBLE);
            mStatusSep.setVisibility(View.GONE);
            mStatus2.setVisibility(View.GONE);
        } else if (mNextAlarm != null && (mShowingBatteryInfo || mLockAlwaysBattery)) {
            // both battery and next alarm
            mStatus1.setText(mNextAlarm);
            mStatusSep.setText("|");
            mStatus1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_idle_alarm, 0, 0, 0);
            if (mPluggedIn) {
                mStatus2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_idle_charging, 0, 0, 0);
            } else if (mBatteryLevel <= 20){
                mStatus2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_idle_low_battery, 0, 0, 0);
            } else {
                mStatus2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_idle_discharging, 0, 0, 0);
            }
            if (mCharging != null) {
                mStatus2.setText(mCharging);
            }
            if (mLockPatternUtils.isShowUnlockMsg()) {
                mStatus1.setVisibility(View.VISIBLE);
            } else {
                mStatus1.setVisibility(View.GONE);
            }

            mStatusSep.setVisibility(View.VISIBLE);
            mStatus2.setVisibility(View.VISIBLE);
        } else {
            // nothing specific to show; show general instructions
            mStatus1.setText(R.string.lockscreen_pattern_instructions);
            mStatus1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_idle_lock, 0, 0, 0);

            mStatus1.setVisibility(View.VISIBLE);
            mStatusSep.setVisibility(View.GONE);
            mStatus2.setVisibility(View.GONE);
        }
    }

    static CharSequence getCarrierString(CharSequence telephonyPlmn, CharSequence telephonySpn) {
        return getCarrierString(telephonyPlmn, telephonySpn, CARRIER_TYPE_DEFAULT, "");
    }

    static CharSequence getCarrierString(CharSequence telephonyPlmn, CharSequence telephonySpn,
            int carrierLabelType, String carrierLabelCustom) {
        switch (carrierLabelType) {
            default:
            case CARRIER_TYPE_DEFAULT:
                if (telephonyPlmn != null && TextUtils.isEmpty(telephonySpn)) {
                    return telephonyPlmn;
                } else if (telephonySpn != null && TextUtils.isEmpty(telephonyPlmn)) {
                    return telephonySpn;
                } else if (telephonyPlmn != null && telephonySpn != null) {
                    return telephonyPlmn + "|" + telephonySpn;
                }
                return "";
            case CARRIER_TYPE_SPN:
                if (telephonySpn != null) {
                    return telephonySpn;
                 }
                 break;
            case CARRIER_TYPE_PLMN:
                if (telephonyPlmn != null) {
                    return telephonyPlmn;
                }
                break;
            case CARRIER_TYPE_CUSTOM:
                // If the custom carrier label contains any "$x" items then we must
                // replace those with the proper text.
                //  - $n = new line
                //  - $d = default carrier text
                //  - $p = plmn carrier text
                //  - $s = spn carrier text
                //
                // First we create the default carrier text in case we need it.
                StringBuilder defaultStr = new StringBuilder();
                if (telephonyPlmn != null && TextUtils.isEmpty(telephonySpn)) {
                    defaultStr.append(telephonyPlmn.toString());
                } else if (telephonySpn != null && TextUtils.isEmpty(telephonyPlmn)) {
                    defaultStr.append(telephonySpn.toString());
                } else if (telephonyPlmn != null && telephonySpn != null) {
                    defaultStr.append(telephonyPlmn.toString() + "|" + telephonySpn.toString());
                }

                String customStr = carrierLabelCustom;
                customStr = customStr.replaceAll("\\$n", "\n");
                customStr = customStr.replaceAll("\\$d", (defaultStr != null) ? defaultStr.toString() : "");
                customStr = customStr.replaceAll("\\$p", (telephonyPlmn != null) ? telephonyPlmn.toString() : "");
                customStr = customStr.replaceAll("\\$s", (telephonySpn != null) ? telephonySpn.toString() : "");
                return customStr;
         }
         return "";
     }

    private void refreshTimeAndDateDisplay() {
        mDate.setText(DateFormat.format(mDateFormatString, new Date()));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            event.startTracking();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        int ls = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCKSCREEN_STYLE_PREF, 6);
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            if (ls >= 6) {
                HoneycombLockscreen.handleHomeLongPress(getContext());
            } else {
                LockScreen.handleHomeLongPress(getContext());
            }
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // as long as the user is entering a pattern (i.e sending a touch
        // event that was handled by this screen), keep poking the
        // wake lock so that the screen will stay on.
        final boolean result = super.dispatchTouchEvent(ev);
        if (result &&
                ((SystemClock.elapsedRealtime() - mLastPokeTime)
                        >  (UNLOCK_PATTERN_WAKE_INTERVAL_MS - 100))) {
            mLastPokeTime = SystemClock.elapsedRealtime();
        }
        return result;
    }


    // ---------- InfoCallback

    /** {@inheritDoc} */
    public void onRefreshBatteryInfo(boolean showBatteryInfo, boolean pluggedIn, int batteryLevel) {
        mShowingBatteryInfo = showBatteryInfo;
        mPluggedIn = pluggedIn;
        mBatteryLevel = batteryLevel;
        refreshBatteryString();
        updateStatusLines();
    }

    /** {@inheritDoc} */
    public void onTimeChanged() {
        refreshTimeAndDateDisplay();
    }

    /** {@inheritDoc} */
    public void onRefreshCarrierInfo(CharSequence plmn, CharSequence spn) {
        if (DBG) Log.d(TAG, "onRefreshCarrierInfo(" + plmn + ", " + spn + ")");
        updateLayout(mStatus);
    }

    private boolean isAirplaneModeOn() {
      return (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1);
    }

    /**
     * Determine the current status of the lock screen given the sim state and other stuff.
     */
    private Status getCurrentStatus(IccCard.State simState) {
        boolean missingAndNotProvisioned = (!mUpdateMonitor.isDeviceProvisioned()
                && simState == IccCard.State.ABSENT);
        if (missingAndNotProvisioned) {
            return Status.SimMissingLocked;
        }

        boolean presentButNotAvailable = isAirplaneModeOn();
        if (presentButNotAvailable) {
            return Status.Normal;
        }

        switch (simState) {
            case ABSENT:
                return Status.SimMissing;
            case NETWORK_LOCKED:
                return Status.SimMissingLocked;
            case NOT_READY:
                return Status.SimMissing;
            case PIN_REQUIRED:
                return Status.SimLocked;
            case PUK_REQUIRED:
                return Status.SimPukLocked;
            case READY:
                return Status.Normal;
            case UNKNOWN:
                return Status.SimMissing;
        }
        return Status.SimMissing;
    }

    /**
     * Update the layout to match the current status.
     */
    private void updateLayout(Status status) {
        if (DBG) Log.d(TAG, "updateLayout: status=" + status);
        String realPlmn = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ALPHA);
        String plmn = (String) mUpdateMonitor.getTelephonyPlmn();
        String spn = (String) mUpdateMonitor.getTelephonySpn();

        switch (status) {
            case Normal:
                // text
                if (plmn == null || plmn.equals(realPlmn)) {
                    mCarrier.setText(getCarrierString(
                            plmn, spn, mCarrierLabelType, mCarrierLabelCustom));
                } else {
                    mCarrier.setText(getCarrierString(plmn, spn));
                }
                break;
            case NetworkLocked:
                // The carrier string shows both sim card status (i.e. No Sim Card) and
                // carrier's name and/or "Emergency Calls Only" status
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_network_locked_message)));
                break;
            case SimMissing:
                // text
                mCarrier.setText(R.string.lockscreen_missing_sim_message_short);
                // do not need to show the e-call button; user may unlock
                break;
            case SimMissingLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_missing_sim_message_short)));
                break;
            case SimLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_sim_locked_message)));
                break;
            case SimPukLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_sim_puk_locked_message)));
                break;
        }
    }

    /** {@inheritDoc} */
    public void onRingerModeChanged(int state) {
        // not currently used
    }

    // ---------- SimStateCallback

    /** {@inheritDoc} */
    public void onSimStateChanged(IccCard.State simState) {
        mStatus = getCurrentStatus(simState);
        updateLayout(mStatus);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
            Log.v(TAG, "***** PATTERN ATTACHED TO WINDOW");
            Log.v(TAG, "Cur orient=" + mCreationOrientation
                    + ", new config=" + getResources().getConfiguration());
        }
        if (getResources().getConfiguration().orientation != mCreationOrientation) {
            mCallback.recreateMe(getResources().getConfiguration());
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
            Log.v(TAG, "***** PATTERN CONFIGURATION CHANGED");
            Log.v(TAG, "Cur orient=" + mCreationOrientation
                    + ", new config=" + getResources().getConfiguration());
        }
        if (newConfig.orientation != mCreationOrientation) {
            mCallback.recreateMe(newConfig);
        }
    }

    /** {@inheritDoc} */
    public void onKeyboardChange(boolean isKeyboardOpen) {}

    /** {@inheritDoc} */
    public boolean needsInput() {
        return false;
    }

    /** {@inheritDoc} */
    public void onPause() {
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
    }

    /** {@inheritDoc} */
    public void onResume() {
        // reset header
        resetStatusInfo(mUpdateMonitor);

        // reset lock pattern
        mLockPatternView.enableInput();
        mLockPatternView.setEnabled(true);
        mLockPatternView.clearPattern();

        // show "forgot pattern?" button if we have an alternate authentication method
        mForgotPatternButton.setVisibility(mCallback.doesFallbackUnlockScreenExist()
                ? View.VISIBLE : View.INVISIBLE);

        // if the user is currently locked out, enforce it.
        long deadline = mLockPatternUtils.getLockoutAttemptDeadline();
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        }

        // the footer depends on how many total attempts the user has failed
        if (mCallback.isVerifyUnlockOnly()) {
            updateFooter(FooterMode.VerifyUnlocked);
        } else if (mEnableFallback &&
                (mTotalFailedPatternAttempts >= LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT)) {
            updateFooter(FooterMode.ForgotLockPattern);
        } else {
            updateFooter(FooterMode.Normal);
        }

        refreshEmergencyButtonText();
    }

    /** {@inheritDoc} */
    public void cleanUp() {
        mUpdateMonitor.removeCallback(this);
        mLockPatternUtils = null;
        mUpdateMonitor = null;
        mCallback = null;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            // when timeout dialog closes we want to update our state
            onResume();
        }
    }

    private class UnlockPatternListener
            implements LockPatternView.OnPatternListener {

        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mCancelPatternRunnable);
        }

        public void onPatternCleared() {
        }

        public void onPatternCellAdded(List<Cell> pattern) {
            // To guard against accidental poking of the wakelock, look for
            // the user actually trying to draw a pattern of some minimal length.
            if (pattern.size() > MIN_PATTERN_BEFORE_POKE_WAKELOCK) {
                mCallback.pokeWakelock(UNLOCK_PATTERN_WAKE_INTERVAL_MS);
            } else {
                // Give just a little extra time if they hit one of the first few dots
                mCallback.pokeWakelock(UNLOCK_PATTERN_WAKE_INTERVAL_FIRST_DOTS_MS);
            }
        }

        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
            mLockPatternUtils.updateLockPatternSize();
            if (mLockPatternUtils.checkPattern(pattern)) {
                mLockPatternView
                        .setDisplayMode(LockPatternView.DisplayMode.Correct);
                mInstructions = "";
                updateStatusLines();
                mCallback.keyguardDone(true);
                mCallback.reportSuccessfulUnlockAttempt();
            } else {
                if (pattern.size() > MIN_PATTERN_BEFORE_POKE_WAKELOCK) {
                    mCallback.pokeWakelock(UNLOCK_PATTERN_WAKE_INTERVAL_MS);
                }
                mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                if (pattern.size() >= LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                    mTotalFailedPatternAttempts++;
                    mFailedPatternAttemptsSinceLastTimeout++;
                    mCallback.reportFailedUnlockAttempt();
                }
                if (mFailedPatternAttemptsSinceLastTimeout >= LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline();
                    handleAttemptLockout(deadline);
                } else {
                    // TODO mUnlockIcon.setVisibility(View.VISIBLE);
                    mInstructions = getContext().getString(R.string.lockscreen_pattern_wrong);
                    updateStatusLines();
                    mLockPatternView.postDelayed(
                            mCancelPatternRunnable,
                            mLockPatternView.getIncorrectDelay());
                }
            }
        }
    }

    private void handleAttemptLockout(long elapsedRealtimeDeadline) {
        mLockPatternView.clearPattern();
        mLockPatternView.setEnabled(false);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                mInstructions = getContext().getString(
                        R.string.lockscreen_too_many_failed_attempts_countdown,
                        secondsRemaining);
                updateStatusLines();
            }

            @Override
            public void onFinish() {
                mLockPatternView.setEnabled(true);
                mInstructions = getContext().getString(R.string.lockscreen_pattern_instructions);
                updateStatusLines();
                // TODO mUnlockIcon.setVisibility(View.VISIBLE);
                mFailedPatternAttemptsSinceLastTimeout = 0;
                if (mEnableFallback) {
                    updateFooter(FooterMode.ForgotLockPattern);
                } else {
                    updateFooter(FooterMode.Normal);
                }
            }
        }.start();
    }

    public void onPhoneStateChanged(String newState) {
        refreshEmergencyButtonText();
    }

    public void onMusicChanged() {

    }
}
