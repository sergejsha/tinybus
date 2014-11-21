![tinybus][1]

TinyBus is
=======
 - tiny (~ 17K jar)
 - fast (optimized for startup and event dispatching)
 - well tested (> 50 junit tests)
 - annotation based (no requiremens to method names, no interfaces to implement)

TinyBus is for those who want
 - to remove unneccessary interfaces and direct component dependencies
 - to simplify communication between Activities, Fragments and Services
 - to simplify events exchange between background and Main Thread
 - to simplify consumption of standard system events (like Battery Level, Connection State etc.)

For those who want to *simplify* their apps and *increase development productivity*, here is TinyBus in detail.

Performance comparison tests
=======
![tinybus][3]

Executed on Galaxy Nexus device with Android 4.3 (Dalvik) with switched off screen.

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

TinyBus extensions
=======

Extensions is a unique feature of TinyBus. With it you can easily subscribe to commonly used events like battery level, connectivity change, phone shake event or even standard Android broadcast Intents. Here is a short example.

```java
public class MainActivity extends Activity {
    private Bus mBus;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // get bus instance and wire device shake event
        mBus = TinyBus.from(this).wire(new ShakeEventWire());
    }
    
    @Override
    protected void onStart() {
        super.onStart();
	    mBus.register(this);
	}
	
    @Override
    protected void onStop() {
        mBus.unregister(this);
        super.onStop();
    }
    
    @Subscribe
    public void onShakeEvent(ShakeEvent event) {
        // device has been shaken
    }
}
```
More detailed example can be found in example application.

Event dispatching
=======

By default, TinyBus dispatches events to all registered subscribers sequentially in Main Thread. If ```post()``` method is called in Main Thread, then subscribers are called directly. If ```post()``` method is called in a background thread, then TinyBus reroutes and dispatches events through Main Thread.

 * If another event gets posted while handling current event in a subscriber, then the bus completes dispatching of current event first, and then dispatches the new event.
 * TinyBus does *not* dispatch ```null``` events coming from a producer method. Such values are silently ignored.

If a subscriber is annotated with ```@Subscribe(Mode.Background)```, then TinyBus notifies it in a background thread. There is a signe background thread for all possible bus instances. So if that thread is blocked, then all new background events will be queued for further processing.

Migrating from Otto event bus
=======

TinyBus is backward compatible to [Otto][2] besides one thing. Because traverse of event class hierarchy makes event dispatching slower, TinyBus doesn't support event inheritance by design. This suppose to keep your code simple and unambiguous. If you don't use event inheritance, you can safely migrate to TinyBus right away. Just replace package names and create bus instance using ```TinyBus``` class. In addition you will get following benefits:

 * Method ```post()``` dispatches events in Main Thread even when called form a background thread.
 * Events can be dispatched into a background thread.
 * TinyBus extentions can be used.
 * Register/unregister as well as event dispatching will be much faster.

 
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

Execute JUnit tests
=======

1. cd <git>/tinybus-tests
2. ant test

Gradle dependencies
=======

For pure event bus implementation
```
dependencies {
    compile 'de.halfbit:tinybus:2.0.+'
}
```
For event bus with extensions
```
dependencies {
    compile 'de.halfbit:tinybus:2.0.+'
    compile 'de.halfbit:tinybus-extensions:2.0.+'
}
```

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
[4]: https://github.com/beworker/tinybus/wiki/Getting-Started
