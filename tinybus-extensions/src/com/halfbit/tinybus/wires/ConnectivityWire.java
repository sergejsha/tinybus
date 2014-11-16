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
	
	public static final class ConnectionChangedEvent {
		
		public static final int CONNECTION_TYPE_UNKNOWN = 0;
		public static final int CONNECTION_TYPE_FAST = 1;
		public static final int CONNECTION_TYPE_2G = 2;
		public static final int CONNECTION_TYPE_3G = 3;
		public static final int CONNECTION_TYPE_4G = 4;
		
		public final NetworkInfo networkInfo;
		
		private final int mConnectionType;
		private final boolean mIsConnected;
		
		public ConnectionChangedEvent(NetworkInfo networkInfo) {
			this.networkInfo = networkInfo;
			
			mIsConnected = networkInfo != null 
					&& networkInfo.isConnectedOrConnecting();
			
			if (mIsConnected) {
				
				switch (networkInfo.getType()) {
					case ConnectivityManager.TYPE_WIFI:
					case ConnectivityManager.TYPE_WIMAX:
					case ConnectivityManager.TYPE_ETHERNET:
						mConnectionType = CONNECTION_TYPE_FAST;
						break;
						
					case ConnectivityManager.TYPE_MOBILE:
						switch (networkInfo.getSubtype()) {
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
		
		public boolean isConnected() {
			return mIsConnected;
		}
		
		public int getConnectionType() {
			return mConnectionType;  
		}

	}
	
	//-- implementation
	
	private final IntentFilter mFilter;
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mConnectionChangedEvent = new ConnectionChangedEvent(
					mConnectivityManager.getActiveNetworkInfo());
			bus.post(mConnectionChangedEvent);
		}
		
	};
	
	private ConnectivityManager mConnectivityManager;
	private ConnectionChangedEvent mConnectionChangedEvent;
	
	public ConnectivityWire() {
		mFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
	}
	
	@Override
	protected void onStart(Context context) {
		if (mConnectivityManager == null) {
			mConnectivityManager = (ConnectivityManager) 
					context.getSystemService(Context.CONNECTIVITY_SERVICE);
		}
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			
			// Older Android versions doesn't notify receiver immediately 
			// after registration. Thus we query the status manually for those.
			
			mConnectionChangedEvent = new ConnectionChangedEvent(
					mConnectivityManager.getActiveNetworkInfo());
		}
		
		bus.register(this);
		context.registerReceiver(mReceiver, mFilter);
	}

	@Override
	protected void onStop(Context context) {
		bus.unregister(this);
		context.unregisterReceiver(mReceiver);
		mConnectionChangedEvent = null;
	}

	@Produce
	public ConnectionChangedEvent getConnectionChangedEvent() {
		return mConnectionChangedEvent;
	}
}
