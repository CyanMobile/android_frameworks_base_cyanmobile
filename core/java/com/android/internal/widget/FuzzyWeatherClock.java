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

package com.android.internal.widget;

import com.android.internal.R;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.AsyncTask;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;

import android.util.Log;
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
import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.Calendar;

/**
 * Displays the time fuzzy style
 */
public class FuzzyWeatherClock extends LinearLayout {

    private static final String TAG = "FuzzyWeatherClock";
    private static final boolean DEBUG = false;

    private static final String ACTION_LOC_UPDATE = "com.android.internal.action.LOCATION_UPDATE";

    private static final long MIN_LOC_UPDATE_INTERVAL = 15 * 60 * 1000; /* 15 minutes */
    private static final float MIN_LOC_UPDATE_DISTANCE = 5000f; /* 5 km */

    private LocationManager mLocManager;
    private ConnectivityManager mConnM;
    private NetworkInfo mInfo;

    private final static String M12 = "hh:mm";
    private final static String M24 = "kk:mm";
    private final String mOclock = getResources().getString(R.string.fuzzy_oclock);
    private final String mFivePast = getResources().getString(R.string.fuzzy_five_past);
    private final String mTenPast = getResources().getString(R.string.fuzzy_ten_past);
    private final String mQuarterPast = getResources().getString(R.string.fuzzy_quarter_past);
    private final String mTwentyPast = getResources().getString(R.string.fuzzy_twenty_past);
    private final String mTwentyFivePast = getResources().getString(R.string.fuzzy_twenty_five_past);
    private final String mHalfPast = getResources().getString(R.string.fuzzy_half_past);
    private final String mTwentyFiveTo = getResources().getString(R.string.fuzzy_twenty_five_to);
    private final String mTwentyTo = getResources().getString(R.string.fuzzy_twenty_to);
    private final String mQuarterTo = getResources().getString(R.string.fuzzy_quarter_to);
    private final String mTenTo = getResources().getString(R.string.fuzzy_ten_to);
    private final String mFiveTo = getResources().getString(R.string.fuzzy_five_to);
    private final String mOne = getResources().getString(R.string.fuzzy_one);
    private final String mTwo = getResources().getString(R.string.fuzzy_two);
    private final String mThree = getResources().getString(R.string.fuzzy_three);
    private final String mFour = getResources().getString(R.string.fuzzy_four);
    private final String mFive = getResources().getString(R.string.fuzzy_five);
    private final String mSix = getResources().getString(R.string.fuzzy_six);
    private final String mSeven = getResources().getString(R.string.fuzzy_seven);
    private final String mEight = getResources().getString(R.string.fuzzy_eight);
    private final String mNine = getResources().getString(R.string.fuzzy_nine);
    private final String mTen = getResources().getString(R.string.fuzzy_ten);
    private final String mEleven = getResources().getString(R.string.fuzzy_eleven);
    private final String mTwelve = getResources().getString(R.string.fuzzy_twelve);
    private String tempC;
    private String timed;
    private String humY;
    private String date;
    private String time;
    private String mLabel;
    private String mLoc;
    private String mDate;
    private String mCond;
    private Calendar mCalendar;
    private String mFormat;
    private TextView mTimeDisplay;
    private TextView mWeatherTemp;
    private TextView mWeatherLoc;
    private TextView mWeatherCond;
    private TextView mWeatherUpdateTime;
    private ImageView mWeatherImage;

    private Drawable drwb;
    private boolean addDrwb = false;
    private boolean updating = false;
    private boolean mForceRefresh = false;
    private PendingIntent mLocUpdateIntent;

    private AmPm mAmPm;
    private ContentObserver mFormatChangeObserver;
    private int mAttached = 0; // for debugging - tells us whether attach/detach is unbalanced

    /* called by system on minute ticks */
    private final Handler mHandler = new Handler();
    private BroadcastReceiver mIntentReceiver;

    private static class TimeChangedReceiver extends BroadcastReceiver {
        private WeakReference<FuzzyWeatherClock> mClock;
        private Context mContext;

