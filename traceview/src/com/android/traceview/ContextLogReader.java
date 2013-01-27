package com.android.traceview;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContextLogReader {
	
	private final String mTraceFileName;
	private Map<Integer, ContextLogData> mLogMap = null;
	ContextLogData[] mSortedLogs;
	int mVersionNumber;
	
	// A regex for matching the thread "id name" lines in the .key file
    private static final Pattern mIdTypeNamePattern = Pattern.compile("(\\d+)\\s(\\d+)\\s(.*)");  //$NON-NLS-1$
	
	static final int PARSE_VERSION = 0;
    static final int PARSE_LOGTYPES = 1;
    static final int PARSE_METHODS = 2;
    static final int PARSE_OPTIONS = 4;

    private enum ClockSource {
        THREAD_CPU, WALL, DUAL,
    };
	    
	public ContextLogReader(String traceFileName) throws IOException {
        mTraceFileName = traceFileName;
		mLogMap = new HashMap<Integer, ContextLogData>();
        
        
        generateTrees();
    }
	
	void generateTrees() throws IOException {
        long offset = parseKeys();
//        parseData(offset);
		analyzeData();
    }
	
	long parseKeys() throws IOException {
        long offset = 0;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(mTraceFileName+".log"), "US-ASCII"));

            int mode = PARSE_VERSION;
            String line = null;
            while (true) {
                line = in.readLine();
                if (line == null) {
                    throw new IOException("Key section does not have an *end marker");
                }

                // Calculate how much we have read from the file so far.  The
                // extra byte is for the line ending not included by readLine().
                offset += line.length() + 1;
                if (line.startsWith("*")) {
                    if (line.equals("*version")) {
                        mode = PARSE_VERSION;
                        continue;
                    }
                    if (line.equals("*logs")) {
                        mode = PARSE_LOGTYPES;
                        continue;
                    }
                    if (line.equals("*end")) {
                        break;
                    }
                }
                switch (mode) {
                case PARSE_VERSION:
                    mVersionNumber = Integer.decode(line);
                    mode = PARSE_OPTIONS;
                    break;
                case PARSE_LOGTYPES:
                    parseLogTypes(line);
                    break;
                case PARSE_OPTIONS:
                    parseOption(line);
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        } finally {
            if (in != null) {
                in.close();
            }
        }

//        if (mClockSource == null) {
//            mClockSource = ClockSource.THREAD_CPU;
//        }

        return offset;
    }
	
	void parseOption(String line) {
	}
	
	void parseLogTypes(String line) {
		String idStr = null;
		String typeStr = null;
        String name = null;
        Matcher matcher = mIdTypeNamePattern.matcher(line);
        if (matcher.find()) {
            idStr = matcher.group(1);
            typeStr = matcher.group(2);
            name = matcher.group(3);
        }
        if (idStr == null || typeStr == null) {
			return;
		}
        if (name == null) {
			name = "(unknown)";
		}

        int id = Integer.decode(idStr);
        int type = Integer.decode(typeStr);
		mLogMap.put(id, new ContextLogData(id, type, name));
	}
	
	private void analyzeData() {
		// sort log by id
		Collection<ContextLogData> lv = mLogMap.values();
		mSortedLogs = lv.toArray(new ContextLogData[lv.size()]);

		Arrays.sort(mSortedLogs, new Comparator<ContextLogData>() {
			@Override
			public int compare(ContextLogData ld1, ContextLogData ld2) {
				if (ld2.getId() > ld1.getId()) {
					return 1;
				}
				return -1;
			}
		});
	}

	public ArrayList<TimeLineView.LogRecord> getLogRecords() {
		TimeLineView.LogRecord record;
		ArrayList<TimeLineView.LogRecord> logRecs;
		logRecs = new ArrayList<TimeLineView.LogRecord>();

		// For now we simply put dummy records containing only log info into the
		// list
		for (ContextLogData logData : mSortedLogs) {
			record = new TimeLineView.LogRecord(logData);
			logRecs.add(record);
		}

		// if (mRegression) {
		// dumpTimeRecs(timeRecs);
		// System.exit(0);
		// }

		return logRecs;
	}
	
	public Map<Integer, ContextLogData> getLogMap() {
		return mLogMap;
	}

}
