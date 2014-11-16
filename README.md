![tinybus][1]
=======

TinyBus is
 - fast (optimized for startup and event dispatching)
 - small (~ 17K jar)
 - well tested (> 50 junit tests)
 - annotation based (no requiremens to method names, no interfaces to implement)

Performance comparison tests
=======
![tinybus][3]

Executed on Galaxy Nexus device with Android 4.3 (Dalvik) with switched off screen.

TinyBus was designed to
 - remove unneccessary interfaces and direct component dependencies
 - simplify communication between Activities, Fragments and Services
 - simplify events exchange between background and Main Thread
 - simplify consumption of standard system events (like Battery Level, Connection State etc.)

Getting started
=======

```java
public class MainActivity extends Activity {

    // 1. First we need to get instance of bus attached to current
    // context, which is out activity in this case. You don't need
    // to create anything. TinyBus will create instance for you if
    // there is no bus instance for the context yet.
    
    private Bus mBus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mBus = TinyBus.from(this);
        ...
    } 
    
    
    // 2. Every event receiver has to be registered to be able to receive 
    // events and unregister when it is not interested in events anymore. 
    // The best place to do it is inside onStart()/onStop() methods or your
    // activity or fragment.
    
    @Override
    protected void onStart(Bundle savedInstance) {
      super.onStart(savedInstance);
      mBus.register(this);
    }

    @Override
    protected void onStop() {
      mBus.unregister(this);
      super.onStop();
    }
  
    // 3. Now activity is able to receive events. Let's implement a callback
    // method for an event of type LoadingEvent. Callback must be a public
    // method with @Subscribe annotation.
    
    @Subscribe
    public void onLoadingEvent(LoadingEvent event) {
        if (event.state == LoadingEvent.STATE_STARTED) {
            // put your logic here, e.g. update ActionBar state
        }
    }
}
```

```java
public class LoadingEvent {

    // 4. Here is our event class itself. An event can be any class.

    public static final int STATE_STARTED = 0;
    public static final int STATE_FINISHED = 1;
    
    public int state;
    
    public LoadingEvent(state) {
        this.state = state;
    }
}
```

```java
public class LoadingFragment extends Fragment {

    // 5. Now let's have a fragment which initiates loading and posts
    // loading event to the bus.

    private LoadingEvent mLoadingEvent;

    @Override
    public void onClick(View view) {
        // ... initiate loading first 
        
        mLoadingEvent = new LoadingEvent(LoadingEvent.STATE_STARTED);
        TinyBus.from(getActivity()).post(mLoadingEvent);
    }
    
    // 6. If we want to have a "sticky" event which gets posted to all
    // listeners even if they are registered after we posted the event, 
    // we have to implement a producer method as following.
    
    @Produce
    public LoadingEvent getLastLoadingEvent() {
        return mLoadingEvent;
    }
    
    // 7. To be able to produce events, producer has to be registered 
    // onto the bus. To register a fragment as a listener, we use 
    // pretty much same techniques, but we use another TinyBus.from() 
    // factory method to access the bus instance.
    
    @Override
    public void onStart() {
        super.onStart();
        TinyBus.from(getActivity()).register(this);
    }
    
    @Override
    protected void onStop() {
      TinyBus.from(getActivity()).unregister(this);
      super.onStop();
    }

    // 8. Now, once user initialtes a loading inside our fragment, 
    // activity gets notified too.
}
```

```java
public class AnotherFragment extends Fragment {

    // 9. If we have another fragment, which needs to receive loading
    // events, we can register it in the very same way.

    @Override
    public void onStart() {
        super.onStart();
        TinyBus.from(getActivity()).register(this);
    }
    
    @Override
    protected void onStop() {
      TinyBus.from(getActivity()).unregister(this);
      super.onStop();
    }

    @Subscribe
    public void onLoadingEvent(LoadingEvent event) {
        // do something with received event here
    }
}
```

Alternatively, if you need a single bus instance for the whole application, you have to request it from your application context. In the example below a fragment will post an event to the single event bus instance. Any receiver registrered at the same bus instance will receive this event.

```java
public class BackgroundFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        // 1. Get singleton bus instance
        TinyBus bus = TinyBus.from(getActivity().getApplicationContext());
        
        // 2. Post event
        bus.post(new LoadingEvent(LoadingEvent.STATE_STARTED));
    }
  
}
```

Event dispatching
=======

By default, ```TinyBus``` dispatches events to all registered subscribers sequentially in Main Thread. If ```post()``` method is called in Main Thread, then subscribers are called directly. If ```post()``` method is called in a background thread, then ```TinyBus``` reroutes and dispatches events through Main Thread.

 * If another event gets posted while handling current event in a subscriber, then the bus completes dispatching of current event first, and then dispatches the new event.
 * ```TinyBus``` does *not* dispatch ```null``` events coming from a producer method. Such values are silently ignored.

If a subscriber is annotated with ```@Subscribe(Mode.Background)```, then ```TinyBus``` notifies it in a background thread. There is a signe background thread for all possible bus instances. So if that thread is blocked, then all new background events will be queued for further processing.

Relation to Otto event bus 
=======

TinyBus adopts interfaces defined in [Otto project][2]. At the same time TinyBus is not a direct fork of Otto. Although it uses same interfaces, TinyBus has different implementation written from scratch with a slightly different behavior. The main difference form Otto is that ```TinyBus``` is optimized for startup and event dispatching performance.

 * It uses object pool and fast singly linked list for event queue. This allows you to use it in projects where many events get dispatched frequently.
 * It is designed to be called from a single thread only. In most cases this is Main Thread. It doesn't use synchronized classes, which makes it fast.
 * ```TinyBus``` does not analyse event's class hierarhy. It dispatches events to subscribers listening for exaclty these event types, which makes it fast.

Functional correctness 
=======
Functional correctness - a prove that event bus does exaclty what it has to do - is very important. That's why ```TinyBus``` has over 50 test-cases checking its functionality.

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
