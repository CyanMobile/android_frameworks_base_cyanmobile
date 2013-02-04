/*
 * Copyright (C) 2011 The Android Open Source Project
 * This code has been modified.  Portions copyright (C) 2010 ParanoidAndroid Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.provider.Settings;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.MotionEvent;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.android.systemui.statusbar.PieControlPanel;
import com.android.systemui.statusbar.pies.PieItem;
import com.android.systemui.statusbar.pies.PieMenu;

import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Quick Controls pie menu
 */
public class PieControl implements OnClickListener {
    public static final String BACK_BUTTON = "##back##";
    public static final String HOME_BUTTON = "##home##";
    public static final String MENU_BUTTON = "##menu##";
    public static final String SEARCH_BUTTON = "##search##";
    public static final String RECENT_BUTTON = "##recent##";
    public static final String SCREEN_BUTTON = "##screens##";
    public static final String POWER_BUTTON = "##power##";
    public static final String LASTAPP_BUTTON = "##lastapp##";
    public static final String SETTING_BUTTON = "##settings##";
    public static final String CLEARALL_BUTTON = "##clearall##";
    public static final String FAKE_BUTTON = "##fake##";

    protected Context mContext;
    protected PieMenu mPie;
    protected int mItemSize;
    protected TextView mTabsCount;
    private PieItem mBack;
    private PieItem mBack1;
    private PieItem mBack2;
    private PieItem mBack3;
    private PieItem mBack4;
    private PieItem mHome;
    private PieItem mHome1;
    private PieItem mHome2;
    private PieItem mHome3;
    private PieItem mHome4;
    private PieItem mMenu;
    private PieItem mMenu1;
    private PieItem mMenu2;
    private PieItem mMenu3;
    private PieItem mMenu4;
    private PieItem mRecent;
    private PieItem mRecent1;
    private PieItem mRecent2;
    private PieItem mRecent3;
    private PieItem mRecent4;
    private PieItem mSearch;
    private PieItem mSearch1;
    private PieItem mSearch2;
    private PieItem mSearch3;
    private PieItem mSearch4;
    private OnNavButtonPressedListener mListener;
    private PieControlPanel mPanel;

    private int mBackVal = 0;
    private int mBackVal1 = 10;
    private int mBackVal2 = 10;
    private int mBackVal3 = 10;
    private int mBackVal4 = 10;
    private int mHomeVal = 1;
    private int mHomeVal1 = 10;
    private int mHomeVal2 = 10;
    private int mHomeVal3 = 10;
    private int mHomeVal4 = 10;
    private int mMenuVal = 2;
    private int mMenuVal1 = 10;
    private int mMenuVal2 = 10;
    private int mMenuVal3 = 10;
    private int mMenuVal4 = 10;
    private int mRecentVal = 3;
    private int mRecentVal1 = 7;
    private int mRecentVal2 = 9;
    private int mRecentVal3 = 10;
    private int mRecentVal4 = 8;
    private int mSearchVal = 4;
    private int mSearchVal1 = 6;
    private int mSearchVal2 = 5;
    private int mSearchVal3 = 10;
    private int mSearchVal4 = 10;

    private Handler mHandler;

