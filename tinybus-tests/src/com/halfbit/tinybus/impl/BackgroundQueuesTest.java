package com.halfbit.tinybus.impl;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.test.UiThreadTest;

import com.halfbit.tinybus.Subscribe;
import com.halfbit.tinybus.Subscribe.Mode;
import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.mocks.Callbacks;

public class BackgroundQueuesTest extends InstrumentationTestCase {

	ArrayList<CallbackResult> results;
	
	static class CallbackResult {
		public Object event;
		public String threadName;
	}
	
	void collectEvent(String event) {
		CallbackResult result = new CallbackResult();
		result.event = event;
		result.threadName = Thread.currentThread().getName();
		results.add(result);
	}
	
	void assertEventsNumber(int number) {
		assertEquals(number, results.size());
	}
	
	void assertResult(int index, Object event, String threadName) {
		CallbackResult result = results.get(index);
		assertEquals(event, result.event);
		assertEquals(threadName, result.threadName);
	}
	
	@Override
	protected void setUp() throws Exception {
		results = new ArrayList<CallbackResult>();
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		TinyBusDepot.get(getInstrumentation().getContext()).testDestroy();
		super.tearDown();
	}
	
	@UiThreadTest
	public void testSerialQueueExecution() throws Exception {
		
		final int numberOfEvents = 25;
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(numberOfEvents);

		Callbacks callbacks = new Callbacks() {
			
			@Subscribe(mode=Mode.Background, queue="test")
			public void onEvent(String event) {
				onCallback(event);
				latch.countDown();
				
				long timeout = (long) (2l * Math.random());
				if (timeout > 0l) {
					SystemClock.sleep(timeout);
				}
			}
		};
		
		bus.register(callbacks);
		
		ArrayList<Object> expected = new ArrayList<Object>();
		for(int i=0; i<numberOfEvents; i++) {
			String event = "event" + i; 
			bus.post(event);
			expected.add(event);
		}
		
		latch.await(10, TimeUnit.SECONDS);
		
		callbacks.assertSameEventsList(expected);
	}
	
	public void testNoneBlockingQueues() throws Exception {
		
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		
		final CountDownLatch latch = new CountDownLatch(200);
		final CountDownLatch blockingLatch = new CountDownLatch(100);

		final Callbacks fluentCallback = new Callbacks() {
			@Subscribe(mode=Mode.Background, queue="fluent")
			public void onEvent(String event) {
				onCallback(event);
				latch.countDown();
				blockingLatch.countDown();
			}
		};

		Callbacks blockingCallback = new Callbacks() {
			
			@Subscribe(mode=Mode.Background, queue="block")
			public void onEvent(String event) throws InterruptedException {
				
				// block on first event
				if (getEventsCount() == 0) {
					blockingLatch.await(3, TimeUnit.SECONDS);
				}

				// process, if fluent callback is completed only 
				if (fluentCallback.getEventsCount() == 100) {
					onCallback(event);
					latch.countDown();
				}
			}
		};
		
		bus.register(blockingCallback);
		bus.register(fluentCallback);
		
		ArrayList<Object> expected = new ArrayList<Object>();
		for(int i=0; i<100; i++) {
			String event = "event" + i; 
			bus.post(event);
			expected.add(event);
		}
		
		latch.await(3, TimeUnit.SECONDS);
		
		fluentCallback.assertSameEventsList(expected);
		blockingCallback.assertSameEventsList(expected);
		
	}
	
