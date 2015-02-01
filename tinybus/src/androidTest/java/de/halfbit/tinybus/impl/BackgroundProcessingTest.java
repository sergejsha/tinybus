package de.halfbit.tinybus.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.test.InstrumentationTestCase;
import android.test.UiThreadTest;
import de.halfbit.tinybus.Bus;
import de.halfbit.tinybus.Produce;
import de.halfbit.tinybus.Subscribe;
import de.halfbit.tinybus.Subscribe.Mode;
import de.halfbit.tinybus.mocks.Callbacks;
import de.halfbit.tinybus.mocks.Event1;
import de.halfbit.tinybus.mocks.Event2;
import de.halfbit.tinybus.TinyBus;

public class BackgroundProcessingTest extends InstrumentationTestCase {

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
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		bus = new TinyBus();
		bus.register(new Object() {
			@Subscribe(mode=Mode.Background)
			public void onEvent(String event) {
				stringResult = event;
				latch.countDown();
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
			@Subscribe(mode=Mode.Background)
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
			
			@Subscribe(mode=Mode.Background)
			public void onEvent(String event) {
				onCallback(event);
				latch.countDown();
			}
			
			@Subscribe(mode=Mode.Background)
			public void onEvent(Event1 event) {
				onCallback(event);
				latch.countDown();
			}
			
			@Subscribe(mode=Mode.Background)
			public void onEvent(Event2 event) {
				onCallback(event);
				latch.countDown();
			}
			
			@Subscribe(mode=Mode.Background)
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
		callback.assertSameEvents(event1, event2, event3, event4, eventX);
		
	}

	@UiThreadTest
	public void testPostMainReceiveBackgroundSameEvents() throws Exception {
		
		final int eventCount = 100;
		
		final CountDownLatch latch = new CountDownLatch(eventCount);
		bus = new TinyBus(getInstrumentation().getContext());
		
		final Callbacks callback = new Callbacks() {
			@Subscribe(mode=Mode.Background)
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
		
		latch.await(5, TimeUnit.SECONDS);
		
		assertEquals(0, latch.getCount());
		assertEquals(eventCount, callback.getEventsCount());
		callback.assertSameEvents(sentEvents);
		
	}
	
	@UiThreadTest
	public void testProduceIntoBackground() throws Throwable {
		
		final String producerEvent = "producer 1";
		final String event1 = "event 1";
		final Object event2 = new Object();
		final String event3 = "event 3";

		final CountDownLatch latch = new CountDownLatch(5);
		final Callbacks callback1 = new Callbacks() {
			
			@Subscribe(mode=Mode.Background)
			public void onEvent(String event) {
				onCallback(event);
				latch.countDown();
			}
			
			@Subscribe
			public void onEvent(Object event) {
				onCallback(event);
				latch.countDown();
			}
		};

		final Callbacks producer = new Callbacks() {
			
			@Produce
			public String getProviderEvent() {
				return producerEvent;
			}
			
			@Subscribe
			public void onEvent(Object event) {
				onCallback(event);
			}
			
		};
		
		bus = new TinyBus(getInstrumentation().getContext());
		bus.register(callback1);
		bus.post(event1);
		bus.register(producer);
		bus.post(event2);
		bus.post(event3);
		
		bus.unregister(producer);
		bus.register(producer);
		
		latch.await(6, TimeUnit.SECONDS);
		
		// because of async delivery we cannot guaranty event sequence
		callback1.assertEventsAnyOrder(event1, producerEvent, event2, event3, producerEvent);
		producer.assertSameEvents(event2);
	}
	
	@UiThreadTest
	public void testBackgroundCallbackWithTwoParameters() throws Throwable {
		
		final CountDownLatch latch = new CountDownLatch(1);
		Callbacks subscriber = new Callbacks() {
			@Subscribe(mode=Mode.Background, queue="global")
			public void onEvent(String event, Bus bus) {
				onCallback(event);
				onCallback(bus);
				latch.countDown();
			}
		};
		
		bus = new TinyBus(getInstrumentation().getContext());
		bus.register(subscriber);
		
		bus.post("event");
		latch.await(3, TimeUnit.SECONDS);
		
		subscriber.assertSameEvents("event", bus);
	}
	
	@UiThreadTest
	public void testTwoSubscribersSameInstance() throws Throwable {
		
		final Callbacks subscriber = new Callbacks() {
			
			@Subscribe(mode=Mode.Background)
			public void onEventBackground(Event1 event) {
				synchronized (this) {
					onCallback(event);
				}
			}
			
			@Subscribe
			public void onEventMain(Event1 event) {
				synchronized (this) {
					onCallback(event);
				}
			}
		};
		
		bus = new TinyBus(getInstrumentation().getContext());
		try {
			bus.register(subscriber);
			fail();
		} catch (IllegalArgumentException e) {
			// OK
		}
		
	}
	
	//-- post from background
	
	public void testPostBackgroundReceiveMainThread() throws Throwable {
		
		stringResult = null;
		
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
