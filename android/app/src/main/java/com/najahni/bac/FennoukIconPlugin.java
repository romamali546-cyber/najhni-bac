package com.najahni.bac;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
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

    /** JS: FennoukIcon.shareImage({base64, text}) - shares a PNG through Android's share sheet. */
    @PluginMethod
    public void shareImage(PluginCall call) {
        shareBytes(call, "najahni-bac-share.png", "image/png", "شارك سلسلتك");
    }

    /** JS: FennoukIcon.shareFile({base64, name, mime, text}) - e.g. the progress backup file. */
    @PluginMethod
    public void shareFile(PluginCall call) {
        String name = call.getString("name", "najahni-bac-backup.json").replaceAll("[^A-Za-z0-9._-]", "_");
        shareBytes(call, name, call.getString("mime", "application/json"), "احفظ نسختك الاحتياطية");
    }

    private void shareBytes(PluginCall call, String name, String mime, String title) {
        try {
            byte[] bytes = Base64.decode(call.getString("base64", ""), Base64.DEFAULT);
            File file = new File(getContext().getCacheDir(), name);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(bytes);
            }
            Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(mime);
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_TEXT, call.getString("text", ""));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(send, title);
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getActivity().startActivity(chooser);
            call.resolve();
        } catch (Exception e) {
            call.reject("share failed: " + e.getMessage());
        }
    }

    /** JS: FennoukIcon.systemBars({dark}) - matches the status/navigation bars to the app theme. */
    @PluginMethod
    public void systemBars(PluginCall call) {
        boolean dark = Boolean.TRUE.equals(call.getBoolean("dark", false));
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setSystemBars(dark);
        call.resolve();
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