	@UiThreadTest
	public void testPostSingleEventToSingleQueue() throws Exception {
		
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(1);
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background)
			public void onEvent(String event) {
				collectEvent(event);
				latch.countDown();
			}
		});
		
		bus.post("event a");
		latch.await(3, TimeUnit.SECONDS);
		
		assertEventsNumber(1);
		assertResult(0, "event a", "tinybus-worker-0");
	}
	
	@UiThreadTest
	public void testPostSingleEventToMultipleQueues() throws Exception {
	
		final int numberOfQueues = 6;
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(numberOfQueues);
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue0")
			public void onEvent(String event) {
				collectEvent(event + "0");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue1")
			public void onEvent(String event) {
				collectEvent(event + "1");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue2")
			public void onEvent(String event) {
				collectEvent(event + "2");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue3")
			public void onEvent(String event) {
				collectEvent(event + "3");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue4")
			public void onEvent(String event) {
				collectEvent(event + "4");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue5")
			public void onEvent(String event) {
				collectEvent(event + "5");
				latch.countDown();
			}
		});
		
		bus.post("event");
		latch.await(3, TimeUnit.SECONDS);
		
		ArrayList<String> eventsReduceList = new ArrayList<String>();
		for(int i=0; i<numberOfQueues; i++) {
			eventsReduceList.add("event" + i);
		}
		
		assertEventsNumber(numberOfQueues);
		for(int i=0; i<numberOfQueues; i++) {
			eventsReduceList.remove(results.get(i).event);
		}
		assertEquals(0, eventsReduceList.size());
	}
	
	@UiThreadTest
	public void testPostSingleEventToSameQueueInTwoReceivers() throws Exception {
		
		final int numberOfEvents = 2;
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(numberOfEvents);
		
		Callbacks callbacks1, callbacks2;
		
		bus.register(callbacks1 = new Callbacks() {
			@Subscribe(mode = Mode.Background, queue="queue1")
			public void onEvent(String event) {
				onCallback(event + "0");
				latch.countDown();
			}
		});
		
		bus.register(callbacks2 = new Callbacks() {
			@Subscribe(mode = Mode.Background, queue="queue1")
			public void onEvent(String event) {
				onCallback(event + "1");
				latch.countDown();
			}
		});
		
		bus.post("event");
		latch.await(3, TimeUnit.SECONDS);

		callbacks1.assertEqualEvents("event0");
		callbacks2.assertEqualEvents("event1");
	}
	
	@UiThreadTest
	public void testPostMultipleEventsToSingleQueue() throws Exception {
		
		int numberOfEvents = 100;
		
		TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(numberOfEvents);
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background)
			public void onEvent(String event) {
				collectEvent(event);
				latch.countDown();
			}
		});
		
		for(int i=0; i<numberOfEvents; i++) {
			bus.post("event_" + i);
		}
		latch.await(3, TimeUnit.SECONDS);
		
		assertEventsNumber(numberOfEvents);
		for(int i=0; i<numberOfEvents; i++) {
			assertResult(i, "event_" + i, "tinybus-worker-0");
		}
	}

	@UiThreadTest
	public void testPostMultipleEventsToMultipleQueues() throws Exception {
	
		final int numberOfQueues = 5;
		final int numberOfEvents = 20;
		
		final TinyBus bus = TinyBus.from(getInstrumentation().getContext());
		final CountDownLatch latch = new CountDownLatch(numberOfQueues * numberOfEvents);
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue0")
			public void onEvent(String event) {
				collectEvent(event + "0");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue1")
			public void onEvent(String event) {
				collectEvent(event + "1");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue2")
			public void onEvent(String event) {
				collectEvent(event + "2");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue3")
			public void onEvent(String event) {
				collectEvent(event + "3");
				latch.countDown();
			}
		});
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue4")
			public void onEvent(String event) {
				collectEvent(event + "4");
				latch.countDown();
			}
		});
		
		for (int i=0; i<numberOfEvents; i++) {
			bus.post("event" + i);
		}
		latch.await(3, TimeUnit.SECONDS);
		
		assertEventsNumber(numberOfQueues * numberOfEvents);
		ArrayList<String> eventsReduceList = new ArrayList<String>();
		for(int i=0; i<numberOfEvents; i++) {
			eventsReduceList.add("event" + i + "0");
			eventsReduceList.add("event" + i + "1");
			eventsReduceList.add("event" + i + "2");
			eventsReduceList.add("event" + i + "3");
			eventsReduceList.add("event" + i + "4");
		}
		
		assertEventsNumber(numberOfEvents * numberOfQueues);
		for(int i=0; i<numberOfEvents * numberOfQueues; i++) {
			eventsReduceList.remove(results.get(i).event);
		}
		assertEquals(0, eventsReduceList.size());
	}
	
}
