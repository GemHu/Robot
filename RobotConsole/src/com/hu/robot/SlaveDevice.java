package com.hu.robot;

import java.util.UUID;

import com.hu.robot.tools.Utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Adapter;

/**
 * 从设备，用于进行测试的时候接收数据；
 * 
 * @author hdx_h
 *
 */
public class SlaveDevice {

	public SlaveDevice(Activity activity, BluetoothAdapter adapter) {
		this.mAdapter = adapter;
		this.mActivity = activity;
	}

	private BluetoothAdapter mAdapter;
	private Activity mActivity;
	private BluetoothGattServer bluetoothGattServer;
	
	private void showInfo(String info) {
		Log.i(SlaveDevice.class.getSimpleName(), info);
	}

	private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);

            final String info = service.getUuid().toString();
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showInfo("1.3 BluetoothGattServerCallback-onServiceAdded " + info);
                }
            });


        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            final String info = device.getAddress() + "|" + status + "->" + newState;

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showInfo("1.4 onConnectionStateChange " + info);
                }
            });
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);


            final String deviceInfo = "Address:" + device.getAddress();
            final String info = "Request:" + requestId + "|Offset:" + offset + "|characteristic:" + characteristic.getUuid() + "|Value:" +
                    Utils.toHexString(characteristic.getValue());

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    showInfo("=============================================");
                    showInfo("设备信息 " + deviceInfo);
                    showInfo("数据信息 " + info);
                    showInfo("=========onCharacteristicReadRequest=========");

                }
            });

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());

        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic,
                    preparedWrite, responseNeeded, offset, value);

            final String deviceInfo = "Name:" + device.getAddress() + "|Address:" + device.getAddress();
            final String info = "Request:" + requestId + "|Offset:" + offset + "|characteristic:" + characteristic.getUuid() + "|Value:" + Utils.toHexString(value);


            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
           //TODO:你做数据处理


            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    showInfo("=============================================");
                    showInfo("设备信息 " + deviceInfo);
                    showInfo("数据信息 " + info);
                    showInfo("=========onCharacteristicWriteRequest=========");

                }
            });


        }


        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);

            final String info = "Address:" + device.getAddress() + "|status:" + status;

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showInfo("onNotificationSent " + info);
                }
            });
        }


        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            final String deviceInfo = "Name:" + device.getAddress() + "|Address:" + device.getAddress();
            final String info = "Request:" + requestId + "|Offset:" + offset + "|characteristic:" + descriptor.getUuid() + "|Value:" + Utils.toHexString(value);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    showInfo("=============================================");
                    showInfo("设备信息 " + deviceInfo);
                    showInfo("数据信息 " + info);
                    showInfo("=========onDescriptorWriteRequest=========");

                }
            });


            // 告诉连接设备做好了
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {

            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            final String deviceInfo = "Name:" + device.getAddress() + "|Address:" + device.getAddress();
            final String info = "Request:" + requestId + "|Offset:" + offset + "|characteristic:" + descriptor.getUuid();


            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    showInfo("=============================================");
                    showInfo("设备信息 " + deviceInfo);
                    showInfo("数据信息 " + info);
                    showInfo("=========onDescriptorReadRequest=========");

                }
            });

            // 告诉连接设备做好了
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);


        }

	};

	public void setAdvertise() {
		BluetoothLeAdvertiser advertiser = this.mAdapter.getBluetoothLeAdvertiser();
		if (advertiser == null)
			return;

		// 广播设置
		AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
		builder.setConnectable(true);
		builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
		builder.setTimeout(0);
		builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
		AdvertiseSettings settings = builder.build();

		// 广播参数
		AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
		mAdapter.setName("BLE-Slave");
		dataBuilder.setIncludeDeviceName(true);
		dataBuilder.setIncludeTxPowerLevel(true);

		dataBuilder.addServiceUuid(ParcelUuid.fromString("a992e2b7-be61-417c-b05c-5efa54ebfd87"));
		AdvertiseData data = dataBuilder.build();

		advertiser.startAdvertising(settings, data, new AdvertiseCallback() {
			@Override
			public void onStartSuccess(AdvertiseSettings settingsInEffect) {
				super.onStartSuccess(settingsInEffect);
				SlaveDevice.this.mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showInfo("1.1 AdvertiseCallback-onStartSuccess");
					}
				});

				BluetoothManager bluetoothManager = (BluetoothManager) mActivity
						.getSystemService(Context.BLUETOOTH_SERVICE);
				bluetoothGattServer = bluetoothManager.openGattServer(mActivity.getApplicationContext(),
						mGattServerCallback);

				BluetoothGattService service = new BluetoothGattService(UUID.fromString(UUID_SERVICE),
						BluetoothGattService.SERVICE_TYPE_PRIMARY);

				UUID UUID_CHARREAD = UUID.fromString(UUID_CHARACTERISTIC);

				// 特征值读写设置
				BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHARREAD,
						BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ
								| BluetoothGattCharacteristic.PROPERTY_NOTIFY,
						BluetoothGattCharacteristic.PERMISSION_WRITE);

				UUID UUID_DESCRIPTOR = UUID.fromString(UUID_CHARACTERISTIC_CONFIG);

				BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_DESCRIPTOR,
						BluetoothGattCharacteristic.PERMISSION_WRITE);
				characteristicWrite.addDescriptor(descriptor);
				service.addCharacteristic(characteristicWrite);

				bluetoothGattServer.addService(service);

				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showInfo("1.2. Service Builded ok");
					}
				});

			}

		});

	}
	private final static String UUID_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	private final static String UUID_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
	private final static String UUID_WRITE = "0000fff1-0000-1000-8000-00805f9b34fb";
	private final static String UUID_ENABLE = "0000fff2-0000-1000-8000-00805f9b34fb";
	private final static String UUID_CHARACTERISTIC = "0000fff3-0000-1000-8000-00805f9b34fb";
}
