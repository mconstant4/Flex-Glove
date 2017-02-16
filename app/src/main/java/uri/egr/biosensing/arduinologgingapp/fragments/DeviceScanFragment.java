package uri.egr.biosensing.arduinologgingapp.fragments;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import uri.egr.biosensing.arduinologgingapp.MainActivity;
import uri.egr.biosensing.arduinologgingapp.R;
import uri.egr.biosensing.arduinologgingapp.models.BluetoothDeviceModel;

/**
 * Created by mcons on 11/14/2016.
 */

public class DeviceScanFragment extends Fragment {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<BluetoothDeviceModel> mBluetoothDeviceModels;
    private Map<String,BluetoothDevice> mBluetoothDeviceModelMap;

    private TextView mStatus;
    private ListView mListView;
    private ProgressBar mProgressBar;
    private MainActivity mActivity;
    private CountDownTimer mCountDownTimer;

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
            Log.d("BLE", "Device Found (" + bluetoothDevice.getAddress() + ")");
            if (bluetoothDevice.getName() != null && !mBluetoothDeviceModelMap.containsKey(bluetoothDevice.getAddress())) {
                mBluetoothDeviceModelMap.put(bluetoothDevice.getAddress(), bluetoothDevice);
                mBluetoothDeviceModels.add(new BluetoothDeviceModel(bluetoothDevice, rssi));
                mBluetoothDeviceModels.notifyDataSetChanged();
            }
        }
    };

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mActivity.connect(mBluetoothDeviceModels.getItem(i));
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_scan, container, false);

        mStatus = (TextView) view.findViewById(R.id.status);
        mStatus.setText("Scanning...");
        mListView = (ListView) view.findViewById(R.id.list_view);
        mListView.setOnItemClickListener(mItemClickListener);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mActivity = (MainActivity) getActivity();
        mCountDownTimer = new CountDownTimer(10000, 100) {
            @Override
            public void onTick(long l) {
                mProgressBar.setProgress(mProgressBar.getProgress() + 1);
            }

            @Override
            public void onFinish() {
                mStatus.setText("Select Device to Connect");
                mProgressBar.setProgress(mProgressBar.getMax());
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        };

        mBluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mBluetoothDeviceModelMap = new HashMap<>();
        mBluetoothDeviceModels = new ArrayAdapter<BluetoothDeviceModel>(mActivity, android.R.layout.simple_list_item_1);
        mListView.setAdapter(mBluetoothDeviceModels);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mCountDownTimer.start();
    }

    @Override
    public void onStop() {
        super.onStop();

        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mCountDownTimer.cancel();
    }
}
