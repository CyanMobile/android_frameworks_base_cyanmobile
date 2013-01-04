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

import android.net.Uri;
import android.os.Bundle;
import android.os.Message;

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
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.Calendar;

/**
 * Displays the time Kanji style
 */
public class KanjiWeatherClock extends LinearLayout {

    private static final String TAG = "KanjiWeatherClock";
    private static final boolean DEBUG = false;

    // Dunno what this means, just add from Google translate xD
    private final static String J_0 = "\u96F6";
    private final static String J_1 = "\u4E00";
    private final static String J_2 = "\u4E8C";
    private final static String J_3 = "\u4E09";
    private final static String J_4 = "\u56DB";
    private final static String J_5 = "\u4E94";
    private final static String J_6 = "\u516D";
    private final static String J_7 = "\u4E03";
    private final static String J_8 = "\u516B";
    private final static String J_9 = "\u4E5D";
    private final static String J_10 = "\u5341";
    private final static String J_100 = "\u767E";
    private final static String J_1000 = "\u5343";
    private final static String J_AM = "\u5348\u524D";
    private final static String J_PM = "\u5348\u5F8C";
    private final static String J_HOUR = "\u6642";
    private final static String J_MINUTE = "\u5206";
    private final static String J_SECOND = "\u79D2";

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
    private ContentObserver mFormatChangeObserver;
    private int mAttached = 0; // for debugging - tells us whether attach/detach is unbalanced
    private boolean fullkanji;

    /* called by system on minute ticks */
    private final Handler mHandler = new Handler();
    private BroadcastReceiver mIntentReceiver;

    private static class TimeChangedReceiver extends BroadcastReceiver {
        private WeakReference<KanjiWeatherClock> mClock;
        private Context mContext;

        public TimeChangedReceiver(KanjiWeatherClock clock) {
            mClock = new WeakReference<KanjiWeatherClock>(clock);
            mContext = clock.getContext();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Post a runnable to avoid blocking the broadcast.
            final boolean timezoneChanged =
                    intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED);
            final KanjiWeatherClock clock = mClock.get();
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

    private static class FormatChangeObserver extends ContentObserver {
        private WeakReference<KanjiWeatherClock> mClock;
        private Context mContext;
        public FormatChangeObserver(KanjiWeatherClock clock) {
            super(new Handler());
            mClock = new WeakReference<KanjiWeatherClock>(clock);
            mContext = clock.getContext();
        }
        @Override
        public void onChange(boolean selfChange) {
            KanjiWeatherClock kanjiClock = mClock.get();
            if (kanjiClock != null) {
                kanjiClock.updateTime();
                kanjiClock.refreshWeather();
            } else {
                try {
                    mContext.getContentResolver().unregisterContentObserver(this);
                } catch (RuntimeException e) {
                    // Shouldn't happen
                }
            }
        }
    }

    public KanjiWeatherClock(Context context) {
        this(context, null);
    }

    public KanjiWeatherClock(Context context, AttributeSet attrs) {
        super(context, attrs);
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

        fullkanji = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCKSCREEN_FUZZY_CLOCK, 1) == 3;

        int font = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.CLOCK_FONT, 0);
        if (font == 0) {
            mTimeDisplay.setTypeface(Typeface.createFromFile("/system/fonts/DroidSans.ttf"));
        } else if (font == 1) {
            mTimeDisplay.setTypeface(Typeface
                    .createFromFile("/system/fonts/DroidSansJapanese.ttf"));
        }
        mCalendar = Calendar.getInstance();
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

