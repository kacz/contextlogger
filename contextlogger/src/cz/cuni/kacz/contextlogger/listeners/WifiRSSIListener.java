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
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

public class WifiRSSIListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String TAG = "WifiRSSIListener";

	private BroadcastReceiver mWifiBCReceiver = null;
	private HandlerThread mThread = null;
	private int lastRSSI = 0;

	// log names and types
	private final String labelRSSI = "Wifi strength";
	private final int typeRSSI = DataManager.INT;

	@Override
	public void startListening() {
		mWifiBCReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				long time = TimeSource.getTimeOfDay();
				Log.i(TAG, "rssi action rcvd");
				int rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
				if (rssi != 0 && rssi != lastRSSI) {
					mDataManager.insertLog(labelRSSI, time, rssi);
					lastRSSI = rssi;
				}
			};
		};

		mThread = new HandlerThread(TAG);
		mThread.start();
		Looper looper = mThread.getLooper();
		Handler handler = new Handler(looper);
		getAppContext().registerReceiver(mWifiBCReceiver,
				new IntentFilter(WifiManager.RSSI_CHANGED_ACTION), null,
				handler);
		Log.d(TAG, "context: " + getAppContext().hashCode());
		// ContextLoggerService.mAppContext.registerReceiver(mWifiBCReceiver,
		// new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
	}

	@Override
	public void stopListening() {
		getAppContext().unregisterReceiver(mWifiBCReceiver);
		mThread.quit();
	}

	@Override
	public void initLogTypes() {
		addLogType(labelRSSI, typeRSSI);
	}
}
