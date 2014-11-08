package com.halfbit.tinybus;

import java.util.WeakHashMap;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

class TinyBusDepot implements ActivityLifecycleCallbacks {

	private static final boolean DEBUG = false;
	private static final String TAG = TinyBusDepot.class.getSimpleName();
	private static TinyBusDepot INSTANCE;
	
	public static TinyBusDepot get(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new TinyBusDepot(context);
		}
		return INSTANCE;
	}

	private final WeakHashMap<Context, TinyBus> mBuses;
	private BackgroundDispatcher mBackgroundDispatcher;
	
	public TinyBusDepot(Context context) {
		mBuses = new WeakHashMap<Context, TinyBus>();
		final Application app = (Application) context.getApplicationContext();
		app.registerActivityLifecycleCallbacks(this);
	}

	public TinyBus createBusInContext(Context context) {
		final TinyBus bus = new TinyBus(context);
		mBuses.put(context, bus);
		return bus;
	}
	
	public TinyBus getBusInContext(Context context) {
		return mBuses.get(context);
	}
	
	synchronized BackgroundDispatcher getBackgroundDispatcher() {
		if (mBackgroundDispatcher == null) {
			mBackgroundDispatcher = new BackgroundDispatcher();
		}
		return mBackgroundDispatcher;
	}
	
	@Override
	public void onActivityStarted(Activity activity) {
		TinyBus bus = mBuses.get(activity);
		if (bus != null) {
			bus.dispatchOnStartWireable(activity);
		}
		if (DEBUG) Log.d(TAG, " #### STARTED, bus count: " + mBuses.size());
	}
	
	@Override
	public void onActivityStopped(Activity activity) {
		TinyBus bus = mBuses.get(activity);
		if (bus != null) {
			bus.dispatchOnStopWireable(activity);
		}
		if (DEBUG) Log.d(TAG, " #### STOPPED, bus count: " + mBuses.size());
	}
	
	@Override
	public void onActivityDestroyed(Activity activity) {
		mBuses.remove(activity);
		if (DEBUG) Log.d(TAG, " #### DESTROYED, bus count: " + mBuses.size());
	}
	
	@Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
	@Override public void onActivityResumed(Activity activity) { }
	@Override public void onActivityPaused(Activity activity) { }
	@Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }

}
