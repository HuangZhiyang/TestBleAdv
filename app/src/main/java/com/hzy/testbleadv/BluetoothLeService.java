/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.hzy.testbleadv;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = "TestBleAdv";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;



    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if( (gatt!=null) && (mBluetoothGatt!= null) && (mBluetoothGatt.hashCode()==gatt.hashCode() ) ) {
                Log.e(TAG, "onDescriptorRead  status" + status);
            }

        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            if( (gatt!=null) && (mBluetoothGatt!= null) && (mBluetoothGatt.hashCode()==gatt.hashCode() ) ) {

            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if( (gatt!=null) && (mBluetoothGatt!= null) && (mBluetoothGatt.hashCode()==gatt.hashCode() ) ) {

            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if( (gatt!=null) && (mBluetoothGatt!= null) && (mBluetoothGatt.hashCode()==gatt.hashCode() ) ) {
                Log.d(TAG, "onCharacteristicWrite  status" + status + "uuid" + characteristic.getUuid().toString());
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }




        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if( (gatt!=null) && (mBluetoothGatt!= null) && (mBluetoothGatt.hashCode()==gatt.hashCode() ) ) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.e(TAG, "gatt hashcode:" +gatt.hashCode() + "address:" +gatt.getDevice().getAddress() +"Connected to GATT server.");
                // Attempts to discover services after successful connection.
               // Log.e(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.e(TAG, "gatt hashcode:" +gatt.hashCode() + "address:" +gatt.getDevice().getAddress() +"Disconnected from GATT server.");
                if((gatt != null) && (mBluetoothGatt != null) ){
                    if(gatt.equals(mBluetoothGatt)){
                        Log.e(TAG,"gatt close");   //这里要考虑是采用回连的方式还是如何。
                        //mBluetoothGatt.disconnect();
                        mBluetoothGatt.close();
                        mBluetoothGatt = null;
                    }
                }
                broadcastUpdate(intentAction);
            }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if( (gatt!=null) && (mBluetoothGatt!= null) && (mBluetoothGatt.hashCode()==gatt.hashCode() ) ){
                if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "gatt hashcode:" +gatt.hashCode() + "onServicesDiscovered received: " + status);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.e(TAG, "gatt hashcode:" +gatt.hashCode() + "onServicesDiscovered received: " + status);
            }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if( (gatt!=null) && (mBluetoothGatt!= null) && (mBluetoothGatt.hashCode()==gatt.hashCode() ) ) {
                Log.e(TAG, "gatt hashcode:" + gatt.hashCode() + "onCharacteristicRead received  status " + status);
                Log.e(TAG, "onCharacteristicRead" + characteristic.getUuid().toString() + "value:" + OutputStringUtils.toHexString(characteristic.getValue()));
                if (characteristic.getUuid().toString().equals(SampleGattAttributes.READ_CHARACTERISTIC)) {
                    Log.e(TAG, "读取魔投设备信息成功");
                }
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
                //Log.e(TAG,"charact value" +characteristic.getValue());
            }
        }

        /*
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.e(TAG,"onCharacteristicRead");
            Log.e(TAG,"status" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
            //Log.e(TAG,"charact value" +characteristic.getValue());
        }*/

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if( (gatt!=null) && (mBluetoothGatt!= null) && (mBluetoothGatt.hashCode()==gatt.hashCode() ) ) {
                Log.e(TAG, "gatt hashcode:" + gatt.hashCode() + "onCharacteristicChanged received status ");
                Log.e(TAG, "onCharacteristicChanged" + characteristic.getUuid() + OutputStringUtils.toHexString(characteristic.getValue()));
                Log.e(TAG, "收到魔投端Notify过来的数据" + OutputStringUtils.toHexString(characteristic.getValue()));
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public void discoverService(){
        if(mBluetoothGatt != null) {
            mBluetoothGatt.discoverServices();
        }else {
            //在连接过程中快速断开可能会导致这个问题
            Log.e(TAG,"discoverService mBluetoothGatt is null");
        }
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.e(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.e(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.e(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }


    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG,"BluetoothLeService onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        Log.e(TAG,"BluetoothLeService onUnBind");
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */

    public boolean connect(final String address) {
        boolean reconnect = true;
        if (mBluetoothAdapter == null || address == null) {
            Log.e(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        /*
        if(mBluetoothGatt != null) {
            for (BluetoothDevice device : mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)) {
                Log.e(TAG,"device" +device.getAddress() + "与当前设备处于连接状态");
                if (device.getAddress().equals(address)) {
                    //当前设备已经连接上了
                    Log.e(TAG,"当前设备已经处于连接状态");
                    mBluetoothGatt.disconnect();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    reconnect = false;
                    break;
                 }
        }
        }
        */


        // Previously connected device.  Try to reconnect.
        /*实际发现这种重连功能不好用，还是在断开时close掉这个bluetoothGatt
        if (reconnect && mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.e(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        */


        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.e(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.e(TAG, "Trying to create a new connection.");
        Log.e(TAG,"device.connectGatt mBluetoothGatt  HashCode:" +mBluetoothGatt.hashCode());
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /*added by huangzhiyang for disconnect connected device*/
    public void disconnect(BluetoothDevice device) {

        if (mBluetoothGatt != null) {
                if(mBluetoothGatt.getDevice().getAddress().equals(device.getAddress())) {
                    Log.e(TAG, "gatt disconnect");
                    mBluetoothGatt.disconnect();
                }else{
                    Log.e(TAG,"连接的其他设备，不用管，不影响，不过也可以考虑断开掉");
                }
        } else {
            Log.e(TAG,"mBluetoothGatt is Null");
            //当前设备已经连接上，但是 mBluetoothGatt为空,也就是说不是当前应用连接的，
            //或者说当前应用已经被杀了

            /*不能连接后又去断开，这样会导致整个流程异常.会走到discovery service.
            Log.e(TAG, "gatt connect");
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            //refreshGattCache(mBluetoothGatt);
            Log.e(TAG," device connectGatt  mBluetoothGatt HashCode:" +mBluetoothGatt.hashCode());
            try {
                Thread.sleep(100);//加一点点延时
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "gatt disconnect");
            Log.e(TAG,"mBluetoothGattdisconnect   HashCode:" +mBluetoothGatt.hashCode());
            mBluetoothGatt.disconnect();
            */

        }
    }

    public static boolean refreshGattCache(BluetoothGatt gatt) {
        boolean result = false;
        try {
            if (gatt != null) {
                Method refresh = BluetoothGatt.class.getMethod("refresh");
                if (refresh != null) {
                    refresh.setAccessible(true);
                    result = (boolean) refresh.invoke(gatt, new Object[0]);
                }
            }
        } catch (Exception e) {
            Log.e(TAG,e.toString());
        }

       Log.e(TAG,String.format("refreshDeviceCache return %b", result));

        return result;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.e(TAG,"mBluetoothGatt disconnect HashCode:" +mBluetoothGatt.hashCode());

        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.e(TAG,"mBluetoothGatt close HashCode:" +mBluetoothGatt.hashCode());
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.e(TAG,"readCharacteristic addr = "+ mBluetoothGatt.getDevice().getAddress());
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.e(TAG,"setCharacteristicNotification uuid" + characteristic.getUuid() + "enabled" +enabled );
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        //if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
       // }

    }

    public void readDescritpor(BluetoothGattDescriptor descriptor){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.e(TAG,"readDescritpor addr = "+ mBluetoothGatt.getDevice().getAddress() );
        Log.e(TAG,"readDescritpor uuid = " + descriptor.getUuid());
        mBluetoothGatt.readDescriptor(descriptor);
    }

    public void WriteCharacteristic(BluetoothGattCharacteristic characteristic ,byte[] value){
        //characteristic.setWriteType();
        characteristic.setValue(value);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

}
