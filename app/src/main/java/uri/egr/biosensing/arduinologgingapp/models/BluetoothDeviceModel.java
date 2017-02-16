package uri.egr.biosensing.arduinologgingapp.models;

import android.bluetooth.BluetoothDevice;

/**
 * Created by mcons on 11/14/2016.
 */

public class BluetoothDeviceModel {
    private String mDisplayName, mDeviceAddress;
    private int mRSSI;

    public BluetoothDeviceModel(BluetoothDevice bluetoothDevice, int rssi) {
        mDisplayName = bluetoothDevice.getName();
        mDeviceAddress = bluetoothDevice.getAddress();
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getDeviceAddress() {
        return mDeviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        mDeviceAddress = deviceAddress;
    }

    public int getRSSI() {
        return mRSSI;
    }

    public void setRSSI(int RSSI) {
        mRSSI = RSSI;
    }

    @Override
    public String toString() {
        return mDisplayName;
    }
}
