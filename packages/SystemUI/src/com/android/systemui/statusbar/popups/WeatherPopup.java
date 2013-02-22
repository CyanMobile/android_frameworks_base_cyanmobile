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

package com.android.systemui.statusbar.popups;

import android.app.PendingIntent;
import android.app.Service;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import org.w3c.dom.Document;
import android.text.format.DateFormat;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import com.android.internal.util.weather.HttpRetriever;
import com.android.internal.util.weather.WeatherInfo;
import com.android.internal.util.weather.WeatherXmlParser;
import com.android.internal.util.weather.YahooPlaceFinder;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * WeatherPopup clock view for desk docks.
 */
public class WeatherPopup extends QuickSettings {
    public WeatherPopup(View anchor) {
        super(anchor);
    }
    private static final boolean DEBUG = false;

    private static final String TAG = "WeatherPopup";

    private static final String ACTION_LOC_UPDATE = "com.android.systemui.weather.action.LOCATION_UPDATE";

    private static final long MIN_LOC_UPDATE_INTERVAL = 15 * 60 * 1000; /* 15 minutes */
    private static final float MIN_LOC_UPDATE_DISTANCE = 5000f; /* 5 km */

    private LocationManager mLocManager;
    private ConnectivityManager mConnM;
    private NetworkInfo mInfo;

    private TextView mWeatherCity, mWeatherHumi, mWeatherWind, mWeatherCondition, mWeatherLowHigh, mWeatherTemp, mWeatherUpdateTime;
    private ImageView mWeatherImage;
    private ViewGroup root;
    private Context mContext;

    private boolean mForceRefresh;
    private PendingIntent mLocUpdateIntent;

