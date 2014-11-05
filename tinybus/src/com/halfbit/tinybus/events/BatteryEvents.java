package com.halfbit.tinybus.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.halfbit.tinybus.Produce;
import com.halfbit.tinybus.TrolleyBus.Events;

public class BatteryEvents extends Events {

	//-- public events
	
	public static class BatteryLevelEvent {
		
		public final Intent intent;
		public final int level;
		
		public BatteryLevelEvent(Intent intent) {
			this.intent = intent;
			
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			if (scale == 0) {
				this.level = 0;
				
			} else {
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				level = level * 100 / scale;
				if (level <= 0) {
					level = 0;
				} else if (level >= 100) {
					level = 100;
				}
				this.level = level;
			}
		}

		public boolean isPluggedAc() {
			return getPlugged() == BatteryManager.BATTERY_PLUGGED_AC;
		}
		
		public boolean isPluggedUsb() {
			return getPlugged() == BatteryManager.BATTERY_PLUGGED_USB;
		}
		
		public boolean isPluggedWireless() {
			return getPlugged() == 4; // BatteryManager.BATTERY_PLUGGED_WIRELESS; (requires higher API level)
		}
		
		public int getPlugged() {
			return intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		}
		
		public boolean isCharging() {
			final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
		}
	}
	
	public static class BatteryLowEvent {}
	public static class BatteryOkayEvent {}
	
	//-- implementation
	
	private final IntentFilter mFilter;
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
				mBatteryLevelEvent = new BatteryLevelEvent(intent);
				bus.post(mBatteryLevelEvent);
				
			} else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
				bus.post(new BatteryLowEvent());
				
			} else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
				bus.post(new BatteryOkayEvent());
			}
		}
	};
	
	private BatteryLevelEvent mBatteryLevelEvent;
	
	public BatteryEvents() {
		mFilter = new IntentFilter();
		mFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		mFilter.addAction(Intent.ACTION_BATTERY_LOW);
		mFilter.addAction(Intent.ACTION_BATTERY_OKAY);
	}
	
	@Override
	protected void onStart(Context context) {
		bus.register(this);
		context.registerReceiver(mReceiver, mFilter);
	}

	@Override
	protected void onStop(Context context) {
		bus.unregister(this);
		context.unregisterReceiver(mReceiver);
		mBatteryLevelEvent = null;
	}

	@Produce
	public BatteryLevelEvent getBatteryLevelEvent() {
		return mBatteryLevelEvent;
	}
	
}
