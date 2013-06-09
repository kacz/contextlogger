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
import cz.cuni.kacz.contextlogger.ContextLoggerService;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

public class PassiveLocationListener extends DefaultContextListener {

	private LocationManager mLocManager;
	private LocationListener mLocListener;
	private HandlerThread mThread = null;

	String labelPassiveLat = "Passive latitude";
	int typePassiveLat = DataManager.DOUBLE;
	String labelPassiveLong = "Passive longitude";
	int typePassiveLong = DataManager.DOUBLE;

	@Override
	public void startListening() {
		mLocManager = (LocationManager) ContextLoggerService.mAppContext
				.getSystemService(Context.LOCATION_SERVICE);
		mLocListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelPassiveLat, time,
						location.getLatitude());
				mDataManager.insertLog(labelPassiveLong, time,
						location.getLongitude());
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub

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

		mLocManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0,
				0, mLocListener, looper);

	}

	@Override
	public void stopListening() {
		mLocManager.removeUpdates(mLocListener);
	}

	@Override
	public void initLogTypes() {
		addLogType(labelPassiveLat, typePassiveLat);
		addLogType(labelPassiveLong, typePassiveLong);
	}

	@Override
	public boolean checkPermissions() {
		if (ContextLoggerService.mAppContext
				.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
			return false;
		}
		return true;
	}

}
