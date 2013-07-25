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
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

/**
 * ContextListener for logging Wifi connection information.
 * 
 * @author kacz
 * 
 */
public class WifiListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// private static final String TAG = "WifiConnectionStateListener";

	private BroadcastReceiver mWifiBCReceiver = null;
	private WifiManager mWifiManager = null;
	private HandlerThread mThread = null;
	private String lastBSSID = null;
	private String lastSSID = null;
	private int oldIp = 0;

	// log names and types
	private final String labelBSSID = "BSSID";
	private final int typeBSSID = DataManager.STRING;
	private final String labelSSID = "SSID";
	private final int typeSSID = DataManager.STRING;
	private final String labelIP = "IP address";
	private final int typeIP = DataManager.STRING;
	private final String labelSpeed = "Link speed";
	private final int typeSpeed = DataManager.INT;

	private final String labelWifiConnectionState = "Wifi connection state";
	private final int typeWifiConnectionState = DataManager.STRING;

	@Override
	public void startListening() {
		mWifiManager = (WifiManager) getAppContext().getSystemService(
				Context.WIFI_SERVICE);
		mWifiBCReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				long time = TimeSource.getTimeOfDay();
				Log.i(TAG, "state change action rcvd");
				NetworkInfo netInfo = intent
						.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				State state = netInfo.getState();

				mDataManager.insertLog(labelWifiConnectionState, time,
						state.name());

				String bssid = "";
				String ssid = "";
				int ip = 0;
				int speed = 0;
				if (state == State.CONNECTED) {
					bssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
					WifiInfo wi = mWifiManager.getConnectionInfo();
					ssid = wi.getSSID();
					ip = wi.getIpAddress();
					speed = wi.getLinkSpeed();
				}

				if (bssid == null) {
					bssid = "";
				}
				if (!bssid.equals(lastBSSID)) {
					mDataManager.insertLog(labelBSSID, time, bssid);
					lastBSSID = bssid;
				}

				if (ssid == null) {
					ssid = "";
				}
				if (!ssid.equals(lastSSID)) {
					mDataManager.insertLog(labelSSID, time, ssid);
					lastSSID = ssid;
				}

				if (ip != oldIp) {
					String ipString = String.format("%d.%d.%d.%d", (ip & 0xff),
						(ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
					mDataManager.insertLog(labelIP, time, ipString);
					oldIp = ip;
				}
				mDataManager.insertLog(labelSpeed, time, speed);


			};
		};

		mThread = new HandlerThread(TAG);
		mThread.start();
		Looper looper = mThread.getLooper();
		Handler handler = new Handler(looper);
		getAppContext().registerReceiver(mWifiBCReceiver,
				new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION),
				null, handler);
		Log.d(TAG, "context: " + getAppContext().hashCode());
	}

	@Override
	public void stopListening() {
		getAppContext().unregisterReceiver(mWifiBCReceiver);
		mThread.quit();
	}

	@Override
	public void initLogTypes() {
		addLogType(labelBSSID, typeBSSID);
		addLogType(labelSSID, typeSSID);
		addLogType(labelIP, typeIP);
		addLogType(labelSpeed, typeSpeed);

		addLogType(labelWifiConnectionState, typeWifiConnectionState);
	}

	@Override
	public boolean checkPermissions() {
		if (getAppContext().checkCallingOrSelfPermission(
				"android.permission.ACCESS_WIFI_STATE") != PackageManager.PERMISSION_GRANTED) {
			return false;
		}
		return true;
	}
}
