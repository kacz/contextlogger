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
import android.os.HandlerThread;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

/**
 * ContextListener for logging telephony service status information.
 * 
 * @author kacz
 * 
 */
public class TelephonyListener extends DefaultContextListener {

	private TelephonyManager mTelephonyManager;
	private PhoneStateListener mPhoneStateListener;
	private HandlerThread mThread = null;

	String labelDataActivity = "Cell data activity";
	int typeDataActivity = DataManager.INT;
	String labelDataConnectivityState = "Cell data connection state";
	int typeDataConnectivityState = DataManager.INT;
	String labelDataNetworkType = "Cell data connection type";
	int typeDataNetworkType = DataManager.INT;
	String labelCallState = "Cell call state";
	int typeCallState = DataManager.INT;

	String labelOperatorName = "Cell operator name";
	int typeOperatorName = DataManager.STRING;
	String labelOperatorId = "Cell operator ID";
	int typeOperatorId = DataManager.STRING;
	String labelCellRoaming = "Cell roaming";
	int typeCellRoaming = DataManager.INT;
	String labelCellServiceState = "Cell service state";
	int typeCellServiceState = DataManager.INT;

	String labelCdmaSignalDbm = "CDMA signal dbm";
	int typeCdmaSignalDbm = DataManager.INT;
	String labelCdmaSignalEcio = "CDMA signal ECIO";
	int typeCdmaSignalEcio = DataManager.INT;

	String labelEvdoSignalDbm = "Evdo signal dbm";
	int typeEvdoSignalDbm = DataManager.INT;
	String labelEvdoSignalEcio = "Evdo signal ECIO";
	int typeEvdoSignalEcio = DataManager.INT;
	String labelEvdoSignalSnr = "Evdo signal snr";
	int typeEvdoSignalSnr = DataManager.INT;

	String labelGsmBitErrorRate = "GSM signal error rate";
	int typeGsmBitErrorRate = DataManager.INT;
	String labelGsmSignalStrength = "GSM signal strngth";
	int typeGsmSignalStrength = DataManager.INT;

	String labelPhoneType = "Radio type";
	int typePhoneType = DataManager.INT;

	long minTime;
	float minDistance;

	public TelephonyListener() {
		minTime = 3 * 1000; // 3 sec
		minDistance = 0;
	}

	public TelephonyListener(long time, float dist) {
		minTime = time;
		minDistance = dist;
	}

	@Override
	public void startListening() {
		mTelephonyManager = (TelephonyManager) getAppContext()
				.getSystemService(Context.TELEPHONY_SERVICE);

		long time = TimeSource.getTimeOfDay();
		mDataManager.insertLog(labelDataActivity, time,
				mTelephonyManager.getDataActivity());
		mDataManager.insertLog(labelDataConnectivityState, time,
				mTelephonyManager.getDataState());
		mDataManager.insertLog(labelDataNetworkType, time,
				mTelephonyManager.getNetworkType());
		mDataManager.insertLog(labelCallState, time,
				mTelephonyManager.getCallState());

		mDataManager.insertLog(labelPhoneType, time,
				mTelephonyManager.getPhoneType());

		mPhoneStateListener = new PhoneStateListener() {


			@Override
			public void onDataActivity(int direction) {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelDataActivity, time, direction);
			}

			@Override
			public void onDataConnectionStateChanged(int state, int networkType) {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelDataConnectivityState, time, state);
				mDataManager.insertLog(labelDataNetworkType, time, networkType);
			}

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelCallState, time, state);
			}

			@Override
			public void onServiceStateChanged(ServiceState serviceState) {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelOperatorName, time,
						serviceState.getOperatorAlphaShort());
				mDataManager.insertLog(labelOperatorId, time,
						serviceState.getOperatorNumeric());
				mDataManager.insertLog(labelCellRoaming, time,
						serviceState.getRoaming() ? 1 : 0);
				mDataManager.insertLog(labelCellServiceState, time,
						serviceState.getState());
			}

			@Override
			public void onSignalStrengthsChanged(SignalStrength signalStrength) {
				long time = TimeSource.getTimeOfDay();
				mDataManager.insertLog(labelCdmaSignalDbm, time,
						signalStrength.getCdmaDbm());
				mDataManager.insertLog(labelCdmaSignalEcio, time,
						signalStrength.getCdmaEcio());

				mDataManager.insertLog(labelEvdoSignalDbm, time,
						signalStrength.getEvdoDbm());
				mDataManager.insertLog(labelEvdoSignalEcio, time,
						signalStrength.getEvdoEcio());
				mDataManager.insertLog(labelEvdoSignalSnr, time,
						signalStrength.getEvdoSnr());

				mDataManager.insertLog(labelGsmBitErrorRate, time,
						signalStrength.getGsmBitErrorRate());
				mDataManager.insertLog(labelGsmSignalStrength, time,
						signalStrength.getGsmSignalStrength());

				mDataManager.insertLog(labelPhoneType, time,
						mTelephonyManager.getPhoneType());
			}

		};

		mThread = new HandlerThread(TAG);
		mThread.start();
		Looper looper = mThread.getLooper();

		mTelephonyManager.listen(mPhoneStateListener,
				PhoneStateListener.LISTEN_DATA_ACTIVITY
						| PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
						| PhoneStateListener.LISTEN_CALL_STATE
						| PhoneStateListener.LISTEN_SERVICE_STATE
						| PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}

	@Override
	public void stopListening() {
		mTelephonyManager.listen(mPhoneStateListener,
				PhoneStateListener.LISTEN_NONE);
	}

	@Override
	public void initLogTypes() {
		addLogType(labelDataActivity, typeDataActivity);
		addLogType(labelDataConnectivityState, typeDataConnectivityState);
		addLogType(labelDataNetworkType, typeDataNetworkType);
		addLogType(labelCallState, typeCallState);

		addLogType(labelOperatorName, typeOperatorName);
		addLogType(labelOperatorId, typeOperatorId);
		addLogType(labelCellRoaming, typeCellRoaming);
		addLogType(labelCellServiceState, typeCellServiceState);

		addLogType(labelCdmaSignalDbm, typeCdmaSignalDbm);
		addLogType(labelCdmaSignalEcio, typeCdmaSignalEcio);

		addLogType(labelEvdoSignalDbm, typeEvdoSignalDbm);
		addLogType(labelEvdoSignalEcio, typeEvdoSignalEcio);
		addLogType(labelEvdoSignalSnr, typeEvdoSignalSnr);

		addLogType(labelGsmBitErrorRate, typeGsmBitErrorRate);
		addLogType(labelGsmSignalStrength, typeGsmSignalStrength);

		addLogType(labelPhoneType, typePhoneType);
	}

	@Override
	public boolean checkPermissions() {
		if (getAppContext().checkCallingOrSelfPermission(
				"android.permission.READ_PHONE_STATE") != PackageManager.PERMISSION_GRANTED) {
			return false;
		}
		return true;
	}
}
