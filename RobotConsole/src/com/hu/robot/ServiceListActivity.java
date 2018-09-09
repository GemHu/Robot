package com.hu.robot;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
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

public class ServiceListActivity extends Activity {

	private static final String TAG = ServiceListActivity.class.getName();
	protected static final String EXTRAS_DEVICE_NAME = "DeviceName";
	protected static final String EXTRAS_DEVICE_ADDRESS = "DeviceAddress";

    private BluetoothLeService mBluetoothLeService;
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;
    private ListView mLvServices;
    private ServiceAdapter mAdapter;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
                mAdapter.clear();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } 
        }
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_main);
		
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        this.mLvServices = (ListView) findViewById(R.id.lv_devices);
        this.mAdapter = new ServiceAdapter();
        this.mLvServices.setAdapter(mAdapter);
        this.mLvServices.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				BluetoothGattService service = mAdapter.dataList.get(position);
				Intent intent1 = new Intent(ServiceListActivity.this, CharacterListActivity.class);
				intent1.putExtra(CharacterListActivity.EXTRAS_SERVICE_UUID, service.getUuid().toString());
				startActivity(intent1);
			}
		});
        

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        
        // Loops through available GATT Services.
        this.mAdapter.clear();
        for (BluetoothGattService gattService : gattServices) {
            this.mAdapter.AddService(gattService);
            Log.i(TAG, "ServiceUUID = " + gattService.getUuid());
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
	class ServiceAdapter extends BaseAdapter {

		List<BluetoothGattService> dataList = new ArrayList<BluetoothGattService>();
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return this.dataList.size();
		}
		
		public void AddService(BluetoothGattService service) {
			this.dataList.add(service);
			this.notifyDataSetChanged();
		}

		public void clear() {
			this.dataList.clear();
			this.notifyDataSetChanged();
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
			// Get Item UI
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = LayoutInflater.from(ServiceListActivity.this).inflate(R.layout.item_service, null);
				holder = new ViewHolder();
				convertView.setTag(holder);
				
				holder.serviceName = (TextView) convertView.findViewById(R.id.item_service_name);
				holder.serviceUUID = (TextView) convertView.findViewById(R.id.item_service_uuid);
				holder.instanceId = (TextView) convertView.findViewById(R.id.item_service_instanceId);
				holder.serviceType = (TextView) convertView.findViewById(R.id.item_service_type);
			}
			else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			// Set UI Content
			BluetoothGattService data = this.dataList.get(position);
			holder.serviceName.setText("UnKnownService");
			holder.serviceUUID.setText(String.format("%s", data.getUuid()));
			holder.instanceId.setText(String.format("%s", data.getInstanceId()));
			holder.serviceType.setText(String.format("%s", data.getType()));
			
			return convertView;
		}
		
		class ViewHolder {
			TextView serviceName;
			TextView serviceUUID;
			TextView instanceId;
			TextView serviceType;
		}
	}
	
}
