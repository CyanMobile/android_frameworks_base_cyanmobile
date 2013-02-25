/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.provider.MediaStore;
import com.android.internal.widget.multiwaveview.GlowPadView;
import com.android.internal.widget.multiwaveview.GlowPadView.OnTriggerListener;
import android.content.ActivityNotFoundException;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.systemui.R;

/**
 * Present the user with a dialog of QwikWidgets
 */
public class RingPanelView extends FrameLayout {

    private static final String TAG = "RingPanelView";
    private Context mContext;
    private boolean mShowing;
    private Handler mHandler;
    StatusBarService mService;
    private View mSearchTargetsContainer;
    private GlowPadView mGlowPadView;

    private class GlowPadViewMethods implements GlowPadView.OnTriggerListener {

        public void updateResources() {
            int resId = R.array.lockscreen_targets_without_lock;
            if (mGlowPadView.getTargetResourceId() != resId) {
                mGlowPadView.setTargetResources(resId);
            }
            setEnabled(com.android.internal.R.drawable.ic_lockscreen_phone, true);
            setEnabled(com.android.internal.R.drawable.ic_lockscreen_sms, true);
        }

        public void onGrabbed(View v, int handle) {
        }

        public void onReleased(View v, int handle) {
        }

        public void onTrigger(View v, int target) {
            final int resId = mGlowPadView.getResourceIdForTarget(target);
            switch (resId) {
                case com.android.internal.R.drawable.ic_lockscreen_sms:
                    launchSms();
                    mHandler.removeCallbacks(mResetRing);
                    mHandler.postDelayed(mResetRing, 100);
                    break;
                case com.android.internal.R.drawable.ic_lockscreen_phone:
                    launchPhone();
                    mHandler.removeCallbacks(mResetRing);
                    mHandler.postDelayed(mResetRing, 100);
                    break;
           }
        }

        private void launchPhone() {
            Intent iophone = new Intent(Intent.ACTION_MAIN);
            iophone.setClassName("com.android.contacts",
                                     "com.android.contacts.DialtactsActivity");
            iophone.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                mContext.startActivity(iophone);
            } catch (ActivityNotFoundException e) {
            }
        }

        private void launchSms() {
            Intent ioinbox = new Intent(Intent.ACTION_MAIN);  
            ioinbox.addCategory(Intent.CATEGORY_DEFAULT);  
            ioinbox.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ioinbox.setType("vnd.android-dir/mms-sms");
            try {
                mContext.startActivity(ioinbox);
            } catch (ActivityNotFoundException e) {
            }
        }

        public void onGrabbedStateChange(View v, int handle) {
            if (handle == OnTriggerListener.NO_HANDLE) {
                mService.toggleRingPanel();
            }
        }

        public void setEnabled(int resourceId, boolean enabled) {
            mGlowPadView.setEnableTarget(resourceId, enabled);
        }

        public void onFinishFinalAnimation() {
        }
    }

    private GlowPadViewMethods mGlowPadViewMethods;

    public RingPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHandler = new Handler();
        mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSearchTargetsContainer = findViewById(R.id.search_panel_container);
        mGlowPadView = (GlowPadView) findViewById(R.id.ringswidget);
        mGlowPadViewMethods = new GlowPadViewMethods();
        mGlowPadView.setOnTriggerListener(mGlowPadViewMethods);
        mGlowPadViewMethods.updateResources();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // setPanelHeight(mSearchTargetsContainer.getHeight());
    }

    private boolean pointInside(int x, int y, View v) {
        final int l = v.getLeft();
        final int r = v.getRight();
        final int t = v.getTop();
        final int b = v.getBottom();
        return x >= l && x < r && y >= t && y < b;
    }

    public boolean isInContentArea(int x, int y) {
        if (pointInside(x, y, mSearchTargetsContainer)) {
            return true;
        } else {
            return false;
        }
    }

    private Runnable mResetRing = new Runnable() {
        public void run() {
           hide();
        }
    };

    private OnPreDrawListener mPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        public boolean onPreDraw() {
            getViewTreeObserver().removeOnPreDrawListener(this);
            mGlowPadView.resumeAnimations();
            return false;
        }
    };

    public void show(final boolean show) {
        mShowing = show;
        if (show) {
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
                mGlowPadView.suspendAnimations();
                mGlowPadView.ping();
                getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
            }
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        } else {
            setVisibility(View.GONE);
        }
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    /**
     * Whether the panel is showing, or, if it's animating, whether it will be
     * when the animation is done.
     */
    public boolean isShowing() {
        return mShowing;
    }
}
