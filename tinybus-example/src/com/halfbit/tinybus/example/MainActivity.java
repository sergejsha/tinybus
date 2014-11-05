package com.halfbit.tinybus.example;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.halfbit.tinybus.Bus;
import com.halfbit.tinybus.Subscribe;
import com.halfbit.tinybus.TinyBus;
import com.halfbit.tinybus.events.BatteryEvents;
import com.halfbit.tinybus.events.BatteryEvents.BatteryLevelEvent;
import com.halfbit.tinybus.events.ConnectivityEvents;
import com.halfbit.tinybus.events.ConnectivityEvents.ConnectionChangedEvent;

public class MainActivity extends Activity {

	private Bus mBus;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mBus = TinyBus.create(this)
			.wire(new ConnectivityEvents())
			.wire(new BatteryEvents());
		
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
	public void onConnectivityEvent(ConnectionChangedEvent event) {
		if (event.isConnected()) {
			// check type
		}
	}

	@Subscribe
	public void onBatteryEvent(BatteryLevelEvent event) {
		Toast.makeText(this, "Battery: " + event.level + "%", Toast.LENGTH_SHORT).show();
	}
}
