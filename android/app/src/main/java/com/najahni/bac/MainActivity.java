package com.najahni.bac;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FennoukIconPlugin.class);
        super.onCreate(savedInstanceState);

        final WebView webView = getBridge().getWebView();

        // Keep the app between the status bar and the navigation bar (Android 15+ draws edge to edge).
        ViewCompat.setOnApplyWindowInsetsListener(webView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.setMargins(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            v.setLayoutParams(lp);
            return WindowInsetsCompat.CONSUMED;
        });
        setSystemBars(false);

        // Phone back button / back gesture: let the app close its screens first.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                webView.evaluateJavascript("(window.__fennoukBack ? window.__fennoukBack() : false)", value -> {
                    if (!"true".equals(value)) {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                        setEnabled(true);
                    }
                });
            }
        });
    }

    /** Colours the area behind the status and navigation bars to match the app theme. */
    void setSystemBars(boolean dark) {
        runOnUiThread(() -> {
            View parent = (View) getBridge().getWebView().getParent();
            int bg = dark ? Color.parseColor("#090F0E") : Color.parseColor("#F3F5F4");
            if (parent != null) parent.setBackgroundColor(bg);
            getWindow().getDecorView().setBackgroundColor(bg);
            WindowInsetsControllerCompat c = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            c.setAppearanceLightStatusBars(!dark);
            c.setAppearanceLightNavigationBars(!dark);
        });
    }
}
