package com.android.traceview;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
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

import cz.cuni.kacz.contextlogger.LogType;

public class ContextLogReader {
	
	private final String mTraceFileName;
	private Map<Integer, ContextLogData> mLogMap = null;
	ContextLogData[] mSortedLogs;
	int mVersionNumber;
	long mStartTime;
	long mEndTime;

	// A regex for matching the thread "id name" lines in the .key file
    private static final Pattern mIdTypeNamePattern = Pattern.compile("(\\d+)\\s(\\d+)\\s(.*)");  //$NON-NLS-1$
	
	static final int PARSE_VERSION = 0;
    static final int PARSE_LOGTYPES = 1;
    static final int PARSE_METHODS = 2;
    static final int PARSE_OPTIONS = 4;

	private static final int DATA_MAGIC = 0x574f4c53;

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
		parseData(offset);
		analyzeData();
    }
	
	void parseData(long offset) throws IOException {
		DataInputStream in = null;
		try {
			in = new DataInputStream(new FileInputStream(mTraceFileName+".log"));
			in.skip(offset);
			int magic = in.readInt();
			if (magic != DATA_MAGIC) {
				// TODO
			}
			byte version = in.readByte();
			mStartTime = in.readLong();

			try {
				while (true) {
					int logId = in.readInt();
					ContextLogData ld = mLogMap.get(logId);
					if (ld == null) {
						// TODO: return with error
					}
					long timeStamp = in.readLong();
					switch (ld.getType()) {
					case INT:{
						int val = in.readInt();
						ld.getIntDataMap().put(timeStamp, val);
						break;
					}
					case LONG: {
						long val = in.readLong();
						ld.getLongDataMap().put(timeStamp, val);
						break;
					}
					case FLOAT: {
						float val = in.readFloat();
						ld.getFloatDataMap().put(timeStamp, val);
						break;
					}
					case DOUBLE: {
						double val = in.readDouble();
						ld.getDoubleDataMap().put(timeStamp, val);
						break;
					}
					default: {// String
						String val = in.readUTF();
						ld.getStringDataMap().put(timeStamp, val);
						break;
					}

					}

				}
			} catch (EOFException e) {
				// TODO EOF reached
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}

		// dump
/*
		for (ContextLogData ld : mLogMap.values()) {
			switch (ld.getType()) {
			case INT: {
				Map<Long, Integer> map = ld.getIntDataMap();
				for (Long ts : map.keySet()) {
					System.out.println(ld.getName() + ts + " " + map.get(ts));
				}
				break;
			}
			case LONG: {
				Map<Long, Long> map = ld.getLongDataMap();
				for (Long ts : map.keySet()) {
					System.out.println(ld.getName() + ts + " " + map.get(ts));
				}
				break;
			}
			case FLOAT: {
				Map<Long, Float> map = ld.getFloatDataMap();
				for (Long ts : map.keySet()) {
					System.out.println(ld.getName() + ts + " " + map.get(ts));
				}
				break;
			}
			case DOUBLE: {
				Map<Long, Double> map = ld.getDoubleDataMap();
				for (Long ts : map.keySet()) {
					System.out.println(ld.getName() + ts + " " + map.get(ts));
				}
				break;
			}
			case STRING: {
				Map<Long, String> map = ld.getStringDataMap();
				for (Long ts : map.keySet()) {
					System.out.println(ld.getName() + ts + " " + map.get(ts));
				}
				break;
			}
			}
		}
		*/
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
		// no options available in current version
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
		int typeInt = Integer.decode(typeStr);
		LogType type = LogType.byType(typeInt);
		mLogMap.put(id, new ContextLogData(id, type, name));
	}
	
	private void analyzeData() {
		// sort log by id
		Collection<ContextLogData> lv = mLogMap.values();

		for (ContextLogData ld : lv) {
			switch (ld.getType()) {
			case INT: {
				if(ld.getIntDataMap().isEmpty()) {
					ld.setMinTime(0);
					ld.setMaxTime(0);
					ld.setMinValue(0);
					ld.setMaxValue(0);
					ld.setMaxDiff(0);
				}else{
					ld.setMinTime(ld.getIntDataMap().firstKey());
					ld.setMaxTime(ld.getIntDataMap().lastKey());
					int minVal = ld.getIntDataMap().firstEntry().getValue();
					int maxVal = minVal;
					for (Integer i : ld.getIntDataMap().values()) {
						if (i < minVal) {
							minVal = i;
						}
						if (i > maxVal) {
							maxVal = i;
						}
					}
					ld.setMinValue(minVal);
					ld.setMaxValue(maxVal);
					ld.setMaxDiff(maxVal - minVal);
				}
				break;
			}
			case LONG: {
				if (ld.getLongDataMap().isEmpty()) {
					ld.setMinTime(0);
					ld.setMaxTime(0);
					ld.setMinValue(0);
					ld.setMaxValue(0);
					ld.setMaxDiff(0);
				} else {
					ld.setMinTime(ld.getLongDataMap().firstKey());
					ld.setMaxTime(ld.getLongDataMap().lastKey());
					long minVal = ld.getLongDataMap().firstEntry().getValue();
					long maxVal = minVal;
					for (Long i : ld.getLongDataMap().values()) {
						if (i < minVal) {
							minVal = i;
						}
						if (i > maxVal) {
							maxVal = i;
						}
					}
					ld.setMinValue(minVal);
					ld.setMaxValue(maxVal);
					ld.setMaxDiff(maxVal - minVal);
				}
				break;
			}
			case FLOAT: {
				if (ld.getFloatDataMap().isEmpty()) {
					ld.setMinTime(0);
					ld.setMaxTime(0);
					ld.setMinValue(0);
					ld.setMaxValue(0);
					ld.setMaxDiff(0);
				} else {
					ld.setMinTime(ld.getFloatDataMap().firstKey());
					ld.setMaxTime(ld.getFloatDataMap().lastKey());
					float minVal = ld.getFloatDataMap().firstEntry().getValue();
					float maxVal = minVal;
					for (Float i : ld.getFloatDataMap().values()) {
						if (i < minVal) {
							minVal = i;
						}
						if (i > maxVal) {
							maxVal = i;
						}
					}
					ld.setMinValue(minVal);
					ld.setMaxValue(maxVal);
					ld.setMaxDiff(maxVal - minVal);
				}
				break;
			}
			case DOUBLE: {
				if (ld.getDoubleDataMap().isEmpty()) {
					ld.setMinTime(0);
					ld.setMaxTime(0);
					ld.setMinValue(0);
					ld.setMaxValue(0);
					ld.setMaxDiff(0);
				} else {
					ld.setMinTime(ld.getDoubleDataMap().firstKey());
					ld.setMaxTime(ld.getDoubleDataMap().lastKey());
					double minVal = ld.getDoubleDataMap().firstEntry()
							.getValue();
					double maxVal = minVal;
					for (Double i : ld.getDoubleDataMap().values()) {
						if (i < minVal) {
							minVal = i;
						}
						if (i > maxVal) {
							maxVal = i;
						}
					}
					ld.setMinValue(minVal);
					ld.setMaxValue(maxVal);
					ld.setMaxDiff(maxVal - minVal);
				}
				break;
			}
			case STRING:
				if (ld.getStringDataMap().isEmpty()) {
					ld.setMinTime(0);
					ld.setMaxTime(0);
					ld.setMinValue(0);
					ld.setMaxValue(0);
					ld.setMaxDiff(0);
				} else {
					ld.setMinTime(ld.getStringDataMap().firstKey());
					ld.setMaxTime(ld.getStringDataMap().lastKey());
					ld.setMinValue(0);
					ld.setMaxValue(0);
					ld.setMaxDiff(0);
				}
			}

		}

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

	public long getStartTime() {
		return mStartTime;
	}
}
