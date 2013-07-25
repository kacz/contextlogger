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
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

/**
 * ContextListener for logging GPS location data.
 * 
 * @author kacz
 * 
 */
public class GpsLocationListener extends DefaultContextListener {

	private LocationManager mLocManager;
	private LocationListener mLocListener;
	private HandlerThread mThread = null;

	String labelGpsProviderStatus = "Gps status";
	int typeGpsProviderStatus = DataManager.INT;

	String labelGpsLat = "Gps latitude";
	int typeGpsLat = DataManager.DOUBLE;
	String labelGpsLong = "Gps longitude";
	int typeGpsLong = DataManager.DOUBLE;
	String labelGpsAltitude = "Gps altitude";
	int typeGpsAltitude = DataManager.DOUBLE;

	String labelGpsBearing = "Gps altitude";
	int typeGpsBearing = DataManager.DOUBLE;
	String labelGpsSpeed = "Gps speed (mps)";
	int typeGpsSpeed = DataManager.FLOAT;
	String labelGpsSpeedKph = "Gps speed (kph)";
	int typeGpsSpeedKph = DataManager.FLOAT;
	String labelGpsAccuracy = "Gps accuracy";
	int typeGpsAccuracy = DataManager.FLOAT;

	String labelGpsUsedSats = "Gps sats used";
	int typeGpsUsedSats = DataManager.INT;

	long minTime;
	float minDistance;

	public GpsLocationListener() {
		minTime = 3 * 1000; // 3 sec
		minDistance = 0;
	}

	public GpsLocationListener(long time, float dist) {
		minTime = time;
		minDistance = dist;
	}

	@Override
	public void startListening() {
		mLocManager = (LocationManager) getAppContext().getSystemService(
				Context.LOCATION_SERVICE);
		mLocListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelGpsLat, time,
						location.getLatitude());
				mDataManager.insertLog(labelGpsLong, time,
						location.getLongitude());
				mDataManager.insertLog(labelGpsAltitude, time,
						location.getAltitude());
				mDataManager.insertLog(labelGpsBearing, time,
						location.getBearing());
				mDataManager
						.insertLog(labelGpsSpeed, time, location.getSpeed());
				mDataManager.insertLog(labelGpsSpeedKph, time,
						(float) (location.getSpeed() * 3.6));
				mDataManager.insertLog(labelGpsAccuracy, time,
						location.getAccuracy());
				Bundle extras = location.getExtras();
				for (String key : extras.keySet()) {
					Log.d(TAG, "extrakey: " + key);
				}
				mDataManager.insertLog(labelGpsUsedSats, time,
						extras.getInt("satellites", -1));
			}

			@Override
			public void onProviderDisabled(String provider) {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelGpsProviderStatus, time, 0);
			}

			@Override
			public void onProviderEnabled(String provider) {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelGpsProviderStatus, time, 1);
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub

			}

		};

		mThread = new HandlerThread(TAG);
		mThread.start();
		Looper looper = mThread.getLooper();

		mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				minTime, minDistance, mLocListener, looper);

	}

	@Override
	public void stopListening() {
		mLocManager.removeUpdates(mLocListener);
	}

	@Override
	public void initLogTypes() {
		addLogType(labelGpsProviderStatus,
				typeGpsProviderStatus);
		addLogType(labelGpsLat, typeGpsLat);
		addLogType(labelGpsLong, typeGpsLong);
		addLogType(labelGpsAltitude, typeGpsAltitude);

		addLogType(labelGpsBearing, typeGpsBearing);
		addLogType(labelGpsSpeed, typeGpsSpeed);
		addLogType(labelGpsSpeedKph, typeGpsSpeedKph);
		addLogType(labelGpsAccuracy, typeGpsAccuracy);

		addLogType(labelGpsUsedSats, typeGpsUsedSats);
	}

	@Override
	public boolean checkPermissions() {
		if (getAppContext().checkCallingOrSelfPermission(
				"android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
			return false;
		}
		return true;
	}
}
