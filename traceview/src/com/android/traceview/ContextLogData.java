package com.android.traceview;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import cz.cuni.kacz.contextlogger.LogType;

public class ContextLogData implements TimeLineView.LogRow {

	private final int mId;
	private final String mName;
	private final LogType mType;
	private NavigableMap<Long, Integer> mIntDataMap = null;
	private NavigableMap<Long, Long> mLongDataMap = null;
	private NavigableMap<Long, Float> mFloatDataMap = null;
	private NavigableMap<Long, Double> mDoubleDataMap = null;
	private NavigableMap<Long, String> mStringDataMap = null;

	private double minValue;
	private double maxValue;
	private double maxDiff;

	private double minTime;
	private double maxTime;

	public double getMaxDiff() {
		return maxDiff;
	}

	public void setMaxDiff(double maxDiff) {
		this.maxDiff = maxDiff;
	}
	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	public double getMinTime() {
		return minTime;
	}

	public void setMinTime(double minTime) {
		this.minTime = minTime;
	}

	public double getMaxTime() {
		return maxTime;
	}

	public void setMaxTime(double maxTime) {
		this.maxTime = maxTime;
	}

	ContextLogData(int id, LogType type, String name) {
		mId = id;
		mName = name;
		mType = type;
		switch (mType) {
		case LONG:
			mLongDataMap = new TreeMap<Long, Long>();
			break;
		case INT:
			mIntDataMap = new TreeMap<Long, Integer>();
			break;
		case DOUBLE:
			mDoubleDataMap = new TreeMap<Long, Double>();
			break;
		case FLOAT:
			mFloatDataMap = new TreeMap<Long, Float>();
			break;
		default: // String
			mStringDataMap = new TreeMap<Long, String>();
			break;
		}
	}

	NavigableMap<Long, Integer> getIntDataMap() {
		return mIntDataMap;
	}

	NavigableMap<Long, Long> getLongDataMap() {
		return mLongDataMap;
	}

	NavigableMap<Long, Float> getFloatDataMap() {
		return mFloatDataMap;
	}

	NavigableMap<Long, Double> getDoubleDataMap() {
		return mDoubleDataMap;
	}

	NavigableMap<Long, String> getStringDataMap() {
		return mStringDataMap;
	}

	Integer getIntValueAt(long timestamp) {
		if (mIntDataMap == null) {
			return null;
		}
		Entry<Long, Integer> intEntry = mIntDataMap.floorEntry(timestamp);
		if (intEntry != null) {
			return intEntry.getValue();
		}
		return null;
	}

	Long getLongValueAt(long timestamp) {
		if (mLongDataMap == null) {
			return null;
		}
		Entry<Long, Long> longEntry = mLongDataMap.floorEntry(timestamp);
		if (longEntry != null) {
			return longEntry.getValue();
		}
		return null;
	}

	Float getFloatValueAt(long timestamp) {
		if (mFloatDataMap == null) {
			return null;
		}
		Entry<Long, Float> ceilingFloatEntry = mFloatDataMap
				.ceilingEntry(timestamp);
		Entry<Long, Float> floorFloatEntry = mFloatDataMap
				.floorEntry(timestamp);
		if (ceilingFloatEntry != null && floorFloatEntry != null) {
			float floatValue = floorFloatEntry.getValue()
					+ (float) (timestamp - floorFloatEntry.getKey())
					/ (float) (ceilingFloatEntry.getKey() - floorFloatEntry
							.getKey())
					* (ceilingFloatEntry.getValue() - floorFloatEntry
							.getValue());
			return floatValue;
		}
		return null;
	}

	Double getDoubleValueAt(long timestamp) {
		if (mDoubleDataMap == null) {
			return null;
		}
		Entry<Long, Double> ceilingDoubleEntry = mDoubleDataMap
				.ceilingEntry(timestamp);
		Entry<Long, Double> floorDoubleEntry = mDoubleDataMap
				.floorEntry(timestamp);
		if (ceilingDoubleEntry != null && floorDoubleEntry != null) {
			double doubleValue = floorDoubleEntry.getValue()
					+ (double) (timestamp - floorDoubleEntry.getKey())
					/ (double) (ceilingDoubleEntry.getKey() - floorDoubleEntry
							.getKey()) * ceilingDoubleEntry.getValue();
			return doubleValue;
		}
		return null;
	}

	String getStringValueAt(long timestamp) {
		if (mStringDataMap == null) {
			return null;
		}
		Entry<Long, String> stringEntry = mStringDataMap.floorEntry(timestamp);
		if (stringEntry != null) {
			return stringEntry.getValue();
		}
		return null;
	}

	@Override
	public int getId() {
		return mId;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public LogType getType() {
		return mType;
	}

}