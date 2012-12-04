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

package cz.cuni.kacz.contextlogger.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import cz.cuni.kacz.contextlogger.ContextLoggerService;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

public class WifiStateListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// private static final String TAG = "WifiStateListener";

	private BroadcastReceiver mWifiBCReceiver = null;
	private HandlerThread mThread = null;

	// log names and types
	private String labelWifiState = "Wifi state";
	private int typeWifiState = DataManager.INT;

	@Override
	public void startListening() {
		mWifiBCReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				long time = TimeSource.getTimeOfDay();
				Log.i(TAG, "state change action rcvd");
				int state = intent
						.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
				if (state != -1) {
					mDataManager.insertLog(labelWifiState, time, state);
				}
			};
		};

		mThread = new HandlerThread(TAG);
		mThread.start();
		Looper looper = mThread.getLooper();
		Handler handler = new Handler(looper);
		ContextLoggerService.mAppContext.registerReceiver(mWifiBCReceiver,
				new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION), null,
				handler);
		Log.d(TAG, "context: " + ContextLoggerService.mAppContext.hashCode());
	}

	@Override
	public void stopListening() {
		ContextLoggerService.mAppContext.unregisterReceiver(mWifiBCReceiver);
		mThread.quit();
	}

	@Override
	public void initLogTypes() {
		addLogType(labelWifiState, typeWifiState);
		Log.d(TAG, "vajon: " + checkPermissions());
	}

}
