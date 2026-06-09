package com.nyx.pubg;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int SYSTEM_ALERT_WINDOW_PERMISSION = 2084;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Android 6.0 va undan yuqorilar uchun Overlay ruxsatini sorash
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            askPermission();
        } else {
            startOverlayService();
        }
    }

    private void askPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SYSTEM_ALERT_WINDOW_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startOverlayService();
            } else {
                Toast.makeText(this, "ESP ishlashi uchun Overlay (Draw over apps) ruxsati kerak!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startOverlayService() {
        Intent serviceIntent = new Intent(this, FloatingService.class);
        startService(serviceIntent);
        Toast.makeText(this, "NYX ESP ishga tushdi...", Toast.LENGTH_SHORT).show();
        finish(); // Ilovani orqa fonga o'tkazamiz
    }
}
