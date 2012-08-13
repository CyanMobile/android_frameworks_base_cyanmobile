/*
 * Copyright (C) 2011 ST-Ericsson
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

package android.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothPan;
import android.bluetooth.BluetoothPan;
import android.server.BluetoothPanNetworkObserver;
import android.server.BluetoothService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.os.Binder;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.ServiceManager;
import java.util.ArrayList;



public class BluetoothPanService extends IBluetoothPan.Stub{

    private Context mContext;
    private final String TAG = "BluetoothPanService";
    public static final String BLUETOOTH_PAN_SERVICE = "bluetooth_pan";
    private final BluetoothAdapter mAdapter;
    private final BluetoothService mBluetoothService;
    private BluetoothPanNetworkObserver mBluetoothPanNetworkObserver;
    private boolean mNapServiceRunning;
    private boolean mNapServiceEnabled;
    private static final int INDEX_IFACE   = 0;
    private static final int INDEX_ADDRESS = 1;
    private static final int INDEX_ROLE    = 2;
    private ArrayList<PanDevice> mCachedPanDevices;
    private final boolean DBG = false;


    public BluetoothPanService(Context context, BluetoothService bluetoothService) {
        Log.d(TAG, "BluetoothPanService starting");
        mContext = context;
        mNapServiceRunning = false;
        mNapServiceEnabled = false;
        mBluetoothPanNetworkObserver = null;
        mBluetoothService = bluetoothService;
        if (mBluetoothService == null) {
            throw new RuntimeException("Platform does not support Bluetooth");
        }

        mCachedPanDevices = new ArrayList<PanDevice>();
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        // subscribe to Bluetooth on/off broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothPanNetworkObserver.INTERFACE_ADDED);
        filter.addAction(BluetoothPanNetworkObserver.INTERFACE_REMOVED);

        mContext.registerReceiver(mReceiver, filter);
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON && mNapServiceEnabled) {
                    startNap(true);
                } else if (state == BluetoothAdapter.STATE_TURNING_OFF && mNapServiceEnabled) {
                    startNap(false);
                }
            } else if (action.equals(BluetoothPanNetworkObserver.INTERFACE_ADDED)) {
                refreshCachedPanDevices();
                String Iface = intent.getStringExtra(BluetoothPanNetworkObserver.EXTRA_INTERFACE_NAME);
                PanDevice pd = getCachedPanDevice(Iface);
                if (pd != null) {
                    if (DBG) Log.d(TAG, "BluetoothPanDevice added, Address:" + pd.getAddress()
                                                               + " Iface:" + pd.getIface()
                                                               + " Role:" + pd.getRole());
                    Intent broadcast = new Intent(BluetoothPan.INTERFACE_ADDED);
                    broadcast.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                    broadcast.putExtra(BluetoothPan.EXTRA_INTERFACE_NAME,
                            Iface);
                    broadcast.putExtra(BluetoothDevice.EXTRA_DEVICE,
                        mAdapter.getRemoteDevice(pd.getAddress()));
                    broadcast.putExtra(BluetoothPan.EXTRA_PAN_ROLE,
                            stringToRole(pd.getRole()));
                    mContext.sendStickyBroadcast(broadcast);
                }

            } else if (action.equals(BluetoothPanNetworkObserver.INTERFACE_REMOVED)) {
                String Iface = intent.getStringExtra(BluetoothPanNetworkObserver.EXTRA_INTERFACE_NAME);
                PanDevice pd = getCachedPanDevice(Iface);
                if (pd != null) {
                    if (DBG) Log.d(TAG, "BluetoothPanDevice removed, Address:" + pd.getAddress()
                                                                 + " Iface:" + pd.getIface()
                                                                 + " Role:" + pd.getRole());

                    Intent broadcast = new Intent(BluetoothPan.INTERFACE_REMOVED);
                    broadcast.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                    broadcast.putExtra(BluetoothPan.EXTRA_INTERFACE_NAME,
                            Iface);
                    broadcast.putExtra(BluetoothDevice.EXTRA_DEVICE,
                        mAdapter.getRemoteDevice(pd.getAddress()));
                    broadcast.putExtra(BluetoothPan.EXTRA_PAN_ROLE,
                            stringToRole(pd.getRole()));
                    refreshCachedPanDevices();
                    if (mCachedPanDevices.isEmpty()) {
                        if (DBG) Log.d(TAG, "All BluetoothPanDevices is disconnected");
                        Intent broadcast2 = new Intent(BluetoothPan.ALL_DISCONNECTED);
                        broadcast2.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                        mContext.sendStickyBroadcast(broadcast2);
                    }
                    mContext.sendStickyBroadcast(broadcast);
                }
            } else {
                Log.e(TAG, "Unknown intent");
            }
        }
    };

    public synchronized BluetoothDevice[] getConnectedPanDevices() {
        if (DBG) Log.d(TAG, "getConnectedPanDevices");
        ArrayList<BluetoothDevice> panDevConnects = new ArrayList<BluetoothDevice>();

        synchronized(mCachedPanDevices) {
            try {
                for (Object o : mCachedPanDevices) {
                    PanDevice pd = (PanDevice) o;
                    panDevConnects.add(mAdapter.getRemoteDevice(pd.getAddress()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getCachedPanDevice :" + e);
                return null;
            }
        }
        BluetoothDevice bd[] = new BluetoothDevice[panDevConnects.size()];
        return panDevConnects.toArray(bd);
    }

    public int getPanRole(BluetoothDevice device){
        if (DBG) Log.d(TAG, "getPanRole");
        String role = getRole(device.getAddress());
        return stringToRole(role);
    }

    private int stringToRole (String role) {
        if (DBG) Log.d(TAG, "stringToRole" + role);
        if(role.matches("NAP")) {
            return BluetoothPan.ROLE_NAP;
        } else if (role.matches("PANU")){
            return BluetoothPan.ROLE_PANU;
        } else if (role.matches("GN")){
            return BluetoothPan.ROLE_GN;
        } else {
            Log.e(TAG, "Error unknown role :" + role);
            return BluetoothPan.ROLE_NONE_DISCONNECTED;
        }
    }

    private String roleToString (int role) {
        if (DBG) Log.d(TAG, "roleToString" + role);
        if(role == BluetoothPan.ROLE_NAP) {
            return "NAP";
        } else if (role == BluetoothPan.ROLE_PANU){
            return "PANU";
        } else if (role == BluetoothPan.ROLE_GN){
            return "GN";
        } else {
            Log.e(TAG, "Error unknown role :" + role);
            return null;
        }
    }

    public String getPanIface(BluetoothDevice device){
        if (DBG) Log.d(TAG, "getPanIface" + device.getAddress());
        return getIface(device.getAddress());
    }

    public BluetoothDevice getPanDevice(String Iface){
        if (DBG) Log.d(TAG, "getPanDevice" + Iface);
        String address = getAddress(Iface);
        return mAdapter.getRemoteDevice(address);
    }

    public boolean isPanDeviceConnected(BluetoothDevice device){
        if (DBG) Log.d(TAG, "isPanDeviceConnected" + device.getAddress());
        String address = device.getAddress();

        PanDevice pd = getCachedPanDevice(address);
        if (pd != null) {
            return true;
        } else {
            return false;
        }
    }

    public  boolean isPanIface(String Iface){
        if (DBG) Log.d(TAG, "isPanIface" + Iface);

        PanDevice pd = getCachedPanDevice(Iface);
        if (pd != null) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean enablePanNapService(boolean enabled) {
        if (DBG) Log.d(TAG, "enableBtPan" + enabled);
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
                INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);

        try {
            if (mBluetoothPanNetworkObserver == null) {
                mBluetoothPanNetworkObserver = new BluetoothPanNetworkObserver(mContext);
            }

            if(mAdapter.isEnabled()) {
                mNapServiceEnabled = enabled;
                return startNap(enabled);
            } else {
                mNapServiceEnabled = enabled;
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling Bluetooth PAN :" + e);
            return false;
        }
    }

    private boolean startNap(boolean enabled) {
        if (DBG) Log.d(TAG, "enableNap" + enabled);
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);

        try {
            if (enabled) {
                if (!service.isBtPanStarted()) {
                    service.startBtPanNap();
                    mNapServiceRunning = true;
                    return true;
                }
            } else {
                if (mNapServiceRunning) {
                    disconnectAllPan();
                    service.stopBtPan();
                    mNapServiceRunning = service.isBtPanStarted();
                    if (!mNapServiceRunning) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling Bluetooth NAP :" + e);
            return false;
        }
        return false;
    }

    public boolean isPanNapServiceEnabled() {
        return mNapServiceEnabled;
    }

    private void disconnectAllPan() {
        if (DBG) Log.d(TAG, "getConnectedPanDevices");
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
        synchronized(mCachedPanDevices) {
            try {
                for (Object o : mCachedPanDevices) {
                    PanDevice pd = (PanDevice) o;
                    service.disconnectBtPan(pd.getAddress());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error disconnectAllPanu :" + e);
            }
        }
    }

//TODO sort out how connect should work before make public
    private synchronized boolean connectPan(String address, int role) {
        Log.d(TAG, "connectBtPan" + address);
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
        synchronized(this) {
            try{
                if (mBluetoothPanNetworkObserver == null) {
                    mBluetoothPanNetworkObserver = new BluetoothPanNetworkObserver(mContext);
                }
                service.connectBtPan(address, roleToString(role));
            } catch (Exception e) {
                Log.e(TAG, "Error connectBtPan :" + e);
                return false;
            }
            return true;
        }
    }

    public synchronized boolean disconnectPan(String address) {
        if (DBG) Log.d(TAG, "disconnectBtPan" + address);
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);

        synchronized(this) {
            try{
                service.disconnectBtPan(address);
            } catch (Exception e) {
                Log.e(TAG, "Error disconnectBtPan :" + e);
                return false;
            }
            return true;
        }
    }

    private void refreshCachedPanDevices() {
        if (DBG) Log.d(TAG, "refreshPanDevices");
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);

        synchronized(mCachedPanDevices) {
            try {
               String[] panListConnects = service.listBtPanConnections();
               mCachedPanDevices.clear();
               for (String panListConn : panListConnects) {
                   if (DBG) Log.d(TAG, "List Connected to pan:" + panListConn);
                   PanDevice pd = new PanDevice(panListConn);
                   mCachedPanDevices.add(pd);
               }
           } catch (Exception e) {
               Log.e(TAG, "Error refreshPanDevices :" + e);
           }
       }
    }

    private PanDevice getCachedPanDevice(String name) {
         if (DBG) Log.d(TAG, "getCachedPanDevice" + name);
         synchronized(mCachedPanDevices) {
            try {
                for (Object o : mCachedPanDevices) {
                    PanDevice pd = (PanDevice) o;
                    if(pd.isAddress(name) || pd.isIface(name)) {
                        return pd;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getCachedPanDevice :" + e);
            }
        }
        return null;
    }

    private String getRole(String name) {
        if (DBG) Log.d(TAG, "getRole" + name);
        return getCachedPanDevice(name).getRole();
    }

    private String getAddress(String name) {
        if (DBG) Log.d(TAG, "getAddress" + name);
        return getCachedPanDevice(name).getAddress();
    }

    private String getIface(String name) {
        if (DBG) Log.d(TAG, "getIface" + name);
        return getCachedPanDevice(name).getIface();
    }

    private class PanDevice{
        private String mAddress;
        private String mIface;
        private String mRole;

        private PanDevice(String line) {
            String []tok = line.split(" ");
            mAddress = tok[INDEX_ADDRESS];
            mIface = tok[INDEX_IFACE];
            mRole = tok[INDEX_ROLE];
        }

        private boolean isAddress(String Address) {
            if (mAddress.matches(Address)) {
                return true;
            }
            else {
                return false;
            }
        }

        private boolean isIface(String Iface) {
            if (mIface.matches(Iface)) {
                return true;
            }
            else {
                return false;
            }
        }

        private String getAddress() {
            return mAddress;
        }
        private String getIface() {
            return mIface;
        }
        private String getRole() {
            return mRole;
        }
    }
}

