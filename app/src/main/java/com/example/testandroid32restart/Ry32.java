package com.example.testandroid32restart;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import java.lang.reflect.Method;

public class Ry32 {


    public static void popStayInForegroundTasks(Activity activity) {
        Intent trampolineIntent = new Intent(activity, TrampolineActivity.class);
        activity.startActivity(trampolineIntent);
        Intent chooserIntent = Intent.createChooser(new Intent(activity.getPackageName() + ".action.TRAMPOLINE"), "Loading...");
        activity.startActivity(chooserIntent);
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void forceRestartIntoDifferentAbi(Activity activity, Intent targetIntent) {
        final Intent theIntent = targetIntent != null? targetIntent:
                activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        boolean needsToStayInForeground = Build.VERSION.SDK_INT >= 29; // Android 10 requires alarm
        if (needsToStayInForeground) {
            popStayInForegroundTasks(activity);
        }
        rebootIntoInstrumentation(activity, theIntent);
    }

    @SuppressLint("PrivateApi")
    private static void rebootIntoInstrumentation(Activity activity, Intent targetIntent) {

        boolean launchViaAlarm = Build.VERSION.SDK_INT >= 29; // Android 10 requires alarm
        if (launchViaAlarm) {
            setupAlarm(activity, targetIntent);
        }
        String abiOverride = "armeabi-v7a";
        Bundle bundle = new Bundle();
        if (launchViaAlarm) {
            bundle.putByte("skipLaunch", (byte)1);
        } else {
            bundle.putParcelable("launchIntent", targetIntent);
        }
        ComponentName componentName = new ComponentName(activity, Relaunch.class);

        Log.i("Ry32", "component name: " + componentName);
        try {
            Object iActivityManager;
            try {
                Method ActivityManager_getService = ActivityManager.class.getMethod("getService");
                iActivityManager = ActivityManager_getService.invoke(null);
            } catch (NoSuchMethodException nme) {
                Log.e("Ry32", String.valueOf(nme));
                // Android 7.1 and below
                Method ActivityManagerNative_getDefault =
                        Class.forName("android.app.ActivityManagerNative").getMethod("getDefault");
                iActivityManager = ActivityManagerNative_getDefault.invoke(null);
            }

            assert iActivityManager != null;
            Method IActivityManager_startInstrumentation = iActivityManager.getClass().getMethod("startInstrumentation",
                    ComponentName.class, String.class, Integer.TYPE, Bundle.class,
                    Class.forName("android.app.IInstrumentationWatcher"),
                    Class.forName("android.app.IUiAutomationConnection"),
                    Integer.TYPE, String.class);
            Log.i("Ry32", "Starting app as 32 bit");
            boolean response = (boolean)IActivityManager_startInstrumentation.invoke(iActivityManager,
                    /* className= */ componentName,
                    /* profileFile= */ null, /* flags= */ 0, /* arguments= */ bundle,
                    /* watcher= */ null, /* connection= */ null, /* userId= */ 0,
                    /* abiOverride= */ abiOverride);

//            Bundle arguments = new Bundle();
//            arguments.putString("abi", abiOverride);
//
//            Method runInstrumentationMethod = iActivityManager.getClass().getMethod(
//                    "startInstrumentation", ComponentName.class, String.class, int.class, Bundle.class,
//                    Class.forName("android.app.IInstrumentationWatcher"),
//                    Class.forName("android.app.IUiAutomationConnection"), int.class, String.class);
//            runInstrumentationMethod.invoke(iActivityManager, componentName, null, 0, arguments, null, null, 0, null);

            Log.i("Ry32", "Started app as 32 bit (it should): " + false);
            // this call will force-close the process momentarily.
//            while (true) {
//                Thread.sleep(10000);
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setupAlarm(Activity activity, Intent targetIntent) {
        // borrowed from the existing restart code
        int delay = 1000;
        AlarmManager alarmMgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        long timeMillis = SystemClock.elapsedRealtime() + delay;
        Intent intent = new Intent(targetIntent);
        // trying to close the stupid chooser activity...
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME, timeMillis,
                PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_IMMUTABLE));
    }
}
