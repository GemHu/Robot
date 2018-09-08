package com.gemhu.bleslave;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

public class BLEMsgReceive implements ILog {
	public final static String TAG = BLEMsgReceive.class.getSimpleName();
//	public final static String CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
//	public final static String LOST_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
//	public final static String LOST_WRITE = "0000fff1-0000-1000-8000-00805f9b34fb";
//	public final static String LOST_ENABLE = "0000fff2-0000-1000-8000-00805f9b34fb";
//	
//	public final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//	public final static UUID UUID_LOST_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
//	public final static UUID UUID_LOST_WRITE = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
//	public final static UUID UUID_LOST_ENABLE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");

	public ILog log;
	private Activity mActivity;
	private BluetoothAdapter mBTAdapter;
	
	public BLEMsgReceive(Activity activity) {
		this.mActivity = activity;
		this.init();
	}

    private short mMajor = 0;
    private short mMinor = 0;

    public void setMajor(int major) {
        mMajor = (short) major;
    }

    public void setMinor(int minor) {
        mMinor = (short) minor;
    }

	/**
	 * 初始化BLE设备；
	 */
	public void init() {
//		mHandler = new Handler();
		// 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
		if (!mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			this.write("InitBLE", "当前手机不支持ble 蓝牙!");
			return;
		}

		// 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
		final BluetoothManager bluetoothManager = getBluetoothManager();
		mBTAdapter = bluetoothManager.getAdapter();
		if (mBTAdapter == null) {
			this.write("InitBLE", "当前手机不支持ble 蓝牙!");
			return;
		}
	}
	
	private BluetoothManager getBluetoothManager() {
		return (BluetoothManager) this.mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
	}
	
	private BluetoothLeAdvertiser getAdvertiser() {
		if (this.mBTAdapter == null)
			return null;
		BluetoothLeAdvertiser advertiser = this.mBTAdapter.getBluetoothLeAdvertiser();
		if (advertiser == null) {
        	this.write("InitBLE", "该设备不支持蓝牙低功耗从设备通讯");
        	return null;
		}
		
		return advertiser;
	}
	
	/**
	  *广播的一些基本设置
	  **/
	public AdvertiseSettings createAdvSettings(boolean connectAble, int timeoutMillis) {
	    AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
	    // 设置广播模式，低功耗，平衡，低延迟，从左到右广播的间隔会越来越短；
	    // ADVERTISE_MODE_LOW_POWER  ,ADVERTISE_MODE_BALANCED ,ADVERTISE_MODE_LOW_LATENCY
	    builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
	    // 设置是否可以连接。广播分为可连接广播和不可连接广播，一般不可连接广播应用在iBeacon设备上，这样APP无法连接上iBeacon设备
	    builder.setConnectable(connectAble);
	    // 设置广播的最长时间，最大值为常量：AdvertiseSettings.LIMITED_ADVERTISING_MAX_MILLIS = 180 * 1000;  180秒
	    // 设置0表示无时间限制，会一直广播；
	    builder.setTimeout(timeoutMillis);
	    // 设置广播的信号强度，下列常量从左到右分别表示强度越来越强. 
	    // 常量有AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW, ADVERTISE_TX_POWER_LOW, ADVERTISE_TX_POWER_MEDIUM, ADVERTISE_TX_POWER_HIGH 
	    builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
	    AdvertiseSettings mAdvertiseSettings = builder.build();
	    if (mAdvertiseSettings == null) {
	        this.write("createAdvSettings", "mAdvertiseSettings == null");
	    }
	    return mAdvertiseSettings;
	}
	
	public AdvertiseData createAdvertiseData() {
		AdvertiseData.Builder builder = new AdvertiseData.Builder();
		// 广播名称也需要字节长度
		builder.setIncludeDeviceName(true);
		builder.setIncludeTxPowerLevel(true);
		ParcelUuid uuid = ParcelUuid.fromString(BluetoothUUID.BLE_START.toString());
//		builder.addManufacturerData(0x0301, new byte[] {0x01, 0x03});
//		builder.addServiceUuid(uuid);
		builder.addServiceData(uuid, new byte[] {0x01, 0x03});
		AdvertiseData data = builder.build();
		if (data == null)
			this.write("createAdvertiseData", "advertiseData == null");
		
		return data;
	}
	
	//设置scan广播数据
    public AdvertiseData createScanAdvertiseData(ParcelUuid uuid, short major, short minor, byte txPower) {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.setIncludeDeviceName(true);

        byte[] serverData = new byte[5];
        ByteBuffer bb = ByteBuffer.wrap(serverData);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putShort(major);
        bb.putShort(minor);
        bb.put(txPower);
        builder.addServiceData(uuid, serverData);

        AdvertiseData adv = builder.build();
        return adv;
    }
    
