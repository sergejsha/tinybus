package de.halfbit.tinybus.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.test.UiThreadTest;
import de.halfbit.tinybus.Subscribe;
import de.halfbit.tinybus.Subscribe.Mode;
import de.halfbit.tinybus.mocks.Callbacks;
import de.halfbit.tinybus.mocks.Callbacks.EventIterator;
import de.halfbit.tinybus.TinyBus;

public class DelayedEventsTest extends InstrumentationTestCase {

	private static class TimedEvent {
		public final int id;
		public final long created;
		
		public TimedEvent(int id) {
			this(id, SystemClock.elapsedRealtime());
		}
		
		public TimedEvent(int id, long created) {
			this.id = id;
			this.created = created;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof TimedEvent) {
				return id == ((TimedEvent)o).id;
			}
			return false;
		}
	}
	
	private static class ReceivedTimedEvent extends TimedEvent {
		public final long received;
		
		public ReceivedTimedEvent(int id) {
			super(id);
			received = SystemClock.elapsedRealtime();
		}
		
		public ReceivedTimedEvent(TimedEvent event) {
			super(event.id, event.created);
			received = SystemClock.elapsedRealtime();
		}
		
		public long getDispatchTime() {
			return received - created;
		}
	}
	
	private TinyBus bus;
	private HandlerThread mainThread;
	private Handler handler;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		bus = null;
		
		mainThread = new HandlerThread("tinybus-test-mainthread");
		mainThread.start();
		
		handler = new Handler(mainThread.getLooper());
	}

	@Override
	protected void tearDown() throws Exception {
		mainThread.getLooper().quit();
		mainThread = null;
		bus = null;
		super.tearDown();
	}
	
	public void testPostDelayedMainThreadAndReceive() throws InterruptedException {

		final long delayTime = 100l;
		final CountDownLatch latch = new CountDownLatch(2);		
		
		final Callbacks callbacks1 = new Callbacks() {
			@Subscribe
			public void onEvent(TimedEvent event) {
				onCallback(new ReceivedTimedEvent(event));
				synchronized (latch) {
					latch.countDown();
				}
			}
		}; 
		
		final Callbacks callbacks2 = new Callbacks() {
			@Subscribe(mode=Mode.Background)
			public void onEvent(TimedEvent event) {
				onCallback(new ReceivedTimedEvent(event));
				synchronized (latch) {
					latch.countDown();
				}
			}
		}; 
		
		// initialize in main thread
		handler.post(new Runnable() {
			@Override
			public void run() {
				bus = new TinyBus(getInstrumentation().getContext());
				bus.register(callbacks1);
				bus.register(callbacks2);
				bus.postDelayed(new TimedEvent(1), delayTime);
			}
		});
		
		latch.await(3, TimeUnit.SECONDS);
		
		final ReceivedTimedEvent expected = new ReceivedTimedEvent(1);
		
		callbacks1.assertEqualEvents(expected);
		callbacks1.iterate(new EventIterator() {
			@Override
			public void onEvent(Object event) {
				ReceivedTimedEvent e = (ReceivedTimedEvent) event;
				assertTrue(e.getDispatchTime() >= delayTime);
			}
		});
		
		callbacks2.assertEqualEvents(expected);
		callbacks2.iterate(new EventIterator() {
			@Override
			public void onEvent(Object event) {
				ReceivedTimedEvent e = (ReceivedTimedEvent) event;
				assertTrue(e.getDispatchTime() >= delayTime);
			}
		});
	}

	public void testPostDelayedMainThreadRepostAndReceive() throws InterruptedException {

		final long delayTime = 100l;
		final CountDownLatch latch = new CountDownLatch(2);		
		
		final Callbacks callbacks1 = new Callbacks() {
			@Subscribe
			public void onEvent(TimedEvent event) {
				onCallback(new ReceivedTimedEvent(event));
				synchronized (latch) {
					latch.countDown();
				}
			}
		}; 
		
		final Callbacks callbacks2 = new Callbacks() {
			@Subscribe(mode=Mode.Background)
			public void onEvent(TimedEvent event) {
				onCallback(new ReceivedTimedEvent(event));
				synchronized (latch) {
					latch.countDown();
				}
			}
		}; 
		
		// initialize in main thread
		handler.post(new Runnable() {
			@Override
			public void run() {
				bus = new TinyBus(getInstrumentation().getContext());
				bus.register(callbacks1);
				bus.register(callbacks2);
				bus.postDelayed(new TimedEvent(1), delayTime);
				bus.postDelayed(new TimedEvent(2), delayTime);
			}
		});
		
		latch.await(3, TimeUnit.SECONDS);
		
		final ReceivedTimedEvent expected = new ReceivedTimedEvent(2);
		
		callbacks1.assertEqualEvents(expected);
		callbacks1.iterate(new EventIterator() {
			@Override
			public void onEvent(Object event) {
				ReceivedTimedEvent e = (ReceivedTimedEvent) event;
				assertTrue(e.getDispatchTime() >= delayTime);
			}
		});
		
		callbacks2.assertEqualEvents(expected);
		callbacks2.iterate(new EventIterator() {
			@Override
			public void onEvent(Object event) {
				ReceivedTimedEvent e = (ReceivedTimedEvent) event;
				assertTrue(e.getDispatchTime() >= delayTime);
			}
		});
	}
	
	public void testPostDelayedMainThreadAndCancel() throws InterruptedException {
		
		final long delayTime = 100l;
		
		final Callbacks callbacks1 = new Callbacks() {
			@Subscribe
			public void onEvent(TimedEvent event) {
				onCallback(new ReceivedTimedEvent(event));
			}
		}; 
		
		final Callbacks callbacks2 = new Callbacks() {
			@Subscribe(mode=Mode.Background)
			public void onEvent(TimedEvent event) {
				onCallback(new ReceivedTimedEvent(event));
			}
		}; 
		
		// initialize in main thread
		handler.post(new Runnable() {
			@Override
			public void run() {
				bus = new TinyBus(getInstrumentation().getContext());
				bus.register(callbacks1);
				bus.register(callbacks2);
				bus.postDelayed(new TimedEvent(1), delayTime);
				bus.cancelDelayed(TimedEvent.class);
			}
		});
		
		SystemClock.sleep(delayTime + 100);
		
		callbacks1.assertNoEvents();
		callbacks2.assertNoEvents();
	}
	
	public void testPostDelayedBackgroundThreadAndReceive() throws InterruptedException {
		
		final long delayTime = 100l;
		final CountDownLatch initLatch = new CountDownLatch(1);		
		final CountDownLatch latch = new CountDownLatch(2);		
		
		final Callbacks callbacks1 = new Callbacks() {
			@Subscribe
			public void onEvent(TimedEvent event) {
				onCallback(new ReceivedTimedEvent(event));
				synchronized (latch) {
					latch.countDown();
				}
			}
		}; 
		
		final Callbacks callbacks2 = new Callbacks() {
			@Subscribe(mode=Mode.Background)
			public void onEvent(TimedEvent event) {
				onCallback(new ReceivedTimedEvent(event));
				synchronized (latch) {
					latch.countDown();
				}
			}
		}; 
		
		// initialize in main thread
		handler.post(new Runnable() {
			@Override
			public void run() {
				bus = new TinyBus(getInstrumentation().getContext());
				bus.register(callbacks1);
				bus.register(callbacks2);
				initLatch.countDown();
			}
		});
		
		// wait for initialization
		initLatch.await(3, TimeUnit.SECONDS);
		
		// post from a background thread
		bus.postDelayed(new TimedEvent(1), delayTime);
		
		// wait for events
		latch.await(3, TimeUnit.SECONDS);
		
		final ReceivedTimedEvent expected = new ReceivedTimedEvent(1);
		
		callbacks1.assertEqualEvents(expected);
		callbacks1.iterate(new EventIterator() {
			@Override
			public void onEvent(Object event) {
				ReceivedTimedEvent e = (ReceivedTimedEvent) event;
				assertTrue(e.getDispatchTime() >= delayTime);
			}
		});
		
		callbacks2.assertEqualEvents(expected);
		callbacks2.iterate(new EventIterator() {
			@Override
			public void onEvent(Object event) {
				ReceivedTimedEvent e = (ReceivedTimedEvent) event;
				assertTrue(e.getDispatchTime() >= delayTime);
			}
		});
	}
	
	public void testPostDelayedBackgroundThreadAndCancel() throws InterruptedException {
		
		final long delayTime = 100l;
		final CountDownLatch initLatch = new CountDownLatch(1);		
		
		final Callbacks callbacks1 = new Callbacks() {
			@Subscribe
			public void onEvent(TimedEvent event) {
				onCallback(new ReceivedTimedEvent(event));
			}
		}; 
		
		final Callbacks callbacks2 = new Callbacks() {
			@Subscribe(mode=Mode.Background)
			public void onEvent(TimedEvent event) {
				onCallback(new ReceivedTimedEvent(event));
			}
		}; 
		
		// initialize in main thread
		handler.post(new Runnable() {
			@Override
			public void run() {
				bus = new TinyBus(getInstrumentation().getContext());
				bus.register(callbacks1);
				bus.register(callbacks2);
				initLatch.countDown();
			}
		});
		
		// wait for initialization
		initLatch.await(3, TimeUnit.SECONDS);
		
		// post from a background thread
		bus.postDelayed(new TimedEvent(1), delayTime);
		bus.cancelDelayed(TimedEvent.class);
		
		// wait for possible events
		SystemClock.sleep(delayTime + 100);
		
		callbacks1.assertNoEvents();
		callbacks2.assertNoEvents();
	}
	
	@UiThreadTest
	public void testCancelDelayedNotPosted() {
		bus = new TinyBus(getInstrumentation().getContext());
		bus.cancelDelayed(TimedEvent.class);
		// no exceptions, ok
	}

    public void testCancelDelayedOnDestroy() throws InterruptedException {

        final CountDownLatch initLatch = new CountDownLatch(1);
        final Callbacks callback = new Callbacks() {
            @Subscribe
            public void onEvent(String event) {
                onCallback(event);
            }
        };

        // initialize in main thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                bus = new TinyBus(getInstrumentation().getContext());
                bus.register(callback);
                initLatch.countDown();
            }
        });

        // wait for initialization
        initLatch.await(3, TimeUnit.SECONDS);

        bus.postDelayed("event to cancel", 100);
        bus.getLifecycleCallbacks().onDestroy();

        SystemClock.sleep(200);
        callback.assertNoEvents();
    }
}
