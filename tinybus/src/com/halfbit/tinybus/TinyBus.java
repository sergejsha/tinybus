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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import android.content.Context;

public class TinyBus implements Bus {
	
	//-- factory methods

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
	
	//-- public api
	
	@Override
	public void register(Object obj) {
		if (mPostingEvent) {
			if (mPostponedRegisters == null) {
				mPostponedRegisters = new ArrayList<Object>();
			}
			mPostponedRegisters.add(obj);
			mHasPostponedRegisters = true;
		} else {
			registerInternal(obj);
		}
	}
	
	@Override
	public void unregister(Object obj) {
		if (mPostingEvent) {
			if (mPostponedUnregisters == null) {
				mPostponedUnregisters = new ArrayList<Object>();
			}
			mPostponedUnregisters.add(obj);
			mHasPostponedUnregisters = true;
		} else {
			unregisterInternal(obj);
		}
	}
	
	@Override
	public void post(Object event) {
		if (event == null) {
			throw new IllegalArgumentException("Event must not be null");
		}
		
		if (mPostingEvent) {
			if (mPostponedEvents == null) {
				mPostponedEvents = new LinkedList<Object>();
			}
			mPostponedEvents.add(event);
			
		} else {
			
			final HashMap<Class<?>, ObjectMeta> metas = OBJECTS_META;
			
			mPostingEvent = true;
			ArrayList<Object> callbacks;
			
			try {
				
				while (true) {
					
					final Class<?> eventClass = event.getClass();
					final HashSet<Object> receivers = mEventReceivers.get(eventClass);
					
					// dispatch current event
					if (receivers != null) {
						ObjectMeta meta;
						Method callback;
						try {
							boolean ignoreObject = false;
							for (Object receiver : receivers) {
								ignoreObject = mHasPostponedUnregisters && mPostponedUnregisters.contains(receiver);
								if (!ignoreObject) {
									meta = metas.get(receiver.getClass());
									callback = meta.getEventCallback(eventClass);
									callback.invoke(receiver, event);
								}
							}
							
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
					
					// register / unregister postponed callbacks
					if (mHasPostponedUnregisters) {
						callbacks = new ArrayList<Object>(mPostponedUnregisters);
						mPostponedUnregisters.clear();
						mHasPostponedUnregisters = false;
						for (Object object : callbacks) {
							unregisterInternal(object);
						}
					}

					if (mHasPostponedRegisters) {
						callbacks = new ArrayList<Object>(mPostponedRegisters);
						mPostponedRegisters.clear();
						mHasPostponedRegisters = false;
						for (Object object : callbacks) {
							registerInternal(object);
						}
					}

					// get next event if such, or exit
					if (mPostponedEvents != null && mPostponedEvents.size() > 0) {
						event = mPostponedEvents.removeFirst();
					} else {
						break;
					}
				}
				
			} finally {
				mPostingEvent = false;
			}
		}
	}

	//-- private methods
	
	private static HashMap<Class<?>/*receiver or producer class*/, ObjectMeta> OBJECTS_META 
		= new HashMap<Class<?>, ObjectMeta>();
	
	private HashMap<Class<?>/*event class*/, HashSet<Object>/*multiple receiver objects*/> mEventReceivers
		= new HashMap<Class<?>, HashSet<Object>>();
	
	private HashMap<Class<?>/*event class*/, Object/*single producer objects*/> mEventProducers
		= new HashMap<Class<?>, Object>(); 
	
	private boolean mPostingEvent;
	private boolean mHasPostponedUnregisters;
	private boolean mHasPostponedRegisters;
	
	private LinkedList<Object> mPostponedEvents;
	private ArrayList<Object> mPostponedRegisters;
	private ArrayList<Object> mPostponedUnregisters;

	private void registerInternal(Object obj) {
		ObjectMeta meta = OBJECTS_META.get(obj.getClass());
		if (meta == null) {
			meta = new ObjectMeta(obj);
			OBJECTS_META.put(obj.getClass(), meta);
		}
		meta.registerAtReceivers(obj, mEventReceivers);
		meta.registerAtProducers(obj, mEventProducers);
		
		// dispatch from object's producers to all receivers
		meta.dispatchEvents(obj, mEventReceivers, OBJECTS_META);
		// dispatch from other's producers to object's receivers
		meta.dispatchEvents(mEventProducers, obj, OBJECTS_META);
	}
	
	private void unregisterInternal(Object obj) {
		ObjectMeta meta = OBJECTS_META.get(obj.getClass());
		meta.unregisterFromReceivers(obj, mEventReceivers);
		meta.unregisterFromProducers(obj, mEventProducers);
	}
	
	//-- object meta
	
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
		}

		private Object produceEvent(Class<? extends Object> eventClass, Object producer) {
			Method callback = mProducerCallbacks.get(eventClass);
			try {
				return callback.invoke(producer);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public void dispatchEventIfCallback(Class<? extends Object> eventClass, Object event, Object receiver) {
			Method callback = mEventCallbacks.get(eventClass);
			if (callback != null) {
				try {
					callback.invoke(receiver, event);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		public void unregisterFromProducers(Object obj,
				HashMap<Class<? extends Object>, Object>producers) {
			
			Class<? extends Object> key;
			Iterator<Class<? extends Object>> keys = mProducerCallbacks.keySet().iterator();
			while (keys.hasNext()) {
				key = keys.next();
				if (producers.remove(key) == null) {
					throw new IllegalArgumentException("Unable to unregister producer, because it wasn't registered before, " + obj);
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
					throw new IllegalArgumentException("Unable to register producer, because another producer is already registered, " + obj);
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
					throw new IllegalArgumentException("Unable to registered receiver because another receiver is already registered: " + obj);
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
					throw new IllegalArgumentException("Unregistering receiver which was not registered: " + obj);
				}
			}
		}
	}	
	
}
