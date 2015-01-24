package com.halfbit.tinybus.wires;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import com.halfbit.tinybus.Produce;
import com.halfbit.tinybus.TinyBus.Wireable;

public class ScreenEventWire extends Wireable {

	// -- public events

	public static class ScreenEvent {
		public final boolean isScreenOn;

		public ScreenEvent(boolean isScreenOn) {
			this.isScreenOn = isScreenOn;
		}
		
		public boolean isScreenOn() {
			return isScreenOn;
		}
	}

	//-- context
	
	private ScreenEvent mLastScreenEvent;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_SCREEN_ON.equals(action)) {
				mLastScreenEvent = new ScreenEvent(true);
				bus.post(mLastScreenEvent);
				
			} else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
				mLastScreenEvent = new ScreenEvent(false);
				bus.post(mLastScreenEvent);
			}
		}
	};

	@Override
	@SuppressWarnings("deprecation")
	protected void onStart() {
		super.onStart();
		context.registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
		context.registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		
		// send first event immediately
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mLastScreenEvent = new ScreenEvent(pm.isScreenOn());
		bus.post(mLastScreenEvent);
		
		bus.register(this);
	}

	@Override
	protected void onStop() {
		bus.unregister(this);
		context.unregisterReceiver(mReceiver);
		mLastScreenEvent = null;
		super.onStop();
	};

	@Produce
	public ScreenEvent getLastScreenEvent() {
		return mLastScreenEvent;
	}
}
