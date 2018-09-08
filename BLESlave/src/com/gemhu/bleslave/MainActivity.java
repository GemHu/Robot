package com.gemhu.bleslave;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends Activity implements View.OnClickListener, ILog {

	private boolean mParing;
	private EditText mSendEdit;
	private BLEMsgReceive mBleReceive;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//
		this.mSendEdit = (EditText) findViewById(R.id.edit_send_msg);
		// init listview
		this.initReceiveList();
		
		// 过程：
		// 1、 init ble
		this.mBleReceive = new BLEMsgReceive(this);
		this.mBleReceive.log = this;
		// 2、程序启动后，开始发送广播；
		this.bleAdvertising(true);
		// 2、当有请求连接的时候建立连接；
		// 3、连接成功后启动服务；？？？？？？？？？
		// 4、实时等待请求命令；
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (this.mParing) {
			// 正在配对，则隐藏配对按钮，显示停止按钮
			menu.findItem(R.id.menu_discover).setVisible(false);
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
		} else {
			menu.findItem(R.id.menu_discover).setVisible(true);
			menu.findItem(R.id.menu_stop).setVisible(false);
			menu.findItem(R.id.menu_refresh).setActionView(null);
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.menu_discover) {
			bleAdvertising(true);
		}
		else if (id == R.id.menu_stop) {
			bleAdvertising(false);
		} 
		return true;
	}
	
	private void bleAdvertising(boolean start) {
		this.mParing = start;
		if (start) {
			this.mBleReceive.startAdvertising();
		} else {
			this.mBleReceive.stopAdvertising();
		}
		invalidateOptionsMenu();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_send:
			String msg = this.mSendEdit.getText().toString();
			sendMessage(msg);
			break;

		default:
			break;
		}
	}
	
	private  ArrayAdapter<String> mAdapter; 
	/**
	 * 初始化消息接收器；
	 */
	private void initReceiveList() {
		ListView lv = (ListView) findViewById(R.id.list_receive_msg);
		this.mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		lv.setAdapter(this.mAdapter);
	}
	private void addMessage(String type, String info) {
		if (this.mAdapter != null) {
			this.mAdapter.add(type + ":" + info);
			this.mAdapter.notifyDataSetInvalidated();
		}
	}
	
	private void sendMessage(String msg) {
		if (TextUtils.isEmpty(msg))
			return;
		
		this.writeSendMsg(msg);
	}

	@Override
	public void write(String type, String info) {
		this.addMessage(type, info);
	}

	@Override
	public void writeReceiveMsg(String info) {
		this.addMessage("Receive", info);
	}

	@Override
	public void writeSendMsg(String info) {
		this.addMessage("Send", info);
	}
}
