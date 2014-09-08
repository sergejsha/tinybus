

![tinybus][1]


=======

A lightweight and simple event bus for Android. 

 - small footprint
 - simple to use
 - optimized for startup and event dispatching


Usage example
=======

```java
public MainActivity extends Activity implements BusDepot {

    // 1. First we need to implement BusDepot interface

    private Bus mBus = new TinyBus();

    @Override
    public Bus getBus() {
        return mBus;
    } 
    
    
    // 2. Every event receiver has to be registered to be able to receive 
    // events and unregister when it is not interested in events anymore. 
    // The best place is to do it inside onStart/onStop methods.
    
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
    // methow with @Subscribe annotation.
    
    @Subscribe
    public void onLoadingEvent(LoadingEvent event) {
        if (event.state == LoadingEvent.STATE_STARTED) {
            // do something, for instance update ActionBar state
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
public LoadingFragment extends Fragment {

    // 5. Now let's have a fragment, which initiates loading and posts
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
    // preatty much same technics, but we use another TinyBus.from() 
    // factory method to access the bus instance.
    
    @Oerride
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
public AnotherFragment extends Fragment {

    // 9. If we have nother pragment, which needs to receive loading
    // events, we can register it in the very same way.

    @Oerride
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


Event dispatching
=======

Event is dispatched in calling thread to all registered subscribers sequentially.

 * If another event gets posted while handling current event in a subscriber, then the bus completes dispatching of current event first, and then dispatches the new event.
 * TinyBus does not dispatch ```null``` events coming from a producer method. Such values are silently ignored.


Relation to Otto event bus 
=======

Fist of all TinyBus respects the work and keeps copyright notes of all parts adopted from [Otto][2] project. At the same time TinyBus is not a fork of Otto project. TinyBus uses very similar interfaces but it has different implementation with a slightly different behavior written from scratch. The main difference to Otto is that TinyBus is optimized for startup and event dispatching performance.

 * It doesn't create additional objects while dispatching an event. This allows you to use it in projects where many events get dispatched frequently.
 * TinyBus is designed to be called from a single thread only. In most cases this is Main Thread. It doesn't use synchronized classes, which makes it fast.
 * TinyBus does not analyse event's class hierarhy. It dispatches events to subscribers listening for exaclty these events, which makes it fast.


Functional correctness 
=======
Functional correctness - a prove that event bus does exaclty what it has to do - is very importaint. That's why TinyBus has over 40 test-cases checking its functionality.


[1]: web/logo.png
[2]: https://github.com/square/otto
