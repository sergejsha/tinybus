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
package com.halfbit.tinybus;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.halfbit.tinybus.Subscribe.Mode;
import com.halfbit.tinybus.impl.ObjectsMeta;
import com.halfbit.tinybus.impl.ObjectsMeta.EventCallback;
import com.halfbit.tinybus.impl.ObjectsMeta.EventDispatchCallback;
import com.halfbit.tinybus.impl.Task;
import com.halfbit.tinybus.impl.Task.TaskQueue;
import com.halfbit.tinybus.impl.TinyBusDepot;
import com.halfbit.tinybus.impl.TinyBusDepot.LifecycleComponent;

/**
 * Main bus implementation. You can either create a bus instance 
 * by calling constructor or use static {@link #from(Context)} method. 
 * 
 * <p>When you use constructor you create a stand alone bus instance
 * which is not bound to any context. You are not able to {@link #wire(Wireable)}
 * any <code>Wireable</code>'s to such bus.
 * 
 * <p>Create your bus using {@link #from(Context)} method, if you want to use
 * <code>Wireable</code>'s.
 * 
 * @author sergej
 */
public class TinyBus implements Bus {
	
	/**
	 * You can wire instances of this class to a bus instance using 
	 * {@link TinyBus#wire(Wireable)} method. Once wired, <code>Wireable</code>
	 * instance will be started and stopped automatically, reflecting the
	 * status of context to which the bus instance is bound.
	 *
	 * <p>
	 * If a bus is bound to an <code>Activity</code>, then <code>Wireable</code>
	 * instance will be started when <code>Activity</code> starts and stopped 
	 * when <code>Activity</code> stops. If it is bound to the <code>Application</code>,
	 * then it will only be started once and newer stopped.
	 * 
	 * @author sergej
	 */
	public static abstract class Wireable {
		
		protected Bus bus;
		protected Context context;
		
		protected void onCreate(Bus bus, Context context) {
			this.bus = bus;
			this.context = context;
		}
		
		protected void onDestroy() {
			this.bus = null;
			this.context = null;
		}

		protected void onStart() { }
		protected void onStop() { }
		
		void assertSuperOnCreateCalled() {
			if (bus == null) {
				throw new IllegalStateException(
						"You must call super.onCreate(bus, context) method when overriding it.");
			}
		}
	}
	
	/**
	 * Use this method to get a bus instance bound to the given context.
	 * If instance does not yet exist in the context, a new instance 
	 * will be created. Otherwise existing instance gets returned.
	 *
	 * <p>Bus instance can be bound to a context. 
	 * 
	 * <p>If you need a singleton instance existing only once for the 
	 * whole application, provide application context here. This option
	 * can be chosen for <code>Activity</code> to <code>Service</code> 
	 * communication.
	 * 
	 * <p>
	 * If you need a bus instance per <code>Activity</code>, you have 
	 * to provide activity context in there. This option is perfect for
	 * <code>Activity</code> to <code>Fragment</code> or 
	 * <code>Fragment</code> to <code>Fragment</code> communication.
	 * 
	 * <p>
	 * Bus instance gets destroyed when your context is destroyed.
	 * 
	 * @param context	context to which this instance of bus has to be bound
	 * @return			event bus instance, never null
	 */
	public static synchronized TinyBus from(Context context) {
		final TinyBusDepot depot = TinyBusDepot.get(context);
		TinyBus bus = depot.getBusInContext(context);
		if (bus == null) {
			bus = depot.createBusInContext(context);
		}
		return bus;
	}
	
	//-- implementation
	
	private static final String TAG = "tinybus";
	
	// callback's (receivers and/or producers) meta
	private static final HashMap<Class<?>, ObjectsMeta> OBJECTS_META 
		= new HashMap<Class<?>, ObjectsMeta>();
	
	// event class to receiver objects map 
	private final HashMap<Class<?>, HashSet<Object>> mEventReceivers
		= new HashMap<Class<?>, HashSet<Object>>();
	
	// event class to producer object map
	private final HashMap<Class<?>, Object> mEventProducers 
		= new HashMap<Class<?>, Object>(); 
	
	private final TinyBusImpl mImpl;
	private final TaskQueue mTaskQueue;
	private final Handler mWorkerHandler;
	private final Thread mWorkerThread;

