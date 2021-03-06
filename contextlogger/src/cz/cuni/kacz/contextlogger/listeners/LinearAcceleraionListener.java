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
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

/**
 * ContextListener for logging linear acceleration data.
 * 
 * @author kacz
 * 
 */
public class LinearAcceleraionListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	SensorEventListener mAccelerometer = null;
	SensorManager mSensorManager = null;

	String labelX = "X linear acceleration";
	int typeX = DataManager.FLOAT;
	String labelY = "Y linear acceleration";
	int typeY = DataManager.FLOAT;
	String labelZ = "Z linear acceleration";
	int typeZ = DataManager.FLOAT;
	String labelSum = "Overall linear acceleration";
	int typeSum = DataManager.DOUBLE;

	@Override
	public void startListening() {
		// TODO Auto-generated method stub
		mSensorManager = (SensorManager) getAppContext().getSystemService(
				Context.SENSOR_SERVICE);
		Sensor mAccelerationSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		if (mAccelerationSensor != null) {
			mAccelerometer = new SensorEventListener() {

				@Override
				public void onSensorChanged(SensorEvent event) {
					long time = TimeSource.getTimeOfDay();
					mDataManager.insertLog(labelX, time, event.values[0]);
					mDataManager.insertLog(labelY, time, event.values[1]);
					mDataManager.insertLog(labelZ, time, event.values[2]);
					mDataManager.insertLog(
							labelSum,
							time,
							Math.sqrt(Math.pow(event.values[0], 2)
									+ Math.pow(event.values[1], 2)
									+ Math.pow(event.values[2], 2)));
				}

				@Override
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
		addLogType(labelX, typeX);
		addLogType(labelY, typeY);
		addLogType(labelZ, typeZ);
		addLogType(labelSum, typeSum);
	}

}
