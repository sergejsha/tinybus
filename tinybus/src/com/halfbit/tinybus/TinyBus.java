/*
 * Copyright (C) 2014 Sergej Shafarenka, halfbit.de
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.halfbit.tinybus.ObjectMeta.EventCallback;
import com.halfbit.tinybus.Subscribe.Mode;

public class TinyBus implements Bus {
	
	/**
	 * You can {@link TinyBus#wire()} instances of this class to a bus.
	 * Once wired, your <code>Wirable</code> will be started and stopped
	 * depending on the context state. 
	 * 
	 * <p>
	 * If the bus was created for an <i>Activity</i>, then all wired instances 
	 * will be started and stopped when Activity starts and stops.
	 * 
	 * <p>
	 * If context is <i>Application</i>, then all wired instance will be started
	 * immediately after they became wired and won't be stopped afterwards.
	 * 
	 * <p>
	 * <i>Service</i> context is not yet properly supported (TODO)
	 * 
	 * @author sergej
	 */
	public static abstract class Wireable {
		protected Bus bus;
		protected abstract void onStart(Context context);
		protected abstract void onStop(Context context);
	}	
	
	/**
	 * Use this method to get a bus instance available in current context. 
	 * 
	 * TODO add more details 
	 * 
	 * @param context
	 * @return	event bus instance, never null
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
	
	private static final int QUEUE_SIZE = 12;
	
	// callback's (receivers and/or producers) meta
	private static final HashMap<Class<?>, ObjectMeta> OBJECTS_META 
		= new HashMap<Class<?>, ObjectMeta>();
	
	// event class to receiver objects map 
	private final HashMap<Class<?>, HashSet<Object>> mEventReceivers
		= new HashMap<Class<?>, HashSet<Object>>();
	
	// event class to producer object map
	private final HashMap<Class<?>, Object> mEventProducers 
		= new HashMap<Class<?>, Object>(); 
	
	private final TaskQueue mTaskQueue;
	private final Handler mWorkerHandler;
	private final Thread mWorkerThread;
	private final BackgroundDispatcher mBackgroundDispatcher;
	private final WeakReference<Context> mContextRef;

	private ArrayList<Wireable> mWireable;
	private boolean mProcessing;
	
	//-- public api

	public TinyBus() {
		this(null);
	}
	
	public TinyBus(Context context) {
		mTaskQueue = new TaskQueue();
		mWorkerThread = Thread.currentThread();
		
		if (context == null) {
			mContextRef = null;
			mBackgroundDispatcher = null;
			
		} else {
			mContextRef = new WeakReference<Context>(context);
			mBackgroundDispatcher = TinyBusDepot.get(context).getBackgroundDispatcher();
		}
		
		final Looper looper = Looper.myLooper();
		mWorkerHandler = looper == null ? null : new Handler(looper);
	}
	
	@Override
	public void register(Object obj) {
		if (obj == null) throw new NullPointerException("Object must not be null");
		assertWorkerThread();
		
		mTaskQueue.offer(Task.obtainTask(obj, Task.CODE_REGISTER));
		if (!mProcessing) processQueue();
	}

	@Override
	public void unregister(Object obj) {
		if (obj == null) throw new NullPointerException("Object must not be null");
		assertWorkerThread();
		
		mTaskQueue.offer(Task.obtainTask(obj, Task.CODE_UNREGISTER));
		if (!mProcessing) processQueue();
	}

	@Override
	public void post(Object event) {
		if (event == null) throw new NullPointerException("Event must not be null");
		
		if (mWorkerThread == Thread.currentThread()) {
			mTaskQueue.offer(Task.obtainTask(event, Task.CODE_POST_EVENT));
			if (!mProcessing) processQueue();
			
		} else {
			if (mWorkerHandler == null) {
				throw new IllegalStateException("You can only call post() from a different "
						+ "thread, if the thread, in which TinyBus was created, had a Looper. "
						+ "Solution: create TinyBus in MainThread or in another thread with Looper.");
			}
			
			mWorkerHandler.post(Task.obtainTask(event, Task.RUNNABLE_REPOST_EVENT)
					.setupRepostHandler(this));
		}
	}
	
	public TinyBus wire(Wireable wireable) {
		if (mWireable == null) {
			mWireable = new ArrayList<Wireable>();
		}
		mWireable.add(wireable);
		wireable.bus = this;
		
		if (mContextRef != null) {
			Context context = mContextRef.get();
			if (context instanceof Application) {
				wireable.onStart(context);
			}
		}
		return this;
	}
	
	private void assertWorkerThread() {
		if (mWorkerThread != Thread.currentThread()) {
			throw new IllegalStateException("You must call this method from the same thread, "
					+ "in which TinyBus was created. Created: " + mWorkerThread 
					+ ", current thread: " + Thread.currentThread());
		}
	}
	
	//-- private methods
	
	private void processQueue() {
		mProcessing = true;
		Task task;
		
		ObjectMeta meta;
		
		while((task = mTaskQueue.poll()) != null) {
			final Object obj = task.obj;
			final Class<?> objClass = obj.getClass();
			
			switch (task.code) {
			
				case Task.CODE_REGISTER: {
					meta = OBJECTS_META.get(objClass);
					if (meta == null) {
						meta = new ObjectMeta(obj);
						OBJECTS_META.put(objClass, meta);
					}
					meta.registerAtReceivers(obj, mEventReceivers);
					meta.registerAtProducers(obj, mEventProducers);
					meta.dispatchEvents(obj, mEventReceivers, OBJECTS_META, this);
					meta.dispatchEvents(mEventProducers, obj, OBJECTS_META, this);
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
								dispatchEvent(eventCallback, receiver, obj);
							}
						} catch (Exception e) {
							if (e instanceof RuntimeException) {
								throw (RuntimeException) e;
							}
							throw new RuntimeException(e);
						}
					}
					break;
				}
				
				default: throw new IllegalStateException("unexpected task code: " + task.code);
			}
			task.recycle();
		}
		
		mProcessing = false;
	}
	
	//-- package methods
	
	void dispatchEvent(EventCallback eventCallback, Object receiver, 
			Object event) throws Exception {
		
		if (eventCallback.mode == Mode.Background) {
			if (mBackgroundDispatcher == null) {
				throw new IllegalStateException("To enable multithreaded dispatching "
						+ "you have to create bus using TinyBus(Context) constructor.");
			}
			mBackgroundDispatcher.dispatchEvent(eventCallback, receiver, event);
			
		} else {
			eventCallback.method.invoke(receiver, event);
		}
	}
	
	void dispatchOnStartWireable(Activity activity) {
		if (mWireable != null) {
			for (Wireable producer : mWireable) {
				producer.onStart(activity);
			}
		}
	}
	
	void dispatchOnStopWireable(Activity activity) {
		if (mWireable != null) {
			for (Wireable producer : mWireable) {
				producer.onStop(activity);
			}
		}
	}
	
	//-- inner classes
	
	static class Task implements Runnable {
		
		private static final TaskPool POOL = new TaskPool(QUEUE_SIZE);
		
		public static final int CODE_REGISTER = 0;
		public static final int CODE_UNREGISTER = 1;
		public static final int CODE_POST_EVENT = 2;
		
		public static final int RUNNABLE_REPOST_EVENT = 10;
		public static final int RUNNABLE_DISPATCH_BACKGROUND_EVENT = 11;
		
		public Task prev;
		public int code;
		public Object obj;
		
		// runnable repost
		public TinyBus bus;
		
		// runnable dispatch
		public EventCallback eventCallback;
		public WeakReference<Object> receiverRef;
		
		private Task() {}
		
		public static Task obtainTask(Object obj, int code) {
			Task task;
			synchronized (POOL) {
				task = POOL.acquire();
			}
			task.code = code;
			task.obj = obj;
			task.prev = null;
			return task;
		}
		
		public void recycle() {
			synchronized (POOL) {
				POOL.release(this);
			}
		}

		//-- handling repost event
		
		public Task setupRepostHandler(TinyBus bus) {
			this.bus = bus;
			return this;
		}
		
		@Override
		public void run() {
			if (code != RUNNABLE_REPOST_EVENT) {
				throw new IllegalStateException("Assertion. Expected task " 
						+ RUNNABLE_REPOST_EVENT + " while received " + code);
			}
			code = CODE_POST_EVENT;
			bus.mTaskQueue.offer(this);
			bus.processQueue();
			bus = null;
		}
		
		//-- handling dispatch event
		
		public Task setupDispatchEventHandler(EventCallback eventCallback, Object receiver, Object event) {
			this.eventCallback = eventCallback;
			this.receiverRef = new WeakReference<Object>(receiver);
			this.obj = event;
			return this;
		}
		
		public void dispatchInBackground() throws Exception {
			if (code != RUNNABLE_DISPATCH_BACKGROUND_EVENT) {
				throw new IllegalStateException("Assertion. Expected task " 
						+ RUNNABLE_DISPATCH_BACKGROUND_EVENT + " while received " + code);
			}
			
			final Object receiver = this.receiverRef.get();
			if (receiver != null) {
				eventCallback.method.invoke(receiver, obj);
			}
			
			eventCallback = null;
			receiverRef = null;
			obj = null;
		}
		
	}
	
	/**
	 * Singly linked list used as a FIFO queue.
	 * @author sergej
	 */
    static class TaskQueue {
    	private Task head;
    	private Task tail;
    	
    	public void offer(Task task) {
    		if (tail == null) {
    			tail = head = task;
    		} else {
    			tail.prev = task;
    			tail = task;
    		}
    	}
    	
    	public Task poll() {
    		if (head == null) {
    			return null;
    		} else {
    			Task task = head;
    			head = head.prev;
    			if (head == null) tail = null;
    			return task;
    		}
    	}
    }
	
    // pool of tasks
    static class TaskPool {
    	private final int mMaxSize;
    	private int mSize;
    	private Task tail;
    	
    	public TaskPool(int maxSize) {
    		mMaxSize = maxSize;
    	}
    	
    	Task acquire() {
    		Task acquired = tail;
    		if (acquired == null) {
    			acquired = new Task();
    		} else {
    			tail = acquired.prev;
    			mSize--;
    		}
    		return acquired;
    	}
    	
		void release(Task task) {
			if (mSize < mMaxSize) {
				task.prev = tail;
				tail = task;
				mSize++;
			}
		}
    }
    
}
