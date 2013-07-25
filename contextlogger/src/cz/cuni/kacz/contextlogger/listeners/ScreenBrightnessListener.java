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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Timer;
import java.util.TimerTask;

import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

/**
 * ContextListener for logging screen brightness information.
 * 
 * @author kacz
 * 
 */
public class ScreenBrightnessListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final boolean running = false;
	private Timer timer;
	int period = 100;
	int oldBrightness = -1;
	int oldButtonBrightness = -1;

	RandomAccessFile mScreenReader;
	RandomAccessFile mButtonReader;

	// android 2.x
	private static final String LCD_COOPER_7 = "/sys/class/leds/lcd-backlight/brightness";
	private static final String BUTTONS_COOPER_7 = "/sys/class/leds/button-backlight/brightness";

	// android 4.x
	private static final String LCD_NEXUS_7 = "/sys/class/backlight/pwm-backlight/brightness";

	private static final String LCD_S4 = "/sys/class/backlight/panel/brightness";

	// log names and types
	private final String labelBrightness = "Screen brightness";
	private final int typeBrightness = DataManager.INT;

	private final String labelButtonBrightness = "Button brightness";
	private final int typeButtonBrightness = DataManager.INT;

	@Override
	public void startListening() {
		if (mScreenReader == null && mButtonReader == null) {
			return;
		}

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			/*
			 * public void run() { long time = TimeSource.getTimeOfDay(); int
			 * brightness = 0; try { brightness =
			 * android.provider.Settings.System.getInt(
			 * getAppContext().getContentResolver(),
			 * android.provider.Settings.System.SCREEN_BRIGHTNESS); } catch
			 * (SettingNotFoundException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); } Log.d(TAG, "bright: " + brightness); if
			 * (brightness != oldBrightness) { oldBrightness = brightness;
			 * mDataManager.insertLog(labelBrightness, time, brightness); } }
			 */
			public void run() {
				long time = TimeSource.getTimeOfDay();

				if (mScreenReader != null) {
				try {
					mScreenReader.seek(0);
					String brightness = mScreenReader.readLine();

					int iBrightness = Integer.parseInt(brightness);
					if (iBrightness != oldBrightness) {
						mDataManager.insertLog(labelBrightness, time,
								iBrightness);
						oldBrightness = iBrightness;
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				}

				if (mButtonReader != null) {
				try {
					mButtonReader.seek(0);
					String buttonBrightness = mButtonReader.readLine();

					int iButtonBrightness = Integer.parseInt(buttonBrightness);
					if (iButtonBrightness != oldButtonBrightness) {
						mDataManager.insertLog(labelButtonBrightness, time,
								iButtonBrightness);
						oldButtonBrightness = iButtonBrightness;
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
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
		// screen
		try {
			if (new File(LCD_COOPER_7).exists()) {
				mScreenReader = new RandomAccessFile(LCD_COOPER_7, "r");
			} else if (new File(LCD_S4).exists()) {
				mScreenReader = new RandomAccessFile(LCD_S4, "r");
			} else if (new File(LCD_NEXUS_7).exists()) {
				mScreenReader = new RandomAccessFile(LCD_NEXUS_7, "r");
			}
		} catch (FileNotFoundException e) {
		}

		addLogType(labelBrightness, typeBrightness);

		// buttons
		try {
			if (new File(BUTTONS_COOPER_7).exists()) {
				mButtonReader = new RandomAccessFile(BUTTONS_COOPER_7, "r");
			}

		} catch (FileNotFoundException e) {
		}

		addLogType(labelButtonBrightness, typeButtonBrightness);
	}

}
