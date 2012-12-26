/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Slog;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;

public class LocationController extends BroadcastReceiver {
    private static final String TAG = "StatusBar.LocationController";

    private Context mContext;

    private ArrayList<LocationGpsStateChangeCallback> mChangeCallbacks =
            new ArrayList<LocationGpsStateChangeCallback>();

    public interface LocationGpsStateChangeCallback {
        public void onLocationGpsStateChanged(boolean inUse, String description);
    }

    public LocationController(Context context) {
        mContext = context;

        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationManager.GPS_ENABLED_CHANGE_ACTION);
        filter.addAction(LocationManager.GPS_FIX_CHANGE_ACTION);
        context.registerReceiver(this, filter);
    }

    public void addStateChangedCallback(LocationGpsStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final boolean enabled = intent.getBooleanExtra(LocationManager.EXTRA_GPS_ENABLED, false);

        boolean visible;
        int iconId, textResId;

        if (action.equals(LocationManager.GPS_FIX_CHANGE_ACTION) && enabled) {
            visible = true;
        } else if (action.equals(LocationManager.GPS_ENABLED_CHANGE_ACTION) && !enabled) {
            // GPS is off
            visible = false;
        } else {
            visible = true;
        }
        
            if (visible) {
                for (LocationGpsStateChangeCallback cb : mChangeCallbacks) {
                    cb.onLocationGpsStateChanged(true, null);
                }
            } else {
                for (LocationGpsStateChangeCallback cb : mChangeCallbacks) {
                    cb.onLocationGpsStateChanged(false, null);
                }
            }
    }
}

