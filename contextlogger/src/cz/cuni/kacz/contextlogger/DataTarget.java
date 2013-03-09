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

import android.content.Context;

public interface DataTarget {
	public void open();

	public void close();

	public void insertLog(int listenerId, long time, int value);

	public void insertLog(int listenerId, long time, long value);

	public void insertLog(int listenerId, long time, float value);

	public void insertLog(int listenerId, long time, double value);

	public void insertLog(int listenerId, long time, String value);

	public void registerListener(int listenerId, int type, String listenerName);

	public boolean checkPermissions();

	void initCtx(Context ctx);
}
