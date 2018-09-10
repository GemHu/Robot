package com.gemhu.blemaster;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * 用于保存操作过程中的一些临时数据，方便下次打开程序的时候进行自动化处理；
 * @author Administrator
 *
 */
public class AppConfig {

	public static void saveDeviceInfo(Context context, DeviceInfo info) {
		if (info == null)
			return;
		
	    SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);  
	    info.save(sharedPreferences);
	}

	public static DeviceInfo loadDeviceInfo(Context context) {
		 SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
		 DeviceInfo device = new DeviceInfo(sharedPreferences);
		 if (TextUtils.isEmpty(device.address))
			 return null;
		 
		 return device;
	}
}
