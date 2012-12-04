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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;

import cz.cuni.kacz.contextlogger.listeners.ContextListener;

/**
 * 
 * @author kacz
 * 
 */

public class ContextLogger {

	private static final String TAG = "ContextLogger";

	private final String traceName = "programNeve";

	Activity mCallerActivity = null;

	private boolean mIsRunning = false;

	private boolean mDoTrace = false;

	/** Messenger for communicating with the service. */
	Messenger mService = null;

	/** Flag indicating whether we have called bind on the service. */
	boolean mBound;

	private ArrayList<ContextListener> mListeners = new ArrayList<ContextListener>();

	/**
	 * Constructor. Sets up application context reference and starts the logger
	 * service process.
	 * 
	 * @param context
	 */
	public ContextLogger(Activity activity) {
		mCallerActivity = activity;

		if (this.mCallerActivity == null)
			Log.d(TAG, "mappcontext");
		if (ContextLogger.class == null)
			Log.d(TAG, "class");
		Intent akarmi = new Intent(this.mCallerActivity,
				ContextLoggerService.class);
		Log.d(TAG, "hahaaa");
		boolean succ = mCallerActivity.bindService(new Intent(
				this.mCallerActivity, ContextLoggerService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		if (!succ) {
			Log.i(TAG, "bind unsuccessful");
		} else {
			Log.i(TAG, "bind successful");
		}

	}

	public void stopService() {
		mCallerActivity.unbindService(mConnection);
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the object we can use to
			// interact with the service. We are communicating with the
			// service using a Messenger, so here we get a client-side
			// representation of that from the raw IBinder object.
			mService = new Messenger(service);
			mBound = true;
			Log.i(TAG, "messenger connected");
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			mBound = false;
			Log.i(TAG, "messenger disconnected");
		}
	};

	/**
	 * 
	 */
	public void startLogging() {
		Log.d(TAG, "startLogging");

		if (!mBound)
			return;

		if (mIsRunning == true) {
			Log.e(TAG, "Logging already in progress.");
			return;
		}
		mIsRunning = true;

		initListeners();

		// start tracing if needed
		if (mDoTrace) {
			Debug.startMethodTracing(Environment
					.getExternalStoragePublicDirectory(
							Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
					+ "/" + traceName);
			Log.d(TAG, "method tracing started");
		}

		// send the listeners to the service
		Message msg = Message.obtain(null,
				ContextLoggerService.MSG_START_LOGGING, 0, 0);
		// Bundle data = new Bundle();
		// data.putSerializable("listeners", mListeners);
		// msg.setData(data);

		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.d(TAG, "startLogging - START_MSG sending error");
			e.printStackTrace();
		}
		Log.d(TAG, "startLogging - START_MSG sent");
	};

	public void initListeners() {
		Log.d(TAG, "initListeners");
		if (!mBound)
			return;
		// send the listeners to the service
		Message msg = Message.obtain(null,
				ContextLoggerService.MSG_INIT_LISTENERS, 0, 0);
		Bundle data = new Bundle();
		data.putSerializable("listeners", mListeners);
		msg.setData(data);

		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.d(TAG, "startLogging - START_MSG sending error");
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public void stopLogging() {
		Log.d(TAG, "stopLogging");
		if (mIsRunning == false) {
			Log.e(TAG, "Logging is not running currently.");
			return;
		}
		mIsRunning = false;

		// stop tracing
		if (mDoTrace) {
			Debug.stopMethodTracing();
			Log.d(TAG, "stopmethod tracing stopped");
		}

		if (!mBound)
			return;
		// Create and send a message to the service, using a supported 'what'
		// value
		Message msg = Message.obtain(null,
				ContextLoggerService.MSG_STOP_LOGGING, 0, 0);
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
			return;
		}
		return;
	};

	public void clearListeners() {
		mListeners.clear();
	};

	/**
	 * 
	 */
	public void addListener(ContextListener listener) {
		if (mIsRunning == true) {
			Log.e(TAG, "Logging already in progress.");
			return;
		}
		mListeners.add(listener);
	};

	/**
	 * 
	 * @param enable
	 */
	public void enableTracing(boolean enable) {
		if (mIsRunning == true) {
			Log.e(TAG, "Logging already in progress.");
			return;
		}
		mDoTrace = enable;
	};

}
