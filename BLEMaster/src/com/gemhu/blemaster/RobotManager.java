package com.gemhu.blemaster;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

/**
 * 机器人相关控制操作管理类；
 * 
 * @author hdx_h
 *
 */
public class RobotManager {
	private final static String TAG = RobotManager.class.getSimpleName();

	private BLEService mService;
	private BluetoothGattCharacteristic mCharacteristic;
	private OnConnectChangedListener mChangedListener;
	private OnDataChangedListener mOnDataChangedListener;

	/**
	 * 命令消息队列；消息的发送机处理需要一段时间， 为避免连续发送的时候出现发送失败的问题，需要等到上一条消息处理完毕后，在发送吓一跳消息；
	 */
	private BlockingDeque<Worker> mCmdQueue;
	/**
	 * 当前正在处理的命令；
	 */
	private Worker mcurrWorker;
	private int mSendCount = 0;
	private OnWriteListener mWriteListener = new OnWriteListener() {

		@Override
		public void onWriteSuccess(final BluetoothGattCharacteristic characteristic) {
			Log.i(TAG, "Data write success: " + Utils.bytesToHexString(characteristic.getValue()));

			if (mcurrWorker != null && mcurrWorker.getWriteListener() != null)
				mContext.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (mcurrWorker != null)
							mcurrWorker.getWriteListener().onWriteSuccess(characteristic);
					}
				});
		}

		@Override
		public void onResponse(final BluetoothGattCharacteristic characteristic) {
			Log.i(TAG, "Data receive success: " + Utils.bytesToHexString(characteristic.getValue()));
			if (mcurrWorker != null && mcurrWorker.getWriteListener() != null)
				mContext.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (mcurrWorker != null)
							mcurrWorker.getWriteListener().onResponse(characteristic);
					}
				});
		}

		@Override
		public void onFailure(final int code) {
			Log.i(TAG, "Data write failed: " + code);
			if (mcurrWorker != null && mcurrWorker.getWriteListener() != null)
				mContext.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (mcurrWorker != null)
							mcurrWorker.getWriteListener().onFailure(code);
					}
				});
		}
	};

	private Handler mHandler = new Handler(Looper.getMainLooper());

	interface OnConnectChangedListener {
		void OnStateChanged(boolean connected);
	}

	interface OnDataChangedListener {

		void onSpeedChanged(float speed);

		void onPosChanged(float pos, int axis);
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
			} else if (BLEGattCallback.ACTION_GATT_DISCONNECTED.equals(action)) {
				// 蓝牙连接失败
				mCharacteristic = null;
				if (mChangedListener != null)
					mChangedListener.OnStateChanged(false);
			} else if (BLEGattCallback.ACTION_DATA_AVAILABLE.equals(action)) {
				// 接收到消息
				// byte[] data = intent.getByteArrayExtra(BLEGattCallback.EXTRA_DATA);
				// if (data != null)
				// RobotManager.this.onReceiveData(data);
			} else if (BLEGattCallback.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
				// displayGattServices(mBleService.getSupportedGattServices());
				if (mService == null)
					return;

				mCharacteristic = getCharacteristic();
				if (mCharacteristic == null) {
					mService.disconnect();
					return;
				}
				// 更新蓝牙连接状态；
				mService.setOnWriteListener(mWriteListener);

				if (mChangedListener != null) {
					mChangedListener.OnStateChanged(true);
				}
				// 订阅特征值，用于接收信息
				mService.setCharacteristicNotification(mCharacteristic, true);
//				if ((mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
//					return;
//				BluetoothGattDescriptor descriptor = mCharacteristic.getDescriptor(BLEUUID.CONFIG);
//				if (descriptor == null)
//					return;
//				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//				mService.mBluetoothGatt.writeDescriptor(descriptor);
//				mService.mBluetoothGatt.setCharacteristicNotification(mCharacteristic, true);
			}
		}
	};

	private Activity mContext;

	public RobotManager(Activity context) {
		this.mContext = context;
	}

	public void onResume() {
		//

		// 注册广播接收器，用于接受蓝牙先关信息；
		this.mContext.registerReceiver(this.mGattUpdateReceiver, makeGattUpdateIntentFilter());
		// 自动连接蓝牙设备；
		this.connectDevice(AppConfig.getDeviceInfo(this.mContext));
	}

	public void onPause() {
		// 断开连接
		if (this.mService != null)
			this.mService.disconnect();
		this.mContext.unregisterReceiver(this.mGattUpdateReceiver);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BLEGattCallback.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BLEGattCallback.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BLEGattCallback.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BLEGattCallback.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	public void setService(BLEService service) {
		this.mService = service;
		this.connectDevice(AppConfig.getDeviceInfo(this.mContext));
	}

	public void setDeviceInfo(DeviceInfo device) {
		AppConfig.setDeviceInfo(mContext, device);
		this.connectDevice(device);
	}

	public void setOnConnectChangedListener(OnConnectChangedListener listener) {
		this.mChangedListener = listener;
	}

	public void setOnDataChangedListener(OnDataChangedListener listener) {
		this.mOnDataChangedListener = listener;
	}

	private BluetoothGattCharacteristic getCharacteristic() {
		BluetoothGattService service = this.mService.getGattService(BLEUUID.SERVICE);
		if (service == null)
			return null;

		return service.getCharacteristic(BLEUUID.WRITE);
	}

	private void connectDevice(DeviceInfo device) {
		if (this.mService == null || device == null || TextUtils.isEmpty(device.address))
			return;

		this.mService.connect(device.address);
	}

	// -----------------------------------------------//
	// 接收到底层发过来的数据，或者读取到的数据；
	// ------------------------------------------------//
	private void onReceiveData(DataPackage repo) {
		if (repo == null)
			return;

		if (!repo.isCheckedOk()) {
			Toast.makeText(mContext, "数据校验错误", Toast.LENGTH_LONG).show();
			return;
		}
		//
		if (this.mOnDataChangedListener == null)
			return;

		if (repo.isUpload()) {
			if (repo.isGetSpeed()) {
				// 更新速度信息；
				this.mOnDataChangedListener.onSpeedChanged(repo.getSpeed());
			} else if (repo.isGetPos()) {
				// 更新位置信息；
				this.mOnDataChangedListener.onPosChanged(repo.getPos(), repo.getAxis());
			}
		}
	}
	
	private boolean isDeviceMoving;
	/**
	 * 当设备移动的过程中，需要定时更新当前位置；
	 */
	private void updatePositionInfo(final int axis, final long delay) {
		mHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				if (isDeviceMoving) {
					// 执行命令，获取当前位置信息
					executeGetPosition(axis);
					updatePositionInfo(axis, delay);
				}
			}
		}, delay);	// 一定时间间隔后刷新一次位置信息;
	}

	// ------------------------------------------------//
	// --------------- 执行操作命令 -----------------------//
	// ------------------------------------------------//

	/**
	 * 此命令可以为机械手提供机械零点，在无限位模式下控制各关节到达机械零位，下发该命令即可将当前位置设置为零点。
	 * 下位机收到该命令后需要按照应答格式返回命令，手机 2 秒内没有收到应答认为命令丢包，连续 3 次丢包提示通讯中断。
	 * 
	 * @return
	 */
	public boolean executeSetZeroPoint() {
		byte high = 0x54;
		byte low = 0x64;
		DataPackage data = DataPackage.getDataOfSetZeroCmd();
		data.setData(high, low);

		return this.executeCmd(data);
	}

	/**
	 * 此命令可以设置机械手各关节运行速度，0~100 百分比形式。 下位机收到该命令后需要按照应答格式返回命令，手机 2 秒内没有收到应答认为命令丢包，连续 3
	 * 次丢包提示通讯中断。
	 * 
	 * @param speed
	 * @return
	 */
	public boolean executeSetSpeed(int speed) {
		DataPackage data = DataPackage.getDataOfSetSpeed();
		data.setData(DataPackage.ZERO, (byte) speed);

		if (!this.executeCmd(data))
			return false;
		
		// 修改运行速度后，需要同步的获取当前速度
		executeGetSpeed();
		return true;
	}

	/**
	 * 此命令可以设置各个轴的反方向运动停止点，单位 0.1°，有符号整型数据。 下位机收到该命令后需要按照应答格式返回命令，手机 2
	 * 秒内没有收到应答认为命令丢包，连续 3 次丢包提示通讯中断。
	 * 
	 * @param minValue
	 * @return
	 */
	public boolean executeSetMinPosition(int axis, float minValue) {
		int iValue = (int) (minValue * 10);
		byte high = (byte) (iValue >> 8 & 0xFF);
		byte low = (byte) (iValue & 0xFF);
		DataPackage data = DataPackage.getDataOfSetMinLimit();
		data.setAxis((byte) axis);
		data.setData(high, low);

		return this.executeCmd(data);
	}

	/**
	 * 此命令可以设置各个轴的正方向运动停止点，单位 0.1°，有符号整型数据。 下位机收到该命令后需要按照应答格式返回命令，手机 2
	 * 秒内没有收到应答认为命令丢包，连续 3 次丢包提示通讯中断。
	 * 
	 * @param maxValue
	 * @return
	 */
	public boolean executeSetMaxPosition(int axis, float maxValue) {
		int iValue = (int) (maxValue * 10);
		byte high = (byte) (iValue >> 8 & 0xFF);
		byte low = (byte) (iValue & 0xFF);
		DataPackage data = DataPackage.getDataOfSetMinLimit();
		data.setAxis((byte) axis);
		data.setData(high, low);

		return this.executeCmd(data);
	}

	/**
	 * 该命令可以控制机械手各个轴开始运动，数据域表示运动方向。 下位机收到该命令后需要按照应答格式返回命令，手机 2 秒内没有收到应答认为命令丢包，连续 3
	 * 次丢包提示通讯中断。
	 * 
	 * @param axis
	 * @param forward
	 * @return
	 */
	public boolean executeStartMove(int axis, boolean reverse) {
		byte high = 0x00;
		byte low = reverse ? DataPackage.MOVING_REVERSE : DataPackage.MOVING_DIRECT;
		DataPackage data = DataPackage.getDataOfStartMove();
		data.setAxis((byte) axis);
		data.setData(high, low);

		if (!this.executeCmd(data))
			return false;
		
		// 500毫秒后，刷新位置信息；
		isDeviceMoving = true;
		updatePositionInfo(axis, 500);
		return true;
	}

	/**
	 * 该命令可以控制机械手各个轴停止运动。 下位机收到该命令后需要按照应答格式返回命令，手机 2 秒内没有收到应答认为命令丢包，连续 3 次丢包提示通讯中断。
	 * 
	 * @return
	 */
	public boolean executeStopMove(int axis) {
		isDeviceMoving = false;
		DataPackage data = DataPackage.getDataOfStopMove();
		data.setAxis((byte) axis);

		if (!this.executeCmd(data))
			return false;
		
		// 停止前事实刷新下当前速度
		executeGetPosition(axis);
		return true;
	}

	/**
	 * 该命令可以控制机械手按照预设好的轨迹运行。 下位机收到该命令后需要按照应答格式返回命令，手机 2 秒内没有收到应答认为命令丢包，连续 3
	 * 次丢包提示通讯中断。
	 * 
	 * @param traceType
	 * @return
	 */
	public boolean executeTraceRunning(int traceType) {
		byte low = (byte) traceType;
		DataPackage data = DataPackage.getDataOfRunningTrace();
		data.setData(DataPackage.ZERO, low);

		return this.executeCmd(data);
	}

	/**
	 * 该命令可以设置机械手当前运行模式：无限位模式、标准模式。 下位机收到该命令后需要按照应答格式返回命令，手机 2 秒内没有收到应答认为命令丢包，连续 3
	 * 次丢包提示通讯中断。
	 * 
	 * @param isNormalMode
	 * @return
	 */
	public boolean executeModeSwitch(boolean isNormalMode) {
		byte low = (byte) (isNormalMode ? 0x02 : 0x01);
		DataPackage data = DataPackage.getDataOfSwitchMode();
		data.setData(DataPackage.ZERO, low);

		return this.executeCmd(data);
	}

	/**
	 * 该命令由设备主动上传，上位机收到命令后解析并做相应的数据展示，无需应答。
	 * 
	 * @return
	 */
	public boolean executeGetSpeed() {
		DataPackage data = DataPackage.getDataOfGetSpeed();

		return this.executeCmd(data);
	}

	/**
	 * 该命令由设备主动上传，上位机收到命令后解析并做相应的数据展示，无需应答。
	 * 
	 * @param axis
	 * @return
	 */
	public boolean executeGetPosition(int axis) {
		DataPackage data = DataPackage.getDataOfGetPos();
		data.setAxis((byte)axis);

		return this.executeCmd(data);
	}

	private boolean executeCmd(DataPackage data) {
		// 1、发送消息；
		// 2、n*100毫秒后，查看相应信息（notifycation）
		// 3、如果接收到相应，并且验证成功，则表示一条消息处理成功，开始处理吓一跳消息；
		// 4、如果仍为接收到设备反馈的消息，则可能出现丢包现象，重新发送？？？
		if (data == null || this.mCharacteristic == null || this.mService == null)
			return false;
		int property = this.mCharacteristic.getProperties();
		if ((property & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
				&& (property & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
			Toast.makeText(mContext, "当前特征值不支持数据发送操作", Toast.LENGTH_LONG).show();
			return false;
		}

		if (this.mCmdQueue == null)
			this.mCmdQueue = new LinkedBlockingDeque<Worker>();

		this.mCmdQueue.add(new Worker(data, mCharacteristic));
		this.processNextCmd();
		return true;
	}

	/**
	 * 处理吓一跳命令；
	 */
	private void processNextCmd() {
		if (this.mcurrWorker == null) {
			mcurrWorker = this.mCmdQueue.poll();
			if (mcurrWorker != null) {
				sendData(mcurrWorker);
				return;
			}
		}
		// 一段时间后在处理吓一跳命令；
		processNextCmdDelay();
	}

	private void processNextCmdDelay() {
		this.mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				processNextCmd();
			}
		}, 200);
	}

	private void sendData(final Worker worker) {
		if (worker == null)
			return;

		worker.setTimeoutListener(new Worker.OnTimeOutListener() {

			@Override
			public void onTimeOut() {
				Log.w(TAG, String.format("命令%s响应超时！", worker.getPackage().getKey()));
				if (mSendCount >= 2) {
					mSendCount = 0;
					Toast.makeText(mContext, "消息响应超时", Toast.LENGTH_LONG).show();
					mcurrWorker = null;
				} else {
					mSendCount++;
					sendData(mcurrWorker);
				}
			}
		});
		worker.setWriteListener(new OnWriteListener() {

			@Override
			public void onWriteSuccess(BluetoothGattCharacteristic characteristic) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onResponse(BluetoothGattCharacteristic characteristic) {
				mcurrWorker.onResponse();
				DataPackage repo = DataPackage.create(characteristic.getValue());
				if (!repo.isUpload()) {
					if (repo.isReponseNormal()) {
						// 验证成功
						mcurrWorker = null;
						// 执行下一条命令
						processNextCmd();
					} else {
						// 验证失败
						Log.w(TAG, String.format("命令%s响应校验失败！", worker.getPackage().getKey()));
						if (mSendCount >= 2) {
							mSendCount = 0;
							Toast.makeText(mContext, "命令响应验证失败，连接可能已中断", Toast.LENGTH_LONG).show();
							mcurrWorker = null;
						} else {
							mSendCount++;
							sendData(mcurrWorker);
						}
					}
				} else {
					onReceiveData(repo);
					mcurrWorker = null;
					// 执行下一条命令
					processNextCmd();
				}
			}

			@Override
			public void onFailure(int code) {
				Log.i(TAG, "数据写入失败！");
			}
		});
		if (!worker.sendData(this.mService)) {
			Toast.makeText(mContext, "数据发送失败！", Toast.LENGTH_LONG).show();
		}
	}
}
