package com.gemhu.blemaster;

import java.util.UUID;

public class BLEUUID {

	// 正式服务
	public final static UUID SERVICE = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
	public final static UUID WRITE = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
	public final static UUID CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	
	// 测试服务 UUID不区分大小写
//	public final static UUID SERVICE = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
//	public final static UUID READ = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB");
//	public final static UUID WRITE = UUID.fromString("0000FFF3-0000-1000-8000-00805F9B34FB");
}
