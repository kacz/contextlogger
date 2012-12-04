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

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class IntentDataTarget implements DataTarget {

	private static final String TAG = "IntentDataTarget";
	public static final String ACTION_CONTEXT_CHANGED = "cz.cuni.kacz.contextLogger.ACTION_CONTEXT_CHANGED";
	public static final String ACTION_LISTENER_ADDED = "cz.cuni.kacz.contextLogger.ACTION_LISTENER_ADDED";
	private Context mContext = null;

	public IntentDataTarget(Context context) {
		mContext = context;
	}

	public void insertLog(int listenerId, long time, int value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: int");
		Intent intent = new Intent(ACTION_CONTEXT_CHANGED);
		intent.putExtra("id", listenerId);
		intent.putExtra("time", time);
		intent.putExtra("intValue", value);
		mContext.sendBroadcast(intent);
	}

	public void insertLog(int listenerId, long time, long value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: long");
		Intent intent = new Intent(ACTION_CONTEXT_CHANGED);
		intent.putExtra("id", listenerId);
		intent.putExtra("time", time);
		intent.putExtra("longValue", value);
		mContext.sendBroadcast(intent);
	}

	public void insertLog(int listenerId, long time, float value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: float");
		Intent intent = new Intent(ACTION_CONTEXT_CHANGED);
		intent.putExtra("id", listenerId);
		intent.putExtra("time", time);
		intent.putExtra("floatValue", value);
		mContext.sendBroadcast(intent);
	}

	public void insertLog(int listenerId, long time, String value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: string");
		Intent intent = new Intent(ACTION_CONTEXT_CHANGED);
		intent.putExtra("id", listenerId);
		intent.putExtra("time", time);
		intent.putExtra("stringValue", value);
		mContext.sendBroadcast(intent);
	}

	public void insertLog(int listenerId, long time, double value) {
		Log.d(TAG, "id: " + listenerId + " value: " + value + " type: double");
		Intent intent = new Intent(ACTION_CONTEXT_CHANGED);
		intent.putExtra("id", listenerId);
		intent.putExtra("time", time);
		intent.putExtra("doubleValue", value);
		mContext.sendBroadcast(intent);
	}

	public void registerListener(int listenerId, int type, String listenerName) {
		Log.d(TAG, "reg3");
		Intent intent = new Intent(ACTION_LISTENER_ADDED);
		intent.putExtra("name", listenerName);
		intent.putExtra("id", listenerId);
		Log.d(TAG, "context: " + mContext.hashCode());
		mContext.sendBroadcast(intent);
	}

	public void open() {
		return;
	}

	public void close() {
		return;
	}

	public boolean checkPermissions() {
		return true;
	}
}
