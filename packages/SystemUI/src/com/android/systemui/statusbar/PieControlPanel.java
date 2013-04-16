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
import android.view.Surface;
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
        mDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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

    private static int[] gravityArray = {Gravity.BOTTOM, Gravity.LEFT, Gravity.TOP, Gravity.RIGHT, Gravity.BOTTOM, Gravity.LEFT};
    public static int findGravityOffset(int gravity) {    
        for (int gravityIndex = 1; gravityIndex < gravityArray.length - 2; gravityIndex++) {	
            if (gravity == gravityArray[gravityIndex])
                return gravityIndex;
        }	
        return 4;
    }

    public void configurationChanges() {
        if (Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.PIE_STICK, 0) == 1) {

            // Get original offset
            int gravityIndex = findGravityOffset(convertPieGravitytoGravity(
                    Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.PIE_GRAVITY, 3)));

            // Orient Pie to that place
            reorient(gravityArray[gravityIndex], false);

            // Now re-orient it for landscape orientation
            switch(mDisplay.getRotation()) {
                case Surface.ROTATION_270:
                    reorient(gravityArray[gravityIndex + 1], false);
                    break;
                case Surface.ROTATION_90:
                    reorient(gravityArray[gravityIndex - 1], false);
                    break;
            }
        }

        show(false);
        if (mPieControl != null) mPieControl.configurationChanges();
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

    public void hidePanels(boolean wth) {
        if (mPieControl != null) {
            mPieControl.hidePanels(wth);
        }
    }

    public void setNotifNew(boolean notifnew) {
        if (mPieControl != null) {
            mPieControl.setNotifNew(notifnew);
        }
    }

    public static int convertGravitytoPieGravity(int gravity) {
        switch(gravity) {
            case Gravity.LEFT:  return 0;
            case Gravity.TOP:   return 1;
            case Gravity.RIGHT: return 2;
            default:            return 3;
        }
    }

    public static int convertPieGravitytoGravity(int gravity) {
        switch(gravity) {
            case 0:  return Gravity.LEFT;
            case 1:  return Gravity.TOP;
            case 2:  return Gravity.RIGHT;	
            default: return Gravity.BOTTOM;
        }
    }

    public void reorient(int orientation, boolean storeSetting) {
        mOrientation = orientation;
        WindowManagerImpl.getDefault().removeView(mTrigger);
        WindowManagerImpl.getDefault().addView(mTrigger, StatusBarService.getPieTriggerLayoutParams(mContext, mOrientation));
        show(mShowing);

        if (storeSetting) {
            int gravityOffset = mOrientation;
            if (Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.PIE_STICK, 0) == 1) {

                gravityOffset = findGravityOffset(mOrientation);
                switch(mDisplay.getRotation()) {
                    case Surface.ROTATION_270:
                        gravityOffset = gravityArray[gravityOffset - 1];
                        break;
                    case Surface.ROTATION_90:
                        gravityOffset = gravityArray[gravityOffset + 1];
                        break;
                    default:
                        gravityOffset = mOrientation;
                        break;
                }
            }
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.PIE_GRAVITY, convertGravitytoPieGravity(gravityOffset));
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
        mPieControl.show(show);
    }

    public boolean EnableBack() {
      return mPieControl.EnableBack();
    }

    public boolean EnableHome() {
      return mPieControl.EnableHome();
    }

    public boolean EnableMenu() {
      return mPieControl.EnableMenu();
    }

    public boolean EnableRecent() {
      return mPieControl.EnableRecent();
    }

    public boolean EnableSearch() {
      return mPieControl.EnableSearch();
    }

    // verticalPos == -1 -> center PIE
    public void show(int verticalPos) {
        mShowing = true;
        setVisibility(View.VISIBLE);
        Point outSize = new Point(0,0);
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
