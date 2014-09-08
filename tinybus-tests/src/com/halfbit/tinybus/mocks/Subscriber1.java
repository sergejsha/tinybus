package com.halfbit.tinybus.mocks;

import com.halfbit.tinybus.Subscribe;

public class Subscriber1 extends Callbacks {

	@Subscribe
	public void onEvent(Event1 event) {
		onCallback(event);
	}

}