        mFormatChangeObserver = null;
        mIntentReceiver = null;
    }

    void updateTime(Calendar c) {
        mCalendar = c;
        refreshWeather();
    }

    /*
     * CyanogenMod Lock screen Weather related functionality
     */
    private static final String URL_YAHOO_API_WEATHER = "http://weather.yahooapis.com/forecastrss?w=%s&u=";
    private static WeatherInfo mWeatherInfo = new WeatherInfo();
    private static final int QUERY_WEATHER = 0;
    private static final int UPDATE_WEATHER = 1;
    private boolean mWeatherRefreshing;

    private Handler mHandling = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case QUERY_WEATHER:
                Thread queryWeather = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        LocationManager locationManager = (LocationManager) mContext.
                                getSystemService(Context.LOCATION_SERVICE);
                        final ContentResolver resolver = mContext.getContentResolver();
                        boolean useCustomLoc = Settings.System.getInt(resolver,
                                Settings.System.WEATHER_USE_CUSTOM_LOCATION, 0) == 1;
                        String customLoc = Settings.System.getString(resolver,
                                    Settings.System.WEATHER_CUSTOM_LOCATION);
                        String woeid = null;

                        // custom location
                        if (customLoc != null && useCustomLoc) {
                            try {
                                woeid = YahooPlaceFinder.GeoCode(mContext, customLoc);
                                if (DEBUG)
                                    Log.d(TAG, "Yahoo location code for " + customLoc + " is " + woeid);
                            } catch (Exception e) {
                                Log.e(TAG, "ERROR: Could not get Location code");
                                e.printStackTrace();
                            }
                        // network location
                        } else {
                            Criteria crit = new Criteria();
                            crit.setAccuracy(Criteria.ACCURACY_COARSE);
                            String bestProvider = locationManager.getBestProvider(crit, true);
                            Location loc = null;
                            if (bestProvider != null) {
                                loc = locationManager.getLastKnownLocation(bestProvider);
                            } else {
                                loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                            }
                            try {
                                woeid = YahooPlaceFinder.reverseGeoCode(mContext, loc.getLatitude(),
                                        loc.getLongitude());
                                if (DEBUG)
                                    Log.d(TAG, "Yahoo location code for current geolocation is " + woeid);
                            } catch (Exception e) {
                                Log.e(TAG, "ERROR: Could not get Location code");
                                e.printStackTrace();
                            }
                        }
                        Message msg = Message.obtain();
                        msg.what = UPDATE_WEATHER;
                        msg.obj = woeid;
                        mHandling.sendMessage(msg);
                    }
                });
                mWeatherRefreshing = true;
                queryWeather.setPriority(Thread.MIN_PRIORITY);
                queryWeather.start();
                break;
            case UPDATE_WEATHER:
                String woeid = (String) msg.obj;
                if (woeid != null) {
                    if (DEBUG) {
                        Log.d(TAG, "Location code is " + woeid);
                    }
                    WeatherInfo w = null;
                    try {
                        w = parseXml(getDocument(woeid));
                    } catch (Exception e) {
                    }
                    mWeatherRefreshing = false;
                    if (w == null) {
                        setNoWeatherData();
                    } else {
                        setWeatherData(w);
                        mWeatherInfo = w;
                    }
                } else {
                    mWeatherRefreshing = false;
                    if (mWeatherInfo.temp.equals(WeatherInfo.NODATA)) {
                        setNoWeatherData();
                    } else {
                        setWeatherData(mWeatherInfo);
                    }
                }
                break;
            }
        }
    };

    /**
     * Reload the weather forecast
     */
    public void refreshWeather() {
        final ContentResolver resolver = mContext.getContentResolver();
            final long interval = Settings.System.getLong(resolver,
                    Settings.System.WEATHER_UPDATE_INTERVAL, 0); // Default to manual
            boolean manualSync = (interval == 0);
            if (!manualSync && (((System.currentTimeMillis() - mWeatherInfo.last_sync) / 60000) >= interval)) {
                updating = true;
                updateTime();
                if (!mWeatherRefreshing && !mHandling.hasMessages(QUERY_WEATHER)) {
                    mHandling.sendEmptyMessage(QUERY_WEATHER);
                }
            } else if (manualSync && mWeatherInfo.last_sync == 0) {
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
        final Resources res = mContext.getResources();
        String conditionCode = w.condition_code;
        String condition_filename = "weather_" + conditionCode;
        int resID = res.getIdentifier(condition_filename, "drawable",
                        mContext.getPackageName());

        if (resID != 0) {
            addDrwb = true;
            drwb = res.getDrawable(resID);
        } else {
            addDrwb = false;
        }
        mLabel = (w.temp + " | " + w.humidity) ;
        mLoc = (w.city + "  ");
        mCond = (w.condition + " ");
        Date lastTime = new Date(mWeatherInfo.last_sync);
        date = DateFormat.getDateFormat(mContext).format(lastTime);
        time = DateFormat.getTimeFormat(mContext).format(lastTime);
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

    /**
     * Get the weather forecast XML document for a specific location
     * @param woeid
     * @return
     */
    private Document getDocument(String woeid) {
        try {
            boolean celcius = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.WEATHER_USE_METRIC, 1) == 1;
            String urlWithDegreeUnit;

            if (celcius) {
                urlWithDegreeUnit = URL_YAHOO_API_WEATHER + "c";
            } else {
                urlWithDegreeUnit = URL_YAHOO_API_WEATHER + "f";
            }

            return new HttpRetriever().getDocumentFromURL(String.format(urlWithDegreeUnit, woeid));
        } catch (IOException e) {
            Log.e(TAG, "Error querying Yahoo weather");
        }

        return null;
    }

    /**
     * Parse the weather XML document
     * @param wDoc
     * @return
     */
    private WeatherInfo parseXml(Document wDoc) {
        try {
            return new WeatherXmlParser(mContext).parseWeatherResponse(wDoc);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Yahoo weather XML document");
            e.printStackTrace();
        }
        return null;
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
        int mSeconds = mCalendar.get(mCalendar.SECOND);
        int mMinutes = mCalendar.get(mCalendar.MINUTE);
        int mHour = mCalendar.get(mCalendar.HOUR);
        int mAmPm = mCalendar.get(mCalendar.AM_PM);
        String mTimeString;

        fullkanji = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCKSCREEN_FUZZY_CLOCK, 1) == 3;

        if (fullkanji) {
           mTimeString = getKanjiHour(mHour, mAmPm) + " " + getKanjiMinute(mMinutes) + " " + getKanjiSecond(mSeconds);
        } else {
           mTimeString = getHours(mHour, mAmPm) + " " + getMinutes(mMinutes) + " " + getSeconds(mSeconds);
        }

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

    private String getKanjiHour(Integer calendarHour, Integer amPm) {
        return (amPm == Calendar.AM ? J_AM : J_PM) + " " + getKanji(calendarHour.toString()) + J_HOUR;
    }

    private String getKanjiMinute(Integer calendarMinute) {
        return getKanji(calendarMinute.toString()) + J_MINUTE;
    }

    private String getKanjiSecond(Integer calendarSecond) {
        return getKanji(calendarSecond.toString()) + J_SECOND;
    }

    private String getHours(Integer calendarHour, Integer amPm) {
        return (amPm == Calendar.AM ? J_AM : J_PM) + " " + calendarHour.toString() + J_HOUR;
    }

    private String getMinutes(Integer calendarMinute) {
        return calendarMinute.toString() + J_MINUTE;
    }

    private String getSeconds(Integer calendarSecond) {
        return calendarSecond.toString() + J_SECOND;
    }

    private String getKanji(String arabicNumber) {
        try {
            Integer.parseInt(arabicNumber);
        } catch (NumberFormatException ex) {
            return "";
        }

        return parseArabicNumber(arabicNumber);
    }

    private String parseArabicNumber(String arabicNumber) {
        Integer numberLength = arabicNumber.length();
        Integer currentDigit = 1;
        String kanji = "";
        CharacterIterator it = new StringCharacterIterator(arabicNumber);

        for (char digit = it.last(); digit != CharacterIterator.DONE; digit = it.previous()) {

            Boolean isPrintableTens = currentDigit > 1 && digit != '0';
            if (isPrintableTens) {
                switch (currentDigit) {
                    case 2:
                        kanji = J_10 + kanji;
                        break;
                    case 3:
                        kanji = J_100 + kanji;
                        break;
                    case 4:
                        kanji = J_1000 + kanji;
                        break;
                    default:
                        break;
                }
            }

            Boolean isPrintableDigit = true;

            Boolean isFirstDigit = currentDigit == 1;
            Boolean isFirstDigitPrintable = numberLength == 1 || digit != '0';
            isPrintableDigit = !isFirstDigit || isFirstDigitPrintable;

            if (!isFirstDigit) {
                isPrintableDigit = digit != '1' && digit != '0';
            }

            if (isPrintableDigit) {
                kanji = digitToKanji(digit) + kanji;
            }

            currentDigit++;
        }

        return kanji;
    }

    private Character iteratorPeek(CharacterIterator iterator) {
        int currentIndex = iterator.getIndex();
        Character peeked = iterator.next();
        iterator.setIndex(currentIndex);
        return peeked;
    }

    private View.OnClickListener mRefreshListener = new View.OnClickListener() {
        public void onClick(View v) {
            updating = true;
            updateTime();
            if (!mWeatherRefreshing && !mHandling.hasMessages(QUERY_WEATHER)) {
                mHandling.sendEmptyMessage(QUERY_WEATHER);
            }
        }
    };

    private String digitToKanji(char digit) {
        String digitString;
        switch (digit) {
            case '0':
                digitString = J_0;
                break;
            case '1':
                digitString = J_1;
                break;
            case '2':
                digitString = J_2;
                break;
            case '3':
                digitString = J_3;
                break;
            case '4':
                digitString = J_4;
                break;
            case '5':
                digitString = J_5;
                break;
            case '6':
                digitString = J_6;
                break;
            case '7':
                digitString = J_7;
                break;
            case '8':
                digitString = J_8;
                break;
            case '9':
                digitString = J_9;
                break;
            default:
                digitString = "";
                break;
        }
        return digitString;
    }
}
