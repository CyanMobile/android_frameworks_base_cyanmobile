package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.RemoteException;
import android.os.Power;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.preference.MultiSelectListPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;

public class BrightnessTile extends QuickSettingsTile {

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

    public BrightnessTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, final QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mAutoBrightnessSupported = context.getResources().getBoolean(
                    com.android.internal.R.bool.config_automatic_brightness_available);
        updateSettings(context.getContentResolver());
        updateState();

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
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.settings", "com.android.settings.DisplaySettings");
                startSettingsActivity(intent);
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

    @Override
    public void onReceive(Context context, Intent intent) {
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
