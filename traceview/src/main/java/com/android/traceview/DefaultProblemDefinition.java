package com.android.traceview;

public class DefaultProblemDefinition extends AbstractProblemDefinition {

	public DefaultProblemDefinition(String logName, ProblemConstraint constraint, String function) {
		if((logName == null || constraint == null) && function == null) {
			throw new IllegalArgumentException();
		}
		
		this.logName = logName;
		this.constraint = constraint;
		this.functionName = function;
	}
}
