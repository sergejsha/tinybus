package de.halfbit.tinybus.impl.v14;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.os.Build;

import de.halfbit.tinybus.impl.v10.TinyBusDepotCompat;

/** Native way of callbacks. */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class TinyBusDepotNative extends TinyBusDepotCompat
    implements Application.ActivityLifecycleCallbacks {

  public TinyBusDepotNative(Context context) {
    super(context);

    // attach us to native implementation
    final Application app = (Application) context.getApplicationContext();
    app.registerActivityLifecycleCallbacks(this);
  }

}
