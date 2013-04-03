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

package com.android.systemui.statusbar;

import android.animationing.ValueAnimator;
import android.animationing.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.provider.Settings;
import android.view.animation.DecelerateInterpolator;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;

public class PieStatusPanel {

    public static final int NOTIFICATIONS_PANEL = 0;
    public static final int QUICK_SETTINGS_PANEL = 1;

    private Context mContext;
    private PieControlPanel mPanel;
    private ScrollView mScrollView;
    private View mContentFrame;
    private QuickSettingsContainerView mQS;
    private LinearLayout mNotificationPanel;
    private ViewGroup[] mPanelParents = new ViewGroup[2];

    private int mCurrentViewState = -1;
    private int mFlipViewState = -1;

    public PieStatusPanel(Context context, PieControlPanel panel) {
        mContext = context;
        mPanel = panel;

        if (Settings.System.getInt(context.getContentResolver(), Settings.System.EXPANDED_VIEW_WIDGET, 5) == 5) {
            mNotificationPanel = mPanel.getBar().getNotificationLinearLayout();
            mNotificationPanel.setTag(NOTIFICATIONS_PANEL);
            mQS = mPanel.getBar().getQuickSettingsPanel();
            mQS.setTag(QUICK_SETTINGS_PANEL);

            mPanelParents[NOTIFICATIONS_PANEL] = (ViewGroup) mNotificationPanel.getParent();
            mPanelParents[QUICK_SETTINGS_PANEL] = (ViewGroup) mQS.getParent();

            mContentFrame = (View) mPanel.getBar().mContainer.findViewById(R.id.content_frame);
            mScrollView = (ScrollView) mPanel.getBar().mContainer.findViewById(R.id.content_scroll);
            mScrollView.setOnTouchListener(new ViewOnTouchListener());
            mContentFrame.setOnTouchListener(new ViewOnTouchListener());
        } else {
            mNotificationPanel = null;
            mQS = null;
        }
        mPanel.getBar().mContainer.setVisibility(View.GONE);
    }

    private class ViewOnTouchListener implements OnTouchListener {
        final int SCROLLING_DISTANCE_TRIGGER = 100;
            float scrollX;
            float scrollY;
            boolean hasScrolled;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        scrollX = event.getX();
                        scrollY = event.getY();
                        hasScrolled = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float distanceY = Math.abs(event.getY() - scrollY);
                        float distanceX = Math.abs(event.getX() - scrollX);
                        if(distanceY > SCROLLING_DISTANCE_TRIGGER ||
                            distanceX > SCROLLING_DISTANCE_TRIGGER) {
                            hasScrolled = true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if(!hasScrolled) {
                            hidePanels(true);
                        }
                        break;
                }
                return false;
            }                  
    }

    public int getFlipViewState() {
        return mFlipViewState;
    }

    public void setFlipViewState(int state) {
        mFlipViewState = state;
    }

    public int getCurrentViewState() {
        return mCurrentViewState;
    }

    public void setCurrentViewState(int state) {
        mCurrentViewState = state;
    }

    public void hidePanels(boolean reset) {
        if (mCurrentViewState == NOTIFICATIONS_PANEL) {
            if (mNotificationPanel != null) hidePanel(mNotificationPanel);
        } else if (mCurrentViewState == QUICK_SETTINGS_PANEL) {
            if (mQS != null) hidePanel(mQS);
        }
        if (reset) mCurrentViewState = -1;
    }

    public void swapPanels() {
        hidePanels(false);
        if (mCurrentViewState == NOTIFICATIONS_PANEL) {
            mCurrentViewState = QUICK_SETTINGS_PANEL;
            if (mQS != null) showPanel(mQS);
        } else if (mCurrentViewState == QUICK_SETTINGS_PANEL) {
            mCurrentViewState = NOTIFICATIONS_PANEL;
            if (mNotificationPanel != null) showPanel(mNotificationPanel);
        }
    }

    private ViewGroup getPanelParent(View panel) {
        if (((Integer)panel.getTag()).intValue() == NOTIFICATIONS_PANEL) {
            return mPanelParents[NOTIFICATIONS_PANEL];
        } else {
            return mPanelParents[QUICK_SETTINGS_PANEL];
        }
    }

    public void showTilesPanel() {
        if (mQS != null) {
            mPanel.getBar().setIsFullExpanded(true);
            showPanel(mQS);
        }
    }

    public void showNotificationsPanel() {
        if (mNotificationPanel != null) showPanel(mNotificationPanel);
    }

    public void hideTilesPanel() {
        if (mQS != null) {
            mPanel.getBar().setIsFullExpanded(false);
            hidePanel(mQS);
        }
    }

    public void hideNotificationsPanel() {
        if (mNotificationPanel != null) hidePanel(mNotificationPanel);
    }

    private void showPanel(View panel) {
        final boolean quickPanel = (((Integer)panel.getTag()).intValue() == QUICK_SETTINGS_PANEL);
        mContentFrame.setBackgroundColor(0);
        ValueAnimator alphAnimation  = ValueAnimator.ofInt(0, 1);
        alphAnimation.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int setsX = ((int)((1-animation.getAnimatedFraction()) * mPanel.getWidth() * 1.5));
                mScrollView.setX(quickPanel ? -setsX : setsX);
                mContentFrame.setBackgroundColor((int)(animation.getAnimatedFraction() * 0xB0) << 24);
                mPanel.invalidate();
            }
        });
        alphAnimation.setDuration(600);
        alphAnimation.setInterpolator(new DecelerateInterpolator());
        alphAnimation.start();

        ViewGroup parent = getPanelParent(panel);
        parent.removeAllViews();
        mScrollView.removeAllViews();
        mScrollView.addView(panel);
        updateContainer(true);
    }

    private void hidePanel(View panel) {
        ViewGroup parent = getPanelParent(panel);
        mScrollView.removeAllViews();
        parent.removeAllViews();
        parent.addView(panel, panel.getLayoutParams());
        updateContainer(false);
    }

    private void updateContainer(boolean visible) {
        mPanel.getBar().mContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        updatePanelConfiguration();
    }

    public void updatePanelConfiguration() {
        if ((mQS == null) || (mNotificationPanel == null)) return;
        int padding = mContext.getResources().getDimensionPixelSize(R.dimen.pie_panel_padding);
        mScrollView.setPadding(padding,0,padding,0);
    }
}
