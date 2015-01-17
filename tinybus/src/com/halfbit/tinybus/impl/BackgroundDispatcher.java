package com.halfbit.tinybus.impl;

import android.os.Process;

import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.impl.ObjectsMeta.EventCallback;
import com.halfbit.tinybus.impl.Task.TaskQueue;

public class BackgroundDispatcher implements Runnable {

	private final TaskQueue mTaskQueue;
	private Thread mBackgroundThread;
	
	public BackgroundDispatcher() {
		mTaskQueue = new TaskQueue();
		mBackgroundThread = new Thread(this, "tinybus-background");
		mBackgroundThread.start();
	}
	
	public void dispatchEvent(TinyBus bus, EventCallback eventCallback, 
			Object receiver, Object event) throws Exception {
		final Task task = Task.obtainTask(bus, Task.RUNNABLE_DISPATCH_BACKGROUND_EVENT, null)
				.setupDispatchEventHandler(eventCallback, receiver, event);
		
		synchronized (mTaskQueue) {
			mTaskQueue.offer(task);
			mTaskQueue.notifyAll();
		}
	}

	@Override
	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		
		Task task;
		while (true) {
			synchronized (mTaskQueue) {
				task = mTaskQueue.poll();
			}
			
			if (task == null) {
				synchronized (mTaskQueue) {
					try {
						mTaskQueue.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
			} else {
				try {
					task.dispatchInBackground();
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					task.recycle();
				}
			}
		}
	}
	
}
