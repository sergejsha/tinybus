package com.halfbit.tinybus.impl;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.test.InstrumentationTestCase;
import android.test.UiThreadTest;

import com.halfbit.tinybus.Subscribe;
import com.halfbit.tinybus.Subscribe.Mode;
import com.halfbit.tinybus.TinyBus;

public class TinyBusWithBackgroundQueuesTest extends InstrumentationTestCase {

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
	public void testPostSingleEventToSingleQueue() throws Exception {
		
		TinyBus bus = TinyBus.from(getInstrumentation().getContext());
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
		
		bus.register(new Object() {
			@Subscribe(mode = Mode.Background, queue="queue1")
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
		
		bus.post("event");
		latch.await(3, TimeUnit.SECONDS);

		
		assertEventsNumber(2);
		ArrayList<String> eventsReduceList = new ArrayList<String>();
		eventsReduceList.add("event0");
		eventsReduceList.add("event1");
		eventsReduceList.remove(results.get(0).event);
		eventsReduceList.remove(results.get(1).event);
		assertEquals(0, eventsReduceList.size());
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
