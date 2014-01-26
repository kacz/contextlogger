package com.android.traceview;

import java.util.List;
import java.util.Map;

public class ProblemOccurence {

	private long time;
	private boolean inside;
	private Map<String,Object> contextValues;
	
	public ProblemOccurence(long timeStamp,boolean inside, Map<String,Object> values) {
		this.time = timeStamp;
		this.inside = inside;
		this.contextValues = values;
	}

	public long getTime() {
		return time;
	}

	public boolean isInside() {
		return inside;
	}

	public Map<String, Object> getContextValues() {
		return contextValues;
	}
}
