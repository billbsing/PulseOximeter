# Pulse Oximeter Reader for Android

#### To Use

* Listen too bluetooth scanner service scan result BluetoothScanService.ON_SCAN_RESULT

* Listen too bluetooth scanner service stop event BluetoothScanService.ON_SCAN_RESULT

* Listen to BloodOximeterDevice sercive events by using BloodOximeterDeviceService.ON_EVENT

* Startup the bluetooth scanner service to find any turned on device.

* Once your device is found

* Request the scanner to stop, by calling 

	BluetoothScanService.requestStop(MainActivity.this);
	
* On the bluetooth scanner service stopping you can then ..

* Start the BloodOximeterService with your found device address:

	BloodOximeterDeviceService.requestData(MainActivity.this, mDeviceAddress);




