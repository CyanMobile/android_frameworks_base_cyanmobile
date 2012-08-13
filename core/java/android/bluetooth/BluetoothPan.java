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

package android.bluetooth;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.server.BluetoothPanService;
import android.content.Context;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.os.IBinder;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * Public API for controlling the Bluetooth PAN Profile Service.
 *
 * BluetoothPan is a proxy object for controlling the Bluetooth PAN
 * Service via IPC.
 *
 * Creating a BluetoothPan object will initiate a binding with the
 * BluetoothPan service. Users of this object should call close() when they
 * are finished, so that this proxy object can unbind from the service.
 *
 * Currently the BluetoothPan service runs in the system server and this
 * proxy object will be immediately bound to the service on construction.
 *
 * Currently this class provides methods to get info about PAN connections.
 *
 * @hide
 */
public final class BluetoothPan {
    private static final String TAG = "BluetoothPan";
    private static final boolean DBG = false;


    public static final int ROLE_NONE_DISCONNECTED  = 0;
    public static final int ROLE_PANU               = 1;
    public static final int ROLE_NAP                = 2;
    public static final int ROLE_GN                 = 3;

    private final IBluetoothPan mService;

    /**
     * Broadcast an interface added.
     */
    public static final String INTERFACE_ADDED = "PanInterfaceAdded";

    /**
     * Broadcast an interface removed.
     */
    public static final String INTERFACE_REMOVED = "PanInterfaceRemoved";

    /**
     * Broadcast all pan devices is disconnected.
     */
    public static final String ALL_DISCONNECTED = "AllPanDevicesDisconnected";

    /**
    *  Extra the interface name.
    */
    public static final String EXTRA_INTERFACE_NAME = "PanInterfaceName";

    /**
    *  Extra the device address.
    */
    public static final String EXTRA_DEVICE_ADDRESS = "PanDeviceAddress";

    /**
    *  Extra the Pan role.
    */
    public static final String EXTRA_PAN_ROLE = "PanRole";




    /**
     *  Create a BluetoothPan proxy object for interacting with the local
     *  Bluetooth Pan service.
     */
    public BluetoothPan() {
        Log.d(TAG, "Create");
        IBinder b = ServiceManager.getService(BluetoothPanService.BLUETOOTH_PAN_SERVICE);
        if (b != null) {
            mService = IBluetoothPan.Stub.asInterface(b);
        } else {
            Log.w(TAG, "Bluetooth Pan service not available!");

            // Instead of throwing an exception which prevents people from going
            // into Wireless settings in the emulator. Let it crash later when it is actually used.
            mService = null;
        }
    }


    /**
     *  Check if any Remote Pan Devices is connected.
     *  @return a unmodifiable set of connected Remote Pan Devices, or null on error.
     *  @hide
     */
    public Set<BluetoothDevice> getConnectedPanDevices(){
        try {
            return Collections.unmodifiableSet(
                    new HashSet<BluetoothDevice>(Arrays.asList(mService.getConnectedPanDevices())));
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return null;
        }
    }


    /**
     *  Get the local role of an Pan connection
     *  @param device Remote BT device.
     *  @return role code, one of ROLE_
     *  @hide
     */
    public int getPanRole(BluetoothDevice device){
        try {
            return mService.getPanRole(device);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return ROLE_NONE_DISCONNECTED;
        }
    }


    /**
     *  Get the local interface of an Pan connection
     *  @param device Remote BT device.
     *  @return name of the interface or null if device is not connected.
     *  @hide
     */
    public String getPanIface(BluetoothDevice device){
        try {
            return mService.getPanIface(device);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return null;
        }
    }


    /**
     *  Get bluetooth device of an local interface
     *  @param Iface Network interface.
     *  @return Remote bluetooth device or null on error.
     *  @hide
     */
    public BluetoothDevice getPanDevice(String Iface){
        try {
            return mService.getPanDevice(Iface);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return null;
        }
    }


    /**
     *  Check if a specified Pan Device is connected.
     *  @param device Remote BT device.
     *  @return true if connected, false otherwise and on error.
     *  @hide
     */
    public boolean isPanDeviceConnected(BluetoothDevice device){
        try {
            return mService.isPanDeviceConnected(device);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }


    /**
     *  Enable/disable local Bluetooth Pan service.
     *  @param enabled true to enable, false to disable.
     *  @return true if enabled, false otherwise and on error.
     *  @hide
     */
    public boolean enablePanNapService(boolean enabled) {
        try {
            return mService.enablePanNapService(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }


    /**
     *  Check if  Bluetooth Pan Nap service is enabled.
     *  @return true if enabled, false otherwise and on error.
     *  @hide
     */
    public boolean isPanNapServiceEnabled() {
        try {
            return mService.isPanNapServiceEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }


    /**
     *  Check if  Bluetooth network interface is a Pan interface.
     *  @param Iface interface name.
     *  @return true if Pan interface, false otherwise and on error.
     *  @hide
     */
    public boolean isPanIface(String Iface){
        try {
            return mService.isPanIface(Iface);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }


    /**
     *  Disconnect from remote Bluetooth Pan device.
     *  @param device address.
     *  @return true if success, false otherwise.
     *  @hide
     */
    public boolean disconnectPan(String address) {
        try {
            return mService.disconnectPan(address);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

}

