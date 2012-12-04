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
package cz.cuni.kacz.contextlogger.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import cz.cuni.kacz.contextlogger.ContextLoggerService;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.listeners.DefaultContextListener;

public class TestListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String TAG = "TestListener";

	private BroadcastReceiver mWifiBCReceiver = null;
	private HandlerThread mThread = null;
	private String lastBSSID = null;

	// log names and types
	private String labelTest = "Test";
	private int typeTest = DataManager.STRING;

	@Override
	public void startListening() {
		mWifiBCReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				long time = System.nanoTime();
				Log.i(TAG, "state change action rcvd");
				NetworkInfo netInfo = intent
						.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				State state = netInfo.getState();
				String bssid = "";
				if (state == State.CONNECTED) {
					bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
				}
				if (!bssid.equals(lastBSSID)) {
					mDataManager.insertLog(labelTest, time, bssid);
					lastBSSID = bssid;
				}

			};
		};

		mThread = new HandlerThread(TAG);
		mThread.start();
		Looper looper = mThread.getLooper();
		Handler handler = new Handler(looper);
		ContextLoggerService.mAppContext.registerReceiver(mWifiBCReceiver,
				new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION),
				null, handler);
		Log.d(TAG, "context: " + ContextLoggerService.mAppContext.hashCode());
	}

	@Override
	public void stopListening() {
		ContextLoggerService.mAppContext.unregisterReceiver(mWifiBCReceiver);
		mThread.quit();
	}

	@Override
	public void initLogTypes() {
		addLogType(labelTest, typeTest);
	}

}
