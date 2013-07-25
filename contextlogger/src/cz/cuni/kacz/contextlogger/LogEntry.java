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

/**
 * POJO class representing a context change event.
 * 
 * @author kacz
 * 
 */
public class LogEntry {

	/**
	 * Constructor of a INT type record.
	 * 
	 * @param time
	 *            Timestamp of the record.
	 * @param label
	 *            Name of the piece of context.
	 * @param value
	 *            Value of the record.
	 */
	LogEntry(long time, String label, int value) {
		this.time = time;
		this.label = label;
		this.intValue = value;
	}

	/**
	 * Constructor of a LONG type record.
	 * 
	 * @param time
	 *            Timestamp of the record.
	 * @param label
	 *            Name of the piece of context.
	 * @param value
	 *            Value of the record.
	 */
	LogEntry(long time, String label, long value) {
		this.time = time;
		this.label = label;
		this.longValue = value;
	}

	/**
	 * Constructor of a FLOAT type record.
	 * 
	 * @param time
	 *            Timestamp of the record.
	 * @param label
	 *            Name of the piece of context.
	 * @param value
	 *            Value of the record.
	 */
	LogEntry(long time, String label, float value) {
		this.time = time;
		this.label = label;
		this.floatValue = value;
	}

	/**
	 * Constructor of a DOUBLE type record.
	 * 
	 * @param time
	 *            Timestamp of the record.
	 * @param label
	 *            Name of the piece of context.
	 * @param value
	 *            Value of the record.
	 */
	LogEntry(long time, String label, double value) {
		this.time = time;
		this.label = label;
		this.doubleValue = value;
	}

	/**
	 * Constructor of a STRING type record.
	 * 
	 * @param time
	 *            Timestamp of the record.
	 * @param label
	 *            Name of the piece of context.
	 * @param value
	 *            Value of the record.
	 */
	LogEntry(long time, String label, String value) {
		this.time = time;
		this.label = label;
		this.stringValue = value;
	}

	/** Timestamp of the event. */
	long time;

	/** Name of the piece of context. */
	String label;

	/** Value of a INT type record. */
	int intValue;

	/** Value of a LONG type record. */
	long longValue;

	/** Value of a FLOAT type record. */
	float floatValue;

	/** Value of a DOUBLE type record. */
	double doubleValue;

	/** Value of a STRING type record. */
	String stringValue;
}
