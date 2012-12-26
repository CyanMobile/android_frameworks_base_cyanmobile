package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.MultiSelectListPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;

public class RingerModeTile extends QuickSettingsTile {

    private static final int VIBRATE_DURATION = 250; // 0.25s

    // Define the available ringer modes
    private final Ringer mSilentRinger = new Ringer(AudioManager.RINGER_MODE_SILENT, false, 
            AudioManager.VIBRATE_SETTING_OFF, false);
    private final Ringer mVibrateRinger = new Ringer(AudioManager.RINGER_MODE_VIBRATE, true,
            AudioManager.VIBRATE_SETTING_ONLY_SILENT, true);
    private final Ringer mSoundRinger = new Ringer(AudioManager.RINGER_MODE_NORMAL, true,
            AudioManager.VIBRATE_SETTING_ONLY_SILENT, false);
    private final Ringer mSoundVibrateRinger = new Ringer(AudioManager.RINGER_MODE_NORMAL, true,
            AudioManager.VIBRATE_SETTING_ON, true);

    private final Ringer[] mRingers = new Ringer[] {
            mSilentRinger, mVibrateRinger, mSoundRinger, mSoundVibrateRinger
    };

    private int mRingersIndex = 2;
    private int[] mRingerValues = new int[] {
            0, 1, 2, 3
    };
    private int mRingerValuesIndex = 2;

    private AudioManager mAudioManager;
    private Handler mHandler;
    protected Vibrator mVibrator;

    public RingerModeTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler();

        // Load the available ringer modes
        updateSettings(mContext.getContentResolver());

        // Make sure we show the initial state correctly
        updateState();

        // Tile actions
        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleState();
                applyVibrationChanges();
            }
        };

        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity(android.provider.Settings.ACTION_SOUND_SETTINGS);
                return true;
            }
        };
        qsc.registerAction(AudioManager.RINGER_MODE_CHANGED_ACTION, this);
        qsc.registerAction(AudioManager.VIBRATE_SETTING_CHANGED_ACTION, this);
        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.EXPANDED_RING_MODE)
                , this);
        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.VIBRATE_IN_SILENT)
                , this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        applyVibrationChanges();
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        updateSettings(mContext.getContentResolver());
        applyVibrationChanges();
    }

    private void applyVibrationChanges(){
        updateState();
        updateQuickSettings();
    }

    protected void updateState() {
        // The title does not change
        mLabel = mContext.getString(R.string.quick_settings_ringer_normal);

        // The icon will change depending on index
        findCurrentState();
        switch (mRingersIndex) {
            case 0:
                mDrawable = R.drawable.stat_silent;
                break;
            case 1:
                mDrawable = R.drawable.stat_vibrate_off;
                break;
            case 2:
                mDrawable = R.drawable.stat_ring_on;
                break;
            case 3:
                mDrawable = R.drawable.stat_ring_vibrate_on;
                break;
        }

        for (int i = 0; i < mRingerValues.length; i++) {
            if (mRingersIndex == mRingerValues[i]) {
                mRingerValuesIndex = i;
                break;
            }
        }
    }

    protected void toggleState() {
        mRingerValuesIndex++;
        if (mRingerValuesIndex > mRingerValues.length - 1) {
            mRingerValuesIndex = 0;
        }

        mRingersIndex = mRingerValues[mRingerValuesIndex];
        if (mRingersIndex > mRingers.length - 1) {
            mRingersIndex = 0;
        }

        Ringer ringer = mRingers[mRingersIndex];
        ringer.execute(mContext);
    }

    private void updateSettings(ContentResolver resolver) {
        String[] modes = MultiSelectListPreference.parseStoredValue(Settings.System.getString(
                resolver, Settings.System.EXPANDED_RING_MODE));
        if (modes == null || modes.length == 0) {
            mRingerValues = new int[] {
                    0, 1, 2, 3
            };
        } else {
            mRingerValues = new int[modes.length];
            for (int i = 0; i < modes.length; i++) {
                mRingerValues[i] = Integer.valueOf(modes[i]);
            }
        }
    }

    private void findCurrentState() {
        ensureAudioManager(mContext);
        ContentResolver resolver = mContext.getContentResolver();
        boolean vibrateWhenRinging = Settings.System.getInt(resolver,
                Settings.System.VIBRATE_IN_SILENT, 0) == 1;
        int vibrateSetting = mAudioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
        int ringerMode = mAudioManager.getRingerMode();
        // Sometimes the setting don't quite match up to the states we've defined.
        // In that case, override the reported settings to get us "close" to the
        // defined settings. This bit is a little ugly but oh well.
        if (!vibrateWhenRinging && ringerMode == AudioManager.RINGER_MODE_SILENT) {
            vibrateSetting = AudioManager.VIBRATE_SETTING_OFF; // match Silent ringer
        } else if (!vibrateWhenRinging && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            vibrateWhenRinging = true; // match either Sound or SoundVibrate ringer
            if (vibrateSetting == AudioManager.VIBRATE_SETTING_OFF) {
                vibrateSetting = AudioManager.VIBRATE_SETTING_ONLY_SILENT; // match Sound ringer
            }
        } else if (vibrateWhenRinging && ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            vibrateSetting = AudioManager.VIBRATE_SETTING_ONLY_SILENT; // match Vibrate ringer 
        }

        Ringer ringer = new Ringer(ringerMode, vibrateWhenRinging, vibrateSetting, false);
        for (int i = 0; i < mRingers.length; i++) {
            if (mRingers[i].equals(ringer)) {
                mRingersIndex = i;
                break;
            }
        }
    }

    private void ensureAudioManager(Context context) {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
    }

    private class Ringer {
        final int mRingerMode;
        final boolean mVibrateWhenRinging;
        final int mVibrateSetting;
        final boolean mDoHapticFeedback;

        Ringer(int ringerMode, boolean vibrateWhenRinging, int vibrateSetting, boolean doHapticFeedback) {
            mRingerMode = ringerMode;
            mVibrateWhenRinging = vibrateWhenRinging;
            mVibrateSetting = vibrateSetting;
            mDoHapticFeedback = doHapticFeedback;
        }

        void execute(Context context) {
            // If we are setting a vibrating state, vibrate to indicate it
            if (mDoHapticFeedback && mVibrateWhenRinging) {
                mVibrator.vibrate(VIBRATE_DURATION);
            }

            // Set the desired state
            ContentResolver resolver = context.getContentResolver();
            Settings.System.putInt(resolver, Settings.System.VIBRATE_IN_SILENT,
                    (mVibrateWhenRinging ? 1 : 0));
            ensureAudioManager(context);
            mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, mVibrateSetting);
            mAudioManager.setRingerMode(mRingerMode);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o.getClass() != getClass()) {
                return false;
            }

            Ringer r = (Ringer) o;
            return r.mVibrateWhenRinging == mVibrateWhenRinging
                    && r.mRingerMode == mRingerMode;
        }
    }
}
