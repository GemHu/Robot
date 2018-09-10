package com.gemhu.blemaster;

import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("InflateParams")
public class MainActivity extends Activity implements OnClickListener {

	private final static String TAG = MainActivity.class.getSimpleName();
	private DeviceDialog mDeviceDialog;

    private BLEService mBleService;
	private DeviceInfo mDeviceInfo;
    private boolean mConnected;
    private boolean mCharacteristicConnected;
	private BluetoothGattCharacteristic mGattCharacteristic;
	
	private TextView txtDeviceName;
	private View viewConnectState;
	private TextView txtConnectStateName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//
		this.initView();
		//
		this.mDeviceInfo = AppConfig.loadDeviceInfo(this);
		// 
		bindBleService();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// 注册广播接收器，用于接受蓝牙先关信息；
        this.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        // 自动连接蓝牙设备；
        if (mBleService != null && this.mDeviceInfo != null) {
            final boolean result = mBleService.connect(mDeviceInfo.address);
            Log.d(TAG, "Connect request result=" + result);
        }
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		this.unregisterReceiver(this.mGattUpdateReceiver);
	}

	private void initView() {
		this.findViewById(R.id.ble_group).setOnClickListener(this);
		this.txtDeviceName = (TextView) findViewById(R.id.ble_device_name);
		this.viewConnectState = findViewById(R.id.ble_status_icon);
		this.txtConnectStateName = (TextView) findViewById(R.id.ble_status_name);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ble_group:
			this.scanDevice();
			break;

		default:
			break;
		}

	}

	// ------------------------------------------------------------//
	// 发现并连接打印机
	// ------------------------------------------------------------//

	private void scanDevice() {
		if (this.mDeviceDialog == null)
			this.mDeviceDialog = new DeviceDialog(this);
		mDeviceDialog.scanDevice(new DeviceDialog.OnClickListener() {

			@Override
			public void onClick(Map<String, Object> data) {
				DeviceInfo info = new DeviceInfo(data);
				if (TextUtils.isEmpty(info.address))
					return;
				
				MainActivity.this.setDeviceInfo(info);
				if (MainActivity.this.mBleService != null) {
					MainActivity.this.mBleService.connect(info.address);
				}
			}

		});
	}
	
	public void setDeviceInfo(DeviceInfo info) {
		if (info == null || TextUtils.isEmpty(info.address))
			return;
		
		this.mDeviceInfo = info;
		AppConfig.saveDeviceInfo(this, info);
	}
	
	/**
	 * 创建一个广播接收器，用于接受蓝牙服务发送的相关信息；
	 * ACTION_GATT_CONNECTED: GATT服务连接成功；
	 * ACTION_GATT_DISCONNECTED: GATT服务断开连接；
	 * ACTION_GATT_SERVICES_DISCOVERED: 发现GATT服务；
	 * ACTION_DATA_AVAILABLE: 从服务端接受数据。 This can be a result of read or notification operations.
	 */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

		@Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEGattCallback.ACTION_GATT_CONNECTED.equals(action)) {
            	// 蓝牙连接成功
                mConnected = true;
                updateBleStatus();
            } else if (BLEGattCallback.ACTION_GATT_DISCONNECTED.equals(action)) {
            	// 蓝牙连接失败
                mConnected = false;
                updateBleStatus();
            } else if (BLEGattCallback.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBleService.getSupportedGattServices());
            	mCharacteristicConnected = connectDevice();
            	// 刷新连接状态
            	updateBleStatus();
            } 
        }
    };
    
    /**
     * 连接设备；
     */
    private boolean connectDevice() {
    	BluetoothGattService gattService = mBleService.getGattService(BLEUUID.SERVICE);
    	if (gattService == null)
    		return false;
    	
    	mGattCharacteristic = gattService.getCharacteristic(BLEUUID.WRITE);
    	if (mGattCharacteristic == null)
    		return false;
    	
		mGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
		
		// 打印机连接成功；
		return true;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEGattCallback.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEGattCallback.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEGattCallback.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEGattCallback.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
	// ------------------------------------------------------------//
	// 启动BLE服务；
	// ------------------------------------------------------------//

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBleService = ((BLEService.LocalBinder) service).getService();
            if (!mBleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                return;
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (mDeviceInfo != null && !TextUtils.isEmpty(mDeviceInfo.address))
            	mBleService.connect(mDeviceInfo.address);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleService = null;
        }
	};
	
	private void bindBleService() {
        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	// ------------------------------------------------------------//
	// 界面相关函数；
	// ------------------------------------------------------------//

	private void updateBleStatus() {
		if (this.mCharacteristicConnected)
			Toast.makeText(this, "设备连接成功", Toast.LENGTH_LONG).show();
		else if (this.mConnected) {
			Toast.makeText(this, "特征值连接成功", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "设备连接失败", Toast.LENGTH_LONG).show();
		}
		
		if (this.mCharacteristicConnected) {
			this.txtConnectStateName.setText("蓝牙已连接");
			this.viewConnectState.setBackgroundResource(R.drawable.ble_state_connected);
			this.txtDeviceName.setText(this.mDeviceInfo.name);
		} else {
			this.txtConnectStateName.setText("蓝牙未连接");
			this.viewConnectState.setBackgroundResource(R.drawable.ble_state_disconnected);
		}
	}
}
