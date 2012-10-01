/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.view;

import com.android.internal.R;

import android.app.Dialog;
import android.bluetooth.HeadsetBase;
import android.content.DialogInterface.OnDismissListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioService;
import android.media.AudioSystem;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Config;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.HashMap;

/**
 * Handle the volume up and down keys.
 *
 * This code really should be moved elsewhere.
 *
 * @hide
 */
public class VolumePanel extends Handler implements OnSeekBarChangeListener, View.OnClickListener
{
    private static final String TAG = "VolumePanel";
    private static boolean LOGD = false;

    /**
     * The delay before playing a sound. This small period exists so the user
     * can press another key (non-volume keys, too) to have it NOT be audible.
     * <p>
     * PhoneWindow will implement this part.
     */
    public static final int PLAY_SOUND_DELAY = 300;

    /**
     * The delay before vibrating. This small period exists so if the user is
     * moving to silent mode, it will not emit a short vibrate (it normally
     * would since vibrate is between normal mode and silent mode using hardware
     * keys).
     */
    public static final int VIBRATE_DELAY = 300;

    private static final int VIBRATE_DURATION = 300;
    private static final int BEEP_DURATION = 150;
    private static final int MAX_VOLUME = 100;
    private static final int FREE_DELAY = 10000;
    private static final int TIMEOUT_DELAY = 3000;

    private static final int MSG_VOLUME_CHANGED = 0;
    private static final int MSG_FREE_RESOURCES = 1;
    private static final int MSG_PLAY_SOUND = 2;
    private static final int MSG_STOP_SOUNDS = 3;
    private static final int MSG_VIBRATE = 4;
    private static final int MSG_TIMEOUT = 5;
    private static final int MSG_RINGER_MODE_CHANGED = 6;

    protected Context mContext;
    private AudioManager mAudioManager;
    protected AudioService mAudioService;
    private boolean mRingIsSilent;

    private final Dialog mDialog;
    private final View mView;

    private final ViewGroup mSliderGroup;
    private final View mMoreButton;

    // Synchronize when accessing this
    private ToneGenerator mToneGenerators[];
    private Vibrator mVibrator;

    private int mActiveStreamType = -1;
    private HashMap<Integer,StreamControl> mStreamControls;

    private static final int [] STREAM_TYPES = {
        AudioManager.STREAM_RING,
        AudioManager.STREAM_VOICE_CALL,
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_ALARM,
        AudioManager.STREAM_NOTIFICATION
    };

    private static final int [] STREAM_ICONS_NORMAL = {
        R.drawable.ic_audio_vol,
        R.drawable.ic_audio_ring_notif,
        R.drawable.ic_audio_vol,
        R.drawable.ic_audio_alarm,
        R.drawable.ic_audio_notification,
    };

    private static final int [] STREAM_ICONS_MUTED = {
        R.drawable.ic_audio_vol_mute,
        R.drawable.ic_audio_phone,
        R.drawable.ic_audio_vol_mute,
        R.drawable.ic_audio_alarm_mute,
        R.drawable.ic_audio_notification_mute,
    };

    private class StreamControl {
        int streamType;
        ViewGroup group;
        ImageView icon;
        SeekBar seekbarView;
        int iconRes;
        int iconMuteRes;
    }

