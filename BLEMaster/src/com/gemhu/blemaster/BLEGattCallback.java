package com.gemhu.blemaster;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.util.Log;

public class BLEGattCallback extends BluetoothGattCallback {
	private final static String TAG = BLEGattCallback.class.getSimpleName();
	
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public int mConnectionState = STATE_DISCONNECTED;

    public final static String ACTION_GATT_CONNECTED           = "com.gemhu.ble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED        = "com.gemhu.ble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.gemhu.ble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE           = "com.gemhu.ble.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA                      = "com.gemhu.ble.EXTRA_DATA";

	private BLEService mService;
	
	public BLEGattCallback(BLEService service) {
		this.mService = service;
	}

	@Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        String intentAction;
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            intentAction = ACTION_GATT_CONNECTED;
            mConnectionState = STATE_CONNECTED;
            this.broadcastUpdate(intentAction);
            Log.i(TAG, "Connected to GATT server.");
            // Attempts to discover services after successful connection.
            Log.i(TAG, "Attempting to start service discovery:" +
                    mService.mBluetoothGatt.discoverServices());
            gatt.requestMtu(63);

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            intentAction = ACTION_GATT_DISCONNECTED;
            mConnectionState = STATE_DISCONNECTED;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
            
            mService.mBluetoothGatt.disconnect();
            mService.mBluetoothGatt.close();
            mService.mBluetoothGatt = null;
        }
    }

    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
    	Log.e(TAG, "MTU Changed : " + status);
    };
    
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
        } else {
            Log.w(TAG, "onServicesDiscovered received: " + status);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic,
                                     int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
    }
    
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    	// super.onCharacteristicWrite(gatt, characteristic, status);
//    	if (status == BluetoothGatt.GATT_SUCCESS)
//    		Log.i(TAG, "Write SUCCESS");
//    	else 
//    		Log.i(TAG, "Write Faild!");
    };
    
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
    	if (status == BluetoothGatt.GATT_SUCCESS)
    		Log.i(TAG, "Reliable Write SUCCESS");
    	else 
    		Log.i(TAG, "Reliable Write Faild!");
    };
    

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        this.mService.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                String info = new String(data) + "\n" + stringBuilder.toString();
                intent.putExtra(EXTRA_DATA, info);
                // Log.i(TAG, CharacterOperaterActivity.bytesToHexString(data));
                // log.info(CharacterOperaterActivity.bytesToHexString(data));
//                if (CharacterOperaterActivity.ResponseCheckUtils != null) {
//                	// CharacterOperaterActivity.ResponseCheckUtils.checkResponse(CharacterOperaterActivity.bytesToHexString(data));
//                	CharacterOperaterActivity.ResponseCheckUtils.checkSendCount(CharacterOperaterActivity.bytesToHexString(data), CharacterOperaterActivity.SendPackages);
//                }
                // Log2File.Write(CharacterOperaterActivity.bytesToHexString(data));
            }
        
        this.mService.sendBroadcast(intent);
    }

}
