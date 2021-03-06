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
package cz.cuni.kacz.contextlogger.demo;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cz.cuni.kacz.contextlogger.ContextLogger;
import cz.cuni.kacz.contextlogger.listeners.AcceleraionListener;
import cz.cuni.kacz.contextlogger.listeners.AmbientTemperatureListener;
import cz.cuni.kacz.contextlogger.listeners.BarometerListener;
import cz.cuni.kacz.contextlogger.listeners.BatteryListener;
import cz.cuni.kacz.contextlogger.listeners.ContextListener;
import cz.cuni.kacz.contextlogger.listeners.CpuListener;
import cz.cuni.kacz.contextlogger.listeners.GpsLocationListener;
import cz.cuni.kacz.contextlogger.listeners.GpsStatusListener;
import cz.cuni.kacz.contextlogger.listeners.GyroscopeListener;
import cz.cuni.kacz.contextlogger.listeners.LightListener;
import cz.cuni.kacz.contextlogger.listeners.LinearAcceleraionListener;
import cz.cuni.kacz.contextlogger.listeners.MagnetoListener;
import cz.cuni.kacz.contextlogger.listeners.PassiveLocationListener;
import cz.cuni.kacz.contextlogger.listeners.ProximityListener;
import cz.cuni.kacz.contextlogger.listeners.RamListener;
import cz.cuni.kacz.contextlogger.listeners.RotationListener;
import cz.cuni.kacz.contextlogger.listeners.ScreenBrightnessListener;
import cz.cuni.kacz.contextlogger.listeners.ScreenOrientationListener;
import cz.cuni.kacz.contextlogger.listeners.ScreenStateListener;
import cz.cuni.kacz.contextlogger.listeners.TelephonyListener;
import cz.cuni.kacz.contextlogger.listeners.TrafficListener;
import cz.cuni.kacz.contextlogger.listeners.TrafficSpeedListener;
import cz.cuni.kacz.contextlogger.listeners.WifiListener;
import cz.cuni.kacz.contextlogger.listeners.WifiRSSIListener;
import cz.cuni.kacz.contextlogger.listeners.WifiStateListener;

public class MainActivity extends Activity {

	ContextLogger mCL = null;
	private static final String TAG = "DisplayMessageActivity";

	private boolean running = false;

	final Activity mainActivity = this;

	Button startButton;
	Button stopButton;

	Thread mStarterThread;
	Boolean doStart;
	Boolean doStop;
	Boolean stopStarter;

	boolean intentTargetRegistered = false;
	private BroadcastReceiver mBCReceiver;

	private final Map<Integer, TextView> mValues = new HashMap<Integer, TextView>();

	Resources res;

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (action == KeyEvent.ACTION_DOWN) {
				mCL.brb();
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_DOWN) {
				if (running) {
					stopLogging();
				} else {
					startLogging();
				}
			}
			return true;
		default:
			return super.dispatchKeyEvent(event);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		// Create the ContextLogger class
		mCL = ContextLogger.getInstance();
		mCL.init(this.getApplicationContext());
		mCL.setTraceName("CLDemo");

		res = getResources();

		Log.d(TAG, "ContextLogger created");

