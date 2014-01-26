package com.android.traceview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Map.Entry;
import java.util.Set;

import com.android.traceview.IntervalSelection.Action;
import com.android.traceview.ProblemConstraint.Relation;
import com.android.traceview.TimeLineView.Record;
import com.google.common.base.Objects;
import com.sun.org.apache.bcel.internal.classfile.InnerClass;

/**
 * Class representing a Problem with given definition, on given inputs.
 * @author kacz
 *
 */
public class Problem {
	private ProblemDefinition problemDefinition;
	private final CombinedReader reader;
	private final ProfileProvider profileProvider;
	private ContextLogData mProblemLog;
	private final ArrayList<Record> mTraceRecords;
	
	
	private List<Long> insideTimestamps;
	private List<Long> outsideTimestamps;
	private List<Long> mTimestamps;
	private List<IntervalSelection> mIntervals;
	private List<ProblemOccurence> mOccurences;
	
	/**
	 * Constructs a new instance with provided problem definition and reader.
	 * @param def Definition of the problem.
	 * @param reader Reader, must not be null and must contain context log reader.
	 */
	public Problem(ProblemDefinition def, CombinedReader reader) {
		if(reader == null || !reader.isLogReaderAvailable()) {
			throw new IllegalArgumentException();
		}
		
		this.reader = reader;
		this.profileProvider = new ProfileProvider(reader.getTraceReader());
		this.mTraceRecords = reader.getTraceReader().getThreadTimeRecords();
		
		setProblemDefinition(def);
	};
	
	public void setProblemDefinition(ProblemDefinition def) {
		if(problemDefinition == null) {
			mTimestamps = null;
			insideTimestamps = null;
			outsideTimestamps = null;
			mOccurences = null;
			mIntervals = null;
		}
		
		if(def == null) {
			mTimestamps = null;
			insideTimestamps = null;
			outsideTimestamps = null;
			mOccurences = null;
			mIntervals = null;
			
			problemDefinition = def;
			return;
		} else if(this.problemDefinition != null) {
			if(!Objects.equal(this.problemDefinition.getFunction(),def.getFunction())) {
				mTimestamps = null;
				insideTimestamps = null;
				outsideTimestamps = null;
				mOccurences = null;
			}
			
			if(!Objects.equal(this.problemDefinition.getLogName(), def.getLogName()) 
					|| !Objects.equal(this.problemDefinition.getConstraint(), def.getConstraint())) {
				mIntervals = null;
				insideTimestamps = null;
				outsideTimestamps = null;
				mOccurences = null;
			}
		}
		
		this.problemDefinition = def;
		
		if(def.getLogName() != null && def.getConstraint() != null) {
			Map<String, ContextLogData> logMapByName = reader.getLogReader().getLogMapByName();
			mProblemLog = logMapByName.get(def.getLogName());
			if(mProblemLog == null) {
				//TODO: throw something else
				throw new IllegalArgumentException();
			}
			if(mProblemLog.getType() != def.getConstraint().type) {
				//TODO: throw some exception
			}
		}
	}
	
	List<Long> getInsideTimestamps() {
		if(insideTimestamps == null) {
			generateLocalTimestamps();
		}
		return insideTimestamps;
	}
	
	List<Long> getOutsideTimestamps() {
		if(outsideTimestamps == null) {
			generateLocalTimestamps();
		}
		return outsideTimestamps;
	}

	List<Long> getTimestamps() {
		if(mTimestamps == null) {
			generateTimestamps();
		}
		return mTimestamps;
	}
	
	List<IntervalSelection> getIntervals() {
		if(mIntervals == null) {
			generateIntervals();
		}
		return mIntervals;
	}
	
	List<ProblemOccurence> getOccurences() {
		if(mOccurences == null) {
			generateOccurences();
		}
		return mOccurences;
	}
	
	private void generateTimestamps() {
		if(mIntervals == null) {
			generateIntervals();
		}
		
		mTimestamps = new ArrayList<Long>();
		insideTimestamps = new ArrayList<Long>();
		outsideTimestamps = new ArrayList<Long>();
		
		if(problemDefinition == null) {
			return;
		}
		MethodData md = profileProvider.findMatchingName(problemDefinition.getFunction());

		if (md != null) {
			for (Record rec : mTraceRecords) {
				if (rec.block.getMethodData() == md) {
					long timeStamp = rec.block.getStartTime();
					mTimestamps.add(timeStamp);
					if(isInside(timeStamp,mIntervals)) {
						insideTimestamps.add(timeStamp);
					} else {
						outsideTimestamps.add(timeStamp);
					}
				}
			}
		}
	};

