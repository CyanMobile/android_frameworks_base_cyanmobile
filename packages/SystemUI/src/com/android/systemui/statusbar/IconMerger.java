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

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.widget.LinearLayout;
import android.util.DisplayMetrics;
import android.os.Handler;
import android.database.ContentObserver;

import com.android.internal.statusbar.StatusBarIcon;

import com.android.systemui.R;


public class IconMerger extends LinearLayout {
    private static final String TAG = "IconMerger";
    private Handler mHandler;
    private int mIconSize;
    private StatusBarIconView mMoreView;
    private StatusBarIcon mMoreIcon = new StatusBarIcon(null, R.drawable.stat_notify_more, 0);

    private boolean mClockCenter;
    private boolean mCarrierCenter;
    private boolean mLogoCenter;
    private boolean mStatusBarReverse;
    private boolean mAttached;
    private SettingsObserver mSettingsObserver;

    // observes changes in system battery settings and enables/disables view accordingly
    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = getContext().getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUSBAR_ICONS_SIZE), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CARRIER), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.CARRIER_LOGO), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_REVERSE), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public IconMerger(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler);
        updateSettings();

        mMoreView = new StatusBarIconView(context, "more");
        mMoreView.set(mMoreIcon);
        addView(mMoreView, 0, new LinearLayout.LayoutParams(mIconSize, mIconSize));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            mSettingsObserver.observe();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().getContentResolver().unregisterContentObserver(mSettingsObserver);
            mAttached = false;
        }
    }

    public void addView(StatusBarIconView v, int index) {
        if (index == 0) {
            throw new RuntimeException("Attempt to put view before the more view: " + v);
        }
        addView(v, index, new LinearLayout.LayoutParams(mIconSize, mIconSize));
    }

    private void updateSettings() {
        int defValuesIconSize = getContext().getResources().getInteger(com.android.internal.R.integer.config_iconsize_default_cyanmobile);
        float mIconSizeval = (float) Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.STATUSBAR_ICONS_SIZE, defValuesIconSize);
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        int IconSizepx = (int) (metrics.density * mIconSizeval);
        mIconSize = IconSizepx;
        mClockCenter = (Settings.System.getInt(getContext().getContentResolver(), Settings.System.STATUS_BAR_CLOCK, 1) == 2);
        mCarrierCenter = (Settings.System.getInt(getContext().getContentResolver(), Settings.System.STATUS_BAR_CARRIER, 6) == 2);
        mLogoCenter = (Settings.System.getInt(getContext().getContentResolver(), Settings.System.CARRIER_LOGO, 0) == 2);
        mStatusBarReverse = (Settings.System.getInt(getContext().getContentResolver(), Settings.System.STATUS_BAR_REVERSE, 0) == 1);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

	if (mClockCenter || mCarrierCenter || mLogoCenter) {
            r = (mStatusBarReverse ? ((((LinearLayout) this.getParent()).getLeft() / 2) - 15) : ((((LinearLayout) this.getParent()).getRight() / 2) - 15));
        }

        final int maxWidth = r - l;
        final int N = getChildCount();
        int i;

        // get the rightmost one, and see if we even need to do anything
        int fitRight = -1;
        for (i=N-1; i>=0; i--) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                fitRight = (mStatusBarReverse ? child.getLeft() : child.getRight());
                break;
            }
        }

        // find the first visible one that isn't the more icon
        final StatusBarIconView moreView = mMoreView;
        int fitLeft = -1;
        int startIndex = -1;
        for (i=0; i<N; i++) {
            final View child = getChildAt(i);
            if (child == moreView) {
                startIndex = i+1;
            }
            else if (child.getVisibility() != GONE) {
                fitLeft = (mStatusBarReverse ? child.getRight() : child.getLeft());
                break;
            }
        }

        if (moreView == null || startIndex < 0) {
            return;
            /*
            throw new RuntimeException("Status Bar / IconMerger moreView == " + moreView
                    + " startIndex=" + startIndex);
            */
        }
        
        // if it fits without the more icon, then hide the more icon and update fitLeft
        // so everything gets pushed left
        int adjust = 0;
        if (fitRight - fitLeft <= maxWidth) {
            adjust = fitLeft - (mStatusBarReverse ? moreView.getRight() : moreView.getLeft());
            fitLeft -= adjust;
            fitRight -= adjust;
            moreView.layout(0, moreView.getTop(), 0, moreView.getBottom());
        }
        int extra = fitRight - r;
        int shift = -1;

        int breakingPoint = fitLeft + extra + adjust;
        int number = 0;
        for (i=startIndex; i<N; i++) {
            final StatusBarIconView child = (StatusBarIconView)getChildAt(i);
            if (child.getVisibility() != GONE) {
                int childLeft = (mStatusBarReverse ? child.getRight() : child.getLeft());
                int childRight = (mStatusBarReverse ? child.getLeft() : child.getRight());
                if (childLeft < breakingPoint) {
                    // hide this one
                    child.layout(0, child.getTop(), 0, child.getBottom());
                    int n = child.getStatusBarIcon().number;
                    if (n == 0) {
                        number += 1;
                    } else if (n > 0) {
                        number += n;
                    }
                } else {
                    // decide how much to shift by
                    if (shift < 0) {
                        shift = childLeft - fitLeft;
                    }
                    // shift this left by shift
                    child.layout(childLeft-shift, child.getTop(),
                                    childRight-shift, child.getBottom());
                }
            }
        }

        mMoreIcon.number = number;
        mMoreView.set(mMoreIcon);
    }
}
