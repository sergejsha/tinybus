package de.halfbit.tinybus.impl.v10;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import de.halfbit.tinybus.TinyBus;
import de.halfbit.tinybus.impl.TinyBusDepot;

/** Implementation compatible with API 10. */
@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class TinyBusDepotCompat extends TinyBusDepot implements Compat.LifecycleCallbacks {

  public TinyBusDepotCompat(final Context context) {
    super(context);

    // register compatible interface
    if (!Compat.IS_NATIVE) {
      Compat.registerActivityLifecycleCallbacks(this);
    }
  }

  @Override
  public void onActivityStarted(Activity activity) {
    final TinyBus bus = mBuses.get(activity);

    if (bus != null) {
      bus.getLifecycleCallbacks().onStart();

      if (DEBUG) {
        int transientBusIndex = mTransientBuses.indexOfValue(bus);
        if (transientBusIndex > -1) {
          throw new IllegalStateException(
              "Unexpected transient bus left in the bucket after onCreate()");
        }
      }
    }

    if (DEBUG) Log.d(TAG, " ### STARTED, bus count: " + mBuses.size());
  }

  @Override
  public void onActivityStopped(Activity activity) {
    onContextStopped(activity);
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    onContextDestroyed(activity);
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    if (savedInstanceState != null) {

      // try to restore bus from transient state
      final int busId = savedInstanceState.getInt(KEY_BUS_ID, -1);
      if (busId > -1) {

        final TinyBus bus = mTransientBuses.get(busId);
        if (bus != null) {

          mTransientBuses.delete(busId);
          bus.getLifecycleCallbacks().attachContext(activity);
          mBuses.put(activity, bus);

          if (DEBUG) {
            Log.d(TAG, " ### onCreated(), bus restored for " + activity +
                ", busId: " + busId +
                ", bus: " + bus +
                ", active buses: " + mBuses.size() +
                ", transient buses: " + mTransientBuses.size());
          }
        }
      }
    }
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    if (Compat.isChangingConfigurations(activity)) {

      final TinyBus bus = mBuses.get(activity);

      if (bus != null) {
        // Store this bus into transient list to
        // restore it later, when activity is recreated
        final int busId = mNextTransientBusId++;

        mTransientBuses.put(busId, bus);
        outState.putInt(KEY_BUS_ID, busId);
        if (DEBUG) {
          Log.d(TAG, " ### storing transient bus, id: " + busId + ", bus: " + bus);
        }
      }
    }
  }

  @Override
  public void onActivityResumed(Activity activity) {
    // do nothing
  }

  @Override
  public void onActivityPaused(Activity activity) {
    // do nothing
  }
}
