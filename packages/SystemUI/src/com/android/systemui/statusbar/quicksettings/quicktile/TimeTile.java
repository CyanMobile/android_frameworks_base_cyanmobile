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

        mHandler.removeCallbacks(mResetFlip);
        mHandler.postDelayed(mResetFlip, 10000); //10 second
    }

    private Runnable mResetFlip = new Runnable() {
        public void run() {
            flipTile();
            mHandler.removeCallbacks(mResetFlip);
            mHandler.postDelayed(mResetFlip, 10000); //10 second
        }
    };

    @Override
    void updateQuickSettings(){}
}
