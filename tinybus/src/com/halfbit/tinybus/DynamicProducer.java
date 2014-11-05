package com.halfbit.tinybus;

import android.content.Context;

public abstract class DynamicProducer {

	protected boolean isStarted;
	protected Bus bus;
	
	protected abstract void onStart(Context context);
	protected abstract void onStop(Context context);
	
}