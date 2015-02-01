![tinybus][1]

[![Flattr this git repo](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=beworker&url=https://github.com/beworker/tinybus&title=tinybus&language=java&tags=github&category=software)

Faster implementation of [Otto][2] event bus with additional features you missed.

Version 3.0 (in test)
=======
  - [x] Background processing queues
  - [x] Post delayed events
  - [x] Full Gradle support (Ant build is obsolate)
 
TinyBus is
=======
 - tiny (~ 26K jar)
 - fast (optimized for startup and event dispatching)
 - well tested (> 90 junit tests)
 - annotation based (no requiremens on method names, no interfaces to implement)

TinyBus API in a nutshell
=======
 - `@Subscribe` annotates event handler methods running in the main thread.
 - `@Subscribe(mode=Mode.Background)` annotates event handler methods running in a background thread.
 - `@Subscribe(mode=Mode.Background, queue="web")` annotates event handler methods running in a serialized background queue.
 - `@Produce` annotates methods delivering most recent events (aka sticky events).
 - `Bus.register(Object)` and `Bus.unregister(Object)` register and unregister objects with subscriber and producer methods.
 - `Bus.hasRegistered(Object)` checks, whether given object is registered.
 - `Bus.post(Object)` posts given event object.
 - `Bus.postDelayed(Object, long)` and `Bus.cancelDelayed(Class)` schedules event delivery for later in time and cancels it.

For a more detailed example check out [Getting started][4] step-by-step guide or example application.

Performance reference tests
=======
![tinybus][3]

Executed on Nexus 5 device (Android 5.0.1, ART, screen off).

TinyBus extensions (still in 'β')
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
More detailed usage example can be found in example application.

Build
=======

1. git clone git@github.com:beworker/tinybus.git
2. cd <git>/tinybus
3. gradle build (or ant release)

Execute JUnit tests
=======

1. cd <git>/tinybus-tests
2. ant test

Gradle dependencies
=======

For pure event bus implementation
```
dependencies {
    compile 'de.halfbit:tinybus:2.1.+'
}
```
For event bus with extensions
```
dependencies {
    compile 'de.halfbit:tinybus:2.1.+'
    compile 'de.halfbit:tinybus-extensions:2.1.+'
}
```

Proguard configuration (Version 2.1.x and below)
=======

```
-keepclassmembers class ** {
    @com.halfbit.tinybus.Subscribe public *;
    @com.halfbit.tinybus.Produce public *;
}

-keepclassmembers enum com.halfbit.tinybus.Subscribe$Mode {
	public *;
}
```

Proguard configuration (Since version 3)
=======

```
-keepclassmembers class ** {
    @de.halfbit.tinybus.Subscribe public *;
    @de.halfbit.tinybus.Produce public *;
}
```

Used in
=======

 - [franco.Kernel updater][6]
 - [Settings Extended][5]

License
=======

    Copyright (c) 2014-2015 Sergej Shafarenka, halfbit.de
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
[5]: https://play.google.com/store/apps/details?id=com.hb.settings
[6]: https://play.google.com/store/apps/details?id=com.franco.kernel
