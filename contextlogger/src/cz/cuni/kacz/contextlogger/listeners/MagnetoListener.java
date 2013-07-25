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
 * ContextListener for logging magnetic field data.
 * 
 * @author kacz
 * 
 */
public class MagnetoListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	SensorEventListener mMagnetometer = null;
	SensorManager mSensorManager = null;

	String labelX = "X magnetic field";
	int typeX = DataManager.FLOAT;
	String labelY = "Y magnetic field";
	int typeY = DataManager.FLOAT;
	String labelZ = "Z magnetic field";
	int typeZ = DataManager.FLOAT;

	@Override
	public void startListening() {
		// TODO Auto-generated method stub
		mSensorManager = (SensorManager) getAppContext().getSystemService(
				Context.SENSOR_SERVICE);
		Sensor mMagneticSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (mMagneticSensor != null) {
			mMagnetometer = new SensorEventListener() {

				@Override
				public void onSensorChanged(SensorEvent event) {
					long time = TimeSource.getTimeOfDay();
					mDataManager.insertLog(labelX, time, event.values[0]);
					mDataManager.insertLog(labelY, time, event.values[1]);
					mDataManager.insertLog(labelZ, time, event.values[2]);

				}

				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {
					// TODO Auto-generated method stub

				}
			};
			mSensorManager.registerListener(mMagnetometer,
					mMagneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}

	}

	@Override
	public void stopListening() {
		mSensorManager.unregisterListener(mMagnetometer);
	}

	@Override
	public void initLogTypes() {
		addLogType(labelX, typeX);
		addLogType(labelY, typeY);
		addLogType(labelZ, typeZ);
	}

}
