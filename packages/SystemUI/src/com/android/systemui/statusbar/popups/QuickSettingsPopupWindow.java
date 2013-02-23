/*
 * Copyright (C) 2012 Crossbones Software
 * Copyright (C) 2009 The Android Open Source Project - Some code in this file referenced from AOSP
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

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CheckBox;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.internal.telephony.Phone;
import com.android.systemui.statusbar.CmStatusBarView;
import com.android.systemui.R;

public class QuickSettingsPopupWindow extends QuickSettings implements OnClickListener, CompoundButton.OnCheckedChangeListener {
    public QuickSettingsPopupWindow(View anchor) {
        super(anchor);
    }

    private final String TAG = "QuickSettingsPopupWindow";

    // This widget keeps track of two sets of states:
    // "3-state": STATE_DISABLED, STATE_ENABLED, STATE_INTERMEDIATE
    // "5-state": STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON, STATE_TURNING_OFF, STATE_UNKNOWN
    private static final int STATE_DISABLED = 0;
    private static final int STATE_ENABLED = 1;
    private static final int STATE_TURNING_ON = 2;
    private static final int STATE_TURNING_OFF = 3;
    private static final int STATE_UNKNOWN = 4;
    private static final int STATE_INTERMEDIATE = 5;

    public static final String ACTION_MODIFY_NETWORK_MODE = "com.android.internal.telephony.MODIFY_NETWORK_MODE";
    public static final String ACTION_MOBILE_DATA_CHANGED = "com.android.internal.telephony.MOBILE_DATA_CHANGED";
    public static final String EXTRA_NETWORK_MODE = "networkMode";

    // Is the state in the process of changing?
    private boolean mInTransition = false;
    private Boolean mActualState = null;  // initially not set
    private Boolean mIntendedState = null;  // initially not set

    // Did a toggle request arrive while a state update was
    // already in-flight?  If so, the mIntendedState needs to be
    // requested when the other one is done, unless we happened to
    // arrive at that state already.
    private boolean mDeferredStateChangeRequestNeeded = false;

    // Tracker for toggles
    private static final int WIFI = 1;
    private static final int BLUETOOTH = 2;
    private static final int GPS = 3;
    private static final int SYNC = 4;
    private static final int AIRPL = 5;
    private static final int MBLDATA = 6;
    private static final int AUTOROT = 7;
    private int mToggleTracker = 0;

    @Override
    protected void onCreate() {
        // inflate layout
        LayoutInflater inflater =
                (LayoutInflater) this.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.quick_settings, null);

        // setup button events
        for(int i = 0, icount = root.getChildCount() ; i < icount ; i++) {
            View v = root.getChildAt(i);

            if(v instanceof TableRow) {
                TableRow row = (TableRow) v;

                for(int j = 0, jcount = row.getChildCount() ; j < jcount ; j++) {
                    View item = row.getChildAt(j);

                    if(item instanceof TextView) {
                        TextView tv = (TextView) item;
                        tv.setOnClickListener(this);
                    }
                    if(item instanceof CheckBox) {
                        CheckBox s = (CheckBox) item;
                        s.setOnCheckedChangeListener(this);
                        s.setChecked(getServiceSwitchState(s));

                    }
                }
            }
        }

        // set the inflated view as what we want to display
        this.setContentView(root);
    }

    @Override
    public void onClick(View v) {
        TextView tv = (TextView) v;

        Intent intentSetting = new Intent(Intent.ACTION_MAIN);
        Intent intentStatusBar = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        this.dismiss();
        v.getContext().sendBroadcast(intentStatusBar);

        switch(tv.getId()) {
            case R.id.tv_wifi:
                CmStatusBarView.runSettings("com.android.settings.wifi.WifiSettings", v.getContext());
            break;
            case R.id.tv_bluetooth:
                CmStatusBarView.runSettings("com.android.settings.bluetooth.BluetoothSettings", v.getContext());
            break;
            case R.id.tv_gps:
                CmStatusBarView.runSettings("com.android.settings.SecuritySettings", v.getContext());
            break;
            case R.id.tv_sync:
                Intent intenta = new Intent("android.settings.SYNC_SETTINGS");
                intenta.addCategory(Intent.CATEGORY_DEFAULT);
                intenta.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                v.getContext().startActivity(intenta);
            break;
            case R.id.tv_airpl:
                Intent intentu = new Intent("android.settings.AIRPLANE_MODE_SETTINGS");
                intentu.addCategory(Intent.CATEGORY_DEFAULT);
                intentu.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                v.getContext().startActivity(intentu);
            break;
            case R.id.tv_mbldata:
                CmStatusBarView.runPhoneSettings("com.android.phone.Settings", v.getContext());
            break;
            case R.id.tv_autorot:
                Intent intentur = new Intent("android.settings.DISPLAY_SETTINGS");
                intentur.addCategory(Intent.CATEGORY_DEFAULT);
                intentur.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                v.getContext().startActivity(intentur);
            break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        CheckBox s = (CheckBox) v;

        switch(s.getId()) {
            case R.id.switch_wifi:
                //Log.d(TAG, "Wifi switched to " + isChecked);
                mToggleTracker = WIFI;
                requestStateChange(anchor.getContext(), isChecked);
            break;
            case R.id.switch_bluetooth:
                //Log.d(TAG, "Bluetooth switched to " + isChecked);
                mToggleTracker = BLUETOOTH;
                requestStateChange(anchor.getContext(), isChecked);
            break;
            case R.id.switch_gps:
                //Log.d(TAG, "GPS switched to " + isChecked);
                mToggleTracker = GPS;
                requestStateChange(anchor.getContext(), isChecked);
            break;
            case R.id.switch_sync:
                //Log.d(TAG, "Sync switched to " + isChecked);
                mToggleTracker = SYNC;
                requestStateChange(anchor.getContext(), isChecked);
            break;
            case R.id.switch_airpl:
                //Log.d(TAG, "AIRPLANE switched to " + isChecked);
                mToggleTracker = AIRPL;
                requestStateChange(anchor.getContext(), isChecked);
            break;
            case R.id.switch_mbldata:
                //Log.d(TAG, "MOBILEDATA switched to " + isChecked);
                mToggleTracker = MBLDATA;
                requestStateChange(anchor.getContext(), isChecked);
            break;
            case R.id.switch_autorot:
                //Log.d(TAG, "AUTOROTATE switched to " + isChecked);
                mToggleTracker = AUTOROT;
                requestStateChange(anchor.getContext(), isChecked);
            break;
        }
    }

    private boolean getServiceSwitchState(CheckBox s) {
        boolean serviceStatus = false;

        switch(s.getId()) {
            case R.id.switch_wifi:
                // Wi-Fi state
                WifiManager wm = (WifiManager) anchor.getContext().getSystemService(Context.WIFI_SERVICE);
                serviceStatus = wm.isWifiEnabled();
            break;
            case R.id.switch_bluetooth:
                // Bluetooth state
                BluetoothAdapter ba = (BluetoothAdapter) BluetoothAdapter.getDefaultAdapter();
                serviceStatus = ba.isEnabled();
            break;
            case R.id.switch_gps:
                // GPS state
                LocationManager lm = (LocationManager) this.anchor.getContext().getSystemService(Context.LOCATION_SERVICE);
                serviceStatus = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            break;
            case R.id.switch_sync:
                // Sync state
                ConnectivityManager cm = (ConnectivityManager) this.anchor.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                serviceStatus = ContentResolver.getMasterSyncAutomatically();
            break;
            case R.id.switch_airpl:
                // Airplane state
                serviceStatus = Settings.System.getInt(this.anchor.getContext().getContentResolver(),
                 Settings.System.AIRPLANE_MODE_ON, 0) == 1;
            break;
            case R.id.switch_mbldata:
                // Sync state
                ConnectivityManager cms = (ConnectivityManager) this.anchor.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                serviceStatus = cms.getMobileDataEnabled();
            break;
            case R.id.switch_autorot:
                // AutoRotate state
                serviceStatus = Settings.System.getInt(this.anchor.getContext().getContentResolver(),
                 Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
            break;
        }
        return serviceStatus;
    }

    /*
     * Code to toggle features
     */

    public int getActualState(Context context) {
        if(mToggleTracker == WIFI) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                return wifiStateToFiveState(wifiManager.getWifiState());
            }
            return STATE_UNKNOWN;
        } else if(mToggleTracker == BLUETOOTH) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                return STATE_UNKNOWN;  // On emulator?
            }
            return bluetoothStateToFiveState(bluetoothAdapter.getState());
        } else if(mToggleTracker == GPS) {
            ContentResolver resolver = context.getContentResolver();
            boolean on = Settings.Secure.isLocationProviderEnabled(
                resolver, LocationManager.GPS_PROVIDER);
            return on ? STATE_ENABLED : STATE_DISABLED;
        } else if(mToggleTracker == SYNC) {
            boolean on = ContentResolver.getMasterSyncAutomatically();
            return on ? STATE_ENABLED : STATE_DISABLED;
        } else if(mToggleTracker == AIRPL) {
            ContentResolver resolver = context.getContentResolver();
            boolean on = Settings.System.getInt(resolver,
                 Settings.System.AIRPLANE_MODE_ON, 0) == 1;
            return on ? STATE_ENABLED : STATE_DISABLED;
        } else if(mToggleTracker == MBLDATA) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean on = cm.getMobileDataEnabled();
            return on ? STATE_ENABLED : STATE_DISABLED;
        } else if(mToggleTracker == AUTOROT) {
            ContentResolver resolver = context.getContentResolver();
            boolean on = Settings.System.getInt(resolver,
                 Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
            return on ? STATE_ENABLED : STATE_DISABLED;
        }
        return STATE_UNKNOWN;
    }

    public void requestStateChange(Context context, final boolean desiredState) {
        if(mToggleTracker == WIFI) {
            final WifiManager wifiManager =
                    (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Log.d(TAG, "No wifiManager.");
                return;
            }

            // Actually request the wifi change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    /**
                     * Disable tethering if enabling Wifi
                     */
                    int wifiApState = wifiManager.getWifiApState();
                    if (desiredState && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
                                         (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                        wifiManager.setWifiApEnabled(null, false);
                    }

                    wifiManager.setWifiEnabled(desiredState);
                    return null;
                }
            }.execute();
        } else if(mToggleTracker == BLUETOOTH) {

            // Actually request the Bluetooth change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if(desiredState) {
                        bluetoothAdapter.enable();
                    } else {
                        bluetoothAdapter.disable();
                    }
                    return null;
                }
            }.execute();
        } else if(mToggleTracker == GPS) {
            final ContentResolver resolver = context.getContentResolver();
            final Context fContext = context;

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                    Settings.Secure.setLocationProviderEnabled(
                        resolver,
                        LocationManager.GPS_PROVIDER,
                        desiredState);
                    return desiredState;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(
                        fContext,
                        result ? STATE_ENABLED : STATE_DISABLED);
                }
            }.execute();
        } else if(mToggleTracker == SYNC) {
            //final ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final boolean sync = ContentResolver.getMasterSyncAutomatically();
            final Context fContext = context;

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                    if (desiredState) {
                        if (!sync) {
                            ContentResolver.setMasterSyncAutomatically(true);
                        }
                        return true;
                    }
                    if (sync) {
                        ContentResolver.setMasterSyncAutomatically(false);
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(
                        fContext,
                        result ? STATE_ENABLED : STATE_DISABLED);
                }
            }.execute();
        } else if(mToggleTracker == AIRPL) {
            final ContentResolver resolver = context.getContentResolver();
            final Context fContext = context;

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                        Settings.System.putInt(resolver,
                                  Settings.System.AIRPLANE_MODE_ON, desiredState ? 1 : 0);

                        Intent intente = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                        intente.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                        intente.putExtra("state", desiredState);
                        fContext.sendBroadcast(intente);
                    return desiredState;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(
                        fContext,
                        result ? STATE_ENABLED : STATE_DISABLED);
                }
            }.execute();
        } else if(mToggleTracker == MBLDATA) {
            final ConnectivityManager cms = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final ContentResolver resolver = context.getContentResolver();
            final Context fContext = context;
            final boolean toggleNetworkMode = Settings.System.getInt(resolver,
                Settings.System.EXPANDED_MOBILEDATANETWORK_MODE, 0) == 1;

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                    if(desiredState) {
                        if (toggleNetworkMode) {
                           // mobile data being enabled - switch network mode to 2g/3g
                           Intent intentr = new Intent(ACTION_MODIFY_NETWORK_MODE);
                           intentr.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_WCDMA_PREF);
                           fContext.sendBroadcast(intentr);
                        }
                        // enable mobile data
                        cms.setMobileDataEnabled(true);
                        return true;
                    } else {
                        if (toggleNetworkMode) {
                           // mobile data being disabled - switch network mode to 2g only
                           Intent intentrr = new Intent(ACTION_MODIFY_NETWORK_MODE);
                           intentrr.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_GSM_ONLY);
                           fContext.sendBroadcast(intentrr);
                        }
                        // disable mobile data
                        cms.setMobileDataEnabled(false);
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(
                        fContext,
                        result ? STATE_ENABLED : STATE_DISABLED);
                }
            }.execute();
        } else if(mToggleTracker == AUTOROT) {
            final ContentResolver resolver = context.getContentResolver();
            final Context fContext = context;

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                    if(desiredState) {
                        Settings.System.putInt(resolver,
                                  Settings.System.ACCELEROMETER_ROTATION, 1);
                        return true;
                    } else {
                        Settings.System.putInt(resolver,
                                  Settings.System.ACCELEROMETER_ROTATION, 0);
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(
                        fContext,
                        result ? STATE_ENABLED : STATE_DISABLED);
                }
            }.execute();
        }
    }

    public void onActualStateChange(Context context, Intent intent) {
        if(mToggleTracker == WIFI) {
            if (!WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                return;
            }
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            setCurrentState(context, wifiStateToFiveState(wifiState));
        } else if(mToggleTracker == BLUETOOTH) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }
            int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            setCurrentState(context, bluetoothStateToFiveState(bluetoothState));
        } else if(mToggleTracker == GPS) {
            // Note: the broadcast location providers changed intent
            // doesn't include an extras bundles saying what the new value is.
            setCurrentState(context, getActualState(context));
        } else if(mToggleTracker == SYNC) {
            setCurrentState(context, getActualState(context));
        } else if(mToggleTracker == AIRPL) {
            setCurrentState(context, getActualState(context));
        } else if(mToggleTracker == MBLDATA) {
            setCurrentState(context, getActualState(context));
        } else if(mToggleTracker == AUTOROT) {
            setCurrentState(context, getActualState(context));
        }
    }

    /**
     * Converts WifiManager's state values into our
     * Wifi/Bluetooth-common state values.
     */
    private static int wifiStateToFiveState(int wifiState) {
        switch (wifiState) {
            case WifiManager.WIFI_STATE_DISABLED:
                return STATE_DISABLED;
            case WifiManager.WIFI_STATE_ENABLED:
                return STATE_ENABLED;
            case WifiManager.WIFI_STATE_DISABLING:
                return STATE_TURNING_OFF;
            case WifiManager.WIFI_STATE_ENABLING:
                return STATE_TURNING_ON;
            default:
                return STATE_UNKNOWN;
        }
    }

        /**
         * Converts BluetoothAdapter's state values into our
         * Wifi/Bluetooth-common state values.
         */
        private static int bluetoothStateToFiveState(int bluetoothState) {
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_OFF:
                    return STATE_DISABLED;
                case BluetoothAdapter.STATE_ON:
                    return STATE_ENABLED;
                case BluetoothAdapter.STATE_TURNING_ON:
                    return STATE_TURNING_ON;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    return STATE_TURNING_OFF;
                default:
                    return STATE_UNKNOWN;
            }
        }

    /*
     * State tracker code
     */

    /**
     * Sets the value that we're now in.  To be called from onActualStateChange.
     *
     * @param newState one of STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON,
     *                 STATE_TURNING_OFF, STATE_UNKNOWN
     */
    protected final void setCurrentState(Context context, int newState) {
        final boolean wasInTransition = mInTransition;
        switch (newState) {
            case STATE_DISABLED:
                mInTransition = false;
                mActualState = false;
                break;
            case STATE_ENABLED:
                mInTransition = false;
                mActualState = true;
                break;
            case STATE_TURNING_ON:
                mInTransition = true;
                mActualState = false;
                break;
            case STATE_TURNING_OFF:
                mInTransition = true;
                mActualState = true;
                break;
        }

        if (wasInTransition && !mInTransition) {
            if (mDeferredStateChangeRequestNeeded) {
                Log.v(TAG, "processing deferred state change");
                if (mActualState != null && mIntendedState != null &&
                    mIntendedState.equals(mActualState)) {
                    Log.v(TAG, "... but intended state matches, so no changes.");
                } else if (mIntendedState != null) {
                    mInTransition = true;
                    requestStateChange(context, mIntendedState);
                }
                mDeferredStateChangeRequestNeeded = false;
            }
        }
    }

}
