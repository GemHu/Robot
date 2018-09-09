package com.hu.robot;

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.hu.robot.tools.LoopValueTools;
import com.hu.robot.tools.ResponseCheckUtils;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class CharacterOperaterActivity extends Activity implements OnClickListener {

	private static final String TAG = CharacterOperaterActivity.class.getName();
	protected static final String EXTRA_UUID_SERVICE = "uuid_service";
	protected static final String EXTRA_UUID_CHARACTERISTIC = "uuid_characteristic";

	private UUID mUUID_Service;
	private UUID mUUID_Characteristic;
	private BluetoothLeService mBluetoothLeService;
	private BluetoothGattCharacteristic mGattCharacteristic;

	private LinearLayout mWriteLayout;
	private LinearLayout mNotifyLayout;
	private TextView mTVDescreptor;
	private TextView mTVDescreptorValueStr;
	private TextView mTVDescreptorValueHex;

	private RadioGroup mRgOperaterType;
	private Button mBtnOperator;
	private Button mBtnAutoSend;
	private Button mBtnStopSend;
	private RadioButton mRbString;
	private RadioButton mRbHex;

	private EditText mEditInputValue;
	private EditText mEditAutoSendInterval;
	private EditText mEditSendTicks;
	private EditText mEditTotalPeriod;
	private EditText mEditPeriodIndex;
	private EditText mEditSendCount;
	private EditText mEditSendSpeed;

	private ServiceConnection mServiceConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBluetoothLeService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}

			BluetoothGattService gattService = mBluetoothLeService.getGattService(mUUID_Service);
			mGattCharacteristic = gattService.getCharacteristic(mUUID_Characteristic);
			mGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

			updateView();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.characteristics_operater);

		//
		Intent intent = this.getIntent();
		this.mUUID_Service = UUID.fromString(intent.getStringExtra(EXTRA_UUID_SERVICE));
		this.mUUID_Characteristic = UUID.fromString(intent.getStringExtra(EXTRA_UUID_CHARACTERISTIC));
		//
		this.initView();

		//
		Intent service = new Intent(this, BluetoothLeService.class);
		this.bindService(service, this.mServiceConn, BIND_AUTO_CREATE);
	}

	private void initView() {
		mWriteLayout = (LinearLayout) findViewById(R.id.llayout_write);
		mNotifyLayout = (LinearLayout) findViewById(R.id.llayout_notify);

		mTVDescreptor = (TextView) findViewById(R.id.tv_descrepters);
		mTVDescreptorValueStr = (TextView) findViewById(R.id.tv_characteristic_value_str);
		mTVDescreptorValueHex = (TextView) findViewById(R.id.tv_characteristic_value_hex);

		this.mBtnOperator = (Button) findViewById(R.id.btn_characteristic_click);
		this.mBtnAutoSend = (Button) findViewById(R.id.btn_characteristic_autosend);
		this.mBtnStopSend = (Button) findViewById(R.id.btn_characteristic_stopsend);
		this.mRgOperaterType = (RadioGroup) findViewById(R.id.rg_characteristic_type);
		this.mRbString = (RadioButton) findViewById(R.id.rb_characteristic_type_str);
		this.mRbHex = (RadioButton) findViewById(R.id.rb_characteristic_type_hex);

		this.mEditInputValue = (EditText) findViewById(R.id.edt_write_value);
		this.mEditAutoSendInterval = (EditText) findViewById(R.id.edt_write_interval);
		this.mEditSendTicks = (EditText) findViewById(R.id.edt_autosend_ticks);
		this.mEditTotalPeriod = (EditText) findViewById(R.id.edt_send_total_period);
		this.mEditPeriodIndex = (EditText) findViewById(R.id.edt_send_current_period);
		this.mEditSendCount = (EditText) findViewById(R.id.edt_autosend_count);
		this.mEditSendSpeed = (EditText) findViewById(R.id.edt_autosend_speed);

		this.mBtnOperator.setOnClickListener(this);
		this.mBtnAutoSend.setOnClickListener(this);
		this.mBtnStopSend.setOnClickListener(this);

		this.mRbHex.setChecked(true);
	}

	// 更新下布局信息
	private void updateView() {
		if (this.mGattCharacteristic == null)
			return;

		// 1、描述列表
		List<BluetoothGattDescriptor> descriptorList = this.mGattCharacteristic.getDescriptors();
		if (descriptorList != null && descriptorList.size() > 0) {
			StringBuilder value = new StringBuilder();
			for (BluetoothGattDescriptor item : descriptorList) {
				value.append("uuid:" + item.getUuid().toString() + "; value:");
				String itemValue = bytesToHexString(item.getValue());
				if (itemValue == null)
					value.append("null\n");
				else
					value.append(itemValue + "\n");
			}
			mTVDescreptor.setText(value.toString().substring(0, value.length() - 1));
		}
		// 2、特征值
		String value = bytesToHexString(this.mGattCharacteristic.getValue());
		if (value != null) {
			this.mTVDescreptorValueStr.setText(new String(this.mGattCharacteristic.getValue()));
			this.mTVDescreptorValueHex.setText(value);
		}

		int propertis = this.mGattCharacteristic.getProperties();
		if ((propertis & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
			this.mWriteLayout.setVisibility(View.VISIBLE);
			this.mNotifyLayout.setVisibility(View.GONE);

			this.mBtnOperator.setText("发送数据");
		} else if ((propertis & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
			this.mWriteLayout.setVisibility(View.GONE);
			this.mNotifyLayout.setVisibility(View.VISIBLE);
			this.mBtnOperator.setText("开启通知");
		} else {
			this.mWriteLayout.setVisibility(View.GONE);
			this.mNotifyLayout.setVisibility(View.GONE);
			this.mBtnOperator.setText("读取数据");
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_characteristic_click:
			if (this.mWriteLayout.getVisibility() == View.VISIBLE) {
				// 发送；
				this.sendData();
			} else if (this.mNotifyLayout.getVisibility() == View.VISIBLE) {
				// 接受通知；
				this.mBluetoothLeService.setCharacteristicNotification(mGattCharacteristic, false);
			} else {
				// 读取
				this.mBluetoothLeService.readCharacteristic(mGattCharacteristic);
			}
			break;
		case R.id.btn_characteristic_autosend:
			this.autoSendDatas();
			break;
		case R.id.btn_characteristic_stopsend:
			this.mStopSend = true;
			break;
		default:
			break;
		}
	}

	private void sendData() {
		String inputValue = this.mEditInputValue.getText().toString();
		if (TextUtils.isEmpty(inputValue)) {
			Toast.makeText(this, "请输入发送数据！", Toast.LENGTH_LONG);
			return;
		}

		this.sendData(inputValue);
	}

	private boolean mStopSend = false;
	private long mSendBytes;
	private int mPeriodIndex;
	private int mPeriodCount;
	private static final int MSG_WHAT_PERIOD = 1;
	private Handler mHandler = new Handler() {
		public void dispatchMessage(Message msg) {
			switch (msg.what) {
			case MSG_WHAT_PERIOD:

				if (mPeriodIndex < mPeriodCount && !mStopSend) {
					autoSendDataPerPeriod();
					mPeriodIndex++;
					mEditPeriodIndex.setText(mPeriodIndex + "");

					// 继续发送
					Message message = mHandler.obtainMessage(MSG_WHAT_PERIOD);
					mHandler.sendMessageDelayed(message, 3000);
				}

				break;

			default:
				break;
			}
		};
	};

	/**
	 * 自动发送数据，按周期发送。
	 */
	private void autoSendDatas() {
		String inputValue = this.mEditInputValue.getText().toString();
		String sSendPeriods = this.mEditTotalPeriod.getText().toString();
		if (TextUtils.isEmpty(inputValue)) {
			Toast.makeText(this, "请输入发送数据！", Toast.LENGTH_LONG);
			return;
		}

		this.mStopSend = false;
		int iSendPeriods = 0;
		if (!TextUtils.isEmpty(sSendPeriods))
			iSendPeriods = Integer.parseInt(sSendPeriods);
		if (iSendPeriods < 1)
			iSendPeriods = 1;

		this.mPeriodCount = iSendPeriods;
		this.mPeriodIndex = 0;
		this.mEditPeriodIndex.setText(this.mPeriodIndex + "");

		Message message = mHandler.obtainMessage(MSG_WHAT_PERIOD);
		mHandler.sendMessage(message);
	}

	private void autoSendDataPerPeriod() {
		String inputValue = this.mEditInputValue.getText().toString();
		String sTimerInterval = this.mEditAutoSendInterval.getText().toString();
		String sSendTicks = this.mEditSendTicks.getText().toString();

		int iTimerInterval = 0;
		int iSendCountPerPeriod = 0;
		if (!TextUtils.isEmpty(sTimerInterval))
			iTimerInterval = Integer.parseInt(sTimerInterval);
		if (!TextUtils.isEmpty(sSendTicks))
			iSendCountPerPeriod = Integer.parseInt(sSendTicks);

		this.sendDataPerPeriod(inputValue, iTimerInterval, iSendCountPerPeriod);
	}

	public static ResponseCheckUtils ResponseCheckUtils;
	public static int SendPackages;

	private void sendDataPerPeriod(String inputValue, int timerInterval, int sendCountPerPeriod) {
		if (sendCountPerPeriod <= 0) {
			this.sendData(inputValue);
			return;
		}

		this.mBtnAutoSend.setEnabled(false);
		this.mBtnStopSend.setEnabled(false);
		this.mEditPeriodIndex.setText("0");
		this.mSendBytes = 0;
		int index = 0;
		ResponseCheckUtils = new ResponseCheckUtils();
		SendPackages = 0;

		byte[] temp = hexStringToBytes(inputValue);
		LoopValueTools lvp = new LoopValueTools(temp);
		// 开始发送，记录时间；
		long startTime = System.currentTimeMillis();
		// long prevTime = startTime;
		Log.i(TAG, "****** Start to auto send!");
		while (index < sendCountPerPeriod) {
			////
			this.mRbHex.setChecked(true);
			inputValue = "1B61001B61001B61001B401B401B77";
			String sIndex = String.format("%08X", index++);

			if (index == 1) {
				inputValue += (sIndex + "BB");
			} else if (index == sendCountPerPeriod) {
				inputValue += (sIndex + "DD");
			} else {
				inputValue += (sIndex + "CC");
				SendPackages += 3;
			}

			inputValue = inputValue + inputValue + inputValue;
			// Log.i(TAG, inputValue);
			// log.info(inputValue);
			byte[] data = hexStringToBytes(inputValue);
			this.sendHexData(data);

			// this.sendData(inputValue);
			// this.sendHexData(lvp.loopValues(20));
			// long currentTime = System.currentTimeMillis();
			// Log.i(TAG, "TimeSpace = " + (currentTime - prevTime));
			// prevTime = currentTime;

			try {
				if (timerInterval > 0) {
					Thread.sleep(timerInterval);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// 发送结束，记录时间；
		long endTime = System.currentTimeMillis();
		long runTimes = endTime - startTime;
		Log.i(TAG, "Stop to auto send! SendTimes = " + runTimes + "ms");
		Log.i(TAG, "Package send over!!!");
		this.onStopSend(runTimes);

		ResponseCheckUtils.checkResult();
	}

	private void onStopSend(long runTimes) {
		Log.i(TAG, "SendBytes = " + mSendBytes);

		double speed = mSendBytes / (double) runTimes * 1000;
		mEditPeriodIndex.setText(String.format("%s", runTimes));
		mEditSendSpeed.setText(String.format("%s", speed));

		mBtnAutoSend.setEnabled(true);
		mBtnStopSend.setEnabled(true);
	}

	/**
	 * 发送蓝牙写入数据。
	 * 
	 * @param inputValue
	 */
	private void sendData(String inputValue) {
		if (TextUtils.isEmpty(inputValue))
			return;

		if (this.mRbHex.isChecked()) {
			byte[] data = hexStringToBytes(inputValue);
			this.mSendBytes += data.length;
			this.mGattCharacteristic.setValue(data);
		} else {
			this.mSendBytes += inputValue.length();
			this.mGattCharacteristic.setValue(inputValue);
		}

		this.mBluetoothLeService.writeCharacteristic(this.mGattCharacteristic);
	}

	private void sendHexData(byte[] data) {
		this.mSendBytes += data.length;
		this.mGattCharacteristic.setValue(data);
		this.mBluetoothLeService.writeCharacteristic(this.mGattCharacteristic);
	}

	/**
	 * Convert byte[] to hex string.这里我们可以将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。
	 * 
	 * @param src
	 *            byte[] data
	 * @return hex string
	 */
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	/**
	 * Convert hex string to byte[]
	 * 
	 * @param hexString
	 *            the hex string
	 * @return byte[]
	 */
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	/**
	 * Convert char to byte
	 * 
	 * @param c
	 *            char
	 * @return byte
	 */
	public static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}
}
