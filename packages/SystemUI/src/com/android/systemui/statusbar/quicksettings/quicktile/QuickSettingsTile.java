package com.android.systemui.statusbar.quicksettings.quicktile;

import com.android.internal.statusbar.IStatusBarService;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.animation.AccelerateInterpolator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsTileView;
import com.android.systemui.statusbar.quicksettings.DisplayNextTile;
import com.android.systemui.statusbar.quicksettings.Tile3dFlipAnimation;

public class QuickSettingsTile implements OnClickListener {

    protected final Context mContext;
    protected final ViewGroup mContainerView;
    protected final LayoutInflater mInflater;
    protected QuickSettingsTileView mTile;
    protected OnClickListener mOnClick;
    protected OnLongClickListener mOnLongClick;
    protected int mTileLayout;
    protected int mDrawable;
    protected String mLabel;
    protected QuickSettingsController mQsc;
    IStatusBarService mStatusBarService;
    Handler mHandler;
    protected boolean mHapticFeedback;
    protected Vibrator mVibrator;
    private long[] mClickPattern;
    private long[] mLongClickPattern;

    public QuickSettingsTile(Context context, LayoutInflater inflater, QuickSettingsContainerView container, QuickSettingsController qsc) {
        mContext = context;
        mContainerView = container;
        mInflater = inflater;
        mDrawable = R.drawable.stat_sys_roaming_cdma_0;
        mLabel = mContext.getString(R.string.quick_settings_label_enabled);
        mQsc = qsc;
        mTileLayout = R.layout.quick_settings_tile_generic;
        mHandler = new Handler();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void setupQuickSettingsTile(){
        createQuickSettings();
        onPostCreate();
        updateQuickSettings();
        mTile.setOnClickListener(this);
        mTile.setOnLongClickListener(mOnLongClick);
        updateHapticFeedbackSetting();
    }

    void createQuickSettings(){
        mTile = (QuickSettingsTileView) mInflater.inflate(R.layout.quick_settings_tile, mContainerView, false);
        mTile.setContent(mTileLayout, mInflater);
        mContainerView.addView(mTile);
    }

    void onPostCreate(){}

    public void onReceive(Context context, Intent intent) {}

    public void onChangeUri(ContentResolver resolver, Uri uri) {}

    void updateQuickSettings(){
        TextView tv = (TextView) mTile.findViewById(R.id.tile_textview);
        tv.setCompoundDrawablesWithIntrinsicBounds(0, mDrawable, 0, 0);
        tv.setText(mLabel);
    }

    void startSettingsActivity(String action){
        Intent intent = new Intent(action);
        startSettingsActivity(intent);
    }

    void startSettingsActivity(Intent intent) {
        updateHapticFeedbackSetting();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
        provideHapticFeedback(mLongClickPattern);
        startCollapseActivity();
    }

    void startCollapseActivity() {
        mHandler.post(new Runnable() { public void run() {
            try {
                 IStatusBarService statusbar = getStatusBarService();
                 if (statusbar != null) {
                     statusbar.collapse();
                 }
            } catch (RemoteException ex) {
                 // re-acquire status bar service next time it is needed.
                 mStatusBarService = null;
            }
        }});
    }

    IStatusBarService getStatusBarService() {
        if (mStatusBarService == null) {
            mStatusBarService = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService("statusbar"));
        }
        return mStatusBarService;
    }

    private void updateHapticFeedbackSetting() {
        ContentResolver cr = mContext.getContentResolver();
        int expandedHapticFeedback = Settings.System.getInt(cr,
                Settings.System.EXPANDED_HAPTIC_FEEDBACK, 2);
        long[] clickPattern = null, longClickPattern = null;
        boolean hapticFeedback;
	
        if (expandedHapticFeedback == 2) {
             hapticFeedback = Settings.System.getInt(cr,
                     Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1;
        } else {
            hapticFeedback = (expandedHapticFeedback == 1);
        }
	
        if (hapticFeedback) {
            clickPattern = Settings.System.getLongArray(cr,
                    Settings.System.HAPTIC_DOWN_ARRAY, null);
            longClickPattern = Settings.System.getLongArray(cr,
                    Settings.System.HAPTIC_LONG_ARRAY, null);
        }

        setHapticFeedback(hapticFeedback, clickPattern, longClickPattern);
    }

    void setHapticFeedback(boolean enabled, long[] clickPattern, long[] longClickPattern) {
        mHapticFeedback = enabled;
        mClickPattern = clickPattern;
        mLongClickPattern = longClickPattern;
    }

    void flipTile() {
        if (mTile == null || !enableFlip()) return;

        final float centerX = mTile.getWidth() / 2.0f;
        final float centerY = mTile.getHeight() / 2.0f;

         // Create a new 3D rotation with the supplied parameter
         // The animation listener is used to trigger the next animation
         final Tile3dFlipAnimation rotation =
                new Tile3dFlipAnimation(0, 90, centerX, centerY);
         rotation.setDuration(500);
         rotation.setFillAfter(true);
         rotation.setInterpolator(new AccelerateInterpolator());
         rotation.setAnimationListener(new DisplayNextTile(true, mTile, mTile));
         mTile.startAnimation(rotation);
    }

    boolean enableFlip() {
        return (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.ENABLE_FLIP_ANIMATE, 1) == 1);
    }

    @Override
    public final void onClick(View v) {
        updateHapticFeedbackSetting();
        mOnClick.onClick(v);
        flipTile();
        provideHapticFeedback(mClickPattern);
        ContentResolver resolver = mContext.getContentResolver();
        boolean shouldCollapse = Settings.System.getInt(resolver, Settings.System.EXPANDED_HIDE_ONCHANGE, 0) == 1;
        if (shouldCollapse) {
            startCollapseActivity();
        }
    }

    private void provideHapticFeedback(long[] pattern) {
        if (mHapticFeedback && pattern != null) {
            if (pattern.length == 1) {
                mVibrator.vibrate(pattern[0]);
            } else {
                mVibrator.vibrate(pattern, -1);
            }
        }
    }
}
