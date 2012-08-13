/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

/**
 * WeatherPopup clock view for desk docks.
 */
public class WeatherPopup extends QuickSettings {
    public WeatherPopup(View anchor) {
        super(anchor);
    }
    private static final boolean DEBUG = false;

    private static final String LOG_TAG = "WeatherPopup";

    // Interval between forced polls of the weather widget.
    private final long QUERY_WEATHER_DELAY = 60 * 60 * 1000; // 1 hr

    // Internal message IDs.
    private final int QUERY_WEATHER_DATA_MSG     = 0x1000;
    private final int UPDATE_WEATHER_DISPLAY_MSG = 0x1001;

    // Weather widget query information.
    private static final String GENIE_PACKAGE_ID = "com.google.android.apps.genie.geniewidget";
    private static final String WEATHER_CONTENT_AUTHORITY = GENIE_PACKAGE_ID + ".weather";
    private static final String WEATHER_CONTENT_PATH = "/weather/current";
    private static final String[] WEATHER_CONTENT_COLUMNS = new String[] {
            "location",
            "timestamp",
            "temperature",
            "highTemperature",
            "lowTemperature",
            "iconUrl",
            "iconResId",
            "description",
        };

    private static final String ACTION_GENIE_REFRESH = "com.google.android.apps.genie.REFRESH";

    private TextView mWeatherCurrentTemperature;
    private TextView mWeatherHighTemperature;
    private TextView mWeatherLowTemperature;
    private TextView mWeatherLocation;
    private ImageView mWeatherIcon;
    private ViewGroup root;

    private String mWeatherCurrentTemperatureString;
    private String mWeatherHighTemperatureString;
    private String mWeatherLowTemperatureString;
    private String mWeatherLocationString;
    private Drawable mWeatherIconDrawable;

    private Resources mGenieResources = null;

    @Override
    protected void onCreate() {
        // give up any internal focus before we switch layouts
        LayoutInflater inflater =
                (LayoutInflater) this.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        root = (ViewGroup)inflater.inflate(R.layout.weatherpopup, null);

        mWeatherCurrentTemperature = (TextView) root.findViewById(R.id.weather_temperature);
        mWeatherHighTemperature = (TextView) root.findViewById(R.id.weather_high_temperature);
        mWeatherLowTemperature = (TextView) root.findViewById(R.id.weather_low_temperature);
        mWeatherLocation = (TextView) root.findViewById(R.id.weather_location);
        mWeatherIcon = (ImageView) root.findViewById(R.id.weather_icon);

        mWeatherIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!supportsWeatherIcon()) return;

