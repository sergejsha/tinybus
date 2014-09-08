package com.halfbit.tinybus.mocks;

import com.halfbit.tinybus.Produce;

public class Producer2 {

	public Event1 lastEvent1 = new Event1("producer2.event1");
	public Event2 lastEvent2 = new Event2(10);
	
	@Produce
	public Event1 getLastEvent1() {
		return lastEvent1;
	}
	
	@Produce
	public Event2 getLastEvent2() {
		return lastEvent2;
	}
	
}
