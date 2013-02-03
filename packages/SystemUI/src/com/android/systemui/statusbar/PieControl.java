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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
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
    private PieItem mHome;
    private PieItem mMenu;
    private PieItem mRecent;
    private PieItem mSearch;
    private PieItem mScreenshot;
    private PieItem mPower;
    private PieItem mLastapp;
    private PieItem mSettings;
    private PieItem mClears;
    private OnNavButtonPressedListener mListener;
    private PieControlPanel mPanel;

    public PieControl(Context context, PieControlPanel panel) {
        mContext = context;
        mPanel = panel;
        mItemSize = (int) context.getResources().getDimension(R.dimen.pie_item_size);
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

    protected void populateMenu() {
        mBack = makeItem(R.drawable.ic_sysbar_back, 1, BACK_BUTTON, false);
        mHome = makeItem(R.drawable.ic_sysbar_home, 1, HOME_BUTTON, false);
        mRecent = makeItem(R.drawable.ic_sysbar_recent, 1, RECENT_BUTTON, false);
        mMenu = makeItem(R.drawable.ic_sysbar_menu, 1, MENU_BUTTON, false);
        mSearch = makeItem(R.drawable.ic_sysbar_search_side, 1, SEARCH_BUTTON, true);
        mScreenshot = makeItem(R.drawable.ic_sysbar_screenshot, 1, SCREEN_BUTTON, false);
        mPower = makeItem(R.drawable.ic_sysbar_power, 1, POWER_BUTTON, false);
        mLastapp = makeItem(R.drawable.ic_sysbar_lastapp, 1, LASTAPP_BUTTON, false);
        mSettings = makeItem(R.drawable.ic_sysbar_setts, 1, SETTING_BUTTON, false);
        mClears = makeItem(R.drawable.ic_sysbar_clearall, 1, CLEARALL_BUTTON, false);

        // base
        mPie.addItem(mMenu);
        // level 1
        mPie.addItem(mSearch);
        mSearch.addItem(makeFiller());
        mSearch.addItem(makeFiller());
        mSearch.addItem(mScreenshot);
        mSearch.addItem(mPower);
        // level 2
        mPie.addItem(mRecent);
        mRecent.addItem(mSettings);
        mRecent.addItem(makeTidFiller());
        mRecent.addItem(mClears);
        mRecent.addItem(mLastapp);
        // base
        mPie.addItem(mHome);
        mPie.addItem(mBack);
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
