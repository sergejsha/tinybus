![tinybus][1]
=======

TinyBus is
 - fast (optimized for startup and event dispatching)
 - tiny (~ 17K jar)
 - well tested (> 50 junit tests)
 - annotation based (no requiremens to method names, no interfaces to implement)

Performance comparison tests
=======
![tinybus][3]

Executed on Galaxy Nexus device with Android 4.3 (Dalvik) with switched off screen.

TinyBus helps
=======
 - to remove unneccessary interfaces and direct component dependencies
 - to simplify communication between Activities, Fragments and Services
 - to simplify events exchange between background and Main Thread
 - to simplify consumption of standard system events (like Battery Level, Connection State etc.)

TinyBus quick start
=======

```java
   // 1. Create event
   public class LoadingEvent {
       // some fields if needed
   }
   
   // 2. Prepare event subscriber (Activity, Fragment or any other component)
   @Subscribe
   public void onEvent(LoadingEvent event) {
       // event handler logic
   }
   bus.register(this);
   
   // 3. post event
   bus.post(new LoadingEvent());
   
```

For a more detailed example check out [Getting started][4] step-by-step guide or example application.

Event dispatching
=======

By default, ```TinyBus``` dispatches events to all registered subscribers sequentially in Main Thread. If ```post()``` method is called in Main Thread, then subscribers are called directly. If ```post()``` method is called in a background thread, then ```TinyBus``` reroutes and dispatches events through Main Thread.

 * If another event gets posted while handling current event in a subscriber, then the bus completes dispatching of current event first, and then dispatches the new event.
 * ```TinyBus``` does *not* dispatch ```null``` events coming from a producer method. Such values are silently ignored.

If a subscriber is annotated with ```@Subscribe(Mode.Background)```, then ```TinyBus``` notifies it in a background thread. There is a signe background thread for all possible bus instances. So if that thread is blocked, then all new background events will be queued for further processing.

Differences to Otto event bus
=======

TinyBus adopts interfaces defined in [Otto project][2]. At the same time TinyBus is not a direct fork of Otto. It has different implementation written from scratch with a slightly different behavior. The main difference from Otto is that ```TinyBus``` is optimized for startup and event dispatching performance.

 * TinyBus ```post()``` method can be called from any thread.
 * TinyBus can dispatch events into a background thread.
 * ```TinyBus``` does not analyse event's class hierarhy. It dispatches events to subscribers listening for exaclty same event type.
 * TinyBus is much faster.

Build with Ant
=======

1. git clone git@github.com:beworker/tinybus.git
2. cd <git>/tinybus
3. ant release

Build with Gradle
=======

1. git clone git@github.com:beworker/tinybus.git
2. cd <git>/tinybus
3. gradle build

How to execute JUnit tests
=======

1. cd <git>/tinybus-tests
2. ant test

Proguard configuration
=======

```
-keepclassmembers class ** {
    @com.halfbit.tinybus.Subscribe public *;
    @com.halfbit.tinybus.Produce public *;
}
```

[![Flattr this git repo](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=beworker&url=https://github.com/beworker/tinybus&title=tinybus&language=java&tags=github&category=software)
License
=======

    Copyright (c) 2014 Sergej Shafarenka, halfbit.de
    Copyright (C) 2012 Square, Inc.
    Copyright (C) 2007 The Guava Authors
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[1]: web/tinybus.png
[2]: https://github.com/square/otto
[3]: web/performance.png
[4]: wiki/Getting-Started
