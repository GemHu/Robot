package com.hu.robot.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Environment;

public class Log2File {

	private static FileWriter writer;
	private static FileWriter getWriter() throws IOException {
		if (writer == null) {
			String filePath = Environment.getExternalStorageDirectory() + File.separator + "MyApp" + File.separator + "logs";
			File logDir = new File(filePath);
			if (!logDir.exists() || !logDir.isDirectory())
				logDir.mkdirs();
			filePath += (File.separator + "log.txt");
			File logFile = new File(filePath);
			if (!logFile.exists())
				logFile.createNewFile();
			
			writer = new FileWriter(logFile);
		}
		
		return writer;
	}
	
	public static boolean Write(String data) {
		try {
			FileWriter writer = getWriter();
			if (writer == null)
				return false;
			
			writer.write(data);
			writer.flush();
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public static boolean close() {
		if (writer == null)
			return true;
		
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		writer = null;
		return true;
	}
}
