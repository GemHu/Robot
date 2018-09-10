package com.gemhu.blemaster;

import java.util.HashMap;
import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class DeviceInfo {
	public final static String TAG_DEVICE_NAME = "name";		// 设备名称
	public final static String TAG_DEVICE_ADDRESS = "address";	// mac地址
	public final static String TAG_DEVICE_RSSI = "rssi";		// 信号强度
	public final static String TAG_DEVICE_TYPE = "type";		// 设别类型
	public final static String TAG_DEVICE_STATE = "state";		// 绑定状态

	public String name;
	public String address;
	public int rssi;
	public int type;
	public int bondState;

	public DeviceInfo() {
		
	}
	
	public DeviceInfo(String name, String address) {
		this.name = name;
		this.address = address;
	}
	
	public static DeviceInfo getDeviceInfo(Map<String, Object> map) {
		if (map == null)
			return null;
		
		return new DeviceInfo(map);
	}
	
	public DeviceInfo(Map<String, Object> map) {
		this.name = (String) map.get(TAG_DEVICE_NAME);
		this.address = (String) map.get(TAG_DEVICE_ADDRESS);
		this.rssi = (Integer) map.get(TAG_DEVICE_RSSI);
		this.type = (Integer) map.get(TAG_DEVICE_TYPE);
		this.bondState = (Integer) map.get(TAG_DEVICE_STATE);
	}
	
	public DeviceInfo(SharedPreferences sp) {
		this.name = sp.getString(TAG_DEVICE_NAME, "");
		this.address = sp.getString(TAG_DEVICE_ADDRESS, "");
		this.rssi = sp.getInt(TAG_DEVICE_RSSI, 0);
		this.type = sp.getInt(TAG_DEVICE_TYPE, 0);
		this.bondState = sp.getInt(TAG_DEVICE_STATE, 0);
	}
	
	public Map<String, Object> getMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(TAG_DEVICE_NAME, this.name);
		map.put(TAG_DEVICE_ADDRESS, this.address);
		map.put(TAG_DEVICE_RSSI, this.rssi);
		map.put(TAG_DEVICE_TYPE, this.type);
		map.put(TAG_DEVICE_STATE, this.bondState);
		
		return map;
	}
	
	public void save(SharedPreferences sp) {
		Editor editor = sp.edit();  
		editor.putString(TAG_DEVICE_NAME, this.name);
	    editor.putString(TAG_DEVICE_ADDRESS, this.address);  
	    editor.putInt(TAG_DEVICE_RSSI, this.rssi);
	    editor.putInt(TAG_DEVICE_TYPE, this.type);
	    editor.putInt(TAG_DEVICE_STATE, this.bondState);
	    editor.commit();  
	}
}
