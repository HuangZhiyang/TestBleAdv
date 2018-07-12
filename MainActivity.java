package com.hzy.testbleadv;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanCallback;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TestBleAdv";
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final String UNKNOWN_SERVICE ="unknown_service";
    private final String UNKNOWN_CHARACTERISTIC ="unknown_characteristic";
    //private final String


    Button btnScan;
    Button btnAdvertise;
    Button btnSend;

    private BluetoothAdapter mBtAdpter;
    private BluetoothManager mBtManager;
    private BluetoothLeScanner mBtLeScanner;
    private BluetoothLeAdvertiser mBtLeAdertiser;
    private static String MY_UUID = "0000F018-0000-1000-8000-00805F9B34FB";
    private Context mContext;
    private Boolean mScanning =false;
    private Boolean mAdvertising = false;
    private boolean isDeviceFound = false;
    //private BroadcastReceiver mBondBroadcastReceiver;
    private String mTargetDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private Boolean  mIsAdvertiser = false;

    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothManager mBluetoothManager;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean  mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mCharacteristic;
    private BluetoothGattCharacteristic mCharacteristicNotify;
    private boolean mWriteCharacteristicFindOK = false;
    private boolean mNotifyCharacteristicFindOK = false;
    private boolean mReadCharacteristicFindOK = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        bindGattService();
        initVariables();
        initViews();
        initListeners();
        initReceivers();
    }
    private void bindGattService(){
        Log.e(TAG,"binGattService");
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mContext.startService(gattServiceIntent);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.e(TAG,"on Service Connected");
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            Log.e(TAG,"on Service DisConnected");
        }
    };




    private void initVariables(){
        mBtAdpter       =  BluetoothAdapter.getDefaultAdapter();
        mBtLeScanner    =  mBtAdpter.getBluetoothLeScanner();
        Log.e(TAG,""+mBtLeScanner);
        mBtLeAdertiser  =  mBtAdpter.getBluetoothLeAdvertiser();
        mIsAdvertiser = false;
        mBluetoothManager =(BluetoothManager)mContext.getSystemService(BLUETOOTH_SERVICE);

        Log.e(TAG,""+mBtLeAdertiser);
    }
    private void initViews(){
        btnScan       = (Button) findViewById(R.id.button_scan);
        btnAdvertise = (Button) findViewById(R.id.button_advertise);
        btnSend       = (Button) findViewById(R.id.button_sendData);
    }

    private void initListeners(){
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBleScan();
            }
        });
        btnAdvertise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBleAdvertiser();
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendNotify();
            }
        });
    }




    private  void initReceivers(){
        registerBondBroadcastReceiver();
        RegisterGattUpdateReceiver();
    }

    private void registerBondBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mBondBroadcastReceiver, intentFilter);
    }
    private void RegisterGattUpdateReceiver(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        mContext.registerReceiver(mGattUpdateReceiver,intentFilter);
    }


    private  void  BondTargetDevice(BluetoothDevice device){
        Log.d(TAG, "device bond state" + device.getBondState());
        final BluetoothDevice tmpDevice = device;
        if (device.getBondState() != BluetoothDevice.BOND_NONE) {
            Log.e(TAG,"当前设备已经配对，直接连接");
            ConnectTargetDevice(device);
        } else {
            boolean createBondSuccess = device.createBond();
            Log.e(TAG, "设备尚未配对，开始进行配对");
        }
    }

    private  void  ConnectTargetDevice(BluetoothDevice device){
        Log.e(TAG, "ConnectTargetDevice" + device.getAddress());
        if(mIsAdvertiser) {

        }else {
          //  connectCentralDevice(device);
        }
        mBluetoothLeService.connect(device.getAddress());

    }

    public boolean connectCentralDevice(BluetoothDevice device) {
        if(mBluetoothGattServer != null ) {
            mBluetoothGattServer.connect(device,false);
        }
        return true;
    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Log.e(TAG,"设备已经链接上");
                //updateConnectionState(R.string.connected);
                //invalidateOptionsMenu();
                if(mIsAdvertiser) {
                }else{
                    //发送ATT Notification
                    //Log.e(TAG,"开始发送ATT Notification");
                    //sendNotification();
                    Log.e(TAG,"开始查找Service");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBluetoothLeService.discoverService();
                        }
                    }, 3000);

                }


            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
                //clearUI();
                Log.e(TAG,"设备已经断开连接");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.e(TAG,"发现GattService");
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.e(TAG,"Data Availble");
                // displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

       //String unknownServiceString = getResources().getString(R.string.unknown_service);
        //String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        //HashMap <String,String> 存放名称和uuid.实际上不需要存方这么多，只需要存放service的UUID就可以了。可以简化代码。
        ArrayList<HashMap<String, String>> allGattServiceInfo = new ArrayList<HashMap<String, String>>();
        //ArrayList<String> gattServiceData = new ArrayList<>();

        //HashMap存放 uuid和名称
        //ArrayList存放一个service的特性
        //外层ArrayList存方多个Service的特性
        ArrayList<ArrayList<HashMap<String, String>>> allGattCharacteristicInfo
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        //存放所有的特性
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        Log.e(TAG,"找到" + gattServices.size() + "个service");

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceInfo = new HashMap<String, String>();  //存放当前Service的UUID和名称
            String Serviceuuid = gattService.getUuid().toString();
            currentServiceInfo.put(LIST_NAME, SampleGattAttributes.lookup(Serviceuuid, UNKNOWN_SERVICE));
            currentServiceInfo.put(LIST_UUID, Serviceuuid);
            allGattServiceInfo.add(currentServiceInfo);
            Log.e(TAG,"Gatt Service uuid:" + Serviceuuid  + "||name:" + SampleGattAttributes.lookup(Serviceuuid, UNKNOWN_SERVICE));

            ArrayList<HashMap<String, String>> characteristicInfoInCurrentService =
                    new ArrayList<HashMap<String, String>>();
            ArrayList<BluetoothGattCharacteristic> charasInCurrtenService =   new ArrayList<BluetoothGattCharacteristic>();   //存方当前Service的所有Characteristics.
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                charasInCurrtenService.add(gattCharacteristic);
                HashMap<String, String> currentCharaInfo = new HashMap<String, String>(); //存方当前characteristicinfo.
                String CharacterUuid = gattCharacteristic.getUuid().toString();
                Log.e(TAG,"Gatt Characteristic uuid:" + CharacterUuid  + "||name:" + SampleGattAttributes.lookup(CharacterUuid, UNKNOWN_CHARACTERISTIC));
                currentCharaInfo.put(LIST_NAME,SampleGattAttributes.lookup(CharacterUuid,UNKNOWN_CHARACTERISTIC) );
                currentCharaInfo.put(LIST_UUID, CharacterUuid);
                characteristicInfoInCurrentService.add(currentCharaInfo);
            }
            Log.e(TAG,"--------------------------------------");
            mGattCharacteristics.add(charasInCurrtenService);
            allGattCharacteristicInfo.add(characteristicInfoInCurrentService);
        }
        LoopCharacteristic();
    }
    private void  LoopCharacteristic(){
        mNotifyCharacteristicFindOK = false;
        mWriteCharacteristicFindOK  = false;
        //new Thread(new Runnable() {
        //    @Override
        //    public void run() {
                Log.e(TAG,"begin to Loop characteristic");
                if (mGattCharacteristics == null) { return; }
                for(int i = 0;i< mGattCharacteristics.size();i++ ) {
                    for (BluetoothGattCharacteristic characteristic : mGattCharacteristics.get(i)) {
                        Log.e(TAG,"第"+i + "个 Service" + "size=" + mGattCharacteristics.get(i).size());
                        Log.e(TAG,"characteristic service" + characteristic.getService().getUuid().toString() + "value:" + characteristic.getValue() );
                        String Characteruuid = characteristic.getUuid().toString();
                        Log.e(TAG,"characteristic uuid" + Characteruuid);
                        //if (Characteruuid.equals(SampleGattAttributes.WIFI_PASSWORD_CHARACTERISTIC)) {
                        //    Log.e(TAG,"find wifi password charateristic");
                        //final int charaProp = characteristic.getProperties();
                        //Log.e(TAG,"charaProp" + charaProp);
                        //if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        //If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        //if (mNotifyCharacteristic != null) {
                        //    Log.e(TAG,"set setCharacteristicNotification false");
                        //    mBluetoothLeService.setCharacteristicNotification(
                        //             mNotifyCharacteristic, false);
                        //     mNotifyCharacteristic = null;
                        // }
                        //  Log.e(TAG,"begin readCharacteristic");
                        //   mBluetoothLeService.readCharacteristic(characteristic);

                        // }
                    /*
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = characteristic;
                        Log.e(TAG,"set setCharacteristicNotification true");
                        mBluetoothLeService.setCharacteristicNotification(
                                characteristic, true);
                    }
                    */

                    /*
                    List<BluetoothGattDescriptor> gattDescsriptors = characteristic.getDescriptors();
                    Log.e(TAG,"gattDescriptors length"+ gattDescsriptors.size());
                    for(BluetoothGattDescriptor gattDescsriptor:gattDescsriptors){
                        Log.e(TAG,"begin readDescriptor");
                        //mBluetoothLeService.readDescritpor(gattDescsriptor);


                        /*String descsriptorUUID = gattDescsriptor.getUuid().toString();
                        Log.e(TAG,"Descsriptor uuid = " + descsriptorUUID);
                        if(gattDescsriptor.getValue() != null) {
                            if (descsriptorUUID.equals(SampleGattAttributes.WIFI_SSID)) {
                                Log.e(TAG, "Descriptor value:" + new String(gattDescsriptor.getValue()));
                            }
                        }else {
                            Log.e(TAG,"descriptor value is null");
                        }
                        */
                        //} else
                        if(Characteruuid.equals(SampleGattAttributes.READ_CHARACTERISTIC)){
                            Log.e(TAG,"找到可读的属性");
                            mReadCharacteristicFindOK = true;
                            ReadDeviceInfo(characteristic);
                        }
                        /*
                        if (Characteruuid.equals(SampleGattAttributes.WRITE_CHARACTERISTIC)) {
                            Log.e(TAG,"找到WRITE_CHARACTERISTIC ");
                            mWriteCharacteristicFindOK  = true;
                            WriteWifiInfo(characteristic);
                        }
                        else if (Characteruuid.equals(SampleGattAttributes.NOTIFY_CHARCTERISTIC)){
                            Log.e(TAG,"找到NOTIFY_CHARCTERISTIC");
                            mNotifyCharacteristicFindOK = true;
                        }*/
                    }
                }
                if ( mNotifyCharacteristicFindOK && mNotifyCharacteristicFindOK && mReadCharacteristicFindOK ) {
                    Log.e(TAG,"特性都找到了,开始执行对应操作");
                }else{
                    Log.e(TAG,"特性没找到，需要重新开始扫描Service");
                }
         //   }
       // }).start();
        }
    private void ReadDeviceInfo(BluetoothGattCharacteristic characteristic){
        Log.e(TAG,"读取魔投设备信息");
        mBluetoothLeService.readCharacteristic(characteristic);
        /*try {
            Thread.sleep(1000);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }
    private void WriteWifiInfo(BluetoothGattCharacteristic characteristic){
        Log.e(TAG,"往魔投端写WiFi热点信息数据");
        byte [] ssid = {'0','b','j','-','b','f','t','v'};//首字符0代表ssid.
        byte [] password = {'1','b','f','t','v','2','0','1','5','1','1','3','0'}; //首字符1代表密码
        byte [] security = {'2','1'};                   //首字符2代表加密方式
        mBluetoothLeService.WriteCharacteristic(characteristic,ssid);
        try {
            Thread.sleep(80);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }

        mBluetoothLeService.WriteCharacteristic(characteristic,password);
        try {
            Thread.sleep(80);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBluetoothLeService.WriteCharacteristic(characteristic,security);
    }


    private  void sendNotify(){
    }

    private void sendReplyNotifyToClient(byte [] requestCmd ,BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic){
        if(characteristic.getUuid().toString().equals(SampleGattAttributes.WRITE_CHARACTERISTIC)) {
            Log.e(TAG,"sendReplyNotifyToClient to " + device.getAddress() +  "requestCmd =" + OutputStringUtils.toHexString(requestCmd) );
            mCharacteristicNotify.setValue("I Got" + OutputStringUtils.toHexString(requestCmd));
            mBluetoothGattServer.notifyCharacteristicChanged(device,mNotifyCharacteristic,false);
        }
    }
    private  final BroadcastReceiver mBondBroadcastReceiver = new  BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.e(TAG, "mBondBroadcastReceiver" + device);
            if (device == null) {
                return;
            }
            if (mTargetDeviceAddress != null && mTargetDeviceAddress.equals(device.getAddress())){
                int currentBondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothAdapter.ERROR);
                int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothAdapter.ERROR);
                if(currentBondState == BluetoothDevice.BOND_BONDED && previousBondState == BluetoothDevice.BOND_BONDING) {
                    //boolean connectSuccess = ConnectTargetDevice(device);
                    Log.e(TAG,"Bonded OK, Begin Connect Devices");
					ConnectTargetDevice(device);
                }
            }
        }
    };




    ScanCallback mBleScanCallbak = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(result.getScanRecord().getServiceUuids() != null) {
                Log.e(TAG,"scanRecord.getDeviceName" + result.getScanRecord().getDeviceName());
                Log.e(TAG,"uuid " +result.getScanRecord().getServiceUuids());
                Log.e(TAG,"onScanResult" +result.toString());
                Log.e(TAG,"rssi  =" +result.getRssi());
                Log.e(TAG,"device.getname" +result.getDevice().getName());
                Log.e(TAG,"tx Level " +result.getScanRecord().getTxPowerLevel());
                Log.e(TAG,"bytes:length" + result.getScanRecord().getBytes().length);
                Log.e(TAG,"bytes:" +Arrays.toString(result.getScanRecord().getBytes()) );
                Log.e(TAG,"" + result.getScanRecord().getManufacturerSpecificData());

                for(ParcelUuid parcelUuid:result.getScanRecord().getServiceUuids()){
                    Log.e(TAG,"parcelUuid.toString()" + parcelUuid.toString());
                    if(parcelUuid.toString().toUpperCase().equals(MY_UUID)){
                            Log.e(TAG,"Find A Little Magic Projector Device,hahahahaha! DeviceName =" + result.getDevice().getName());
                            //此时开始连接小魔投，需要先停止扫描
                        mBtLeScanner.stopScan(mBleScanCallbak);
                        mScanning =false;
                        Log.e(TAG,"find device,Stop Le Scan ");
                        if(isDeviceFound == false){
                            isDeviceFound = true;
                            mTargetDeviceAddress = result.getDevice().getAddress();
                            Log.e(TAG,"Begin to Bond Device " + mTargetDeviceAddress);
                            BondTargetDevice(result.getDevice());
                        }
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.e(TAG,"onBatchScanResultst");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG,"onScanFailed:" + errorCode);
        }

    };

    AdvertiseCallback mBleAdvtiseCallBack = new  AdvertiseCallback(){

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.e(TAG,"start advertise ok");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG,"start advertise fail");
        }
    };

    BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.e(TAG,"on Service Added status" + status );
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.e(TAG,"uuid" + characteristic.getUuid().toString() + " value:" + OutputStringUtils.toHexString(value));
            Log.e(TAG,"");

            if(value[0] =='0') {
                Log.e(TAG, "收到wifi SSID信息");
            }else if (value[0] == '1') {
                Log.e(TAG,"收到 wifi 密码信息");
            }else if (value[0] == '2') {
                Log.e(TAG,"收到Wifi热点安全加密方式信息");
            }

            /*
            if(characteristic.getUuid().toString().equals(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)){

            }*/
           // byte reply[] = {'o','k'};
           // mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,reply);
            //sendReplyNotifyToClient(value, device, requestId, characteristic);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            //super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            //mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset)
            Log.e(TAG, String.format("2.onDescriptorWriteRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("2.onDescriptorWriteRequest：requestId = %s, preparedWrite = %s, responseNeeded = %s, offset = %s, value = %s,", requestId, preparedWrite, responseNeeded, offset,
                    OutputStringUtils.toHexString(value)));
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            //super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.e(TAG,"on onDescriptorReadRequest "  + device +"descriptor" + descriptor.getUuid() );
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.e(TAG,"on Connection State Changed :" + device.getAddress() + "status:" +status + "state" + newState);
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e(TAG,"onConnectionStateChange" +device.getAddress() + "连接成功");
                if(mAdvertising) {
                   // mBtLeAdertiser.stopAdvertising(mBleAdvtiseCallBack);
                }
            }else {
                Log.e(TAG,"onConnectionStateChange" +device.getAddress() + "断开了");
            }
        }


        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            //super.onNotificationSent(device, status);
            Log.e(TAG,"onNotificationSent "  + device +  "status" +status );
            Log.e(TAG, String.format("5.onNotificationSent：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("5.onNotificationSent：status = %s", status));
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.e(TAG,"onCharacteristicReadRequest" + device +"characteristic" + characteristic.getUuid());
            //super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Log.e(TAG,"onMtuChanged" + device );
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            Log.e(TAG,"onExecuteWrite");
        }
    };
    private void initGattServer(){

        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext,mBluetoothGattServerCallback);
        BluetoothGattService service=new BluetoothGattService(UUID.fromString(SampleGattAttributes.USER_DEFINE_SERVICE),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //characteristic for read
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(SampleGattAttributes.READ_CHARACTERISTIC),
        BluetoothGattCharacteristic.PROPERTY_READ ,BluetoothGattCharacteristic.PERMISSION_READ);
        byte hello[] = {'b','f','t','v','c'};
        characteristic.setValue(hello);

        BluetoothGattDescriptor descriptor =new BluetoothGattDescriptor(UUID.fromString(SampleGattAttributes.READ_CHARACTERISTIC),
                BluetoothGattDescriptor.PERMISSION_READ );
        byte ssid[] = {'b','f','t','v'};
        descriptor.setValue(ssid);
        characteristic.addDescriptor(descriptor);
        service.addCharacteristic(characteristic);

        //characteristic for write
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(
                UUID.fromString(SampleGattAttributes.WRITE_CHARACTERISTIC),
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristicWrite);



        //characteristic for reply write cmd;
        mCharacteristicNotify = new BluetoothGattCharacteristic(UUID.fromString(SampleGattAttributes.NOTIFY_CHARCTERISTIC),
                BluetoothGattCharacteristic.PROPERTY_READ| BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(mCharacteristicNotify);

        mBluetoothGattServer.addService(service);
        Log.e(TAG,"Add Service");

    }
    private void startBleScan() {
        if(Build.VERSION.SDK_INT >=23) {
            int checkPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);

            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

            checkPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);

            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
        if(mScanning){
            Log.e(TAG,"上一次扫描还在进行中，请等待扫描结束");
        }
        Log.e(TAG,"startBleScan");
        if(mBtAdpter.isDiscovering()) {
            mBtAdpter.cancelDiscovery();
            Log.e(TAG, "如果当前正在进行普通扫描，取消扫描");
        }
        Log.e(TAG,"先停止可能进行的Ble扫描");
        mBtLeScanner.stopScan(mBleScanCallbak);
        //for(BluetoothDevice device:mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)){
        //    Log.e(TAG,"当前已经连接了设备"+device.getName() + "address:" + device.getAddress());
        //   mBluetoothLeService.disconnect(device);
       // }
        mIsAdvertiser = false;
        Log.e(TAG,"I'm Scnner,not a Advertiser");
        isDeviceFound = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mScanning) {
                    Log.e(TAG, "BleScan超时了,停止扫描");
                    mBtLeScanner.stopScan(mBleScanCallbak);
                }
                mScanning = false;
            }
        }, 30000);
        mScanning = true;
        mBtLeScanner.startScan(mBleScanCallbak);
    }

    private void startBleAdvertiser(){
        mIsAdvertiser = true;
        Log.e(TAG,"I'm a Advertiser");
        if(mScanning) {
            mBtLeScanner.stopScan(mBleScanCallbak);
        }
        Log.e(TAG,"start advertise");
        AdvertiseData advData =  createAdvertiseData();
        if(mBtLeAdertiser== null){
            Toast.makeText(mContext,"本机不支持发送BLE广播",Toast.LENGTH_LONG).show();
            return;
        }

        if(!mAdvertising) {
            mAdvertising = true;
            Log.e(TAG,"Start Advertising");
            mBtLeAdertiser.startAdvertising(createAdvertiseSettings(true, 0), advData, mBleAdvtiseCallBack);
        }
        Log.e(TAG,"开始注册Service和Character");
        initGattServer();

    }

    public static AdvertiseData createAdvertiseData(){
        AdvertiseData.Builder    mDataBuilder = new AdvertiseData.Builder();
        mDataBuilder.addServiceUuid(ParcelUuid.fromString(MY_UUID));
        AdvertiseData mAdvertiseData = mDataBuilder.build();
        if(mAdvertiseData==null){
            Log.e(TAG,"mAdvertiseSettings == null");
        }

        return mAdvertiseData;
    }

    public static AdvertiseSettings  createAdvertiseSettings(boolean connectable, int timeoutMs ){
        AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
        settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingBuilder.setConnectable(connectable);
        settingBuilder.setTimeout(timeoutMs);
        settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        AdvertiseSettings advertiseSettings  = settingBuilder.build();

        return advertiseSettings;
    }

}
