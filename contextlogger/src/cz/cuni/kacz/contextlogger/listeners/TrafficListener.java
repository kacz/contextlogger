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

import java.util.Timer;
import java.util.TimerTask;

import android.net.TrafficStats;
import android.os.Process;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import cz.cuni.kacz.contextlogger.ContextLogger;
import cz.cuni.kacz.contextlogger.ContextLoggerService;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

public class TrafficListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private boolean running = false;
	private Timer timer;
	int period = 100;

	// log names and types
	private String labelTotalSent = "Total sent traffic";
	private int typeTotalSent = DataManager.LONG;
	private String labelTotalRcvd = "Total received traffic";
	private int typeTotalRcvd = DataManager.LONG;
	private String labelMySent = "My sent traffic";
	private int typeMySent = DataManager.LONG;
	private String labelMyRcvd = "My received traffic";
	private int typeMyRcvd = DataManager.LONG;

	@Override
	public void startListening() {
		final int myUid = Process.myUid();
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelTotalSent, time,
						TrafficStats.getTotalTxBytes());
				mDataManager.insertLog(labelTotalRcvd, time,
						TrafficStats.getTotalRxBytes());
				mDataManager.insertLog(labelMySent, time,
						TrafficStats.getUidTxBytes(myUid));
				mDataManager.insertLog(labelMyRcvd, time,
						TrafficStats.getUidRxBytes(myUid));
			}
		}, 0, period);
	}

	@Override
	public void stopListening() {
		timer.cancel();
	}

	@Override
	public void initLogTypes() {
		addLogType(labelTotalSent, typeTotalSent);
		addLogType(labelTotalRcvd, typeTotalRcvd);
		addLogType(labelMySent, typeMySent);
		addLogType(labelMyRcvd, typeMyRcvd);
	}

}
