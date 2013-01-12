package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;

public class TimeTile extends QuickSettingsTile {

    public TimeTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mTileLayout = R.layout.quick_settings_tile_time;

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.deskclock", "com.android.deskclock.DeskClock");
                startSettingsActivity(intent);
            }
        };

        if (enableFlip()) mHandler.postDelayed(mResetFlip, 10000); //10 second
    }

    Runnable mResetFlip = new Runnable() {
        public void run() {
            flipTile();
            if (enableFlip()) mHandler.postDelayed(mResetFlip, 10000); //10 second
        }
    };

    @Override
    void updateQuickSettings(){}
}
