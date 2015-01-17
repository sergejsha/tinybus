package com.halfbit.tinybus.impl;

import java.lang.ref.WeakReference;

import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.impl.ObjectsMeta.EventCallback;

public class Task implements Runnable {
	
	private static final TaskPool POOL = new TaskPool(12);
	
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

	//-- handling repost event
	
	public Task setupRepostHandler(TinyBus bus) {
		this.bus = bus;
		return this;
	}
	
	@Override
	public void run() {
		if (code != BACKGROUND_DISPATCH_FROM_BACKGROUND) {
			throw new IllegalStateException("Assertion. Expected task " 
					+ BACKGROUND_DISPATCH_FROM_BACKGROUND + " while received " + code);
		}
		code = CODE_POST_EVENT;
		bus.post(this);
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
				eventCallback.method.invoke(receiver, obj, this.bus);
			} else {
				eventCallback.method.invoke(receiver, obj);
			}
		}
	}
	
	//-- static classes
	
	// singly linked list as a FIFO task queue
	public static class TaskQueue {
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