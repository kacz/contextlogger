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

public class LightListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	SensorEventListener mLightEventListener = null;
	SensorManager mSensorManager = null;

	String labelLight = "Light";
	int typeLight = DataManager.FLOAT;

	@Override
	public void startListening() {
		// TODO Auto-generated method stub
		mSensorManager = (SensorManager) getAppContext().getSystemService(
				Context.SENSOR_SERVICE);
		Sensor mLightSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_LIGHT);
		if (mLightSensor != null) {
			mLightEventListener = new SensorEventListener() {

				@Override
				public void onSensorChanged(SensorEvent event) {
					long time = TimeSource.getTimeOfDay();
					mDataManager.insertLog(labelLight, time,
							event.values[0]);
				}

				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {
					// TODO Auto-generated method stub

				}
			};
			mSensorManager.registerListener(mLightEventListener, mLightSensor,
					SensorManager.SENSOR_DELAY_NORMAL);
		}

	}

	@Override
	public void stopListening() {
		mSensorManager.unregisterListener(mLightEventListener);
	}

	@Override
	public void initLogTypes() {
		addLogType(labelLight, typeLight);
	}

}
