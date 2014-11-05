package com.halfbit.tinybus;

import android.content.Context;

public abstract class Events {

	protected Bus bus;
	
	protected abstract void onStarted(Context context);
	protected abstract void onStopped(Context context);
	
}