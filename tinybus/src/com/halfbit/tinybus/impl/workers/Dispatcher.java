package com.halfbit.tinybus.impl.workers;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.impl.ObjectsMeta.EventCallback;
import com.halfbit.tinybus.impl.Task;

public class Dispatcher {

	// context
	private final ThreadPool mThreadPool;
	private final DispatcherHandler mDispatcherHandler;
	
	// two queue structures for faster access (any better options?)
	private final HashMap<String, SerialTaskQueue> mQueuesMap;
	private final ArrayList<SerialTaskQueue> mQueuesList;
	
	// state
	private Task mReservedTask;	
	
	/**
	 * Dispatcher TODO
	 */
	public Dispatcher() {
		HandlerThread thread = new HandlerThread("tinybus-dispatcher");
		thread.start();
		
		mThreadPool = new ThreadPool(this, 3);
		mQueuesMap = new HashMap<String, SerialTaskQueue>(4);
		mQueuesList = new ArrayList<SerialTaskQueue>(4);
		mDispatcherHandler = new DispatcherHandler(thread.getLooper(), this);
	}
	
	/** 
	 * Multiple bus instances can call this method, when an event needs to
	 * be dispatched to given subscriber in a background thread.
	 * 
	 * <p>This method can be called in any thread
	 */
	public void dispatchEventInBackground(TinyBus bus, EventCallback eventCallback, 
			Object receiver, Object event) {
		
		final Task task = Task
				.obtainTask(bus, Task.BACKGROUND_DISPATCH_IN_BACKGROUND, null)
				.setupDispatchEventHandler(eventCallback, receiver, event);
		
		mDispatcherHandler.postMessageProcessTask(task);
	}

	public void destroy() {
		mDispatcherHandler.postMessageDestroy();
	}
	
	void onTaskProcessed(Task task) {
		mDispatcherHandler.postMessageOnTaskProcessed(task);
	}
	
	void assertDispatcherThread() {
		if (Thread.currentThread() != mDispatcherHandler.getLooper().getThread()) {
			throw new IllegalStateException("method accessed from wrong thread");
		}
	}
	
	//-- methods called from handler
	
	void handlerProcessTask(Task task) {
		assertDispatcherThread();
		
		SerialTaskQueue taskQueue = mQueuesMap.get(task.eventCallback.queue);
		if (taskQueue == null) {
			taskQueue = new SerialTaskQueue(task.eventCallback.queue);
			mQueuesMap.put(task.eventCallback.queue, taskQueue);
			mQueuesList.add(taskQueue);
		}
		taskQueue.offer(task);

		processNextTask();
	}
	
	void handlerOnTaskProcessed(Task task) {
		assertDispatcherThread();
		
		// update task queue status
		SerialTaskQueue queue = mQueuesMap.get(task.eventCallback.queue);
		queue.setHasTaskInProcess(false);
		
		task.recycle();
		processNextTask();
	}
	
	void handlerDestroy() {
		mThreadPool.destroy();
	}
	
	private void processNextTask() {
		assertDispatcherThread();
		
		Task task = mReservedTask;
		if (task == null) {
			for(SerialTaskQueue queue : mQueuesList) {
				if (queue.hasTaskInProcess()) {
					continue;
				}
				
				task = queue.poll();
				if (task != null) {
					mReservedTask = task;
					queue.setHasTaskInProcess(true);
					break;
				}
			}
		}
		
		if (task == null) {
			return; // no tasks to process
		}

		boolean taskAccepted = mThreadPool.processTask(task);
		if (taskAccepted) {
			mReservedTask = null;
		}
	}
	
	//-- inner handler
	
	static class DispatcherHandler extends Handler {
		
		static final int MSG_PROCESS_TASK = 1;
		static final int MSG_ON_TASK_PROCESSED = 2;
		static final int MSG_DESTROY = 100;
		
		private final WeakReference<Dispatcher> mDispatcherRef;

		public DispatcherHandler(Looper looper, Dispatcher dispatcher) {
			super(looper);
			mDispatcherRef = new WeakReference<Dispatcher>(dispatcher);
		}
		
		public void handleMessage(Message msg) {
			Dispatcher dispatcher = mDispatcherRef.get();
			if (dispatcher == null) {
				msg.what = MSG_DESTROY; 
			}
			
			switch(msg.what) {
				case MSG_PROCESS_TASK: {
					dispatcher.handlerProcessTask((Task) msg.obj);
					break;
				}
				
				case MSG_ON_TASK_PROCESSED: {
					dispatcher.handlerOnTaskProcessed((Task) msg.obj);
					break;
				}
				
				case MSG_DESTROY: {
					dispatcher.handlerDestroy();
					getLooper().quit();
					break;
				}
			}
		}
		
		void postMessageProcessTask(Task task) {
			obtainMessage(MSG_PROCESS_TASK, task).sendToTarget();
		}
		
		void postMessageDestroy() {
			obtainMessage(MSG_DESTROY).sendToTarget();
		}
		
		void postMessageOnTaskProcessed(Task task) {
			obtainMessage(MSG_ON_TASK_PROCESSED, task).sendToTarget();
		}
	}

}
