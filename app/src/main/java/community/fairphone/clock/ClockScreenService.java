package community.fairphone.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import community.fairphone.clock.widget.ClockWidget;

import java.util.Calendar;

public class ClockScreenService extends Service {

	private static final String TAG = ClockScreenService.class.getSimpleName();

	private static final String ACTION_ALARM_CHANGED = "android.app.action.NEXT_ALARM_CLOCK_CHANGED";
    private static final String ACTION_ALARM_CHANGED_V18 = "android.intent.action.ALARM_CHANGED";
    private static final String ACTION_CLOCK_UPDATE = "community.fairphone.clock.widget.ClockWidget.CLOCK_AM_PM_UPDATE";

    private BroadcastReceiver mTimeChangedReceiver;
    private BroadcastReceiver mAmPmCheckReceiver;
    private BroadcastReceiver mAlarmChangedReceiver;
    private BroadcastReceiver mLocaleChangedReceiver;

    private PendingIntent triggerUpdateIntent;


    public ClockScreenService(){
        Log.d(TAG, "ClockScreenService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);

        setupAMPMManager();
        setupAMPMReceiver();
        setupAlarmChangeReceiver();
        setupTimeChangedReceiver();
        setupLocaleChangeReceiver();

        return START_STICKY;
    }

    private PendingIntent getUpdateIntent() {
        if(triggerUpdateIntent == null) {
            Intent intent = new Intent(ACTION_CLOCK_UPDATE);
            triggerUpdateIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return triggerUpdateIntent;
    }

    private void setupAMPMManager() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE, 60 - calendar.get(Calendar.MINUTE));
        calendar.add(Calendar.SECOND, 60 - calendar.get(Calendar.SECOND));
        AlarmManager alarmManager = (AlarmManager) this
                .getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 3600000, getUpdateIntent());
    }

    private void clearAMPMManager() {
        if ( triggerUpdateIntent != null ) {
            AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(getUpdateIntent());
        }
    }

    @Override
	public void onDestroy() {
        Log.d(TAG, "onDestroy");
		super.onDestroy();

        clearAMPMManager();
        clearAMPMReceiver();
        clearAlarmChangeReceiver();
        clearTimeChangedReceiver();
        clearLocaleChangeReceiver();
	}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, ClockWidget.class));
        if (appWidgetIds.length > 0) {
            new ClockWidget().onUpdate(this, appWidgetManager, appWidgetIds);
        }
    }

	private void setupAMPMReceiver() {
		if (mAmPmCheckReceiver == null) {
			mAmPmCheckReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					updateWidget();
				}
			};
			registerReceiver(mAmPmCheckReceiver, new IntentFilter(ACTION_CLOCK_UPDATE));
		}
	}

    private void clearAMPMReceiver() {
        if ( mAmPmCheckReceiver != null ) {
            unregisterReceiver(mAmPmCheckReceiver);
            mAmPmCheckReceiver = null;
        }
    }

    private void setupTimeChangedReceiver() {
        if (mTimeChangedReceiver == null) {
            mTimeChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateWidget();
                }
            };

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
            intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            registerReceiver(mTimeChangedReceiver, intentFilter);
        }
    }

    private void clearTimeChangedReceiver() {
        if ( mTimeChangedReceiver != null ) {
            unregisterReceiver(mTimeChangedReceiver);
            mTimeChangedReceiver = null;
        }
    }

    private void setupAlarmChangeReceiver() {
        if (mAlarmChangedReceiver == null) {
            mAlarmChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG,"update widget");
                    updateWidget();
                }
            };
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                registerReceiver(mAlarmChangedReceiver, new IntentFilter(ACTION_ALARM_CHANGED_V18));
            }else
            {
                registerReceiver(mAlarmChangedReceiver, new IntentFilter(ACTION_ALARM_CHANGED));
            }
        }
    }

    private void clearAlarmChangeReceiver() {
        if ( mAlarmChangedReceiver != null ) {
            unregisterReceiver(mAlarmChangedReceiver);
            mAlarmChangedReceiver = null;
        }
    }

    private void setupLocaleChangeReceiver() {
        if (mLocaleChangedReceiver == null) {
            mLocaleChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG,"update widget");
                    updateWidget();
                }
            };
            registerReceiver(mLocaleChangedReceiver, new IntentFilter(Intent.ACTION_LOCALE_CHANGED));
        }
    }

    private void clearLocaleChangeReceiver() {
        if ( mLocaleChangedReceiver != null ) {
            unregisterReceiver(mLocaleChangedReceiver);
            mLocaleChangedReceiver = null;
        }
    }
}
