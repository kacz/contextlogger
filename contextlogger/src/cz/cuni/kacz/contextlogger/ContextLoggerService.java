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

package cz.cuni.kacz.contextlogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;
import cz.cuni.kacz.contextlogger.listeners.ContextListener;

public class ContextLoggerService extends Service {

	static final String TAG = "ContextLoggerService";
	static final int MSG_INIT_LISTENERS = 1;
	static final int MSG_START_LOGGING = 2;
	static final int MSG_STOP_LOGGING = 3;

	public static Context mAppContext = null;
	public String ACTION = "START_LOGGING";

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	private List<ContextListener> mListeners = null;

	static {
		System.loadLibrary("timesource");
	}

	DataManager mDataManager = null;

	@Override
	public void onCreate() {
		Log.i(TAG, "onCreate");
		mAppContext = getApplicationContext();
		mDataManager = new DataManager();
		// mDataManager
		// .addDataTarget(new IntentDataTarget(getApplicationContext()));
		// mDataManager.addDataTarget(new
		// FileDataTarget(getApplicationContext(),
		// "probaxx"));
		// mDataManager.addDataTarget(new TextFileDataTarget(
		// getApplicationContext(), "probaxx"));
		Log.i(TAG, "onCreate ready");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		// startLogging();
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind");
		return mMessenger.getBinder();
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_INIT_LISTENERS:
				Log.i(TAG, "initListeners MSG rcvd");
				initTargets(msg.getData());
				initListeners((ArrayList<ContextListener>) msg.getData()
						.getSerializable("listeners"));
				break;
			case MSG_START_LOGGING:
				Log.i(TAG, "start MSG rcvd");
				startLogging();
				break;
			case MSG_STOP_LOGGING:
				Log.i(TAG, "stop MSG rcvd");
				stopLogging();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void startLogging() {
		Log.d(TAG, "startlogging");

		timerTest();

		// start the listeners
		for (ContextListener l : mListeners) {
			l.startListening();
		}
	}

	private void timerTest() {
		// test timers
		long[] direct = new long[10];
		direct[0] = System.nanoTime();
		direct[1] = System.nanoTime();
		direct[2] = System.nanoTime();
		direct[3] = System.nanoTime();
		direct[4] = System.nanoTime();
		direct[5] = System.nanoTime();
		direct[6] = System.nanoTime();
		direct[7] = System.nanoTime();
		direct[8] = System.nanoTime();
		direct[9] = System.nanoTime();

		long[] indirect = new long[10];
		indirect[0] = TimeSource.nanoTime();
		indirect[1] = TimeSource.nanoTime();
		indirect[2] = TimeSource.nanoTime();
		indirect[3] = TimeSource.nanoTime();
		indirect[4] = TimeSource.nanoTime();
		indirect[5] = TimeSource.nanoTime();
		indirect[6] = TimeSource.nanoTime();
		indirect[7] = TimeSource.nanoTime();
		indirect[8] = TimeSource.nanoTime();
		indirect[9] = TimeSource.nanoTime();

		long[] nat = new long[10];
		nat[0] = TimeSource.getTimeOfDay();
		nat[1] = TimeSource.getTimeOfDay();
		nat[2] = TimeSource.getTimeOfDay();
		nat[3] = TimeSource.getTimeOfDay();
		nat[4] = TimeSource.getTimeOfDay();
		nat[5] = TimeSource.getTimeOfDay();
		nat[6] = TimeSource.getTimeOfDay();
		nat[7] = TimeSource.getTimeOfDay();
		nat[8] = TimeSource.getTimeOfDay();
		nat[9] = TimeSource.getTimeOfDay();

		long min, max, avg, diff;

		Log.d(TAG, "direct:");
		min = Long.MAX_VALUE;
		max = Long.MIN_VALUE;
		avg = 0;
		for (int i = 1; i < 10; ++i) {
			Log.d(TAG, String.valueOf(direct[i]));
			if (i != 0) {
				diff = direct[i] - direct[i - 1];
				if (diff < min) {
					min = diff;
				}
				if (diff > max) {
					max = diff;
				}
				avg += diff;
			}
		}
		avg /= 9;
		Log.d(TAG, "min:" + min + " max:" + max + " avg:" + avg);

		Log.d(TAG, "indirect:");
		min = Long.MAX_VALUE;
		max = Long.MIN_VALUE;
		avg = 0;
		for (int i = 1; i < 10; ++i) {
			Log.d(TAG, String.valueOf(indirect[i]));
			if (i != 0) {
				diff = indirect[i] - indirect[i - 1];
				if (diff < min) {
					min = diff;
				}
				if (diff > max) {
					max = diff;
				}
				avg += diff;
			}
		}
		avg /= 9;
		Log.d(TAG, "min:" + min + " max:" + max + " avg:" + avg);

		Log.d(TAG, "native:");
		min = Long.MAX_VALUE;
		max = Long.MIN_VALUE;
		avg = 0;
		for (int i = 1; i < 10; ++i) {
			Log.d(TAG, String.valueOf(nat[i]));
			if (i != 0) {
				diff = nat[i] - nat[i - 1];
				if (diff < min) {
					min = diff;
				}
				if (diff > max) {
					max = diff;
				}
				avg += diff;
			}
		}
		avg /= 9;
		Log.d(TAG, "min:" + min + " max:" + max + " avg:" + avg);
	}

	private void initTargets(Bundle msg) {
		Log.d(TAG, "initTargets");

		String fileName = msg.getString("fileName");
		if (fileName == null) {
			fileName = "default-"
					+ new SimpleDateFormat("-yyMMdd-hhmmss").format(new Date());
		}
		// add the file target
		DataTarget dt = new FileDataTarget(fileName);
		dt.initCtx(getApplicationContext());
		mDataManager.addDataTarget(dt);

		if (msg.getBoolean("useTextFileDataTarget")) {
			dt = new TextFileDataTarget(fileName);
			dt.initCtx(getApplicationContext());
			mDataManager.addDataTarget(dt);
		}
		if (msg.getBoolean("useIntentDataTarget")) {
			Log.d(TAG, "intent setting received");
			dt = new IntentDataTarget();
			dt.initCtx(getApplicationContext());
			mDataManager.addDataTarget(dt);
		}

		// for (DataTarget dt : targets) {
		// dt.initCtx(getApplicationContext());
		// mDataManager.addDataTarget(dt);
		// }
		// DataTarget dt = new IntentDataTarget();
		// dt.initCtx(getApplicationContext());
		// mDataManager.addDataTarget(dt);
		// dt = new FileDataTarget("/mnt/sdcard/Download/akarmi");
		// dt.initCtx(getApplicationContext());
		// mDataManager.addDataTarget(dt);
	}

	private void initListeners(List<ContextListener> listeners) {
		Log.d(TAG, "initListeners");

		// register the listeners
		mListeners = new ArrayList<ContextListener>(listeners.size());

		for (ContextListener l : listeners) {
			if (l.checkPermissions()) {
				l.init(mDataManager);
				mListeners.add(l);
			} else {
				String toastMessage = "Permission error in listener: "
						+ l.getTag();
				Toast toast = Toast.makeText(mAppContext, toastMessage,
						Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	}

	private void stopLogging() {
		mDataManager.finish();
		Log.d(TAG, "stoplogging");
		for (ContextListener l : mListeners) {
			l.stopListening();
		}

		// mListeners.clear();
	}

	@Override
	public void onDestroy() {
		// Tell the user we stopped.
		Log.i(TAG, "onDestroy");
		// stopLogging();
	}

}
