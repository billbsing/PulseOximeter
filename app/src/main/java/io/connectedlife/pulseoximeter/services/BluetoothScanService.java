package io.connectedlife.pulseoximeter.services;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;


public class BluetoothScanService extends IntentService {
    private static final String ACTION_SCAN = "BluetoothScanService.action.scan";
    private static final String ON_REQUEST_STOP = "BluetoothScanService.on_request_stop";

    public static final String ON_SCAN_RESULT = "BluetoothScanService.on_scan_result";
    public static final String ON_SCAN_STOP = "BluetoothScanService.on_scan_stop";

    public static final String PARAM_TIMEOUT_SECONDS = "BluetoothScanService.param.timeout_seconds";
    public static final String PARAM_SCAN_RESULT = "BluetoothScanService.param.scan_result";

    private static final Integer DEFAULT_TIMEOUT_SECONDS = 30;

    private static final String TAG = "BluetoothScanService";

    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver mOnScanStopBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setScanRunning(false);
        }
    };
    private ScanCallback mScanCallback;
    private static AtomicBoolean mIsScanRunning = new AtomicBoolean(false);

    public BluetoothScanService() {
        super("BluetoothScanService");
    }

    public static void startScan(Context context) {
        startScan(context, DEFAULT_TIMEOUT_SECONDS);
    }

    public static void startScan(Context context, Integer timeoutSeconds) {
        Intent intent = new Intent(context, BluetoothScanService.class);
        intent.putExtra(PARAM_TIMEOUT_SECONDS, timeoutSeconds);
        intent.setAction(ACTION_SCAN);
        context.startService(intent);
    }

    public static void requestStop(Context context) {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        Intent intent = new Intent(ON_REQUEST_STOP);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SCAN.equals(action)) {
                int timeoutSeconds = intent.getIntExtra(PARAM_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
                handleActionScan(timeoutSeconds);
            }
        }
    }

    private void handleActionScan(Integer timeoutSeconds) {

        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                broadcastScanResult(result);
            }
        };
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(mOnScanStopBroadcastReceiver, new IntentFilter(ON_REQUEST_STOP));
        BluetoothManager bluetoothManager = (BluetoothManager)  getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if ( bluetoothAdapter.isEnabled()) {
            Log.d(TAG, String.format("Staring scan for %d seconds", timeoutSeconds));
            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            scanner.startScan(mScanCallback);
            setScanRunning(true);
            long timeoutTime = SystemClock.currentThreadTimeMillis() + timeoutSeconds * DateUtils.SECOND_IN_MILLIS;
            while ( isScanRunning() && SystemClock.currentThreadTimeMillis() < timeoutTime) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Thread interrupted");
                    setScanRunning(false);
                }
            }
            setScanRunning(false);
            Log.d(TAG, "Stop scan");
            scanner.stopScan(mScanCallback);
            Intent intent = new Intent(ON_SCAN_STOP);
            broadcastManager.sendBroadcast(intent);
        }
        broadcastManager.unregisterReceiver(mOnScanStopBroadcastReceiver);
    }

    private void broadcastScanResult(ScanResult scanResult) {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent(ON_SCAN_RESULT);
        intent.putExtra(PARAM_SCAN_RESULT, scanResult);
        broadcastManager.sendBroadcast(intent);
    }

    private synchronized boolean isScanRunning() {
        return mIsScanRunning.get();
    }
    private synchronized void setScanRunning(boolean value) {
        mIsScanRunning.set(value);
    }

}
