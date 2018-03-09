# Pulse Oximeter Reader for Android

### To Use

* Listen too bluetooth scanner service scan result BluetoothScanService.ON_SCAN_RESULT

* Listen too bluetooth scanner service stop event BluetoothScanService.ON_SCAN_RESULT

* Listen to BloodOximeterDevice sercive events by using BloodOximeterDeviceService.ON_EVENT

* Startup the bluetooth scanner service to find any turned on device. In this example the user needs to select the 
scan menu item.
```java
BluetoothScanService.startScan(this);
```
* Once your device is found

* Request the scanner to stop, by calling 
```java
if (scanResult.getDevice().getName() != null && scanResult.getDevice().getName().equals("BerryMed")) {
	Log.d(TAG, "found BerryMed device");
	if ( mDeviceAddress == null || mDeviceAddress.isEmpty()) {
		mDeviceAddress = scanResult.getDevice().getAddress();
		BluetoothScanService.requestStop(MainActivity.this);
	}
}
```

* On the bluetooth scanner service stopping you can then ..

* Start the BloodOximeterService with your found device address:
```java
BloodOximeterDeviceService.requestData(MainActivity.this, mDeviceAddress);
```

### Data Format

The data is returned as a BCIData Object, this has the following properties:

```java
public int getSignalStrength()
public int getPleth() 
public int getBargraph() 
public int getPulseRate()
public int getSpO2() 
public boolean isBargraphValid() 
public boolean isPlethValid() 
public boolean isFingerPresent() 
public boolean isPulseFound() 
public boolean isProbePlugged() 
public boolean isSignal()
public boolean isPulseBeep()
public String toString()
```
### Reference Material

For more inforamtion see 

The [android demo app](https://github.com/zh2x/SpO2-BLE-for-Android)

The [protocol data definition](https://github.com/zh2x/BCI_Protocol)

