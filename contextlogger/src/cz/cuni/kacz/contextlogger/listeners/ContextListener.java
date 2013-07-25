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

import cz.cuni.kacz.contextlogger.DataManager;

/**
 * ContextListener interface can be implemented by classes with the aim to
 * capture context data and save it.
 * 
 * @author kacz
 * 
 */
public interface ContextListener {

	/**
	 * Start the listening. After this method gets called, the listener starts
	 * to produce log records and save them to DataManager's queue.
	 */
	public void startListening();

	/**
	 * Stop the listening.
	 */
	public void stopListening();

	/**
	 * Initialize the listener. Save a reference to the DataManager.
	 * 
	 * @param dm
	 *            Reference to the DataManager.
	 */
	public void init(DataManager dm);

	/**
	 * Function to check whether the application has the correct permissions to
	 * access data required by this listener.
	 * 
	 * @return If every permission is granted returns true, otherwise false.
	 */
	public boolean checkPermissions();

	/**
	 * Return the name of the listener.
	 * 
	 * @return the name of the listener.
	 */
	public String getTag();

}
