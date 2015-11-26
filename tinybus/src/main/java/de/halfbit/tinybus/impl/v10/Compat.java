package de.halfbit.tinybus.impl.v10;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

import java.util.ArrayList;

/***/
@SuppressWarnings({"unused"})
public final class Compat {
  private static final ArrayList<LifecycleCallbacks> sCallbacks = new ArrayList<>();

  private static final Object[] EMPTY = new Object[0];

  /** Is API 14 level reached and native implementation is available. */
  public static final boolean IS_NATIVE = (Build.VERSION_CODES.ICE_CREAM_SANDWICH <= Build.VERSION.SDK_INT);

  public static void registerActivityLifecycleCallbacks(LifecycleCallbacks callback) {
    synchronized (sCallbacks) {
      sCallbacks.add(callback);
    }
  }

  public static void unregisterActivityLifecycleCallbacks(LifecycleCallbacks callback) {
    synchronized (sCallbacks) {
      sCallbacks.remove(callback);
    }
  }

  private static Object[] collectActivityLifecycleCallbacks() {
    Object[] callbacks = EMPTY;

    synchronized (sCallbacks) {
      if (sCallbacks.size() > 0) {
        callbacks = sCallbacks.toArray();
      }
    }

    return callbacks;
  }

  public static void dispatchActivityCreated(Activity activity, Bundle savedInstanceState) {
    final Object[] callbacks = collectActivityLifecycleCallbacks();

    for (final Object callback : callbacks) {
      ((LifecycleCallbacks) callback).onActivityCreated(activity, savedInstanceState);
    }
  }

  public static void dispatchActivityStarted(Activity activity) {
    final Object[] callbacks = collectActivityLifecycleCallbacks();

    for (final Object callback : callbacks) {
      ((LifecycleCallbacks) callback).onActivityStarted(activity);
    }
  }

  public static void dispatchActivityResumed(Activity activity) {
    final Object[] callbacks = collectActivityLifecycleCallbacks();

    for (final Object callback : callbacks) {
      ((LifecycleCallbacks) callback).onActivityResumed(activity);
    }
  }

  public static void dispatchActivityPaused(Activity activity) {
    final Object[] callbacks = collectActivityLifecycleCallbacks();

    for (final Object callback : callbacks) {
      ((LifecycleCallbacks) callback).onActivityPaused(activity);
    }
  }

  public static void dispatchActivityStopped(Activity activity) {
    final Object[] callbacks = collectActivityLifecycleCallbacks();

    for (final Object callback : callbacks) {
      ((LifecycleCallbacks) callback).onActivityStopped(activity);
    }
  }

  public static void dispatchActivitySaveInstanceState(Activity activity, Bundle outState) {
    final Object[] callbacks = collectActivityLifecycleCallbacks();

    for (final Object callback : callbacks) {
      ((LifecycleCallbacks) callback).onActivitySaveInstanceState(activity, outState);
    }
  }

  public static void dispatchActivityDestroyed(Activity activity) {
    final Object[] callbacks = collectActivityLifecycleCallbacks();

    for (final Object callback : callbacks) {
      ((LifecycleCallbacks) callback).onActivityDestroyed(activity);
    }
  }

  public static boolean isChangingConfigurations(Activity activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      return activity.isChangingConfigurations();
    }

    return false; // TODO: implement me!
  }

  /** Compatibility interface that mimics {@link Application.ActivityLifecycleCallbacks} of API 14 class. */
  public interface LifecycleCallbacks {
    void onActivityCreated(Activity activity, Bundle savedInstanceState);

    void onActivityStarted(Activity activity);

    void onActivityResumed(Activity activity);

    void onActivityPaused(Activity activity);

    void onActivityStopped(Activity activity);

    void onActivitySaveInstanceState(Activity activity, Bundle outState);

    void onActivityDestroyed(Activity activity);
  }
}
