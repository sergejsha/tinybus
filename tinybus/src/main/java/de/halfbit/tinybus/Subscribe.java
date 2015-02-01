/*
 * Copyright (C) 2014, 2015 Sergej Shafarenka, halfbit.de
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler, as used by {@link Bus}.
 *
 * <p>The method's first parameter defines the event type.
 * 
 * <p><b>Main mode</b>
 * <p>If subscriber's <code>mode</code> is undefined or 
 * <code>Mode.Main</code>, then this subscriber will be called 
 * in main bus thread. 
 * 
 * <p><b>Background mode</b>
 * <p>If subscriber's <code>mode</code> is <code>Mode.Background</code>
 * then subscriber will be called in a background thread. 
 * 
 * <p>For subscriber notified in background you can also specify a 
 * <code>queue</code> name. All subscribers with the same 
 * <code>queue</code> name are notified serially, meaning there is
 * only one active (called) subscriber at a time. Different queues 
 * processed in parallel, completely independent of each other. 
 * 
 * <p>This ensures, that database events processed in a one queue 
 * do not get blocked by possibly much slower events processed 
 * in another queue.    
 *
 * <p>You cannot expect, that same queue is always processed by same
 * background thread.
 * 
 * <p>If <code>queue</code> is not specified, then default "global"
 * queue is used.
 *
 * @author Cliff Biffle
 * @author Sergej Shafarenka
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
	
	public static final class Mode {
		public static final int Main = 0;
		public static final int Background = 1;
	}	
	
	int mode() default Mode.Main;
	String queue() default "global";
	
}