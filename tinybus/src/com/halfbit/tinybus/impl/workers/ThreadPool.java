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
			
			taskAccepted = thread.process(task);
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
