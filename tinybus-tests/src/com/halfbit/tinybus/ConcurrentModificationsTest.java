package com.halfbit.tinybus;

import junit.framework.TestCase;

import com.halfbit.tinybus.mocks.Event1;
import com.halfbit.tinybus.mocks.Event2;
import com.halfbit.tinybus.mocks.Event3;

public class ConcurrentModificationsTest extends TestCase {

	public class Subscriber {
		
		@Subscribe
		public void onEvent1(Event1 event) {
			
		}
	}

	public class Producer1 {
		
		@Produce
		public Event1 getEvent() {
			return new Event1("event1");
		}
	}
	
	public class RegisteringSubscriber {
		
		@Subscribe
		public void onEvent1(Event1 event) {
			bus.register(producer2);
		}
	}
	

	public class Producer2 {
		
		@Produce
		public Event2 getEvent() {
			return new Event2(2);
		}
		
		@Subscribe
		public void onEvent1(Event1 event) {
		}
		
		@Subscribe
		public void onEvent2(Event2 event) {
		}
	}
	
	public class Producer3 {
		
		@Produce
		public Event3 getEvent() {
			return new Event3("3", 3);
		}
	}
	
	private TinyBus bus;
	private Producer1 producer1;
	private Producer2 producer2;
	
	private Subscriber subscriber1;
	private Subscriber subscriber2;
	private Subscriber subscriber3;
	private Subscriber subscriber4;
	
	//private NewSubscriber subscriber5;
	
	private RegisteringSubscriber registeringSubscriber;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		bus = new TinyBus(new TinyBus.SingleThreadAssertion());
		
		subscriber1 = new Subscriber();
		subscriber2 = new Subscriber();
		subscriber3 = new Subscriber();
		subscriber4 = new Subscriber();
		
		registeringSubscriber = new RegisteringSubscriber();
		
		producer1 = new Producer1();
		producer2 = new Producer2();
		
	}

	@Override
	protected void tearDown() throws Exception {
		bus = null;
	}
	
	public void testRegisterPostRegisterModification() {
		
		bus.register(subscriber1);
		bus.register(subscriber2);
		bus.register(registeringSubscriber);
		bus.register(subscriber3);
		bus.register(subscriber4);
		bus.register(producer1);
		
		// OK if no exceptions
		
	}
	
}
