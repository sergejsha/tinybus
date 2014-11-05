package com.halfbit.tinybus;

import java.util.WeakHashMap;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

class TrolleyBusDepot implements ActivityLifecycleCallbacks {

	private static final String TAG = TrolleyBusDepot.class.getSimpleName();
	private static final boolean DEBUG = false;
	
	private static TrolleyBusDepot INSTANCE;
	
	public static TrolleyBusDepot get(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new TrolleyBusDepot(context);
		}
		return INSTANCE;
	}

	private final WeakHashMap<Activity, TrolleyBus> mBuses;
	
	public TrolleyBusDepot(Context context) {
		mBuses = new WeakHashMap<Activity, TrolleyBus>();
		final Application app = (Application) context.getApplicationContext();
		app.registerActivityLifecycleCallbacks(this);
	}

	public TrolleyBus create(Activity context) {
		TrolleyBus bus = mBuses.get(context);
		if (bus != null) {
			throw new IllegalArgumentException("Bus has already been created with the context. "
					+ "Use TinyBus.from(Context) method to access created bus instance. "
					+ "Context: " + context);
		}
		bus = new TrolleyBus();
		mBuses.put(context, bus);
		return bus;
	}
	
	public TrolleyBus getBus(Activity context) {
		final TrolleyBus bus = mBuses.get(context);
		if (bus == null) {
			throw new IllegalArgumentException("Bus has not yet been created in the context. "
					+ "Use TinyBus.create(Context) method inside Activity.onCreate() method "
					+ "to create bus instance first. Context: " + context);
		}
		return bus;
	}
	
	@Override
	public void onActivityStarted(Activity activity) {
		TrolleyBus bus = mBuses.get(activity);
		if (bus != null) {
			bus.dispatchOnStart(activity);
		}
		if (DEBUG) Log.d(TAG, " #### STARTED, bus count: " + mBuses.size());
	}
	
	@Override
	public void onActivityStopped(Activity activity) {
		TrolleyBus bus = mBuses.get(activity);
		if (bus != null) {
			bus.dispatchOnStop(activity);
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
