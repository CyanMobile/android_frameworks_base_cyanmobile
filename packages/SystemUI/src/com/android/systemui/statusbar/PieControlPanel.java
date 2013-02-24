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

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.widget.FrameLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.PieControl.OnNavButtonPressedListener;

public class PieControlPanel extends FrameLayout implements OnNavButtonPressedListener{

    private static final boolean DEBUG = false;
    private static final String TAG = "PieView";

    private Handler mHandler;
    boolean mShowing;
    private PieControl mPieControl;
    private Context mContext;
    private int mOrientation;
    private int mWidth;
    private int mHeight;
    private View mTrigger;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    private StatusBarService mService;

    private ViewGroup mContentFrame;
    private Rect mContentArea = new Rect();

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

    public void init(Handler h, StatusBarService mServices, View trigger, int orientation) {
        mHandler = h;
        mService = (StatusBarService) mServices;
        mTrigger = trigger;
        mOrientation = orientation;
        mPieControl.init();
    }

    public StatusBarService getBar() {
        return mService;
    }

    public NotificationData setNotifications(NotificationData list) {
        if (mPieControl != null) {
            mPieControl.setNotifications(list);
        }
        return list;
    }

    public void setNotifNew(boolean notifnew) {
        if (mPieControl != null) {
            mPieControl.setNotifNew(notifnew);
        }
    }

    public void reorient(int orientation) {
        mOrientation = orientation;
        WindowManagerImpl.getDefault().removeView(mTrigger);
        WindowManagerImpl.getDefault().addView(mTrigger, StatusBarService.getPieTriggerLayoutParams(mContext, mOrientation));
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
        mPieControl.show(show);
    }

    // verticalPos == -1 -> center PIE
    public void show(int verticalPos) {
        mShowing = true;
        setVisibility(View.VISIBLE);
        Point outSize = new Point(0,0);
        mDisplay = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mDisplay.getMetrics(mDisplayMetrics);
        mWidth = mDisplayMetrics.widthPixels;
        mHeight = mDisplayMetrics.heightPixels;
        switch(mOrientation) {
            case Gravity.LEFT:
                mPieControl.setCenter(0, (verticalPos != -1 ? verticalPos : mHeight / 2));
                break;
            case Gravity.TOP:
                mPieControl.setCenter((verticalPos != -1 ? verticalPos : mWidth / 2), 0);
                break;
            case Gravity.RIGHT:
                mPieControl.setCenter(mWidth, (verticalPos != -1 ? verticalPos : mHeight / 2));
                break;
            case Gravity.BOTTOM: 
                mPieControl.setCenter((verticalPos != -1 ? verticalPos : mWidth / 2), mHeight);
                break;
        }
        mPieControl.show(true);
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
            CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_BACK);
        } else if (buttonName.equals(PieControl.HOME_BUTTON)) {
            CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_HOME);
        } else if (buttonName.equals(PieControl.MENU_BUTTON)) {
            CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_MENU);
        } else if (buttonName.equals(PieControl.RECENT_BUTTON)) {
            CmStatusBarView.toggleRecentApps(mContext);
        } else if (buttonName.equals(PieControl.SEARCH_BUTTON)) {
            CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_SEARCH);
        } else if (buttonName.equals(PieControl.SCREEN_BUTTON)) {
            CmStatusBarView.toggleScreenshot(mContext);
        } else if (buttonName.equals(PieControl.POWER_BUTTON)) {
            CmStatusBarView.togglePowerMenu(mContext);
        } else if (buttonName.equals(PieControl.LASTAPP_BUTTON)) {
            CmStatusBarView.toggleLastApp(mContext);
        } else if (buttonName.equals(PieControl.SETTING_BUTTON)) {
            CmStatusBarView.toggleSettingsApps(mContext);
        } else if (buttonName.equals(PieControl.CLEARALL_BUTTON)) {
            mService.toggleClearNotif();
        } else if (buttonName.equals(PieControl.FAKE_BUTTON)) {
            // do nothing
        } else {
            CmStatusBarView.runCustomApp(buttonName, mContext);
        }
    }

}
