package cz.cuni.kacz.contextlogger;

import java.io.Serializable;

import android.content.Context;

/**
 * Default implementation of the DataTarget interface. Defines a final method
 * for saving a reference to the application context.
 * 
 * @author kacz
 * 
 */
public abstract class DefaultDataTarget implements DataTarget, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	Context mContext;

	@Override
	public final void initCtx(Context ctx) {
		mContext = ctx;
	}
}
