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
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

public class BatteryListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String TAG = "WifiRSSIListener";

	private BroadcastReceiver mBatteryBCReceiver = null;
	private HandlerThread mThread = null;

	// log names and types
	private final String labelHealth = "Battery health";
	private final int typeHealth = DataManager.INT;

	private final String labelLevel = "Battery level";
	private final int typeLevel = DataManager.INT;

	private final String labelPlugged = "Battery plugged";
	private final int typePlugged = DataManager.INT;

	private final String labelBatteryPresent = "Battery present";
	private final int typeBatteryPresent = DataManager.INT;

	private final String labelBatteryStatus = "Battery status";
	private final int typeBatteryStatus = DataManager.INT;

	private final String labelTemperature = "Battery temperature";
	private final int typeTemperature = DataManager.INT;

	private final String labelVoltage = "Battery voltage";
	private final int typeVoltage = DataManager.INT;

	@Override
	public void startListening() {
		mBatteryBCReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				long time = TimeSource.getTimeOfDay();

				mDataManager.insertLog(labelHealth, time,
						intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0));
				mDataManager.insertLog(labelLevel, time,
						intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0));
				mDataManager.insertLog(labelPlugged, time,
						intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));
				mDataManager.insertLog(labelBatteryPresent, time,
						intent.getIntExtra(BatteryManager.EXTRA_PRESENT, 0));
				mDataManager.insertLog(labelBatteryStatus, time,
						intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0));
				mDataManager
						.insertLog(labelTemperature, time, intent.getIntExtra(
								BatteryManager.EXTRA_TEMPERATURE, 0));
				mDataManager.insertLog(labelVoltage, time,
						intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0));
			};
		};

		mThread = new HandlerThread(TAG);
		mThread.start();
		Looper looper = mThread.getLooper();
		Handler handler = new Handler(looper);
		getAppContext().registerReceiver(mBatteryBCReceiver,
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED), null,
				handler);
	}

	@Override
	public void stopListening() {
		getAppContext().unregisterReceiver(mBatteryBCReceiver);
		mThread.quit();
	}

	@Override
	public void initLogTypes() {
		addLogType(labelHealth, typeHealth);
		addLogType(labelLevel, typeLevel);
		addLogType(labelPlugged, typePlugged);
		addLogType(labelBatteryPresent, typeBatteryPresent);
		addLogType(labelBatteryStatus, typeBatteryStatus);
		addLogType(labelTemperature, typeTemperature);
		addLogType(labelVoltage, typeVoltage);
	}
}
