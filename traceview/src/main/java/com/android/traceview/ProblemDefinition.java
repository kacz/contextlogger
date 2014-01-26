package com.android.traceview;

public interface ProblemDefinition {
	
	String BRB_FUNCTION_NAME = "cz/cuni/kacz/contextlogger/ContextLogger.brb ()V";
	
	String getLogName();
	ProblemConstraint getConstraint();
	String getFunction();
}
