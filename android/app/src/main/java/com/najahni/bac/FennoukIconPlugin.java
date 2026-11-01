package com.najahni.bac;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.concurrent.TimeUnit;

/**
 * JS: FennoukIcon.sync({mood, lastStudy, start, remind})
 * Stores the study state; MoodWorker changes the launcher icon while the app is closed.
 */
@CapacitorPlugin(name = "FennoukIcon")
public class FennoukIconPlugin extends Plugin {
    @Override
    public void load() {
        IconSwitcher.prefs(getContext()).edit().putBoolean("foreground", true).apply();
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(MoodWorker.class, 3, TimeUnit.HOURS).build();
        WorkManager.getInstance(getContext())
                .enqueueUniquePeriodicWork("fennouk-mood", ExistingPeriodicWorkPolicy.KEEP, req);
    }

    @PluginMethod
    public void sync(PluginCall call) {
        String mood = call.getString("mood", "neutral");
        IconSwitcher.prefs(getContext()).edit()
                .putString("lastStudy", call.getString("lastStudy", ""))
                .putString("start", call.getString("start", ""))
                .putString("remind", call.getString("remind", "18:00"))
                .apply();
        JSObject ret = new JSObject();
        ret.put("pending", mood);
        call.resolve(ret);
    }

    @PluginMethod
    public void current(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("mood", IconSwitcher.prefs(getContext()).getString("appliedMood", "happy"));
        call.resolve(ret);
    }

    @Override
    protected void handleOnResume() {
        super.handleOnResume();
        IconSwitcher.prefs(getContext()).edit().putBoolean("foreground", true).apply();
    }

    @Override
    protected void handleOnStop() {
        super.handleOnStop();
        IconSwitcher.prefs(getContext()).edit().putBoolean("foreground", false).apply();
        // Apply the icon a little later, and only if the app is still closed. Switching the launcher
        // alias right away could close the app while a system screen (photo picker, permission) is open.
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(MoodWorker.class)
                .setInitialDelay(20, TimeUnit.SECONDS).build();
        WorkManager.getInstance(getContext())
                .enqueueUniqueWork("fennouk-apply", ExistingWorkPolicy.REPLACE, req);
    }

    @Override
    protected void handleOnDestroy() {
        IconSwitcher.prefs(getContext()).edit().putBoolean("foreground", false).apply();
        super.handleOnDestroy();
    }
}
