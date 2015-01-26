package de.halfbit.tinybus.impl;

/** Simple and fast FIFO linked list of Tasks */
public class TaskQueue {
	
	protected Task head;
	protected Task tail;
	
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
			if (head == null) {
				tail = null;
			}
			return task;
		}
	}
	
	public void unpoll(Task task) {
		if (head == null) {
			head = tail = task;
		} else {
			task.prev = head;
			head = task;
		}
	}
	
	public boolean isEmpty() {
		return head == null;
	}
}
