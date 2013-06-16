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

import android.annotation.SuppressLint;
import android.net.TrafficStats;
import android.os.Process;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

public class TrafficSpeedListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final int API_VERSION = android.os.Build.VERSION.SDK_INT;
	private final boolean running = false;
	private Timer timer;
	int period = 100;

	long oldTotalTxBytes;
	long oldTotalRxBytes;
	long oldUidTxBytes;
	long oldUidRxBytes;
	long oldTotalTxPackets;
	long oldTotalRxPackets;
	long oldUidTxPackets;
	long oldUidRxPackets;
	
	// log names and types
	private final String labelTotalSent = "Total sent traffic speed";
	private final int typeTotalSent = DataManager.LONG;
	private final String labelTotalRcvd = "Total received traffic speed";
	private final int typeTotalRcvd = DataManager.LONG;
	private final String labelMySent = "My sent traffic speed";
	private final int typeMySent = DataManager.LONG;
	private final String labelMyRcvd = "My received traffic speed";
	private final int typeMyRcvd = DataManager.LONG;

	private final String labelTotalSentPackets = "Total sent packets speed";
	private final int typeTotalSentPackets = DataManager.LONG;
	private final String labelTotalRcvdPackets = "Total received packets speed";
	private final int typeTotalRcvdPackets = DataManager.LONG;
	private final String labelMySentPackets = "My sent packets speed";
	private final int typeMySentPackets = DataManager.LONG;
	private final String labelMyRcvdPackets = "My received packets speed";
	private final int typeMyRcvdPackets = DataManager.LONG;

	@SuppressLint("NewApi")
	@Override
	public void startListening() {
		final int myUid = Process.myUid();
		timer = new Timer();
		
		oldTotalTxBytes = TrafficStats.getTotalTxBytes();
		oldTotalRxBytes = TrafficStats.getTotalRxBytes();
		oldUidTxBytes = TrafficStats.getUidTxBytes(myUid);
		oldUidRxBytes = TrafficStats.getUidRxBytes(myUid);
		oldTotalTxPackets = TrafficStats.getTotalTxPackets();
		oldTotalRxPackets = TrafficStats.getTotalRxPackets();
		if (API_VERSION >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
			oldUidTxPackets = TrafficStats.getUidTxPackets(myUid);
			oldUidRxPackets = TrafficStats.getUidRxPackets(myUid);
		}
		
		timer.scheduleAtFixedRate(new TimerTask() {
			@SuppressLint("NewApi")
			@Override
			public void run() {
				long time = TimeSource.getTimeOfDay();
				
				long tmp = TrafficStats.getTotalTxBytes();
				mDataManager.insertLog(labelTotalSent, time,
						tmp - oldTotalTxBytes);
				oldTotalTxBytes = tmp;
				
				tmp = TrafficStats.getTotalRxBytes();
				mDataManager.insertLog(labelTotalRcvd, time,
						tmp - oldTotalRxBytes);
				oldTotalRxBytes = tmp;
				
				tmp = TrafficStats.getUidTxBytes(myUid);
				mDataManager.insertLog(labelMySent, time,
						tmp - oldUidTxBytes);
				oldUidTxBytes = tmp;
				
				tmp = TrafficStats.getUidRxBytes(myUid);
				mDataManager.insertLog(labelMyRcvd, time,
						tmp - oldUidRxBytes);
				oldUidRxBytes = tmp;
				
				tmp = TrafficStats.getTotalTxPackets();
				mDataManager.insertLog(labelTotalSentPackets, time,
						tmp - oldTotalTxPackets);
				oldTotalTxPackets = tmp;
				
				tmp = TrafficStats.getTotalRxPackets();
				mDataManager.insertLog(labelTotalRcvdPackets, time,
						tmp - oldTotalRxPackets);
				oldTotalRxPackets = tmp;
				
				if (API_VERSION >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
					tmp = TrafficStats.getUidTxPackets(myUid);
					mDataManager.insertLog(labelMySentPackets, time,
							tmp - oldUidTxPackets);
					oldUidTxPackets = tmp;
					
					tmp = TrafficStats.getUidRxPackets(myUid);
					mDataManager.insertLog(labelMyRcvdPackets, time,
							tmp - oldUidRxPackets);
					oldUidRxPackets = tmp;
				}

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

		addLogType(labelTotalSentPackets, typeTotalSentPackets);
		addLogType(labelTotalRcvdPackets, typeTotalRcvdPackets);
		if (API_VERSION >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
			addLogType(labelMySentPackets, typeMySentPackets);
			addLogType(labelMyRcvdPackets, typeMyRcvdPackets);
		}
	}

}
