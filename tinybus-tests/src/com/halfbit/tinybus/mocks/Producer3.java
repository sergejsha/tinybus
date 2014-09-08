package com.halfbit.tinybus.mocks;

import com.halfbit.tinybus.Produce;
import com.halfbit.tinybus.Subscribe;

public class Producer3 extends Callbacks {

	public Event2 lastEvent = new Event2(3);
	
	@Produce
	public Event2 getLastEvent() {
		return lastEvent;
	}
	
	@Subscribe
	public void onEvent1(Event1 event) {
		onCallback(event);
	}
	
}