    @Override
    protected void onCreate() {
        // give up any internal focus before we switch layouts
        mContext = this.anchor.getContext();

        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mLocManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mConnM = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mInfo = mConnM.getActiveNetworkInfo();
        mLocUpdateIntent = PendingIntent.getService(mContext, 0, new Intent(ACTION_LOC_UPDATE), 0);
        mForceRefresh = false;

        root = (ViewGroup)inflater.inflate(R.layout.weatherpopup, null);

        mWeatherCity = (TextView) root.findViewById(R.id.weather_city);
        mWeatherCondition = (TextView) root.findViewById(R.id.weather_condition);
        mWeatherTemp = (TextView) root.findViewById(R.id.weather_temp);
        mWeatherHumi = (TextView) root.findViewById(R.id.weather_humi);
        mWeatherWind = (TextView) root.findViewById(R.id.weather_wind);
        mWeatherLowHigh = (TextView) root.findViewById(R.id.weather_low_high);
        mWeatherUpdateTime = (TextView) root.findViewById(R.id.update_time);
        mWeatherImage = (ImageView) root.findViewById(R.id.weather_image);

        mWeatherImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mWeatherCondition != null) {
                    mWeatherCondition.setText(com.android.internal.R.string.weather_refreshing);
                }
                updateLocationListenerState();
                mForceRefresh = true;
                refreshWeather();
            }
        });

        this.setContentView(root);
    }


    //===============================================================================================
    // Weather related functionality
    //===============================================================================================
    private static final String URL_YAHOO_API_WEATHER = "http://weather.yahooapis.com/forecastrss?w=%s&u=";
    private static WeatherInfo mWeatherInfo = new WeatherInfo();
    private WeatherQueryTask mWeatherQueryTask;
    private LocationQueryTask mLocationQueryTask;
    private LocationInfo mLocationInfo = new LocationInfo();
    private String mLastKnownWoeid;
    private boolean mNeedsWeatherRefresh;
    private Set<String> mTrackedProviders;

    private void updateLocationListenerState() {
        if (mInfo == null || !mInfo.isConnected()) {
            return;
        }

        final ContentResolver resolver = mContext.getContentResolver();
        boolean useCustomLoc = Settings.System.getInt(resolver,
                                Settings.System.WEATHER_USE_CUSTOM_LOCATION, 0) == 1;
        String customLoc = Settings.System.getString(resolver,
                                    Settings.System.WEATHER_CUSTOM_LOCATION);
        if (useCustomLoc && customLoc != null) {
            mLocManager.removeUpdates(mLocUpdateIntent);
            mLocationInfo.customLocation = customLoc;
            triggerLocationQueryWithLocation(null);
        } else {
            mTrackedProviders = getTrackedProviders();
            List<String> locationProviders = mLocManager.getProviders(true);
            for (String providerName : locationProviders) {
                 if (mTrackedProviders.contains(providerName)) {
                     mLocManager.requestLocationUpdates(providerName, MIN_LOC_UPDATE_INTERVAL,
                             MIN_LOC_UPDATE_DISTANCE, mLocUpdateIntent);
                     triggerLocationQueryWithLocation(mLocManager.getLastKnownLocation(providerName));
                 }
            }
            mLocationInfo.customLocation = null;
        }
    }

    private Set<String> getTrackedProviders() {
        Set<String> providerSet = new HashSet<String>();

        if (trackGPS()) {
            providerSet.add(LocationManager.GPS_PROVIDER);
        }
        if (trackNetwork()) {
            providerSet.add(LocationManager.NETWORK_PROVIDER);
        }
        return providerSet;
    }

    private boolean trackNetwork() {
        return true;
    }

    private boolean trackGPS() {
        return true;
    }

    private void triggerLocationQueryWithLocation(Location location) {
        if (mInfo == null || !mInfo.isConnected()) {
            return;
        }

        if (location != null) {
            mLocationInfo.location = location;
        }
        if (mLocationQueryTask != null) {
            mLocationQueryTask.cancel(true);
        }
        mLocationQueryTask = new LocationQueryTask();
        mLocationQueryTask.execute(mLocationInfo);
    }

    private boolean triggerWeatherQuery(boolean force) {
        if (mInfo == null || !mInfo.isConnected()) {
            return false;
        }

        if (!force) {
            if (mLocationQueryTask != null && mLocationQueryTask.getStatus() != AsyncTask.Status.FINISHED) {
                /* the location query task will trigger the weather query */
                return true;
            }
        }
        if (mWeatherQueryTask != null) {
            if (force) {
                mWeatherQueryTask.cancel(true);
            } else if (mWeatherQueryTask.getStatus() != AsyncTask.Status.FINISHED) {
                return false;
            }
        }
        mWeatherQueryTask = new WeatherQueryTask();
        mWeatherQueryTask.execute(mLastKnownWoeid);
        return true;
    }

    private static class LocationInfo {
        Location location;
        String customLocation;
    }

    private class LocationQueryTask extends AsyncTask<LocationInfo, Void, String> {
        @Override
        protected String doInBackground(LocationInfo... params) {
            LocationInfo info = params[0];

            try {
                if (info.customLocation != null) {
                    String woeid = YahooPlaceFinder.GeoCode(
                            mContext, info.customLocation);
                    if (DEBUG)
                        Log.d(TAG, "Yahoo location code for " + info.customLocation + " is " + woeid);
                    return woeid;
                } else if (info.location != null) {
                    String woeid = YahooPlaceFinder.reverseGeoCode(mContext,
                            info.location.getLatitude(), info.location.getLongitude());
                    if (DEBUG)
                        Log.d(TAG, "Yahoo location code for geolocation " + info.location + " is " + woeid);
                    return woeid;
                }
            } catch (Exception e) {
                Log.e(TAG, "ERROR: Could not get Location code", e);
                mNeedsWeatherRefresh = true;
            }

            return null;
        }

        @Override
        protected void onPostExecute(String woeid) {
            mLastKnownWoeid = woeid;
            triggerWeatherQuery(true);
        }
    }

    private class WeatherQueryTask extends AsyncTask<String, Void, WeatherInfo> {
        private Document getDocument(String woeid) throws IOException {
            final boolean celsius = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.WEATHER_USE_METRIC, 1) == 1;
            final String urlWithUnit = URL_YAHOO_API_WEATHER + (celsius ? "c" : "f");
            return new HttpRetriever().getDocumentFromURL(String.format(urlWithUnit, woeid));
        }

        private WeatherInfo parseXml(Document doc) {
            WeatherXmlParser parser = new WeatherXmlParser(mContext);
            return parser.parseWeatherResponse(doc);
        }

        @Override
        protected WeatherInfo doInBackground(String... params) {
            String woeid = params[0];

            if (DEBUG)
                Log.d(TAG, "Querying weather for woeid " + woeid);

            if (woeid != null) {
                try {
                    return parseXml(getDocument(woeid));
                } catch (Exception e) {
                    Log.e(TAG, "ERROR: Could not parse weather return info", e);
                    mNeedsWeatherRefresh = true;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(WeatherInfo info) {
            if (info != null) {
                setWeatherData(info);
                mWeatherInfo = info;
            } else if (mWeatherInfo.getTemp() == 0) {
                setNoWeatherData();
            } else {
                setWeatherData(mWeatherInfo);
            }
        }
    }

    /**
     * Reload the weather forecast
     */
    private void refreshWeather() {
        if (mInfo == null || !mInfo.isConnected()) {
            return;
        }

        final ContentResolver resolver = mContext.getContentResolver();
        if (mForceRefresh) {
            if (triggerWeatherQuery(false)) {
                mForceRefresh = false;
            }
        } else if (mWeatherInfo.getLastSync() == 0) {
            setNoWeatherData();
        } else {
            setWeatherData(mWeatherInfo);
        }
    }

    /**
     * Display the weather information
     * @param w
     */
    private void setWeatherData(WeatherInfo w) {
        final ContentResolver resolver = mContext.getContentResolver();
        final Resources res = mContext.getResources();
        boolean showLocation = Settings.System.getInt(resolver,
                Settings.System.WEATHER_SHOW_LOCATION, 1) == 1;
        boolean showTimestamp = Settings.System.getInt(resolver,
                Settings.System.WEATHER_SHOW_TIMESTAMP, 1) == 1;
        boolean invertLowhigh = Settings.System.getInt(resolver,
                Settings.System.WEATHER_INVERT_LOWHIGH, 0) == 1;

            if (mWeatherImage != null) {
                if (w.getConditionResource() != 0) {
                    mWeatherImage.setImageDrawable(res.getDrawable(w.getConditionResource()));
                } else {
                    mWeatherImage.setImageResource(com.android.internal.R.drawable.weather_na);
                }
            }
            if (mWeatherTemp != null) {
                mWeatherTemp.setText(w.getFormattedTemperature());
            }
            if (mWeatherCity != null) {
                mWeatherCity.setText(w.getCity());
                mWeatherCity.setVisibility(showLocation ? View.VISIBLE : View.GONE);
            }
            if (mWeatherCondition != null) {
                mWeatherCondition.setText(w.getCondition());
            }
            if (mWeatherHumi != null) {
                mWeatherHumi.setText(w.getFormattedHumidity());
            }
            if (mWeatherWind != null) {
                mWeatherWind.setText(w.getWindDirection());
            }
            if (mWeatherLowHigh != null) {
                mWeatherLowHigh.setText(invertLowhigh ? w.getFormattedHigh() + " | " + w.getFormattedLow() : w.getFormattedLow() + " | " + w.getFormattedHigh());
            }
            if (mWeatherUpdateTime != null) {
                String date = DateFormat.getDateFormat(mContext).format(w.getTimestamp());
                String time = DateFormat.getTimeFormat(mContext).format(w.getTimestamp());
                mWeatherUpdateTime.setText(date + " " + time);
                mWeatherUpdateTime.setVisibility(showTimestamp ? View.VISIBLE : View.GONE);
            }
    }

    /**
     * There is no data to display, display 'empty' fields and the
     * 'Tap to reload' message
     */
    private void setNoWeatherData() {
        final ContentResolver resolver = mContext.getContentResolver();
        boolean useMetric = Settings.System.getInt(resolver,
                Settings.System.WEATHER_USE_METRIC, 1) == 1;

            if (mWeatherImage != null) {
                mWeatherImage.setImageResource(com.android.internal.R.drawable.weather_na);
            }
            if (mWeatherTemp != null) {
                mWeatherTemp.setVisibility(View.GONE);
            }
            if (mWeatherCity != null) {
                mWeatherCity.setText(com.android.internal.R.string.weather_no_data);
                mWeatherCity.setVisibility(View.VISIBLE);
            }
            if (mWeatherCondition != null) {
                mWeatherCondition.setText(com.android.internal.R.string.weather_tap_to_refresh);
            }
            if (mWeatherHumi != null) {
                mWeatherHumi.setVisibility(View.GONE);
            }
            if (mWeatherWind != null) {
                mWeatherWind.setVisibility(View.GONE);
            }
            if (mWeatherLowHigh != null) {
                mWeatherLowHigh.setVisibility(View.GONE);
            }
            if (mWeatherUpdateTime != null) {
                mWeatherUpdateTime.setVisibility(View.GONE);
            }
    }

    @Override
    protected void onShow() {
        updateLocationListenerState();
    }

    @Override
    public void dismiss() {
	this.dismiss();
    }
}
