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

/**
 * ContextListener for logging RAM information.
 * 
 * @author kacz
 * 
 */
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

public class RamListener extends DefaultContextListener {

	public RamListener(int pid, boolean debug) {
		mAppPid = pid;
		mDebug = debug;
	}

	public RamListener(int pid) {
		super();
		mAppPid = pid;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final boolean running = false;
	private Timer timer;
	int period = 1000;

	int mAppPid;
	int mMyPid;
	boolean mDebug = false;

	// log names and types
	private final String labelAvailMem = "System RAM available";
	private final int typeAvailMem = DataManager.LONG;
	private final String labelLowMem = "System RAM low";
	private final int typeLowMem = DataManager.INT;
	private final String labelRamThreshold = "System RAM threshold	";
	private final int typeRamThreshold = DataManager.INT;

	private final String labelAppDalvikPss = "App dalvik ram PSS";
	private final int typeAppDalvikPss = DataManager.INT;
	private final String labelAppNativePss = "App native ram PSS";
	private final int typeAppNativePss = DataManager.INT;
	private final String labelAppOtherPss = "App other ram PSS";
	private final int typeAppOtherPss = DataManager.INT;

	private final String labelAppDalvikPrivateDirty = "App dalvik ram private dirty";
	private final int typeAppDalvikPrivateDirty = DataManager.INT;
	private final String labelAppNativePrivateDirty = "App native ram private dirty";
	private final int typeAppNativePrivateDirty = DataManager.INT;
	private final String labelAppOtherPrivateDirty = "App other ram private dirty";
	private final int typeAppOtherPrivateDirty = DataManager.INT;

	private final String labelCLDalvikPss = "CL dalvik ram used";
	private final int typeCLDalvikPss = DataManager.INT;
	private final String labelCLNativePss = "CL native ram used";
	private final int typeCLNativePss = DataManager.INT;
	private final String labelCLOtherPss = "CL other ram used";
	private final int typeCLOtherPss = DataManager.INT;

	private final String labelCLDalvikPrivateDirty = "CL dalvik ram private dirty";
	private final int typeCLDalvikPrivateDirty = DataManager.INT;
	private final String labelCLNativePrivateDirty = "CL native ram private dirty";
	private final int typeCLNativePrivateDirty = DataManager.INT;
	private final String labelCLOtherPrivateDirty = "CL other ram private dirty";
	private final int typeCLOtherPrivateDirty = DataManager.INT;

	long oldCpu = 0;
	long oldIdle = 0;


	@Override
	public void startListening() {

		final ActivityManager activityManager = (ActivityManager) getAppContext()
				.getSystemService(Context.ACTIVITY_SERVICE);
		final MemoryInfo mi = new MemoryInfo();

		mMyPid = android.os.Process.myPid();
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				long time = TimeSource.getTimeOfDay();
				activityManager.getMemoryInfo(mi);
				mDataManager.insertLog(labelAvailMem, time, mi.availMem);
				mDataManager.insertLog(labelLowMem, time, mi.lowMemory ? 1 : 0);
				mDataManager.insertLog(labelRamThreshold, time, mi.threshold);

				android.os.Debug.MemoryInfo[] mis = activityManager
						.getProcessMemoryInfo(new int[] { mAppPid });
				if (mis.length > 0) {
					mDataManager.insertLog(labelAppDalvikPss, time,
							mis[0].dalvikPss);
					mDataManager.insertLog(labelAppNativePss, time,
							mis[0].nativePss);
					mDataManager.insertLog(labelAppOtherPss, time,
							mis[0].otherPss);
					mDataManager.insertLog(labelAppDalvikPrivateDirty, time,
							mis[0].dalvikPrivateDirty);
					mDataManager.insertLog(labelAppNativePrivateDirty, time,
							mis[0].nativePrivateDirty);
					mDataManager.insertLog(labelAppOtherPrivateDirty, time,
							mis[0].otherPrivateDirty);
				}

				// CL
				if (mDebug) {
				mis = activityManager
						.getProcessMemoryInfo(new int[] { mMyPid });
				if (mis.length > 0) {
					mDataManager.insertLog(labelCLDalvikPss, time,
							mis[0].dalvikPss);
					mDataManager.insertLog(labelCLNativePss, time,
							mis[0].nativePss);
					mDataManager.insertLog(labelCLOtherPss, time,
							mis[0].otherPss);
					mDataManager.insertLog(labelCLDalvikPrivateDirty, time,
							mis[0].dalvikPrivateDirty);
					mDataManager.insertLog(labelCLNativePrivateDirty, time,
							mis[0].nativePrivateDirty);
					mDataManager.insertLog(labelCLOtherPrivateDirty, time,
							mis[0].otherPrivateDirty);
				}
				}
			}
		}, 0, period);
	}

	@Override
	public void stopListening() {
		timer.cancel();
	}

	@Override
	public void initLogTypes() {
		addLogType(labelAvailMem, typeAvailMem);
		addLogType(labelLowMem, typeLowMem);
		addLogType(labelRamThreshold, typeRamThreshold);

		addLogType(labelAppDalvikPss, typeAppDalvikPss);
		addLogType(labelAppNativePss, typeAppNativePss);
		addLogType(labelAppOtherPss, typeAppOtherPss);

		addLogType(labelAppDalvikPrivateDirty, typeAppDalvikPrivateDirty);
		addLogType(labelAppNativePrivateDirty, typeAppNativePrivateDirty);
		addLogType(labelAppOtherPrivateDirty, typeAppOtherPrivateDirty);

		if (mDebug) {
			addLogType(labelCLDalvikPss, typeCLDalvikPss);
			addLogType(labelCLNativePss, typeCLNativePss);
			addLogType(labelCLOtherPss, typeCLOtherPss);
			addLogType(labelCLDalvikPrivateDirty, typeCLDalvikPrivateDirty);
			addLogType(labelCLNativePrivateDirty, typeCLNativePrivateDirty);
			addLogType(labelCLOtherPrivateDirty, typeCLOtherPrivateDirty);
		}
	}

}