	private void generateLocalTimestamps() {
		if(mTimestamps == null) {
			generateTimestamps();
			return;
		}
		
		if(mIntervals == null) {
			generateIntervals();
		}
		
		insideTimestamps = new ArrayList<Long>();
		outsideTimestamps = new ArrayList<Long>();
		
		for(long ts: mTimestamps) {
			if(isInside(ts, mIntervals)) {
				insideTimestamps.add(ts);
			} else {
				outsideTimestamps.add(ts);
			}
		}
	}
	
	private void generateIntervals() {
		if(problemDefinition == null || problemDefinition.getLogName() == null || problemDefinition.getConstraint() == null) {
			mIntervals = new ArrayList<IntervalSelection>();
			return;
		}
		
		ArrayList<IntervalSelection> intervals = null;
		
		switch(problemDefinition.getConstraint().type) {
		case INT:
		case LONG:
			intervals = generateIntervalsForInts();
			break;
		case FLOAT:
		case DOUBLE:
			intervals = generateIntervalsForFloats();
			break;
		case STRING:
			intervals = generateIntervalsForStrings();
		}
		
		mIntervals = intervals;
		
		insideTimestamps = new ArrayList<Long>();
		outsideTimestamps = new ArrayList<Long>();
	};
	
	private void generateOccurences() {
		if(mTimestamps == null) {
			generateTimestamps();
		}
		
		if(mIntervals == null) {
			generateIntervals();
		}
		
		List<ProblemOccurence> occurrences = new LinkedList<ProblemOccurence>();
		
		Set<Entry<String,ContextLogData>> entries = reader.getLogReader().getLogMapByName().entrySet();
		for(long ts: mTimestamps) {
			Map<String, Object> values = new HashMap<String, Object>();
			long logTs = ts + reader.getStartDiff();
			
			for(Entry<String, ContextLogData> entry: entries) {
				String logname = entry.getKey();
				ContextLogData log = entry.getValue();
				switch(log.getType()) {
				case INT:
					Integer valueInt = log.getIntValueAt(logTs);
					values.put(logname, valueInt);
					break;
				case LONG:
					Long valueLong = log.getLongValueAt(logTs);
					values.put(logname, valueLong);
					break;
				case FLOAT:
					Float valueFloat = log.getFloatValueAt(logTs);
					values.put(logname, valueFloat);
					break;
				case DOUBLE:
					Double valueDouble = log.getDoubleValueAt(logTs);
					values.put(logname, valueDouble);
					break;
				case STRING:
					String valueString = log.getStringValueAt(logTs);
					values.put(logname, valueString);
					break;
				}
			}
			
			boolean inside = isInside(ts, mIntervals);
			occurrences.add(new ProblemOccurence(ts,inside,values));
			
		}
		
		mOccurences = occurrences;
	};
	
	private ArrayList<IntervalSelection> generateIntervalsForStrings() {
		ArrayList<IntervalSelection> intervals = new ArrayList<IntervalSelection>();

		long constraint = problemDefinition.getConstraint().longValue;
		Relation rel = problemDefinition.getConstraint().relation;
		
		boolean inside = false;
		long begining = -1;
		long end;
		boolean equals = (rel == Relation.EQUALS);

		NavigableMap<Long, String> logMap = mProblemLog.getStringDataMap();
		for (Entry<Long, String> e : logMap.entrySet()) {
			if (inside == false && e.getValue().equals(constraint) == equals) {
				begining = e.getKey();
				inside = true;
				continue;
			}
			if (inside == true && e.getValue().equals(constraint) == equals) {
				continue;
			}
			if (inside == true && e.getValue().equals(constraint) != equals) {
				end = e.getKey();
				inside = false;
				intervals.add(new IntervalSelection(Action.Highlight, begining,
						end));
				begining = -1;
				continue;
			}
		}
		if (begining != -1) {
			intervals.add(new IntervalSelection(Action.Highlight, begining,
					Long.MAX_VALUE));
		}

		return intervals;
	}

