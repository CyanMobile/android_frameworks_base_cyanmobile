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

package com.android.systemui.statusbar;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Display;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class TrackingView extends LinearLayout {
    StatusBarService mService;
    private boolean mTracking;
    private int mStartX, mStartY;
    boolean mIsAttachedToWindow;
    private static final int MAJOR_MOVE = 60;
    private GestureDetector mGestureDetector;
    private Handler mHandler = new Handler();

    public TrackingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                       float velocityY) {
                    int dx = (int) (e2.getX() - e1.getX());

                    // don't accept the fling if it's too short
                    // as it may conflict with tracking move
                    if (Math.abs(dx) > MAJOR_MOVE && Math.abs(velocityX) > Math.abs(velocityY)) {
                        if (velocityX > 0) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                     mService.togglePower();
                                }
                            });
                        } else {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                     mService.toggleNotif();
                                }
                            });
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            });
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_BACK:
            if (down) {
                //mService.deactivate();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mService.onTrackingViewAttached();
        mIsAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mService.onTrackingViewDetached();
        mIsAttachedToWindow = false;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mService.updateExpandedViewPos(StatusBarService.EXPANDED_LEAVE_ALONE);
                }
            });
        }
    }
}
