package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;

public class AirplaneModeTile extends QuickSettingsTile {

    private boolean mEnabled = false;

    public AirplaneModeTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        getAirState();

        mOnClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
             // Change the system setting
                Settings.System.putInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON,
                                        !mEnabled ? 1 : 0);

                // Post the intent
                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", !mEnabled);
                mContext.sendBroadcast(intent);
            }
        };
        mOnLongClick = new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                return true;
            }
        };

        qsc.registerAction(Intent.ACTION_AIRPLANE_MODE_CHANGED, this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)){
            getAirState();
        }
        applyAirChanges();
    }

    private void applyAirChanges() {
        if (mEnabled) {
            mDrawable = R.drawable.ic_qs_airplane_on;
            mLabel = mContext.getString(R.string.quick_settings_airplane_on_label);
        } else {
            mDrawable = R.drawable.ic_qs_airplane_off;
            mLabel = mContext.getString(R.string.quick_settings_airplane_off_label);
        }
        updateQuickSettings();
    }

    @Override
    void onPostCreate() {
        getAirState();
        applyAirChanges();
        super.onPostCreate();
    }

    private void getAirState() {
        mEnabled = (Settings.System.getInt(mContext.getContentResolver(),
                 Settings.System.AIRPLANE_MODE_ON,0) == 1);
    }

}