	private boolean mProcessing;
	
	WeakReference<Context> mContextRef;
	ArrayList<Wireable> mWireables;
	
	//-- public api

	public TinyBus() {
		this(null);
	}
	
	public TinyBus(Context context) {
		mImpl = new TinyBusImpl();
		mImpl.attachContext(context);
		
		mTaskQueue = new TaskQueue();
		mWorkerThread = Thread.currentThread();
		
		final Looper looper = Looper.myLooper();
		mWorkerHandler = looper == null ? null : new Handler(looper);
	}
	
	@Override
	public void register(Object obj) {
		if (obj == null) throw new NullPointerException("Object must not be null");
		assertWorkerThread();
		
		mTaskQueue.offer(Task.obtainTask(this, Task.CODE_REGISTER, obj));
		if (!mProcessing) processQueue();
	}

	@Override
	public void unregister(Object obj) {
		if (obj == null) throw new NullPointerException("Object must not be null");
		assertWorkerThread();
		
		mTaskQueue.offer(Task.obtainTask(this, Task.CODE_UNREGISTER, obj));
		if (!mProcessing) processQueue();
	}

	@Override
	public boolean hasRegistered(Object obj) {
		if (obj == null) throw new NullPointerException("Object must not be null");
		assertWorkerThread();
		
		ObjectsMeta meta = OBJECTS_META.get(obj.getClass());
		return meta != null && meta.hasRegisteredObject(obj, mEventReceivers, mEventProducers); 
	}
	
	@Override
	public void post(Object event) {
		if (event == null) throw new NullPointerException("Event must not be null");
		
		if (mWorkerThread == Thread.currentThread()) {
			
			// we post a Task instance when dispatching from a background thread
			Task task = event instanceof Task ? (Task) event 
					: Task.obtainTask(this, Task.CODE_POST_EVENT, event);
					
			mTaskQueue.offer(task);
			if (!mProcessing) processQueue();
			
		} else {
			if (mWorkerHandler == null) {
				throw new IllegalStateException("You can only call post() from a different "
						+ "thread, if the thread, in which TinyBus was created, had a Looper. "
						+ "Solution: create TinyBus in MainThread or in another thread with Looper.");
			}
			
			mWorkerHandler.post(Task.obtainTask(this, Task.BACKGROUND_DISPATCH_FROM_BACKGROUND, event)
					.setupRepostHandler(this));
		}
	}
	
	//-- wireable implementation
	
