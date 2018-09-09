package com.gemhu.blemaster;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

@SuppressLint("InflateParams")
public class MainActivity extends Activity implements OnClickListener{

	private DeviceAdapter mDeviceAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// 
		this.initView();
		// 读取上次连接的蓝牙设备，如果有则自动连接；
		String device = AppConfig.getLastDevice();
		// 连接设备
	}
	
	private void initView() {
		this.findViewById(R.id.ble_group).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ble_group:
			showDeviceList();
			break;

		default:
			break;
		}
		
	}
	
	
	
	private void showDeviceList() {
		View view = LayoutInflater.from(this).inflate(R.layout.device_list, null);
		ListView list = (ListView) view.findViewById(R.id.device_list_lv);
		this.mDeviceAdapter = new DeviceAdapter(this);
		list.setAdapter(this.mDeviceAdapter);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setIcon(R.drawable.launcher);
		builder.setTitle("扫描蓝牙设备");
		builder.setView(view);
		builder.setCancelable(false);
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mDeviceAdapter = null;
				dialog.dismiss();
			}
		});
		builder.setPositiveButton("连接", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				
				dialog.dismiss();
			}
		});
		list.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				DeviceItem item = (DeviceItem) mDeviceAdapter.getItem(position);
				Toast.makeText(MainActivity.this, item.name, Toast.LENGTH_LONG).show();
			}
		});
		builder.show();
	}
}
