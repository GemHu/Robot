package com.gemhu.blemaster;

import android.bluetooth.BluetoothGattCharacteristic;

public interface OnWriteListener {

	/**
	 * 消息发送成功；
	 * @param characteristic
	 */
	void onWriteSuccess(BluetoothGattCharacteristic characteristic);
	/**
	 * 消息相应成功；
	 * 通过NotifyCation获取 到的消息；
	 * @param characteristic
	 */
	void onResponse(BluetoothGattCharacteristic characteristic);
	/**
	 * 消息发送失败；
	 * @param e
	 */
	void onFailure(int code);
}
