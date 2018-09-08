package com.hu.robot;

import java.util.ArrayList;
import java.util.List;

import com.dothantech.bletestdemo.R;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final int REQUEST_ENABLE_BT = 1;

	private ListView mLvDevice;
	private DeviceAdapter mAdapter;
	private Handler mHandler;
	private BluetoothAdapter mBluetoothAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.mLvDevice = (ListView) this.findViewById(R.id.lv_devices);
		this.mAdapter = new DeviceAdapter();
		this.mLvDevice.setAdapter(this.mAdapter);
		this.mLvDevice.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final DeviceItem device = (DeviceItem) mAdapter.getItem(position);
				if (device == null)
					return;
				final Intent intent = new Intent(MainActivity.this, ServiceListActivity.class);
				intent.putExtra(ServiceListActivity.EXTRAS_DEVICE_NAME, device.name);
				intent.putExtra(ServiceListActivity.EXTRAS_DEVICE_ADDRESS, device.address);
				if (mScanning) {
					scanLeDevice(false);
				}
				startActivity(intent);
			}
		});

		this.initBLE();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (this.mBluetoothAdapter == null)
			return;

		// 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		scanLeDevice(true);
	}

	private void initBLE() {
		mHandler = new Handler();
		// 请求权限
		// 请求蓝牙位置权限
		if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 998);
		}

		// 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "当前手机不支持ble 蓝牙!", Toast.LENGTH_SHORT).show();
			return;
		}

		// 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// 检查设备上是否支持蓝牙
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "当前手机不支持ble 蓝牙!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		BluetoothLeAdvertiser bluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (bluetoothLeAdvertiser == null) {
			Toast.makeText(this, "该设备不支持蓝牙低功耗从设备通讯", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (!mScanning) {
			menu.findItem(R.id.menu_stop).setVisible(false);
			menu.findItem(R.id.menu_scan).setVisible(true);
			menu.findItem(R.id.menu_refresh).setActionView(null);
		} else {
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_scan).setVisible(false);
			menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_scan:
			mAdapter.clear();
			scanLeDevice(true);
			break;
		case R.id.menu_stop:
			scanLeDevice(false);
			break;
		}

		return true;
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
		case 998:
			Toast.makeText(this, "位置权限请求成功", Toast.LENGTH_LONG).show();
			break;

		default:
			break;
		}
	}

	private boolean mScanning;

	/**
	 * 开始或者停止搜索ble设备；
	 * @param enable true表示开始搜索，false表示停止搜索；
	 */
	private void scanLeDevice(final boolean enable) {
		final BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					scanner.stopScan(mScanCallback);
					invalidateOptionsMenu();
				}
			}, 10 * 1000);

			mScanning = true;
			scanner.startScan(mScanCallback);
		} else {
			mScanning = false;
			scanner.stopScan(mScanCallback);
		}
		invalidateOptionsMenu();
	}

	private ScanCallback mScanCallback = new ScanCallback() {
		public void onScanResult(int callbackType, final ScanResult result) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mAdapter.addDevice(new DeviceItem(result.getDevice(), result.getRssi()));
				}
			});
		};
	};

	@SuppressLint("InflateParams")
	class DeviceAdapter extends BaseAdapter {

		private List<DeviceItem> dataList = new ArrayList<DeviceItem>();

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
				convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_device, null);
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

	class DeviceItem {

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
}
