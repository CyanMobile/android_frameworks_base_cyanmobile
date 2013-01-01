package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.cmcustom.SmsHelper;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;

public class UserTile extends QuickSettingsTile {

    private static final String TAG = "UserTile";
    private Drawable userAvatar;

    public UserTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mTileLayout = R.layout.quick_settings_tile_user;

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do nothing
            }
        };
        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.USER_MY_NUMBERS)
                , this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        queryForUserInformation();
    }

    @Override
    void onPostCreate() {
        queryForUserInformation();
        super.onPostCreate();
    }

    @Override
    void updateQuickSettings() {
        ImageView iv = (ImageView) mTile.findViewById(R.id.user_imageview);
        TextView tv = (TextView) mTile.findViewById(R.id.user_textview);
        tv.setText(mLabel);
        iv.setImageDrawable(userAvatar);
    }

    private void queryForUserInformation() {
        ContentResolver resolver = mContext.getContentResolver();
        String numbers = Settings.System.getString(resolver, Settings.System.USER_MY_NUMBERS);
        if (numbers.equals("000000000")) {
            numbers = null;
        }
        if (numbers != null) {
            String name = SmsHelper.getName(mContext, numbers);
            Drawable avatar = null;
            Bitmap rawAvatar = SmsHelper.getContactPicture(mContext, numbers);
            if (rawAvatar != null) {
                avatar = new BitmapDrawable(mContext.getResources(), rawAvatar);
            } else {
                avatar = mContext.getResources().getDrawable(com.android.internal.R.drawable.ic_contact_picture);
            }
            setUserTileInfo(name, avatar);
        }
    }

    void setUserTileInfo(String name, Drawable avatar) {
        mLabel = name;
        userAvatar = avatar;
        updateQuickSettings();
    }
}