	public TinyBus wire(Wireable wireable) {
		assertWorkerThread();
		Context context = getNotNullContext();

		if (mWireables == null) {
			mWireables = new ArrayList<Wireable>();
		}
		mWireables.add(wireable);
		
		wireable.onCreate(this, context.getApplicationContext());
		wireable.assertSuperOnCreateCalled();
		
		if (context instanceof Application 
				|| context instanceof Service) {
			wireable.onStart();
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T extends Wireable> T unwire(Class<T> wireClass) {
		assertWorkerThread();
		Context context = getNotNullContext();
		
		Wireable wireable = getWireable(wireClass);
		if (wireable != null) {
			
			if (context instanceof Application 
					|| context instanceof Service) {
				wireable.onStop();
				wireable.onDestroy();
			}
			
			mWireables.remove(wireable);
		}
		return (T) wireable;
	}
	
	public boolean hasWireable(Class<? extends Wireable> wireClass) {
		return getWireable(wireClass) != null;
	}
	
	private Wireable getWireable(Class<? extends Wireable> wireClass) {
		if (mWireables == null) {
			return null;
		}
		for(Wireable wireable : mWireables) {
			if (wireClass.equals(wireable.getClass())) {
				return wireable;
			}
		}
		return null;
	}
	
	//-- private methods
	
	private Context getNotNullContext() {
		Context context = mContextRef == null ? null : mContextRef.get();
		if (context == null) {
			throw new IllegalStateException(
				"You must create bus with TinyBus.from() method to use this function.");
		}
		return context;
	}
	
	private void assertWorkerThread() {
		if (mWorkerThread != Thread.currentThread()) {
			throw new IllegalStateException("You must call this method from the same thread, "
					+ "in which TinyBus was created. Created: " + mWorkerThread 
					+ ", current thread: " + Thread.currentThread());
		}
	}
	
	private RuntimeException handleExceptionOnEventDispatch(Exception e) {
		if (e instanceof RuntimeException) {
			return (RuntimeException) e;
		}
		
		if (e instanceof InvocationTargetException) {
			// Extract subscriber method name to give developer more details
			String method = Log.getStackTraceString(e.getCause());
			int start = method.indexOf("at") + 3;
			method = method.substring(start, method.indexOf('\n', start));
			Log.e(TAG, "Exception in @Subscriber method: " + method + ". See stack trace for more details.");
		} 
		return new RuntimeException(e);		
	}
	
	private void processQueue() {
		Task task;
		ObjectsMeta meta;
		
		mProcessing = true;
		try {
			
			while((task = mTaskQueue.poll()) != null) {
				final Object obj = task.obj;
				final Class<?> objClass = obj.getClass();
				
				switch (task.code) {
				
					case Task.CODE_REGISTER: {
						meta = OBJECTS_META.get(objClass);
						if (meta == null) {
							meta = new ObjectsMeta(obj);
							OBJECTS_META.put(objClass, meta);
						}
						meta.registerAtReceivers(obj, mEventReceivers);
						meta.registerAtProducers(obj, mEventProducers);
						try {
							meta.dispatchEvents(obj, mEventReceivers, OBJECTS_META, mImpl);
							meta.dispatchEvents(mEventProducers, obj, OBJECTS_META, mImpl);
						} catch (Exception e) {
							throw handleExceptionOnEventDispatch(e);
						}
						break;
					}
					
					case Task.CODE_UNREGISTER: {
						meta = OBJECTS_META.get(objClass);
						meta.unregisterFromReceivers(obj, mEventReceivers);
						meta.unregisterFromProducers(obj, mEventProducers);
						break;
					}
					
					case Task.CODE_POST_EVENT: {
						final HashSet<Object> receivers = mEventReceivers.get(objClass);
						if (receivers != null) {
							EventCallback eventCallback;
							try {
								for (Object receiver : receivers) {
									meta = OBJECTS_META.get(receiver.getClass());
									eventCallback = meta.getEventCallback(objClass);
									mImpl.dispatchEvent(eventCallback, receiver, obj);
								}
							} catch (Exception e) {
								throw handleExceptionOnEventDispatch(e);
							}
						}
						break;
					}
					
					default: throw new IllegalStateException("unexpected task code: " + task.code);
				}
				task.recycle();
			}
			
		} finally {
			mProcessing = false;
		}		
	}
	
	//-- package methods
	
	public LifecycleComponent getLifecycleComponent() {
		return mImpl;
	}
	
	// inner, not public implementation
	
	class TinyBusImpl implements EventDispatchCallback, LifecycleComponent {

		@Override
		public void dispatchEvent(EventCallback eventCallback, Object receiver, Object event) throws Exception {
			
			if (eventCallback.mode == Mode.Background) {
				// TODO fix me
				Context context = mContextRef == null ? null : mContextRef.get();
				if (context == null) {
					throw new IllegalStateException("To enable multithreaded dispatching "
							+ "you have to create bus using TinyBus(Context) constructor.");
				}
				TinyBusDepot.get(context).getDispatcher()
					.dispatchEventInBackground(TinyBus.this, eventCallback, receiver, event);
				
			} else {
				eventCallback.method.invoke(receiver, event);
			}
		}

		/**
		 * This method gets called when bus is transferred from 
		 * one activity to another during configuration change.
		 * 
		 * NOTE: Context instance can be null, thus bus can be 
		 * in a state when there is no attached context. 
		 */
		@Override
		public void attachContext(Context context) {
			mContextRef = context == null ? null : new WeakReference<Context>(context);
		}
		
		@Override
		public void onStart() {
			if (mWireables != null) {
				for (Wireable wireable : mWireables) {
					wireable.onStart();
				}
			}
		}
		
		@Override
		public void onStop() {
			if (mWireables != null) {
				for (Wireable wireable : mWireables) {
					wireable.onStop();
				}
			}
		}
		
		@Override
		public void onDestroy() {
			if (mWireables != null) {
				for (Wireable wireable : mWireables) {
					wireable.onDestroy();
				}
			}
		}
		
	}
}
