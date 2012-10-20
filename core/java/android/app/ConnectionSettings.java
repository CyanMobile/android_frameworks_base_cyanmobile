package android.app;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.ConnectivityManager;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;

import android.server.PowerSaverService;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/** @hide */
public final class ConnectionSettings implements Parcelable {

    private int mConnectionId;
    private int mValue;
    private boolean mOverride;
    private boolean mDirty;

    public static final int PROFILE_CONNECTION_MOBILEDATA = 0;
    public static final int PROFILE_CONNECTION_WIFI = 1;
    public static final int PROFILE_CONNECTION_WIFIAP = 2;
    public static final int PROFILE_CONNECTION_WIMAX = 3;
    public static final int PROFILE_CONNECTION_GPS = 4;
    public static final int PROFILE_CONNECTION_SYNC = 5;
    public static final int PROFILE_CONNECTION_BLUETOOTH = 7;
    public static final int PROFILE_CONNECTION_AIRPLANE = 8;
    public static final int PROFILE_CONNECTION_POWERSAVER = 9;

    /** @hide */
    public static final Parcelable.Creator<ConnectionSettings> CREATOR = new Parcelable.Creator<ConnectionSettings>() {
        public ConnectionSettings createFromParcel(Parcel in) {
            return new ConnectionSettings(in);
        }

        @Override
        public ConnectionSettings[] newArray(int size) {
            return new ConnectionSettings[size];
        }
    };


    public ConnectionSettings(Parcel parcel) {
        readFromParcel(parcel);
    }

    public ConnectionSettings(int connectionId) {
        this(connectionId, 0, false);
    }

    public ConnectionSettings(int connectionId, int value, boolean override) {
        mConnectionId = connectionId;
        mValue = value;
        mOverride = override;
        mDirty = false;
    }

    public int getConnectionId() {
        return mConnectionId;
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int value) {
        mValue = value;
        mDirty = true;
    }

    public void setOverride(boolean override) {
        mOverride = override;
        mDirty = true;
    }

    public boolean isOverride() {
        return mOverride;
    }

    /** @hide */
    public boolean isDirty() {
        return mDirty;
    }

    public void processOverride(Context context) {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean forcedState = getValue() == 1;
        boolean currentState;

        switch (getConnectionId()) {
             case PROFILE_CONNECTION_MOBILEDATA:
                currentState = cm.getMobileDataEnabled();
                if (forcedState != currentState) {
                    cm.setMobileDataEnabled(forcedState);
                }
                break;
            case PROFILE_CONNECTION_AIRPLANE:
                currentState = (Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1);
                if (forcedState) {
                    if (!currentState) {
                        Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 1);
                        Intent intentOn = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                        intentOn.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                        intentOn.putExtra("state", true);
                        context.sendBroadcast(intentOn);
                    }
                } else {
                    if (currentState) {
                        Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
                        Intent intentOff = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                        intentOff.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                        intentOff.putExtra("state", false);
                        context.sendBroadcast(intentOff);
                    }
                }
                break;
            case PROFILE_CONNECTION_POWERSAVER:
                currentState = (Settings.Secure.getInt(context.getContentResolver(),
                       Settings.Secure.POWER_SAVER_MODE,
                       PowerSaverService.POWER_SAVER_MODE_OFF) == PowerSaverService.POWER_SAVER_MODE_ON);
                if (forcedState) {
                    if (!currentState) {
                        Settings.Secure.putInt(context.getContentResolver(),
                            Settings.Secure.POWER_SAVER_MODE, PowerSaverService.POWER_SAVER_MODE_ON);
                    }
                } else {
                    if (currentState) {
                        Settings.Secure.putInt(context.getContentResolver(),
                            Settings.Secure.POWER_SAVER_MODE, PowerSaverService.POWER_SAVER_MODE_OFF);
                    }
                }
                break;
            case PROFILE_CONNECTION_BLUETOOTH:
                currentState = bta.isEnabled();
                if (forcedState) {
                    if (!currentState) {
                        bta.enable();
                    }
                } else {
                    if (currentState) {
                        bta.disable();
                    }
                }
                break;
            case PROFILE_CONNECTION_GPS:
                currentState = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (forcedState) {
                    if (!currentState) {
                        Settings.Secure.setLocationProviderEnabled(context.getContentResolver(), LocationManager.GPS_PROVIDER, true);
                    }
                } else {
                    if (currentState) {
                        Settings.Secure.setLocationProviderEnabled(context.getContentResolver(), LocationManager.GPS_PROVIDER, false);
                    }
                }
                break;
            case PROFILE_CONNECTION_SYNC:
                currentState = ContentResolver.getMasterSyncAutomatically();
                if (forcedState != currentState) {
                    ContentResolver.setMasterSyncAutomatically(forcedState);
                }
                break;
            case PROFILE_CONNECTION_WIFI:
                int wifiApState = wm.getWifiApState();
                currentState = wm.isWifiEnabled();
                if (forcedState) {
                    if ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) || (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED)) {
                        wm.setWifiApEnabled(null, false);
                    }
                    if (!currentState) {
                        wm.setWifiEnabled(true);
                    }
                } else {
                    if (currentState) {
                        wm.setWifiEnabled(false);
                    }
                }
                break;
            case PROFILE_CONNECTION_WIFIAP:
                int wifiState = wm.getWifiState();
                currentState = wm.isWifiApEnabled();
                if (forcedState) {
                    if ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED)) {
                        wm.setWifiEnabled(false);
                    }
                    if (!currentState) {
                        wm.setWifiApEnabled(null, true);
                    }
                } else {
                    if (currentState) {
                        wm.setWifiApEnabled(null, false);
                    }
                }
                break;
            default: break;
        }
    }

    /** @hide */
    public static ConnectionSettings fromXml(XmlPullParser xpp, Context context)
            throws XmlPullParserException, IOException {
        int event = xpp.next();
        ConnectionSettings connectionDescriptor = new ConnectionSettings(0);
        while (event != XmlPullParser.END_TAG || !xpp.getName().equals("connectionDescriptor")) {
            if (event == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                if (name.equals("connectionId")) {
                    connectionDescriptor.mConnectionId = Integer.parseInt(xpp.nextText());
                } else if (name.equals("value")) {
                    connectionDescriptor.mValue = Integer.parseInt(xpp.nextText());
                } else if (name.equals("override")) {
                    connectionDescriptor.mOverride = Boolean.parseBoolean(xpp.nextText());
                }
            }
            event = xpp.next();
        }
        return connectionDescriptor;
    }

    /** @hide */
    public void getXmlString(StringBuilder builder, Context context) {
        builder.append("<connectionDescriptor>\n<connectionId>");
        builder.append(mConnectionId);
        builder.append("</connectionId>\n<value>");
        builder.append(mValue);
        builder.append("</value>\n<override>");
        builder.append(mOverride);
        builder.append("</override>\n</connectionDescriptor>\n");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mConnectionId);
        dest.writeInt(mOverride ? 1 : 0);
        dest.writeInt(mValue);
        dest.writeInt(mDirty ? 1 : 0);
    }

    /** @hide */
    public void readFromParcel(Parcel in) {
        mConnectionId = in.readInt();
        mOverride = in.readInt() != 0;
        mValue = in.readInt();
        mDirty = in.readInt() != 0;
    }


}
