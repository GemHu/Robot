package com.gemhu.blemaster;

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
	public class Axis{
		public final static byte AllAxis = 0x00;
		public final static byte Axis1 = 0x01;
		public final static byte Axis2 = 0x02;
		public final static byte Axis3 = 0x03;
		public final static byte Axis4 = 0x04;
		public final static byte Axis5 = 0x05;
		public final static byte Axis6 = 0x06;
	}
	
//	private byte[] mData = new byte[6];
	
	public DataPackage(byte header, byte cmd, byte axis) {
		
	}
	
	public void setData(byte high, byte low) {
		
	}
	
	public void send() {
		
	}
}
