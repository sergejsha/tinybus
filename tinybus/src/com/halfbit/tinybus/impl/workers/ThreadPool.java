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

import com.halfbit.tinybus.impl.Task;

class ThreadPool {

	// context
	private final Dispatcher mDispatcher;
	private final WorkerThread[] mThreads;

	public ThreadPool(Dispatcher dispatcher, int size) {
		mDispatcher = dispatcher;
		mThreads = new WorkerThread[size];
	}

	boolean processTask(Task task) {
		mDispatcher.assertDispatcherThread();
		
		boolean taskAccepted = false;
		final int size = mThreads.length;
		for(int i=0; i<size; i++) {
			WorkerThread thread = mThreads[i];
			if (thread == null) {
				thread = new WorkerThread(this, "tinybus-worker-" + i);
				thread.start();
				mThreads[i] = thread;
			}
			
			taskAccepted = thread.processTask(task);
			if (taskAccepted) {
				break;
			} // else, try our luck with the next thread
		}
		
		return taskAccepted;
	}

	void onTaskProcessed(Task task) {
		mDispatcher.onTaskProcessed(task);
	}

	public void destroy() {
		mDispatcher.assertDispatcherThread();
		
		final int size = mThreads.length;
		for(int i=0; i<size; i++) {
			WorkerThread thread = mThreads[i];
			if (thread != null) {
				thread.stopIt();
			}
		}
	}

}
