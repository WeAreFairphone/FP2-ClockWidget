package com.fairphone.clock.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
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

public class ClockWidget extends AppWidgetProvider {

    private static final String TAG = ClockWidget.class.getSimpleName();

    public static final int[] CLOCK_WIDGET_LAYOUTS = { R.id.clock_widget_main, R.id.clock_widget_peace_of_mind, R.id.clock_widget_battery, R.id.clock_widget_yours_since};

    private static final SecureRandom r = new SecureRandom();


    @Override
    public void onEnabled(Context context)
    {
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.wtf(TAG, "onUpdate()");
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        context.startService(new Intent(context, ClockScreenService.class));

        RemoteViews mainWidgetView = new RemoteViews(context.getPackageName(), R.layout.widget_main);
        setupView(context, mainWidgetView);

        appWidgetManager.updateAppWidget(appWidgetIds, null);
        appWidgetManager.updateAppWidget(appWidgetIds, mainWidgetView);
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
                setNextScheduledAlarm(context, widget);
                break;
            case R.id.clock_widget_peace_of_mind:
                setupShareOnClick(context, widget, R.id.peace_share_button);
                break;
            case R.id.clock_widget_battery:
                int batteryLevel = sharedPrefs.getInt(ClockScreenService.PREFERENCE_BATTERY_LEVEL, 0);
                showBatteryLevel(widget, batteryLevel);
                setupLastLongerOnClick(context, widget);
                break;
            case R.id.clock_widget_yours_since:
                setupShareOnClick(context, widget, R.id.yours_since_share_button);
                break;
            default:
                Log.wtf(TAG, "Unknow layout: " + active_layout);
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            SimpleDateFormat sdf;
            if(DateFormat.is24HourFormat(context)) {
                sdf = new SimpleDateFormat(context.getResources().getString(R.string.alarm_clock_24h_format));
            }else {
                sdf = new SimpleDateFormat(context.getResources().getString(R.string.alarm_clock_12h_format));
            }
            if(am != null && am.getNextAlarmClock() != null) {
                nextAlarm = sdf.format(am.getNextAlarmClock().getTriggerTime());
            }
        } else {
            nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        }
        return nextAlarm;
    }

    private void showBatteryLevel(RemoteViews widget, int batteryLevel) {

        if (batteryLevel <= 10) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_10);
        } else if (batteryLevel <= 20) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_20);
        } else if (batteryLevel <= 30) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_30);
        } else if (batteryLevel <= 40) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_40);
        } else if (batteryLevel <= 50) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_50);
        } else if (batteryLevel <= 60) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_60);
        } else if (batteryLevel <= 70) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_70);
        } else if (batteryLevel <= 80) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_80);
        } else if (batteryLevel <= 90) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_90);
        } else if (batteryLevel <= 100) {
            widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_100);
        }
    }
}
