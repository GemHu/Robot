package com.hu.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class CharacterListActivity extends Activity {

	private static final String TAG = CharacterListActivity.class.getName();
	protected static final String EXTRAS_SERVICE_UUID = "Service_UUID";

    private BluetoothLeService mBluetoothLeService;
	private ListView mLvCharacteristics;
	private CharacteristicAdapter mAdapter;
	private String mServiceUUID;
	
	private ServiceConnection mServiceConn = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBluetoothLeService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            
            UUID uuid = UUID.fromString(mServiceUUID);
            BluetoothGattService gattService = mBluetoothLeService.getGattService(uuid);
            List<BluetoothGattCharacteristic> characterList = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : characterList) {
				mAdapter.dataList.add(bluetoothGattCharacteristic);
				
				if ((bluetoothGattCharacteristic.getProperties() & bluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)
						mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristic, true);
			}
            mAdapter.notifyDataSetChanged();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_main);
		
		this.mLvCharacteristics = (ListView) findViewById(R.id.lv_devices);
		
		this.mServiceUUID = getIntent().getStringExtra(EXTRAS_SERVICE_UUID);
		getActionBar().setTitle("特征值列表");
        getActionBar().setDisplayHomeAsUpEnabled(true);
		
        // 
        this.mAdapter = new CharacteristicAdapter();
        this.mLvCharacteristics.setAdapter(this.mAdapter);
        this.mLvCharacteristics.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				BluetoothGattCharacteristic item = (BluetoothGattCharacteristic) mAdapter.getItem(position);
				Intent service = new Intent(CharacterListActivity.this, CharacterOperaterActivity.class);
				service.putExtra(CharacterOperaterActivity.EXTRA_UUID_SERVICE, mServiceUUID);
				service.putExtra(CharacterOperaterActivity.EXTRA_UUID_CHARACTERISTIC, item.getUuid().toString());
				startActivity(service);
			}
		});
        
		// 绑定服务
        
		Intent service = new Intent(this, BluetoothLeService.class);
		this.bindService(service, this.mServiceConn, BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		this.unbindService(mServiceConn);
	}
	
	class CharacteristicAdapter extends BaseAdapter {

		public List<BluetoothGattCharacteristic> dataList = new ArrayList<BluetoothGattCharacteristic>();
		
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
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = LayoutInflater.from(CharacterListActivity.this).inflate(R.layout.item_characteristics, null);
				holder = new ViewHolder();
				convertView.setTag(holder);
				
				holder.characterName = (TextView) convertView.findViewById(R.id.item_characteristic_name);
				holder.characterUUID = (TextView) convertView.findViewById(R.id.item_characteristic_uuid);
				holder.instanceId = (TextView) convertView.findViewById(R.id.item_characteristic_instanceId);
				holder.property = (TextView) convertView.findViewById(R.id.item_characteristic_property);
			}
			else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			//
			BluetoothGattCharacteristic data = this.dataList.get(position);
			holder.characterName.setText("UnKnownCharacteristics");
			holder.characterUUID.setText(data.getUuid().toString());
			holder.instanceId.setText(String.format("%s", data.getInstanceId()));
			holder.property.setText(String.format("%s", data.getProperties()));
			
			return convertView;
		}
		
		class ViewHolder {
			TextView characterName;
			TextView characterUUID;
			TextView instanceId;
			TextView property;
		}
	}
	
}
