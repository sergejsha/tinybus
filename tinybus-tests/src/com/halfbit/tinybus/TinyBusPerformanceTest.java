package com.halfbit.tinybus;

import com.halfbit.tinybus.mocks.Event1;
import com.halfbit.tinybus.mocks.Event2;

import de.greenrobot.event.EventBus;
import junit.framework.TestCase;

/**
 * Copyright notice.
 * 
 * Otto by Square, Inc.
 * 		https://github.com/square/otto
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * EventBus by Markus Junginger, greenrobot (http://greenrobot.de)
 * 		https://github.com/greenrobot/EventBus
 * 		http://www.apache.org/licenses/LICENSE-2.0
 */

public class TinyBusPerformanceTest extends TestCase {

	private static final int EVENTS_NUMBER = 10000;
	
	private Bus mTinyBus;
	private com.squareup.otto.Bus mOttoBus;
	private EventBus mEventBus;
	
	private Subsriber1 mSubscriber1;
	private Subsriber2 mSubscriber2;
	private Subsriber3 mSubscriber3;
	
	private class Subsriber1 {
		
		@Subscribe @com.squareup.otto.Subscribe
		public void onEvent(Event1 event) { }
		
		@Subscribe @com.squareup.otto.Subscribe 
		public void onEvent(Event2 event) { }
	}

	private class Subsriber2 {
		
		@Subscribe @com.squareup.otto.Subscribe
		public void onEvent(Event1 event) { }
	}
	
	private class Subsriber3 {
		
		@Subscribe @com.squareup.otto.Subscribe
		public void onEvent(Event2 event) { }
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		mTinyBus = new TinyBus();
		mEventBus = new EventBus();
		mOttoBus = new com.squareup.otto.Bus(new com.squareup.otto.ThreadEnforcer() {
			public void enforce(com.squareup.otto.Bus bus) {}
		});
		
		mSubscriber1 = new Subsriber1();
		mSubscriber2 = new Subsriber2();
		mSubscriber3 = new Subsriber3();
	}
	
	public void testA() {
		
	}
	
	//-- post events to empty bus
	
	public void testPostNoSubscribersTinyBus() {
		Event1 event1 = new Event1("event");
		Event2 event2 = new Event2(2);
		
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mTinyBus.post(event1);
			mTinyBus.post(event2);
		}
	}
	
	public void testPostNoSubscribersOtto() {
		Event1 event1 = new Event1("event");
		Event2 event2 = new Event2(2);
		
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mOttoBus.post(event1);
			mOttoBus.post(event2);
		}
	}
	
	public void testPostNoSubscribersEventBus() {
		Event1 event1 = new Event1("event");
		Event2 event2 = new Event2(2);
		
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mEventBus.post(event1);
			mEventBus.post(event2);
		}
	}
	
	//-- post event to subscribers
	
	public void testPostThreeDynamicSubscribersTinyBus() {
		Event1 event1 = new Event1("event");
		Event2 event2 = new Event2(2);
		
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mTinyBus.register(mSubscriber1);
			mTinyBus.register(mSubscriber2);
			mTinyBus.register(mSubscriber3);
			mTinyBus.post(event1);
			mTinyBus.post(event2);
			mTinyBus.unregister(mSubscriber3);
			mTinyBus.unregister(mSubscriber2);
			mTinyBus.unregister(mSubscriber1);
		}
	}
	
	public void testPostThreeDynamicSubscribersOtto() {
		Event1 event1 = new Event1("event");
		Event2 event2 = new Event2(2);
		
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mOttoBus.register(mSubscriber1);
			mOttoBus.register(mSubscriber2);
			mOttoBus.register(mSubscriber3);
			mOttoBus.post(event1);
			mOttoBus.post(event2);
			mOttoBus.unregister(mSubscriber3);
			mOttoBus.unregister(mSubscriber2);
			mOttoBus.unregister(mSubscriber1);
		}
	}
	
	public void testPostThreeDynamicSubscribersEventBus() {
		Event1 event1 = new Event1("event");
		Event2 event2 = new Event2(2);
		
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mEventBus.register(mSubscriber1);
			mEventBus.register(mSubscriber2);
			mEventBus.register(mSubscriber3);
			mEventBus.post(event1);
			mEventBus.post(event2);
			mEventBus.unregister(mSubscriber3);
			mEventBus.unregister(mSubscriber2);
			mEventBus.unregister(mSubscriber1);
		}
	}
	
	public void testPostThreeStaticSubscribersTinyBus() {
		Event1 event1 = new Event1("event");
		Event2 event2 = new Event2(2);
		
		mTinyBus.register(mSubscriber1);
		mTinyBus.register(mSubscriber2);
		mTinyBus.register(mSubscriber3);
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mTinyBus.post(event1);
			mTinyBus.post(event2);
		}
		mTinyBus.unregister(mSubscriber3);
		mTinyBus.unregister(mSubscriber2);
		mTinyBus.unregister(mSubscriber1);
	}
	
	public void testPostThreeStaticSubscribersOtto() {
		Event1 event1 = new Event1("event");
		Event2 event2 = new Event2(2);
		
		mOttoBus.register(mSubscriber1);
		mOttoBus.register(mSubscriber2);
		mOttoBus.register(mSubscriber3);
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mOttoBus.post(event1);
			mOttoBus.post(event2);
		}
		mOttoBus.unregister(mSubscriber3);
		mOttoBus.unregister(mSubscriber2);
		mOttoBus.unregister(mSubscriber1);
	}
	
	public void testPostThreeStaticSubscribersEventBus() {
		Event1 event1 = new Event1("event");
		Event2 event2 = new Event2(2);
		
		mEventBus.register(mSubscriber1);
		mEventBus.register(mSubscriber2);
		mEventBus.register(mSubscriber3);
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mEventBus.post(event1);
			mEventBus.post(event2);
		}
		mEventBus.unregister(mSubscriber3);
		mEventBus.unregister(mSubscriber2);
		mEventBus.unregister(mSubscriber1);
	}	
	
	//-- register and unregister subscribers
	
	public void testRegisterThreeSubscribersTinyBus() {
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mTinyBus.register(mSubscriber1);
			mTinyBus.register(mSubscriber2);
			mTinyBus.register(mSubscriber3);
			mTinyBus.unregister(mSubscriber3);
			mTinyBus.unregister(mSubscriber2);
			mTinyBus.unregister(mSubscriber1);
		}
	}
	
	public void testRegisterThreeSubscribersOtto() {
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mOttoBus.register(mSubscriber1);
			mOttoBus.register(mSubscriber2);
			mOttoBus.register(mSubscriber3);
			mOttoBus.unregister(mSubscriber3);
			mOttoBus.unregister(mSubscriber2);
			mOttoBus.unregister(mSubscriber1);
		}
	}
	
	public void testRegisterThreeSubscribersEventBus() {
		for (int i=0; i<EVENTS_NUMBER; i++) {
			mEventBus.register(mSubscriber1);
			mEventBus.register(mSubscriber2);
			mEventBus.register(mSubscriber3);
			mEventBus.unregister(mSubscriber3);
			mEventBus.unregister(mSubscriber2);
			mEventBus.unregister(mSubscriber1);
		}
	}	
	
	public void testZ() {
		
	}
	
}
