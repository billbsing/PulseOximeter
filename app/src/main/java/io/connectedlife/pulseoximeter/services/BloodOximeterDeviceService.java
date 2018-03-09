package io.connectedlife.pulseoximeter.services;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.connectedlife.pulseoximeter.data.BCIData;
import io.connectedlife.pulseoximeter.data.BCIReader;


public class BloodOximeterDeviceService extends IntentService {

    private static final String TAG = "BloodOximeterService";

    private static final String ACTION_REQUEST_DATA = "BloodOximeterDeviceService.action_request_data";

    public static final String ON_EVENT = "BloodOximeterDeviceService.on_event";

    public static final String EVENT_DATA = "BloodOximeterDeviceService.event_data";
    public static final String EVENT_ERROR = "BloodOximeterDeviceService.event_error";
    public static final String EVENT_CONNECTED = "BloodOximeterDeviceService.event_connected";
    public static final String EVENT_DISCONNECTED = "BloodOximeterDeviceService.event_disconnected";


    public static final String PARAM_EVENT_TYPE = "BloodOximeterDeviceService.param.event_type";
    public static final String PARAM_ADDRESS = "BloodOximeterDeviceService.param.address";
    public static final String PARAM_ERROR_MESSAGE = "BloodOximeterDeviceService.param.error_message";
    public static final String PARAM_DATA = "BloodOximeterDeviceService.param.data";


    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mBluetoothDevice;
    List<BluetoothGattCharacteristic> mCharacteristics = new ArrayList<>(20);
    private Integer mCharacteristicIndex;
    private boolean mIsConnected;
    private BluetoothGattCharacteristic mReceiveDataCharacteristic;
    private BCIReader mBCIReader = new BCIReader();

    private static final long DEFAULT_THREAD_SLEEP_TIME_MILLIS = 500;


    public static final UUID UUID_SERVICE_DATA              = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455");
    public static final UUID UUID_CHARACTER_RECEIVE         = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616");
    public static final UUID UUID_MODIFY_BT_NAME            = UUID.fromString("00005343-0000-1000-8000-00805F9B34FB");
    public static final UUID UUID_CLIENT_CHARACTER_CONFIG   = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, String.format("onConnection change %d", newState));
            if ( newState == BluetoothGatt.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
                raiseOnEvent(EVENT_CONNECTED);
            }
            else if ( newState == BluetoothGatt.STATE_DISCONNECTED) {
                setConnected(false);
                raiseOnEvent(EVENT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, String.format("onServicesDiscovered: %d", status));
            mReceiveDataCharacteristic = null;

            List<BluetoothGattService> services = gatt.getServices();
            for ( int i = 0; i < services.size(); i ++ ) {
                BluetoothGattService service = services.get(i);
                if ( service.getUuid().equals(UUID_SERVICE_DATA)) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (int j = 0; j < characteristics.size(); j++) {
                        BluetoothGattCharacteristic characteristic = characteristics.get(j);
                        if (characteristic.getUuid().equals(UUID_CHARACTER_RECEIVE)) {
                            mReceiveDataCharacteristic = characteristic;
                        }
                    }
                }
            }
            if ( mReceiveDataCharacteristic != null) {
                setCharacteristicNotification(mReceiveDataCharacteristic, true);
            }
            setConnected(true);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if ( UUID_CHARACTER_RECEIVE.equals(characteristic.getUuid())) {
                processReceivedData(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if ( UUID_CHARACTER_RECEIVE.equals(characteristic.getUuid())) {
                processReceivedData(characteristic.getValue());
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }
    };

    public BloodOximeterDeviceService() {
        super("BloodOximeterDeviceService");
    }

    public static void requestData(Context context, String address) {
        Intent intent = new Intent(context, BloodOximeterDeviceService.class);
        intent.setAction(ACTION_REQUEST_DATA);
        intent.putExtra(PARAM_ADDRESS, address);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if ( intent == null) {
            return;
        }
        final String action = intent.getAction();
        if ( ACTION_REQUEST_DATA.equals(action)) {
            String address = intent.getStringExtra(PARAM_ADDRESS);
            if ( ! init() ) {
                return;
            }
            if ( !connect(address)) {
                return;
            }
        }
    }

    protected void close() {
        disconnect();
    }

    protected boolean init() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if ( bluetoothManager != null ) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        else {
            raiseOnErrorEvent("Cannot get bluetooth service");
            return false;
        }
        setCharacteristicsIndex(-1);
        setConnected(false);
        return true;
    }
    protected boolean connect(String address) {
        if ( mBluetoothAdapter == null) {
            raiseOnErrorEvent("Cannot get bluetooth service");
            return false;
        }
        mBluetoothGatt = null;
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        if ( mBluetoothDevice != null) {
            mBluetoothGatt = mBluetoothDevice.connectGatt(this, false, mBluetoothGattCallback);
        }
        else {
            raiseOnErrorEvent( String.format("Cannot connect to remote device %s", address));
            return false;
        }
        return true;
    }

    protected void disconnect() {
        if ( mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
        mBluetoothGatt = null;
        mBluetoothDevice = null;
    }


    protected synchronized boolean isConnected() {
        return mIsConnected;
    }
    protected synchronized void setConnected(boolean value) {
        mIsConnected = value;
    }

    protected synchronized int getCharacteristicIndex() {
        return mCharacteristicIndex;
    }
    protected synchronized void setCharacteristicsIndex(int value) {
        mCharacteristicIndex = value;
    }
    protected synchronized void incrementCharacterisiticsIndex() {
        mCharacteristicIndex ++;
    }

    protected void raiseOnEvent(String errorType) {
        Intent intent = new Intent(ON_EVENT);
        intent.putExtra(PARAM_EVENT_TYPE, errorType);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(intent);
    }
    protected void raiseOnErrorEvent(String errorMessage) {
        Intent intent = new Intent(ON_EVENT);
        intent.putExtra(PARAM_EVENT_TYPE, EVENT_ERROR);
        intent.putExtra(PARAM_ERROR_MESSAGE, errorMessage);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(intent);
    }

    protected void raiseOnDataEvent(BCIData data) {
        Intent intent = new Intent(ON_EVENT);
        intent.putExtra(PARAM_EVENT_TYPE, EVENT_DATA);
        intent.putExtra(PARAM_DATA, data);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(intent);
    }

    protected void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            raiseOnErrorEvent("BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Oximeter Data Transfer.
        if (UUID_CHARACTER_RECEIVE.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_CLIENT_CHARACTER_CONFIG);
            if (enabled) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }
            else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    protected synchronized void processReceivedData(byte[] rawData) {
        mBCIReader.add(rawData);

        // for safety set a timeout on the while loop, so that we do not
        // spend too much time processing the data
        long timeoutTime = SystemClock.currentThreadTimeMillis() + 1000;
        // loop around the queue and send out any waiting data
        while ( mBCIReader.hasData() && timeoutTime > SystemClock.currentThreadTimeMillis() ) {
            BCIData data = mBCIReader.getData();
            if ( data != null) {
                raiseOnDataEvent(data);
            }
        }
    }
}
