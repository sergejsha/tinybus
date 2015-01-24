/*
 * Copyright (C) 2015 Sergej Shafarenka, halfbit.de
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
	
	public boolean processTask(Task task) {
		synchronized (mLock) {
			if (mTask != null) {
				return false;
			}
			mTask = task;
			mLock.notify();
			return true;
		}
	}

	public void stopIt() {
		mRunning.set(false);
	}	
	
	@Override
	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		
		while (mRunning.get()) {

			// wait for task 
			synchronized (mLock) {
				while (mTask == null) {
					try {
						mLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			try {
				// process task
				mTask.dispatchInBackground();
				
			} catch (Exception e) {
				throw new RuntimeException(e);
				
			} finally {
				Task task = mTask;
				synchronized (mLock) {
					mTask = null;
				}
				mThreadPool.onTaskProcessed(task);
			}
		}
	}
}
