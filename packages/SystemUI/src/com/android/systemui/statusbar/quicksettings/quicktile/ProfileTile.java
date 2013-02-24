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

import android.app.AlertDialog;
import android.app.ProfileManager;
import android.app.Profile;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.content.DialogInterface;
import android.view.WindowManager;

import com.android.server.ProfileManagerService;
import com.android.systemui.R;
import com.android.systemui.statusbar.CmStatusBarView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;

import java.util.UUID;

public class ProfileTile extends QuickSettingsTile {

    private ProfileManager mProfileManager;
    private Profile mChosenProfile;

    public ProfileTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container,
            QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mTileLayout = R.layout.quick_settings_tile_profile;

        mDrawable = com.android.internal.R.drawable.stat_cm_profile_bg;

        mProfileManager = (ProfileManager) mContext.getSystemService(Context.PROFILE_SERVICE);
        mLabel = mProfileManager.getActiveProfile().getName();

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCollapseActivity();
                showProfileDialog();
            }
        };
        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                CmStatusBarView.runSettings("com.android.settings.ProfileList", mContext);
                startCollapseActivity();
                return true;
            }
        };
        qsc.registerAction(ProfileManagerService.INTENT_ACTION_PROFILE_SELECTED, this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ProfileManagerService.INTENT_ACTION_PROFILE_SELECTED)) {
            // set profile vars
            mLabel = intent.getStringExtra("name");
            updateQuickSettings();
        }
    }

    @Override
    void updateQuickSettings() {
        TextView tv = (TextView) mTile.findViewById(R.id.user_textview);
        tv.setText(mLabel);
    }

    private void showProfileDialog() {
        final ProfileManager profileManager = (ProfileManager)mContext.getSystemService(Context.PROFILE_SERVICE);

        final Profile[] profiles = profileManager.getProfiles();
        UUID activeProfile = profileManager.getActiveProfile().getUuid();
        final CharSequence[] names = new CharSequence[profiles.length];

        int i=0;
        int checkedItem = 0;
        for(Profile profile : profiles){
            if(profile.getUuid().equals(activeProfile)){
                checkedItem = i;
                mChosenProfile = profile;
            }
            names[i++] = profile.getName();
        }

        final AlertDialog.Builder ab = new AlertDialog.Builder(mContext);

        AlertDialog dialog = ab
                .setSingleChoiceItems(names, checkedItem, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < 0)
                            return;
                        mChosenProfile = profiles[which];
                    }
                })
                .setPositiveButton(com.android.internal.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                profileManager.setActiveProfile(mChosenProfile.getUuid());
                            }
                        })
                .setNegativeButton(com.android.internal.R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        dialog.show();
    }
}
