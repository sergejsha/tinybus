package com.halfbit.tinybus;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;

public class TrolleyBus extends TinyBus {

	//-- public classes and methods
	
	public static abstract class Events {
		protected Bus bus;
		protected abstract void onStarted(Context context);
		protected abstract void onStopped(Context context);
	}	
	
	public TrolleyBus wire(Events events) {
		if (mEvents == null) {
			mEvents = new ArrayList<Events>();
		}
		mEvents.add(events);
		events.bus = this;
		return this;
	}
	
	//-- implementation
	
	private ArrayList<Events> mEvents;

	void dispatchOnStart(Activity activity) {
		if (mEvents != null) {
			for (Events producer : mEvents) {
				producer.onStarted(activity);
			}
		}
	}
	
	void dispatchOnStop(Activity activity) {
		if (mEvents != null) {
			for (Events producer : mEvents) {
				producer.onStopped(activity);
			}
		}
	}
	
}
