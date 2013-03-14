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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.content.pm.PackageManager;
import android.util.Log;

public class FileDataTarget extends DefaultDataTarget {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String TAG = "FileDataTarget";
	public static final String ACTION_CONTEXT_CHANGED = "cz.cuni.kacz.contextLogger.ACTION_CONTEXT_CHANGED";
	public static final String ACTION_LISTENER_ADDED = "cz.cuni.kacz.contextLogger.ACTION_LISTENER_ADDED";
	private final String mFileName;
	private long mStartTime;

	private static final int DATA_MAGIC = 0x574f4c53;
	private static final byte VERSION = 1;

	// private DataOutputStream mHeaderStream;
	private DataOutputStream mDataStream;
	private BufferedWriter mHeaderWriter;

	public FileDataTarget(String fileName) {
		mFileName = fileName;
	}

	@Override
	public void insertLog(int listenerId, long time, int value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: int");
		try {
			mDataStream.writeInt(listenerId);
			mDataStream.writeLong(time - mStartTime);
			Log.d(TAG, String.valueOf(time - mStartTime));
			mDataStream.writeInt(value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void insertLog(int listenerId, long time, long value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: long");
		try {
			mDataStream.writeInt(listenerId);
			mDataStream.writeLong(time - mStartTime);
			Log.d(TAG, String.valueOf(time - mStartTime));
			mDataStream.writeLong(value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void insertLog(int listenerId, long time, float value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: float");
		try {
			mDataStream.writeInt(listenerId);
			mDataStream.writeLong(time - mStartTime);
			Log.d(TAG, String.valueOf(time - mStartTime));
			mDataStream.writeFloat(value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void insertLog(int listenerId, long time, String value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: string");
		try {
			mDataStream.writeInt(listenerId);
			mDataStream.writeLong(time - mStartTime);
			Log.d(TAG, String.valueOf(time - mStartTime));
			mDataStream.writeUTF(value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void insertLog(int listenerId, long time, double value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: double");
		try {
			mDataStream.writeInt(listenerId);
			mDataStream.writeLong(time - mStartTime);
			Log.d(TAG, String.valueOf(time - mStartTime));
			mDataStream.writeDouble(value);
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
					new FileOutputStream(new File(mFileName + ".clog"))));
			mDataStream = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(mFileName + ".cdata"))));
			Log.d(TAG, "streams open");
			Log.d(TAG, "fileName: " + mFileName + ".clog");
			Log.d(TAG, "fileName: " + mFileName + ".cdata");
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
			mDataStream.writeInt(DATA_MAGIC);
			mDataStream.writeByte(VERSION);
			mDataStream.writeLong(mStartTime);
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
			FileOutputStream out = new FileOutputStream(mFileName + ".clog",
					true /* append */);
			FileInputStream in = new FileInputStream(mFileName + ".cdata");
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

			File file = new File(mFileName + ".cdata");
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
