package com.android.traceview;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader encapsulating TraceReader and ContextLogReader.
 * Provides methods to access information. 
 * @author kacz
 *
 */
public class CombinedReader {
	private TraceReader mTraceReader;
	private ContextLogReader mLogReader;
	
	private ProfileProvider mProfileProvider;
	private boolean mLogReaderAvailable = false;
	
	/**
	 * Constructs new instance encapsulating the provided trace and log readers.
	 * @param traceReader {@link TraceReader}, must not be null
	 * @param logReader {@link ContextLogReader}
	 */
	public CombinedReader(TraceReader traceReader, ContextLogReader logReader) {
		if(traceReader == null) {
			throw new IllegalArgumentException();
		}
		
		mTraceReader = traceReader;
		mLogReader = logReader;
		if(mLogReader != null) {
			mLogReaderAvailable = true;
		}
		
		mProfileProvider = traceReader.getProfileProvider();
	}
	
	public CombinedReader(String baseTraceName) throws IllegalArgumentException {
		if(baseTraceName == null) {
			throw new IllegalArgumentException();
		}
		
		String traceName = baseTraceName;
        File file = new File(traceName);
        if (file.exists() && file.isDirectory()) {
            throw new IllegalArgumentException();
        } else {
            // If the filename as given doesn't exist...
            if (!file.exists()) {
                // Try appending .trace.
                if (new File(traceName + ".trace").exists()) {
                    traceName = traceName + ".trace";
                // Otherwise, give up.
                } else {
                	throw new IllegalArgumentException();
                }
            }
			if (baseTraceName.contains(".trace")) {
				baseTraceName = baseTraceName.replace(".trace", "");
			}

			String contextLogName = baseTraceName + ".clog";
            try {
            	mTraceReader = new DmTraceReader(traceName, false);
            } catch (IOException e) {
            	throw new IllegalArgumentException();
            }
			if (new File(contextLogName).exists()) {
				try {
					mLogReader = new ContextLogReader(contextLogName);
				} catch (IOException e) {
					throw new IllegalArgumentException();
				}
			}
        }

        mTraceReader.getTraceUnits().setTimeScale(TraceUnits.TimeScale.MilliSeconds);
        mProfileProvider = mTraceReader.getProfileProvider();
        mLogReaderAvailable = true;
	}
	
	/**
	 * Checks whether log reader is available.
	 * @return true, if available.
	 */
	public boolean isLogReaderAvailable() {
		return mLogReaderAvailable;
	}
	
	/**
	 * Returns the difference between start times of trace and log readers,
	 * or 0 if log reader is not available.
	 */
	public long getStartDiff() {
		if(mLogReaderAvailable) {
			return mTraceReader.getStartTime() - mLogReader.getStartTime();
		} else {
			return 0;
		}
	}
	
	public TraceReader getTraceReader() {
		return mTraceReader;
	}
	
	public ContextLogReader getLogReader() {
		return mLogReader;
	}
	
	/**
	 * Query trace reader for functions with matching signature.
	 * @param query Signature of function.
	 * @return Array containing functions matching the query.
	 */
	public MethodData[] findAllMethodDataByName(String query) {
    	List<MethodData> mds = new ArrayList<MethodData>(); 
    	
		MethodData md = mProfileProvider.findMatchingName(query);

		while (md != null) {
			mds.add(md);
			md = mProfileProvider.findNextMatchingName(query);
		}
    	
		return mds.toArray(new MethodData[0]);
    }

//    public void findMethodDataByName(String query) {
//        MethodData md = mProfileProvider.findMatchingName(query);
//    }
//
//    public void findNextMethodDataByName(String query) {
//        MethodData md = mProfileProvider.findNextMatchingName(query);
//    }
}
