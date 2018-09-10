package com.gemhu.blemaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.widget.SimpleAdapter;
import android.widget.Toast;

@SuppressLint("NewApi")
public class DeviceDialog {
	public final static int REQUEST_CODE_PERMISSION_LOCATION = 999;

	private Activity mActivity;
	private BluetoothAdapter mBTAdapter;

	private Handler mHandler;

	private SimpleAdapter mListAdapter;
	private List<Map<String, Object>> mDataList = new ArrayList<Map<String, Object>>();
	private Map<String, Map<String, Object>> mDeviceMap = new HashMap<String, Map<String,Object>>(); 
	private OnClickListener mOnClickListener;

	public DeviceDialog(Activity activity) {
		this.mActivity = activity;
		//
		this.initView();
		//
		this.initBLE();
	}

	private void initView() {
		String[] from = {
						DeviceInfo.TAG_DEVICE_NAME,
						DeviceInfo.TAG_DEVICE_ADDRESS,
						DeviceInfo.TAG_DEVICE_RSSI,
						DeviceInfo.TAG_DEVICE_TYPE,
						DeviceInfo.TAG_DEVICE_STATE
		};
		int to[] = {
						R.id.item_device_name,
						R.id.item_device_mac,
						R.id.item_device_rssi,
						R.id.item_device_type,
						R.id.item_device_bondState
		};
		this.mListAdapter = new SimpleAdapter(this.mActivity, this.mDataList, R.layout.item_device, from, to);
	}
	
	public void addDevice(BluetoothDevice device, int rssi) {
		if (device == null)
			return;
		
		DeviceInfo info = new DeviceInfo(device.getName(), device.getAddress());
		info.rssi = rssi;
		info.type = device.getType();
		info.bondState = device.getBondState();

		Map<String, Object> map = null;
		if (this.mDeviceMap.containsKey(info.address)) {
			map = this.mDeviceMap.get(info.address);
		} else {
			map = info.getMap();
			this.mDataList.add(map);
			this.mDeviceMap.put(info.address, map);
		}
		this.mListAdapter.notifyDataSetChanged();
	}

	public void initBLE() {
		mHandler = new Handler();
		// 请求权限
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			try {
				if (this.mActivity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					this.mActivity.requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_CODE_PERMISSION_LOCATION);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
		if (!this.mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this.mActivity, "当前手机不支持ble 蓝牙!", Toast.LENGTH_SHORT).show();
			return;
		}

		// 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
		BluetoothManager bluetoothManager = (BluetoothManager) this.mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
		this.mBTAdapter = bluetoothManager.getAdapter();

		// 检查设备上是否支持蓝牙
		if (this.mBTAdapter == null) {
			Toast.makeText(this.mActivity, "当前手机不支持ble 蓝牙!", Toast.LENGTH_LONG).show();
			return;
		}
	}

	private void scanBle(boolean start) {
		this.scanBle(start, new ScanCallback() {
			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				super.onScanResult(callbackType, result);
				DeviceDialog.this.addDevice(result.getDevice(), result.getRssi());
			}
		});
	}

	private void scanBle(boolean start, final ScanCallback callback) {
		final BluetoothLeScanner scanner = this.mBTAdapter.getBluetoothLeScanner();
		if (start) {
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					scanner.stopScan(callback);
				}
			}, 30 * 1000);
			scanner.startScan(callback);
		} else {
			scanner.stopScan(callback);
		}
	}

	public void scanDevice(OnClickListener listener) {
		this.mOnClickListener = listener;
		this.mDataList.clear();
		this.mDeviceMap.clear();
		// 1、弹出对话框；
		this.showDialog();
		// 2、开始三扫描BLE设备；
		this.scanBle(true);
	}

	private void showDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.mActivity, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
		builder.setTitle("扫描设备");
		builder.setAdapter(this.mListAdapter, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 停止扫面；
				scanBle(false);
				// 关闭对话框；
				dialog.dismiss();
				// 回调接口
				if (mOnClickListener != null)
					mOnClickListener.onClick(mDataList.get(which));
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				// 停止扫描
				scanBle(false);
			}
		});
		AlertDialog dialog = builder.show();
		// 设置对话框大小
		DisplayMetrics dm = new DisplayMetrics();
        this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        int screenHeigh = dm.heightPixels;
		dialog.getWindow().setLayout((int)(screenWidth * 0.85), (int)(screenHeigh * 0.85));
	}
	
	interface OnClickListener{
		
		void onClick(Map<String, Object> data);
	}
}
