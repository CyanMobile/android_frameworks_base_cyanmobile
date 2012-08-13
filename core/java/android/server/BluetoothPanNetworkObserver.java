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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.INetworkManagementService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ServiceManager;
import android.net.INetworkManagementEventObserver;
import android.util.Log;


public class BluetoothPanNetworkObserver extends INetworkManagementEventObserver.Stub {

    private Context mContext;
    private final String TAG = "BluetoothPanNetworkObserver";

    /**
     * Broadcast an interface added.
     */
    public static final String INTERFACE_ADDED = "BluetoothPanNetworkObserver.NetworkInterfaceAdded";

    /**
     * Broadcast an interface removed.
     */
    public static final String INTERFACE_REMOVED = "BluetoothPanNetworkObserver.NetworkInterfaceRemoved";

    /**
    *  Extra the interface name.
    */
    public static final String EXTRA_INTERFACE_NAME = "BluetoothPanNetworkObserver.NetworkInterfaceName";




    public BluetoothPanNetworkObserver(Context context) {
        Log.d(TAG, "BluetoothPanNetworkObserver starting");
        mContext = context;


        // register for notifications from NetworkManagement Service
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
        try {
            service.registerObserver(this);
        } catch (Exception e) {
            Log.e(TAG, "Error registering observer :" + e);
        }
    }

    public void interfaceLinkStatusChanged(String iface, boolean link) {
        //Do nothing
    }

    public void interfaceAdded(String iface) {
        Log.d(TAG, "interfaceAdded:" + iface);

        Intent broadcast = new Intent(BluetoothPanNetworkObserver.INTERFACE_ADDED);
        broadcast.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        broadcast.putExtra(BluetoothPanNetworkObserver.EXTRA_INTERFACE_NAME,
                iface);
        mContext.sendStickyBroadcast(broadcast);
    }


    public void interfaceRemoved(String iface) {
        Log.d(TAG, "interfaceRemoved:" + iface);

        Intent broadcast = new Intent(BluetoothPanNetworkObserver.INTERFACE_REMOVED);
        broadcast.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        broadcast.putExtra(BluetoothPanNetworkObserver.EXTRA_INTERFACE_NAME,
                iface);
        mContext.sendStickyBroadcast(broadcast);
    }
}



