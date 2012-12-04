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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import cz.cuni.kacz.contextlogger.ContextLoggerService;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

public class AcceleraionListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	SensorEventListener mAccelerometer = null;
	SensorManager mSensorManager = null;

	String labelX = "X acceleration";
	int typeX = DataManager.FLOAT;
	String labelY = "Y acceleration";
	int typeY = DataManager.FLOAT;
	String labelZ = "Z acceleration";
	int typeZ = DataManager.FLOAT;

	@Override
	public void startListening() {
		// TODO Auto-generated method stub
		mSensorManager = (SensorManager) ContextLoggerService.mAppContext
				.getSystemService(Context.SENSOR_SERVICE);
		Sensor mAccelerationSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (mAccelerationSensor != null) {
			mAccelerometer = new SensorEventListener() {

				public void onSensorChanged(SensorEvent event) {
					long time = TimeSource.getTimeOfDay();
					mDataManager.insertLog(labelX, time, event.values[0]);
					mDataManager.insertLog(labelY, time, event.values[1]);
					mDataManager.insertLog(labelZ, time, event.values[2]);
				}

				public void onAccuracyChanged(Sensor sensor, int accuracy) {
					// TODO Auto-generated method stub

				}
			};
			mSensorManager.registerListener(mAccelerometer,
					mAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}

	}

	@Override
	public void stopListening() {
		mSensorManager.unregisterListener(mAccelerometer);
	}

	@Override
	public void initLogTypes() {
		mDataManager.registerListener(labelX, typeX);
		mDataManager.registerListener(labelY, typeY);
		mDataManager.registerListener(labelZ, typeZ);
	}

}
