package com.najahni.bac;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import java.util.Calendar;

/**
 * Switches the launcher icon between the four moods of Fennouk.
 * Each mood is an <activity-alias> in AndroidManifest.xml; exactly one is enabled.
 */
public final class IconSwitcher {
    static final String PREFS = "fennouk";
    static final String[] MOODS = {"happy", "neutral", "worried", "sad"};

    private IconSwitcher() {}

    static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String alias(String mood) {
        return "com.najahni.bac.Icon" + Character.toUpperCase(mood.charAt(0)) + mood.substring(1);
    }

    /** Enables the alias for {@code mood} and disables the others. Safe to call repeatedly. */
    static void apply(Context ctx, String mood) {
        if (mood == null) return;
        boolean known = false;
        for (String m : MOODS) if (m.equals(mood)) known = true;
        if (!known) return;
        SharedPreferences p = prefs(ctx);
        if (mood.equals(p.getString("appliedMood", "happy"))) return;
        PackageManager pm = ctx.getPackageManager();
        // Enable the new one first so the app never has zero launcher entries.
        pm.setComponentEnabledSetting(new ComponentName(ctx, alias(mood)),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        for (String m : MOODS) {
            if (m.equals(mood)) continue;
            pm.setComponentEnabledSetting(new ComponentName(ctx, alias(m)),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        p.edit().putString("appliedMood", mood).apply();
    }

    /** Same rules as mood() in the web app, computed from what the app last synced. */
    static String computeMood(Context ctx) {
        SharedPreferences p = prefs(ctx);
        String last = p.getString("lastStudy", "");
        String start = p.getString("start", "");
        String remind = p.getString("remind", "18:00");
        long today = dayNumber(Calendar.getInstance());
        if (!last.isEmpty() && dayNumber(parse(last)) == today) return "happy";
        if (last.isEmpty() && !start.isEmpty() && dayNumber(parse(start)) >= today) return "neutral";
        long ref = !last.isEmpty() ? dayNumber(parse(last)) : (start.isEmpty() ? today - 1 : dayNumber(parse(start)) - 1);
        long missed = Math.max(0, today - ref - 1);
        if (missed == 0) {
            Calendar now = Calendar.getInstance();
            int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int remMin = 18 * 60;
            try {
                String[] hm = remind.split(":");
                remMin = Integer.parseInt(hm[0]) * 60 + Integer.parseInt(hm[1]);
            } catch (Exception ignored) {}
            return nowMin >= remMin ? "worried" : "neutral";
        }
        if (missed == 1) return "worried";
        return "sad";
    }

    private static Calendar parse(String ymd) {
        Calendar c = Calendar.getInstance();
        try {
            String[] a = ymd.split("-");
            c.set(Integer.parseInt(a[0]), Integer.parseInt(a[1]) - 1, Integer.parseInt(a[2]), 12, 0, 0);
        } catch (Exception ignored) {}
        return c;
    }

    private static long dayNumber(Calendar c) {
        Calendar n = (Calendar) c.clone();
        n.set(Calendar.HOUR_OF_DAY, 12); n.set(Calendar.MINUTE, 0); n.set(Calendar.SECOND, 0); n.set(Calendar.MILLISECOND, 0);
        long local = n.getTimeInMillis() + n.get(Calendar.ZONE_OFFSET) + n.get(Calendar.DST_OFFSET);
        return Math.floorDiv(local, 86400000L);
    }
}
