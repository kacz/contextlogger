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
import android.provider.Settings.SettingNotFoundException;
import cz.cuni.kacz.contextlogger.ContextLoggerService;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

public class ScreenBrightnessListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private boolean running = false;
	private Timer timer;
	int period = 100;
	int oldBrightness = 0;

	// log names and types
	private String labelBrightness = "Screen brightness";
	private int typeBrightness = DataManager.INT;

	@Override
	public void startListening() {
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				long time = TimeSource.getTimeOfDay();
				int brightness = 0;
				try {
					brightness = android.provider.Settings.System.getInt(
							ContextLoggerService.mAppContext
									.getContentResolver(),
							android.provider.Settings.System.SCREEN_BRIGHTNESS);
				} catch (SettingNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (brightness != oldBrightness) {
					oldBrightness = brightness;
					mDataManager.insertLog(labelBrightness, time, brightness);
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
		addLogType(labelBrightness, typeBrightness);
	}

}
