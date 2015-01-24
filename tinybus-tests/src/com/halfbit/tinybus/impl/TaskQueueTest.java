package com.halfbit.tinybus.impl;

import junit.framework.TestCase;

public class TaskQueueTest extends TestCase {

	private TaskQueue mTaskQueue;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mTaskQueue = new TaskQueue();
	}
	
	@Override
	protected void tearDown() throws Exception {
		mTaskQueue = null;
		super.tearDown();
	}

	public void testEmptyInitiallyState() {
		assertNull(mTaskQueue.poll());
	}
	
	public void testOfferPoll() {
		mTaskQueue.offer(Task.obtainTask(null, 1, null));
		mTaskQueue.offer(Task.obtainTask(null, 2, null));
		mTaskQueue.offer(Task.obtainTask(null, 3, null));
		
		assertEquals(1, mTaskQueue.poll().code);
		assertEquals(2, mTaskQueue.poll().code);
		assertEquals(3, mTaskQueue.poll().code);
		assertNull(mTaskQueue.poll());
		
		mTaskQueue.offer(Task.obtainTask(null, 4, null));
		assertEquals(4, mTaskQueue.poll().code);
		assertNull(mTaskQueue.poll());
	}
	
	public void testUnpollMultipleTimes() {
		mTaskQueue.offer(Task.obtainTask(null, 1, null));
		mTaskQueue.offer(Task.obtainTask(null, 2, null));
		
		Task task = mTaskQueue.poll();
		assertNotNull(task);
		assertEquals(1, task.code);

		mTaskQueue.unpoll(task);
		mTaskQueue.poll();
		assertNotNull(task);
		assertEquals(1, task.code);
		
		mTaskQueue.unpoll(task);
		mTaskQueue.poll();
		assertNotNull(task);
		assertEquals(1, task.code);

		assertEquals(2, mTaskQueue.poll().code);
		assertNull(mTaskQueue.poll());
	}
	
	public void testUnpollLast() {
		mTaskQueue.offer(Task.obtainTask(null, 1, null));
		
		Task task = mTaskQueue.poll();
		assertNotNull(task);
		assertEquals(1, task.code);
		assertNull(mTaskQueue.poll());
		
		mTaskQueue.unpoll(task);
		assertEquals(1, mTaskQueue.poll().code);
		assertNull(mTaskQueue.poll());
	}
	
	public void testUnpollNotLast() {
		mTaskQueue.offer(Task.obtainTask(null, 1, null));
		mTaskQueue.offer(Task.obtainTask(null, 2, null));
		mTaskQueue.offer(Task.obtainTask(null, 3, null));
		
		Task task = mTaskQueue.poll();
		assertNotNull(task);
		assertEquals(1, task.code);
		
		mTaskQueue.unpoll(task);
		
		task = mTaskQueue.poll();
		assertNotNull(task);
		assertEquals(1, task.code);

		assertEquals(2, mTaskQueue.poll().code);
		assertEquals(3, mTaskQueue.poll().code);
		assertNull(mTaskQueue.poll());
	}

	public void testIsEmpty() {
		assertTrue(mTaskQueue.isEmpty());
		
		mTaskQueue.offer(Task.obtainTask(null, 1, null));
		assertFalse(mTaskQueue.isEmpty());
		
		Task task = mTaskQueue.poll();
		assertTrue(mTaskQueue.isEmpty());
		
		mTaskQueue.unpoll(task);
		assertFalse(mTaskQueue.isEmpty());
	}
}
