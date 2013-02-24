package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.CmStatusBarView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;

public class ScreenshotTile extends QuickSettingsTile {

    private static final String TAG = "ScreenShotButton";
    Context mContext;

    public ScreenshotTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mContext = context;

        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                 CmStatusBarView.toggleScreenshot(mContext);
            }
        };

        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                CmStatusBarView.toggleScreenshot(mContext);
                startCollapseActivity();
                return true;
            }
        };
    }

    void applyScreenShotChanges() {
        mDrawable = R.drawable.ic_qs_screenshot;
        mLabel = mContext.getString(R.string.quick_settings_screenshot);
        updateQuickSettings();
    }

    @Override
    void onPostCreate() {
        applyScreenShotChanges();
        super.onPostCreate();
    }
}
