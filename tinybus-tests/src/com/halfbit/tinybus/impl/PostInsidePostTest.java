package com.halfbit.tinybus.impl;

import junit.framework.TestCase;

import com.halfbit.tinybus.Subscribe;
import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.mocks.Callbacks;
import com.halfbit.tinybus.mocks.Event1;
import com.halfbit.tinybus.mocks.Event2;

public class PostInsidePostTest extends TestCase {

	protected class SubscriberPostingDifferentEvent extends Callbacks {
		
		public Event2 sentEvent = new Event2(2);
		public Event2 sentEvent2 = new Event2(2);

		public boolean postMultipleEvents;
		
		@Subscribe
		public void onEvent1(Event1 event) {
			onCallback(event);
			bus.post(sentEvent);
			if (postMultipleEvents) {
				bus.post(sentEvent2);
			}
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
	}
	
	protected class SubscriberPostingSameEvent extends Callbacks {
		
		public Event1[] sentEvent = new Event1[] {
			new Event1("event1"), 
			new Event1("event2"),
			new Event1("event3"),
			new Event1("event4"),
			new Event1("event5")
		};
		
		@Subscribe
		public void onEvent1(Event1 event) {
			int index = getEventsCount(); 
			onCallback(event);
			if (index < sentEvent.length) {
				bus.post(sentEvent[index]);
			}
		}
	}
	
	
	private TinyBus bus;
	private SubscriberPostingDifferentEvent subscriberPostingDifferentEvent;
	private SubscriberPostingSameEvent subscriberPostingSameEvent;
	private ListeningSubscriber listeningSubscriber;
	
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		bus = new TinyBus();
		subscriberPostingDifferentEvent = new SubscriberPostingDifferentEvent();
		listeningSubscriber = new ListeningSubscriber();
		subscriberPostingSameEvent = new SubscriberPostingSameEvent();
	}

	@Override
	protected void tearDown() throws Exception {
		bus = null;
		subscriberPostingDifferentEvent = null;
		listeningSubscriber = null;
		subscriberPostingSameEvent = null;
		super.tearDown();
	}
	
	public void testPostDifferentEventRegistrationSequence1() {
		bus.register(subscriberPostingDifferentEvent);
		bus.register(listeningSubscriber);
		
		Event1 event1 = new Event1("event1"); 
		Event2 event2 = new Event2(2); 
		
		bus.post(event1);
		bus.post(event2);
		
		subscriberPostingDifferentEvent.assertEvents(event1, 
				subscriberPostingDifferentEvent.sentEvent, event2);
		
		listeningSubscriber.assertEvents(event1, 
				subscriberPostingDifferentEvent.sentEvent, event2);
	}

	public void testPostDifferentEventRegistrationSequence2() {
		bus.register(listeningSubscriber);
		bus.register(subscriberPostingDifferentEvent);
		
		Event1 event1 = new Event1("event1"); 
		Event2 event2 = new Event2(2); 
		
		bus.post(event1);
		bus.post(event2);
		
		subscriberPostingDifferentEvent.assertEvents(event1, 
				subscriberPostingDifferentEvent.sentEvent, event2);
		
		listeningSubscriber.assertEvents(event1, 
				subscriberPostingDifferentEvent.sentEvent, event2);
	}
	
	public void testPostMultipleDifferentEventRegistrationSequence1() {
		
		subscriberPostingDifferentEvent.postMultipleEvents = true;
		bus.register(subscriberPostingDifferentEvent);
		bus.register(listeningSubscriber);
		
		Event1 event1 = new Event1("event1"); 
		Event2 event2 = new Event2(2); 
		
		bus.post(event1);
		bus.post(event2);
		
		subscriberPostingDifferentEvent.assertEvents(event1, 
				subscriberPostingDifferentEvent.sentEvent, 
				subscriberPostingDifferentEvent.sentEvent2, event2);
		
		listeningSubscriber.assertEvents(event1, 
				subscriberPostingDifferentEvent.sentEvent, 
				subscriberPostingDifferentEvent.sentEvent2, event2);
	}
	
	public void testPostMultipleDifferentEventRegistrationSequence2() {
		
		subscriberPostingDifferentEvent.postMultipleEvents = true;
		bus.register(listeningSubscriber);
		bus.register(subscriberPostingDifferentEvent);
		
		Event1 event1 = new Event1("event1"); 
		Event2 event2 = new Event2(2); 
		
		bus.post(event1);
		bus.post(event2);
		
		subscriberPostingDifferentEvent.assertEvents(event1, 
				subscriberPostingDifferentEvent.sentEvent, 
				subscriberPostingDifferentEvent.sentEvent2, event2);
		
		listeningSubscriber.assertEvents(event1, 
				subscriberPostingDifferentEvent.sentEvent, 
				subscriberPostingDifferentEvent.sentEvent2, event2);
	}
	
	public void testPostSameEventRegistrationSequence1() {
		
		bus.register(listeningSubscriber);
		bus.register(subscriberPostingSameEvent);
		
		Event1 event1 = new Event1("event1");
		bus.post(event1);
	
		subscriberPostingSameEvent.assertEvents(event1,
				subscriberPostingSameEvent.sentEvent[0],
				subscriberPostingSameEvent.sentEvent[1],
				subscriberPostingSameEvent.sentEvent[2],
				subscriberPostingSameEvent.sentEvent[3],
				subscriberPostingSameEvent.sentEvent[4]);
		
		listeningSubscriber.assertEvents(event1,
				subscriberPostingSameEvent.sentEvent[0],
				subscriberPostingSameEvent.sentEvent[1],
				subscriberPostingSameEvent.sentEvent[2],
				subscriberPostingSameEvent.sentEvent[3],
				subscriberPostingSameEvent.sentEvent[4]);
	}
	
}
