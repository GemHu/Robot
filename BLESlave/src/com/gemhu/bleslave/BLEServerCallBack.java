package com.gemhu.bleslave;

import java.util.Arrays;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

public class BLEServerCallBack extends BluetoothGattServerCallback {

	private static final String TAG = BLEServerCallBack.class.getSimpleName();
	private Context mContext;
	private BluetoothGattServer mGattServer;
	private BluetoothGattCharacteristic mCharacteristicRead;
	public ILog mLog;
	int mIndex = 0;

	public void setupServices(Context context, BluetoothGattServer gattServer) {
		this.mContext = context;
		this.mGattServer = gattServer;

		BluetoothGattService service = new BluetoothGattService(BluetoothUUID.BLE_SERVER,
				BluetoothGattService.SERVICE_TYPE_PRIMARY);

		// add a read characteristic.
		// 当是ios设备连接过来时，需添加BluetoothGattCharacteristic.PROPERTY_INDICATE或者notify进行兼容。
		mCharacteristicRead = new BluetoothGattCharacteristic(BluetoothUUID.BLE_READ,
				BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_INDICATE,
				BluetoothGattCharacteristic.PERMISSION_READ);
		// add a descriptor
		BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(BluetoothUUID.BLE_CONFIG,
				BluetoothGattCharacteristic.PERMISSION_WRITE);
		mCharacteristicRead.addDescriptor(descriptor);
		service.addCharacteristic(mCharacteristicRead);

		BluetoothGattCharacteristic write = new BluetoothGattCharacteristic(
				BluetoothUUID.BLE_WRITE, BluetoothGattCharacteristic.PROPERTY_WRITE
						| BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_INDICATE,
				BluetoothGattCharacteristic.PERMISSION_WRITE);

		// 一个服务可以添加多个特征值
		service.addCharacteristic(write);

		// 添加第一个服务；
		mGattServer.addService(service);

		// 添加第二个服务
		BluetoothGattService service2 = new BluetoothGattService(BluetoothUUID.BLE_SERVER2,
				BluetoothGattService.SERVICE_TYPE_PRIMARY);
		mGattServer.addService(service2);
	}

	// 当添加一个GattService成功后会回调改接口。
	public void onServiceAdded(int status, BluetoothGattService service) {
		super.onServiceAdded(status, service);
		if (status == BluetoothGatt.GATT_SUCCESS) {
			this.mLog.write("服务添加成功", service.getUuid().toString());
			Log.d(TAG, "onServiceAdded status=GATT_SUCCESS service=" + service.getUuid().toString());
		} else {
			this.mLog.write("服务添加失败", service == null ? "" : service.getUuid().toString());
			Log.d(TAG, "onServiceAdded status!=GATT_SUCCESS");
		}
	}

	// BLE设备连接状态发生改变后回调的接口
	public void onConnectionStateChange(android.bluetooth.BluetoothDevice device, int status, int newState) {
		super.onConnectionStateChange(device, status, newState);
		Log.e(TAG, String.format("1.onConnectionStateChange：device name = %s, address = %s", device.getName(),
				device.getAddress()));
		Log.d(TAG, "onConnectionStateChange status=" + status + "->" + newState);
		if (newState == BluetoothProfile.STATE_DISCONNECTED) {
			// btClient = null; // 移除客户端连接设备
			this.mLog.write("设备连接已断开", "");
		} else if (newState == BluetoothProfile.STATE_CONNECTED) {
			this.mLog.write("设备连接成功", "");
			this.mLog.write("device.name", device.getName());
			this.mLog.write("device.address", device.getAddress());
		} else if (newState == BluetoothProfile.STATE_CONNECTING) {
			this.mLog.write("正在连接设备", "");
		}
	}

	// 当有客户端来读数据时回调的接口
	/**
	 * 特征被读取。当回复响应成功后，客户端会读取然后触发本方法,
	 */
	public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device, int requestId, int offset,
			BluetoothGattCharacteristic characteristic) {
		super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
		characteristic.setValue(new byte[] { 0x03, 0x01 });

		this.mLog.write("读取特征值请求", "device.name = " + device.getName() + "; device.address = " + device.getAddress());
		Log.e(TAG, String.format("1.onCharacteristicReadRequest：device name = %s, address = %s", device.getName(),
				device.getAddress()));
		Log.e(TAG, String.format("onCharacteristicReadRequest：requestId = %s, offset = %s", requestId, offset));
		boolean result = mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
				characteristic.getValue());

		this.mLog.write(result ? "特征值读取成功" : "特征值读取失败", "");
		Log.e(TAG, "read request send response:" + result);
	}

	// 当有客户端来写数据时回调的接口
	/**
	 * 接受具体数据字节
	 */
	@Override
	public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device, int requestId,
			BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset,
			byte[] value) {
		super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset,
				value);
		// 需调用 sendResponse 来响应，为了保持连接。
		this.mLog.writeReceiveMsg(new String(value));
		this.mIndex++;
		this.mLog.writeSendMsg(this.mIndex + "");
		byte[] repValue = (this.mIndex + "").getBytes();
		mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, repValue);

		// 处理其它设备写进来的数据
		// value. // 处理数据 byte[] value，记住连接设备

	}

	// 当有客户端来写Descriptor 时回调的接口
	/**
	 * 描述被写入时，在这里执行bluetoothGattServer.sendResponse(device, requestId,
	 * BluetoothGatt.GATT_SUCCESS...) 时 触发onCharacteristicWriteRequest
	 **/
	@Override
	public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
			boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
		super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

		Log.d(TAG, "onDescriptorWriteRequest:" + Arrays.toString(value));
		this.mLog.writeReceiveMsg("接收到Descripter请求：" + new String(value));
		// now tell the connected device that this was all successfull
		mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
	}

}
