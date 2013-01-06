package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.BatteryManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;

public class BatteryTile extends QuickSettingsTile {

    private boolean charging = false;
    private int batteryLevel = 0;
    private Drawable batteryIcon;

    private LevelListDrawable batteryLevels;
    private LevelListDrawable chargingBatteryLevels;

    public BatteryTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mTileLayout = R.layout.quick_settings_tile_battery;
        batteryLevels = (LevelListDrawable) mContext.getResources().getDrawable(R.drawable.qs_sys_battery);
        chargingBatteryLevels = (LevelListDrawable) mContext.getResources().getDrawable(R.drawable.qs_sys_battery_charging);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSettingsActivity(Intent.ACTION_POWER_USAGE_SUMMARY);
            }
        };

        qsc.registerAction(Intent.ACTION_BATTERY_CHANGED, this);
    }

    @Override
    void onPostCreate() {
        applyBatteryChanges();
        super.onPostCreate();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            applyBatteryChanges();
        }
    }

    void applyBatteryChanges() {
        batteryIcon = charging
                ? chargingBatteryLevels :
                    batteryLevels;
        if (batteryLevel == 100) {
            mLabel = mContext.getString(R.string.quick_settings_battery_charged_label);
        } else {
            mLabel = charging
                    ? mContext.getString(R.string.quick_settings_battery_charging_label,
                            batteryLevel)
                    : mContext.getString(R.string.status_bar_settings_battery_meter_format,
                            batteryLevel);
        }
        updateQuickSettings();
    }

    @Override
    void updateQuickSettings() {
        TextView tv = (TextView) mTile.findViewById(R.id.battery_textview);
        tv.setText(mLabel);
        ImageView iv = (ImageView) mTile.findViewById(R.id.battery_image);
        iv.setImageDrawable(batteryIcon);
        iv.setImageLevel(batteryLevel);
        flipTile();
    }

}