    public VolumePanel(final Context context, AudioService volumeService) {
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAudioService = volumeService;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mView = inflater.inflate(R.layout.volume_adjust, null);
        mView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                resetTimeout();
                return true;
            }
        });
        mSliderGroup = (ViewGroup) mView.findViewById(R.id.slider_group);
        mMoreButton = (ImageView) mView.findViewById(R.id.expand_button);
        mMoreButton.setOnClickListener(this);

        mDialog = new Dialog(context, R.style.Theme_Panel_Volume);
        mDialog.setTitle("Volume control"); // No need to localize
        mDialog.setContentView(mView);
        mDialog.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                mActiveStreamType = -1;
            }
        });
        // Change some window properties
        Window window = mDialog.getWindow();
        window.setGravity(Gravity.TOP);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = null;
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        mToneGenerators = new ToneGenerator[AudioSystem.getNumStreamTypes()];
        mVibrator = new Vibrator();

        listenToRingerMode();
    }

    private void listenToRingerMode() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        mContext.registerReceiver(new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action)) {
                    removeMessages(MSG_RINGER_MODE_CHANGED);
                    sendMessage(obtainMessage(MSG_RINGER_MODE_CHANGED));
                }
            }
        }, filter);
    }

    private boolean isMuted() {
        int ringerMode = mAudioService.getRingerMode();
        return (ringerMode == AudioManager.RINGER_MODE_SILENT);
    }

    private void createSliders() {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mStreamControls = new HashMap<Integer,StreamControl>(STREAM_TYPES.length);
        for (int i = 0; i < STREAM_TYPES.length; i++) {
            StreamControl sc = new StreamControl();
            sc.streamType = STREAM_TYPES[i];
            sc.group = (ViewGroup) inflater.inflate(R.layout.volume_adjust_item, null);
            sc.group.setTag(sc);
            sc.icon = (ImageView) sc.group.findViewById(R.id.stream_icon);
            sc.icon.setOnClickListener(this);
            sc.icon.setTag(sc);
            sc.iconRes = STREAM_ICONS_NORMAL[i];
            sc.iconMuteRes = STREAM_ICONS_MUTED[i];
            sc.icon.setImageResource(sc.iconRes);
            sc.seekbarView = (SeekBar) sc.group.findViewById(R.id.seekbar);
            sc.seekbarView.setMax(mAudioManager.getStreamMaxVolume(STREAM_TYPES[i]));
            sc.seekbarView.setOnSeekBarChangeListener(this);
            sc.seekbarView.setTag(sc);
            mStreamControls.put(STREAM_TYPES[i], sc);
        }
    }

    private void reorderSliders(int activeStreamType) {
        mSliderGroup.removeAllViews();

        StreamControl active = mStreamControls.get(activeStreamType);
        if (active == null) {
            Log.e("VolumePanel", "Missing stream type! - " + activeStreamType);
            mActiveStreamType = -1;
        } else {
            mSliderGroup.addView(active.group);
            mActiveStreamType = activeStreamType;
            active.group.setVisibility(View.VISIBLE);
            updateSlider(active);
        }

        for (int i = 0; i < STREAM_TYPES.length; i++) {
            // Skip the phone specific ones and the active one
            final int streamType = STREAM_TYPES[i];
            if (streamType == AudioManager.STREAM_RING
                    || streamType == AudioManager.STREAM_VOICE_CALL
                    || streamType == activeStreamType) {
                continue;
            }
            StreamControl sc = mStreamControls.get(streamType);
            mSliderGroup.addView(sc.group);
            updateSlider(sc);
        }
    }

    /** Update the mute and progress state of a slider */
    private void updateSlider(StreamControl sc) {
        sc.seekbarView.setProgress(mAudioManager.getStreamVolume(sc.streamType));
        final boolean muted = isMuted();
        sc.icon.setImageResource(muted ? sc.iconMuteRes : sc.iconRes);
        sc.seekbarView.setEnabled(!muted);
    }

    private boolean isExpanded() {
        return mMoreButton.getVisibility() != View.VISIBLE;
    }

    private void expand() {
        final int count = mSliderGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            mSliderGroup.getChildAt(i).setVisibility(View.VISIBLE);
        }
        mMoreButton.setVisibility(View.INVISIBLE);
    }

    private void collapse() {
        mMoreButton.setVisibility(View.VISIBLE);
        final int count = mSliderGroup.getChildCount();
        for (int i = 1; i < count; i++) {
            mSliderGroup.getChildAt(i).setVisibility(View.GONE);
        }
    }

    private void updateStates() {
        final int count = mSliderGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            StreamControl sc = (StreamControl) mSliderGroup.getChildAt(i).getTag();
            updateSlider(sc);
        }
    }

    public void postVolumeChanged(int streamType, int flags) {
        if (hasMessages(MSG_VOLUME_CHANGED)) return;
        if (mStreamControls == null) {
            createSliders();
        }
        removeMessages(MSG_FREE_RESOURCES);
        obtainMessage(MSG_VOLUME_CHANGED, streamType, flags).sendToTarget();
    }

    /**
     * Override this if you have other work to do when the volume changes (for
     * example, vibrating, playing a sound, etc.). Make sure to call through to
     * the superclass implementation.
     */
    protected void onVolumeChanged(int streamType, int flags) {

        if (LOGD) Log.d(TAG, "onVolumeChanged(streamType: " + streamType + ", flags: " + flags + ")");

        if (mActiveStreamType == -1) {
            reorderSliders(streamType);
        }

        if ((flags & AudioManager.FLAG_SHOW_UI) != 0) {
            onShowVolumeChanged(streamType, flags);
        }

        if ((flags & AudioManager.FLAG_PLAY_SOUND) != 0 && ! mRingIsSilent) {
            removeMessages(MSG_PLAY_SOUND);
            sendMessageDelayed(obtainMessage(MSG_PLAY_SOUND, streamType, flags), PLAY_SOUND_DELAY);
        }

        if ((flags & AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE) != 0) {
            removeMessages(MSG_PLAY_SOUND);
            removeMessages(MSG_VIBRATE);
            onStopSounds();
        }

        removeMessages(MSG_FREE_RESOURCES);
        sendMessageDelayed(obtainMessage(MSG_FREE_RESOURCES), FREE_DELAY);

        resetTimeout();
    }

    protected void onShowVolumeChanged(int streamType, int flags) {
        int index = mAudioService.getStreamVolume(streamType);
        mRingIsSilent = false;

        if (LOGD) {
            Log.d(TAG, "onShowVolumeChanged(streamType: " + streamType
                    + ", flags: " + flags + "), index: " + index);
        }

        // get max volume for progress bar
        int max = mAudioService.getStreamMaxVolume(streamType);

        switch (streamType) {

            case AudioManager.STREAM_RING: {
                setRingerIcon();
                Uri ringuri = RingtoneManager.getActualDefaultRingtoneUri(
                        mContext, RingtoneManager.TYPE_RINGTONE);
                if (ringuri == null) {
                    mRingIsSilent = true;
                }
                break;
            }

            case AudioManager.STREAM_MUSIC: {
                setMusicIcon();
                break;
            }

            case AudioManager.STREAM_VOICE_CALL: {
                /*
                 * For in-call voice call volume, there is no inaudible volume.
                 * Rescale the UI control so the progress bar doesn't go all
                 * the way to zero and don't show the mute icon.
                 */
                index++;
                max++;
                break;
            }

            case AudioManager.STREAM_ALARM: {
                break;
            }

            case AudioManager.STREAM_FM: {
                break;
            }

            case AudioManager.STREAM_NOTIFICATION: {
                Uri ringuri = RingtoneManager.getActualDefaultRingtoneUri(
                        mContext, RingtoneManager.TYPE_NOTIFICATION);
                if (ringuri == null) {
                    mRingIsSilent = true;
                }
                break;
            }

            case AudioManager.STREAM_BLUETOOTH_SCO: {
                /*
                 * For in-call voice call volume, there is no inaudible volume.
                 * Rescale the UI control so the progress bar doesn't go all
                 * the way to zero and don't show the mute icon.
                 */
                index++;
                max++;
                setScoIcon();
                break;
            }
        }

        StreamControl sc = mStreamControls.get(streamType);
        if (sc != null) {
            sc.seekbarView.setProgress(index);
        }

        if (!mDialog.isShowing()) {
            mDialog.setContentView(mView);
            // Showing dialog - use collapsed state
            collapse();
            mDialog.show();
        }

        // Do a little vibrate if applicable (only when going into vibrate mode)
        if ((flags & AudioManager.FLAG_VIBRATE) != 0 &&
                mAudioService.isStreamAffectedByRingerMode(streamType) &&
                mAudioService.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE &&
                mAudioService.shouldVibrate(AudioManager.VIBRATE_TYPE_RINGER)) {
            sendMessageDelayed(obtainMessage(MSG_VIBRATE), VIBRATE_DELAY);
        }
    }

    protected void onPlaySound(int streamType, int flags) {

        if (hasMessages(MSG_STOP_SOUNDS)) {
            removeMessages(MSG_STOP_SOUNDS);
            // Force stop right now
            onStopSounds();
        }

        synchronized (this) {
            ToneGenerator toneGen = getOrCreateToneGenerator(streamType);
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP);
        }

        sendMessageDelayed(obtainMessage(MSG_STOP_SOUNDS), BEEP_DURATION);
    }

    protected void onStopSounds() {

        synchronized (this) {
            int numStreamTypes = AudioSystem.getNumStreamTypes();
            for (int i = numStreamTypes - 1; i >= 0; i--) {
                ToneGenerator toneGen = mToneGenerators[i];
                if (toneGen != null) {
                    toneGen.stopTone();
                }
            }
        }
    }

    protected void onVibrate() {

        // Make sure we ended up in vibrate ringer mode
        if (mAudioService.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE) {
            return;
        }

        mVibrator.vibrate(VIBRATE_DURATION);
    }

    /**
     * Lock on this VolumePanel instance as long as you use the returned ToneGenerator.
     */
    private ToneGenerator getOrCreateToneGenerator(int streamType) {
        synchronized (this) {
            if (mToneGenerators[streamType] == null) {
                return mToneGenerators[streamType] = new ToneGenerator(streamType, MAX_VOLUME);
            } else {
                return mToneGenerators[streamType];
            }
        }
    }

    /**
     * Makes the ringer icon visible with an icon that is chosen
     * based on the current ringer mode.
     */
    private void setRingerIcon() {
        StreamControl sc = mStreamControls.get(AudioManager.STREAM_RING);

        int ringerMode = mAudioService.getRingerMode();
        if (sc != null) {
           if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
               sc.iconRes =  R.drawable.ic_audio_vol_mute;
               sc.iconMuteRes = R.drawable.ic_volume_headset_off;
               sc.icon.setImageResource(mAudioManager.isWiredHeadsetOn() ? sc.iconMuteRes : sc.iconRes);
           } else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
               sc.iconRes = R.drawable.ic_audio_ring_notif_vibrate;
               sc.icon.setImageResource(sc.iconRes);
           } else {
               sc.iconRes = R.drawable.ic_audio_vol;
               sc.iconMuteRes = R.drawable.ic_volume_headset;
               sc.icon.setImageResource(mAudioManager.isWiredHeadsetOn() ? sc.iconMuteRes : sc.iconRes);
           }
        }
    }

    private void setMusicIcon() {
        StreamControl sc = mStreamControls.get(AudioManager.STREAM_MUSIC);

        if (sc != null) {
            sc.iconRes = R.drawable.ic_audio_vol;
            sc.iconMuteRes = R.drawable.ic_volume_bluetooth_ad2p;
            sc.icon.setImageResource(mAudioManager.isBluetoothA2dpOn() ? sc.iconMuteRes : sc.iconRes);
        }
    }

    private void setScoIcon() {
        StreamControl sc = mStreamControls.get(AudioManager.STREAM_BLUETOOTH_SCO);

        if (sc != null) {
            sc.iconRes = R.drawable.ic_volume_bluetooth_in_call;
            sc.icon.setImageResource(sc.iconRes);
        }
    }

    protected void onFreeResources() {
        synchronized (this) {
            for (int i = mToneGenerators.length - 1; i >= 0; i--) {
                if (mToneGenerators[i] != null) {
                    mToneGenerators[i].release();
                }
                mToneGenerators[i] = null;
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {

            case MSG_VOLUME_CHANGED: {
                onVolumeChanged(msg.arg1, msg.arg2);
                break;
            }

            case MSG_FREE_RESOURCES: {
                onFreeResources();
                break;
            }

            case MSG_STOP_SOUNDS: {
                onStopSounds();
                break;
            }

            case MSG_PLAY_SOUND: {
                onPlaySound(msg.arg1, msg.arg2);
                break;
            }

            case MSG_VIBRATE: {
                onVibrate();
                break;
            }

            case MSG_TIMEOUT: {
                if (mDialog.isShowing()) {
                    mDialog.dismiss();
                    mActiveStreamType = -1;
                }
                break;
            }

            case MSG_RINGER_MODE_CHANGED: {
                if (mDialog.isShowing()) {
                    updateStates();
                }
                break;
            }

        }
    }

    private void resetTimeout() {
        removeMessages(MSG_TIMEOUT);
        sendMessageDelayed(obtainMessage(MSG_TIMEOUT), TIMEOUT_DELAY);
    }

    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        final Object tag = seekBar.getTag();
        if (fromTouch && tag instanceof StreamControl) {
            StreamControl sc = (StreamControl) tag;
            if (mAudioManager.getStreamVolume(sc.streamType) != progress) {
                mAudioManager.setStreamVolume(sc.streamType, progress, 0);
            }
        }
        resetTimeout();
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void onClick(View v) {
        if (v == mMoreButton) {
            expand();
        } else if (v.getTag() instanceof StreamControl) {
            StreamControl sc = (StreamControl) v.getTag();
            // Expand the dialog if it hasn't been expanded yet.
            if (!isExpanded()) expand();
        }
        resetTimeout();
    }
}
