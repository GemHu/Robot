package com.gemhu.blemaster;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class Worker {
	private static final String TAG = Worker.class.getSimpleName();

	public Worker(DataPackage pkg, BluetoothGattCharacteristic characteristic) {
		this.pkg = pkg;
		this.characteristic = characteristic;
	}
	
	private DataPackage pkg;
	BluetoothGattCharacteristic characteristic;
	private OnWriteListener listener;
	private OnTimeOutListener  timeOutListener;
	private long delay = 2 * 1000;
	private Handler handler = new Handler(Looper.getMainLooper());
	private boolean noResponse = true;
	
	public DataPackage getPackage() {
		return this.pkg;
	}
	
	public OnWriteListener getWriteListener() {
		return this.listener;
	}
	
	public void setWriteListener(OnWriteListener listener) {
		this.listener = listener;
	}
	
	public void setTimeoutListener(OnTimeOutListener listener) {
		this.timeOutListener = listener;
	}
	
	interface OnTimeOutListener{
		void onTimeOut();
	}
	
	public void onResponse() {
		this.noResponse = false;
	}
	
	public boolean sendData(BLEService service) {
		// 写入数据
		pkg.setCharecteristic(characteristic);
		Log.i(TAG, "Start to write data : " + pkg.getKey());
		boolean ret = service.writeCharacteristic(characteristic);
		if (ret) {
			handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					if (timeOutListener != null && noResponse)
						timeOutListener.onTimeOut();
				}
			}, delay);
		}
		
		return ret;
	}
}
