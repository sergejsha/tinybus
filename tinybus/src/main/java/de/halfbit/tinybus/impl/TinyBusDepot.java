/*
 * Copyright (C) 2014, 2015 Sergej Shafarenka, halfbit.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.halfbit.tinybus.impl;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import java.util.WeakHashMap;

import de.halfbit.tinybus.TinyBus;
import de.halfbit.tinybus.impl.v10.Compat;
import de.halfbit.tinybus.impl.v10.TinyBusDepotCompat;
import de.halfbit.tinybus.impl.v14.TinyBusDepotNative;
import de.halfbit.tinybus.impl.workers.Dispatcher;

@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public abstract class TinyBusDepot {
  /* [ CONSTANTS ] ================================================================================================= */

  protected static final boolean DEBUG = true;
  protected static final String TAG = TinyBusDepot.class.getSimpleName();
  protected static final String KEY_BUS_ID = "de.halfbit.tinybus.id";

	/* [ STATIC MEMBERS ] ============================================================================================ */

  private static TinyBusDepot INSTANCE;

	/* [ MEMBERS ] =================================================================================================== */

  //-- bus management

  protected final WeakHashMap<Context, TinyBus> mBuses = new WeakHashMap<Context, TinyBus>();
  protected final SparseArray<TinyBus> mTransientBuses = new SparseArray<TinyBus>(3);
  protected int mNextTransientBusId;

  private Dispatcher mDispatcher;

	/* [ CONSTRUCTORS ] ============================================================================================== */

  protected TinyBusDepot(Context context) {
  }

	/* [ STATIC METHODS ] ============================================================================================ */

  public static TinyBusDepot get(Context context) {
    if (INSTANCE == null) {
      INSTANCE = (Compat.IS_NATIVE) ?
          new TinyBusDepotNative(context) :
          new TinyBusDepotCompat(context);
    }

    return INSTANCE;
  }

	/* [ GETTER / SETTER METHODS ] =================================================================================== */

  public synchronized Dispatcher getDispatcher() {
    if (mDispatcher == null) {
      mDispatcher = new Dispatcher();
    }

    return mDispatcher;
  }

	/* [ IMPLEMENTATION & HELPERS ] ================================================================================== */

  public TinyBus createBusInContext(Context context) {
    final TinyBus bus = new TinyBus(context);
    mBuses.put(context, bus);
    return bus;
  }

  public TinyBus getBusInContext(Context context) {
    return mBuses.get(context);
  }

  protected void onContextDestroyed(Context context) {
    TinyBus bus = mBuses.remove(context);

    if (bus != null) {
      boolean keepBusInstance = context instanceof Activity
          && Compat.isChangingConfigurations((Activity) context);

      if (!keepBusInstance) {
        bus.getLifecycleCallbacks().onDestroy();
        if (DEBUG) {
          Log.d(TAG, " ### destroying bus: " + bus);
        }
      }
      if (DEBUG) {
        Log.d(TAG, " ### onDestroy() " + context +
            ", active buses: " + mBuses.size() +
            ", transient buses: " + mTransientBuses.size());
      }
    }
  }

  protected void onContextStopped(Context context) {
    TinyBus bus = mBuses.get(context);
    if (bus != null) {
      bus.getLifecycleCallbacks().onStop();
    }
    if (DEBUG) Log.d(TAG, " ### STOPPED, bus count: " + mBuses.size());
  }

	/* [ NESTED DECLARATIONS ] ======================================================================================= */

  public interface LifecycleCallbacks {
    void attachContext(Context context);

    void onStart();

    void onStop();

    void onDestroy();
  }
}
