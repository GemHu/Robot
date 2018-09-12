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
	// 运动模式
	public final static int RUNNING_MODE_NO_LIMIT = 0;
	public final static int RUNNING_MODE_STANDARD = 1;

	private static DeviceInfo sDeviceInfo;
	private static int sCurrRunningMode = -1;
	public static void setDeviceInfo(Context context, DeviceInfo info) {
		if (info == null)
			return;
		
		sDeviceInfo = info;
	    SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);  
	    info.save(sharedPreferences.edit());
	}

	public static DeviceInfo getDeviceInfo(Context context) {
		if (sDeviceInfo == null) {
			 SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
			 DeviceInfo device = new DeviceInfo(sharedPreferences);
			 if (!TextUtils.isEmpty(device.address))
				 sDeviceInfo = device;
		}
		 
		 return sDeviceInfo;
	}

	public static int getRunningMode(Context context) {
		if (sCurrRunningMode < 0) {
		    SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE); 
			sCurrRunningMode = sharedPreferences.getInt(TAG_RUNNING_MODE, RUNNING_MODE_NO_LIMIT);
		}
		
		return sCurrRunningMode;
	}
	
	public static boolean setRunningMode(Context context, int mode) {
		sCurrRunningMode = mode;
		SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		editor.putInt(TAG_RUNNING_MODE, mode);
		return editor.commit();
	}
}
