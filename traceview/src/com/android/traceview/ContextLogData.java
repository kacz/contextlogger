package com.android.traceview;

public class ContextLogData implements TimeLineView.LogRow {

	private final int mId;
	private final String mName;
	private final int mType;

	ContextLogData(int id, int type, String name) {
		mId = id;
		mName = name;
		mType = type;
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
	public int getType() {
		return mType;
	}

}