/*
 * Copyright (C) 2014, 2015 Sergej Shafarenka, halfbit.de
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.halfbit.tinybus;

/**
 * Dispatches events to listeners, and provides ways for listeners to register themselves.
 *
 * <p>The Bus allows publish-subscribe-style communication between components without requiring
 * the components to explicitly register with one another (and thus be aware of each other).
 * It is designed exclusively to replace traditional Android in-process event distribution using
 * explicit registration or listeners. It is <em>not</em> a general-purpose publish-subscribe
 * system, nor is it intended for interprocess communication.
 *
 * <h2>Receiving Events</h2>
 * To receive events, an object should:
 * <ol>
 *      <li>Expose a <b>public</b> method, known as the <i>event handler</i>, which accepts
 *      a single argument of the type of event desired;</li>
 *      <li>Mark it with a {@link de.halfbit.tinybus.Subscribe} annotation;</li>
 *      <li>Pass itself to an Bus instance's {@link #register(Object)} method.</li>
 * </ol>
 *
 * <h2>Posting Events</h2>
 * To post an event, simply provide the event object to the {@link #post(Object)} method.
 * The Bus instance will determine the type of event and route it to all registered listeners.
 *
 * <p>Events are routed to all subscribers registered for exactly this type of event. Event's
 * class hierarchy is ignored.
 *
 * <p>When {@code post} is called, all registered subscribers for an event are run in sequence
 * synchronously, so subscribers should be reasonably quick. If an event may trigger an extended
 * process (such as a database load), spawn a thread or queue it for later.
 *
 * <p>You are allowed to post a new event while already handling an event inside a subscriber
 * method. In this case the bus will finalize dispatching of current event first and then a
 * new event will be dispatched.
 *
 * <h2>Handler Methods</h2>
 * Event handler methods must accept only one argument: the event.
 *
 * <p>Handlers should not, in general, throw. If they do, the Bus will wrap the exception
 * and re-throw it.
 *
 * <h2>Producer Methods</h2>
 * Producer methods should accept no arguments and return their event type. When a subscriber
 * is registered for a type that a producer is also already registered for, the subscriber
 * will be called with the return value from the producer. Null values coming from producers
 * are <b>not</b> dispatched to subscribers.
 *
 * <p>This class is <b>not</b> safe for concurrent use. It must be called from a single thread.
 *
 * @author Cliff Biffle
 * @author Jake Wharton
 * @author Sergej Shafarenka
 */
public interface Bus {

    /**
     * Registers all handler methods on {@code object} to receive events and producer methods to provide events.
     * <p>
     * If any subscribers are registering for types which already have a producer they will be called immediately
     * with the result of calling that producer.
     * <p>
     * If any producers are registering for types which already have subscribers, each subscriber will be called with
     * the value from the result of calling the producer.
     *
     * @param object object whose handler methods should be registered.
     * @throws NullPointerException if the object is null.
     */
    void register(Object object);

	/**
	 * Unregisters all producer and handler methods on a registered {@code object}.
	 *
	 * @param object object whose producer and handler methods should be unregistered.
	 * @throws IllegalArgumentException if the object was not previously registered.
	 * @throws NullPointerException if the object is null.
	 */
	void unregister(Object object);

	/**
	 * Posts an event to all registered handlers. This method will return successfully after the event has been posted to
	 * all handlers if there was no exceptions thrown by handlers. Exceptions in handlers are wrapper with a
	 * RuntimeException and re-thrown.
	 *
	 * @param event     event to post.
	 * @throws NullPointerException if the event is null.
	 */
	void post(Object event);

	/**
	 * Checks whether given object is currently registered in the bus.
	 * <p>In most cases, when you (un)register objects inside standard
	 * <code>onStart()</code> and <code>onStop()</code> lifecycle callbacks,
	 * you don't need this method at all. But in some more trickier
	 * cases, when you (un)register objects depending on some other
	 * conditions, this method can be very helpful.
	 *
	 * @param object	the object to check
	 * @return			<code>true</code> if object is registered or
	 * 					<code>false</code> otherwise
	 */
	boolean hasRegistered(Object object);

    /**
     * Causes the event to be posted to the bus after the specified amount of time elapses.
     * Only one event of same type can be delayed. If another event of same type is already
     * pending for delivery, then the bus will replace old event with the new one and reschedule
     * it with the new delay value.
     * <p>
     *     <b>Note</b> that the time-base is <code>SystemClock.uptimeMillis()</code>. This means
     *     the time spent in deep sleep will add an additional delay to execution.
     *
     * @param event         event to be posted
     * @param delayMillis   delay (in milliseconds) until the event will be executed
     */
	void postDelayed(Object event, long delayMillis);

    /**
     * Removes a pending event of given type from delivery queue.
     *
     * @param eventClass    event type of event to be cancelled
     */
	void cancelDelayed(Class<?> eventClass);
}
