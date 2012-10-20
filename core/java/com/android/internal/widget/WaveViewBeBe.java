/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.internal.widget;

import java.util.ArrayList;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.android.internal.R;

/**
 * A special widget containing a center and outer ring. Moving the center ring to the outer ring
 * causes an event that can be caught by implementing OnTriggerListener.
 */
public class WaveViewBeBe extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final String TAG = "WaveViewBeBe";
    private static final boolean DBG = false;
    private static final int WAVE_COUNT = 5; // default wave count
    private static final long VIBRATE_SHORT = 0;  // msec
    private static final long VIBRATE_LONG = 0;  // msec

    // Lock state machine states
    private static final int STATE_RESET_LOCK = 0;
    private static final int STATE_READY = 1;
    private static final int STATE_START_ATTEMPT = 2;
    private static final int STATE_ATTEMPTING = 3;
    private static final int STATE_UNLOCK_ATTEMPT = 4;
    private static final int STATE_UNLOCK_SUCCESS = 5;

    // Animation properties.
    private static final long DURATION = 300; // duration of transitional animations
    private static final long FINAL_DURATION = 500; // duration of final animations when unlocking
    private static final long RING_DELAY = 1300; // when to start fading animated rings
    private static final long FINAL_DELAY = 200; // delay for unlock success animation
    private static final long SHORT_DELAY = 100; // for starting one animation after another.
    private static final long RESET_TIMEOUT = 300; // elapsed time of inactivity before we reset

    private Vibrator mVibrator;
    private OnTriggerListener mOnTriggerListener;
    private ArrayList<DrawableHolder> mDrawables = new ArrayList<DrawableHolder>(4);
    private boolean mFingerDown = false;
    private float mRingRadius; // Radius of bitmap ring. Used to snap halo to it
    private int mSnapRadius = 200; // minimum threshold for drag unlock
    private float mLockCenterX; // center of widget as dictated by widget size
    private float mLockCenterY;
    private float mMouseX; // current mouse position as of last touch event
    private float mMouseY;
    private DrawableHolder mUnlockRing;
    private DrawableHolder mUnlockWave;
    private DrawableHolder mUnlockDefault;
    private DrawableHolder mUnlockHalo;
    private int mLockState = STATE_RESET_LOCK;
    private int mGrabbedState = OnTriggerListener.NO_HANDLE;

    public WaveViewBeBe(Context context) {
        this(context, null);
    }

    public WaveViewBeBe(Context context, AttributeSet attrs) {
        super(context, attrs);

        // TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WaveViewBeBe);
        // mOrientation = a.getInt(R.styleable.WaveViewBeBe_orientation, HORIZONTAL);
        // a.recycle();
        Resources r = getResources();
        int densityDpi = r.getDisplayMetrics().densityDpi;

        /* --copied from RotarySelector.java ;)
         *
         * this hack assumes people change build.prop for increasing
         * the virtual size of their screen by decreasing dpi in
         * build.prop file. this is often done especially for hd
         * phones. keep in mind changing build.prop and density
         * isnt officially supported, but this should do for most cases
         */
        if (densityDpi <= 240 && densityDpi >= 200) {
            mRingRadius = 800.0f;
        } else if (densityDpi < 200 && densityDpi >= 160) {
            mRingRadius = 480.0f;
        } else if (densityDpi < 160 && densityDpi >= 120) {
            mRingRadius = 320.0f;
        }

        initDrawables();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mLockCenterX = 0.5f * w;
        mLockCenterY = 0.5f * h;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        // View should be large enough to contain the unlock ring + halo
        return mUnlockRing.getWidth() + mUnlockHalo.getWidth();
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        // View should be large enough to contain the unlock ring + halo
        return mUnlockRing.getHeight() + mUnlockHalo.getHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
        int width;
        int height;

        if (widthSpecMode == MeasureSpec.AT_MOST) {
            width = Math.min(widthSpecSize, getSuggestedMinimumWidth());
        } else if (widthSpecMode == MeasureSpec.EXACTLY) {
            width = widthSpecSize;
        } else {
            width = getSuggestedMinimumWidth();
        }

        if (heightSpecMode == MeasureSpec.AT_MOST) {
            height = Math.min(heightSpecSize, getSuggestedMinimumWidth());
        } else if (heightSpecMode == MeasureSpec.EXACTLY) {
            height = heightSpecSize;
        } else {
            height = getSuggestedMinimumHeight();
        }

        setMeasuredDimension(width, height);
    }

    private void initDrawables() {
        mUnlockRing = new DrawableHolder(createDrawable(R.drawable.unlockbebe_ring));
        mUnlockRing.setX(mLockCenterX);
        mUnlockRing.setY(mLockCenterY);
        mUnlockRing.setScaleX(1.0f);
        mUnlockRing.setScaleY(1.0f);
        mUnlockRing.setAlpha(0.0f);
        mDrawables.add(mUnlockRing);

        mUnlockWave = new DrawableHolder(createDrawable(R.drawable.unlockbebe_wave));
        mUnlockWave.setX(mLockCenterX);
        mUnlockWave.setY(mLockCenterY);
        mUnlockWave.setScaleX(1.0f);
        mUnlockWave.setScaleY(1.0f);
        mUnlockWave.setAlpha(1.0f);
        mDrawables.add(mUnlockWave);

        mUnlockDefault = new DrawableHolder(createDrawable(R.drawable.unlockbebe_default));
        mUnlockDefault.setX(mLockCenterX);
        mUnlockDefault.setY(mLockCenterY);
        mUnlockDefault.setScaleX(1.0f);
        mUnlockDefault.setScaleY(1.0f);
        mUnlockDefault.setAlpha(1.0f);
        mDrawables.add(mUnlockDefault);

        mUnlockHalo = new DrawableHolder(createDrawable(R.drawable.unlockbebe_halo));
        mUnlockHalo.setX(mLockCenterX);
        mUnlockHalo.setY(mLockCenterY + 120);
        mUnlockHalo.setScaleX(2.0f);
        mUnlockHalo.setScaleY(1.0f);
        mUnlockHalo.setAlpha(0.0f);
        mDrawables.add(mUnlockHalo);
    }

    private void waveUpdateFrame(float mouseX, float mouseY, boolean fingerDown) {
        double distX = mouseX - mLockCenterX;
        double distY = mouseY - mLockCenterY;
        int dragDistance = (int) Math.ceil(Math.hypot(distX, distY));
        double touchA = Math.atan2(distX, distY);
        float ringX = (float) (mLockCenterX + mRingRadius * Math.sin(touchA));
        float ringY = (float) (mLockCenterY + mRingRadius * Math.cos(touchA));

        switch (mLockState) {
            case STATE_RESET_LOCK:
                if (DBG) Log.v(TAG, "State RESET_LOCK");
                mUnlockRing.removeAnimationFor("x");
                mUnlockRing.removeAnimationFor("y");
                mUnlockRing.removeAnimationFor("scaleX");
                mUnlockRing.removeAnimationFor("scaleY");
                mUnlockRing.removeAnimationFor("alpha");
                mUnlockRing.setX(mLockCenterX);
                mUnlockRing.setY(mLockCenterY);
                mUnlockRing.setScaleY(1.0f);
                mUnlockRing.setScaleY(1.0f);
                mUnlockRing.setAlpha(0.0f);

                mUnlockWave.removeAnimationFor("x");
                mUnlockWave.removeAnimationFor("y");
                mUnlockWave.removeAnimationFor("scaleX");
                mUnlockWave.removeAnimationFor("scaleY");
                mUnlockWave.removeAnimationFor("alpha");
                // mUnlockWave.setX(mLockCenterX);
                // mUnlockWave.setY(mLockCenterY);
                mUnlockWave.setScaleX(1.0f);
                mUnlockWave.setScaleY(1.0f);
                mUnlockWave.setAlpha(1.0f);
                mUnlockWave.addAnimTo(DURATION, 0, "x", mLockCenterX, true);
                mUnlockWave.addAnimTo(DURATION, 0, "y", mLockCenterY, true);

                mUnlockDefault.removeAnimationFor("x");
                mUnlockDefault.removeAnimationFor("y");
                mUnlockDefault.removeAnimationFor("scaleX");
                mUnlockDefault.removeAnimationFor("scaleY");
                mUnlockDefault.removeAnimationFor("alpha");
                mUnlockDefault.setX(mLockCenterX);
                mUnlockDefault.setY(mLockCenterY);
                mUnlockDefault.setScaleX(1.0f);
                mUnlockDefault.setScaleY(1.0f);
                mUnlockDefault.setAlpha(1.0f);

                mUnlockHalo.removeAnimationFor("x");
                mUnlockHalo.removeAnimationFor("y");
                mUnlockHalo.removeAnimationFor("scaleX");
                mUnlockHalo.removeAnimationFor("scaleY");
                mUnlockHalo.removeAnimationFor("alpha");
                mUnlockHalo.setScaleX(2.0f);
                mUnlockHalo.setScaleY(1.0f);
                // mUnlockHalo.setAlpha(1.0f);
                mUnlockHalo.addAnimTo(DURATION, 0, "x", mLockCenterX, true);
                mUnlockHalo.addAnimTo(DURATION, 0, "y", mLockCenterY + 480, true);
                mUnlockHalo.addAnimTo(0, DURATION, "y", mLockCenterY + 320, true);
                mUnlockHalo.addAnimTo(0, DURATION, "alpha", 0.0f, true);

                removeCallbacks(mLockTimerActions);

                mLockState = STATE_READY;
                break;

            case STATE_READY:
                if (DBG) Log.v(TAG, "State READY");
                break;

            case STATE_START_ATTEMPT:
                if (DBG) Log.v(TAG, "State START_ATTEMPT");

                mLockState = STATE_ATTEMPTING;
                break;

            case STATE_ATTEMPTING:
                if (DBG) Log.v(TAG, "State ATTEMPTING (fingerDown = " + fingerDown + ")");
                if (mouseY < mLockCenterY - 55) {
                    if (fingerDown) {
                        mUnlockWave.addAnimTo(0, 0, "x", mLockCenterX, true);
                        mUnlockWave.addAnimTo(0, 0, "y", mouseY - 480, true);
                        mUnlockWave.addAnimTo(0, 0, "scaleX", 1.0f, true);
                        mUnlockWave.addAnimTo(0, 0, "scaleY", 1.0f, true);
                        mUnlockWave.addAnimTo(0, 0, "alpha", 1.0f, true);

                        mUnlockDefault.addAnimTo(0, 0, "x", mLockCenterX, true);
                        mUnlockDefault.addAnimTo(0, 0, "y", mLockCenterY, true);
                        mUnlockDefault.addAnimTo(0, 0, "scaleX", 1.0f, true);
                        mUnlockDefault.addAnimTo(0, 0, "scaleY", 1.0f, true);
                        mUnlockDefault.addAnimTo(0, 0, "alpha", 0.0f, true);

                        mUnlockHalo.addAnimTo(0, 0, "x", mouseX, true);
                        mUnlockHalo.addAnimTo(0, 0, "y", mouseY, true);
                        mUnlockHalo.addAnimTo(0, 0, "scaleX", 2.0f, true);
                        mUnlockHalo.addAnimTo(0, 0, "scaleY", 1.0f, true);
                        mUnlockHalo.addAnimTo(0, 0, "alpha", 1.0f, true);
                    }  else {
                        if (DBG) Log.v(TAG, "up detected, moving to STATE_UNLOCK_ATTEMPT");
                        mLockState = STATE_UNLOCK_ATTEMPT;
                    }
                } else {
                    mUnlockWave.addAnimTo(0, 0, "x", mLockCenterX, true);
                    mUnlockWave.addAnimTo(0, 0, "y", mouseY - 480, true);
                    mUnlockWave.addAnimTo(0, 0, "scaleX", 1.0f, true);
                    mUnlockWave.addAnimTo(0, 0, "scaleY", 1.0f, true);
                    mUnlockWave.addAnimTo(0, 0, "alpha", 1.0f, true);

                    mUnlockDefault.addAnimTo(0, 0, "x", mLockCenterX, true);
                    mUnlockDefault.addAnimTo(0, 0, "y", mLockCenterY, true);
                    mUnlockDefault.addAnimTo(0, 0, "scaleX", 1.0f, true);
                    mUnlockDefault.addAnimTo(0, 0, "scaleY", 1.0f, true);
                    mUnlockDefault.addAnimTo(0, 0, "alpha", 1.0f, true);

                    mUnlockHalo.addAnimTo(0, 0, "x", mouseX, true);
                    mUnlockHalo.addAnimTo(0, 0, "y", mouseY, true);
                    mUnlockHalo.addAnimTo(0, 0, "scaleX", 2.0f, true);
                    mUnlockHalo.addAnimTo(0, 0, "scaleY", 1.0f, true);
                    mUnlockHalo.addAnimTo(0, 0, "alpha", 1.0f, true);
                }
                break;

            case STATE_UNLOCK_ATTEMPT:
                if (DBG) Log.v(TAG, "State UNLOCK_ATTEMPT");
                if (mouseY < mLockCenterY - 55) {
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "x", mLockCenterX, true);
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "y", mLockCenterY - 800, true);
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "scaleX", 2.0f, false);
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "scaleY", 1.0f, false);
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "alpha", 1.0f, false);

                    mUnlockHalo.addAnimTo(FINAL_DURATION, 0, "x", mLockCenterX, true);
                    mUnlockHalo.addAnimTo(FINAL_DURATION, 0, "y", mLockCenterY - 480, true);
                    mUnlockHalo.addAnimTo(FINAL_DURATION, 0, "scaleX", 1.0f, false);
                    mUnlockHalo.addAnimTo(FINAL_DURATION, 0, "scaleY", 1.0f, false);
                    mUnlockHalo.addAnimTo(FINAL_DURATION, 0, "alpha", 1.0f, false);

                    removeCallbacks(mLockTimerActions);

                    postDelayed(mLockTimerActions, RESET_TIMEOUT);

                    dispatchTriggerEvent(OnTriggerListener.CENTER_HANDLE);
                    mLockState = STATE_UNLOCK_SUCCESS;
                } else {
                    mLockState = STATE_RESET_LOCK;
                }
                break;

            case STATE_UNLOCK_SUCCESS:
                if (DBG) Log.v(TAG, "State UNLOCK_SUCCESS");
                break;

            default:
                if (DBG) Log.v(TAG, "Unknown state " + mLockState);
                break;
        }
        mUnlockDefault.startAnimations(this);
        mUnlockHalo.startAnimations(this);
        mUnlockRing.startAnimations(this);
        mUnlockWave.startAnimations(this);
    }

    BitmapDrawable createDrawable(int resId) {
        Resources res = getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(res, resId);
        return new BitmapDrawable(res, bitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        waveUpdateFrame(mMouseX, mMouseY, mFingerDown);
        for (int i = 0; i < mDrawables.size(); ++i) {
            mDrawables.get(i).draw(canvas);
        }
    }

    private final Runnable mLockTimerActions = new Runnable() {
        public void run() {
            if (DBG) Log.v(TAG, "LockTimerActions");
            // reset lock after inactivity
            if (mLockState == STATE_ATTEMPTING) {
                if (DBG) Log.v(TAG, "Timer resets to STATE_RESET_LOCK");
                mLockState = STATE_RESET_LOCK;
            }
            invalidate();
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        mMouseX = event.getX();
        mMouseY = event.getY();
        boolean handled = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                removeCallbacks(mLockTimerActions);
                mFingerDown = true;
                setGrabbedState(OnTriggerListener.CENTER_HANDLE);
                {
                    float x = mMouseX - mUnlockHalo.getX();
                    float y = mMouseY - mUnlockHalo.getY();
                    float dist = (float) Math.hypot(x, y);
                    if (dist < mUnlockHalo.getWidth()*0.5f) {
                        if (mLockState == STATE_READY) {
                            mLockState = STATE_START_ATTEMPT;
                        }
                    }
                }
                handled = true;
                break;

            case MotionEvent.ACTION_MOVE:
                handled = true;
                break;

            case MotionEvent.ACTION_UP:
                if (DBG) Log.v(TAG, "ACTION_UP");
                mFingerDown = false;
                postDelayed(mLockTimerActions, RESET_TIMEOUT);
                setGrabbedState(OnTriggerListener.NO_HANDLE);
                // Normally the state machine is driven by user interaction causing redraws.
                // However, when there's no more user interaction and no running animations,
                // the state machine stops advancing because onDraw() never gets called.
                // The following ensures we advance to the next state in this case,
                // either STATE_UNLOCK_ATTEMPT or STATE_RESET_LOCK.
                waveUpdateFrame(mMouseX, mMouseY, mFingerDown);
                handled = true;
                break;

            case MotionEvent.ACTION_CANCEL:
                mFingerDown = false;
                handled = true;
                break;
        }
        invalidate();
        return handled ? true : super.onTouchEvent(event);
    }

    /**
     * Triggers haptic feedback.
     */
    private synchronized void vibrate(long duration) {
        if (mVibrator == null) {
            mVibrator = (android.os.Vibrator)
                    getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        mVibrator.vibrate(duration);
    }

    /**
     * Registers a callback to be invoked when the user triggers an event.
     *
     * @param listener the OnDialTriggerListener to attach to this view
     */
    public void setOnTriggerListener(OnTriggerListener listener) {
        mOnTriggerListener = listener;
    }

    /**
     * Dispatches a trigger event to listener. Ignored if a listener is not set.
     * @param whichHandle the handle that triggered the event.
     */
    private void dispatchTriggerEvent(int whichHandle) {
        vibrate(VIBRATE_LONG);
        if (mOnTriggerListener != null) {
            mOnTriggerListener.onTrigger(this, whichHandle);
        }
    }

    /**
     * Sets the current grabbed state, and dispatches a grabbed state change
     * event to our listener.
     */
    private void setGrabbedState(int newState) {
        if (newState != mGrabbedState) {
            mGrabbedState = newState;
            if (mOnTriggerListener != null) {
                mOnTriggerListener.onGrabbedStateChange(this, mGrabbedState);
            }
        }
    }

    public interface OnTriggerListener {
        /**
         * Sent when the user releases the handle.
         */
        public static final int NO_HANDLE = 0;

        /**
         * Sent when the user grabs the center handle
         */
        public static final int CENTER_HANDLE = 10;

        /**
         * Called when the user drags the center ring beyond a threshold.
         */
        void onTrigger(View v, int whichHandle);

        /**
         * Called when the "grabbed state" changes (i.e. when the user either grabs or releases
         * one of the handles.)
         *
         * @param v the view that was triggered
         * @param grabbedState the new state: {@link #NO_HANDLE}, {@link #CENTER_HANDLE},
         */
        void onGrabbedStateChange(View v, int grabbedState);
    }

    public void onAnimationUpdate(ValueAnimator animation) {
        invalidate();
    }

    public void reset() {
        if (DBG) Log.v(TAG, "reset() : resets state to STATE_RESET_LOCK");
        mLockState = STATE_RESET_LOCK;
        invalidate();
    }
}
