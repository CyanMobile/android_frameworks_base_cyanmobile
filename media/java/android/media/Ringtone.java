/*
 * Copyright (C) 2006 The Android Open Source Project
 * This code has been modified.  Portions copyright (C) 2010, T-Mobile USA, Inc.
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

package android.media;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.DrmStore;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.VideoView;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Ringtone provides a quick method for playing a ringtone, notification, or
 * other similar types of sounds.
 * <p>
 * For ways of retrieving {@link Ringtone} objects or to show a ringtone
 * picker, see {@link RingtoneManager}.
 * 
 * @see RingtoneManager
 */
public class Ringtone {
    private static String TAG = "Ringtone";

    private static final String[] MEDIA_AUDIO_COLUMNS = new String[] {
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE
    };

    private static final String[] MEDIA_VIDEO_COLUMNS = new String[] {
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.TITLE
    };

    private static final String[] DRM_COLUMNS = new String[] {
        DrmStore.Audio._ID,
        DrmStore.Audio.DATA,
        DrmStore.Audio.TITLE
    };

    private MediaPlayer mAudio;
    private VideoView mVideoView;

    private Uri mUri;
    private String mTitle;
    private String mMimeType;
    private FileDescriptor mFileDescriptor;
    private AssetFileDescriptor mAssetFileDescriptor;

    private int mStreamType = AudioManager.STREAM_RING;
    private AudioManager mAudioManager;

    private Context mContext;

