package de.halfbit.tinybus.impl;

import junit.framework.TestCase;
import de.halfbit.tinybus.TinyBus;
import de.halfbit.tinybus.mocks.Event1;
import de.halfbit.tinybus.mocks.Event3;
import de.halfbit.tinybus.mocks.Producer1;
import de.halfbit.tinybus.mocks.Producer3;
import de.halfbit.tinybus.mocks.Subscriber2;

public class MixedProducersAndSubscribersTest extends TestCase {

	private TinyBus bus;
	private Subscriber2 subscriber2;
	private Producer1 producer1;
	private Producer3 producer3;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		bus = new TinyBus();
		subscriber2 = new Subscriber2();
		producer1 = new Producer1();
		producer3 = new Producer3();
	}

	@Override
	protected void tearDown() throws Exception {
		bus = null;
		subscriber2 = null;
		producer1 = null;
		producer3 = null;
		super.tearDown();
	}
	
	public void testRegisterMixedProducerSubscriber() {
		bus.register(producer3);

		Event1 event1 = new Event1("event1");
		bus.post(event1);
		producer3.assertSameEvents(event1);
		
		Event3 event2 = new Event3("event3", 1);
		bus.post(event2);
		producer3.assertSameEvents(event1);
		
	}

	public void testUnregisterMixedProducerSubscriber() {
		bus.register(producer3);

		Event1 event1 = new Event1("event1");
		bus.post(event1);
		producer3.assertSameEvents(event1);
		
		bus.unregister(producer3);
		bus.post(event1);
		producer3.assertSameEvents(event1);
	}

	public void testRegisterMultipleMixedProducerSubscribers() {
		
		bus.register(producer1);
		bus.register(subscriber2);
		bus.register(producer3);
		
		Event1 event1 = new Event1("event1");
		Event3 event2 = new Event3("event2", 2);
		Event3 event3 = new Event3("event3", 3);
		Event1 event4 = new Event1("event4");
		
		bus.post(event1);
		bus.post(event2);
		bus.post(event3);
		bus.post(event4);
		
		producer3.assertSameEvents(producer1.lastEvent, event1, event4);
		subscriber2.assertEventsAnyOrder(producer1.lastEvent, producer3.lastEvent,
				event1, event2, event3, event4);
	}
	
	public void testUnregisterMultipleMixedProducerSubscribers() {
		
		bus.register(producer1);
		bus.register(subscriber2);
		bus.register(producer3);
		
		bus.unregister(producer3);
		
		Event1 event1 = new Event1("event1");
		Event3 event2 = new Event3("event2", 2);
		Event3 event3 = new Event3("event3", 3);
		Event1 event4 = new Event1("event4");
		
		bus.post(event1);
		bus.post(event2);
		
		bus.unregister(subscriber2);
		
		bus.post(event3);
		bus.post(event4);

		producer3.assertSameEvents(producer1.lastEvent);
		subscriber2.assertEventsAnyOrder(producer1.lastEvent, producer3.lastEvent, event1, event2);
	}
}
