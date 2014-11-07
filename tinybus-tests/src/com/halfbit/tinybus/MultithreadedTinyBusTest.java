package com.halfbit.tinybus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.test.InstrumentationTestCase;
import android.test.UiThreadTest;

import com.halfbit.tinybus.Subscribe.Mode;
import com.halfbit.tinybus.mocks.Callbacks;
import com.halfbit.tinybus.mocks.Event1;
import com.halfbit.tinybus.mocks.Event2;

public class MultithreadedTinyBusTest extends InstrumentationTestCase {

	private Bus bus;
	private String stringResult;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		stringResult = null;
		bus = null;
	}
	
	public void testRegisterInWrongThread() throws Throwable {
		runTestOnUiThread(new Runnable() {
			public void run() {
				bus = new TinyBus();
			}
		});
		try {
			bus.register(new Object());
			fail("exception expected");
		} catch (IllegalStateException e) {
			// OK
		}
	}
	
	public void testUnregisterInWrongThread() throws Throwable {
		runTestOnUiThread(new Runnable() {
			public void run() {
				bus = new TinyBus();
				bus.register(new Object());
			}
		});
		try {
			bus.unregister(new Object());
			fail("exception expected");
		} catch (IllegalStateException e) {
			// OK
		}
	}

	//-- post from main
	
	@UiThreadTest
	public void testPostMainReceiveMainThreadConstructor1() {
		bus = new TinyBus();
		
		bus.register(new Object () {
			@Subscribe
			public void onEvent(String event) {
				stringResult = event;
			} 
		});
		
		bus.post("event 1");
		assertEquals("event 1", stringResult);
	}
	
	@UiThreadTest
	public void testPostMainReceiveMainThreadConstructor2() {
		bus = new TinyBus(getInstrumentation().getContext());
		
		bus.register(new Object () {
			@Subscribe
			public void onEvent(String event) {
				stringResult = event;
			} 
		});
		
		bus.post("event 1");
		assertEquals("event 1", stringResult);
	}
	
	@UiThreadTest
	public void testPostMainReceiveBackgroundWrongConstructor() {
		
		bus = new TinyBus();
		bus.register(new Object() {
			@Subscribe(Mode.Background)
			public void onEvent(String event) {
				stringResult = event;
			}
		});
		
		try {
			bus.post("event a");
			fail();
		} catch (IllegalStateException e) {
			// OK
		}
	}
	
	@UiThreadTest
	public void testPostMainReceiveBackground() throws Exception {
		
		final CountDownLatch latch = new CountDownLatch(1);
		bus = new TinyBus(getInstrumentation().getContext());
		
		bus.register(new Object() {
			@Subscribe(Mode.Background)
			public void onEvent(String event) {
				stringResult = event;
				latch.countDown();
			}
		});
		
		bus.post("event a");
		latch.await(3, TimeUnit.SECONDS);
		
		assertEquals("event a", stringResult);
	}
	
	@UiThreadTest
	public void testPostMainReceiveBackgroundDifferentEvents() throws Exception {
		
		final CountDownLatch latch = new CountDownLatch(5);
		bus = new TinyBus(getInstrumentation().getContext());
		
		final Callbacks callback = new Callbacks() {
			
			@Subscribe(Mode.Background)
			public void onEvent(String event) {
				onCallback(event);
				latch.countDown();
			}
			
			@Subscribe(Mode.Background)
			public void onEvent(Event1 event) {
				onCallback(event);
				latch.countDown();
			}
			
			@Subscribe(Mode.Background)
			public void onEvent(Event2 event) {
				onCallback(event);
				latch.countDown();
			}
			
			@Subscribe(Mode.Background)
			public void onEvent(Object event) {
				onCallback(event);
				latch.countDown();
			}
			
		};
		
		bus.register(callback);
		
		final String event1 = "event 1";
		final Event1 event2 = new Event1("event 2");
		final Event2 event3 = new Event2(3);
		final String event4 = "event 4";
		final Object eventX = new Object();
		
		bus.post(event1);
		bus.post(event2);
		bus.post(event3);
		bus.post(event4);
		bus.post(eventX);
		
		latch.await(3, TimeUnit.SECONDS);
		
		assertEquals(5, callback.getEventsCount());
		callback.assertEvents(event1, event2, event3, event4, eventX);
		
	}

	@UiThreadTest
	public void testPostMainReceiveBackgroundSameEvents() throws Exception {
		
		final int eventCount = 100;
		
		final CountDownLatch latch = new CountDownLatch(eventCount);
		bus = new TinyBus(getInstrumentation().getContext());
		
		final Callbacks callback = new Callbacks() {
			
			@Subscribe(Mode.Background)
			public void onEvent(String event) {
				onCallback(event);
				latch.countDown();
			}
		};
		
		bus.register(callback);
		
		final Object[] sentEvents = new Object[eventCount];
		for (int i=0; i<eventCount; i++) {
			sentEvents[i] = "event " + i; 
			bus.post(sentEvents[i]);
		}
		
		latch.await(3, TimeUnit.SECONDS);
		
		assertEquals(eventCount, callback.getEventsCount());
		callback.assertEvents(sentEvents);
		
	}
	
	//-- post from background
	
	public void testPostBackgroundReceiveMainThread() throws Throwable {
		
		runTestOnUiThread(new Runnable() {
			public void run() {
				bus = new TinyBus();
				bus.register(new Object () {
					@Subscribe
					public void onEvent(String event) {
						stringResult = event;
					}
				});
			}
		});
		getInstrumentation().waitForIdleSync();
		
		Thread thread = new Thread(new Runnable() {
			public void run() {
				bus.post("event 1");
			}
		});
		thread.start();
		thread.join();
		getInstrumentation().waitForIdleSync();
		
		assertEquals("event 1", stringResult);
	}
	
}
