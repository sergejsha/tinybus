package com.halfbit.tinybus.impl;

import junit.framework.TestCase;

import com.halfbit.tinybus.Produce;
import com.halfbit.tinybus.Subscribe;
import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.mocks.Callbacks;
import com.halfbit.tinybus.mocks.Event1;
import com.halfbit.tinybus.mocks.Event2;
import com.halfbit.tinybus.mocks.Event3;

public class RegisterUnregisterInsidePostTest extends TestCase {

	protected class DynamicSubscriber extends Callbacks {
		@Subscribe
		public void onEvent(Event3 event) {
			onCallback(event);
		}
	} 
	
	protected class DynamicProducer extends Callbacks {
		
		public Event3 lastEvent = new Event3("event3", 76);
		
		@Subscribe
		public void onEvent(Event1 event) {
			onCallback(event);
		}
		
		@Produce
		public Event3 getEvent() {
			return lastEvent;
		}
	} 
	
	protected class RegisteringSubscriber extends Callbacks {
		
		@Subscribe
		public void onEvent(Event1 event) {
			onCallback(event);
			bus.register(dynamicProducer);
		}
		
		@Subscribe
		public void onEvent(Event2 event) {
			onCallback(event);
			bus.register(dynamicSubscriber);
			
		}
		
		@Subscribe
		public void onEvent(Event3 event) {
			onCallback(event);
		}
	} 
	
	protected class SelfUnregisteringProducer extends Callbacks {

		public Event1 lastEvent = new Event1("event1");
		
		@Subscribe
		public void onEvent2(Event2 event) {
			onCallback(event);
			bus.unregister(this);
		}

		@Produce
		public Event1 getEvent1() {
			return lastEvent;
		}
	}
	
	protected class SelfUnregisteringSubscriber extends Callbacks {
		
		@Subscribe
		public void onEvent1(Event1 event) {
			onCallback(event);
			bus.unregister(this);
		}
		
		@Subscribe
		public void onEvent2(Event2 event) {
			onCallback(event);
		}
	}
	
	protected class ListeningSubscriber extends Callbacks {
		
		@Subscribe
		public void onEvent1(Event1 event) {
			onCallback(event);
		}
		
		@Subscribe
		public void onEvent2(Event2 event) {
			onCallback(event);
		}
		
		@Subscribe
		public void onEvent3(Event3 event) {
			onCallback(event);
		}
	}
	
	private TinyBus bus;
	private SelfUnregisteringSubscriber selfUnregisteringSubscriber;
	private SelfUnregisteringProducer selfUnregisteringProducer;
	private ListeningSubscriber listeningSubscriber1;
	private ListeningSubscriber listeningSubscriber2;
	private RegisteringSubscriber registeringSubscriber;
	private DynamicSubscriber dynamicSubscriber;
	private DynamicProducer dynamicProducer;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		bus = new TinyBus();
		listeningSubscriber1 = new ListeningSubscriber();
		listeningSubscriber2 = new ListeningSubscriber();
		selfUnregisteringSubscriber = new SelfUnregisteringSubscriber();
		selfUnregisteringProducer = new SelfUnregisteringProducer();
		registeringSubscriber = new RegisteringSubscriber();
		dynamicSubscriber = new DynamicSubscriber();
		dynamicProducer = new DynamicProducer();
	}

	@Override
	protected void tearDown() throws Exception {
		bus = null;
		listeningSubscriber1 = null;
		listeningSubscriber2 = null;
		selfUnregisteringSubscriber = null;
		selfUnregisteringProducer = null;
		registeringSubscriber = null;
		dynamicSubscriber = null;
		dynamicProducer = null;
		super.tearDown();
	}

	public void testUnregisterSubscriberInsidePost() {
		bus.register(listeningSubscriber1);
		bus.register(selfUnregisteringSubscriber);
		bus.register(listeningSubscriber2);
		
		Event1 event1 = new Event1("event1");
		Event2 event2 = new Event2(2);
		Event1 event3 = new Event1("event3");
		
		bus.post(event1);
		bus.post(event2);
		bus.post(event3);
		
		listeningSubscriber1.assertEvents(event1, event2, event3);
		selfUnregisteringSubscriber.assertEvents(event1);
		listeningSubscriber1.assertEvents(event1, event2, event3);
	}
	
	public void testUnregisterProducerInsidePost() {
		bus.register(listeningSubscriber1);
		bus.register(selfUnregisteringProducer);
		
		Event1 event1 = new Event1("event1");
		Event2 event2 = new Event2(2);
		Event1 event3 = new Event1("event3");

		bus.post(event1);
		bus.post(event2);
		
		bus.register(listeningSubscriber2);
		bus.post(event3);
		
		listeningSubscriber1.assertEvents(selfUnregisteringProducer.lastEvent, event1, event2, event3);
		selfUnregisteringProducer.assertEvents(event2);
		listeningSubscriber2.assertEvents(event3);
	}
	
	public void testRegisterSubscriber() {
		bus.register(listeningSubscriber1);
		bus.register(registeringSubscriber);
		bus.register(listeningSubscriber2);
		
		Event1 event1 = new Event1("event1");
		Event2 event2 = new Event2(2);
		Event3 event3 = new Event3("event3", 3);
		
		bus.post(event1);
		bus.post(event2);
		bus.post(event3);
		
		listeningSubscriber1.assertEvents(event1, dynamicProducer.lastEvent, event2, event3);
		registeringSubscriber.assertEvents(event1, dynamicProducer.lastEvent, event2, event3);
		dynamicProducer.assertNoEvents();
		dynamicSubscriber.assertEvents(dynamicProducer.lastEvent, event3);
		listeningSubscriber2.assertEvents(event1, dynamicProducer.lastEvent, event2, event3);
	}
	
}
