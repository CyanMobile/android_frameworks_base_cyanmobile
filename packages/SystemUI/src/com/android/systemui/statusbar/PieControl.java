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

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Matrix;
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
import java.net.URISyntaxException;

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

    private int mBackVal;
    private int mBackVal1;
    private int mBackVal2;
    private int mBackVal3;
    private int mBackVal4;
    private boolean mBackAllowLevel;
    private boolean mBackApp;
    private String mBackString1;
    private String mBackString2;
    private String mBackString3;
    private String mBackString4;

    private int mHomeVal;
    private int mHomeVal1;
    private int mHomeVal2;
    private int mHomeVal3;
    private int mHomeVal4;
    private boolean mHomeAllowLevel;
    private boolean mHomeApp;
    private String mHomeString1;
    private String mHomeString2;
    private String mHomeString3;
    private String mHomeString4;

    private int mMenuVal;
    private int mMenuVal1;
    private int mMenuVal2;
    private int mMenuVal3;
    private int mMenuVal4;
    private boolean mMenuAllowLevel;
    private boolean mMenuApp;
    private String mMenuString1;
    private String mMenuString2;
    private String mMenuString3;
    private String mMenuString4;

    private int mRecentVal;
    private int mRecentVal1;
    private int mRecentVal2;
    private int mRecentVal3;
    private int mRecentVal4;
    private boolean mRecentAllowLevel;
    private boolean mRecentApp;
    private String mRecentString1;
    private String mRecentString2;
    private String mRecentString3;
    private String mRecentString4;

    private int mSearchVal;
    private int mSearchVal1;
    private int mSearchVal2;
    private int mSearchVal3;
    private int mSearchVal4;
    private boolean mSearchAllowLevel;
    private boolean mSearchApp;
    private String mSearchString1;
    private String mSearchString2;
    private String mSearchString3;
    private String mSearchString4;

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
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_ENABLE_BUTTON_MENU_LEVEL), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_ENABLE_BUTTON_MENU_APP), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_MENU_APP1), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_MENU_APP2), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_MENU_APP3), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_MENU_APP4), false, this);
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
            mMenuAllowLevel = false; //Settings.System.getInt(resolver, Settings.System.PIE_ENABLE_BUTTON_MENU_LEVEL, 0) == 1;
            mMenuApp = false; //Settings.System.getInt(resolver, Settings.System.PIE_ENABLE_BUTTON_MENU_APP, 0) == 1;
            mMenuString1 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_MENU_APP1);
            mMenuString2 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_MENU_APP2);
            mMenuString3 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_MENU_APP3);
            mMenuString4 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_MENU_APP4);
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
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_ENABLE_BUTTON_SEARCH_LEVEL), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_ENABLE_BUTTON_SEARCH_APP), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_SEARCH_APP1), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_SEARCH_APP2), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_SEARCH_APP3), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_SEARCH_APP4), false, this);
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
            mSearchAllowLevel = false; //Settings.System.getInt(resolver, Settings.System.PIE_ENABLE_BUTTON_SEARCH_LEVEL, 0) == 1;
            mSearchApp = false; //Settings.System.getInt(resolver, Settings.System.PIE_ENABLE_BUTTON_SEARCH_APP, 0) == 1;
            mSearchString1 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_SEARCH_APP1);
            mSearchString2 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_SEARCH_APP2);
            mSearchString3 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_SEARCH_APP3);
            mSearchString4 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_SEARCH_APP4);
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
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_ENABLE_BUTTON_RECENT_LEVEL), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_ENABLE_BUTTON_RECENT_APP), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_RECENT_APP1), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_RECENT_APP2), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_RECENT_APP3), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_RECENT_APP4), false, this);
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
            mRecentAllowLevel = false; //Settings.System.getInt(resolver, Settings.System.PIE_ENABLE_BUTTON_RECENT_LEVEL, 0) == 1;
            mRecentApp = false; //Settings.System.getInt(resolver, Settings.System.PIE_ENABLE_BUTTON_RECENT_APP, 0) == 1;
            mRecentString1 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_RECENT_APP1);
            mRecentString2 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_RECENT_APP2);
            mRecentString3 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_RECENT_APP3);
            mRecentString4 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_RECENT_APP4);
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
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_ENABLE_BUTTON_HOME_LEVEL), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_ENABLE_BUTTON_HOME_APP), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_HOME_APP1), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_HOME_APP2), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_HOME_APP3), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_HOME_APP4), false, this);
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
            mHomeAllowLevel = false; //Settings.System.getInt(resolver, Settings.System.PIE_ENABLE_BUTTON_HOME_LEVEL, 0) == 1;
            mHomeApp = false; //Settings.System.getInt(resolver, Settings.System.PIE_ENABLE_BUTTON_HOME_APP, 0) == 1;
            mHomeString1 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_HOME_APP1);
            mHomeString2 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_HOME_APP2);
            mHomeString3 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_HOME_APP3);
            mHomeString4 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_HOME_APP4);
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
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_ENABLE_BUTTON_BACK_LEVEL), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_ENABLE_BUTTON_BACK_APP), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_BACK_APP1), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_BACK_APP2), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_BACK_APP3), false, this);
            //resolver.registerContentObserver(
            //        Settings.System.getUriFor(Settings.System.PIE_CUSTOM_BUTTON_BACK_APP4), false, this);
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
            mBackAllowLevel = false; //Settings.System.getInt(resolver, Settings.System.PIE_ENABLE_BUTTON_BACK_LEVEL, 0) == 1;
            mBackApp = false; //Settings.System.getInt(resolver, Settings.System.PIE_ENABLE_BUTTON_BACK_APP, 0) == 1;
            mBackString1 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_BACK_APP1);
            mBackString2 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_BACK_APP2);
            mBackString3 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_BACK_APP3);
            mBackString4 = null; //Settings.System.getString(resolver, Settings.System.PIE_CUSTOM_BUTTON_BACK_APP4);
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
        if (getAllowApp(mMenuApp, mMenuString1)) {
            mMenu1 = makeItem(getDrawable(mMenuString1), 1, mMenuString1, false);
        } else {
            mMenu1 = makeItem(imaged(mMenuVal1), 1, mWhois(mMenuVal1), false);
        }
        if (getAllowApp(mMenuApp, mMenuString2)) {
            mMenu2 = makeItem(getDrawable(mMenuString2), 1, mMenuString2, false);
        } else {
            mMenu2 = makeItem(imaged(mMenuVal2), 1, mWhois(mMenuVal2), false);
        }
        if (getAllowApp(mMenuApp, mMenuString3)) {
            mMenu3 = makeItem(getDrawable(mMenuString3), 1, mMenuString3, false);
        } else {
            mMenu3 = makeItem(imaged(mMenuVal3), 1, mWhois(mMenuVal3), false);
        }
        if (getAllowApp(mMenuApp, mMenuString4)) {
            mMenu4 = makeItem(getDrawable(mMenuString4), 1, mMenuString4, false);
        } else {
            mMenu4 = makeItem(imaged(mMenuVal4), 1, mWhois(mMenuVal4), false);
        }

        if (mMenuAllowLevel) {
            mMenu.addItem(mMenu4);
            mMenu.addItem(mMenu3);
            mMenu.addItem(mMenu2);
            mMenu.addItem(mMenu1);
        } else {
            mMenu.resetItem();
        }
    }

    private void populateSearch() {
        mSearch = makeItem(imaged(mSearchVal), 1, mWhois(mSearchVal), false);
        if (getAllowApp(mSearchApp, mSearchString1)) {
            mSearch1 = makeItem(getDrawable(mSearchString1), 1, mSearchString1, false);
        } else {
            mSearch1 = makeItem(imaged(mSearchVal1), 1, mWhois(mSearchVal1), false);
        }
        if (getAllowApp(mSearchApp, mSearchString2)) {
            mSearch2 = makeItem(getDrawable(mSearchString2), 1, mSearchString2, false);
        } else {
            mSearch2 = makeItem(imaged(mSearchVal2), 1, mWhois(mSearchVal2), false);
        }
        if (getAllowApp(mSearchApp, mSearchString3)) {
            mSearch3 = makeItem(getDrawable(mSearchString3), 1, mSearchString3, false);
        } else {
            mSearch3 = makeItem(imaged(mSearchVal3), 1, mWhois(mSearchVal3), false);
        }
        if (getAllowApp(mSearchApp, mSearchString4)) {
            mSearch4 = makeItem(getDrawable(mSearchString4), 1, mSearchString4, false);
        } else {
            mSearch4 = makeItem(imaged(mSearchVal4), 1, mWhois(mSearchVal4), false);
        }

        if (mSearchAllowLevel) {
            mSearch.addItem(mSearch4);
            mSearch.addItem(mSearch3);
            mSearch.addItem(mSearch2);
            mSearch.addItem(mSearch1);
        } else {
            mSearch.resetItem();
        }
    }

    private void populateRecent() {
        mRecent = makeItem(imaged(mRecentVal), 1, mWhois(mRecentVal), false);
        if (getAllowApp(mRecentApp, mRecentString1)) {
            mRecent1 = makeItem(getDrawable(mRecentString1), 1, mRecentString1, false);
        } else {
            mRecent1 = makeItem(imaged(mRecentVal1), 1, mWhois(mRecentVal1), false);
        }
        if (getAllowApp(mRecentApp, mRecentString2)) {
            mRecent2 = makeItem(getDrawable(mRecentString2), 1, mRecentString2, false);
        } else {
            mRecent2 = makeItem(imaged(mRecentVal2), 1, mWhois(mRecentVal2), false);
        }
        if (getAllowApp(mRecentApp, mRecentString3)) {
            mRecent3 = makeItem(getDrawable(mRecentString3), 1, mRecentString3, false);
        } else {
            mRecent3 = makeItem(imaged(mRecentVal3), 1, mWhois(mRecentVal3), false);
        }
        if (getAllowApp(mRecentApp, mRecentString4)) {
            mRecent4 = makeItem(getDrawable(mRecentString4), 1, mRecentString4, false);
        } else {
            mRecent4 = makeItem(imaged(mRecentVal4), 1, mWhois(mRecentVal4), false);
        }

        if (mRecentAllowLevel) {
            mRecent.addItem(mRecent4);
            mRecent.addItem(mRecent3);
            mRecent.addItem(mRecent2);
            mRecent.addItem(mRecent1);
        } else {
            mRecent.resetItem();
        }
    }

    private void populateHome() {
        mHome = makeItem(imaged(mHomeVal), 1, mWhois(mHomeVal), false);
        if (getAllowApp(mHomeApp, mHomeString1)) {
            mHome1 = makeItem(getDrawable(mHomeString1), 1, mHomeString1, false);
        } else {
            mHome1 = makeItem(imaged(mHomeVal1), 1, mWhois(mHomeVal1), false);
        }
        if (getAllowApp(mHomeApp, mHomeString2)) {
            mHome2 = makeItem(getDrawable(mHomeString2), 1, mHomeString2, false);
        } else {
            mHome2 = makeItem(imaged(mHomeVal2), 1, mWhois(mHomeVal2), false);
        }
        if (getAllowApp(mHomeApp, mHomeString3)) {
            mHome3 = makeItem(getDrawable(mHomeString3), 1, mHomeString3, false);
        } else {
            mHome3 = makeItem(imaged(mHomeVal3), 1, mWhois(mHomeVal3), false);
        }
        if (getAllowApp(mHomeApp, mHomeString4)) {
            mHome4 = makeItem(getDrawable(mHomeString4), 1, mHomeString4, false);
        } else {
            mHome4 = makeItem(imaged(mHomeVal4), 1, mWhois(mHomeVal4), false);
        }

        if (mHomeAllowLevel) {
            mHome.addItem(mHome4);
            mHome.addItem(mHome3);
            mHome.addItem(mHome2);
            mHome.addItem(mHome1);
        } else {
            mHome.resetItem();
        }
    }

    private void populateBack() {
        mBack = makeItem(imaged(mBackVal), 1, mWhois(mBackVal), false);
        if (getAllowApp(mBackApp, mBackString1)) {
            mBack1 = makeItem(getDrawable(mBackString1), 1, mBackString1, false);
        } else {
            mBack1 = makeItem(imaged(mBackVal1), 1, mWhois(mBackVal1), false);
        }
        if (getAllowApp(mBackApp, mBackString2)) {
            mBack2 = makeItem(getDrawable(mBackString2), 1, mBackString2, false);
        } else {
            mBack2 = makeItem(imaged(mBackVal2), 1, mWhois(mBackVal2), false);
        }
        if (getAllowApp(mBackApp, mBackString3)) {
            mBack3 = makeItem(getDrawable(mBackString3), 1, mBackString3, false);
        } else {
            mBack3 = makeItem(imaged(mBackVal3), 1, mWhois(mBackVal3), false);
        }
        if (getAllowApp(mBackApp, mBackString4)) {
            mBack4 = makeItem(getDrawable(mBackString4), 1, mBackString4, false);
        } else {
            mBack4 = makeItem(imaged(mBackVal4), 1, mWhois(mBackVal4), false);
        }

        if (mBackAllowLevel) {
            mBack.addItem(mBack4);
            mBack.addItem(mBack3);
            mBack.addItem(mBack2);
            mBack.addItem(mBack1);
        } else {
            mBack.resetItem();
        }
    }

    private boolean getAllowApp(boolean allow, String uri) {
        if (allow && uri != null) {
            return true;
        }
        return false;
    }

    public void populateMenu() {
        // base
        mPie.addItem(mMenu);
        // level 1
        mPie.addItem(mSearch);
        // level 2
        mPie.addItem(mRecent);
        // level 3
        mPie.addItem(mHome);
        // level 4
        mPie.addItem(mBack);
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

    private Drawable getDrawable(String uri) {
        if (uri != null) {
            try {
                Intent i = Intent.parseUri(uri, 0);
                PackageManager pm = mContext.getPackageManager();
                ActivityInfo ai = i.resolveActivityInfo(pm, PackageManager.GET_ACTIVITIES);
                if (ai != null) {
                    return ai.loadIcon(pm);
                }
            } catch (URISyntaxException e) {
            }
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onNavButtonPressed((String) v.getTag());
        }
    }

    public PieItem makeItem(Drawable image, int l, String name, boolean lesser) {
        ImageView view = new ImageView(mContext);
        view.setImageResource(0);
        view.setImageDrawable(image);
        view.setMinimumWidth(mItemSize);
        view.setMinimumHeight(mItemSize);
        view.setScaleType(ScaleType.CENTER);
        LayoutParams lp = new LayoutParams(mItemSize, mItemSize);
        view.setLayoutParams(lp);
        view.setOnClickListener(this);
        return new PieItem(view, mContext, l, name, lesser);
    }

    public PieItem makeItem(int image, int l, String name, boolean lesser) {
        ImageView view = new ImageView(mContext);
        view.setImageDrawable(null);
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