    /**
     * create AdvertiseDate for iBeacon
     */
    public AdvertiseData createIBeaconAdvertiseData(UUID proximityUuid, short major, short minor, byte txPower) {

        String[] uuidstr = proximityUuid.toString().replaceAll("-", "").toLowerCase().split("");
        byte[] uuidBytes = new byte[16];
        for (int i = 1, x = 0; i < uuidstr.length; x++) {
            uuidBytes[x] = (byte) ((Integer.parseInt(uuidstr[i++], 16) << 4) | Integer.parseInt(uuidstr[i++], 16));
        }
        byte[] majorBytes = {(byte) (major >> 8), (byte) (major & 0xff)};
        byte[] minorBytes = {(byte) (minor >> 8), (byte) (minor & 0xff)};
        byte[] mPowerBytes = {txPower};
        byte[] manufacturerData = new byte[0x17];
        byte[] flagibeacon = {0x02, 0x15};

        System.arraycopy(flagibeacon, 0x0, manufacturerData, 0x0, 0x2);
        System.arraycopy(uuidBytes, 0x0, manufacturerData, 0x2, 0x10);
        System.arraycopy(majorBytes, 0x0, manufacturerData, 0x12, 0x2);
        System.arraycopy(minorBytes, 0x0, manufacturerData, 0x14, 0x2);
        System.arraycopy(mPowerBytes, 0x0, manufacturerData, 0x16, 0x1);

        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.addManufacturerData(0x004c, manufacturerData);

        AdvertiseData adv = builder.build();
        return adv;
    }
    
    /**
     * 开始广播；
     */
	public void startAdvertising() {
		BluetoothLeAdvertiser advertiser = this.getAdvertiser();
		AdvertiseSettings settings = createAdvSettings(true, 0);
		if (advertiser == null || settings == null)
			return;
		
		BLEServerCallBack callback = new BLEServerCallBack();
		callback.mLog = this.log;
		BluetoothGattServer gattServer = getBluetoothManager().openGattServer(mActivity, callback);
		if (gattServer == null) {
			this.log.write("Error", "gattServer == null");
			return;
		}
		
		callback.setupServices(mActivity, gattServer);
		advertiser.startAdvertising(settings, createAdvertiseData(), mAdvCallback);
		
//		AdvertiseData data = createAdvertiseData();
//		AdvertiseData ibeaconData = createIBeaconAdvertiseData(UUID.fromString(LOST_SERVICE), this.mMajor, this.mMinor, (byte) -0x3b);
//		AdvertiseData scanData = this.createScanAdvertiseData(ParcelUuid.fromString(LOST_SERVICE), this.mMajor, this.mMinor, (byte) -0x3b);
//		// 开始广播
//		advertiser.startAdvertising(settings, ibeaconData, scanData, mAdvCallback);
	}
	/**
	 * 停止广播；
	 */
	public void stopAdvertising() {
		BluetoothLeAdvertiser advertiser = getAdvertiser();
		if (advertiser != null)
			advertiser.stopAdvertising(mAdvCallback);
	}
	
	private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
		public void onStartSuccess(android.bluetooth.le.AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            
            write("AdvertiseCallback", "Advertise Start Success");
            Toast.makeText(mActivity, "Advertise Start Success", Toast.LENGTH_SHORT).show();
            if (settingsInEffect != null) {
                Log.d(TAG, "onStartSuccess TxPowerLv=" + settingsInEffect.getTxPowerLevel() + " mode=" + settingsInEffect.getMode() + " timeout=" + settingsInEffect.getTimeout());
            } else {
                Log.d(TAG, "onStartSuccess, settingInEffect is null");
            }
        }

        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            write("AdvertiseCallback", "onStartFailure errorCode=" + errorCode);

            if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                Toast.makeText(mActivity, "advertise_failed_data_too_large", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
            } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                Toast.makeText(mActivity, "advertise_failed_too_many_advertises", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising because no advertising instance is available.");

            } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                Toast.makeText(mActivity, "advertise_failed_already_started", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising as the advertising is already started");

            } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                Toast.makeText(mActivity, "advertise_failed_internal_error", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Operation failed due to an internal error");

            } else if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                Toast.makeText(mActivity, "advertise_failed_feature_unsupported", Toast.LENGTH_LONG).show();
                Log.e(TAG, "This feature is not supported on this platform");

            }
        }
	};
	
	@Override
	public void write(String type, String info) {
		if (this.log != null)
			this.log.write(type, info);
	}

	@Override
	public void writeReceiveMsg(String info) {
		if (this.log != null)
			this.log.writeReceiveMsg(info);
	}

	@Override
	public void writeSendMsg(String info) {
		if (this.log != null)
			this.log.writeSendMsg(info);
	}
}
