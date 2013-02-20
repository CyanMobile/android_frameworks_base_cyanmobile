/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.qwidgets;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.android.systemui.R;
/**
 * Present the user with a dialog of QwikWidgets
 */
public class QwikWidgetsPanelView extends FrameLayout {

    private static final String TAG = "QwikWidgetsPanelView";
    private static final int MAX_PER_LINE = 2;
    private Context mContext;
    private boolean mShowing;

    private ImageView mCloseButton;

    private ScrollView mWidgetsScroller;
    private LayoutInflater mInflater;
    private WidgetBroadcastReceiver mBroadcastReceiver = null;
    private WidgetSettingsObserver mObserver = null;
    private Handler mHandler = new Handler();

    private String mDefaultWidgets;

    private static final LinearLayout.LayoutParams WIDGET_LAYOUT_PARAMS = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);

    private static final LinearLayout.LayoutParams DIVIDER_VERT_PARAMS = new LinearLayout.LayoutParams(
            2, ViewGroup.LayoutParams.MATCH_PARENT);

    private static final LinearLayout.LayoutParams DIVIDER_HORIZ_PARAMS = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 50);

    public static final String WIDGET_DELIMITER = "|";

    public QwikWidgetsPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mCloseButton = (ImageView) findViewById(R.id.close_button);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show(false, false);
            }
        });
        // TODO: Is this necessary?
        setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                show(false, false);
                return true;
            }
        });
        mInflater = (LayoutInflater)mContext.getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);
        mWidgetsScroller = (ScrollView) findViewById(R.id.widgets_scroll);
        mWidgetsScroller.setFadingEdgeLength(5);
        mWidgetsScroller.setVerticalFadingEdgeEnabled(true);

        setupQwikWidgets();
    }

    private class WidgetLayout extends LinearLayout {
        public WidgetLayout(Context context) {
            super(context);
            setOrientation(LinearLayout.HORIZONTAL);
            setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 94));
        }
    }

    /* Used for a dividers in the widget view */
    private class Divider extends LinearLayout {
        public Divider(Context context) {
            super(context);
            setOrientation(LinearLayout.HORIZONTAL);
            setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 20));
        }
    }

    private LinearLayout getWidgetsView() {
        LinearLayout widgetsView = new LinearLayout(mContext);
        widgetsView.setLayoutParams(WIDGET_LAYOUT_PARAMS);
        widgetsView.setOrientation(LinearLayout.VERTICAL);
        String widgets = mDefaultWidgets;

        int lineCount = 0, total = 0, numDummy = 0;
        int numOf = widgets.split("\\|").length;
        String widgetArray[] = widgets.split("\\|");
        LinearLayout ll = null;

        for(int i = 0; i < numOf; i++) {
            total++;
            View widget = null;

            widget = mInflater.inflate(R.
                    layout.qwik_widgets_toggle_only, null, false);
        
            if (lineCount == 0) {
                ll = new WidgetLayout(mContext);
                widgetsView.addView(new Divider(mContext),
                        DIVIDER_HORIZ_PARAMS); // add a divider to the top to make it look better
            }
            
            if(QwikWidget.loadWidget(widgetArray[i], widget)) {
                ll.addView(widget, WIDGET_LAYOUT_PARAMS);
                lineCount++;
                if (lineCount < MAX_PER_LINE) {
                    ll.addView(new Divider(mContext), DIVIDER_VERT_PARAMS); //add a middle divider
                    if (numOf == total) {
                        numDummy = MAX_PER_LINE - lineCount;
                        for(int k = 0; k < numDummy; k++) {
                            ll.addView(new WidgetLayout(mContext),
                                    WIDGET_LAYOUT_PARAMS); // add a dummy view
                        }
                        widgetsView.addView(ll);
                    }
                } else {
                    widgetsView.addView(ll);
                    if (numOf != total) {
                        widgetsView.addView(new Divider(mContext),
                                DIVIDER_HORIZ_PARAMS); // add a bottom divider
                        lineCount = 0;
                    }
                }
            } else {
                Log.e(TAG, "Error setting up widget: " + widgetArray[i]);
            }
        }
        return widgetsView;
    }

    public void setupQwikWidgets() {
        updateSettings();
        // unregister our content receiver
        if (mBroadcastReceiver != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        // unobserve our content
        if (mObserver != null) {
            mObserver.unobserve();
            mObserver = null;
        }
        setupBroadcastReceiver();
        IntentFilter filter = QwikWidget.getAllBroadcastIntentFilters();
        mContext.registerReceiver(mBroadcastReceiver, filter);
        mObserver = new WidgetSettingsObserver(mHandler);
        mObserver.observe();
    }

    private void updateSettings() {
        mDefaultWidgets = getResources().getString(R.string
                .default_qwik_widgets);
    }
    
    public void show(final boolean show, boolean animate) {
        mShowing = show;
        if (show) {
            if (getVisibility() != View.VISIBLE) {
                vibrate();
                setVisibility(View.VISIBLE);
            }
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        } else {
            setVisibility(View.GONE);
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        Log.d(TAG, "onVisibilityChanged");
        switch (visibility) {
            case View.VISIBLE:
                setupQwikWidgets();
                mWidgetsScroller.addView(getWidgetsView());
                Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.in_animation);
                animation.setStartOffset(0);
                mWidgetsScroller.startAnimation(animation);
                break;
            case View.GONE:
                mWidgetsScroller.removeAllViews();
                break;
        }
    }

    public void hide(boolean animate) {
        setVisibility(View.GONE);
    }

    /**
     * Whether the panel is showing, or, if it's animating, whether it will be
     * when the animation is done.
     */
    public boolean isShowing() {
        return mShowing;
    }

    private void vibrate() {
        if (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0) {
            Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(100);
        }
    }

    private void setupBroadcastReceiver() {
        if(mBroadcastReceiver == null) {
            mBroadcastReceiver = new WidgetBroadcastReceiver();
        }
    }

    private class WidgetBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            QwikWidget.handleOnReceive(context, intent);

            /*  Needed to remove this, because android.intent.action.ANY_DATA_STATE was
                being broadcasted so much on my device which is not activated. It's
                possible this will not occur as much on an activated device, however
                it could when a used goes into limited or in and out reception.
                we shouldn't need to update all widgets when only one needs updating.

                QwikWidget.updateAllWidgets();
            */
        }
    };

    private class WidgetSettingsObserver extends ContentObserver {
        public WidgetSettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            for(Uri uri : QwikWidget.getAllObservedUris()) {
                resolver.registerContentObserver(uri, false, this);
            }
        }

        public void unobserve() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChangeUri(Uri uri, boolean selfChange) {
            QwikWidget.handleOnChangeUri(uri);
            QwikWidget.updateAllWidgets();
        }
    }
}
