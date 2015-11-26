package de.halfbit.tinybus.example;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import de.halfbit.tinybus.Subscribe;
import de.halfbit.tinybus.TinyBus;
import de.halfbit.tinybus.wires.BatteryWire;
import de.halfbit.tinybus.wires.BatteryWire.BatteryLevelEvent;
import de.halfbit.tinybus.wires.BroadcastReceiverWire;
import de.halfbit.tinybus.wires.ConnectivityWire;
import de.halfbit.tinybus.wires.ConnectivityWire.ConnectionStateEvent;
import de.halfbit.tinybus.wires.ShakeEventWire;
import de.halfbit.tinybus.wires.ShakeEventWire.ShakeEvent;

public class MainActivity extends LifecycleActivity {

  private TinyBus mBus;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mBus = TinyBus.from(this);
    if (savedInstanceState == null) {
      mBus.wire(new ShakeEventWire())
          .wire(new ConnectivityWire(ConnectionStateEvent.class))
          .wire(new BatteryWire())
          .wire(new BroadcastReceiverWire(
              Intent.ACTION_POWER_CONNECTED,
              Intent.ACTION_POWER_DISCONNECTED));
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    mBus.register(this);
  }

  @Override
  protected void onStop() {
    mBus.unregister(this);
    super.onStop();
  }

  @Subscribe
  public void onConnectivityEvent(ConnectionStateEvent event) {
    if (event.isConnected()) {
      // check type
    }
  }

  @Subscribe
  public void onBatteryEvent(BatteryLevelEvent event) {
    Toast.makeText(this, "Battery: " + event.level + "%", Toast.LENGTH_SHORT).show();
  }

  @Subscribe
  public void onIntent(Intent intent) {
    final String action = intent.getAction();
    if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
      Toast.makeText(this, "Power connected", Toast.LENGTH_SHORT).show();

    } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
      Toast.makeText(this, "Power disconnected", Toast.LENGTH_SHORT).show();

    }
  }

  @Subscribe
  public void onShakeEvent(ShakeEvent event) {
    Toast.makeText(this, "~ Device has been shaken ~", Toast.LENGTH_SHORT).show();
  }
}
