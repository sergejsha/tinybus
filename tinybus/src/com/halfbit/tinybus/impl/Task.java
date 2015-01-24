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
package com.halfbit.tinybus.impl;

import java.lang.ref.WeakReference;

import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.impl.ObjectsMeta.EventCallback;

public class Task implements Runnable {
	
	private static final TaskPool POOL = new TaskPool(24);
	
	public static final int CODE_REGISTER = 0;
	public static final int CODE_UNREGISTER = 1;
	public static final int CODE_POST_EVENT = 2;
	
	public static final int BACKGROUND_DISPATCH_FROM_BACKGROUND = 10;
	public static final int BACKGROUND_DISPATCH_IN_BACKGROUND = 11;
	
	public Task prev;
	
	public TinyBus bus;
	public int code;
	public Object obj;
	
	// runnable dispatch
	public EventCallback eventCallback;
	public WeakReference<Object> receiverRef;
	
	Task() { }
	
	public static Task obtainTask(TinyBus bus, int code, Object obj) {
		Task task;
		synchronized (POOL) {
			task = POOL.acquire();
		}
		task.bus = bus;
		task.code = code;
		task.obj = obj;
		task.prev = null;
		return task;
	}
	
	public void recycle() {
		bus = null;
		obj = null;
		synchronized (POOL) {
			POOL.release(this);
		}
	}

	@Override
	public void run() {
		if (code != BACKGROUND_DISPATCH_FROM_BACKGROUND) {
			throw new IllegalStateException("Assertion. Expected task " 
					+ BACKGROUND_DISPATCH_FROM_BACKGROUND + " while received " + code);
		}
		code = CODE_POST_EVENT;
		bus.post(obj);
		recycle();
	}
	
	//-- handling dispatch event
	
	public Task setupDispatchEventHandler(EventCallback eventCallback, Object receiver, Object event) {
		this.eventCallback = eventCallback;
		this.receiverRef = new WeakReference<Object>(receiver);
		this.obj = event;
		return this;
	}
	
	public void dispatchInBackground() throws Exception {
		if (code != BACKGROUND_DISPATCH_IN_BACKGROUND) {
			throw new IllegalStateException("Assertion. Expected task " 
					+ BACKGROUND_DISPATCH_IN_BACKGROUND + " while received " + code);
		}
		
		final Object receiver = this.receiverRef.get();
		if (receiver != null) {
			if (eventCallback.method.getParameterTypes().length == 2) {
				// expect callback with two parameters
				eventCallback.method.invoke(receiver, obj, this.bus);
			} else {
				// expect callback with a single parameter
				eventCallback.method.invoke(receiver, obj);
			}
		}
	}
	
	//-- static classes
	
	// task pool for better reuse of task instances
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