package com.halfbit.tinybus.mocks;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.Assert;

public abstract class Callbacks {

	public static interface EventIterator {
		void onEvent(Object event);
	}
	
	private final ArrayList<Object> mEvents = new ArrayList<Object>();
	
	protected synchronized void onCallback(Object event) {
		mEvents.add(event);
	}
	
	public synchronized void clearEvents() {
		mEvents.clear();
	}
	
	public synchronized int getEventsCount() {
		return mEvents.size();
	}
	
	public void iterate(EventIterator iterator) {
		for (Object event : mEvents) {
			iterator.onEvent(event);
		}
	}
	
	public void assertNullEvent() {
		Assert.assertEquals(1, mEvents.size());
		Assert.assertSame(null, mEvents.get(0));
	}
	
	public void assertSameEventsList(ArrayList<Object> expectedEvents) {
		Assert.assertEquals(expectedEvents.size(), mEvents.size());
		for(int i=0; i<expectedEvents.size(); i++) {
			Assert.assertSame(expectedEvents.get(0), mEvents.get(0));
		}
	}
	
	public void assertSameEvents(Object... expectedEvents) {
		Assert.assertEquals(expectedEvents.length, mEvents.size());
		for(int i=0; i<expectedEvents.length; i++) {
			Assert.assertSame(expectedEvents[i], mEvents.get(i));
		}
	}
	
	public void assertEqualEvents(Object... expectedEvents) {
		Assert.assertEquals(expectedEvents.length, mEvents.size());
		for(int i=0; i<expectedEvents.length; i++) {
			Assert.assertEquals(expectedEvents[i], mEvents.get(i));
		}
	}
	
	public void assertEventsAnyOrder(Object... expectedEvents) {
		Assert.assertEquals(expectedEvents.length, mEvents.size());
		ArrayList<Object> events = new ArrayList<Object>(Arrays.asList(expectedEvents));
		
		for(int i=0; i<expectedEvents.length; i++) {
			Assert.assertTrue("cannot find event: " + expectedEvents[i], events.remove(expectedEvents[i]));
		}
		Assert.assertEquals("unexpected events: " + events, 0, events.size());
	}
	
	public void assertNoEvents() {
		Assert.assertEquals(0, mEvents.size());
	}
	
}
