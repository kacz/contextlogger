package com.android.traceview;

public abstract class AbstractProblemDefinition implements ProblemDefinition {

	protected String logName;
	protected ProblemConstraint constraint;
	protected String functionName;
	
	@Override
	public String getLogName() {
		return logName;
	}

	@Override
	public ProblemConstraint getConstraint() {
		return constraint;
	}

	@Override
	public String getFunction() {
		return functionName;
	}
	
}
