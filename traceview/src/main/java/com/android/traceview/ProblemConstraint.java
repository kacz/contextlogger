package com.android.traceview;

import com.google.common.base.Objects;
import com.sun.org.apache.xml.internal.utils.IntVector;

import cz.cuni.kacz.contextlogger.LogType;

public class ProblemConstraint {
	Relation relation;
	
	LogType type;
	
	Double doubleValue;
	Long longValue;
	String stringValue;
	
	ProblemConstraint (Relation r, LogType t, double v) {
		if(t != LogType.DOUBLE && t != LogType.FLOAT) {
			throw new IllegalArgumentException();
		}
		
		this.relation = r;
		this.type = t;
		this.doubleValue = v;
	}
	
	ProblemConstraint (Relation r, LogType t, long v) {
		if(t != LogType.LONG && t != LogType.INT) {
			throw new IllegalArgumentException();
		}
		
		this.relation = r;
		this.type = t;
		this.longValue = v;
	}
	
	ProblemConstraint (Relation r, LogType t, String v) {
		if(t != LogType.STRING) {
			throw new IllegalArgumentException();
		}
		
		this.relation = r;
		this.type = t;
		this.stringValue = v;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof ProblemConstraint))
			return false;
		
		ProblemConstraint other = (ProblemConstraint) obj;
		if(this.relation != other.relation)
			return false;
		
		if(this.type != other.type) 
			return false;
		
		switch(this.type) {
		case INT:
		case LONG:
			if(this.longValue != other.longValue)
				return false;
			break;
		case FLOAT:
		case DOUBLE:
			if(this.doubleValue != other.doubleValue)
				return false;
			break;
		case STRING:
			if(!Objects.equal(this.stringValue,other.stringValue))
				return false;
			break;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		if(relation != null)
			hash += relation.hashCode();
		
		switch(type) {
		case INT:
		case LONG:
			hash += longValue;
			break;
		case FLOAT:
		case DOUBLE:
			hash += Math.round(doubleValue);
			break;
		case STRING:
			hash += stringValue.hashCode();
			break;
		}
		return hash;
	}
	
	public enum Relation {
		LESS_THAN,
		LESS_THAN_OR_EQUALS,
		EQUALS,
		NOT_EQUALS,
		GREATER_THAN,
		GREATER_THAN_OR_EQUALS;
		
		public static Relation value(String v) throws IllegalArgumentException {
			if (">".equals(v)) {
				return GREATER_THAN;
			}
			if (">=".equals(v)) {
				return GREATER_THAN_OR_EQUALS;
			}
			if ("<".equals(v)) {
				return LESS_THAN;
			}
			if ("<=".equals(v)) {
				return LESS_THAN_OR_EQUALS;
			}
			if ("<>".equals(v)) {
				return NOT_EQUALS;
			}
			if ("=".equals(v)) {
				return EQUALS;
			}
			throw new IllegalArgumentException(v);
		}
	}
}
