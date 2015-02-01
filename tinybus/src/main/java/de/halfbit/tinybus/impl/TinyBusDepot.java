/*
 * Copyright (C) 2014, 2015 Sergej Shafarenka, halfbit.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.halfbit.tinybus.impl;

import java.util.WeakHashMap;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import de.halfbit.tinybus.TinyBus;
import de.halfbit.tinybus.impl.workers.Dispatcher;

public class TinyBusDepot implements ActivityLifecycleCallbacks {

	public static interface LifecycleCallbacks {
		void attachContext(Context context);
		void onStart();
		void onStop();
		void onDestroy();
	}
	
	private static final boolean DEBUG = false;
	private static final String TAG = TinyBusDepot.class.getSimpleName();
	
	private static final String KEY_BUS_ID = "de.halfbit.tinybus.id";
	private static TinyBusDepot INSTANCE;
	
	public static TinyBusDepot get(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new TinyBusDepot(context);
		}
		return INSTANCE;
	}

	private Dispatcher mDispatcher;
	
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
	
	public synchronized Dispatcher getDispatcher() {
		if (mDispatcher == null) {
			mDispatcher = new Dispatcher();
		}
		return mDispatcher;
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
			
			bus.getLifecycleCallbacks().onStart();
		}
		if (DEBUG) Log.d(TAG, " ### STARTED, bus count: " + mBuses.size());
	}
	
	@Override
	public void onActivityStopped(Activity activity) {
		onContextStopped(activity);
	}
	
	@Override
	public void onActivityDestroyed(Activity activity) {
		onContextDestroyed(activity);
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
				bus.getLifecycleCallbacks().attachContext(activity);
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

	//-- inner lifecycle implementation
	
	void onContextStopped(Context context) {
		TinyBus bus = mBuses.get(context);
		if (bus != null) {
			bus.getLifecycleCallbacks().onStop();
		}
		if (DEBUG) Log.d(TAG, " ### STOPPED, bus count: " + mBuses.size());
	}
	
	void onContextDestroyed(Context context) {
		TinyBus bus = mBuses.remove(context);
		
		boolean dontDestroyBus = context instanceof Activity 
				&& ((Activity)context).isChangingConfigurations();
		
		if (bus != null && !dontDestroyBus) {
			bus.getLifecycleCallbacks().onDestroy();
			if (DEBUG) {
				Log.d(TAG, " ### destroying bus: " + bus);
			}
		}
		if (DEBUG) {
			Log.d(TAG, " ### onDestroy() " + context +
				", active buses: " + mBuses.size() + 
				", transient buses: " + mTransientBuses.size());
		}
	}
	
}
