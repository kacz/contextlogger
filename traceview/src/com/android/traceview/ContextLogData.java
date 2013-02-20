package com.android.traceview;

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