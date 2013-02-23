package com.android.systemui.statusbar.quicksettings.quicktile;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Power;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.preference.MultiSelectListPreference;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.policy.BrightnessController;
import com.android.systemui.statusbar.policy.BrightnessController.BrightnessStateChangeCallback;
import com.android.systemui.statusbar.policy.ToggleSlider;

public class BrightnessTile extends QuickSettingsTile implements BrightnessStateChangeCallback {

    /**
     * Minimum and maximum brightnesses. Don't go to 0 since that makes the
     * display unusable
     */
    private static final int MIN_BACKLIGHT = Power.BRIGHTNESS_DIM + 8;
    private static final int MAX_BACKLIGHT = Power.BRIGHTNESS_ON;

    // Auto-backlight level
    private static final int AUTO_BACKLIGHT = -1;
    // Mid-range brightness values + thresholds
    private static final int LOW_BACKLIGHT = (int) (MAX_BACKLIGHT * 0.3f);
    private static final int LOWMID_BACKLIGHT = (int) (MAX_BACKLIGHT * 0.4f);
    private static final int MID_BACKLIGHT = (int) (MAX_BACKLIGHT * 0.5f);
    private static final int HIGH_BACKLIGHT = (int) (MAX_BACKLIGHT * 0.75f);

    // Defaults for now. MIN_BACKLIGHT will be replaced later
    private static final int[] BACKLIGHTS = new int[] {
            AUTO_BACKLIGHT, MIN_BACKLIGHT, LOW_BACKLIGHT, LOWMID_BACKLIGHT, MID_BACKLIGHT, HIGH_BACKLIGHT,
            MAX_BACKLIGHT
    };

    private boolean mAutoBrightnessSupported = false;
    private boolean mAutoBrightness = false;
    private int mCurrentBrightness;
    private int mCurrentBacklightIndex = 0;

    private int[] mBacklightValues = new int[] {
            0, 1, 2, 3, 4, 5, 6
    };

    private final int mBrightnessDialogLongTimeout;
    private final int mBrightnessDialogShortTimeout;
    private Dialog mBrightnessDialog;
    private BrightnessController mBrightnessController;
    private final Handler mHandler = new Handler();

    public BrightnessTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, final QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mAutoBrightnessSupported = context.getResources().getBoolean(
                    com.android.internal.R.bool.config_automatic_brightness_available);
        updateSettings(context.getContentResolver());
        updateState();

