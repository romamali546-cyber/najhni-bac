package com.najahni.bac;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Runs a few times a day so the icon turns worried/sad even when the app is never opened. */
public class MoodWorker extends Worker {
    public MoodWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        // Changing the launcher alias can close the app on some phones, so never do it while it is open.
        if (IconSwitcher.prefs(ctx).getBoolean("foreground", false)) return Result.success();
        try {
            IconSwitcher.apply(ctx, IconSwitcher.computeMood(ctx));
        } catch (Exception e) {
            return Result.retry();
        }
        return Result.success();
    }
}
