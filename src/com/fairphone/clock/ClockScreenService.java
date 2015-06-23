package com.fairphone.clock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.fairphone.clock.widget.ClockWidget;

import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Locale;

public class ClockScreenService extends Service {

    private static final String TAG = ClockScreenService.class.getSimpleName();

    public static final String ACTION_ROTATE_VIEW = "com.fairphone.clock.ACTION_ROTATE_VIEW";
    public static final String ACTION_SHARE = "com.fairphone.clock.ACTION_SHARE";
    public static final String ACTION_BATTERY_SAVER = "com.fairphone.clock.ACTION_BATTERY_SAVER";
    public static final String FAIRPHONE_CLOCK_PREFERENCES = "com.fairphone.clock.FAIRPHONE_CLOCK_PREFERENCES";
    public static final String PREFERENCE_BATTERY_LEVEL = "com.fairphone.clock.PREFERENCE_BATTERY_LEVEL";
    public static final String PREFERENCE_ACTIVE_LAYOUT = "com.fairphone.clock.PREFERENCE_ACTIVE_LAYOUT";
    public static final String PREFERENCE_BATTERY_STATUS = "com.fairphone.clock.PREFERENCE_BATTERY_STATUS";
    public static final String PREFERENCE_POM_CURRENT = "com.fairphone.clock.PREFERENCE_POM_CURRENT";
    public static final String PREFERENCE_POM_RECORD = "com.fairphone.clock.PREFERENCE_POM_RECORD";
    public static final String PREFERENCE_YOUR_FAIRPHONE_SINCE = "com.fairphone.clock.PREFERENCE_YOUR_FAIRPHONE_SINCE";
    public static final String PREFERENCE_BATTERY_CHANGED_TIMESTAMP = "com.fairphone.clock.PREFERENCE_BATTERY_CHANGED_TIMESTAMP";
    public static final String PREFERENCE_BATTERY_TIME_UNTIL_DISCHARGED = "com.fairphone.clock.PREFERENCE_BATTERY_TIME_UNTIL_DISCHARGED";
    public static final String PREFERENCE_BATTERY_TIME_UNTIL_CHARGED = "com.fairphone.clock.PREFERENCE_BATTERY_TIME_UNTIL_CHARGED";

    private BroadcastReceiver mRotateReceiver;
    private BroadcastReceiver mShareReceiver;
    private BroadcastReceiver mBatterySaverReceiver;
    private BroadcastReceiver mBatteryStatsReceiver;
    private SharedPreferences mSharedPreferences;
    private BroadcastReceiver mLockReceiver;
    private static int CURRENT_LAYOUT = 0;
    private long mScreenOffTimestamp = -1;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand");

        mSharedPreferences = getSharedPreferences(FAIRPHONE_CLOCK_PREFERENCES, MODE_PRIVATE);
        startYourFairphoneSinceCounter();
        setupLayoutRotateReceiver();
        setupShareReceiver();
        setupBatterySaverReceiver();
        setupLockReceiver();

