package com.gemhu.blemaster;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class DeviceActivity extends Activity {

//	private final static String TAG = BLEManager.class.getSimpleName();
	public final static int REQUEST_CODE_PERMISSION_LOCATION = 999;

	private Activity mContext;
	private BluetoothAdapter mBTAdapter;
	private Handler mHandler;

	private ListView mDeviceList;
	private DeviceAdapter mDeviceAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.device_list);

		// 初始化蓝牙适配器
		this.initBLE();
		
		this.mDeviceList = (ListView) this.findViewById(R.id.device_list_lv);
		this.mDeviceAdapter = new DeviceAdapter(this);
		this.mDeviceList.setAdapter(this.mDeviceAdapter);
		this.mDeviceList.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				MyApp.theDevice = (DeviceItem) mDeviceAdapter.getItem(position);
				DeviceActivity.this.finish();
			}
		});
	}

	public void initBLE() {
		mHandler = new Handler();
		// 请求权限
		requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_CODE_PERMISSION_LOCATION, Build.VERSION_CODES.M);

		// 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
		if (!this.mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this.mContext, "当前手机不支持ble 蓝牙!", Toast.LENGTH_SHORT).show();
			return;
		}

		// 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
		this.mBTAdapter = getBluetoothManager().getAdapter();

		// 检查设备上是否支持蓝牙
		if (this.mBTAdapter == null) {
			Toast.makeText(this, "当前手机不支持ble 蓝牙!", Toast.LENGTH_LONG).show();
			return;
		}
	}

	private void requestPermission(String permission, int requestCode, int version) {
		if (version > 0 && Build.VERSION.SDK_INT >= version) {
			try {
				if (this.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
					this.requestPermissions(new String[] { permission }, requestCode);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private BluetoothManager getBluetoothManager() {
		return (BluetoothManager) this.mContext.getSystemService(Context.BLUETOOTH_SERVICE);
	}
	
	public void scanBleDevice(boolean stop) {
		this.scanBleDevice(stop, new ScanCallback() {
			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				super.onScanResult(callbackType, result);
				mDeviceAdapter.addDevice(new DeviceItem(result.getDevice(), result.getRssi()));
			}
		});
	}
	
	public void scanBleDevice(boolean stop, final ScanCallback callback) {
		final BluetoothLeScanner scanner = this.mBTAdapter.getBluetoothLeScanner();
		if (stop) {
			scanner.stopScan(callback);
		} else {
			mHandler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					scanner.stopScan(callback);
				}
			}, 30 * 1000);
			scanner.startScan(callback);
		}
	}
}