    Ringtone(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Sets the stream type where this ringtone will be played.
     * 
     * @param streamType The stream, see {@link AudioManager}.
     */
    public void setStreamType(int streamType) {
        mStreamType = streamType;
        
        if (mAudio != null) {
            /*
             * The stream type has to be set before the media player is
             * prepared. Re-initialize it.
             */
            try {
                openMediaPlayer();
            } catch (IOException e) {
                Log.w(TAG, "Couldn't set the stream type", e);
            }
        }
    }

    /**
     * Gets the stream type where this ringtone will be played.
     * 
     * @return The stream type, see {@link AudioManager}.
     */
    public int getStreamType() {
        return mStreamType;
    }

    /**
     * Returns a human-presentable title for ringtone. Looks in media and DRM
     * content providers. If not in either, uses the filename
     * 
     * @param context A context used for querying. 
     */
    public String getTitle(Context context) {
        if (mTitle != null) return mTitle;
        return mTitle = getTitle(context, mUri, true);
    }

    private static String stringForQuery(Cursor cursor) {
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private static String getTitle(Context context, Uri uri, boolean followSettingsUri) {
        Cursor cursor = null;
        ContentResolver res = context.getContentResolver();
        
        String title = null;

        if (uri != null) {
            String authority = uri.getAuthority();

            if (Settings.AUTHORITY.equals(authority)) {
                if (followSettingsUri) {
                    Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context,
                            RingtoneManager.getDefaultType(uri));
                    String actualTitle = getTitle(context, actualUri, false);
                    title = context
                            .getString(com.android.internal.R.string.ringtone_default_with_actual,
                                    actualTitle);
                }
            } else if (RingtoneManager.THEME_AUTHORITY.equals(authority)) {
                Uri themes = Uri.parse("content://com.tmobile.thememanager.themes/themes");
                title = stringForQuery(res.query(themes, new String[] { "ringtone_name" },
                    "ringtone_uri = ?", new String[] { uri.toString() }, null));
                if (title == null) {
                    title = stringForQuery(res.query(themes, new String[] { "notif_ringtone_name" },
                            "notif_ringtone_uri = ?", new String[] { uri.toString() }, null));
                }
            } else {
                
                if (DrmStore.AUTHORITY.equals(authority)) {
                    cursor = res.query(uri, DRM_COLUMNS, null, null, null);
                } else if (MediaStore.AUTHORITY.equals(authority)) {
                    cursor = res.query(uri, MEDIA_AUDIO_COLUMNS, null, null, null);
                } else if (MediaStore.AUTHORITY.equals(authority)) {
                    cursor = res.query(uri, MEDIA_VIDEO_COLUMNS, null, null, null);
                }
                
                try {
                    if (cursor != null && cursor.getCount() == 1) {
                        cursor.moveToFirst();
                        return cursor.getString(2);
                    } else {
                        title = uri.getLastPathSegment();
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }

        if (title == null) {
            title = context.getString(com.android.internal.R.string.ringtone_unknown);
            
            if (title == null) {
                title = "";
            }
        }
        
        return title;
    }
    
    /**
     * Returns the MIME type of this ringtone's media.
     * 
     * @param context A context used for querying.
     * @return MIME type of this ringtone.
     */
    public String getMimeType(Context context) {
        if (mMimeType != null) return mMimeType;
        return mMimeType = getMimeType(context, mUri, true);
    }

    /**
     * Returns the MIME type of a ringtone Uri. If it is a special
     * settings ringtone (e.g. "default"), then return the type of the
     * actual ringtone to which it refers.
     * 
     * @param context A context used for querying.
     * @param uri Uri to query.
     * @return MIME type of this ringtone.
     */
    public static String getMimeType(Context context, Uri uri) {
        return getMimeType(context, uri, true);
    }

    private static String getMimeType(Context context, Uri uri, boolean followSettingsUri) {
        if (uri == null) {
            return null;
        }

        if (followSettingsUri && Settings.AUTHORITY.equals(uri.getAuthority())) {
            Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context,
                RingtoneManager.getDefaultType(uri));
            return getMimeType(context, actualUri, false);
        }
        else {
            ContentResolver res = context.getContentResolver();
            return res.getType(uri);
        }
    }

    /**
     * Is this ringtone a video clip?
     * 
     * @param context A context used for querying.
     * @return true if this is a video ringtone
     */
    public boolean isVideo(Context context) {
        // It might be more in the spirit of current usage to check
        // based on the Uri. However, the MIME type is probably a more
        // robust check, in the face of potential future uses.
        String mimeType = getMimeType(context);
        return ((mimeType != null) && (mimeType.startsWith("video")));
    }

    /**
     * Is this Uri a video clip?
     * 
     * @param context A context used for querying.
     * @param uri Uri to query.
     * @return true if this is a video ringtone
     */
    public static boolean isVideo(Context context, Uri uri) {
        String mimeType = getMimeType(context, uri);
        return ((mimeType != null) && (mimeType.startsWith("video")));
    }

    private void openMediaPlayer() throws IOException {
        stop();
        mAudio = new MediaPlayer();
        if (mUri != null) {
            mAudio.setDataSource(mContext, mUri);
        } else if (mFileDescriptor != null) {
            mAudio.setDataSource(mFileDescriptor);
        } else if (mAssetFileDescriptor != null) {
            // Note: using getDeclaredLength so that our behavior is the same
            // as previous versions when the content provider is returning
            // a full file.
            if (mAssetFileDescriptor.getDeclaredLength() < 0) {
                mAudio.setDataSource(mAssetFileDescriptor.getFileDescriptor());
            } else {
                mAudio.setDataSource(mAssetFileDescriptor.getFileDescriptor(),
                                     mAssetFileDescriptor.getStartOffset(),
                                     mAssetFileDescriptor.getDeclaredLength());
            }
        } else {
            throw new IOException("No data source set.");
        }
        mAudio.setAudioStreamType(mStreamType);
        mAudio.prepare();
    }

    void open(FileDescriptor fd) throws IOException {
        mFileDescriptor = fd;
        openMediaPlayer();
    }

    void open(AssetFileDescriptor fd) throws IOException {
        mAssetFileDescriptor = fd;
        openMediaPlayer();
    }

    void open(Uri uri) throws IOException {
        mUri = uri;
        openMediaPlayer();
    }
    
    /**
     * Plays the ringtone.
     */
    public void play() {
        if (mAudio == null) {
            try {
                openMediaPlayer();
            } catch (Exception ex) {
                Log.e(TAG, "play() caught ", ex);
                mAudio = null;
            }
        }
        if (mAudio != null) {
            // do not ringtones if stream volume is 0
            // (typically because ringer mode is silent).
            if (mAudioManager.getStreamVolume(mStreamType) != 0) {
                mAudio.start();
            }
        }
    }

    /**
     * Play a ringtone, audio or video.
     * @param activity Activity in which to play video ringtone, or null.
     * @param videoView VideoView on which to play video ringtone, or null.
     */
    public void play(Activity activity, VideoView videoView) {
        if ((activity == null) || !isVideo(activity) || (videoView == null)) {
            // Either this is an audio ringtone, or we don't have a
            // place to show the video (so just play the audio
            // component of a video ringtone).
            play();
        }
        else {
            if (mVideoView != null) {
                // This shouldn't happen
                Log.e(TAG, "Video player already set.");
                stop();
            }
            mVideoView = videoView;
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    mVideoView.setVideoURI(mUri);
                    mVideoView.start();
                }
            });
        }
    }

    /**
     * Stops a playing ringtone.
     */
    public void stop() {
        if (mAudio != null) {
            mAudio.reset();
            mAudio.release();
            mAudio = null;
        }
        if (mVideoView != null) {
            mVideoView.stopPlayback();
            mVideoView = null;
        }
    }

    /**
     * Whether this ringtone is currently playing.
     * 
     * @return True if playing, false otherwise.
     */
    public boolean isPlaying() {
        return ((mAudio != null && mAudio.isPlaying()) ||
                (mVideoView != null && mVideoView.isPlaying()));
    }

    void setTitle(String title) {
        mTitle = title;
    }

    /** 
     * Get duration of the ringtone
     */
    public int getDuration() {
      int duration = 0;
      if (mAudio != null) {
        duration = mAudio.getDuration();
      }
      return duration;
    }
}