        return START_STICKY;
    }

    private void setupLockReceiver() {
        if (mLockReceiver == null) {
            mLockReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                        mScreenOffTimestamp = System.currentTimeMillis();
                    } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                        if (mScreenOffTimestamp != -1) {
                            long pomInMinutes = (System.currentTimeMillis() - mScreenOffTimestamp) / 60000L;
                            long pomRecord = mSharedPreferences.getLong(PREFERENCE_POM_RECORD, 0);
                            if (pomInMinutes > pomRecord) {
                                pomRecord = pomInMinutes;
                            }
                            mSharedPreferences.edit().putLong(PREFERENCE_POM_CURRENT, pomInMinutes).putLong(PREFERENCE_POM_RECORD, pomRecord).apply();
                        }
                    }
                    updateWidget();
                }
            };
            registerReceiver(mLockReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
            registerReceiver(mLockReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        }
    }

    private void setupLayoutRotateReceiver() {
        if (mRotateReceiver == null) {
            mRotateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    getNextActiveLayout();
                    updateWidget();
                }
            };
            registerReceiver(mRotateReceiver, new IntentFilter(ACTION_ROTATE_VIEW));
        }
    }

    private void getNextActiveLayout() {
        CURRENT_LAYOUT++;
        saveIntPreference(PREFERENCE_ACTIVE_LAYOUT, CURRENT_LAYOUT);
    }

    private void setupBatterySaverReceiver() {
        if (mBatterySaverReceiver == null) {
            mBatterySaverReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    dismissKeyguard();
                    startActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            };
            registerReceiver(mBatterySaverReceiver, new IntentFilter(ACTION_BATTERY_SAVER));
        }

        if (mBatteryStatsReceiver == null) {
            mBatteryStatsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent != null && Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                        boolean batteryStatsChanged = updateBatteryPreferences(intent);
                        //only update if the current layout is the battery information
                        if (ClockWidget.getActiveLayout(mSharedPreferences) == R.id.clock_widget_battery && batteryStatsChanged) {
                            updateWidget();
                        }
                    }
                }
            };
            registerReceiver(mBatteryStatsReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }

    private void dismissKeyguard() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        if (keyguardManager.isKeyguardLocked()) {
            KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
            lock.disableKeyguard();
        }
    }

    private boolean updateBatteryPreferences(Intent intent) {
        boolean batteryChanged = false;
        int currentLevel = mSharedPreferences.getInt(PREFERENCE_BATTERY_LEVEL, 0);
        int currentStatus = mSharedPreferences.getInt(PREFERENCE_BATTERY_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        if (currentLevel != level || currentStatus != status) {
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            Log.d(TAG, "Battery Level: " + level + "\nBattery Status: " + ClockWidget.getBatteryStatusAsString(status) + "\nBattery Scale: " + scale);
            //Toast.makeText(getApplicationContext(), "Battery Status: " + ClockWidget.getBatteryStatusAsString(status),Toast.LENGTH_SHORT).show();
            saveIntPreference(PREFERENCE_BATTERY_LEVEL, level);
            saveIntPreference(PREFERENCE_BATTERY_STATUS, status);
            batteryChanged = true;
            long lasttime = mSharedPreferences.getLong(PREFERENCE_BATTERY_CHANGED_TIMESTAMP, System.currentTimeMillis());
            long now = System.currentTimeMillis();
            long timeUntilCharge = (now - lasttime) * (scale - level);
            long timeUntilDischarge = (now - lasttime) * (level);
            saveLongPreference(PREFERENCE_BATTERY_CHANGED_TIMESTAMP, now);
            saveLongPreference(PREFERENCE_BATTERY_TIME_UNTIL_DISCHARGED, timeUntilDischarge);
            saveLongPreference(PREFERENCE_BATTERY_TIME_UNTIL_CHARGED, timeUntilCharge);
        }

        return batteryChanged;
    }

    private void saveIntPreference(String preference, int value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(preference, value);
        editor.commit();
    }

    private void setupShareReceiver() {
        if (mShareReceiver == null) {
            mShareReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");

                    int active_layout = ClockWidget.getActiveLayout(mSharedPreferences);
                    String shareText = null;
                    Resources resources = getResources();
                    switch (active_layout) {
                        case R.id.clock_widget_peace_of_mind:
                            long pom_current = mSharedPreferences.getLong(PREFERENCE_POM_CURRENT, 0);
                            long pom_record = mSharedPreferences.getLong(PREFERENCE_POM_RECORD, 0);
                            shareText = "I've been in peace of mind for " + pom_current + " minutes!";
                            if (pom_current == pom_record) {
                                shareText += " A new personal best!";
                            }
                            break;
                        case R.id.clock_widget_yours_since:
                            shareText = "My Fairphone is ";
                            long startTime = mSharedPreferences.getLong(PREFERENCE_YOUR_FAIRPHONE_SINCE, 0L);
                            if (startTime == 0L) {
                                startTime = System.currentTimeMillis();
                            }
                            Period pp = new Period(startTime, System.currentTimeMillis(), PeriodType.standard().withSecondsRemoved().withMillisRemoved());
                            shareText += PeriodFormat.wordBased().print(pp) + " old!";
                            break;
                        default:
                            Log.w(TAG, "Unknown Share button: " + active_layout);
                    }
                    if (!TextUtils.isEmpty(shareText)) {
                        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                        sendIntent = Intent.createChooser(sendIntent, "Share to");
                        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        dismissKeyguard();
                        startActivity(sendIntent);
                    }
                }
            };
            registerReceiver(mShareReceiver, new IntentFilter(ACTION_SHARE));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, ClockWidget.class));
        if (appWidgetIds.length > 0) {
            new ClockWidget().onUpdate(this, appWidgetManager, appWidgetIds);
        }
    }

    private void startYourFairphoneSinceCounter() {
        if (mSharedPreferences.getLong(PREFERENCE_YOUR_FAIRPHONE_SINCE, 0L) == 0) {
            saveLongPreference(PREFERENCE_YOUR_FAIRPHONE_SINCE, System.currentTimeMillis());
        }
    }

    private void saveLongPreference(String preference, long value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(preference, value);
        editor.commit();
    }
}
