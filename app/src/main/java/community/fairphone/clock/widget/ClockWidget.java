package community.fairphone.clock.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import community.fairphone.clock.ClockScreenService;
import community.fairphone.clock.R;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ClockWidget extends AppWidgetProvider {

    private static final String TAG = ClockWidget.class.getSimpleName();

    private static final SecureRandom r = new SecureRandom();

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled");
        super.onEnabled(context);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        context.startService(new Intent(context, ClockScreenService.class));
        RemoteViews mainWidgetView = new RemoteViews(context.getPackageName(), R.layout.widget_main);
        setupView(context, mainWidgetView);

        appWidgetManager.updateAppWidget(appWidgetIds, null);
        appWidgetManager.updateAppWidget(appWidgetIds, mainWidgetView);

    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled");
        super.onDisabled(context);
        context.stopService(new Intent(context, ClockScreenService.class));
    }

    private void setupView(Context context, RemoteViews mainWidgetView) {
        setupActiveView(context, mainWidgetView);
    }

    private void setupActiveView(Context context, RemoteViews widget) {
        setClockAmPm(context, widget);
        setNextScheduledAlarm(context, widget);
        setupEditOnClick(context, widget);
    }

    private static void setupEditOnClick(Context context, RemoteViews widget) {
        String intentAction = Build.VERSION.SDK_INT >= 19 ? AlarmClock.ACTION_SHOW_ALARMS : AlarmClock.ACTION_SET_ALARM;
        Intent launchIntent = new Intent(intentAction);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(context, r.nextInt(), launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        widget.setOnClickPendingIntent(R.id.clock_edit_button, launchPendingIntent);
    }

    private static void setClockAmPm(Context context, RemoteViews widget) {
        if (DateFormat.is24HourFormat(context)) {
            widget.setViewVisibility(R.id.ampm_text, View.GONE);
        } else {
            widget.setViewVisibility(R.id.ampm_text, View.VISIBLE);
            Calendar currentCalendar = Calendar.getInstance();

            int hour = currentCalendar.get(Calendar.HOUR_OF_DAY);

            if (hour < 12) {
                widget.setTextViewText(R.id.ampm_text, context.getResources().getString(R.string.time_am_default));
            } else {
                widget.setTextViewText(R.id.ampm_text, context.getResources().getString(R.string.time_pm_default));
            }
        }
    }

    private void setNextScheduledAlarm(Context context, RemoteViews widget) {
        String nextAlarm = getNextAlarm(context);

        if (TextUtils.isEmpty(nextAlarm)) {
            widget.setViewVisibility(R.id.alarm_text, View.INVISIBLE);
        } else {
            widget.setTextViewText(R.id.alarm_text, nextAlarm);
            widget.setViewVisibility(R.id.alarm_text, View.VISIBLE);
        }

    }

    private static String getNextAlarm(Context context) {
        String nextAlarm = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null && am.getNextAlarmClock() != null) {
                String amPmMarker = "";
                SimpleDateFormat sdf;
                boolean is24hFormat = DateFormat.is24HourFormat(context);
                long alarmTriggerTime = am.getNextAlarmClock().getTriggerTime();

                if (is24hFormat) {
                    sdf = new SimpleDateFormat(context.getResources().getString(R.string.alarm_clock_24h_format));
                } else {
                    sdf = new SimpleDateFormat(context.getResources().getString(R.string.alarm_clock_12h_format));
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(alarmTriggerTime);

                    if (cal.get(Calendar.HOUR_OF_DAY) < 12) {
                        amPmMarker = " " + context.getResources().getString(R.string.time_am_default);
                    } else {
                        amPmMarker = " " + context.getResources().getString(R.string.time_pm_default);
                    }
                }
                nextAlarm = sdf.format(am.getNextAlarmClock().getTriggerTime()) + amPmMarker;
            }
        } else {
            nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        }
        return nextAlarm;
    }

}

