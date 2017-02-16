package uri.egr.biosensing.arduinologgingapp.fragments;

import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

import uri.egr.biosensing.arduinologgingapp.MainActivity;
import uri.egr.biosensing.arduinologgingapp.R;
import uri.egr.biosensing.arduinologgingapp.gatt_attributes.GattCharacteristics;
import uri.egr.biosensing.arduinologgingapp.gatt_attributes.GattServices;
import uri.egr.biosensing.arduinologgingapp.services.BLEConnectionService;
import uri.egr.biosensing.arduinologgingapp.services.CSVLoggingService;

/**
 * Created by mcons on 11/14/2016.
 */

public class DeviceStreamingFragment extends Fragment {
    public static final String HEADER = "date,time,thumb flex angle,index flex angle";
    public static final String BUNDLE_DEVICE_ADDRESS = "bundle_device_address";

    // Measure the voltage at 5V and the actual resistance of your
    // 47k resistor, and enter them below:
    final float VCC = 4.98f; // Measured voltage of Ardunio 5V line
    final float R_DIV = 10000.0f; // Measured resistance of 3.3k resistor

    // Upload the code, then try to adjust these values to more
    // accurately calculate bend degree.
    final float FLEX1_STRAIGHT_RESISTANCE = 20000.0f; // resistance when straight
    final float FLEX1_BEND_RESISTANCE = 98000.0f; // resistance at 90 deg

    final float FLEX2_STRAIGHT_RESISTANCE = 6080.0f;
    final float FLEX2_BEND_RESISTANCE = 20500.0f;

    private String mDeviceAddress;
    private BLEConnectionService mService;
    private MainActivity mActivity;
    private boolean mServiceBound;
    private File mLogFile;
    private View mView;
    private TextView mValueView;
    private ServiceConnection mServiceConnection = new BLEServiceConnection();

    private Button mDisconnectButton;

    private BroadcastReceiver mBLEUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BLE", "Received Update");
            String action = intent.getStringExtra(BLEConnectionService.INTENT_EXTRA);
            switch (action) {
                case BLEConnectionService.GATT_STATE_CONNECTED:
                    mActivity.showMessage("Gatt Server Connected");
                    mService.discoverServices(mDeviceAddress);
                    break;
                case BLEConnectionService.GATT_STATE_DISCONNECTED:
                    mActivity.showMessage("Gatt Server Disconnected");
                    break;
                case BLEConnectionService.GATT_DISCOVERED_SERVICES:
                    mActivity.showMessage("Gatt Services Discovered");
                    //Enable Notifications for Desired Characteristics Here:
                    BluetoothGattCharacteristic characteristic = mService.getCharacteristic(mDeviceAddress, GattServices.UART_SERVICE, GattCharacteristics.TX_CHARACTERISTIC);
                    if (characteristic != null) {
                        mService.enableNotifications(mDeviceAddress, characteristic);
                    }

                    break;
                case BLEConnectionService.GATT_CHARACTERISTIC_READ:
                    byte[] data = intent.getByteArrayExtra(BLEConnectionService.INTENT_DATA);
                    //Parse contents from data here

                    if (data.length != 6) {
                        Log.d(this.getClass().getSimpleName(), "Invalid Data Packet");
                        return;
                    }

                    //Try 3 bytes

                    int flex1 = ((short)data[0] & 0xFF) | (data[1] << 8) | (data[2] << 16); // (0 -> 33023)
                    int flex2 = ((short)data[3] & 0xFF) | (data[4] << 8) | (data[5] << 16);

                    Log.d("DATA", ((short)data[3] & 0xFF) + " " + data[4] + " " + data[5]);

                    float flex1_V = (float) (flex1 * VCC / 1023.0); //
                    float flex1_R = (float) (R_DIV * (VCC / flex1_V - 1.0)); // -30000-> -9905
                    float flex1_angle = map(flex1_R, -30000, -9905, 0f, 90.0f);

                    float flex2_V = (float) (flex2 * VCC / 1023.0);
                    float flex2_R = (float) (R_DIV * (VCC / flex2_V - 1.0));
                    float flex2_angle = map(flex2_R, FLEX2_STRAIGHT_RESISTANCE, FLEX2_BEND_RESISTANCE, 0, 90.0f);

                    float barValue = (flex2_angle);

                    ViewGroup.LayoutParams params = mView.getLayoutParams();
                    params.height = (int) barValue ;
                    mView.setLayoutParams(params);
                    mValueView.setText(String.valueOf(barValue));

                    //Log data
                    CSVLoggingService.start(mActivity, mLogFile, HEADER, flex1_angle + "," + flex2_angle);
                    break;
                case BLEConnectionService.GATT_DESCRIPTOR_WRITE:
                    break;
                case BLEConnectionService.GATT_NOTIFICATION_TOGGLED:
                    break;
                case BLEConnectionService.GATT_DEVICE_INFO_READ:
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle args) {
        super.onCreate(args);
        mActivity = (MainActivity) getActivity();
        mDeviceAddress = getArguments().getString(BUNDLE_DEVICE_ADDRESS, null);
        if (mDeviceAddress == null) {
            mActivity.finish();
            return;
        }

        mLogFile = new File(Environment.getExternalStorageDirectory(), "StreamingLog.csv");

        mActivity.registerReceiver(mBLEUpdateReceiver, new IntentFilter(BLEConnectionService.INTENT_FILTER_STRING));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_streaming, container, false);

        mView = view.findViewById(R.id.progressBar);
        mValueView = (TextView) view.findViewById(R.id.valueText);
        mDisconnectButton = (Button) view.findViewById(R.id.disconnect_button);
        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mService.disconnect(mDeviceAddress);
                mActivity.disconnect();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mActivity.bindService(new Intent(getActivity(), BLEConnectionService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unbindService(mServiceConnection);
        try {
            mActivity.unregisterReceiver(mBLEUpdateReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    float map(float x, float in_min, float in_max, float out_min, float out_max) {
        return (x-in_min)/(in_max-in_min) * (out_max-out_min) + out_min;
    }

    private class BLEServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mServiceBound = true;
            mService = ((BLEConnectionService.BLEConnectionBinder) iBinder).getService();
            Log.d("BLEServiceConnection", "Connecting to Device...");
            mService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("BLEServiceConnection", "Connection Failed");
            mServiceBound = false;
        }
    }
}
