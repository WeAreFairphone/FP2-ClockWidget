package com.fairphone.clock.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.fairphone.clock.ClockScreenService;
import com.fairphone.clock.R;

import java.security.SecureRandom;

public class ClockWidget extends AppWidgetProvider {

    private static final String TAG = ClockWidget.class.getSimpleName();

    public static final int[] CLOCK_WIDGET_LAYOUTS = { R.id.clock_widget_main, R.id.clock_widget_peace_of_mind, R.id.clock_widget_battery, R.id.clock_widget_yours_since};
    private static int CURRENT_LAYOUT = 0;

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
        int active_layout = CLOCK_WIDGET_LAYOUTS[(CURRENT_LAYOUT++) % CLOCK_WIDGET_LAYOUTS.length];
        mainWidgetView.setViewVisibility(active_layout, View.VISIBLE);
        setupActiveView(mainWidgetView, active_layout);
        setupWidgetOnClick(context, mainWidgetView, active_layout);
    }

    private void setupActiveView(RemoteViews mainWidgetView, int active_layout) {
        switch (active_layout){
            case R.id.clock_widget_main:
                String nextAlarm = "6:45";
                mainWidgetView.setTextViewText(R.id.alarm_text, nextAlarm);
                break;
            case R.id.clock_widget_peace_of_mind:
                break;
            case R.id.clock_widget_battery:
                break;
            case R.id.clock_widget_yours_since:
                break;
            default:
                Log.wtf(TAG, "Unknow layout: " + active_layout);
        }
    }

    private void setupWidgetOnClick(Context context, RemoteViews widget, int viewId) {
        Intent launchIntent = new Intent(ClockScreenService.ACTION_ROTATE_VIEW);
        PendingIntent launchPendingIntent = PendingIntent.getBroadcast(context, r.nextInt(), launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        widget.setOnClickPendingIntent(viewId, launchPendingIntent);
    }
}
