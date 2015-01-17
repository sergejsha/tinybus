package com.halfbit.tinybus.impl.workers;

import com.halfbit.tinybus.impl.Task;
import com.halfbit.tinybus.impl.Task.TaskQueue;

public class SerialTaskQueue extends TaskQueue {

	private final String mQueueName;
	
	private boolean mHasTaskInProcess;
	private int mSize;
	
	public SerialTaskQueue(String queueName) {
		mQueueName = queueName;
	}
	
	public String getQueueName() {
		return mQueueName;
	}
	
	public void setHasTaskInProcess(boolean hasTaskInProcess) {
		mHasTaskInProcess = hasTaskInProcess;
	}
	
	public boolean hasTaskInProcess() {
		return mHasTaskInProcess;
	}
	
	@Override
	public void offer(Task task) {
		super.offer(task);
		mSize++;
	}
	
	@Override
	public Task poll() {
		mSize--;
		return super.poll();
	}
	
	public int getSize() {
		return mSize;
	}
}
