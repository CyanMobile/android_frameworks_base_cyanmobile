/*
 * Copyright (C) 2012 Sven Dawitz for the CyanogenMod Project
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

package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.CmStatusBarView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;

public class PowerMenuTile extends QuickSettingsTile {
    public PowerMenuTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container,
            QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mLabel = mContext.getString(R.string.quick_settings_powermenu_label);
        mDrawable = R.drawable.ic_qs_power_off;

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CmStatusBarView.togglePowerMenu(mContext);
                startCollapseActivity();
            }
        };
        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent=new Intent(Intent.ACTION_POWERMENU_REBOOT);
                mContext.sendBroadcast(intent);
                return true;
            }
        };
    }
}
