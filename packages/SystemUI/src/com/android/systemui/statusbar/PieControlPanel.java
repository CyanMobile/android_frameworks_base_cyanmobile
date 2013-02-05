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
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
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

import java.util.List;
import java.net.URISyntaxException;

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
        setCenter();
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
            CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_BACK);
        } else if (buttonName.equals(PieControl.HOME_BUTTON)) {
            CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_HOME);
        } else if (buttonName.equals(PieControl.MENU_BUTTON)) {
            CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_MENU);
        } else if (buttonName.equals(PieControl.RECENT_BUTTON)) {
            toggleRecentApps();
        } else if (buttonName.equals(PieControl.SEARCH_BUTTON)) {
            CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_SEARCH);
        } else if (buttonName.equals(PieControl.SCREEN_BUTTON)) {
            toggleScreenshot();
        } else if (buttonName.equals(PieControl.POWER_BUTTON)) {
            togglePowerMenu();
        } else if (buttonName.equals(PieControl.LASTAPP_BUTTON)) {
            toggleLastApp();
        } else if (buttonName.equals(PieControl.SETTING_BUTTON)) {
            toggleSettingsApps();
        } else if (buttonName.equals(PieControl.CLEARALL_BUTTON)) {
            mService.toggleClearNotif();
        } else if (buttonName.equals(PieControl.FAKE_BUTTON)) {
            // do nothing
        } else {
            runCustomApp(buttonName);
        }
    }

    private void runCustomApp(String uri) {
        if (uri != null) {
            try {
                Intent i = Intent.parseUri(uri, 0);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                mContext.startActivity(i);
            } catch (URISyntaxException e) {
            } catch (ActivityNotFoundException e) {
            }
        }
    }

    private void toggleRecentApps() {
        Intent intentx = new Intent(Intent.ACTION_MAIN);
        intentx.setClassName("com.cyanmobile.TaskSwitcher", "com.cyanmobile.TaskSwitcher.TaskSwitcherMainActivity");
        intentx.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mContext.startActivity(intentx);
    }

    private void toggleSettingsApps() {
        Intent intenty = new Intent(android.provider.Settings.ACTION_SETTINGS);
        intenty.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mContext.startActivity(intenty);
    }

    private void togglePowerMenu() {
        Intent intentw = new Intent(Intent.ACTION_POWERMENU);
        mContext.sendBroadcast(intentw);
    }

    private void toggleScreenshot() {
        Intent intentz = new Intent("android.intent.action.SCREENSHOT");
        mContext.sendBroadcast(intentz);
    }

    private void toggleLastApp() {
        int lastAppId = 0;
        int looper = 1;
        String packageName;
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Activity.ACTIVITY_SERVICE);
        String defaultHomePackage = "com.android.launcher";
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
        if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
            defaultHomePackage = res.activityInfo.packageName;
        }
        List <ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(5);
        // lets get enough tasks to find something to switch to
        // Note, we'll only get as many as the system currently has - up to 5
        while ((lastAppId == 0) && (looper < tasks.size())) {
            packageName = tasks.get(looper).topActivity.getPackageName();
            if (!packageName.equals(defaultHomePackage) && !packageName.equals("com.android.systemui")) {
                lastAppId = tasks.get(looper).id;
            }
            looper++;
        }
        if (lastAppId != 0) {
            am.moveTaskToFront(lastAppId, am.MOVE_TASK_NO_USER_ACTION);
        }
    }
}
