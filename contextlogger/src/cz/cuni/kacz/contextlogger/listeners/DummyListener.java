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

import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

import android.util.Log;

public class DummyListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// private static final String TAG = "DummyListener";
	private boolean running = false;
	private Timer timer;
	int n = 0;
	int period = 100;

	// log names and types
	private String labelDummy = "Dummy";
	private int typeDummy = DataManager.INT;

	public DummyListener(int period) {
		this.period = period;
	}

	@Override
	public void startListening() {
		Log.d(TAG, "startlogging called");
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelDummy, time, n++);
			}
		}, 0, period);
	}

	@Override
	public void stopListening() {
		Log.d(TAG, "stoplogging called");
		timer.cancel();
	}

	@Override
	public void initLogTypes() {
		addLogType(labelDummy, typeDummy);
	}
}
