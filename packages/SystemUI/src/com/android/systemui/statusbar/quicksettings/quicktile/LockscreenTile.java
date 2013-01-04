package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.net.Uri;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;

public class LockscreenTile extends QuickSettingsTile {
    private static final String KEY_DISABLED = "lockscreen_disabled";

    private KeyguardLock mLock = null;
    private boolean mDisabledLockscreen = false;

    private static final String TAG = "LockButton";
    Context mContext;

    public LockscreenTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mContext = context;
        mDisabledLockscreen = getPreferences(mContext).getBoolean(KEY_DISABLED, false);

        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                 toggleState();
            }
        };

        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startSettingsActivity("android.settings.SECURITY_SETTINGS");
                return true;
            }
        };
    }

    protected void toggleState() {
        mDisabledLockscreen = !mDisabledLockscreen;

        SharedPreferences.Editor editor = getPreferences(mContext).edit();
        editor.putBoolean(KEY_DISABLED, mDisabledLockscreen);
        editor.apply();

        applyState(mContext);
        applyLockChanges();
    }

    private void applyState(Context context) {
        if (mLock == null) {
            KeyguardManager keyguardManager = (KeyguardManager)
                    context.getSystemService(Context.KEYGUARD_SERVICE);
            mLock = keyguardManager.newKeyguardLock("PowerWidget");
        }
        if (mDisabledLockscreen) {
            mLock.disableKeyguard();
        } else {
            mLock.reenableKeyguard();
        }
    }

    void applyLockChanges() {
        if (!mDisabledLockscreen) {
            mDrawable = R.drawable.ic_qs_lock_screen_on;
            mLabel = mContext.getString(R.string.quick_settings_lockon);
        } else {
            mDrawable = R.drawable.ic_qs_lock_screen_off;
            mLabel = mContext.getString(R.string.quick_settings_lockoff);
        }
        updateQuickSettings();
    }

    @Override
    void onPostCreate() {
        mDisabledLockscreen = getPreferences(mContext).getBoolean(KEY_DISABLED, false);
        applyState(mContext);
        applyLockChanges();
        super.onPostCreate();
    }

    protected SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences("PowerButton-" + "toggleLockScreen", Context.MODE_PRIVATE);
    }
}
