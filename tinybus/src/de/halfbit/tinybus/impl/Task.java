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

import java.lang.ref.WeakReference;

import de.halfbit.tinybus.TinyBus;
import de.halfbit.tinybus.impl.ObjectsMeta.EventCallback;

public class Task implements Runnable {
	
	public static interface TaskCallbacks {
		void onPostFromBackground(Task task);
		void onPostDelayed(Task task);
		void onDispatchInBackground(Task task) throws Exception;
	}
	
	private static final TaskPool POOL = new TaskPool(32);
	
	public static final int CODE_REGISTER = 0;
	public static final int CODE_UNREGISTER = 1;
	public static final int CODE_POST = 2;
	public static final int CODE_POST_DELAYED = 3;
	
	public static final int CODE_DISPATCH_FROM_BACKGROUND = 10;
	public static final int CODE_DISPATCH_TO_BACKGROUND = 11;
	
	// task as linked list
	public Task prev;
	
	// general purpose
	public TinyBus bus;
	public int code;
	public Object obj;
	public TaskCallbacks callbacks;
	
	// dispatch in background
	public EventCallback eventCallback;
	public WeakReference<Object> receiverRef;
	
	private Task() { }
	
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
	
	public Task setTaskCallbacks(TaskCallbacks callbacks) {
		this.callbacks = callbacks;
		return this;
	}
	
	public void recycle() {
		bus = null;
		obj = null;
		callbacks = null;
		synchronized (POOL) {
			POOL.release(this);
		}
	}

	@Override
	public void run() {
		switch (code) {
			case CODE_DISPATCH_FROM_BACKGROUND:
				callbacks.onPostFromBackground(this);
				break;
			case CODE_POST_DELAYED:
				callbacks.onPostDelayed(this);
				break;
			default: 
				throw new IllegalStateException(String.valueOf(code));
		}
	}
	
	//-- static classes
	
	/** Task pool for better reuse of task instances */
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