![tinybus][1]

TinyBus is the faster implementation of [Otto][2] event bus with additional features you missed.

[![Flattr this git repo](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=beworker&url=https://github.com/beworker/tinybus&title=tinybus&language=java&tags=github&category=software)

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
 - `@Subscribe(mode=Mode.Background, queue="web")` annotates event handler methods running in a serialized background queue with given name. You can have as many queues as you want.
 - `@Produce` annotates methods returning most recent events (aka sticky events).
 - `Bus.register(Object)` and `Bus.unregister(Object)` register and unregister objects with annotated subscriber and producer methods.
 - `Bus.hasRegistered(Object)` checks, whether given object is already registered.
 - `Bus.post(Object)` posts given event object to all registered subscribers.
 - `Bus.postDelayed(Object, long)` and `Bus.cancelDelayed(Class)` schedules single event delivery for later in time and cancels it.

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
    private TinyBus mBus;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // get bus instance 
        mBus = TinyBus.from(this)
        
        if (savedInstanceState == null) {
            // Note: ShakeEventWire stays wired when activity is re-created
            //       on configuration change. That's why we register is 
            //       only once inside if-statement.

            // wire device shake event provider
            mBus.wire(new ShakeEventWire());
        }
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

Gradle dependencies
=======

For pure event bus implementation
```
dependencies {
    compile 'de.halfbit:tinybus:3.0.+'
}
```
For event bus with extensions
```
dependencies {
    compile 'de.halfbit:tinybus:3.0.+'
    compile 'de.halfbit:tinybus-extensions:3.0.+'
}
```

ProGuard configuration
=======

If you use Gradle build, then you don't need to configure anything, because it will use proper configuration already delivered with Android library archive. Otherwise you can use the configuration below:
```
-keepclassmembers, allowobfuscation class ** {
    @de.halfbit.tinybus.Subscribe public *;
    @de.halfbit.tinybus.Produce public *;
}
```

Used by
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
