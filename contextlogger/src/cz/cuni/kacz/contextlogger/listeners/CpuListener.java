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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Timer;
import java.util.TimerTask;

import cz.cuni.kacz.contextlogger.DataManager;
import cz.cuni.kacz.contextlogger.TimeSource;

/**
 * ContextListener for logging CPU information.
 * 
 * @author kacz
 * 
 */
public class CpuListener extends DefaultContextListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final boolean running = false;
	private Timer timer;
	int period = 1000;

	RandomAccessFile mReader;

	// log names and types
	private final String labelCpu = "CPU";
	private final int typeCpu = DataManager.FLOAT;

	long oldCpu = 0;
	long oldIdle = 0;

	@Override
	public void startListening() {

		try {
			mReader = new RandomAccessFile("/proc/stat", "r");
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			return;
		}

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				long time = TimeSource.getTimeOfDay();

				try {
					mReader.seek(0);
					String load = mReader.readLine();

					String[] toks = load.split(" ");

					long idle = Long.parseLong(toks[5]);
					long cpu = Long.parseLong(toks[2])
							+ Long.parseLong(toks[3]) + Long.parseLong(toks[4])
							+ Long.parseLong(toks[6]) + Long.parseLong(toks[7])
							+ Long.parseLong(toks[8]);
					float usage = (float) (cpu - oldCpu)
							/ ((cpu + idle) - (oldCpu + oldIdle));
					mDataManager.insertLog(labelCpu, time, usage);

					oldCpu = cpu;
					oldIdle = idle;

				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}, 0, period);
	}

	@Override
	public void stopListening() {
		timer.cancel();
		try {
			mReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initLogTypes() {
		addLogType(labelCpu, typeCpu);
	}

}
