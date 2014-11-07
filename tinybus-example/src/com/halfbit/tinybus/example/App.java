package com.halfbit.tinybus.example;

import com.halfbit.tinybus.TinyBus;

import android.app.Application;

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		
		// create a bus and attach it to the application. This is global bus now.
		TinyBus.create(this);
	}
	
}
