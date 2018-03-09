### Pulse Oximeter Reader for Android

## To Use

First start the bluetooth scanner service to find any turned on device.

'''
    @Override
    protected void onStart() {
        super.onStart();
        if ( mBluetoothAdapter != null || !mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BLUETOOTH_REQUEST_ID);
        }
    }
'''
