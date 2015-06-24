package com.fairphone.clock.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.fairphone.clock.ClockScreenService;
import com.fairphone.clock.R;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ClockWidget extends AppWidgetProvider {

    private static final String TAG = ClockWidget.class.getSimpleName();

    public static final int[] CLOCK_WIDGET_LAYOUTS = { R.id.clock_widget_main, R.id.clock_widget_peace_of_mind, R.id.clock_widget_battery, R.id.clock_widget_yours_since};

    private static final SecureRandom r = new SecureRandom();

    public static final String CLOCK_AM_PM_UPDATE = "com.fairphone.clock.widget.ClockWidget.CLOCK_AM_PM_UPDATE";

    @Override
    public void onEnabled(Context context)
    {
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.wtf(TAG, "Update");
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        context.startService(new Intent(context, ClockScreenService.class));

        RemoteViews mainWidgetView = new RemoteViews(context.getPackageName(), R.layout.widget_main);
        setupView(context, mainWidgetView);
        setupAmPmManager(context);

        appWidgetManager.updateAppWidget(appWidgetIds, null);
        appWidgetManager.updateAppWidget(appWidgetIds, mainWidgetView);




    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(createUpdateIntent(context));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(CLOCK_AM_PM_UPDATE.equals(intent.getAction())) {
            context.startService(new Intent(context, ClockScreenService.class));
        }
    }

    private void setupAmPmManager(Context context)
    {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE, 60-calendar.get(Calendar.MINUTE));
        calendar.add(Calendar.SECOND, 60-calendar.get(Calendar.SECOND));

        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 3600000, createUpdateIntent(context));
    }

    private void setupView(Context context, RemoteViews mainWidgetView) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(ClockScreenService.FAIRPHONE_CLOCK_PREFERENCES, Context.MODE_PRIVATE);
        int active_layout = getActiveLayout(sharedPrefs);
        mainWidgetView.setViewVisibility(active_layout, View.VISIBLE);
        setupActiveView(context, mainWidgetView, active_layout, sharedPrefs);
        setupWidgetOnClick(context, mainWidgetView, active_layout);
    }

    public static int getActiveLayout(SharedPreferences sharedPrefs) {
        int currentLayout = sharedPrefs.getInt(ClockScreenService.PREFERENCE_ACTIVE_LAYOUT, 0);
        return CLOCK_WIDGET_LAYOUTS[(currentLayout) % CLOCK_WIDGET_LAYOUTS.length];
    }

    private void setupActiveView(Context context, RemoteViews widget, int active_layout, SharedPreferences sharedPrefs) {
        switch (active_layout){
            case R.id.clock_widget_main:
                setClockAmPm(context, widget);
                setNextScheduledAlarm(context, widget);
                break;
            case R.id.clock_widget_peace_of_mind:
                setupShareOnClick(context, widget, R.id.peace_share_button);
                break;
            case R.id.clock_widget_battery:
                int batteryLevel = sharedPrefs.getInt(ClockScreenService.PREFERENCE_BATTERY_LEVEL, 0);
                int batteryStatus = sharedPrefs.getInt(ClockScreenService.PREFERENCE_BATTERY_STATUS, 0);
                updateBatteryStatusAndLevel(context, widget, batteryLevel, batteryStatus);
                setupLastLongerOnClick(context, widget);
                break;
            case R.id.clock_widget_yours_since:
                setupShareOnClick(context, widget, R.id.yours_since_share_button);
                break;
            default:
                Log.wtf(TAG, "Unknown layout: " + active_layout);
        }
    }

    private void setupLastLongerOnClick(Context context, RemoteViews widget) {
        Intent launchIntent = new Intent(ClockScreenService.ACTION_BATTERY_SAVER);
        PendingIntent launchPendingIntent = PendingIntent.getBroadcast(context, r.nextInt(), launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        widget.setOnClickPendingIntent(R.id.last_longer_button, launchPendingIntent);
    }

    private void setupShareOnClick(Context context, RemoteViews widget, int shareButtonId) {
        Intent launchIntent = new Intent(ClockScreenService.ACTION_SHARE);
        PendingIntent launchPendingIntent = PendingIntent.getBroadcast(context, r.nextInt(), launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        widget.setOnClickPendingIntent(shareButtonId, launchPendingIntent);
    }

    private void setupWidgetOnClick(Context context, RemoteViews widget, int viewId) {
        Intent launchIntent = new Intent(ClockScreenService.ACTION_ROTATE_VIEW);
        PendingIntent launchPendingIntent = PendingIntent.getBroadcast(context, r.nextInt(), launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        widget.setOnClickPendingIntent(viewId, launchPendingIntent);
    }

    private void setClockAmPm(Context context, RemoteViews widget)
    {
        Calendar currentCalendar = Calendar.getInstance();
        int hour = currentCalendar.get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            widget.setTextViewText(R.id.ampm_text, context.getResources().getString(R.string.time_am_default));
        }
        else{
            widget.setTextViewText(R.id.ampm_text, context.getResources().getString(R.string.time_pm_default));
        }
    }

    private void setNextScheduledAlarm(Context context, RemoteViews widget)
    {
        String nextAlarm = getNextAlarm(context);

        if(TextUtils.isEmpty(nextAlarm)) {
            widget.setViewVisibility(R.id.alarm_text, View.INVISIBLE);
        } else {
            widget.setTextViewText(R.id.alarm_text, nextAlarm);
            widget.setViewVisibility(R.id.alarm_text, View.VISIBLE);
        }

    }

    private String getNextAlarm(Context context) {
        String nextAlarm = "";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            if(am != null && am.getNextAlarmClock() != null)
            {
                String amPmMarker = "";
                SimpleDateFormat sdf;
                boolean is24hFormat = DateFormat.is24HourFormat(context);
                long alarmTriggerTime = am.getNextAlarmClock().getTriggerTime();

                if(is24hFormat)
                {
                    sdf = new SimpleDateFormat(context.getResources().getString(R.string.alarm_clock_24h_format));
                }else
                {
                    sdf = new SimpleDateFormat(context.getResources().getString(R.string.alarm_clock_12h_format));
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(alarmTriggerTime);

                    if(cal.get(Calendar.HOUR_OF_DAY) < 12)
                    {
                        amPmMarker = " " + context.getResources().getString(R.string.time_am_default);
                    }else{
                        amPmMarker = " " + context.getResources().getString(R.string.time_pm_default);
                    }
                }
                nextAlarm = sdf.format(am.getNextAlarmClock().getTriggerTime()) + amPmMarker;
            }
        }
        else
        {
            nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        }
        return nextAlarm;
    }

    private void updateBatteryLevel(RemoteViews widget, int level) {

        if (level <= 10) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_10);
        } else if (level <= 20) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_20);
        } else if (level <= 30) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_30);
        } else if (level <= 40) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_40);
        } else if (level <= 50) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_50);
        } else if (level <= 60) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_60);
        } else if (level <= 70) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_70);
        } else if (level <= 80) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_80);
        } else if (level <= 90) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_90);
        } else if (level <= 100) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_100);
        }
    }

    private void updateBatteryStatusAndLevel(Context context, RemoteViews widget, int level, int status) {

        Resources resources = context.getResources();
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                widget.setTextViewText(R.id.battery_description, resources.getString(R.string.battery_will_be_charged_at));
                widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_charging);
                widget.setViewVisibility(R.id.last_longer_button, View.INVISIBLE);
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                widget.setTextViewText(R.id.battery_description, resources.getString(R.string.battery_is_fully_charged));
                widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_100);
                widget.setViewVisibility(R.id.last_longer_button, View.INVISIBLE);
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default:
                widget.setTextViewText(R.id.battery_description, resources.getString(R.string.battery_charge_will_last_until));
                updateBatteryLevel(widget, level);
                widget.setViewVisibility(R.id.last_longer_button, View.VISIBLE);
                break;
        }
    }

    public static String getBatteryStatusAsString(int status){
        String desc = "Uknown: ";
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                desc = "BATTERY_STATUS_CHARGING";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                desc = "BATTERY_STATUS_FULL";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                desc = "BATTERY_STATUS_DISCHARGING";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                desc = "BATTERY_STATUS_NOT_CHARGING";
                break;
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                desc = "BATTERY_STATUS_UNKNOWN";
                break;
            default:
                desc += status;
                break;
        }
        return desc;
    }

    private PendingIntent createUpdateIntent(Context context){
        Intent intent = new Intent(CLOCK_AM_PM_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }
}
