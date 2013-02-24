package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;

public class SettingsTile extends QuickSettingsTile {

    public SettingsTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mDrawable = R.drawable.ic_qs_settings;
        mLabel = context.getString(R.string.quick_settings_settings_label);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSettingsActivity(android.provider.Settings.ACTION_SETTINGS);
            }
        };
    }
}
