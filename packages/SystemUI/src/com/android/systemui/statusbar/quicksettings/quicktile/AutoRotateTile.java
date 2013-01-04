package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;

public class AutoRotateTile extends QuickSettingsTile {

    private static final String TAG = "AutoRotateButton";
    Context mContext;

    public AutoRotateTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mContext = context;

        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                     Settings.System.putInt(
                         mContext.getContentResolver(),
                             Settings.System.ACCELEROMETER_ROTATION, getAutoRotation() ? 0 : 1);
                applyAutoRotationChanges();
            }
        };

        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity(Settings.ACTION_DISPLAY_SETTINGS);
                return true;
            }
        };
        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION)
                , this);
    }

    void applyAutoRotationChanges() {
        if(getAutoRotation()){
            mDrawable = R.drawable.ic_qs_rotation_locked;
            mLabel = mContext.getString(R.string.quick_settings_rotation_unlocked_label);
        } else {
            mDrawable = R.drawable.ic_qs_auto_rotate;
            mLabel = mContext.getString(R.string.quick_settings_rotation_locked_label);
        }
        updateQuickSettings();
    }

    @Override
    void onPostCreate() {
        applyAutoRotationChanges();
        super.onPostCreate();
    }

    private boolean getAutoRotation() {
        return (Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        getAutoRotation();
        applyAutoRotationChanges();
    }
}
