/*
 * Copyright (C) 2012 Kristian Kacz 
 * 
 * This file is part of ContextLogger.
 *
 * ContextLogger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ContextLogger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ContextLogger.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package cz.cuni.kacz.contextlogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.content.pm.PackageManager;
import android.util.Log;

/**
 * DataTarget for saving context logs into a text file. The structure of the
 * file is similar to the output of the cz.cuni.kacz.contextlogger.DataTarget.
 * 
 * @author kacz
 * 
 */
public class TextFileDataTarget extends DefaultDataTarget {

	private static final String TAG = "FileDataTarget";
	public static final String ACTION_CONTEXT_CHANGED = "cz.cuni.kacz.contextLogger.ACTION_CONTEXT_CHANGED";
	public static final String ACTION_LISTENER_ADDED = "cz.cuni.kacz.contextLogger.ACTION_LISTENER_ADDED";
	private final String mFileName;
	private long mStartTime;

	private static final int DATA_MAGIC = 0x574f4c53;
	private static final byte VERSION = 1;

	// private DataOutputStream mHeaderStream;
	private BufferedWriter mDataStream;
	private BufferedWriter mHeaderWriter;

	/**
	 * DataTarget saving the logs into a text file.
	 * 
	 * @param fileName
	 *            Name of the text file to save into.
	 */
	public TextFileDataTarget(String fileName) {
		mFileName = fileName;
	}

	@Override
	public void insertLog(int listenerId, long time, int value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: int");
		try {
			mDataStream.write("Id: " + listenerId + "\n");
			mDataStream.write("time: " + (time - mStartTime) + "\n");
			Log.d(TAG, String.valueOf(time - mStartTime));
			mDataStream.write("value " + value + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void insertLog(int listenerId, long time, long value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: long");
		try {
			mDataStream.write("Id: " + listenerId + "\n");
			mDataStream.write("time: " + (time - mStartTime) + "\n");
			Log.d(TAG, String.valueOf(time - mStartTime));
			mDataStream.write("value " + value + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void insertLog(int listenerId, long time, float value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: float");
		try {
			mDataStream.write("Id: " + listenerId + "\n");
			mDataStream.write("time: " + (time - mStartTime) + "\n");
			Log.d(TAG, String.valueOf(time - mStartTime));
			mDataStream.write("value " + value + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void insertLog(int listenerId, long time, String value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: string");
		try {
			mDataStream.write("Id: " + listenerId + "\n");
			mDataStream.write("time: " + (time - mStartTime) + "\n");
			Log.d(TAG, String.valueOf(time - mStartTime));
			mDataStream.write("value " + value + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void insertLog(int listenerId, long time, double value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: double");
		try {
			mDataStream.write("Id: " + listenerId + "\n");
			mDataStream.write("time: " + (time - mStartTime) + "\n");
			Log.d(TAG, String.valueOf(time - mStartTime));
			mDataStream.write("value " + value + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void registerListener(int listenerId, int type, String listenerName) {
		try {
			mHeaderWriter.write(String.valueOf(listenerId) + " "
					+ String.valueOf(type) + " " + listenerName);
			mHeaderWriter.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void open() {
		try {
			Log.d(TAG, "opening...");
			// File path = Environment
			// .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

			mHeaderWriter = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(new File(mFileName
							+ ".text.clog"))));
			mDataStream = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(new File(mFileName
							+ ".text.cdata"))));
			Log.d(TAG, "streams open");
			mStartTime = TimeSource.getTimeOfDay();

			// write the beggining of head and data files
			// header
			mHeaderWriter.write("*version");
			mHeaderWriter.newLine();
			mHeaderWriter.write("1");
			mHeaderWriter.newLine();
			// options come here

			// end of options, log types come
			mHeaderWriter.write("*logs");
			mHeaderWriter.newLine();

			// data
			mDataStream.write("magic: " + DATA_MAGIC + "\n");
			mDataStream.write("version: " + VERSION + "\n");
			mDataStream.write("starttime: " + mStartTime + "\n");
			Log.d(TAG, "cucc writen.");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "filenotfound");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d(TAG, "ioe");
			e.printStackTrace();
		}
		;

	}

	@Override
	public void close() {
		try {
			Log.d(TAG, "closing...");
			// write the end of files
			mHeaderWriter.write("*end");
			mHeaderWriter.newLine();
			mHeaderWriter.flush();
			mDataStream.flush();

			mHeaderWriter.close();
			mDataStream.close();

			// concat files
			FileOutputStream out = new FileOutputStream(mFileName
					+ ".text.clog", true /* append */);
			FileInputStream in = new FileInputStream(mFileName + ".text.cdata");
			byte[] fileBytes;
			int bytesRead = 0;
			int COPY_BUFFER_SIZE = 1000;
			fileBytes = new byte[COPY_BUFFER_SIZE];
			bytesRead = in.read(fileBytes, 0, COPY_BUFFER_SIZE);
			while (bytesRead != -1) {
				out.write(fileBytes, 0, bytesRead);
				bytesRead = in.read(fileBytes, 0, COPY_BUFFER_SIZE);
			}
			out.flush();
			fileBytes = null;

			File file = new File(mFileName + ".text.cdata");
			file.delete();

			in.close();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean checkPermissions() {
		if (ContextLoggerService.mAppContext
				.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
			return false;
		}
		return true;
	}

}
