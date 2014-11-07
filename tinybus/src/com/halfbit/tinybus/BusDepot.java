/*
 * Copyright (C) 2014 Sergej Shafarenka, halfbit.de
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
package com.halfbit.tinybus;

/**
 * Implement this interface to provide an instance of {@link com.halfbit.tinybus.Bus}.
 * 
 * <p>If you want a global bus instance, then let {@link android.app.Application} to 
 * implement this interface. In case you want to have a separate bus per 
 * {@link android.app.Activity}, then implement this interface in your activity.
 *   
 * <p>Note: This interface is deprecated and will be removed in version 2. Instead of 
 * implementing it just use TinyBus.create(Application) or TinyBus.create(Activity) 
 * methods. For getting created Bus instance use TinyBus.from(Context) method as before.
 * 
 * @author Sergej Shafarenka
 */

@Deprecated
public interface BusDepot {
	
	/**
	 * @return instance of bus
	 */
	Bus getBus();
	
}
