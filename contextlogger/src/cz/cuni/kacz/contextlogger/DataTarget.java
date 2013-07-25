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

/**
 * DataTarget interface can be implemented by classes with the aim to
 * store/provide/persist context log data.
 * 
 * @author kacz
 * 
 */
public interface DataTarget {

	/**
	 * Opens the data target. This method should connect to the underlying data
	 * storage.
	 */
	public void open();

	/**
	 * Closes the data target. This method should close the connection to the
	 * underlying data storage.
	 */
	public void close();

	/**
	 * Saves an INT type log record in the target.
	 * 
	 * @param listenerId
	 *            ID of the piece of context.
	 * @param time
	 *            Timestamp of the record.
	 * @param value
	 *            Value of the record.
	 */
	public void insertLog(int listenerId, long time, int value);

	/**
	 * Saves an LONG type log record in the target.
	 * 
	 * @param listenerId
	 *            ID of the piece of context.
	 * @param time
	 *            Timestamp of the record.
	 * @param value
	 *            Value of the record.
	 */
	public void insertLog(int listenerId, long time, long value);

	/**
	 * Saves a FLOAT type log record in the target.
	 * 
	 * @param listenerId
	 *            ID of the piece of context.
	 * @param time
	 *            Timestamp of the record.
	 * @param value
	 *            Value of the record.
	 */
	public void insertLog(int listenerId, long time, float value);

	/**
	 * Saves a DOUBLE type log record in the target.
	 * 
	 * @param listenerId
	 *            ID of the piece of context.
	 * @param time
	 *            Timestamp of the record.
	 * @param value
	 *            Value of the record.
	 */
	public void insertLog(int listenerId, long time, double value);

	/**
	 * Saves a STRING type log record in the target.
	 * 
	 * @param listenerId
	 *            ID of the piece of context.
	 * @param time
	 *            Timestamp of the record.
	 * @param value
	 *            Value of the record.
	 */
	public void insertLog(int listenerId, long time, String value);

	/**
	 * Registers a piece of context. This method should save information about
	 * the piece of context to the underlying data storage.
	 * 
	 * @param listenerId
	 *            ID of the piece of context.
	 * @param type
	 *            Type of the piece of context.
	 * @param listenerName
	 *            Name of the piece of context.
	 */
	public void registerListener(int listenerId, int type, String listenerName);

	/**
	 * Check whether the application has the correct permissions to access
	 * storage required by this data target.
	 * 
	 * @return
	 */
	public boolean checkPermissions();

	/**
	 * Saves a reference to the Application context.
	 * 
	 * @param ctx
	 */
	void initCtx(Context ctx);
}
