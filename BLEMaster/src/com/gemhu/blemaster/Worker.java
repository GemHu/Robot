package com.gemhu.blemaster;

import android.os.Handler;
import android.os.Looper;

public class Worker {

	public Worker(DataPackage pkg) {
		this.pkg = pkg;
	}
	
	private DataPackage pkg;
	private OnWriteListener listener;
	private Handler handler = new Handler(Looper.getMainLooper());
	private boolean hasResponse = false;
	
	public DataPackage getDataPackage() {
		return this.pkg;
	}
	
	public OnWriteListener getWriteListener() {
		return this.listener;
	}
	
	public void setWriteListener(OnWriteListener listener) {
		this.listener = listener;
	}
	
	public void setTimeoutListener(final OnTimeOutListener listener, long dely) {
		if (listener != null && !hasResponse) {
			handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					listener.onTimeOut();
				}
			}, dely);
		}
	}
	
	interface OnTimeOutListener{
		void onTimeOut();
	}
	
	public void onResponse() {
		this.hasResponse = true;
	}
}
