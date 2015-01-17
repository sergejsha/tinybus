package com.halfbit.tinybus.impl;

import junit.framework.TestCase;

import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.mocks.Producer1;
import com.halfbit.tinybus.mocks.Subscriber1;
import com.halfbit.tinybus.mocks.Subscriber2;

public class HasRegisteredObjectTest extends TestCase {

	private TinyBus bus;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		bus = new TinyBus();
	}

	@Override
	protected void tearDown() throws Exception {
		bus = null;
		super.tearDown();
	}

	public void testNotRegistered() {
		Object obj = new Object();
		assertFalse(bus.hasRegistered(obj));
	}

	public void testRegisteredSubscriber() {
		Subscriber1 subscriber = new Subscriber1();
		
		bus.register(subscriber);
		assertTrue(bus.hasRegistered(subscriber));
	}

	public void testUnregisteredSubscriber() {
		Subscriber1 subscriber = new Subscriber1();
		
		bus.register(subscriber);
		assertTrue(bus.hasRegistered(subscriber));
		bus.unregister(subscriber);
		assertFalse(bus.hasRegistered(subscriber));
	}
	
	public void testRegisteredProducer() {
		Producer1 producer = new Producer1();
		
		bus.register(producer);
		assertTrue(bus.hasRegistered(producer));
	}
	
	public void testUnregisteredProducer() {
		Producer1 producer = new Producer1();
		
		bus.register(producer);
		assertTrue(bus.hasRegistered(producer));
		bus.unregister(producer);
		assertFalse(bus.hasRegistered(producer));
	}
	
	public void testRegisterMultipleObjects() {
		Producer1 producer1 = new Producer1();
		Subscriber1 subscriber1 = new Subscriber1();
		Subscriber2 subscriber2 = new Subscriber2();
		
		assertFalse(bus.hasRegistered(producer1));
		assertFalse(bus.hasRegistered(subscriber1));
		assertFalse(bus.hasRegistered(subscriber2));
	
		// register one by one
		
		bus.register(producer1);
		assertTrue(bus.hasRegistered(producer1));
		assertFalse(bus.hasRegistered(subscriber1));
		assertFalse(bus.hasRegistered(subscriber2));
		
		bus.register(subscriber1);
		assertTrue(bus.hasRegistered(producer1));
		assertTrue(bus.hasRegistered(subscriber1));
		assertFalse(bus.hasRegistered(subscriber2));
		
		bus.register(subscriber2);
		assertTrue(bus.hasRegistered(producer1));
		assertTrue(bus.hasRegistered(subscriber1));
		assertTrue(bus.hasRegistered(subscriber2));
		
		// unregister one by one
		
		bus.unregister(producer1);
		assertFalse(bus.hasRegistered(producer1));
		assertTrue(bus.hasRegistered(subscriber1));
		assertTrue(bus.hasRegistered(subscriber2));
		
		bus.unregister(subscriber1);
		assertFalse(bus.hasRegistered(producer1));
		assertFalse(bus.hasRegistered(subscriber1));
		assertTrue(bus.hasRegistered(subscriber2));
		
		bus.unregister(subscriber2);
		assertFalse(bus.hasRegistered(producer1));
		assertFalse(bus.hasRegistered(subscriber1));
		assertFalse(bus.hasRegistered(subscriber2));
 	}
}
