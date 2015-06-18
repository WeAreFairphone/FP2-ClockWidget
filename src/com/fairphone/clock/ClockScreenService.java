package com.fairphone.clock;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.fairphone.clock.widget.ClockWidget;

public class ClockScreenService extends Service {

	private static final String TAG = ClockScreenService.class.getSimpleName();

	public static final String ACTION_ROTATE_VIEW = "com.fairphone.clock.ACTION_ROTATE_VIEW";
	public static final String ACTION_SHARE = "com.fairphone.clock.ACTION_SHARE";
	public static final String EXTRA_CURRENT_LAYOUT_ID = "com.fairphone.clock.EXTRA_CURRENT_LAYOUT_ID";
	public static final String ACTION_BATTERY_SAVER = "com.fairphone.clock.ACTION_BATTERY_SAVER";

	private BroadcastReceiver mRotateReceiver;
	private BroadcastReceiver mShareReceiver;
	private BroadcastReceiver mBatterySaverReceiver;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.wtf(TAG, "onStartCommand");

		setupLayoutRotateReceiver();
		setupShareReceiver();
		setupBatterySaverReceiver();

		return START_STICKY;
	}

	private void setupLayoutRotateReceiver() {
		if (mRotateReceiver == null) {
			mRotateReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					updateWidget();
				}
			};
			registerReceiver(mRotateReceiver, new IntentFilter(ACTION_ROTATE_VIEW));
		}
	}

	private void setupBatterySaverReceiver() {
		if (mBatterySaverReceiver == null) {
			mBatterySaverReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Toast.makeText(context, "Jump to battery saver setting page", Toast.LENGTH_SHORT).show();
				}
			};
			registerReceiver(mBatterySaverReceiver, new IntentFilter(ACTION_BATTERY_SAVER));
		}
	}

	private void setupShareReceiver() {
		if (mShareReceiver == null) {
			mShareReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {

					Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.setType("text/plain");

					if(intent != null) {
						if (intent.hasExtra(EXTRA_CURRENT_LAYOUT_ID)) {
							int active_layout = intent.getIntExtra(EXTRA_CURRENT_LAYOUT_ID, -1);
							String shareText = null;
							switch (active_layout) {
								case R.id.clock_widget_peace_of_mind:
									shareText = "I've been in peace of mind for 172 minutes!";
//									shareText = "I've been in peace of mind for 325 minutes! A new personal record!";
									break;
								case R.id.clock_widget_yours_since:
									shareText = "My Fairphone is 2 years, 7 months, and 15 days old!";
									break;
								default:
									Log.wtf(TAG, "Unknown Share button: " + active_layout);
							}
							if (!TextUtils.isEmpty(shareText)) {
								sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
								sendIntent = Intent.createChooser(sendIntent, "Share to");
								sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(sendIntent);
							}
						}
					} else {
						Log.wtf(TAG, "Null intent on share receiver");
					}
				}
			};
			registerReceiver(mShareReceiver, new IntentFilter(ACTION_SHARE));
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
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
