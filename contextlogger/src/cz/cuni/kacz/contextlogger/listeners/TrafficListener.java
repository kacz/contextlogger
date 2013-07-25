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

/**
 * ContextListener for logging network traffic information.
 * 
 * @author kacz
 * 
 */
public class TrafficListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final int API_VERSION = android.os.Build.VERSION.SDK_INT;
	private final boolean running = false;
	private Timer timer;
	int period = 100;

	// log names and types
	private final String labelTotalSent = "Total sent traffic";
	private final int typeTotalSent = DataManager.LONG;
	private final String labelTotalRcvd = "Total received traffic";
	private final int typeTotalRcvd = DataManager.LONG;
	private final String labelMySent = "My sent traffic";
	private final int typeMySent = DataManager.LONG;
	private final String labelMyRcvd = "My received traffic";
	private final int typeMyRcvd = DataManager.LONG;

	private final String labelTotalSentPackets = "Total sent packets";
	private final int typeTotalSentPackets = DataManager.LONG;
	private final String labelTotalRcvdPackets = "Total received packets";
	private final int typeTotalRcvdPackets = DataManager.LONG;
	private final String labelMySentPackets = "My sent packets";
	private final int typeMySentPackets = DataManager.LONG;
	private final String labelMyRcvdPackets = "My received packets";
	private final int typeMyRcvdPackets = DataManager.LONG;

	@Override
	public void startListening() {
		final int myUid = Process.myUid();
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@SuppressLint("NewApi")
			@Override
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

				mDataManager.insertLog(labelTotalSentPackets, time,
						TrafficStats.getTotalTxPackets());
				mDataManager.insertLog(labelTotalRcvdPackets, time,
						TrafficStats.getTotalRxPackets());
				if (API_VERSION >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
					mDataManager.insertLog(labelMySentPackets, time,
							TrafficStats.getUidTxPackets(myUid));
					mDataManager.insertLog(labelMyRcvdPackets, time,
							TrafficStats.getUidRxPackets(myUid));
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
