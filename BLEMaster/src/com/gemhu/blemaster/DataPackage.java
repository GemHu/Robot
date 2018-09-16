package com.gemhu.blemaster;

import android.bluetooth.BluetoothGattCharacteristic;

public class DataPackage {

	public class Header{
		/**
		 * 手机命令下发到设备；
		 */
		public final static byte Download = (byte) 0xAA;
		/**
		 * 获取设备数据；
		 */
		public final static byte Upload = 0x55;
	}
	public class Command{
		public final static byte SetZeroPoint = 0x01;	//：设置零点
		public final static byte SetSpeed = 0x02;	//：设置速度
		public final static byte SetMinPosLimit = 0x03;	//：设置最小限位
		public final static byte SetMaxPosLimit = 0x04;	//：设置最大限位
		public final static byte StartMove = 0x05;	//：开始点动
		public final static byte StopMove = 0x06;	//：停止点动
		public final static byte TraceRunning = 0x07;	//：轨迹运行
		public final static byte SwitchMode = 0x08;	//：切换模式
		public final static byte GetSpeed = 0x09;	//：返回速度（设备上传）
		public final static byte GetPos = 0x0A;	//：返回位置（设备上传）
	}

	public final static byte RUNNING_TRACE1 = 0x01;
	public final static byte RUNNING_TRACE2 = 0x02;
	public final static byte RUNNING_TRACE3 = 0x03;
	public final static byte RUNNING_TRACE4 = 0x04;
	
	public final static byte MOVING_DIRECT = 0x01;
	public final static byte MOVING_REVERSE = 0x02;
	
	public final static byte RESPONSE_NORMAL = 0x00;
	public final static byte RESPONSE_CHECK_ERROR = 0x01;
	
	public final static byte ZERO = 0x00;
	
	public static DataPackage getDataOfSetZeroCmd() {
		return new DataPackage(Header.Download, Command.SetZeroPoint);
	}
	
	public static DataPackage getDataOfSetSpeed() {
		return new DataPackage(Header.Download, Command.SetSpeed);
	}
	
	public static DataPackage getDataOfSetMinLimit() {
		return new DataPackage(Header.Download, Command.SetMinPosLimit);
	}
	
	public static DataPackage getDataOfSetMaxLimit() {
		return new DataPackage(Header.Download, Command.SetMaxPosLimit);
	}
	
	public static DataPackage getDataOfStartMove() {
		return new DataPackage(Header.Download, Command.StartMove);
	}
	
	public static DataPackage getDataOfStopMove() {
		return new DataPackage(Header.Download, Command.StopMove);
	}
	
	public static DataPackage getDataOfRunningTrace() {
		return new DataPackage(Header.Download, Command.TraceRunning);
	}
	
	public static DataPackage getDataOfSwitchMode() {
		return new DataPackage(Header.Download, Command.SwitchMode);
	}
	
	public static DataPackage getDataOfGetSpeed() {
		return new DataPackage(Header.Upload, Command.GetSpeed);
	}
	
	public static DataPackage getDataOfGetPos() {
		return new DataPackage(Header.Upload, Command.GetPos);
	}
	
	private byte[] mData = new byte[6];
	
//	private byte[] mData = new byte[6];
	
	private DataPackage(byte header, byte cmd) {
		// header
		this.mData[0] = header;
		// cmd
		this.mData[1] = cmd;
		// axis
		this.mData[2] = 0x00;
	}
	
	public DataPackage setAxis(byte axis) {
		this.mData[2] = axis;
		
		return this;
	}
	
	public DataPackage setData(byte high, byte low) {
		this.mData[3] = high;
		this.mData[4] = low;
		
		return this;
	}
	
	public void check() {
		this.mData[5] = (byte) (this.mData[1] + this.mData[2] + this.mData[3] + this.mData[4]);
	}
	
	public void setCharecteristic(BluetoothGattCharacteristic characteristic) {
		this.check();
		characteristic.setValue(this.mData);
	}
}
