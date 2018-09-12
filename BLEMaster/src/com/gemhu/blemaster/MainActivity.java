package com.gemhu.blemaster;

import java.util.Map;

import com.gemhu.blemaster.RobotManager.OnConnectChangedListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

@SuppressLint("InflateParams")
public class MainActivity extends Activity implements OnClickListener, OnEditorActionListener {

	private final static String TAG = MainActivity.class.getSimpleName();
	private DeviceDialog mDeviceDialog;
	private RobotManager mRobotManager;

	private TextView txtDeviceName;
	private View viewConnectState;
	private TextView txtConnectStateName;
	private int[] limitIds = { //
			R.id.txt_pos_min1, //
			R.id.txt_pos_min2, //
			R.id.txt_pos_min3, //
			R.id.txt_pos_min4, //
			R.id.txt_pos_min5, //
			R.id.txt_pos_min6, //
			R.id.txt_pos_max1, //
			R.id.txt_pos_max2, //
			R.id.txt_pos_max3, //
			R.id.txt_pos_max4, //
			R.id.txt_pos_max5, //
			R.id.txt_pos_max6 //
	};

	private OnSeekBarChangeListener mOnSpeedChangedListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			//
			((TextView) findViewById(R.id.device_running_speed_curr)).setText(Integer.toString(progress));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// 滑动结束后，下发命令，修改实际运行速度；
			mRobotManager.executeSetSpeed(seekBar.getProgress());
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//
		this.initRobotManager();
		// 初始化视图相关信息；
		this.initView();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mRobotManager.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.mRobotManager.onPause();
	}

	private void initView() {
		this.findViewById(R.id.ble_group).setOnClickListener(this);
		this.findViewById(R.id.main_logo).setOnClickListener(this);
		((SeekBar) this.findViewById(R.id.device_running_speed_sb))
				.setOnSeekBarChangeListener(this.mOnSpeedChangedListener);

		this.txtDeviceName = (TextView) findViewById(R.id.ble_device_name);
		this.viewConnectState = findViewById(R.id.ble_status_icon);
		this.txtConnectStateName = (TextView) findViewById(R.id.ble_status_name);
		//
		onRunningModeChanged();
		//

		for (int id : limitIds) {
			((EditText)findViewById(id)).setOnEditorActionListener(this);
		}
//		edit.addTextChangedListener(new TextWatcher() {
//		});
	}

	private void initRobotManager() {
		this.mRobotManager = new RobotManager(this);
		this.mRobotManager.setOnConnectChangedListener(new OnConnectChangedListener() {

			@Override
			public void OnStateChanged(boolean connected) {
				if (connected) {
					DeviceInfo device = AppConfig.getDeviceInfo(MainActivity.this);
					Toast.makeText(MainActivity.this, "设备连接成功", Toast.LENGTH_LONG).show();
					txtConnectStateName.setText("蓝牙已连接");
					viewConnectState.setBackgroundResource(R.drawable.ble_state_connected);
					if (TextUtils.isEmpty(device.name))
						txtDeviceName.setText(device.address);
					else
						txtDeviceName.setText(device.name);

				} else {
					Toast.makeText(MainActivity.this, "设备连接失败", Toast.LENGTH_LONG).show();
					txtConnectStateName.setText("蓝牙未连接");
					viewConnectState.setBackgroundResource(R.drawable.ble_state_disconnected);

				}
			}
		});
		//
		Intent gattServiceIntent = new Intent(this, BLEService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	// ------------------------------------------------------------//
	// 启动BLE服务；
	// ------------------------------------------------------------//

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			BLEService bleService = ((BLEService.LocalBinder) service).getService();
			if (!bleService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				return;
			}
			mRobotManager.setService(bleService);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mRobotManager.setService(null);
		}
	};

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
			this.mRobotManager.executeStartMove(1, true);
			break;
		case R.id.btn_move_add2:
			this.mRobotManager.executeStartMove(2, true);
			break;
		case R.id.btn_move_add3:
			this.mRobotManager.executeStartMove(3, true);
			break;
		case R.id.btn_move_add4:
			this.mRobotManager.executeStartMove(4, true);
			break;
		case R.id.btn_move_add5:
			this.mRobotManager.executeStartMove(5, true);
			break;
		case R.id.btn_move_add6:
			this.mRobotManager.executeStartMove(6, true);
			break;
		case R.id.btn_move_sub1:
			this.mRobotManager.executeStartMove(1, false);
			break;
		case R.id.btn_move_sub2:
			this.mRobotManager.executeStartMove(2, false);
			break;
		case R.id.btn_move_sub3:
			this.mRobotManager.executeStartMove(3, false);
			break;
		case R.id.btn_move_sub4:
			this.mRobotManager.executeStartMove(4, false);
			break;
		case R.id.btn_move_sub5:
			this.mRobotManager.executeStartMove(5, false);
			break;
		case R.id.btn_move_sub6:
			this.mRobotManager.executeStartMove(6, false);
			break;
		case R.id.btn_trace1:
			this.mRobotManager.executeTraceRunning(1);
			break;
		case R.id.btn_trace2:
			this.mRobotManager.executeTraceRunning(2);
			break;
		case R.id.btn_trace3:
			this.mRobotManager.executeTraceRunning(3);
			break;
		case R.id.btn_trace4:
			this.mRobotManager.executeTraceRunning(4);
			break;
		default:
			break;
		}

	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		int id = v.getId();
		switch (id) {
		case R.id.txt_pos_min1:
			this.setLimitValue(v.getText().toString(), 1, false);
			break;
		case R.id.txt_pos_min2:
			this.setLimitValue(v.getText().toString(), 2, false);
			break;
		case R.id.txt_pos_min3:
			this.setLimitValue(v.getText().toString(), 3, false);
			break;
		case R.id.txt_pos_min4:
			this.setLimitValue(v.getText().toString(), 4, false);
			break;
		case R.id.txt_pos_min5:
			this.setLimitValue(v.getText().toString(), 5, false);
			break;
		case R.id.txt_pos_min6:
			this.setLimitValue(v.getText().toString(), 6, false);
			break;
		case R.id.txt_pos_max1:
			this.setLimitValue(v.getText().toString(), 1, true);
			break;
		case R.id.txt_pos_max2:
			this.setLimitValue(v.getText().toString(), 2, true);
			break;
		case R.id.txt_pos_max3:
			this.setLimitValue(v.getText().toString(), 3, true);
			break;
		case R.id.txt_pos_max4:
			this.setLimitValue(v.getText().toString(), 4, true);
			break;
		case R.id.txt_pos_max5:
			this.setLimitValue(v.getText().toString(), 5, true);
			break;
		case R.id.txt_pos_max6:
			this.setLimitValue(v.getText().toString(), 6, true);
			break;
		default:
			break;
		}

		return true;
	}

	private void setLimitValue(String value, int axis, boolean isMaxLimit) {
		if (TextUtils.isEmpty(value))
			return;

		value = value.trim();
		if (value.endsWith("°"))
			value = value.substring(0, value.length() - 1);

		try {
			float fValue = Float.parseFloat(value);
			if (isMaxLimit)
				this.mRobotManager.executeSetMaxPosition(axis, fValue);
			else
				this.mRobotManager.executeSetMinPosition(axis, fValue);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	// ------------------------------------------------------------//
	// 发现并连接打印机
	// ------------------------------------------------------------//

	private void scanDevice() {
		if (this.mDeviceDialog == null) {
			this.mDeviceDialog = new DeviceDialog(this);
		}
		mDeviceDialog.scanDevice(new DeviceDialog.OnClickListener() {

			@Override
			public void onClick(Map<String, Object> data) {
				mRobotManager.setDeviceInfo(new DeviceInfo(data));
			}

		});
	}

	// ------------------------------------------------------------//
	// 界面相关函数；
	// ------------------------------------------------------------//

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

			int mode = AppConfig.getRunningMode(this);
			this.setRunningMode(mode == AppConfig.RUNNING_MODE_NO_LIMIT ? AppConfig.RUNNING_MODE_STANDARD
					: AppConfig.RUNNING_MODE_NO_LIMIT);
		}
	}

	// 切换运行模式，设置0点；
	private void setRunningMode(int mode) {
		int oldMode = AppConfig.getRunningMode(this);
		if (mode == oldMode)
			return;

		if (mode == AppConfig.RUNNING_MODE_STANDARD) {
			this.mRobotManager.executeSetZeroPoint();
		}
		//
		AppConfig.setRunningMode(this, mode);
		this.mRobotManager.executeModeSwitch(mode == AppConfig.RUNNING_MODE_STANDARD);
		//

		this.onRunningModeChanged();
	}

	private void onRunningModeChanged() {
		int mode = AppConfig.getRunningMode(this);
		// 运行模式改变后，需要做如下处理；
		// 2、更新文本框背景色；
		for (int id : this.limitIds) {
			findViewById(id).setEnabled(mode == AppConfig.RUNNING_MODE_NO_LIMIT ? false : true);
		}
		// 限位模式下的相关处理
		SeekBar sBar = (SeekBar) findViewById(R.id.device_running_speed_sb);
		if (mode == AppConfig.RUNNING_MODE_NO_LIMIT) {
			// 禁用滑动条，进制修改速度
			sBar.setEnabled(false);
		} else {
			sBar.setEnabled(true);
		}
	}
}
