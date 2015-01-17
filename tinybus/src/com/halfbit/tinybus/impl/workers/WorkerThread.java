package com.halfbit.tinybus.impl.workers;

import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Process;

import com.halfbit.tinybus.impl.Task;

class WorkerThread extends Thread {

	// context
	private final ThreadPool mThreadPool;
	private final AtomicBoolean mRunning;
	private final Object mLock;
	
	// state
	private Task mTask;
	
	public WorkerThread(ThreadPool threadPool, String name) {
		super(name);
		mThreadPool = threadPool;
		mLock = new Object();
		mRunning = new AtomicBoolean(true);
	}
	
	public boolean process(Task task) {
		synchronized (mLock) {
			if (mTask != null) {
				return false;
			}
			mTask = task;
			mLock.notify();
		}
		return true;
	}

	public void stopIt() {
		mRunning.set(false);
	}	
	
	@Override
	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		
		Task task;
		while (mRunning.get()) {
			synchronized (mLock) {
				task = mTask;
			}
			
			if (task == null) {
				synchronized (mLock) {
					try {
						mLock.wait();
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
					synchronized (mLock) {
						mTask = null;
					}
					mThreadPool.onTaskProcessed(task);
				}
			}
		}
	}
}
