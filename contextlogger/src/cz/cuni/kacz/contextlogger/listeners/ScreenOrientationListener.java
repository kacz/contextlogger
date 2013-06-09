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

import android.content.Context;
import android.view.Display;
import android.view.WindowManager;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

public class ScreenOrientationListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final boolean running = false;
	private Timer timer;
	int period = 100;
	int oldOrientation = 0;
	long lastChangeTime = 0;

	WindowManager mWindowManager;
	Display mDisplay;

	// log names and types
	private final String labelOrientation = "Screen orientation";
	private final int typeOrientation = DataManager.INT;
	private final String labelLastOrientation = "Last screen orientation";
	private final int typeLastOrientation = DataManager.INT;
	private final String labelTimeSinceOrientationChange = "Time since orientation change";
	private final int typeTimeSinceOrientationChange = DataManager.DOUBLE;

	@Override
	public void startListening() {
		mWindowManager = (WindowManager) getAppContext().getSystemService(
				Context.WINDOW_SERVICE);
		mDisplay = mWindowManager.getDefaultDisplay();

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				long time = TimeSource.getTimeOfDay();
				int orientation = -1;

				orientation = mDisplay.getRotation();

				if (orientation != oldOrientation) {
					lastChangeTime = time;
					mDataManager.insertLog(labelOrientation, time, orientation);
					mDataManager.insertLog(labelLastOrientation, time,
							oldOrientation);
					oldOrientation = orientation;
				}

				mDataManager.insertLog(labelTimeSinceOrientationChange, time,
						(double) (time - lastChangeTime) / 1000000);
			}
		}, 0, period);
	}

	@Override
	public void stopListening() {
		timer.cancel();
	}

	@Override
	public void initLogTypes() {
		addLogType(labelOrientation, typeOrientation);
		addLogType(labelLastOrientation, typeLastOrientation);
		addLogType(labelTimeSinceOrientationChange,
				typeTimeSinceOrientationChange);
	}

}
