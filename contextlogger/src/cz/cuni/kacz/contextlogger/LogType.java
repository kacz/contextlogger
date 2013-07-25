package cz.cuni.kacz.contextlogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing the type of the log record.
 * 
 * @author kacz
 * 
 */
public enum LogType {
	INT(1), LONG(2), FLOAT(3), DOUBLE(4), STRING(5);

	int type;

	LogType(int type) {
		this.type = type;
	}

	private static Map<Integer, LogType> types = new HashMap<Integer, LogType>();
	static {
		for (LogType l : LogType.values()) {
			types.put(l.type, l);
		}
	}

	public static LogType byType(int t) {
		return types.get(t);
	}
}
