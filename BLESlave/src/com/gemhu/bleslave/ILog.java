package com.gemhu.bleslave;

public interface ILog {
	void write(String type, String info);
	void writeReceiveMsg(String info);
	void writeSendMsg(String info);
}
