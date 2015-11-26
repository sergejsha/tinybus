package de.halfbit.tinybus.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import de.halfbit.tinybus.impl.v10.Compat;

/***/
public class LifecycleActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Compat.dispatchActivityCreated(this, savedInstanceState);
  }

  @Override
  protected void onStart() {
    super.onStart();
    Compat.dispatchActivityStarted(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Compat.dispatchActivityResumed(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Compat.dispatchActivityPaused(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    Compat.dispatchActivityStopped(this);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Compat.dispatchActivitySaveInstanceState(this, outState);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Compat.dispatchActivityDestroyed(this);
  }
}
