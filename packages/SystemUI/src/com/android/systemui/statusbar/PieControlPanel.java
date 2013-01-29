/*
 * Copyright (C) 2010 ParanoidAndroid Project
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.IWindowManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.widget.FrameLayout;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.PieControl.OnNavButtonPressedListener;

public class PieControlPanel extends FrameLayout implements OnNavButtonPressedListener{

    private static final boolean DEBUG = false;
    private static final String TAG = "PieView";

    private Handler mHandler;
    boolean mShowing;
    private PieControl mPieControl;
    private int mInjectKeycode;
    private long mDownTime;
    private Context mContext;
    private int mOrientation;
    private int mWidth;
    private int mHeight;
    private View mTrigger;
    Display mDisplay;
    DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    ViewGroup mContentFrame;
    Rect mContentArea = new Rect();

    public PieControlPanel(Context context) {
        this(context, null);
    }

    public PieControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPieControl = new PieControl(context, this);
        mPieControl.setOnNavButtonPressedListener(this);
        mOrientation = Gravity.BOTTOM;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public int getDegree() {
        switch(mOrientation) {
            case Gravity.LEFT: return 180;
            case Gravity.TOP: return -90;
            case Gravity.RIGHT: return 0;
            case Gravity.BOTTOM: return 90;
        }
        return 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mPieControl.onTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onAttachedToWindow () {
        super.onAttachedToWindow();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        show(false);
    }

    public void init(Handler h, View trigger, int orientation) {
        mHandler = h;
        mTrigger = trigger;
        mOrientation = orientation;
        setCenter();
        mPieControl.init();
    }

    public NotificationData setNotifications(NotificationData list) {
        if (mPieControl != null) {
            mPieControl.setNotifications(list);
        }
        return list;
    }

    public void reorient(int orientation) {
        mOrientation = orientation;
        WindowManagerImpl.getDefault().removeView(mTrigger);
        WindowManagerImpl.getDefault().addView(mTrigger, StatusBarService.getPieTriggerLayoutParams(mContext, mOrientation));
        setCenter();
        show(mShowing);

        int pieGravity = 3;
        switch(mOrientation) {
            case Gravity.LEFT:
                pieGravity = 0;
                break;
            case Gravity.TOP:
                pieGravity = 1;
                break;
            case Gravity.RIGHT:
                pieGravity = 2;
                break;
        }

        Settings.System.putInt(mContext.getContentResolver(),
            Settings.System.PIE_GRAVITY, pieGravity);
    }

    public void setCenter() {
        Point outSize = new Point(0,0);
        mDisplay = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mDisplay.getMetrics(mDisplayMetrics);
        mWidth = mDisplayMetrics.widthPixels;
        mHeight = mDisplayMetrics.heightPixels;
        switch(mOrientation) {
            case Gravity.LEFT:
                mPieControl.setCenter(0, mHeight / 2);
                break;
            case Gravity.TOP:
                mPieControl.setCenter(mWidth / 2, 0);
                break;
            case Gravity.RIGHT:
                mPieControl.setCenter(mWidth, mHeight / 2);
                break;
            case Gravity.BOTTOM: 
                mPieControl.setCenter(mWidth / 2, mHeight);
                break;
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mContentFrame = (ViewGroup)findViewById(R.id.content_frame);
        setWillNotDraw(false);
        mPieControl.attachToContainer(this);
        mPieControl.forceToTop(this);
        show(false);
    }

    public boolean isShowing() {
        return mShowing;
    }

    public PointF getSize() {
        return new PointF(mWidth, mHeight);
    }

    public void show(boolean show) {
        mShowing = show;
        setVisibility(show ? View.VISIBLE : View.GONE);
        setCenter();
        mPieControl.show(show);
    }

    public boolean isInContentArea(int x, int y) {
        mContentArea.left = mContentFrame.getLeft() + mContentFrame.getPaddingLeft();
        mContentArea.top = mContentFrame.getTop() + mContentFrame.getPaddingTop();
        mContentArea.right = mContentFrame.getRight() - mContentFrame.getPaddingRight();
        mContentArea.bottom = mContentFrame.getBottom() - mContentFrame.getPaddingBottom();
        return mContentArea.contains(x, y);
    }

    @Override
    public void onNavButtonPressed(String buttonName) {
        if (buttonName.equals(PieControl.BACK_BUTTON)) {
            simulateKeypress(KeyEvent.KEYCODE_BACK);
        } else if (buttonName.equals(PieControl.HOME_BUTTON)) {
            simulateKeypress(KeyEvent.KEYCODE_HOME);
        } else if (buttonName.equals(PieControl.MENU_BUTTON)) {
            simulateKeypress(KeyEvent.KEYCODE_MENU);
        } else if (buttonName.equals(PieControl.RECENT_BUTTON)) {
            toggleRecentApps();
        } else if (buttonName.equals(PieControl.SEARCH_BUTTON)) {
            simulateKeypress(KeyEvent.KEYCODE_SEARCH);
        }
    }

    private void toggleRecentApps() {
        Intent intentx = new Intent(Intent.ACTION_MAIN);
        intentx.setClassName("com.cyanmobile.TaskSwitcher", "com.cyanmobile.TaskSwitcher.TaskSwitcherMainActivity");
        intentx.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mContext.startActivity(intentx);
    }

    /**
     * Runnable to hold simulate a keypress.
     *
     * This is executed in a separate Thread to avoid blocking
     */
    private void simulateKeypress(final int keyCode) {
        new Thread(new KeyEventInjector( keyCode ) ).start();
    }

    private class KeyEventInjector implements Runnable {
        private int keyCode;

        KeyEventInjector(final int keyCode) {
            this.keyCode = keyCode;
        }

        public void run() {
            try {
                if (!(IWindowManager.Stub.asInterface(ServiceManager.getService("window")))
                         .injectKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode), true) ) {
                                   Slog.w(TAG, "Key down event not injected");
                                   return;
                              }
                if (!(IWindowManager.Stub.asInterface(ServiceManager.getService("window")))
                         .injectKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode), true) ) {
                                  Slog.w(TAG, "Key up event not injected");
                             }
           } catch (RemoteException ex) {
               Slog.w(TAG, "Error injecting key event", ex);
           }
        }
    }
}
