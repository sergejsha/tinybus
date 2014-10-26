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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import android.content.Context;
import android.os.Looper;

public class TinyBus implements Bus {
	
	/**
	 * Use this method to get a bus instance available in current context. Do not forget to
	 * implement {@link com.halfbit.tinybus.BusDepot} in your activity or application to make
	 * this method working.
	 * 
	 * @see BusDepot
	 * 
	 * @param context
	 * @return	event bus instance, never null
	 */
	public static Bus from(Context context) {
		if (context instanceof BusDepot) {
			return ((BusDepot)context).getBus();
		} else {
			context = context.getApplicationContext();
			if (context instanceof BusDepot) {
				return ((BusDepot)context).getBus();
			}
		}
		throw new IllegalArgumentException("Make sure Activity or Application implements BusDepot interface.");
	}

	//-- static members
	
	// set it to true, if you want the bus to check whether it is called from the main thread 
	private static final boolean ASSERT_ACCESS = false;
	
	private static final int QUEUE_SIZE = 12;
	private static final AccessAssertion MAIN_THREAD_CHECKER = new MainThreadAssertion();
	
	// cached objects meta data
	private static final HashMap<Class<?> /*receivers or producer*/, ObjectMeta> 
		OBJECTS_META = new HashMap<Class<?>, ObjectMeta>();
	
	//-- fields
	
	private final HashMap<Class<?>/*event class*/, HashSet<Object>/*multiple receiver objects*/>
		mEventReceivers = new HashMap<Class<?>, HashSet<Object>>();
	
	private final HashMap<Class<?>/*event class*/, Object/*single producer objects*/>
		mEventProducers = new HashMap<Class<?>, Object>(); 
	
	private final AccessAssertion mAccessAssertion;
	
	private Task head;
	private Task tail;
	private boolean mProcessing;
	
	//-- public api

	public TinyBus() {
		this(MAIN_THREAD_CHECKER);
	}
	
	TinyBus(AccessAssertion checker) {
		mAccessAssertion = checker;
	}
	
	@Override
	public void register(Object obj) {
		if (obj == null) throw new IllegalArgumentException("Object must not be null");
		
		if (mProcessing) {
			offer(Task.obtainTask(obj, Task.CODE_REGISTER));
			
		} else {
			if (ASSERT_ACCESS) mAccessAssertion.assertAccess();
			offer(Task.obtainTask(obj, Task.CODE_REGISTER));
			processQueue();
		}
	}

	@Override
	public void unregister(Object obj) {
		if (obj == null) throw new IllegalArgumentException("Object must not be null");
		
		if (mProcessing) {
			offer(Task.obtainTask(obj, Task.CODE_UNREGISTER));
			
		} else {
			if (ASSERT_ACCESS) mAccessAssertion.assertAccess();
			offer(Task.obtainTask(obj, Task.CODE_UNREGISTER));
			processQueue();
		}
	}

	@Override
	public void post(Object event) {
		if (event == null) throw new IllegalArgumentException("Event must not be null");
		
		if (mProcessing) {
			offer(Task.obtainTask(event, Task.CODE_POST_EVENT));
			
		} else {
			if (ASSERT_ACCESS) mAccessAssertion.assertAccess();
			offer(Task.obtainTask(event, Task.CODE_POST_EVENT));
			processQueue();
		}
	}
	
	//-- private methods
	
	private void offer(Task task) {
		if (tail == null) {
			tail = head = task;
		} else {
			tail.prev = task;
			tail = task;
		}
	}
	
	private Task poll() {
		if (head == null) {
			return null;
		} else {
			Task task = head;
			head = head.prev;
			if (head == null) tail = null;
			return task;
		}
	}
	