        public TimeChangedReceiver(FuzzyWeatherClock clock) {
            mClock = new WeakReference<FuzzyWeatherClock>(clock);
            mContext = clock.getContext();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Post a runnable to avoid blocking the broadcast.
            final boolean timezoneChanged =
                    intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED);
            final FuzzyWeatherClock clock = mClock.get();
            if (clock != null) {
                clock.mHandler.post(new Runnable() {
                    public void run() {
                        if (timezoneChanged) {
                            clock.mCalendar = Calendar.getInstance();
                        }
                        clock.refreshWeather();
                    }
                });
            } else {
                try {
                    mContext.unregisterReceiver(this);
                } catch (RuntimeException e) {
                    // Shouldn't happen
                }
            }
        }
    };

    static class AmPm {
        private TextView mAmPm;
        private String mAmString, mPmString;

        AmPm(View parent, Typeface tf) {
            mAmPm = (TextView) parent.findViewById(R.id.am_pm);
            if (tf != null) {
                mAmPm.setTypeface(tf);
            }

            String[] ampm = new DateFormatSymbols().getAmPmStrings();
            mAmString = ampm[0];
            mPmString = ampm[1];
        }

        void setShowAmPm(boolean show) {
            mAmPm.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        void setIsMorning(boolean isMorning) {
            mAmPm.setText(isMorning ? mAmString : mPmString);
        }
    }

    private static class FormatChangeObserver extends ContentObserver {
        private WeakReference<FuzzyWeatherClock> mClock;
        private Context mContext;
        public FormatChangeObserver(FuzzyWeatherClock clock) {
            super(new Handler());
            mClock = new WeakReference<FuzzyWeatherClock>(clock);
            mContext = clock.getContext();
        }
        @Override
        public void onChange(boolean selfChange) {
            FuzzyWeatherClock fuzzyClock = mClock.get();
            if (fuzzyClock != null) {
                fuzzyClock.setDateFormat();
                fuzzyClock.refreshWeather();
            } else {
                try {
                    mContext.getContentResolver().unregisterContentObserver(this);
                } catch (RuntimeException e) {
                    // Shouldn't happen
                }
            }
        }
    }

    public FuzzyWeatherClock(Context context) {
        this(context, null);
    }

    public FuzzyWeatherClock(Context context, AttributeSet attrs) {
        super(context, attrs);

        mLocManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mConnM = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mInfo = mConnM.getActiveNetworkInfo();
        mLocUpdateIntent = PendingIntent.getService(context, 0, new Intent(ACTION_LOC_UPDATE), 0);
        mForceRefresh = false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTimeDisplay = (TextView) findViewById(R.id.timeDisplay);
        mTimeDisplay.setOnClickListener(mRefreshListener);
        mWeatherImage = (ImageView) findViewById(R.id.weather_image);
        mWeatherTemp = (TextView) findViewById(R.id.weatherone_textview);
        mWeatherLoc = (TextView) findViewById(R.id.weatherthree_textview);
        mWeatherCond = (TextView) findViewById(R.id.weatherfour_textview);
        mWeatherUpdateTime = (TextView) findViewById(R.id.weathertwo_textview);
        mTimeDisplay.setTypeface(Typeface.createFromFile("/system/fonts/DroidSans.ttf"));
        mAmPm = new AmPm(this, Typeface.createFromFile("/system/fonts/DroidSans-Bold.ttf"));
        mCalendar = Calendar.getInstance();

        setDateFormat();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mAttached++;

        /* monitor time ticks, time changed, timezone */
        if (mIntentReceiver == null) {
            mIntentReceiver = new TimeChangedReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            mContext.registerReceiver(mIntentReceiver, filter);
        }

        /* monitor 12/24-hour display preference */
        if (mFormatChangeObserver == null) {
            mFormatChangeObserver = new FormatChangeObserver(this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        }

        updateLocationListenerState();
        refreshWeather();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mAttached--;

        if (mIntentReceiver != null) {
            mContext.unregisterReceiver(mIntentReceiver);
        }
        if (mFormatChangeObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(
                    mFormatChangeObserver);
        }

        mLocManager.removeUpdates(mLocUpdateIntent);
        mFormatChangeObserver = null;
        mIntentReceiver = null;
    }

    void updateTime(Calendar c) {
        mCalendar = c;
        refreshWeather();
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

    private void updateLocationListenerState() {
        if (mInfo == null || !mInfo.isConnected()) {
            return;
        }

        final ContentResolver resolver = mContext.getContentResolver();
        boolean useCustomLoc = Settings.System.getInt(resolver,
                                Settings.System.WEATHER_USE_CUSTOM_LOCATION, 0) == 1;
        String customLoc = Settings.System.getString(resolver,
                                    Settings.System.WEATHER_CUSTOM_LOCATION);
        boolean showWeather = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_WEATHER, 0) == 1;
            final long interval = Settings.System.getLong(resolver,
                    Settings.System.WEATHER_UPDATE_INTERVAL, 0); // Default to manual
            boolean manualSync = (interval == 0);
      if (showWeather) {
       if (!manualSync && (((System.currentTimeMillis() - mWeatherInfo.getLastSync()) / 60000) >= interval)) {
        if (useCustomLoc && customLoc != null) {
            mLocManager.removeUpdates(mLocUpdateIntent);
            mLocationInfo.customLocation = customLoc;
            triggerLocationQueryWithLocation(null);
        } else {
            mLocManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
                    MIN_LOC_UPDATE_INTERVAL, MIN_LOC_UPDATE_DISTANCE, mLocUpdateIntent);
            mLocationInfo.customLocation = null;
            triggerLocationQueryWithLocation(mLocManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
        }
       }
      }
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
    public void refreshWeather() {
        if (mInfo == null || !mInfo.isConnected()) {
            return;
        }

        final ContentResolver resolver = mContext.getContentResolver();
        boolean showWeather = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_WEATHER, 0) == 1;
            final long interval = Settings.System.getLong(resolver,
                    Settings.System.WEATHER_UPDATE_INTERVAL, 0); // Default to manual
            boolean manualSync = (interval == 0);
       if (showWeather) {
            if (mForceRefresh || !manualSync && (((System.currentTimeMillis() - mWeatherInfo.getLastSync()) / 60000) >= interval)) {
                updating = true;
                if (triggerWeatherQuery(false)) {
                    mForceRefresh = false;
                }
            } else if (manualSync && mWeatherInfo.getLastSync() == 0) {
                setNoWeatherData();
            } else {
                setWeatherData(mWeatherInfo);
            }
       }
    }

    /**
     * Display the weather information
     * @param w
     */
    private void setWeatherData(WeatherInfo w) {
        final Resources res = mContext.getResources();
        if (w.getConditionResource() != 0) {
            addDrwb = true;
            drwb = res.getDrawable(w.getConditionResource());
        } else {
            addDrwb = false;
        }
        mLabel = (w.getFormattedTemperature() + " | " + w.getFormattedHumidity()) ;
        mLoc = (w.getCity() + "  ");
        mCond = (w.getCondition() + " ");
        date = DateFormat.getDateFormat(mContext).format(w.getTimestamp());
        time = DateFormat.getTimeFormat(mContext).format(w.getTimestamp());
        mDate = (date + " " + time);
        updateTime();
    }

    /**
     * There is no data to display, display 'empty' fields and the
     * 'Tap to reload' message
     */
    private void setNoWeatherData() {
        mLabel = mContext.getString(R.string.weather_no_data);
        mLoc = mContext.getString(R.string.weather_no_data);
        mCond = mContext.getString(R.string.weather_no_data);
        mDate = mContext.getString(R.string.weather_no_data);
        addDrwb = false;
        updateTime();
    }

    public void updateTime() {
        final ContentResolver resolver = mContext.getContentResolver();
        boolean showLocation = Settings.System.getInt(resolver,
                Settings.System.WEATHER_SHOW_LOCATION, 1) == 1;
        boolean showTimestamp = Settings.System.getInt(resolver,
                Settings.System.WEATHER_SHOW_TIMESTAMP, 1) == 1;
        boolean showWeather = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_WEATHER, 0) == 1;

        mCalendar.setTimeInMillis(System.currentTimeMillis());
        int mMinutes = mCalendar.get(mCalendar.MINUTE);
        int mHour = mCalendar.get(mCalendar.HOUR);
        String mNextH, mTimeH, mTimeString;

        //hours
        if(mHour == 1) { mNextH = mTwo; mTimeH = mOne; }
        else if(mHour == 2) { mNextH = mThree; mTimeH = mTwo; }
        else if(mHour == 3) { mNextH = mFour; mTimeH = mThree; }
        else if(mHour == 4) { mNextH = mFive; mTimeH = mFour; }
        else if(mHour == 5) { mNextH = mSix; mTimeH = mFive; }
        else if(mHour == 6) { mNextH = mSeven; mTimeH = mSix; }
        else if(mHour == 7) { mNextH = mEight; mTimeH = mSeven; }
        else if(mHour == 8) { mNextH = mNine; mTimeH = mEight; }
        else if(mHour == 9) { mNextH = mTen; mTimeH = mNine; }
        else if(mHour == 10) { mNextH = mEleven; mTimeH = mTen; }
        else if(mHour == 11) { mNextH = mTwelve; mTimeH = mEleven; }
        else if(mHour == 12 || mHour == 0) { mNextH = mOne; mTimeH = mTwelve; }
        else { mNextH = mTimeH = "it\'s fucked"; }// { mNextH = mOne; mTimeH = mTwelve; }

        //minutes
        if      ( 0  <= mMinutes && mMinutes <= 4  ) mTimeString = mTimeH + " " + mOclock;
        else if ( 5  <= mMinutes && mMinutes <= 9  ) mTimeString = mFivePast + " " + mTimeH;
        else if ( 10 <= mMinutes && mMinutes <= 14 ) mTimeString = mTenPast + " " + mTimeH;
        else if ( 15 <= mMinutes && mMinutes <= 19 ) mTimeString = mQuarterPast + " " + mTimeH;
        else if ( 20 <= mMinutes && mMinutes <= 24 ) mTimeString = mTimeH + " " + mTwentyPast;
        else if ( 25 <= mMinutes && mMinutes <= 29 ) mTimeString = mTwentyFivePast + " " + mTimeH;
        else if ( 30 <= mMinutes && mMinutes <= 34 ) mTimeString = mHalfPast + " " + mTimeH;
        else if ( 35 <= mMinutes && mMinutes <= 39 ) mTimeString = mTwentyFiveTo + " " + mNextH;
        else if ( 40 <= mMinutes && mMinutes <= 43 ) mTimeString = mTwentyTo + " " + mNextH;
        else if ( 44 <= mMinutes && mMinutes <= 47 ) mTimeString = mQuarterTo + " " + mNextH;
        else if ( 48 <= mMinutes && mMinutes <= 51 ) mTimeString = mTenTo + " " + mNextH;
        else if ( 52 <= mMinutes && mMinutes <= 55 ) mTimeString = mFiveTo + " " + mNextH;
        else if ( 56 <= mMinutes && mMinutes <= 60 ) mTimeString = mNextH + " " + mOclock;
        else { mTimeString = "somethin\'s broke"; }

        //print the time
        mTimeDisplay.setText(mTimeString);

      if (showWeather) {
        if (mWeatherImage != null) {
            mWeatherImage.setVisibility(View.VISIBLE);
            if (addDrwb) {
                mWeatherImage.setImageDrawable(drwb);
            } else {
                mWeatherImage.setImageResource(R.drawable.weather_na);
            }
        }
        if ((mWeatherTemp != null) && (mWeatherUpdateTime != null) && (mWeatherLoc != null) && (mWeatherCond != null)) {
            if (updating) {
                mWeatherTemp.setVisibility(View.GONE);
                mWeatherUpdateTime.setVisibility(View.GONE);
                mWeatherLoc.setVisibility(View.GONE);
                mWeatherCond.setText(R.string.weather_refreshing);
                updating = false;
            } else {
                if (!addDrwb) {
                    mWeatherTemp.setVisibility(View.GONE);
                    mWeatherUpdateTime.setVisibility(View.GONE);
                    mWeatherLoc.setVisibility(View.GONE);
                    mWeatherCond.setText(R.string.weather_no_data);
                } else {
                    mWeatherTemp.setVisibility(View.VISIBLE);
                    mWeatherTemp.setText(mLabel);
                    mWeatherLoc.setVisibility(showLocation ? View.VISIBLE : View.GONE);
                    mWeatherLoc.setText(mLoc);
                    mWeatherCond.setText(mCond);
                    mWeatherUpdateTime.setVisibility(showTimestamp ? View.VISIBLE : View.GONE);
                    mWeatherUpdateTime.setText(mDate);
                }
            }
        }
      } else {
            mWeatherImage.setVisibility(View.GONE);
            mWeatherTemp.setVisibility(View.GONE);
            mWeatherUpdateTime.setVisibility(View.GONE);
            mWeatherLoc.setVisibility(View.GONE);
            mWeatherCond.setVisibility(View.GONE);
      }
    }

    private View.OnClickListener mRefreshListener = new View.OnClickListener() {
        public void onClick(View v) {
            updating = true;
            mForceRefresh = true;
            refreshWeather();
        }
    };

    private void setDateFormat() {
        mFormat = M12;
        mAmPm.setShowAmPm(false);//mFormat.equals(M12));
    }
}
