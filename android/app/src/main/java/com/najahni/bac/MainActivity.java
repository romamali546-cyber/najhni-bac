package com.najahni.bac;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FennoukIconPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
