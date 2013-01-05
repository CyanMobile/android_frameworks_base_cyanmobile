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

package com.android.systemui.statusbar.popups;

import java.lang.Math;
import android.content.res.Resources;
import android.view.Gravity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.Drawable;
import android.widget.ProgressBar;

import com.android.systemui.R;

/**
 * Displays a dialog showing the current brightness
 *
 * @hide
 */
public class BrightnessPanel extends Handler
{
    private static final String TAG = "BrightnessPanel";
    private static boolean LOGD = false;

    private static final int MSG_BRIGHTNESS_CHANGED = 0;

    protected Context mContext;

    /** Dialog containing the brightness info */
    private final Toast mToast;
    /** Dialog's content view */
    private final View mView;
    private Drawable mIcon = null;

    /** View displaying the current brightness */
    private TextView mText;
    private ProgressBar mBrightProg;

    public BrightnessPanel(final Context context) {
        mContext = context;
        mToast = new Toast(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mView = inflater.inflate(R.layout.brightness_adjust, null);
        mText = (TextView) mView.findViewById(R.id.brightness_text);
        mBrightProg = (ProgressBar) mView.findViewById(R.id.brightness_progress);
        mIcon = context.getResources().getDrawable(R.drawable.ic_stats_brightness);
    }

    public void postBrightnessChanged(final int value, final int max) {
        if (hasMessages(MSG_BRIGHTNESS_CHANGED)) return;
        obtainMessage(MSG_BRIGHTNESS_CHANGED, value, max).sendToTarget();
    }

    /**
     * Override this if you have other work to do when the volume changes (for
     * example, vibrating, playing a sound, etc.). Make sure to call through to
     * the superclass implementation.
     */
    protected void onBrightnessChanged(final int value, final int max) {
        mText.setText(Integer.toString(Math.round(value * 100.0f / max)) + "%");
        mText.setCompoundDrawablesWithIntrinsicBounds(mIcon, null, null, null);
	int progress = (int) (Math.round(value * 100.0f / max));
	mBrightProg.setProgress(progress);
        mToast.setView(mView);
        mToast.setDuration(Toast.LENGTH_SHORT);
        mToast.setGravity(Gravity.TOP, 0, 0);
        mToast.show();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_BRIGHTNESS_CHANGED: {
                onBrightnessChanged(msg.arg1, msg.arg2);
                break;
            }
        }
    }
}
