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

import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ClockWidget extends AppWidgetProvider {

    private static final String TAG = ClockWidget.class.getSimpleName();

    public static final int[] CLOCK_WIDGET_LAYOUTS = {R.id.clock_widget_main, R.id.clock_widget_peace_of_mind, R.id.clock_widget_battery, R.id.clock_widget_yours_since};

    private static final SecureRandom r = new SecureRandom();

    @Override
    public void onEnabled(Context context) {
        Log.wtf(TAG, "onEnabled");
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.wtf(TAG, "onUpdate");
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        context.startService(new Intent(context, ClockScreenService.class));
        RemoteViews mainWidgetView = new RemoteViews(context.getPackageName(), R.layout.widget_main);
        setupView(context, mainWidgetView);

        appWidgetManager.updateAppWidget(appWidgetIds, null);
        appWidgetManager.updateAppWidget(appWidgetIds, mainWidgetView);

    }

    @Override
    public void onDisabled(Context context) {
        Log.wtf(TAG, "onDisabled");
        super.onDisabled(context);

        context.stopService(new Intent(context, ClockScreenService.class));
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
        switch (active_layout) {
            case R.id.clock_widget_main:
                setClockAmPm(context, widget);
                setNextScheduledAlarm(context, widget);
                break;
            case R.id.clock_widget_peace_of_mind:
                setupPeaceOfMind(widget, sharedPrefs);
                setupShareOnClick(context, widget, R.id.peace_share_button);
                break;
            case R.id.clock_widget_battery:
                setupBatteryLayout(context, widget, sharedPrefs);
                break;
            case R.id.clock_widget_yours_since:
                setYourFairphoneSince(context, widget, sharedPrefs.getLong(ClockScreenService.PREFERENCE_YOUR_FAIRPHONE_SINCE, 0L));
                setupShareOnClick(context, widget, R.id.yours_since_share_button);
                break;
            default:
                Log.wtf(TAG, "Unknown layout: " + active_layout);

        }
    }

    private void setupBatteryLayout(Context context, RemoteViews widget, SharedPreferences sharedPrefs) {
        int batteryLevel = sharedPrefs.getInt(ClockScreenService.PREFERENCE_BATTERY_LEVEL, 0);
        int batteryStatus = sharedPrefs.getInt(ClockScreenService.PREFERENCE_BATTERY_STATUS, 0);
        long chargingTime = sharedPrefs.getLong(ClockScreenService.PREFERENCE_BATTERY_TIME_UNTIL_CHARGED, System.currentTimeMillis());
        long remainingTime = sharedPrefs.getLong(ClockScreenService.PREFERENCE_BATTERY_TIME_UNTIL_DISCHARGED, System.currentTimeMillis());
        updateBatteryStatusAndLevel(context, widget, batteryLevel, batteryStatus, remainingTime, chargingTime);
        setupLastLongerOnClick(context, widget);
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

    private void setupPeaceOfMind(RemoteViews widget, SharedPreferences sharedPrefs) {
        long pom_current = sharedPrefs.getLong(ClockScreenService.PREFERENCE_POM_CURRENT, 0L);
        long pom_record = sharedPrefs.getLong(ClockScreenService.PREFERENCE_POM_RECORD, 0L);
        widget.setTextViewText(R.id.text_pom_current, Long.toString(pom_current));
        widget.setTextViewText(R.id.text_pom_record, Long.toString(pom_record));
    }

    private void setupWidgetOnClick(Context context, RemoteViews widget, int viewId) {
        Intent launchIntent = new Intent(ClockScreenService.ACTION_ROTATE_VIEW);
        PendingIntent launchPendingIntent = PendingIntent.getBroadcast(context, r.nextInt(), launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        widget.setOnClickPendingIntent(viewId, launchPendingIntent);
    }

    private void setClockAmPm(Context context, RemoteViews widget) {
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

    private String getNextAlarm(Context context) {
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

    private void updateBatteryLevel(RemoteViews widget, int level, boolean isCharging) {

        if (level <= 5) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_00 : R.drawable.battery_00);
        } else if (level <= 10) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_10 : R.drawable.battery_10);
        } else if (level <= 20) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_20 : R.drawable.battery_20);
        } else if (level <= 30) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_30 : R.drawable.battery_30);
        } else if (level <= 40) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_40 : R.drawable.battery_40);
        } else if (level <= 50) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_50 : R.drawable.battery_50);
        } else if (level <= 60) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_60 : R.drawable.battery_60);
        } else if (level <= 70) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_70 : R.drawable.battery_70);
        } else if (level <= 80) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_80 : R.drawable.battery_80);
        } else if (level <= 90) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_90 : R.drawable.battery_90);
        } else if (level <= 100) {
            widget.setImageViewResource(R.id.battery_level_image, isCharging ? R.drawable.battery_charging_100 : R.drawable.battery_100);
        }
    }

    private void updateBatteryStatusAndLevel(Context context, RemoteViews widget, int level, int status, long remainingTime, long chargingTime) {

        Resources resources = context.getResources();
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                updateBatteryLevel(widget, level, true);
                widget.setViewVisibility(R.id.charged_text, View.GONE);
                widget.setViewVisibility(R.id.unplug_charger_text, View.GONE);
                widget.setViewVisibility(R.id.last_longer_button, View.INVISIBLE);

                getRemainingTime(context, resources, widget, chargingTime, true);
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                widget.setTextViewText(R.id.battery_description, resources.getString(R.string.battery_is_fully));
                widget.setImageViewResource(R.id.battery_level_image, R.drawable.battery_charging_100);
                widget.setViewVisibility(R.id.last_longer_button, View.INVISIBLE);
                widget.setViewVisibility(R.id.battery_time_group, View.GONE);
                widget.setViewVisibility(R.id.charged_text, View.VISIBLE);
                widget.setViewVisibility(R.id.unplug_charger_text, View.VISIBLE);
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default:

                updateBatteryLevel(widget, level, false);
                widget.setViewVisibility(R.id.charged_text, View.GONE);
                widget.setViewVisibility(R.id.unplug_charger_text, View.GONE);
                widget.setViewVisibility(R.id.last_longer_button, View.VISIBLE);

                getRemainingTime(context, resources, widget, remainingTime, false);
                break;
        }
    }

    private void getRemainingTime(Context context, Resources resources, RemoteViews widget, long remainingTime, boolean isCharging) {
        DateTime currentTime = new DateTime();
        DateTime endtime = currentTime.plus(remainingTime);

        Log.d(TAG, "\ncurrentTime" + currentTime.toString() + "\nendTime: " + endtime.toString() + "\nRemaining time: " + PeriodFormat.getDefault().withLocale(Locale.getDefault()).print(new Period(remainingTime)));
        if (endtime.getDayOfMonth() <= currentTime.getDayOfMonth() + 1) {
            if (endtime.getDayOfMonth() != currentTime.getDayOfMonth()) {
                widget.setViewVisibility(R.id.day_indicator, View.VISIBLE);
            } else {
                widget.setViewVisibility(R.id.day_indicator, View.INVISIBLE);
            }
            widget.setTextViewText(R.id.battery_description, isCharging ? resources.getString(R.string.battery_will_be_charged_at) : resources.getString(R.string.battery_charge_will_last_until));
            widget.setViewVisibility(R.id.battery_time_group, View.VISIBLE);
            widget.setViewVisibility(R.id.battery_days_left, View.GONE);
            if (DateFormat.is24HourFormat(context)) {
                widget.setTextViewText(R.id.hours_text, String.format("%02d", endtime.getHourOfDay()));
                widget.setTextViewText(R.id.battery_am_pm_indicator, "");
            } else {
                widget.setTextViewText(R.id.hours_text, String.format("%d", endtime.property(DateTimeFieldType.clockhourOfHalfday()).get()));
                widget.setTextViewText(R.id.battery_am_pm_indicator, endtime.property(DateTimeFieldType.halfdayOfDay()).get() == 0 ? resources.getString(R.string.time_am_default) : resources.getString(R.string.time_pm_default));
            }
            widget.setTextViewText(R.id.minutes_text, String.format("%02d", endtime.getMinuteOfHour()));
        } else {
            widget.setTextViewText(R.id.battery_description, isCharging ? resources.getString(R.string.battery_will_be_charged_in) : resources.getString(R.string.battery_charge_will_last));
            Period remaining = new Period(currentTime.getMillis(), endtime.getMillis());
            int diffMonthDays = ((remaining.getWeeks() * 7) + remaining.getDays());
            widget.setTextViewText(R.id.battery_days_left, String.format("%d %s", (diffMonthDays == 1 ? resources.getString(R.string.day) : resources.getString(R.string.days))));
            widget.setViewVisibility(R.id.battery_time_group, View.GONE);
            widget.setViewVisibility(R.id.battery_days_left, View.VISIBLE);
        }
    }

    public static String getBatteryStatusAsString(int status) {
        String desc = "Unknown: ";
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

    private void setYourFairphoneSince(Context context, RemoteViews widget, long startTime) {

        Resources resources = context.getResources();
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }
        Period pp = new Period(startTime, System.currentTimeMillis());

        //Log.d(TAG, "Yours since: " + PeriodFormat.getDefault().print(pp));
        int diffYears = pp.getYears();
        int diffMonths = pp.getMonths();
        int diffWeeks = pp.getWeeks();
        int diffDays = pp.getDays();
        int diffMonthDays = ((pp.getWeeks() * 7) + pp.getDays());
        int diffHours = pp.getHours();

        if (pp.getYears() != 0) {
            widget.setTextViewText(R.id.eleapsed_years_text, String.format("%02d", diffYears));
            widget.setTextViewText(R.id.years_text, diffYears == 1 ? resources.getString(R.string.year) : resources.getString(R.string.years));
            widget.setTextViewText(R.id.eleapsed_months_text, String.format("%02d", diffMonths));
            widget.setTextViewText(R.id.months_text, diffMonths == 1 ? resources.getString(R.string.month) : resources.getString(R.string.months));
            widget.setTextViewText(R.id.eleapsed_days_text, String.format("%02d", diffMonthDays));
            widget.setTextViewText(R.id.days_text, diffMonthDays == 1 ? resources.getString(R.string.day) : resources.getString(R.string.days));
        } else if (pp.getMonths() != 0) {
            widget.setTextViewText(R.id.eleapsed_years_text, String.format("%02d", diffMonths));
            widget.setTextViewText(R.id.years_text, diffMonths == 1 ? resources.getString(R.string.month) : resources.getString(R.string.months));
            widget.setTextViewText(R.id.eleapsed_months_text, String.format("%02d", diffWeeks));
            widget.setTextViewText(R.id.months_text, diffWeeks == 1 ? resources.getString(R.string.week) : resources.getString(R.string.weeks));
            widget.setTextViewText(R.id.eleapsed_days_text, String.format("%02d", diffDays));
            widget.setTextViewText(R.id.days_text, diffDays == 1 ? resources.getString(R.string.day) : resources.getString(R.string.days));
        } else {
            widget.setTextViewText(R.id.eleapsed_years_text, String.format("%02d", diffWeeks));
            widget.setTextViewText(R.id.years_text, diffWeeks == 1 ? resources.getString(R.string.week) : resources.getString(R.string.weeks));
            widget.setTextViewText(R.id.eleapsed_months_text, String.format("%02d", diffDays));
            widget.setTextViewText(R.id.months_text, diffDays == 1 ? resources.getString(R.string.day) : resources.getString(R.string.days));
            widget.setTextViewText(R.id.eleapsed_days_text, String.format("%02d", diffHours));
            widget.setTextViewText(R.id.days_text, diffHours == 1 ? resources.getString(R.string.hour) : resources.getString(R.string.hours));
        }
    }
}

