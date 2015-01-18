package com.halfbit.tinybus.impl;

import junit.framework.TestCase;

import com.halfbit.tinybus.Produce;
import com.halfbit.tinybus.Subscribe;
import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.mocks.Callbacks;
import com.halfbit.tinybus.mocks.Event1;
import com.halfbit.tinybus.mocks.Event2;
import com.halfbit.tinybus.mocks.Event3;

public class CascadingRegisteringPostingUnregisteringTest extends TestCase {

	protected class Registrator extends Callbacks {
		
		public Event2 event = new Event2(20);
		
		@Subscribe
		public void onEvent(Event1 event) {
			onCallback(event);
			bus.register(dynamicProducer);
			bus.post(this.event);
		}
		
		@Subscribe
		public void onEvent(Event2 event) {
			onCallback(event);
			bus.register(dynamicSubscriber);
			bus.unregister(this);
		}
		
		@Subscribe
		public void onEvent(Object event) {
			onCallback(event);
		}
	} 
	
	protected class DynamicProducer extends Callbacks {
		
		public Object event = new Object();
		
		@Subscribe
		public void onEvent(Event3 event) {
			onCallback(event);
		}
		
		@Produce
		public Object getEvent() {
			return event;
		}
	}
	
	protected class DynamicSubscriber extends Callbacks {
		
		@Subscribe
		public void onEvent(Object event) {
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
		
		@Subscribe
		public void onEvent(Object event) {
			onCallback(event);
		}
	}
	
	private TinyBus bus;
	private Registrator registrator;
	private DynamicSubscriber dynamicSubscriber;
	private DynamicProducer dynamicProducer;
	private ListeningSubscriber listeningSubscriber1;
	private ListeningSubscriber listeningSubscriber2;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		bus = new TinyBus();
		registrator = new Registrator();
		listeningSubscriber1 = new ListeningSubscriber();
		listeningSubscriber2 = new ListeningSubscriber();
		dynamicSubscriber = new DynamicSubscriber();
		dynamicProducer = new DynamicProducer();
	}

	@Override
	protected void tearDown() throws Exception {
		bus = null;
		registrator = null; 
		listeningSubscriber1 = null;
		listeningSubscriber2 = null;
		dynamicSubscriber = null;
		dynamicProducer = null;
		super.tearDown();
	}
	
	public void testCascadingRegistration() {
		bus.register(listeningSubscriber1);
		bus.register(registrator);
		bus.register(listeningSubscriber2);
		
		Event1 event1 = new Event1("event1");
		bus.post(event1);
		
		Object event2 = new Object();
		bus.post(event2);
		
		listeningSubscriber1.assertSameEvents(event1, dynamicProducer.event, registrator.event, event2);
		registrator.assertSameEvents(event1, dynamicProducer.event, registrator.event);
		dynamicProducer.assertNoEvents();
		dynamicSubscriber.assertSameEvents(dynamicProducer.event, event2);
		listeningSubscriber2.assertSameEvents(event1, dynamicProducer.event, registrator.event, event2);
	}
}
