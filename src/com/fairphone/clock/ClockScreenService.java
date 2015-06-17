package com.fairphone.clock;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.fairphone.clock.widget.ClockWidget;

public class ClockScreenService extends Service {

	private static final String TAG = ClockScreenService.class.getSimpleName();

	public static final String ACTION_ROTATE_VIEW = "com.fairphone.clock.ACTION_ROTATE_VIEW";

	private BroadcastReceiver mRotateReceiver;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.wtf(TAG, "onStartCommand");

		if (mRotateReceiver == null) {
			mRotateReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					updateWidget();
				}
			};
			registerReceiver(mRotateReceiver, new IntentFilter(ACTION_ROTATE_VIEW));
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.wtf(TAG, "onBind");
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public void updateWidget()
	{
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, ClockWidget.class));
		if (appWidgetIds.length > 0)
		{
			new ClockWidget().onUpdate(this, appWidgetManager, appWidgetIds);
		}
	}

}
