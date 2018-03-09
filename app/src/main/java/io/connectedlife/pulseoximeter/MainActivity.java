package io.connectedlife.pulseoximeter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Locale;

import io.connectedlife.pulseoximeter.data.BCIData;
import io.connectedlife.pulseoximeter.services.BloodOximeterDeviceService;
import io.connectedlife.pulseoximeter.services.BluetoothScanService;

public class MainActivity extends AppCompatActivity {

    private boolean mIsBluetoothEnabled;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int BLUETOOTH_REQUEST_ID = 99;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mBluetoothScanResultBroadcastReceiver;
    private BroadcastReceiver mBluetoothScanStopBroadcastReceiver;
    private BroadcastReceiver mBloodOximeterBroadcastReceiver;
    private String mDeviceAddress;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        else {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }
        mBluetoothScanResultBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ScanResult scanResult = intent.getParcelableExtra(BluetoothScanService.PARAM_SCAN_RESULT);
                if (scanResult.getDevice().getName() != null && scanResult.getDevice().getName().equals("BerryMed")) {
                    Log.d(TAG, "found BerryMed device");
                    if ( mDeviceAddress == null || mDeviceAddress.isEmpty()) {
                        mDeviceAddress = scanResult.getDevice().getAddress();
                        BluetoothScanService.requestStop(MainActivity.this);
                    }
                }
            }
        };

        mBluetoothScanStopBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ( mDeviceAddress != null && !mDeviceAddress.isEmpty()) {
                    Log.d(TAG, "staring BloodOximeter services");
                    BloodOximeterDeviceService.requestData(MainActivity.this, mDeviceAddress);
                }
            }
        };
        mBloodOximeterBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ( BloodOximeterDeviceService.EVENT_CONNECTED.equals(intent.getStringExtra(BloodOximeterDeviceService.PARAM_EVENT_TYPE))) {
                    Log.d(TAG, "Device connected");
                }
                else if ( BloodOximeterDeviceService.EVENT_DATA.equals(intent.getStringExtra(BloodOximeterDeviceService.PARAM_EVENT_TYPE))) {
                    BCIData bciData = intent.getParcelableExtra(BloodOximeterDeviceService.PARAM_DATA);
                    Log.d(TAG, bciData.toString() );
                }
                else if ( BloodOximeterDeviceService.EVENT_ERROR.equals(intent.getStringExtra(BloodOximeterDeviceService.PARAM_EVENT_TYPE))) {
                    Log.d(TAG, String.format(Locale.UK, "Error connecting to device %s", intent.getStringExtra(BloodOximeterDeviceService.PARAM_ERROR_MESSAGE)));
                }
                else if ( BloodOximeterDeviceService.EVENT_DISCONNECTED.equals(intent.getStringExtra(BloodOximeterDeviceService.PARAM_EVENT_TYPE))) {
                    Log.d(TAG, "Device disconnected");
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ( mBluetoothAdapter != null || !mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BLUETOOTH_REQUEST_ID);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(mBluetoothScanResultBroadcastReceiver, new IntentFilter(BluetoothScanService.ON_SCAN_RESULT));
        broadcastManager.registerReceiver(mBluetoothScanStopBroadcastReceiver, new IntentFilter(BluetoothScanService.ON_SCAN_STOP));
        broadcastManager.registerReceiver(mBloodOximeterBroadcastReceiver, new IntentFilter(BloodOximeterDeviceService.ON_EVENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(mBluetoothScanResultBroadcastReceiver);
        broadcastManager.unregisterReceiver(mBluetoothScanStopBroadcastReceiver);
        broadcastManager.unregisterReceiver(mBloodOximeterBroadcastReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( requestCode == BLUETOOTH_REQUEST_ID) {
            mIsBluetoothEnabled = false;
            if ( resultCode == RESULT_OK) {
                mIsBluetoothEnabled = true;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item;
        item = menu.findItem(R.id.action_scan);
        item.setEnabled(mIsBluetoothEnabled);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                backPressed();
                return true;
            case R.id.action_scan:
                BluetoothScanService.startScan(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void backPressed() {

    }

}
