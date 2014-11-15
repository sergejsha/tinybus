package com.halfbit.tinybus.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.halfbit.tinybus.Produce;
import com.halfbit.tinybus.TinyBus.Wireable;

/**
 * Generic wire posting <code>Intent</code> object to the bus according to 
 * given configuration.
 * 
 * @author sergej
 */
public class BroadcastEvents extends Wireable {

	private final IntentFilter mFilter;
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mIntent = intent;
			bus.post(intent);
		}
	};
	
	private Object mProducer;
	private Intent mIntent;
	
	public BroadcastEvents(String action) {
		mFilter = new IntentFilter(action); 
	}
	
	public BroadcastEvents(String... actions) {
		mFilter = new IntentFilter();
		for (String action : actions) {
			mFilter.addAction(action);
		}
	}
	
	public BroadcastEvents(String action, boolean sticky) {
		this(new IntentFilter(action), sticky);
	}
	
	public BroadcastEvents(IntentFilter filter) {
		mFilter = filter;
	}
	
	public BroadcastEvents(IntentFilter filter, boolean sticky) {
		mFilter = filter;
		if (sticky) {
			mProducer = new Object() {
				@Produce
				public Intent getLastIntent() {
					return mIntent;
				}
			};
		}
	}

	@Override
	protected void onStart(Context context) {
		mIntent = context.registerReceiver(mReceiver, mFilter);
		if (mProducer != null) {
			bus.register(mProducer);
		} else {
			if (mIntent != null) {
				bus.post(mIntent);
			}
		}
	}

	@Override
	protected void onStop(Context context) {
		if (mProducer != null) {
			bus.unregister(mProducer);
		}
		context.unregisterReceiver(mReceiver);
		mIntent = null;
	}

}
