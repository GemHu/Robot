package com.gemhu.blemaster;

import java.util.ArrayList;
import java.util.List;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

@SuppressLint("InflateParams")
public class DeviceAdapter extends BaseAdapter {

	private Activity mActivity;
	private List<DeviceItem> dataList = new ArrayList<DeviceItem>();
	
	public DeviceAdapter(Activity activity) {
		this.mActivity = activity;
	}

	public void addDevice(DeviceItem item) {
		DeviceItem oldItem = this.getItemByAddress(item.address);
		if (oldItem != null) {
			oldItem.rssi = item.rssi;
		} else {
			this.dataList.add(item);
		}

		this.notifyDataSetChanged();
	}

	public void clear() {
		this.dataList.clear();
	}

	public DeviceItem getItemByAddress(String address) {
		for (DeviceItem item : this.dataList) {
			if (item.address.equalsIgnoreCase(address))
				return item;
		}

		return null;
	}

	@Override
	public int getCount() {
		return this.dataList.size();
	}

	@Override
	public Object getItem(int position) {
		return this.dataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// GetUI
		ViewHolder holder = null;
		if (convertView == null) {
			convertView = LayoutInflater.from(this.mActivity).inflate(R.layout.item_device, null);
			holder = new ViewHolder();
			convertView.setTag(holder);

			holder.tvDeviceName = (TextView) convertView.findViewById(R.id.item_device_name);
			holder.tvUUID = (TextView) convertView.findViewById(R.id.item_device_uuid);
			holder.tvAddress = (TextView) convertView.findViewById(R.id.item_device_mac);
			holder.tvRSSI = (TextView) convertView.findViewById(R.id.item_device_rssi);
			holder.tvDeviceType = (TextView) convertView.findViewById(R.id.item_device_type);
			holder.tvBondState = (TextView) convertView.findViewById(R.id.item_device_bondState);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// SetData
		DeviceItem data = this.dataList.get(position);
		holder.tvDeviceName.setText(data.name);
		holder.tvAddress.setText(data.address);
		holder.tvUUID.setText(data.supportUUID);
		holder.tvRSSI.setText(String.format("%s", data.rssi));
		holder.tvDeviceType.setText(String.format("%s", data.deviceType));
		holder.tvBondState.setText(String.format("%s", data.bondState));

		return convertView;
	}

	class ViewHolder {
		TextView tvUUID;
		TextView tvDeviceName;
		TextView tvAddress;
		TextView tvRSSI;
		TextView tvDeviceType;
		TextView tvBondState;
	}
}