		// Create BC receiver for the intent data target
		mBCReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent
						.getAction()
						.equals(cz.cuni.kacz.contextlogger.IntentDataTarget.ACTION_LISTENER_ADDED)) {
					String name = intent.getStringExtra("name");
					int id = intent.getIntExtra("id", -1);
					createNewEntryRow(id, name);
				} else if (intent
						.getAction()
						.equals(cz.cuni.kacz.contextlogger.IntentDataTarget.ACTION_CONTEXT_CHANGED)) {
					long time = intent.getLongExtra("time", -1);
					int id = intent.getIntExtra("id", -1);
					String value = "";
					if (intent.hasExtra("longValue")) {
						value = String.valueOf(intent.getLongExtra("longValue",
								-1));
					} else if (intent.hasExtra("intValue")) {
						value = String.valueOf(intent.getIntExtra("intValue",
								-1));
					} else if (intent.hasExtra("floatValue")) {
						value = String.valueOf(intent.getFloatExtra(
								"floatValue", -1));
					} else if (intent.hasExtra("doubleValue")) {
						value = String.valueOf(intent.getDoubleExtra(
								"doubleValue", -1));
					} else if (intent.hasExtra("stringValue")) {
						value = String.valueOf(intent
								.getStringExtra("stringValue"));
					}
					updateEntry(time, id, value);
				}

			}
		};

		// register the BC receiver
		IntentFilter filter = new IntentFilter(
				cz.cuni.kacz.contextlogger.IntentDataTarget.ACTION_CONTEXT_CHANGED);
		filter.addAction(cz.cuni.kacz.contextlogger.IntentDataTarget.ACTION_LISTENER_ADDED);
		getApplicationContext().registerReceiver(mBCReceiver, filter);

		doStart = false;
		doStop = false;
		stopStarter = false;

		// starter thread
		mStarterThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (true) {
					synchronized (doStart) {
						if (doStart) {
							Log.d(TAG, "doStart");
							mCL.startLogging();
							running = true;
							doStart = false;
							mainActivity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									stopButton.setEnabled(true);
									Toast.makeText(mainActivity,
											"ContextLogging started",
											Toast.LENGTH_SHORT).show();
								}
							});

						}
					}
					synchronized (doStop) {
						if (doStop) {
							if (running) {
							Log.d(TAG, "doStop");
							mCL.stopLogging();
							mCL.clearListeners();
							// mCL.clearTargets();

							running = false;
							doStop = false;
							mainActivity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									startButton.setEnabled(true);
									Toast.makeText(mainActivity,
											"ContextLogging stopped",
											Toast.LENGTH_SHORT).show();
								}
							});
							}

						}
					}
					synchronized (stopStarter) {
						if (stopStarter) {
							Log.d(TAG, "stopStarter");
							break;
						}
					}
					synchronized (mStarterThread) {
						try {
							mStarterThread.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							break;
						}
					}

				}
			}

		});
		mStarterThread.start();

		// Prepare the screen
		startButton = (Button) findViewById(R.id.start_button);
		stopButton = (Button) findViewById(R.id.stop_button);
		if (running) {
			startButton.setVisibility(Button.GONE);
			stopButton.setVisibility(Button.VISIBLE);
		} else {
			startButton.setVisibility(Button.VISIBLE);
			stopButton.setVisibility(Button.GONE);
		}

		startButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startLogging();

			}
		});
		stopButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				stopLogging();

			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void createNewEntryRow(int listenerId, String listenerName) {
		// create new line for the new value
		LinearLayout l = new LinearLayout(this);
		l.setOrientation(LinearLayout.HORIZONTAL);

		// display the label...
		TextView label = new TextView(this);
		label.setText(listenerName + ": ");
		l.addView(label);

		// ... and the value
		TextView val = new TextView(this);
		val.setText("-");
		l.addView(val);
		LinearLayout cwl = (LinearLayout) findViewById(R.id.ContextWrapperLayout);
		cwl.addView(l);

		// also put the new textview to the map
		mValues.put(listenerId, val);

		Log.d(TAG, "New line added to screen: " + listenerName + " with id: "
				+ listenerId);
	}

	public void updateEntry(long time, int listenerId, String value) {
		Log.d(TAG, String.valueOf(time) + " - id " + listenerId + ": " + value);
		if (mValues.containsKey(listenerId)) {
			TextView t = mValues.get(listenerId);
			t.setText(value);
		} else {
			Log.d(TAG, "No line for listener id: " + listenerId);
		}
	}

	public void startLogging() {
		if (!running) {
			Log.d(TAG, "startLogging");
			Log.d(TAG, "activitys UID: " + Process.myUid());
			Log.d(TAG, "activitys PID: " + Process.myPid());
			Log.d(TAG, "activitys TID: " + Process.myTid());

			SharedPreferences sharedPref = PreferenceManager
					.getDefaultSharedPreferences(this);

			boolean tracing = sharedPref.getBoolean(
					res.getString(R.string.pref_key_trace), false);
			mCL.enableTracing(tracing);

			ContextListener l;
			mCL.clearListeners();

			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_acceleration_listener),
					false)) {
				l = new AcceleraionListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_temperature_listener),
					false)) {
				l = new AmbientTemperatureListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_barometer_listener), false)) {
				l = new BarometerListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_battery_listener), false)) {
				l = new BatteryListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_cpu_listener), false)) {
				l = new CpuListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_gps_location_listener),
					false)) {
				l = new GpsLocationListener();
				mCL.addListener(l);
			}
			if (sharedPref
					.getBoolean(res
							.getString(R.string.pref_key_gps_status_listener),
							false)) {
				l = new GpsStatusListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_gyroscope_listener), false)) {
				l = new GyroscopeListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_light_listener), false)) {
				l = new LightListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(res
					.getString(R.string.pref_key_linear_acceleration_listener),
					false)) {
				l = new LinearAcceleraionListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_magneto_listener), false)) {
				l = new MagnetoListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_passive_location_listener),
					false)) {
				l = new PassiveLocationListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_proximity_listener), false)) {
				l = new ProximityListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_ram_listener), false)) {
				l = new RamListener(android.os.Process.myPid(), false);
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_rotation_listener), false)) {
				l = new RotationListener();
				mCL.addListener(l);
			}
			if (sharedPref
					.getBoolean(
							res.getString(R.string.pref_key_screen_brightness_listener),
							false)) {
				l = new ScreenBrightnessListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(res
					.getString(R.string.pref_key_screen_orientation_listener),
					false)) {
				l = new ScreenOrientationListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_screen_state_listener),
					false)) {
				l = new ScreenStateListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_telephony_listener), false)) {
				l = new TelephonyListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_traffic_listener), false)) {
				l = new TrafficListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_traffic_speed_listener),
					false)) {
				l = new TrafficSpeedListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_wifi_listener), false)) {
				l = new WifiListener();
				mCL.addListener(l);
			}
			if (sharedPref
					.getBoolean(res
							.getString(R.string.pref_key_wifi_state_listener),
							false)) {
				l = new WifiStateListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_wifirssi_listener), false)) {
				l = new WifiRSSIListener();
				mCL.addListener(l);
			}
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_test_listener), false)) {
				l = new TestListener(1000);
				mCL.addListener(l);
			}


			// if (sharedPref.getBoolean(
			// res.getString(R.string.pref_key_new_test_listener), false)) {
			// l = new RamListener(android.os.Process.myPid());
			// mCL.addListener(l);
			// }

			// set data targets...
			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_target_intent), false)) {
				mCL.useIntentTarget(true);
				Log.d(TAG, "intent setting set" + R.string.pref_target_intent);
				intentTargetRegistered = true;
			}

			if (sharedPref.getBoolean(
					res.getString(R.string.pref_key_target_textfile), false)) {
				mCL.useTextFileTarget(true);
			}

			synchronized (doStart) {
				doStart = true;
			}
			synchronized (mStarterThread) {
				mStarterThread.notify();
			}

			// update the screen
			Button startButton = (Button) findViewById(R.id.start_button);
			startButton.setVisibility(Button.GONE);
			Button stopButton = (Button) findViewById(R.id.stop_button);
			stopButton.setVisibility(Button.VISIBLE);
			stopButton.setEnabled(false);
		}
	}

	public void stopLogging() {
		if (running) {
			Log.d(TAG, "stopLogging");

			// stop the logging process
			// mCL.stopLogging();
			//
			// mCL.clearListeners();
			// mCL.clearTargets();
			//
			// running = false;

			synchronized (doStop) {
				doStop = true;
			}
			synchronized (mStarterThread) {
				mStarterThread.notify();
			}

			// update the screen
			Button stopButton = (Button) findViewById(R.id.stop_button);
			stopButton.setVisibility(Button.GONE);
			Button startButton = (Button) findViewById(R.id.start_button);
			startButton.setVisibility(Button.VISIBLE);
			startButton.setEnabled(false);

			LinearLayout cwl = (LinearLayout) findViewById(R.id.ContextWrapperLayout);
			View divider = new View(this);
			divider.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, 1));
			divider.setBackgroundColor(Color.BLACK);
			cwl.addView(divider);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		// stopLogging(this);

		Log.d(TAG, "onPause");
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		// mCL.stopLogging();
		// mCL.stopService();

		synchronized (doStop) {
			doStop = true;
		}
		synchronized (stopStarter) {
			stopStarter = true;
		}
		synchronized (mStarterThread) {
			mStarterThread.notify();
		}

		// unregister the BC receiver
		if (intentTargetRegistered) {
		getApplicationContext().unregisterReceiver(mBCReceiver);
			intentTargetRegistered = false;
		}
	}
}