    private class MenuObserver extends ContentObserver {
        MenuObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_MENU), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_MENU1), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_MENU2), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_MENU3), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_MENU4), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            ContentResolver resolver = mContext.getContentResolver();
            mMenuVal = 3; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_MENU, 2);
            mMenuVal1 = 0; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_MENU1, 0);
            mMenuVal2 = 1; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_MENU2, 1);
            mMenuVal3 = 4; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_MENU3, 4);
            mMenuVal4 = 2; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_MENU4, 2);
            populateMenus();
        }
    }

    private class SearchObserver extends ContentObserver {
        SearchObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_SEARCH), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_SEARCH1), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_SEARCH2), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_SEARCH3), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_SEARCH4), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            ContentResolver resolver = mContext.getContentResolver();
            mSearchVal = 4; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_SEARCH, 4);
            mSearchVal1 = 6; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_SEARCH1, 6);
            mSearchVal2 = 5; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_SEARCH2, 5);
            mSearchVal3 = 3; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_SEARCH3, 3);
            mSearchVal4 = 0; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_SEARCH4, 0);
            populateSearch();
        }
    }

    private class RecentObserver extends ContentObserver {
        RecentObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_RECENT), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_RECENT1), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_RECENT2), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_RECENT3), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_RECENT4), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            ContentResolver resolver = mContext.getContentResolver();
            mRecentVal = 2; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_RECENT, 3);
            mRecentVal1 = 7; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_RECENT1, 7);
            mRecentVal2 = 9; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_RECENT2, 9);
            mRecentVal3 = 4; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_RECENT3, 4);
            mRecentVal4 = 8; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_RECENT4, 8);
            populateRecent();
        }
    }

    private class HomeObserver extends ContentObserver {
        HomeObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_HOME), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_HOME1), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_HOME2), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_HOME3), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_HOME4), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            ContentResolver resolver = mContext.getContentResolver();
            mHomeVal = 1; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_HOME, 1);
            mHomeVal1 = 0; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_HOME1, 0);
            mHomeVal2 = 2; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_HOME2, 2);
            mHomeVal3 = 4; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_HOME3, 4);
            mHomeVal4 = 3; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_HOME4, 3);
            populateHome();
        }
    }

    private class BackObserver extends ContentObserver {
        BackObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_BACK), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_BACK1), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_BACK2), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_BACK3), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_BUTTON_BACK4), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            ContentResolver resolver = mContext.getContentResolver();
            mBackVal = 0; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_BACK, 0);
            mBackVal1 = 1; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_BACK1, 3);
            mBackVal2 = 2; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_BACK2, 2);
            mBackVal3 = 4; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_BACK3, 4);
            mBackVal4 = 3; //Settings.System.getInt(resolver, Settings.System.PIE_BUTTON_BACK4, 1);
            populateBack();
        }
    }


    public PieControl(Context context, PieControlPanel panel) {
        mContext = context;
        mHandler = new Handler();

        mPanel = panel;
        mItemSize = (int) context.getResources().getDimension(R.dimen.pie_item_size);

        MenuObserver menuObserver = new MenuObserver(mHandler);
        menuObserver.observe();

        SearchObserver searchObserver = new SearchObserver(mHandler);
        searchObserver.observe();

        RecentObserver recentObserver = new RecentObserver(mHandler);
        recentObserver.observe();

        HomeObserver homeObserver = new HomeObserver(mHandler);
        homeObserver.observe();

        BackObserver backObserver = new BackObserver(mHandler);
        backObserver.observe();
    }

    public PieMenu getPieMenu() {
        return mPie;
    }

    public void init() {
        mPie.init();
    }

    public NotificationData setNotifications(NotificationData list) {
        if (mPie != null) {
            mPie.setNotifications(list);
        }
        return list;
    }

    public void setNotifNew(boolean notifnew) {
        if (mPie != null) {
            mPie.setNotifNew(notifnew);
        }
    }

    public void attachToContainer(FrameLayout container) {
        if (mPie == null) {
            mPie = new PieMenu(mContext, mPanel);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);
            mPie.setLayoutParams(lp);
            populateMenu();
        }
        container.addView(mPie);
    }

    public void removeFromContainer(FrameLayout container) {
        container.removeView(mPie);
    }

    public void forceToTop(FrameLayout container) {
        if (mPie.getParent() != null) {
            container.removeView(mPie);
            container.addView(mPie);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mPie.onTouchEvent(event);
    }

    private void populateMenus() {
        mMenu = makeItem(imaged(mMenuVal), 1, mWhois(mMenuVal), false);
        mMenu1 = makeItem(imaged(mMenuVal1), 1, mWhois(mMenuVal1), false);
        mMenu2 = makeItem(imaged(mMenuVal2), 1, mWhois(mMenuVal2), false);
        mMenu3 = makeItem(imaged(mMenuVal3), 1, mWhois(mMenuVal3), true);
        mMenu4 = makeItem(imaged(mMenuVal4), 1, mWhois(mMenuVal4), false);
    }

    private void populateSearch() {
        mSearch = makeItem(imaged(mSearchVal), 1, mWhois(mSearchVal), true);
        mSearch1 = makeItem(imaged(mSearchVal1), 1, mWhois(mSearchVal1), false);
        mSearch2 = makeItem(imaged(mSearchVal2), 1, mWhois(mSearchVal2), false);
        mSearch3 = makeItem(imaged(mSearchVal3), 1, mWhois(mSearchVal3), false);
        mSearch4 = makeItem(imaged(mSearchVal4), 1, mWhois(mSearchVal4), false);
    }

    private void populateRecent() {
        mRecent = makeItem(imaged(mRecentVal), 1, mWhois(mRecentVal), false);
        mRecent1 = makeItem(imaged(mRecentVal1), 1, mWhois(mRecentVal1), false);
        mRecent2 = makeItem(imaged(mRecentVal2), 1, mWhois(mRecentVal2), false);
        mRecent3 = makeItem(imaged(mRecentVal3), 1, mWhois(mRecentVal3), true);
        mRecent4 = makeItem(imaged(mRecentVal4), 1, mWhois(mRecentVal4), false);
    }

    private void populateHome() {
        mHome = makeItem(imaged(mHomeVal), 1, mWhois(mHomeVal), false);
        mHome1 = makeItem(imaged(mHomeVal1), 1, mWhois(mHomeVal1), false);
        mHome2 = makeItem(imaged(mHomeVal2), 1, mWhois(mHomeVal2), false);
        mHome3 = makeItem(imaged(mHomeVal3), 1, mWhois(mHomeVal3), true);
        mHome4 = makeItem(imaged(mHomeVal4), 1, mWhois(mHomeVal4), false);
    }

    private void populateBack() {
        mBack = makeItem(imaged(mBackVal), 1, mWhois(mBackVal), false);
        mBack1 = makeItem(imaged(mBackVal1), 1, mWhois(mBackVal1), false);
        mBack2 = makeItem(imaged(mBackVal2), 1, mWhois(mBackVal2), false);
        mBack3 = makeItem(imaged(mBackVal3), 1, mWhois(mBackVal3), true);
        mBack4 = makeItem(imaged(mBackVal4), 1, mWhois(mBackVal4), false);
    }

    protected void populateMenu() {
        // base
        mPie.addItem(mMenu);
        mMenu.addItem(mMenu3);
        mMenu.addItem(mMenu4);
        mMenu.addItem(mMenu2);
        mMenu.addItem(mMenu1);
        // level 1
        mPie.addItem(mSearch);
        mSearch.addItem(mSearch4);
        mSearch.addItem(mSearch3);
        mSearch.addItem(mSearch2);
        mSearch.addItem(mSearch1);
        // level 2
        mPie.addItem(mRecent);
        mRecent.addItem(mRecent4);
        mRecent.addItem(mRecent3);
        mRecent.addItem(mRecent2);
        mRecent.addItem(mRecent1);
        // level 3
        mPie.addItem(mHome);
        mHome.addItem(mHome4);
        mHome.addItem(mHome3);
        mHome.addItem(mHome2);
        mHome.addItem(mHome1);
        // level 4
        mPie.addItem(mBack);
        mBack.addItem(mBack4);
        mBack.addItem(mBack3);
        mBack.addItem(mBack2);
        mBack.addItem(mBack1);
    }

    private int imaged(int whats) {
       if (whats == 0) {
           return R.drawable.ic_sysbar_back;
       } else if (whats == 1) {
           return R.drawable.ic_sysbar_home;
       } else if (whats == 2) {
           return R.drawable.ic_sysbar_recent;
       } else if (whats == 3) {
           return R.drawable.ic_sysbar_menu;
       } else if (whats == 4) {
           return R.drawable.ic_sysbar_search_side;
       } else if (whats == 5) {
           return R.drawable.ic_sysbar_screenshot;
       } else if (whats == 6) {
           return R.drawable.ic_sysbar_power;
       } else if (whats == 7) {
           return R.drawable.ic_sysbar_lastapp;
       } else if (whats == 8) {
           return R.drawable.ic_sysbar_setts;
       } else if (whats == 9) {
           return R.drawable.ic_sysbar_clearall;
       } else {
           return R.drawable.ic_sysbar_nihil;
       }
    }

    private String mWhois(int whats) {
       if (whats == 0) {
           return BACK_BUTTON;
       } else if (whats == 1) {
           return HOME_BUTTON;
       } else if (whats == 2) {
           return RECENT_BUTTON;
       } else if (whats == 3) {
           return MENU_BUTTON;
       } else if (whats == 4) {
           return SEARCH_BUTTON;
       } else if (whats == 5) {
           return SCREEN_BUTTON;
       } else if (whats == 6) {
           return POWER_BUTTON;
       } else if (whats == 7) {
           return LASTAPP_BUTTON;
       } else if (whats == 8) {
           return SETTING_BUTTON;
       } else if (whats == 9) {
           return CLEARALL_BUTTON;
       } else {
           return FAKE_BUTTON;
       }
    }

    protected PieItem makeFiller() {
        return makeItem(R.drawable.ic_sysbar_nihil, 1, FAKE_BUTTON, false);
    }

    protected PieItem makeTidFiller() {
        return makeItem(R.drawable.ic_sysbar_nihil, 1, FAKE_BUTTON, true);
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onNavButtonPressed((String) v.getTag());
        }
    }

    protected PieItem makeItem(int image, int l, String name, boolean lesser) {
        ImageView view = new ImageView(mContext);
        view.setImageResource(image);
        view.setMinimumWidth(mItemSize);
        view.setMinimumHeight(mItemSize);
        view.setScaleType(ScaleType.CENTER);
        LayoutParams lp = new LayoutParams(mItemSize, mItemSize);
        view.setLayoutParams(lp);
        view.setOnClickListener(this);
        return new PieItem(view, mContext, l, name, lesser);
    }

    public void show(boolean show) {
        mPie.show(show);
    }

    public void setCenter(int x, int y) {
        mPie.setCenter(x, y);
    }

    public void setOnNavButtonPressedListener(OnNavButtonPressedListener listener) {
        mListener = listener;
    }

    public interface OnNavButtonPressedListener {
        public void onNavButtonPressed(String buttonName);
    }
}
