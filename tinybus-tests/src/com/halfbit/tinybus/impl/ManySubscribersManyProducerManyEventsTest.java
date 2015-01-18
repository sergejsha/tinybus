package com.halfbit.tinybus.impl;

import junit.framework.TestCase;

import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.mocks.Event1;
import com.halfbit.tinybus.mocks.Event2;
import com.halfbit.tinybus.mocks.Producer1;
import com.halfbit.tinybus.mocks.Producer2;
import com.halfbit.tinybus.mocks.Subscriber1;
import com.halfbit.tinybus.mocks.Subscriber2;

public class ManySubscribersManyProducerManyEventsTest extends TestCase {

	private TinyBus bus;
	private Subscriber1 subscriber1;
	private Subscriber2 subscriber2;
	private Producer1 producer1;
	private Producer2 producer2;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		bus = new TinyBus();
		subscriber1 = new Subscriber1();
		subscriber2 = new Subscriber2();
		producer1 = new Producer1();
		producer2 = new Producer2();
	}

	@Override
	protected void tearDown() throws Exception {
		bus = null;
		subscriber1 = null;
		subscriber2 = null;
		producer1 = null;
		producer2 = null;
		super.tearDown();
	}

	public void testRegisterSubscribers() {
		bus.register(subscriber1);
		bus.register(subscriber2);
		Event1 event = new Event1("event1");
		bus.post(event);
		
		subscriber1.assertSameEvents(event);
		subscriber2.assertSameEvents(event);
	}

	public void testUnregisterOneSubscriber() {
		bus.register(subscriber1);
		bus.register(subscriber2);
		bus.unregister(subscriber1);

		Event1 event = new Event1("event1");
		bus.post(event);
		subscriber1.assertNoEvents();
		subscriber2.assertSameEvents(event);
	}
	
	public void testUnregisterAllSubscribers() {
		bus.register(subscriber1);
		bus.register(subscriber2);
		bus.unregister(subscriber1);
		bus.unregister(subscriber2);
		
		bus.post(new Event1("event1"));
		subscriber1.assertNoEvents();
		subscriber2.assertNoEvents();
	}
	
	public void testSubscribersFirstBeforeSingleProducer() {
		bus.register(subscriber1);
		bus.register(subscriber2);
		bus.register(producer1);
		
		subscriber1.assertSameEvents(producer1.lastEvent);
		subscriber2.assertSameEvents(producer1.lastEvent);
	}
	
	public void testSingleProducerFirst() {
		bus.register(producer1);
		bus.register(subscriber1);
		bus.register(subscriber2);
		
		subscriber1.assertSameEvents(producer1.lastEvent);
		subscriber2.assertSameEvents(producer1.lastEvent);
	}
	
	public void testSubscribersFirstBeforeMultipleProducers() {
		bus.register(subscriber1);
		bus.register(subscriber2);
		bus.register(producer2);
		
		subscriber1.assertSameEvents(producer2.lastEvent1);
		subscriber2.assertEventsAnyOrder(producer2.lastEvent1, producer2.lastEvent2);
	}
	
	public void testMultipleProducersFirst() {
		bus.register(producer2);
		bus.register(subscriber1);
		bus.register(subscriber2);
		
		subscriber1.assertSameEvents(producer2.lastEvent1);
		subscriber2.assertEventsAnyOrder(producer2.lastEvent1, producer2.lastEvent2);
	}
	
	public void testMultipleEventsWithoutProducers() {
		bus.register(subscriber2);
		bus.register(subscriber1);
		
		Event1 event1 = new Event1("event1");
		Event1 event2 = new Event1("event2");
		Event2 event3 = new Event2(3);
		Event2 event4 = new Event2(4);
		Event1 event5 = new Event1("event5");
		Event2 event6 = new Event2(6);
		
		bus.post(event1);
		bus.post(event2);
		bus.post(event3);
		bus.post(event4);
		bus.post(event5);
		bus.post(event6);
		
		subscriber1.assertSameEvents(event1, event2, event5);
		subscriber2.assertSameEvents(event1, event2, event3, event4, event5, event6);
	}

	public void testMultipleEventsWithProducerBefore() {
		bus.register(producer1);
		bus.register(subscriber2);
		bus.register(subscriber1);
		
		Event1 event1 = new Event1("event1");
		Event1 event2 = new Event1("event2");
		Event2 event3 = new Event2(3);
		Event2 event4 = new Event2(4);
		Event1 event5 = new Event1("event5");
		Event2 event6 = new Event2(6);
		
		bus.post(event1);
		bus.post(event2);
		bus.post(event3);
		bus.post(event4);
		bus.post(event5);
		bus.post(event6);
		
		subscriber1.assertSameEvents(producer1.lastEvent, event1, event2, event5);
		subscriber2.assertSameEvents(producer1.lastEvent, event1, event2, event3, event4, event5, event6);
	}
	
	public void testMultipleEventsWithProducerAfter() {
		bus.register(subscriber2);
		bus.register(subscriber1);
		
		Event1 event1 = new Event1("event1");
		Event1 event2 = new Event1("event2");
		Event2 event3 = new Event2(3);
		Event2 event4 = new Event2(4);
		Event1 event5 = new Event1("event5");
		Event2 event6 = new Event2(6);
		
		bus.post(event1);
		bus.post(event2);
		bus.post(event3);
		bus.post(event4);
		bus.post(event5);
		bus.post(event6);

		bus.register(producer1);
		
		subscriber1.assertSameEvents(event1, event2, event5, producer1.lastEvent);
		subscriber2.assertSameEvents(event1, event2, event3, event4, event5, event6, producer1.lastEvent);
	}
	
}
