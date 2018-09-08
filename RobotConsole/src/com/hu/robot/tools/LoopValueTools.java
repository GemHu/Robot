package com.hu.robot.tools;

public class LoopValueTools {

	private byte[] mBuffer;
	private int mBufferIndex;
	public LoopValueTools(byte[] buffer) {
		this.mBuffer = buffer;
	}
	
	public byte[] loopValues(int length) {
		byte[] values = new byte[length];
		for (int i = 0; i < length; i++) {
			if (this.mBufferIndex >= this.mBuffer.length) {
				this.mBufferIndex = 0;
			}
			
			values[i] = this.mBuffer[this.mBufferIndex++];
		}
		
		return values;
	}
	
}
