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
import java.lang.reflect.Method;
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
import com.halfbit.tinybus.impl.TaskQueue;
import com.halfbit.tinybus.impl.TinyBusDepot;
import com.halfbit.tinybus.impl.Task.TaskCallbacks;
import com.halfbit.tinybus.impl.TinyBusDepot.LifecycleCallbacks;

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
	
	// subscribers and producers methods for a class
	private static final HashMap<Class<?>, ObjectsMeta> OBJECTS_METAS 
		= new HashMap<Class<?>, ObjectsMeta>();
	
	// subscribers for certain event type
	private final HashMap<Class<?>, HashSet<Object>> mEventSubscribers
		= new HashMap<Class<?>, HashSet<Object>>(20);
	
	// producers for certain event type
	private final HashMap<Class<?>, Object> mEventProducers 
		= new HashMap<Class<?>, Object>(); 
	
	// context
	private final TinyBusImpl mImpl;
	private final Handler mMainHandler;
	private final Thread mMainThread;

	// state
	final TaskQueue mTaskQueue;
	boolean mProcessing;
	
	ArrayList<Wireable> mWireables;
	
	//-- public api

	public TinyBus() {
		this(null);
	}
	
	public TinyBus(Context context) {
		mImpl = new TinyBusImpl();
		mImpl.attachContext(context);
		
		mTaskQueue = new TaskQueue();
		mMainThread = Thread.currentThread();
		
		final Looper looper = Looper.myLooper();
		mMainHandler = looper == null ? null : new Handler(looper);
	}
	
	@Override
	public void register(Object obj) {
		assertObjectAndWorkerThread(obj);
		mTaskQueue.offer(Task.obtainTask(this, Task.CODE_REGISTER, obj));
		if (!mProcessing) processQueue();
	}

	@Override
	public void unregister(Object obj) {
		assertObjectAndWorkerThread(obj);
		mTaskQueue.offer(Task.obtainTask(this, Task.CODE_UNREGISTER, obj));
		if (!mProcessing) processQueue();
	}

	@Override
	public boolean hasRegistered(Object obj) {
		assertObjectAndWorkerThread(obj);
		ObjectsMeta meta = OBJECTS_METAS.get(obj.getClass());
		return meta != null && meta.hasRegisteredObject(obj, mEventSubscribers, mEventProducers);
	}
	
	@Override
	public void post(Object event) {
		if (event == null) {
			throw new NullPointerException("Event must not be null");
		}
		
		if (mMainThread == Thread.currentThread()) {
			// this is main thread
			Task task = Task.obtainTask(this, Task.CODE_POST, event);
			mTaskQueue.offer(task);
			if (!mProcessing) processQueue();
			
		} else { 
			// this is a background thread
			
			if (mMainThread.isAlive()) {
				Task task = Task.obtainTask(this, Task.CODE_DISPATCH_FROM_BACKGROUND, event)
						.setTaskCallbacks(mImpl);
				getMainHandlerNotNull().post(task);
			}
		}
	}
	
	private Handler getMainHandlerNotNull() {
		if (mMainHandler == null) {
			throw new IllegalStateException("You can only call post() from a background "
					+ "thread, if the thread, in which TinyBus was created, had a Looper. "
					+ "Solution: create TinyBus in MainThread or in another thread with Looper.");
		}
		return mMainHandler;
	}
	
	
	//-- delayed tasks (experimental)
	
	public void postDelayed(Object event, long delayMillis) {
		if (event == null) {
			throw new NullPointerException("Event must not be null");
		}
		mImpl.postDelayed(event, delayMillis, getMainHandlerNotNull());
	}
	
	public void cancelDelayed(Class<?> eventClass) {
		if (eventClass == null) {
			throw new NullPointerException("Event class must not be null");
		}
		mImpl.cancelDelayed(eventClass, getMainHandlerNotNull());
	}
	
	//-- wireable implementation
	
	public TinyBus wire(Wireable wireable) {
		assertObjectAndWorkerThread(wireable);
		Context context = mImpl.getNotNullContext();

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
		assertObjectAndWorkerThread(wireClass);
		Context context = mImpl.getNotNullContext();
		
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
	
	private void assertObjectAndWorkerThread(Object obj) {
		if (obj == null) {
			throw new NullPointerException("Object must not be null");
		}
		if (mMainThread != Thread.currentThread()) {
			throw new IllegalStateException("You must call this method from the same thread, "
					+ "in which TinyBus was created. Created: " + mMainThread 
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
	
	void processQueue() {
		Task task;
		ObjectsMeta meta;
		
		mProcessing = true;
		try {
			
			while((task = mTaskQueue.poll()) != null) {
				final Object obj = task.obj;
				final Class<?> objClass = obj.getClass();
				
				switch (task.code) {
				
					case Task.CODE_REGISTER: {
						meta = OBJECTS_METAS.get(objClass);
						if (meta == null) {
							meta = new ObjectsMeta(obj);
							OBJECTS_METAS.put(objClass, meta);
						}
						meta.registerAtReceivers(obj, mEventSubscribers);
						meta.registerAtProducers(obj, mEventProducers);
						try {
							meta.dispatchEvents(obj, mEventSubscribers, OBJECTS_METAS, mImpl);
							meta.dispatchEvents(mEventProducers, obj, OBJECTS_METAS, mImpl);
						} catch (Exception e) {
							throw handleExceptionOnEventDispatch(e);
						}
						break;
					}
					
					case Task.CODE_UNREGISTER: {
						meta = OBJECTS_METAS.get(objClass);
						meta.unregisterFromReceivers(obj, mEventSubscribers);
						meta.unregisterFromProducers(obj, mEventProducers);
						break;
					}
					
					case Task.CODE_POST: {
						final HashSet<Object> receivers = mEventSubscribers.get(objClass);
						if (receivers != null) {
							EventCallback eventCallback;
							try {
								for (Object receiver : receivers) {
									meta = OBJECTS_METAS.get(receiver.getClass());
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
	
	public LifecycleCallbacks getLifecycleCallbacks() {
		return mImpl;
	}
	
	//-- inner tinybus implementation used extended features and callbacks
	
	private class TinyBusImpl implements EventDispatchCallback, LifecycleCallbacks, TaskCallbacks {

		private WeakReference<Context> mContextRef;
		private HashMap<Class<?>, Task> mDelayedTasks;
		
		//-- delayed events
		
		public void postDelayed(Object event, long delayMillis, Handler handler) {
			Task task;
			synchronized (this) {
				if (mDelayedTasks == null) {
					mDelayedTasks = new HashMap<Class<?>, Task>();
				}
				task = mDelayedTasks.get(event.getClass());
				if (task == null) {
					task = Task.obtainTask(TinyBus.this, Task.CODE_POST_DELAYED, event)
							.setTaskCallbacks(this);
					mDelayedTasks.put(event.getClass(), task);
					
				} else {
					handler.removeCallbacks(task);
					// replace event and reuse task
					task.obj = event;
				}
			}
			handler.postDelayed(task, delayMillis);
		}

		public void cancelDelayed(Class<?> eventClass, Handler handler) {
			Task task;
			synchronized (this) {
				task = mDelayedTasks.remove(eventClass);
			}
			if (task != null) {
				handler.removeCallbacks(task);
				task.recycle();
			}
		}

		//-- callbacks
		
		@Override
		public void dispatchEvent(EventCallback eventCallback, Object receiver, Object event) throws Exception {
			if (eventCallback.mode == Mode.Background) {
				Task task = Task.obtainTask(TinyBus.this, Task.CODE_DISPATCH_TO_BACKGROUND, event)
						.setTaskCallbacks(this);
				task.eventCallback = eventCallback;
				task.receiverRef = new WeakReference<Object>(receiver);
				
				Context context = getNotNullContext();
				TinyBusDepot.get(context).getDispatcher().dispatchEventToBackground(task);
				
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
		
		public Context getNotNullContext() {
			Context context = mContextRef == null ? null : mContextRef.get();
			if (context == null) {
				throw new IllegalStateException(
					"You must create bus with TinyBus.from(Context) method to use this function.");
			}
			return context;
		}

		//-- task callbacks
		
		@Override
		public void onPostFromBackground(Task task) {
			task.code = Task.CODE_POST;
			mTaskQueue.offer(task);
			if (!mProcessing) processQueue();
		}

		@Override
		public void onPostDelayed(Task task) {
			synchronized (this) {
				mDelayedTasks.remove(task.obj);
			}
			task.code = Task.CODE_POST;
			mTaskQueue.offer(task);
			if (!mProcessing) processQueue();
		}

		@Override
		public void onDispatchInBackground(Task task) throws Exception {
			final Object receiver = task.receiverRef.get();
			if (receiver != null) {
				Method callbackMethod = task.eventCallback.method; 
				if (callbackMethod.getParameterTypes().length == 2) {
					// expect callback with two parameters
					callbackMethod.invoke(receiver, task.obj, task.bus);
				} else {
					// expect callback with a single parameter
					callbackMethod.invoke(receiver, task.obj);
				}
			}
		}
		
	}
}