	private ArrayList<IntervalSelection> generateIntervalsForFloats() {
		ArrayList<IntervalSelection> intervals = new ArrayList<IntervalSelection>();

		boolean inside = false;
		long begining = -1;
		long end;
		long lastKey = -1;
		double lastValue = -1;

		NavigableMap<Long, ? extends Number> logMap;
		switch (mProblemLog.getType()) {
		case FLOAT:
			logMap = mProblemLog.getFloatDataMap();
			break;
		case DOUBLE:
			logMap = mProblemLog.getDoubleDataMap();
			break;
		default:
			//TODO: throw some exc
			return intervals;
		}

		double constraint = problemDefinition.getConstraint().doubleValue;
		Relation rel = problemDefinition.getConstraint().relation;
		
		for (Entry<Long, ? extends Number> e : logMap.entrySet()) {
			double val = e.getValue().doubleValue();
			// start of an interval
			if (inside == false && eval(val, rel, constraint) == true) {
				if (lastKey == -1) {
					begining = e.getKey();
				} else {
					begining = (long) (Math.abs(constraint - lastValue)
							/ Math.abs(val - lastValue)
							* Math.abs(e.getKey() - lastKey) + lastKey);
				}

				inside = true;
				lastKey = e.getKey();
				lastValue = val;
				continue;
			}
			// we are inside of an interval
			if (inside == true && eval(val, rel, constraint) == true) {
				lastKey = e.getKey();
				lastValue = val;
				continue;
			}
			// we leave the interval
			if (inside == true && eval(val, rel, constraint) == false) {
				end = (long) (Math.abs(constraint - lastValue)
						/ Math.abs(val - lastValue)
						* Math.abs(e.getKey() - lastKey) + lastKey);
				inside = false;
				intervals.add(new IntervalSelection(Action.Highlight, begining,
						end));
				begining = -1;

				lastKey = e.getKey();
				lastValue = val;
				continue;
			}
			if (inside == false && eval(val, rel, constraint) == false) {
				lastKey = e.getKey();
				lastValue = val;
				continue;
			}

		}

		if (begining != -1) {
			intervals.add(new IntervalSelection(Action.Highlight, begining,
					Long.MAX_VALUE));
		}

		return intervals;
	}

	private ArrayList<IntervalSelection> generateIntervalsForInts() {
		ArrayList<IntervalSelection> intervals = new ArrayList<IntervalSelection>();
		
		boolean inside = false;
		long begining = -1;
		long end;
		NavigableMap<Long, ? extends Number> logMap;
		switch (mProblemLog.getType()) {
		case INT:
			logMap = mProblemLog.getIntDataMap();
			break;
		case LONG:
			logMap = mProblemLog.getLongDataMap();
			break;
		default:
			//TODO: throw some exc
			return intervals;
		}
		
		long constraint = problemDefinition.getConstraint().longValue;
		Relation rel = problemDefinition.getConstraint().relation;
		
		for(Entry<Long,? extends Number> e : logMap.entrySet()) {
			Long val = e.getValue().longValue();
			// start of an interval
			if (inside == false && eval(val, rel, constraint) == true) {
				begining = e.getKey();
				inside = true;
				continue;
			}
			// we are inside of an interval
			if (inside == true && eval(val, rel, constraint) == true) {
				continue;
			}
			// we leave the interval
			if (inside == true && eval(val, rel, constraint) == false) {
				end = e.getKey();
				inside = false;
				intervals.add(new IntervalSelection(Action.Highlight,begining,end));
				begining = -1;
				continue;
			}
			if (inside == false && eval(val, rel, constraint) == false) {
				continue;
			}

		}
		
		if (begining != -1) {
			intervals.add(new IntervalSelection(Action.Highlight, begining,
					Long.MAX_VALUE));
		}
		
		return intervals;
	}
	
	public Map<String, ContextLogData> getLogMapByName() {
		return reader.getLogReader().getLogMapByName();
	}

	private boolean isInside(long timeStamp, List<IntervalSelection> intervals) {
		if(intervals == null) {
			return true;
		}
	
		if(intervals.isEmpty()) {
			return false;
		}
		
		for(IntervalSelection interval: intervals) {
			if(timeStamp + reader.getStartDiff() >= interval.getmStart() &&
					timeStamp + reader.getStartDiff() <= interval.getmEnd() ) {
				return true;
			}
		}
		return false;
	}
	
	private boolean eval(long a, Relation rel, long b) {
		switch (rel) {
		case EQUALS:
			return a == b;
		case GREATER_THAN:
			return a > b;
		case GREATER_THAN_OR_EQUALS:
			return a >= b;
		case LESS_THAN:
			return a < b;
		case LESS_THAN_OR_EQUALS:
			return a <= b;
		case NOT_EQUALS:
			return a != b;
		default:
			return false;
		}
	}

	private boolean eval(double a, Relation rel, double b) {
		switch (rel) {
		case EQUALS:
			return a == b;
		case GREATER_THAN:
			return a > b;
		case GREATER_THAN_OR_EQUALS:
			return a >= b;
		case LESS_THAN:
			return a < b;
		case LESS_THAN_OR_EQUALS:
			return a <= b;
		case NOT_EQUALS:
			return a != b;
		default:
			return false;
		}
	}
}
