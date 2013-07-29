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

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;
import cz.cuni.kacz.contextlogger.listeners.DefaultContextListener;

/**
 * Simple contextlistener implemented in the calling application
 * 
 * @author kacz
 * 
 */
public class TestListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// private static final String TAG = "DummyListener";
	private final boolean running = false;
	private Timer timer;
	int n = 0;
	int period = 1000;

	// log names and types
	private final String labelDummy = "CLDemo listener";
	private final int typeDummy = DataManager.INT;

	public TestListener(int period) {
		this.period = period;
	}

	@Override
	public void startListening() {
		Log.d(TAG, "startlogging called");
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
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
