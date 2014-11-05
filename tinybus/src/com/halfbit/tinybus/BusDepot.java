package com.halfbit.tinybus;

import java.util.WeakHashMap;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

class BusDepot implements ActivityLifecycleCallbacks {

	private static final String TAG = BusDepot.class.getSimpleName();
	private static final boolean DEBUG = true;
	
	private static BusDepot INSTANCE;
	
	public static BusDepot get(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new BusDepot(context);
		}
		return INSTANCE;
	}

	private final WeakHashMap<Context, TinyBus> mBuses;
	
	public BusDepot(Context context) {
		mBuses = new WeakHashMap<Context, TinyBus>();
		final Application app = (Application) context.getApplicationContext();
		app.registerActivityLifecycleCallbacks(this);
	}

	public TinyBus create(Context context) {
		TinyBus bus = mBuses.get(context);
		if (bus != null) {
			throw new IllegalArgumentException("Bus has already been created with the context. "
					+ "Use TinyBus.from(Context) method to access created bus instance. "
					+ "Context: " + context);
		}
		bus = new TinyBus();
		mBuses.put(context, bus);
		return bus;
	}
	
	public TinyBus getBus(Context context) {
		final TinyBus bus = mBuses.get(context);
		if (bus == null) {
			throw new IllegalArgumentException("Bus has not yet been created in the context. "
					+ "Use TinyBus.create(Context) method inside Activity.onCreate() method "
					+ "to create bus instance first. Context: " + context);
		}
		return bus;
	}
	
	@Override
	public void onActivityStarted(Activity activity) {
		TinyBus bus = mBuses.get(activity);
		if (bus != null) {
			bus.dispatchOnStart(activity);
		}
		if (DEBUG) Log.d(TAG, " #### STARTED, bus count: " + mBuses.size());
	}
	
	@Override
	public void onActivityStopped(Activity activity) {
		TinyBus bus = mBuses.get(activity);
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