	private void processQueue() {
		mProcessing = true;
		Task task;
		
		while((task = poll()) != null) {
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
		
		meta.dispatchEvents(obj, mEventReceivers, OBJECTS_META);
		meta.dispatchEvents(mEventProducers, obj, OBJECTS_META);
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
			Method callback;
			try {
				for (Object receiver : receivers) {
					meta = OBJECTS_META.get(receiver.getClass());
					callback = meta.getEventCallback(eventClass);
					callback.invoke(receiver, event);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	//-- task class
	
	private static class Task {
		
		private static final SimplePool<Task> POOL = new SimplePool<Task>(QUEUE_SIZE);
		
		public static final int CODE_REGISTER = 0;
		public static final int CODE_UNREGISTER = 1;
		public static final int CODE_POST_EVENT = 2;
		
		public Task prev;
		
		public int code;
		public Object obj;
		
		private Task() {}
		
		public static Task obtainTask(Object obj, int code) {
			Task task = POOL.acquire();
			if (task == null) task = new Task();
			task.code = code;
			task.obj = obj;
			task.prev = null;
			return task;
		}
		
		public void recycle() {
			POOL.release(this);
		}
	}
	
	//-- access checkers
	
	static interface AccessAssertion {
		void assertAccess();
	}
	
	static class SingleThreadAssertion 
			implements AccessAssertion {

		private Thread mMasterThread;
		
		@Override
		public void assertAccess() {
			if (mMasterThread == null) {
				mMasterThread = Thread.currentThread();
			} else if (mMasterThread != Thread.currentThread()) {
				throw new IllegalStateException("TinyBus must be accessed from same thread in which it " 
						+ "was accessed for the first time. Expected access thread :" 
						+ mMasterThread + ", while accessed from : " + Thread.currentThread());
			}
		}
	}

	static class MainThreadAssertion 
			implements AccessAssertion {

		@Override
		public void assertAccess() {
			if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
				throw new IllegalStateException("TinyBus must be accessed from MainThread, while assesed from : " 
						+ Thread.currentThread());
			}
		}
	}
	
	//-- private classes
	
	private static class ObjectMeta {

		private HashMap<Class<? extends Object>/*event class*/, Method> mEventCallbacks
			= new HashMap<Class<? extends Object>, Method>();

		private HashMap<Class<? extends Object>/*event class*/, Method> mProducerCallbacks
			= new HashMap<Class<? extends Object>, Method>();
		
		public ObjectMeta(Object obj) {
			Class<? extends Object> clazz = obj.getClass();
			Method[] methods = clazz.getMethods();
			
			Class<?>[] params;
			Class<?> eventClass;
			for (Method method : methods) {
				if (method.isBridge()) continue;
				
				if (method.isAnnotationPresent(Subscribe.class)) {
					params = method.getParameterTypes();
					mEventCallbacks.put(params[0], method);
					
				} else if (method.isAnnotationPresent(Produce.class)) {
					eventClass = method.getReturnType();
					mProducerCallbacks.put(eventClass, method);
				}
			}
		}

		public Method getEventCallback(Class<?> eventClass) {
			return mEventCallbacks.get(eventClass);
		}

		public void dispatchEvents(
				Object obj,
				HashMap<Class<? extends Object>, HashSet<Object>> receivers,
				HashMap<Class<? extends Object>, ObjectMeta> metas) {
			
			Iterator<Entry<Class<? extends Object>, Method>> 
				producerCallbacks = mProducerCallbacks.entrySet().iterator();

			Object event;
			ObjectMeta meta;
			HashSet<Object> targetReceivers;
			Class<? extends Object> eventClass;
			Entry<Class<? extends Object>, Method> producerCallback;
			
			try {
				while (producerCallbacks.hasNext()) {
					producerCallback = producerCallbacks.next();
					eventClass = producerCallback.getKey();
					
					targetReceivers = receivers.get(eventClass);
					if (targetReceivers != null && targetReceivers.size() > 0) {
						event = produceEvent(eventClass, obj);
						if (event != null) {
							for (Object receiver : targetReceivers) {
								meta = metas.get(receiver.getClass());
								meta.dispatchEventIfCallback(eventClass, event, receiver);
							}
						}
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
		}

		public void dispatchEvents(
				HashMap<Class<? extends Object>, Object> producers,
				Object receiver,
				HashMap<Class<? extends Object>, ObjectMeta> metas) {

			Iterator<Class<? extends Object>> 
				eventClasses = mEventCallbacks.keySet().iterator();
			
			Object event;
			ObjectMeta meta;
			Object producer;
			Class<? extends Object> eventClass;
			
			try {
				while (eventClasses.hasNext()) {
					eventClass = eventClasses.next();
					producer = producers.get(eventClass);
					if (producer != null) {
						meta = metas.get(producer.getClass());
						event = meta.produceEvent(eventClass, producer);
						if (event != null) {
							dispatchEventIfCallback(eventClass, event, receiver);
						}
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

		private Object produceEvent(Class<? extends Object> eventClass, 
				Object producer) throws Exception {
			return mProducerCallbacks.get(eventClass).invoke(producer);
		}

		public void dispatchEventIfCallback(Class<? extends Object> eventClass, 
				Object event, Object receiver) throws Exception {
			Method callback = mEventCallbacks.get(eventClass);
			if (callback != null) {
				callback.invoke(receiver, event);
			}
		}
		
		public void unregisterFromProducers(Object obj,
				HashMap<Class<? extends Object>, Object>producers) {
			
			Class<? extends Object> key;
			Iterator<Class<? extends Object>> keys = mProducerCallbacks.keySet().iterator();
			while (keys.hasNext()) {
				key = keys.next();
				if (producers.remove(key) == null) {
					throw new IllegalArgumentException(
							"Unable to unregister producer, because it wasn't registered before, " + obj);
				}
			}
		}

		public void registerAtProducers(Object obj,
				HashMap<Class<? extends Object>, Object> producers) {

			Class<? extends Object> key;
			Iterator<Class<? extends Object>> keys = mProducerCallbacks.keySet().iterator();
			while (keys.hasNext()) {
				key = keys.next();
				if (producers.put(key, obj) != null) {
					throw new IllegalArgumentException(
							"Unable to register producer, because another producer is already registered, " + obj);
				}
			}
		}

		public void registerAtReceivers(Object obj,
				HashMap<Class<? extends Object>, HashSet<Object>> receivers) {
			
			Iterator<Class<? extends Object>> keys = mEventCallbacks.keySet().iterator();
			
			Class<? extends Object> key;
			HashSet<Object> eventReceivers;
			
			while (keys.hasNext()) {
				key = keys.next();
				eventReceivers = receivers.get(key);
				if (eventReceivers == null) {
					eventReceivers = new HashSet<Object>();
					receivers.put(key, eventReceivers);
				}
				if (!eventReceivers.add(obj)) {
					throw new IllegalArgumentException(
							"Unable to registered receiver because it has already been registered: " + obj);
				}
			}
		}

		public void unregisterFromReceivers(Object obj,
				HashMap<Class<? extends Object>, HashSet<Object>> receivers) {
			Iterator<Class<? extends Object>> keys = mEventCallbacks.keySet().iterator();
			
			Class<? extends Object> key;
			HashSet<Object> eventReceivers;
			boolean fail = false;
			while (keys.hasNext()) {
				key = keys.next();
				eventReceivers = receivers.get(key);
				if (eventReceivers == null) {
					fail = true;
				} else {
					fail = !eventReceivers.remove(obj);
				}
				if (fail) {
					throw new IllegalArgumentException(
							"Unregistering receiver which was not registered before: " + obj);
				}
			}
		}
	}	

	/**
	 * Copyright (C) 2013 The Android Open Source Project
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Modified code from AOSP
	 */
    public static class SimplePool<T> {
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