                Intent genieAppQuery = v.getContext().getPackageManager()
                    .getLaunchIntentForPackage(GENIE_PACKAGE_ID)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                if (genieAppQuery != null) {
                    v.getContext().startActivity(genieAppQuery);
                }
                dismiss();
            }
        });

        this.setContentView(root);
    }

    private final Handler mHandy = new Handler() {
        @Override
        public void handleMessage(Message m) {
            if (m.what == QUERY_WEATHER_DATA_MSG) {
                new Thread() { public void run() { queryWeatherData(); } }.start();
                scheduleWeatherQueryDelayed(QUERY_WEATHER_DELAY);
            } else if (m.what == UPDATE_WEATHER_DISPLAY_MSG) {
                updateWeatherDisplay();
            }
        }
    };

    private final ContentObserver mContentObserver = new ContentObserver(mHandy) {
        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) Log.d(LOG_TAG, "content observer notified that weather changed");
            refreshWeather();
        }
    };

    // Tell the Genie widget to load new data from the network.
    private void requestWeatherDataFetch() {
        if (DEBUG) Log.d(LOG_TAG, "forcing the Genie widget to update weather now...");
        this.anchor.getContext().sendBroadcast(new Intent(ACTION_GENIE_REFRESH).putExtra("requestWeather", true));
        // we expect the result to show up in our content observer
    }

    private boolean supportsWeatherIcon() {
        return (mGenieResources != null);
    }

    private void scheduleWeatherQueryDelayed(long delay) {
        // cancel any existing scheduled queries
        unscheduleWeatherQuery();

        if (DEBUG) Log.d(LOG_TAG, "scheduling weather fetch message for " + delay + "ms from now");

        mHandy.sendEmptyMessageDelayed(QUERY_WEATHER_DATA_MSG, delay);
    }

    private void unscheduleWeatherQuery() {
        mHandy.removeMessages(QUERY_WEATHER_DATA_MSG);
    }

    private void queryWeatherData() {
        // if we couldn't load the weather widget's resources, we simply
        // assume it's not present on the device.
        if (mGenieResources == null) return;

        Uri queryUri = new Uri.Builder()
            .scheme(android.content.ContentResolver.SCHEME_CONTENT)
            .authority(WEATHER_CONTENT_AUTHORITY)
            .path(WEATHER_CONTENT_PATH)
            .appendPath(new Long(System.currentTimeMillis()).toString())
            .build();

        if (DEBUG) Log.d(LOG_TAG, "querying genie: " + queryUri);

        Cursor cur;
        try {
            cur = this.anchor.getContext().getContentResolver().query(
                queryUri,
                WEATHER_CONTENT_COLUMNS,
                null,
                null,
                null);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "Weather query failed", e);
            cur = null;
        }

        if (cur != null && cur.moveToFirst()) {
            if (DEBUG) {
                java.lang.StringBuilder sb =
                    new java.lang.StringBuilder("Weather query result: {");
                for(int i=0; i<cur.getColumnCount(); i++) {
                    if (i>0) sb.append(", ");
                    sb.append(cur.getColumnName(i))
                        .append("=")
                        .append(cur.getString(i));
                }
                sb.append("}");
                Log.d(LOG_TAG, sb.toString());
            }

            mWeatherIconDrawable = mGenieResources.getDrawable(cur.getInt(
                cur.getColumnIndexOrThrow("iconResId")));

            mWeatherLocationString = cur.getString(
                cur.getColumnIndexOrThrow("location"));

            // any of these may be NULL
            final int colTemp = cur.getColumnIndexOrThrow("temperature");
            final int colHigh = cur.getColumnIndexOrThrow("highTemperature");
            final int colLow = cur.getColumnIndexOrThrow("lowTemperature");

            mWeatherCurrentTemperatureString =
                cur.isNull(colTemp)
                    ? "\u2014"
                    : String.format("%d\u00b0", cur.getInt(colTemp));
            mWeatherHighTemperatureString =
                cur.isNull(colHigh)
                    ? "\u2014"
                    : String.format("%d\u00b0", cur.getInt(colHigh));
            mWeatherLowTemperatureString =
                cur.isNull(colLow)
                    ? "\u2014"
                    : String.format("%d\u00b0", cur.getInt(colLow));
        } else {
            Log.w(LOG_TAG, "No weather information available (cur="
                + cur +")");
            mWeatherIconDrawable = null;
            mWeatherLocationString = this.anchor.getContext().getString(R.string.weather_fetch_failure);
            mWeatherCurrentTemperatureString =
                mWeatherHighTemperatureString =
                mWeatherLowTemperatureString = "";
        }

        if (cur != null) {
            // clean up cursor
            cur.close();
        }

        mHandy.sendEmptyMessage(UPDATE_WEATHER_DISPLAY_MSG);
    }

    private void refreshWeather() {
        if (supportsWeatherIcon())
            scheduleWeatherQueryDelayed(0);
        updateWeatherDisplay(); // in case we have it cached
    }

    private void updateWeatherDisplay() {
        if (mWeatherCurrentTemperature == null) return;

        mWeatherCurrentTemperature.setText(mWeatherCurrentTemperatureString);
        mWeatherHighTemperature.setText(mWeatherHighTemperatureString);
        mWeatherLowTemperature.setText(mWeatherLowTemperatureString);
        mWeatherLocation.setText(mWeatherLocationString);
        mWeatherIcon.setImageDrawable(mWeatherIconDrawable);
    }

    @Override
    protected void onShow() {
        initViews();
        // Listen for updates to weather data
        Uri weatherNotificationUri = new Uri.Builder()
            .scheme(android.content.ContentResolver.SCHEME_CONTENT)
            .authority(WEATHER_CONTENT_AUTHORITY)
            .path(WEATHER_CONTENT_PATH)
            .build();
        this.anchor.getContext().getContentResolver().registerContentObserver(
            weatherNotificationUri, true, mContentObserver);

        if (supportsWeatherIcon()) {
            requestWeatherDataFetch();
        }
    }

    @Override
    public void dismiss() {
        this.anchor.getContext().getContentResolver().unregisterContentObserver(mContentObserver);
        unscheduleWeatherQuery();
	this.dismiss();
    }

    private void initViews() {
        try {
            mGenieResources = this.anchor.getContext().getPackageManager().getResourcesForApplication(GENIE_PACKAGE_ID);
        } catch (PackageManager.NameNotFoundException e) {
            // no weather info available
            Log.w(LOG_TAG, "Can't find "+GENIE_PACKAGE_ID+". Weather forecast will not be available.");
        }
    }

}
