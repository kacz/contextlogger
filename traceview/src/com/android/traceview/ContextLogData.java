package com.android.traceview;

import java.util.LinkedHashMap;
import java.util.Map;

import cz.cuni.kacz.contextlogger.LogType;

public class ContextLogData implements TimeLineView.LogRow {

	private final int mId;
	private final String mName;
	private final LogType mType;
	private Map<Long, Integer> mIntDataMap = null;
	private Map<Long, Long> mLongDataMap = null;
	private Map<Long, Float> mFloatDataMap = null;
	private Map<Long, Double> mDoubleDataMap = null;
	private Map<Long, String> mStringDataMap = null;

	ContextLogData(int id, LogType type, String name) {
		mId = id;
		mName = name;
		mType = type;
		switch (mType) {
		case LONG:
			mLongDataMap = new LinkedHashMap<Long, Long>();
			break;
		case INT:
			mIntDataMap = new LinkedHashMap<Long, Integer>();
			break;
		case DOUBLE:
			mDoubleDataMap = new LinkedHashMap<Long, Double>();
			break;
		case FLOAT:
			mFloatDataMap = new LinkedHashMap<Long, Float>();
			break;
		default: // String
			mStringDataMap = new LinkedHashMap<Long, String>();
			break;
		}
	}

	Map<Long, Integer> getIntDataMap() {
		return mIntDataMap;
	}

	Map<Long, Long> getLongDataMap() {
		return mLongDataMap;
	}

	Map<Long, Float> getFloatDataMap() {
		return mFloatDataMap;
	}

	Map<Long, Double> getDoubleDataMap() {
		return mDoubleDataMap;
	}

	Map<Long, String> getStringDataMap() {
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