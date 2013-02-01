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

import android.content.Context;
import android.view.View;

import com.android.systemui.R;

public class PieStatusPanel {

    public static final int NOTIFICATIONS_PANEL = 0;
    public static final int QUICK_SETTINGS_PANEL = 1;

    private Context mContext;
    private PieControlPanel mPanel;

    private int mCurrentViewState = -1;
    private int mFlipViewState = -1;

    public PieStatusPanel(Context context, PieControlPanel panel) {
        mContext = context;
        mPanel = panel;
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
        if (reset) mCurrentViewState = -1;
    }

    public void swapPanels() {
        if (mCurrentViewState == NOTIFICATIONS_PANEL) {
            mCurrentViewState = QUICK_SETTINGS_PANEL;
            mPanel.getBar().toggleNotif();
        } else if (mCurrentViewState == QUICK_SETTINGS_PANEL) {
            mCurrentViewState = NOTIFICATIONS_PANEL;
            mPanel.getBar().togglePower();
        }
    }

    public void showTilesPanel() {
        mPanel.getBar().animateExpand();
        mPanel.getBar().toggleNotif();
    }

    public void showNotificationsPanel() {
        mPanel.getBar().animateExpand();
        mPanel.getBar().togglePower();
    }
}