        mBrightnessDialogLongTimeout = mContext.getResources().getInteger(R.integer.quick_settings_brightness_dialog_long_timeout);
        mBrightnessDialogShortTimeout = mContext.getResources().getInteger(R.integer.quick_settings_brightness_dialog_short_timeout);

        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleState();
                applyBrightChanges();
            }
        };
        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showBrightnessDialog();
                startCollapseActivity();
                return true;
            }
        };
        qsc.registerAction(Intent.ACTION_CONFIGURATION_CHANGED, this);
        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), this);
        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), this);
        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.LIGHT_SENSOR_CUSTOM), this);
        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.LIGHT_SCREEN_DIM), this);
        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.EXPANDED_BRIGHTNESS_MODE), this);
    }

    private void showBrightnessDialog() {
        if (mBrightnessDialog == null) {
            mBrightnessDialog = new Dialog(mContext);
            mBrightnessDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mBrightnessDialog.setContentView(R.layout.quick_settings_brightness_dialog);
            mBrightnessDialog.setCanceledOnTouchOutside(true);

            mBrightnessController = new BrightnessController(mContext,
                    (ImageView) mBrightnessDialog.findViewById(R.id.brightness_icon),
                    (ToggleSlider) mBrightnessDialog.findViewById(R.id.brightness_slider));
            mBrightnessDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mBrightnessController = null;
                }
            });

            mBrightnessDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mBrightnessDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        if (!mBrightnessDialog.isShowing()) {
            mBrightnessDialog.show();
            dismissBrightnessDialog(mBrightnessDialogLongTimeout);
        }
    }

    private void dismissBrightnessDialog(int timeout) {
        if (mBrightnessDialog != null) {
            mHandler.removeCallbacks(mDismissBrightnessDialogRunnable);
            mHandler.postDelayed(mDismissBrightnessDialogRunnable, timeout);
        }
    }

    private final Runnable mDismissBrightnessDialogRunnable = new Runnable() {
        @Override
        public void run() {
            if (mBrightnessDialog != null && mBrightnessDialog.isShowing()) {
                mBrightnessDialog.dismiss();
            }
        };
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        applyBrightChanges();
    }

    @Override
    public void onBrightnessLevelChanged() {
        applyBrightChanges();
    }

    private void applyBrightChanges() {
        updateState();
        updateQuickSettings();
    }

    protected void updateState() {
        updateSettings(mContext.getContentResolver());
        if (mAutoBrightness) {
            mDrawable = R.drawable.ic_qs_brightness_auto_on;
            mLabel = mContext.getString(R.string.quick_settings_brightness_dialog_auto_brightness_label);
        } else if (mCurrentBrightness <= LOW_BACKLIGHT) {
            mDrawable = R.drawable.ic_qs_brightness_auto_off;
            mLabel = mContext.getString(R.string.quick_settings_brightness_labellow);
        } else if (mCurrentBrightness <= MID_BACKLIGHT) {
            mDrawable = R.drawable.ic_qs_brightness_auto_off;
            mLabel = mContext.getString(R.string.quick_settings_brightness_labelmid);
        } else {
            mDrawable = R.drawable.ic_qs_brightness_auto_off;
            mLabel = mContext.getString(R.string.quick_settings_brightness_label);
        }
    }

    protected void toggleState() {
        PowerManager power = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        ContentResolver resolver = mContext.getContentResolver();

        mCurrentBacklightIndex++;
        if (mCurrentBacklightIndex > mBacklightValues.length - 1) {
            mCurrentBacklightIndex = 0;
        }

        int backlightIndex = mBacklightValues[mCurrentBacklightIndex];
        int brightness = BACKLIGHTS[backlightIndex];

        if (brightness == AUTO_BACKLIGHT) {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        } else {
            if (mAutoBrightnessSupported) {
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
            power.setBacklightBrightness(brightness);
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
        }
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
            updateSettings(resolver);
            applyBrightChanges();
    }

    private void updateSettings(ContentResolver resolver) {
        boolean lightSensorCustom = (Settings.System.getInt(resolver,
                Settings.System.LIGHT_SENSOR_CUSTOM, 0) != 0);
        if (lightSensorCustom) {
            BACKLIGHTS[1] = Settings.System.getInt(resolver, Settings.System.LIGHT_SCREEN_DIM,
                    MIN_BACKLIGHT);
        } else {
            BACKLIGHTS[1] = MIN_BACKLIGHT;
        }

        String[] modes = MultiSelectListPreference.parseStoredValue(Settings.System.getString(
                resolver, Settings.System.EXPANDED_BRIGHTNESS_MODE));
        if (modes == null || modes.length == 0) {
            mBacklightValues = new int[] {
                    0, 1, 2, 3, 4, 5, 6
            };
        } else {
            mBacklightValues = new int[modes.length];
            for (int i = 0; i < modes.length; i++) {
                mBacklightValues[i] = Integer.valueOf(modes[i]);
            }
        }

        mAutoBrightness = (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                0) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        if (mAutoBrightness) {
            mCurrentBrightness = AUTO_BACKLIGHT;
        } else {
            mCurrentBrightness = Settings.System.getInt(resolver,
                    Settings.System.SCREEN_BRIGHTNESS, -1);
            for (int i = 0; i < BACKLIGHTS.length; i++) {
                if (mCurrentBrightness == BACKLIGHTS[i]) {
                    mCurrentBacklightIndex = i;
                    break;
                }
            }
        }
    }
}
