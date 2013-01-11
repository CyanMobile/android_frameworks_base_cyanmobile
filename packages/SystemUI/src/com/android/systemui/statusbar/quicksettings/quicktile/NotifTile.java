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

public class NotifTile extends QuickSettingsTile {

    private static final String TAG = "NotificationTile";
    Context mContext;

    public NotifTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mContext = context;

        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                     Settings.System.putInt(
                         mContext.getContentResolver(),
                             Settings.System.STATUS_BAR_NOTIF, getNotifEnable() ? 0 : 1);
                applyNotifChanges();
            }
        };

        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION)
                , this);
    }

    void applyNotifChanges() {
        if(getNotifEnable()){
            mDrawable = R.drawable.ic_qs_notif_enable;
            mLabel = mContext.getString(R.string.quick_settings_notif_enable_label);
        } else {
            mDrawable = R.drawable.ic_qs_notif;
            mLabel = mContext.getString(R.string.quick_settings_notif_disable_label);
        }
        updateQuickSettings();
    }

    @Override
    void onPostCreate() {
        applyNotifChanges();
        super.onPostCreate();
    }

    private boolean getNotifEnable() {
        return (Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.STATUS_BAR_NOTIF, 1) == 1);
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        getNotifEnable();
        applyNotifChanges();
    }
}
