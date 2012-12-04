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

import cz.cuni.kacz.contextlogger.ContextLoggerService;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.util.Log;

public class GpsStatusListener extends DefaultContextListener {

	GpsStatus.Listener mGpsStatusListener;
	LocationManager mLocManager;
	GpsStatus mGpsStatus = null;

	String labelGpsStatus = "GPS status";
	int typeGpsStatus = DataManager.INT;
	String labelGpsPrns = "GPS prns";
	int typeGpsPrns = DataManager.STRING;

	@Override
	public void startListening() {
		mLocManager = (LocationManager) ContextLoggerService.mAppContext
				.getSystemService(Context.LOCATION_SERVICE);
		mGpsStatusListener = new GpsStatus.Listener() {

			public void onGpsStatusChanged(int event) {
				long time = TimeSource.getTimeOfDay();
				Log.d(TAG, "" + event);
				switch (event) {
				case GpsStatus.GPS_EVENT_STARTED:
					mDataManager.insertLog(labelGpsStatus, time, 1);
					break;
				case GpsStatus.GPS_EVENT_STOPPED:
					mDataManager.insertLog(labelGpsStatus, time, 0);
					break;
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
					if (mGpsStatus == null) {
						mGpsStatus = mLocManager.getGpsStatus(null);
					} else {
						mLocManager.getGpsStatus(mGpsStatus);
					}
					Iterable<GpsSatellite> sats = mGpsStatus.getSatellites();
					StringBuffer prns = new StringBuffer("");
					for (GpsSatellite sat : sats) {
						prns.append(sat.getPrn()).append(',');
					}
					mDataManager.insertLog(labelGpsPrns, time, prns.toString());
					break;
				}

			}
		};

		mLocManager.addGpsStatusListener(mGpsStatusListener);

	}

	@Override
	public void stopListening() {
		mLocManager.removeGpsStatusListener(mGpsStatusListener);
	}

	@Override
	public void initLogTypes() {
		mDataManager.registerListener(labelGpsStatus, typeGpsStatus);
		mDataManager.registerListener(labelGpsPrns, typeGpsPrns);
	}

	@Override
	public boolean checkPermissions() {
		if (ContextLoggerService.mAppContext
				.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED)
			return false;
		return true;
	}

}
