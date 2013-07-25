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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

/**
 * Component responsible for passing the context log records to all the data
 * targets. Contains a blocking queue of log records. Runs a worker thread for
 * consuming the records from the queue.
 * 
 * @author kacz
 * 
 */
/**
 * @author kacz
 * 
 */
public class DataManager {

	private final String TAG = "DataManager";

	boolean stop;

	/** Queue of data targets. */
	private final ConcurrentLinkedQueue<DataTarget> mDataTargets;

	/**
	 * Log name - log ID map.
	 */
	private Map<String, Integer> mListenerIDs = null;

	/**
	 * List containing types of each log.
	 */
	private List<Integer> mValueTypes = null;

	/**
	 * Consuming thread of context log records.
	 */
	Thread mWorker = null;

	/**
	 * Queue containing log recirds.
	 */
	private BlockingQueue<LogEntry> mLogs = null;

	public static final int INT = 1;
	public static final int LONG = 2;
	public static final int FLOAT = 3;
	public static final int DOUBLE = 4;
	public static final int STRING = 5;

	/**
	 * Package private constructor for the DataManager class. Starts the worker
	 * thread responsible for passing the records to the data targets.
	 */
	DataManager() {
		mDataTargets = new ConcurrentLinkedQueue<DataTarget>();
		
		mLogs = new LinkedBlockingQueue<LogEntry>();

		mListenerIDs = Collections
				.synchronizedMap(new HashMap<String, Integer>());
		mValueTypes = Collections.synchronizedList(new ArrayList<Integer>());

		mWorker = new Thread(new Runnable() {
			@Override
			public void run() {
				stop = false;
				try {
					while (true) {
						LogEntry entry = mLogs.take();
						if (stop) {
							continue;
						}
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
							if (entry.stringValue == null) {
								entry.stringValue = "null";
							}
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

	/**
	 * Add data target to the list of targets.
	 * 
	 * @param t
	 *            DataTarget to add.
	 */
	public void addDataTarget(DataTarget t) {
		if (t.checkPermissions()) {
			t.open();
			mDataTargets.add(t);
			stop = false;
		}
	}

	/**
	 * Stop the work of data targets. They will not accept more log records and
	 * close their opened files if any.
	 */
	public void finish() {
		stop = true;
		Log.d(TAG, "stop true");
		for (DataTarget dt : mDataTargets) {
			dt.close();
		}
		mDataTargets.clear();
		mValueTypes.clear();
	}

	/**
	 * Passes a LONG type log to the data targets.
	 * 
	 * @param listenerName
	 *            Name of the piece of context represented by the record.
	 * @param time
	 *            Timestamp of the record.
	 * @param value
	 *            Value of the record.
	 */
	public void insertLog(String listenerName, long time, long value) {
		mLogs.add(new LogEntry(time, listenerName, value));
		Log.d(TAG, "insertLog: " + String.valueOf(value) + " long");
	}

	/**
	 * Passes a FLOAT type log to the data targets.
	 * 
	 * @param listenerName
	 *            Name of the piece of context represented by the record.
	 * @param time
	 *            Timestamp of the record.
	 * @param value
	 *            Value of the record.
	 */
	public void insertLog(String listenerName, long time, float value) {
		mLogs.add(new LogEntry(time, listenerName, value));
		Log.d(TAG, "insertLog: " + String.valueOf(value) + " float");
	}

	/**
	 * Passes a INT type log to the data targets.
	 * 
	 * @param listenerName
	 *            Name of the piece of context represented by the record.
	 * @param time
	 *            Timestamp of the record.
	 * @param value
	 *            Value of the record.
	 */
	public void insertLog(String listenerName, long time, int value) {
		mLogs.add(new LogEntry(time, listenerName, value));
		Log.d(TAG, "insertLog: " + String.valueOf(value) + " int");
	}

	/**
	 * Passes a DOUBLE type log to the data targets.
	 * 
	 * @param listenerName
	 *            Name of the piece of context represented by the record.
	 * @param time
	 *            Timestamp of the record.
	 * @param value
	 *            Value of the record.
	 */
	public void insertLog(String listenerName, long time, double value) {
		mLogs.add(new LogEntry(time, listenerName, value));
		Log.d(TAG, "insertLog: " + String.valueOf(value) + " double");
	}

	/**
	 * Passes a STING type log to the data targets.
	 * 
	 * @param listenerName
	 *            Name of the piece of context represented by the record.
	 * @param time
	 *            Timestamp of the record.
	 * @param value
	 *            Value of the record.
	 */
	public void insertLog(String listenerName, long time, String value) {
		mLogs.add(new LogEntry(time, listenerName, value));
		Log.d(TAG, "insertLog: " + value + " string");
	}

	/**
	 * Registers a piece of context logged by a listener.
	 * 
	 * @param label
	 *            Name of the piece of context
	 * @param type
	 *            Type of the piece of context.
	 */
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
