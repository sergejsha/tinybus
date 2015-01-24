package com.halfbit.tinybus.wires;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.halfbit.tinybus.Produce;
import com.halfbit.tinybus.TinyBus.Wireable;

public class ConnectivityWire extends Wireable {

	//-- public events

	/** Event to be sent when connection on/off event happends. */
	public static class ConnectionStateEvent {
		protected final boolean mConnected;
		
		public ConnectionStateEvent(NetworkInfo networkInfo) {
			mConnected = networkInfo != null && networkInfo.isConnected();
		}
		
		public boolean isConnected() {
			return mConnected; 
		}
	}
	
	/** Event to be sent when any change happens to the network. */
	public static class ConnectionEvent extends ConnectionStateEvent {
		
		public static final int CONNECTION_TYPE_UNKNOWN = 0;
		public static final int CONNECTION_TYPE_FAST = 1;
		public static final int CONNECTION_TYPE_2G = 2;
		public static final int CONNECTION_TYPE_3G = 3;
		public static final int CONNECTION_TYPE_4G = 4;
				
		public final NetworkInfo mNetworkInfo;
		private int mConnectionType = -1;
		
		public ConnectionEvent(NetworkInfo networkInfo) {
			super(networkInfo);
			mNetworkInfo = networkInfo;
		}
		
		public int getConnectionType() {
			if (mConnectionType == -1) {
				calculateConnectionType();
			}
			return mConnectionType;  
		}

		private void calculateConnectionType() {
			if (mConnected) {
				
				switch (mNetworkInfo.getType()) {
					case ConnectivityManager.TYPE_WIFI:
					case ConnectivityManager.TYPE_WIMAX:
					case ConnectivityManager.TYPE_ETHERNET:
						mConnectionType = CONNECTION_TYPE_FAST;
						break;
						
					case ConnectivityManager.TYPE_MOBILE:
						switch (mNetworkInfo.getSubtype()) {
							case TelephonyManager.NETWORK_TYPE_LTE:
							case TelephonyManager.NETWORK_TYPE_HSPAP:
							case TelephonyManager.NETWORK_TYPE_HSPA:
							case TelephonyManager.NETWORK_TYPE_EHRPD:
								mConnectionType = CONNECTION_TYPE_4G;
								break;
								
							case TelephonyManager.NETWORK_TYPE_UMTS:
							case TelephonyManager.NETWORK_TYPE_CDMA:
							case TelephonyManager.NETWORK_TYPE_HSDPA:
							case TelephonyManager.NETWORK_TYPE_HSUPA:
							case TelephonyManager.NETWORK_TYPE_EVDO_0:
							case TelephonyManager.NETWORK_TYPE_EVDO_A:
							case TelephonyManager.NETWORK_TYPE_EVDO_B:
								mConnectionType = CONNECTION_TYPE_3G;
								break;
								
							case TelephonyManager.NETWORK_TYPE_GPRS:
							case TelephonyManager.NETWORK_TYPE_EDGE:
								mConnectionType = CONNECTION_TYPE_2G;
								break;
								
							default:
								mConnectionType = CONNECTION_TYPE_UNKNOWN;
						}
						break;
						
					default:
						mConnectionType = CONNECTION_TYPE_UNKNOWN;
				}				
				
			} else {
				mConnectionType = CONNECTION_TYPE_UNKNOWN;
			}
		}
		
	}
	
	//-- implementation
	
	private final IntentFilter mFilter;
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			postEvent();
		}
		
	};

	private final Class<? extends ConnectionStateEvent> mpProducedEventClass;
	private ConnectivityManager mConnectivityManager;
	
	// last known events
	private ConnectionStateEvent mConnectionStateEvent;
	private ConnectionEvent mConnectionEvent;
	
	public ConnectivityWire(Class<? extends ConnectionStateEvent> producedEventClass) {
		mpProducedEventClass = producedEventClass;
		mFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
	}
	
	@Override
	protected void onStart() {
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			// Older Android versions don't notify receiver immediately 
			// after registration. Thus we query status manually for those.
			postEvent();
		}
		
		bus.register(this);
		context.registerReceiver(mReceiver, mFilter);
	}

	@Override
	protected void onStop() {
		bus.unregister(this);
		context.unregisterReceiver(mReceiver);
		mConnectionStateEvent = null;
		mConnectionEvent = null;
		mConnectivityManager = null;
	}

	@Produce
	public ConnectionStateEvent getConnectionStateEvent() {
		return mConnectionStateEvent;
	}
	
	@Produce
	public ConnectionEvent getConnectionEvent() {
		return mConnectionEvent;
	}
	
	void postEvent() {
		if (mConnectivityManager == null) {
			mConnectivityManager = (ConnectivityManager) 
					context.getSystemService(Context.CONNECTIVITY_SERVICE);
		}
		NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
		if (mpProducedEventClass == ConnectionStateEvent.class) {
			mConnectionStateEvent = new ConnectionStateEvent(networkInfo);
			bus.post(mConnectionStateEvent);
			
		} else {
			mConnectionEvent = new ConnectionEvent(networkInfo);
			bus.post(mConnectionEvent);
		}
	}
}
