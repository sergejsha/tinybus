package com.halfbit.tinybus.impl;

import java.util.WeakHashMap;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import com.halfbit.tinybus.TinyBus;

public class TinyBusDepot implements ActivityLifecycleCallbacks {

	private static final String TAG = TinyBusDepot.class.getSimpleName();
	private static final boolean DEBUG = false;
	
	private static final String KEY_BUS_ID = "com.halfbit.tinybus.id";
	private static TinyBusDepot INSTANCE;
	
	public static TinyBusDepot get(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new TinyBusDepot(context);
		}
		return INSTANCE;
	}

	private BackgroundDispatcher mBackgroundDispatcher;
	
	private TinyBusDepot(Context context) {
		final Application app = (Application) context.getApplicationContext();
		app.registerActivityLifecycleCallbacks(this);
	}

	//-- bus management

	private final WeakHashMap<Context, TinyBus> mBuses = new WeakHashMap<Context, TinyBus>();
	private final SparseArray<TinyBus> mTransientBuses = new SparseArray<TinyBus>(3);
	private int mNextTransientBusId;
	
	public TinyBus createBusInContext(Context context) {
		final TinyBus bus = new TinyBus(context);
		mBuses.put(context, bus);
		return bus;
	}
	
	public TinyBus getBusInContext(Context context) {
		return mBuses.get(context);
	}
	
	public synchronized BackgroundDispatcher getBackgroundDispatcher() {
		if (mBackgroundDispatcher == null) {
			mBackgroundDispatcher = new BackgroundDispatcher();
		}
		return mBackgroundDispatcher;
	}
	
	@Override
	public void onActivityStarted(Activity activity) {
		TinyBus bus = mBuses.get(activity);
		if (bus != null) {
			
			// if an activity was stopped but not destroyed,
			// then we have to remove bus from transient state
			int transientBusIndex = mTransientBuses.indexOfValue(bus);
			if (transientBusIndex > -1) {
				mTransientBuses.removeAt(transientBusIndex);
			}
			
			bus.onStart();
		}
		if (DEBUG) Log.d(TAG, " ### STARTED, bus count: " + mBuses.size());
	}
	
	@Override
	public void onActivityStopped(Activity activity) {
		TinyBus bus = mBuses.get(activity);
		if (bus != null) {
			bus.onStop();
		}
		if (DEBUG) Log.d(TAG, " ### STOPPED, bus count: " + mBuses.size());
	}
	
	@Override
	public void onActivityDestroyed(Activity activity) {
		TinyBus bus = mBuses.remove(activity);
		if (bus != null && !activity.isChangingConfigurations()) {
			bus.onDestroy();
			if (DEBUG) {
				Log.d(TAG, " ### destroying bus: " + bus);
			}
		}
		if (DEBUG) {
			Log.d(TAG, " ### onDestroy() " + activity +
				", active buses: " + mBuses.size() + 
				", transient buses: " + mTransientBuses.size());
		}
	}
	
	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			// try to restore bus from transient state
			
			final int busId = savedInstanceState.getInt(KEY_BUS_ID, -1);
			if (busId > -1) {
				final TinyBus bus = mTransientBuses.get(busId);
				if (DEBUG) {
					if (bus == null) {
						throw new IllegalStateException("Transient bus not found, id:" + busId);
					}
				}
				
				mTransientBuses.delete(busId);
				bus.assignContext(activity);
				mBuses.put(activity, bus);
				if (DEBUG) {
					Log.d(TAG, " ### onCreated(), bus restored for " + activity + 
							", busId: " + busId + 
							", bus: " + bus + 
							", active buses: " + mBuses.size() + 
							", transient buses: " + mTransientBuses.size());
				}
			}
		}
	}
	
	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
		TinyBus bus = mBuses.get(activity);
		if (bus != null) {
			// store this bus into transient list to restore it later, 
			// if activity is recreated
			final int busId = mNextTransientBusId++;
			mTransientBuses.put(busId, bus);
			outState.putInt(KEY_BUS_ID, busId);
			if (DEBUG) {
				Log.d(TAG, " ### storing transient bus, id: " + busId + ", bus: " + bus);
			}
		}
	}
	
	@Override public void onActivityResumed(Activity activity) { }
	@Override public void onActivityPaused(Activity activity) { }

}
