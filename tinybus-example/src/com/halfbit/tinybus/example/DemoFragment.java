package com.halfbit.tinybus.example;

import com.halfbit.tinybus.Bus;
import com.halfbit.tinybus.Produce;
import com.halfbit.tinybus.Subscribe;
import com.halfbit.tinybus.Subscribe.Mode;
import com.halfbit.tinybus.TinyBus;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;

public class DemoFragment extends Fragment {

	public static class ProcessData {
		public final int totalSteps;
		
		public ProcessData(int totalSteps) {
			this.totalSteps = totalSteps;
		}
	}
	
	public static class ProgressStatusEvent {
		public final int step;
		public final int totalSteps;
		
		public ProgressStatusEvent(int step, int totalSteps) {
			this.step = step;
			this.totalSteps = totalSteps;
		}
	}
	
	private Bus mGlobalBus;
	private ProgressStatusEvent mProgressStatusEvent;
	private ProgressBar mProgressBar;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress);
		
		rootView.findViewById(R.id.button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mGlobalBus.post(new ProcessData(20));
			}
		});
		
		// get global application bus
		mGlobalBus = TinyBus.from(getActivity().getApplicationContext());
		
		return rootView;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mGlobalBus.register(this);
	}
	
	@Override
	public void onStop() {
		mGlobalBus.unregister(this);
		super.onStop();
	}
	
	//-- bus events
	
	@Produce
	public ProgressStatusEvent getProgressStatusEvent() {
		return mProgressStatusEvent;
	}
	
	@Subscribe
	public void onProgressStatusEvent(ProgressStatusEvent event) {
		mProgressStatusEvent = event;
		mProgressBar.setMax(event.totalSteps);
		mProgressBar.setProgress(event.step);
	}
	
	@Subscribe(Mode.Background)
	public void onProcessData(ProcessData data) {
		for(int i=0; i<data.totalSteps; i++) {
			mGlobalBus.post(new ProgressStatusEvent(i + 1, data.totalSteps));
			try {
				synchronized (this) {
					wait(500);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
}