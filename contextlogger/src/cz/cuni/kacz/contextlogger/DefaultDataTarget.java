package cz.cuni.kacz.contextlogger;

import java.io.Serializable;

import android.content.Context;

public abstract class DefaultDataTarget implements DataTarget, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Context mContext;

	public void initCtx(Context ctx) {
		mContext = ctx;
	}
}
