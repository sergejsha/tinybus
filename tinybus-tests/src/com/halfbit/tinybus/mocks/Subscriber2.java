package com.halfbit.tinybus.mocks;

import com.halfbit.tinybus.Subscribe;

public class Subscriber2 extends Callbacks {

	@Subscribe
	public void onEvent(Event1 event) {
		onCallback(event);
	}
	
	@Subscribe
	public void onEvent(Event2 event) {
		onCallback(event);
	}
	
	@Subscribe
	public void onEvent(Event3 event) {
		onCallback(event);
	}
}
