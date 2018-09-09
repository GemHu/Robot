package com.gemhu.blemaster;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.text.TextUtils;

public class DeviceItem {
	public DeviceItem(BluetoothDevice device, int rssi) {
		this.rssi = rssi;

		this.name = device.getName();
		this.address = device.getAddress();
		this.deviceType = device.getType();
		this.bondState = device.getBondState();

		ParcelUuid[] uuids = device.getUuids();
		if (uuids != null && uuids.length > 0) {
			for (ParcelUuid parcelUuid : uuids) {
				this.supportUUID += (parcelUuid + "\n");
			}
			this.supportUUID = this.supportUUID.substring(0, this.supportUUID.length() - 1);
		}

		if (TextUtils.isEmpty(this.name))
			this.name = "UnKnown Device";
	}

	public String supportUUID;
	public String name;
	public String address;
	public int rssi;
	public int deviceType;
	public int bondState;
}
