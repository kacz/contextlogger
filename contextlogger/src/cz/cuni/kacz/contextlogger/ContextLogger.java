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
import android.widget.Toast;
import cz.cuni.kacz.contextlogger.listeners.ContextListener;

/**
 * ContextLogger proxy class.
 * <p>
 * This singleton class provides public API of the ContextLogger library. The
 * application can hold a reference to it and use it for the communication with
 * the background process.
 * 
 * @author kacz
 * 
 */
/**
 * @author kacz
 * 
 */
public class ContextLogger {

	private static final ContextLogger sInstance = new ContextLogger();;

	private boolean mInitialized;

	private static final String TAG = "ContextLogger";

	private static final String DEFAULT_TRACE_NAME = "CLTrace";
	private String mTraceName = DEFAULT_TRACE_NAME;

	private String mTracePath;

	/**
	 * Set the filename of the trace and context log files.
	 * 
	 * @param traceName
	 */
	public void setTraceName(String traceName) {
		this.mTraceName = traceName;
	}


	private boolean mUseIntentDataTarget;

	/**
	 * Enables or disables IntentDataTarget.
	 * 
	 * @param use
	 *            True for enable, false for disable.
	 */
	public void useIntentTarget(boolean use) {
		this.mUseIntentDataTarget = use;
	}

	private boolean mUseTextFileDataTarget;

	/**
	 * Enables or disables TextFiletDataTarget.
	 * 
	 * @param use
	 *            True for enable, false for disable.
	 */
	public void useTextFileTarget(boolean use) {
		this.mUseTextFileDataTarget = use;
	}

	/**
	 * Enables or disables method tracing.
	 * 
	 * @param enable
	 *            True for enable, false for disable.
	 */
	public void enableTracing(boolean enable) {
		if (mIsRunning == true) {
			Log.e(TAG, "Logging already in progress.");
			return;
		}
		mDoTrace = enable;
	};


	Context mCallerContext = null;

	private boolean mIsRunning = false;

	private boolean mDoTrace = false;

	/** Messenger for communicating with the service. */
	Messenger mService = null;

	/** Flag indicating whether we have called bind on the service. */
	boolean mBound;

	/** Default size of the trace file. */
	private final int TRACE_BUFF_SUZE = 128 * 1024 * 1024;// 128M

	/** List of ContextListeners to use. */
	private final ArrayList<ContextListener> mListeners = new ArrayList<ContextListener>();

	/**
	 * Returns reference to the singleton instance.
	 * 
	 * @return Reference to the singleton.
	 */
	public static ContextLogger getInstance() {
		return sInstance;
	}

	/** Private constructor of the singleton. */
	private ContextLogger() {
		mInitialized = false;
	};

	/**
	 * Initializes the library. Sets up application context reference and starts
	 * the logger service process.
	 * 
	 * @param context
	 */
	public boolean init(Context context) {
		if (mInitialized) {
			return false;
		}

		mCallerContext = context;

		if (this.mCallerContext == null) {
			Log.d(TAG, "mappcontext");
		}
		if (ContextLogger.class == null) {
			Log.d(TAG, "class");
		}

		boolean succ = mCallerContext.bindService(new Intent(
				this.mCallerContext, ContextLoggerService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		if (!succ) {
			Log.i(TAG, "bind unsuccessful");
			return false;
		} else {
			Log.i(TAG, "bind successful");
			mInitialized = true;
			return true;
		}
	}

	/**
	 * Stops the background service.
	 */
	public void stopService() {
		mCallerContext.unbindService(mConnection);
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
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

		@Override
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			mBound = false;
			Log.i(TAG, "messenger disconnected");
		}
	};

	/**
	 * Starts the logging session. Sends the listeners to the background
	 * process. Sends a message to the background process to start the the
	 * logging. If method tracing is requested, it will be also started. If the
	 * background process in not connected or the logging was already started,
	 * this method does nothing.
	 */
	public void startLogging() {
		Log.d(TAG, "startLogging");

		// background process not connected
		if (!mBound) {
			return;
		}

		// logging already started
		if (mIsRunning == true) {
			Log.e(TAG, "Logging already in progress.");
			return;
		}
		mIsRunning = true;

		// send the listeners to the service
		initListeners();

		// start logging
		Message msg = Message.obtain(null,
				ContextLoggerService.MSG_START_LOGGING, 0, 0);
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.d(TAG, "startLogging - START_MSG sending error");
			e.printStackTrace();
		}

		// start tracing if needed
		if (mDoTrace) {
			Debug.startMethodTracing(mTracePath, TRACE_BUFF_SUZE);
			Log.d(TAG, "method tracing started");
		}
		Log.d(TAG, "startLogging - START_MSG sent");
	};

	/**
	 * Initializes the listeners. Serializes and sends the list of listeners to
	 * the background process. They will be then registered and become ready to
	 * start listening.
	 */
	private void initListeners() {
		Log.d(TAG, "initListeners");
		if (!mBound) {
			return;
		}

		// Set the filename for current run
		String dateString = new SimpleDateFormat("-yyMMdd-hhmmss")
				.format(new Date());
		mTracePath = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
				+ "/" + mTraceName + dateString;

		// send the listeners to the service
		Message msg = Message.obtain(null,
				ContextLoggerService.MSG_INIT_LISTENERS, 0, 0);
		Bundle data = new Bundle();
		data.putSerializable("listeners", mListeners);

		data.putString("fileName", mTracePath);
		data.putBoolean("useTextFileDataTarget", mUseTextFileDataTarget);
		data.putBoolean("useIntentDataTarget", mUseIntentDataTarget);
		Log.d(TAG, "intent setting set:" + mUseIntentDataTarget);
		msg.setData(data);

		try {
			mService.send(msg);
		} catch (RemoteException e) {
			Log.d(TAG, "startLogging - START_MSG sending error");
			e.printStackTrace();
		}
	}

	/**
	 * Stops the logging session.
	 */
	public void stopLogging() {
		Log.d(TAG, "stopLogging");
		if (mIsRunning == false) {
			Log.e(TAG, "Logging is not running currently.");
			return;
		}
		mIsRunning = false;

		if (mBound) {
			// Create and send a message to the service, using a supported
			// 'what'
			// value
			Message msg = Message.obtain(null,
					ContextLoggerService.MSG_STOP_LOGGING, 0, 0);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();

			}
		}

		// stop tracing
		if (mDoTrace) {
			Debug.stopMethodTracing();
			Log.d(TAG, "stopmethod tracing stopped");
		}

		return;
	};

	/**
	 * Clears the list of listeners.
	 */
	public void clearListeners() {
		mListeners.clear();
	};

	/**
	 * Adds a ContextListener to the list.
	 * 
	 * @param listener
	 *            ContextListener to add.
	 */
	public void addListener(ContextListener listener) {
		if (mIsRunning == true) {
			Log.e(TAG, "Logging already in progress.");
			return;
		}
		mListeners.add(listener);
	};


	/**
	 * Big-red-button signal.
	 */
	public void brb() {
		Log.e(TAG, "Big Red Button pushed.");
		Toast.makeText(mCallerContext, "BRB", Toast.LENGTH_SHORT).show();
	}

}
