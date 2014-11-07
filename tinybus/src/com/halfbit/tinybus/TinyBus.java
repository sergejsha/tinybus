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
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.halfbit.tinybus.ObjectMeta.EventCallback;
import com.halfbit.tinybus.Subscribe.Mode;

public class TinyBus implements Bus {
	
	//-- public classes and methods
	
	public static abstract class Events {
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
	public static Bus from(Context context) {
		return TinyBusDepot.get(context).from(context);
	}
	
	public static TinyBus create(Context context) {
		if (context == null) {
			throw new NullPointerException("context must not be null");
		}
		return TinyBusDepot.get(context).create(context);
	}
	
	public TinyBus wire(Events events) {
		if (mEvents == null) {
			mEvents = new ArrayList<Events>();
		}
		mEvents.add(events);
		events.bus = this;
		return this;
	}
	
	//-- static members
	
	private static final int QUEUE_SIZE = 12;
	
	// cached objects meta data
	private static final HashMap<Class<?> /*receivers or producer*/, ObjectMeta> 
		OBJECTS_META = new HashMap<Class<?>, ObjectMeta>();
	
	//-- fields
	
	private final HashMap<Class<?>/*event class*/, HashSet<Object>/*multiple receiver objects*/>
		mEventReceivers = new HashMap<Class<?>, HashSet<Object>>();
	
	private final HashMap<Class<?>/*event class*/, Object/*single producer objects*/>
		mEventProducers = new HashMap<Class<?>, Object>(); 
	
	private final TaskQueue mTaskQueue;
	private final Handler mWorkerHandler;
	private final Thread mWorkerThread;
	private final BackgroundDispatcher mBackgroundDispatcher;
	
	private ArrayList<Events> mEvents;
	private boolean mProcessing;
	
	//-- public api

	public TinyBus() {
		this(null);
	}
	
	public TinyBus(Context context) {
		mTaskQueue = new TaskQueue();
		mWorkerThread = Thread.currentThread();
		final Looper looper = Looper.myLooper();
		mWorkerHandler = looper == null ? null : new Handler(looper);
		mBackgroundDispatcher = context == null ? null : TinyBusDepot.get(context).getBackgroundDispatcher();
	}
	
	@Override
	public void register(Object obj) {
		if (obj == null) throw new NullPointerException("Object must not be null");
		assertWorkerThread();
		
		if (mProcessing) {
			mTaskQueue.offer(Task.obtainTask(obj, Task.CODE_REGISTER));
			
		} else {
			mTaskQueue.offer(Task.obtainTask(obj, Task.CODE_REGISTER));
			processQueue();
		}
	}

	@Override
	public void unregister(Object obj) {
		if (obj == null) throw new NullPointerException("Object must not be null");
		assertWorkerThread();
		
		if (mProcessing) {
			mTaskQueue.offer(Task.obtainTask(obj, Task.CODE_UNREGISTER));
			
		} else {
			mTaskQueue.offer(Task.obtainTask(obj, Task.CODE_UNREGISTER));
			processQueue();
		}
	}

	@Override
	public void post(Object event) {
		if (event == null) throw new NullPointerException("Event must not be null");
		
		if (mWorkerThread == Thread.currentThread()) {
			if (mProcessing) {
				mTaskQueue.offer(Task.obtainTask(event, Task.CODE_POST_EVENT));
				
			} else {
				mTaskQueue.offer(Task.obtainTask(event, Task.CODE_POST_EVENT));
				processQueue();
			}
			
		} else {
			if (mWorkerHandler != null) {
				mWorkerHandler.post(Task.obtainTask(event, Task.RUNNABLE_REPOST_EVENT)
						.setupRepostHandler(this));
				
			} else {
				throw new IllegalStateException("You can only call post() from a different "
						+ "thread, if the thread, in which TinyBus was created, had a Looper. "
						+ "Solution: create TinyBus in MainThread or in another thread with Looper.");
			}
		}
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
		
		while((task = mTaskQueue.poll()) != null) {
			switch (task.code) {
				case Task.CODE_REGISTER: registerInternal(task.obj); break;
				case Task.CODE_UNREGISTER: unregisterInternal(task.obj); break;
				case Task.CODE_POST_EVENT: postInternal(task.obj); break;
				default: throw new IllegalStateException("unsupported task code: " + task.code);
			}
			task.recycle();
		}
		
		mProcessing = false;
	}
	
	private void registerInternal(Object obj) {
		ObjectMeta meta = OBJECTS_META.get(obj.getClass());
		if (meta == null) {
			meta = new ObjectMeta(obj);
			OBJECTS_META.put(obj.getClass(), meta);
		}
		meta.registerAtReceivers(obj, mEventReceivers);
		meta.registerAtProducers(obj, mEventProducers);
		
		meta.dispatchEvents(obj, mEventReceivers, OBJECTS_META, this);
		meta.dispatchEvents(mEventProducers, obj, OBJECTS_META, this);
	}
	
	private void unregisterInternal(Object obj) {
		ObjectMeta meta = OBJECTS_META.get(obj.getClass());
		meta.unregisterFromReceivers(obj, mEventReceivers);
		meta.unregisterFromProducers(obj, mEventProducers);
	}
	
	private void postInternal(Object event) {
		final Class<?> eventClass = event.getClass();
		final HashSet<Object> receivers = mEventReceivers.get(eventClass);
		if (receivers != null) {
			ObjectMeta meta;
			EventCallback eventCallback;
			try {
				for (Object receiver : receivers) {
					meta = OBJECTS_META.get(receiver.getClass());
					eventCallback = meta.getEventCallback(eventClass);
					dispatchEvent(eventCallback, receiver, event);
				}
			} catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException(e);
			}
		}
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
	
	void dispatchOnStart(Activity activity) {
		if (mEvents != null) {
			for (Events producer : mEvents) {
				producer.onStart(activity);
			}
		}
	}
	
	void dispatchOnStop(Activity activity) {
		if (mEvents != null) {
			for (Events producer : mEvents) {
				producer.onStop(activity);
			}
		}
	}
	
	//-- inner classes
	
	static class Task implements Runnable {
		
		private static final SimplePool<Task> POOL = new SimplePool<Task>(QUEUE_SIZE);
		
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
			if (task == null) task = new Task();
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
	
	/**
	 * Copyright (C) 2013 The Android Open Source Project
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Modified code from AOSP
	 */
    static class SimplePool<T> {
        private final Object[] mPool;
        private int mPoolSize;

        /**
         * Creates a new instance.
         * @param maxPoolSize The max pool size.
         * @throws IllegalArgumentException If the max pool size is less than zero.
         */
        public SimplePool(int maxPoolSize) {
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("The max pool size must be > 0");
            }
            mPool = new Object[maxPoolSize];
        }

        @SuppressWarnings("unchecked")
        public T acquire() {
            if (mPoolSize > 0) {
                final int lastPooledIndex = mPoolSize - 1;
                T instance = (T) mPool[lastPooledIndex];
                mPool[lastPooledIndex] = null;
                mPoolSize--;
                return instance;
            }
            return null;
        }

        public boolean release(T instance) {
            if (mPoolSize < mPool.length) {
                mPool[mPoolSize] = instance;
                mPoolSize++;
                return true;
            }
            return false;
        }
    }
    
}
