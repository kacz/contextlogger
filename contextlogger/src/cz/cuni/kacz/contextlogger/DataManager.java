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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

public class DataManager {

	private final String TAG = "DataManager";

	private final List<DataTarget> mDataTargets;

	private Map<String, Integer> mListenerIDs = null;
	private List<Integer> mValueTypes = null;

	Thread mWorker = null;
	private BlockingQueue<LogEntry> mLogs = null;

	public static final int INT = 1;
	public static final int LONG = 2;
	public static final int FLOAT = 3;
	public static final int DOUBLE = 4;
	public static final int STRING = 5;

	DataManager() {
		mDataTargets = new ArrayList<DataTarget>();
		mLogs = new LinkedBlockingQueue<LogEntry>();

		mListenerIDs = new HashMap<String, Integer>();
		mValueTypes = new ArrayList<Integer>();

		mWorker = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						LogEntry entry = mLogs.take();
						Log.d(TAG, entry.label);
						int listenerId = mListenerIDs.get(entry.label);
						int type = mValueTypes.get(listenerId);
						Log.d(TAG, "id: " + listenerId + " int: "
								+ entry.intValue + " long: " + entry.longValue
								+ " float: " + entry.floatValue + " string: "
								+ entry.stringValue + " type: " + type);
						switch (type) {
						case INT:
							for (DataTarget dt : mDataTargets) {
								dt.insertLog(listenerId, entry.time,
										entry.intValue);
							}
							break;
						case LONG:
							for (DataTarget dt : mDataTargets) {
								dt.insertLog(listenerId, entry.time,
										entry.longValue);
							}
							break;
						case FLOAT:
							for (DataTarget dt : mDataTargets) {
								dt.insertLog(listenerId, entry.time,
										entry.floatValue);
							}
							break;
						case DOUBLE:
							for (DataTarget dt : mDataTargets) {
								dt.insertLog(listenerId, entry.time,
										entry.doubleValue);
							}
							break;
						case STRING:
							for (DataTarget dt : mDataTargets) {
								dt.insertLog(listenerId, entry.time,
										entry.stringValue);
							}
							break;
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		mWorker.start();
	}

	public void addDataTarget(DataTarget t) {
		if (t.checkPermissions()) {
			t.open();
			mDataTargets.add(t);
		}
	}

	public void finish() {
		for (DataTarget dt : mDataTargets) {
			dt.close();
		}
		mDataTargets.clear();
	}

	public void insertLog(String listenerName, long time, long value) {
		mLogs.add(new LogEntry(time, listenerName, value));
		/*
		 * for(DataTarget dt : mDataTargets) { dt.insertLog(listenerName, time,
		 * value); }
		 */
		Log.d(TAG, "insertLog: " + String.valueOf(value) + " long");
	}

	public void insertLog(String listenerName, long time, float value) {
		mLogs.add(new LogEntry(time, listenerName, value));
		Log.d(TAG, "insertLog: " + String.valueOf(value) + " float");
	}

	public void insertLog(String listenerName, long time, int value) {
		mLogs.add(new LogEntry(time, listenerName, value));
		Log.d(TAG, "insertLog: " + String.valueOf(value) + " int");
	}

	public void insertLog(String listenerName, long time, double value) {
		mLogs.add(new LogEntry(time, listenerName, value));
		Log.d(TAG, "insertLog: " + String.valueOf(value) + " double");
	}

	public void insertLog(String listenerName, long time, String value) {
		mLogs.add(new LogEntry(time, listenerName, value));
		Log.d(TAG, "insertLog: " + value + " string");
	}

	public void registerListener(String label, int type) {
		int id = mValueTypes.size();
		mListenerIDs.put(label, id);
		mValueTypes.add(type);
		Log.d(TAG, "listener registered: id=" + id + " type=" + type);
		for (DataTarget dt : mDataTargets) {
			dt.registerListener(id, type, label);
		}
	}

}
