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

import java.io.Serializable;

import android.content.Context;
import cz.cuni.kacz.contextlogger.ContextLoggerService;
import cz.cuni.kacz.contextlogger.DataManager;

public abstract class DefaultContextListener implements ContextListener,
		Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected DataManager mDataManager = null;
	protected String TAG = this.getClass().getSimpleName();

	@Override
	public abstract void startListening();

	@Override
	public abstract void stopListening();

	public abstract void initLogTypes();

	@Override
	public boolean checkPermissions() {
		return true;
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public void init(DataManager dm) {
		mDataManager = dm;
		initLogTypes();
	}

	protected void addLogType(String label, int type) {
		mDataManager.registerListener(label, type);
	}

	protected Context getAppContext() {
		return ContextLoggerService.mAppContext;
	}
}
