package com.gemhu.blemaster;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

/**
 * 用于保存操作过程中的一些临时数据，方便下次打开程序的时候进行自动化处理；
 * @author Administrator
 *
 */
public class AppConfig {
	private final static String TAG_RUNNING_MODE = "runningMode";

	public static void saveDeviceInfo(Context context, DeviceInfo info) {
		if (info == null)
			return;
		
	    SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);  
	    info.save(sharedPreferences.edit());
	}

	public static DeviceInfo loadDeviceInfo(Context context) {
		 SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
		 DeviceInfo device = new DeviceInfo(sharedPreferences);
		 if (TextUtils.isEmpty(device.address))
			 return null;
		 
		 return device;
	}

	public static int getRunningMode(Context context, int defMode) {
	    SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE); 
		return sharedPreferences.getInt(TAG_RUNNING_MODE, defMode);
	}
	
	public static boolean setRunningMode(Context context, int mode) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		editor.putInt(TAG_RUNNING_MODE, mode);
		return editor.commit();
	}
}
