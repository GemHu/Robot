package com.hu.robot.tools;

import android.nfc.Tag;
import android.text.TextUtils;
import android.util.Log;

public class ResponseCheckUtils {

	private boolean isError;
	private String remainStr = "";
	private int mIndex = 0;
	private String prevPackage = "";
	private String currPackage = "";

	private static final int MinPackageSize = 12;

	public boolean checkResponse(String response) {
		prevPackage = currPackage;
		currPackage = response;
		remainStr += response;
		String sIndex = "";
		int index = 0;
		while (remainStr.length() >= MinPackageSize) {
			sIndex = remainStr.substring(2, 10);
			if (remainStr.length() > MinPackageSize)
				remainStr = remainStr.substring(MinPackageSize);
			else
				remainStr = "";

			index = Integer.parseInt(sIndex, 16);
			if (mIndex <= 0)
				mIndex = index;
			else {
				mIndex++;
			}

			if (mIndex != index) {
				Log.e("CheckResponse", "PrevIndex = " + Integer.toHexString(mIndex - 1) + "; Current Index = " + sIndex);
				Log.e("CheckResponse", "; prevPackage = " + prevPackage);
				Log.e("CheckResponse", "; currPackage = " + currPackage);
				mIndex = index;
				return false;
			}
//			else {
//				Log.i("CheckResponse", sIndex);
//			}
		}

		return true;
	}

	public boolean checkSendCount(String response, int sendCounts) {
		prevPackage = currPackage;
		currPackage = response;
		remainStr += response;
		String sRecvCount = "";
		while (remainStr.length() >= MinPackageSize) {
			sRecvCount = remainStr.substring(2, 6);
			if (remainStr.length() > MinPackageSize)
				remainStr = remainStr.substring(MinPackageSize);
			else
				remainStr = "";

			if (currPackage.substring(currPackage.length() - 2).compareToIgnoreCase("dd") == 0) {
				int recvCount = Integer.parseInt(sRecvCount, 16);
				if (sendCounts == recvCount) {
					Log.i("CheckSendCount", "SendCount = Received Count, Count = " + Integer.toHexString(recvCount));
//				Log.e("CheckSendCount",  "; prevPackage = " + prevPackage);
//				Log.e("CheckSendCount",  "; currPackage = " + currPackage);
				} else {
					Log.e("CheckSendCount", "SendCount = " + Integer.toHexString(sendCounts) + "; Received Count = " + Integer.toHexString(recvCount));
					Log.e("CheckSendCount", "; prevPackage = " + prevPackage);
					Log.e("CheckSendCount", "; currPackage = " + currPackage);
				}
			}
		}

		return true;
	}

	public void checkResult() {

	}
}
