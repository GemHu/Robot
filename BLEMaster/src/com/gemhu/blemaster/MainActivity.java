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
import android.os.SystemClock;
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

	// 运动模式
	private final static int RUNNING_MODE_NO_LIMIT = 0;
	private final static int RUNNING_MODE_STANDARD = 1;
	// 软件第一次运行为无限位模式，在进入标准模式后，保存配置信息，下次打开直接进入标准模式；
	private int mRunningMode = RUNNING_MODE_NO_LIMIT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 初始化视图相关信息；
		this.initView();
		// 初始化数据信息；
		this.initData();
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
		// 断开连接
		if (mBleService != null)
			mBleService.disconnect();
		this.unregisterReceiver(this.mGattUpdateReceiver);
	}

	private void initView() {
		this.findViewById(R.id.ble_group).setOnClickListener(this);
		this.findViewById(R.id.main_logo).setOnClickListener(this);
		
		this.txtDeviceName = (TextView) findViewById(R.id.ble_device_name);
		this.viewConnectState = findViewById(R.id.ble_status_icon);
		this.txtConnectStateName = (TextView) findViewById(R.id.ble_status_name);
	}

	private void initData() {
		// 读取运行模式
		this.mRunningMode = AppConfig.getRunningMode(this, RUNNING_MODE_NO_LIMIT);
		this.onRunningModeChanged();
		// 获取历史纪录，绑定成功后会自动连接
		this.mDeviceInfo = AppConfig.loadDeviceInfo(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ble_group:
			this.scanDevice();
			break;
		case R.id.main_logo:
			// 连续点击5次，切换运行模式；
			this.changeRunningMode();
		case R.id.btn_move_add1:
		case R.id.btn_move_add2:
		case R.id.btn_move_add3:
		case R.id.btn_move_add4:
		case R.id.btn_move_add5:
		case R.id.btn_move_add6:
		case R.id.btn_move_sub1:
		case R.id.btn_move_sub2:
		case R.id.btn_move_sub3:
		case R.id.btn_move_sub4:
		case R.id.btn_move_sub5:
		case R.id.btn_move_sub6:
		case R.id.btn_trace1:
		case R.id.btn_trace2:
		case R.id.btn_trace3:
		case R.id.btn_trace4:
			this.executeCommand(v.getId());
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
	 * 创建一个广播接收器，用于接受蓝牙服务发送的相关信息； ACTION_GATT_CONNECTED: GATT服务连接成功；
	 * ACTION_GATT_DISCONNECTED: GATT服务断开连接； ACTION_GATT_SERVICES_DISCOVERED:
	 * 发现GATT服务； ACTION_DATA_AVAILABLE: 从服务端接受数据。 This can be a result of read or
	 * notification operations.
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
				mCharacteristicConnected = false;
				updateBleStatus();
			} else if (BLEGattCallback.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
				// displayGattServices(mBleService.getSupportedGattServices());
				mCharacteristicConnected = connectDevice();
				// 如果特征值连接失败，则断开蓝牙连接，避免造成不必要的占用
				if (!mCharacteristicConnected)
					mBleService.disconnect();
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
			Toast.makeText(this, "特征值连接成功", Toast.LENGTH_LONG).show();
		else if (this.mConnected) {
			Toast.makeText(this, "设备连接成功", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "设备连接失败", Toast.LENGTH_LONG).show();
		}

		if (this.mCharacteristicConnected) {
			this.txtConnectStateName.setText("蓝牙已连接");
			this.viewConnectState.setBackgroundResource(R.drawable.ble_state_connected);
			if (TextUtils.isEmpty(this.mDeviceInfo.name))
				this.txtDeviceName.setText(this.mDeviceInfo.address);
			else
				this.txtDeviceName.setText(this.mDeviceInfo.name);
		} else {
			this.txtConnectStateName.setText("蓝牙未连接");
			this.viewConnectState.setBackgroundResource(R.drawable.ble_state_disconnected);
		}
	}

	// 需要点击几次 就设置几
	long[] mHits = null;

	/**
	 * 连续点击五次，切换运行模式；
	 */
	private void changeRunningMode() {
		if (mHits == null) {
			mHits = new long[5];
		}
		System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);// 把从第二位至最后一位之间的数字复制到第一位至倒数第一位
		mHits[mHits.length - 1] = SystemClock.uptimeMillis();// 记录一个时间
		if (SystemClock.uptimeMillis() - mHits[0] <= 1000) {// 一秒内连续点击。
			mHits = null; // 这里说明一下，我们在进来以后需要还原状态，否则如果点击过快，第六次，第七次 都会不断进来触发该效果。重新开始计数即可

			if (this.mRunningMode == RUNNING_MODE_NO_LIMIT)
				this.mRunningMode = RUNNING_MODE_STANDARD;
			else
				this.mRunningMode = RUNNING_MODE_NO_LIMIT;
			this.onRunningModeChanged();
		}
	}

	private void onRunningModeChanged() {
		// 运行模式改变后，需要做如下处理；
		// 1、保存当前模式
		AppConfig.setRunningMode(this, this.mRunningMode);
		// 2、更新文本框背景色；
		int[] items = { R.id.txt_pos_min1, R.id.txt_pos_min2, R.id.txt_pos_min3, R.id.txt_pos_min4, R.id.txt_pos_min5,
				R.id.txt_pos_min6, R.id.txt_pos_max1, R.id.txt_pos_max2, R.id.txt_pos_max3, R.id.txt_pos_max4,
				R.id.txt_pos_max5, R.id.txt_pos_max6, };
		for (int i = 0; i < items.length; i++) {
			if (this.mRunningMode == RUNNING_MODE_NO_LIMIT)
				((TextView) findViewById(items[i])).setBackgroundResource(R.drawable.txt_bg_red);
			else
				((TextView) findViewById(items[i])).setBackgroundResource(R.drawable.txt_border_red);
		}
	}
	
	class DataPackage{
		
	}
	
	/**
	 * 执行相关命令
	 * @param cmdId 命令ID（也即对应bnttonID）；
	 */
	private void executeCommand(int cmdId) {
		// 1、根据不同的ID，生成不同的命令；
		byte header = (byte) 0xAA;
		byte cmd = 0x01;
		switch (cmdId) {
		case R.id.btn_move_add1:
		case R.id.btn_move_add2:
		case R.id.btn_move_add3:
		case R.id.btn_move_add4:
		case R.id.btn_move_add5:
		case R.id.btn_move_add6:
		case R.id.btn_move_sub1:
		case R.id.btn_move_sub2:
		case R.id.btn_move_sub3:
		case R.id.btn_move_sub4:
		case R.id.btn_move_sub5:
		case R.id.btn_move_sub6:
		case R.id.btn_trace1:
		case R.id.btn_trace2:
		case R.id.btn_trace3:
		case R.id.btn_trace4:
			
			break;

		default:
			break;
		}
	}
}
