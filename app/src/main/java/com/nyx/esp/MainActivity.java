package com.nyx.esp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nyx.esp.virtual.VirtualCore;

public class MainActivity extends Activity {
    private VirtualCore virtualCore;
    private TextView statusText;
    private TextView detailText;
    private Button launchBtn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        virtualCore = new VirtualCore(this);

        // =============== LAUNCHER UI ===============
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#0A0A0A"));
        root.setPadding(60, 100, 60, 60);

        // Logo matn
        TextView logo = new TextView(this);
        logo.setText("NYX ESP");
        logo.setTextColor(Color.parseColor("#00FF00"));
        logo.setTextSize(42f);
        logo.setGravity(Gravity.CENTER);
        logo.setPadding(0, 0, 0, 10);
        root.addView(logo);

        // Alt sarlavha
        TextView sub = new TextView(this);
        sub.setText("PUBG MOBILE TACTICAL LAUNCHER");
        sub.setTextColor(Color.parseColor("#666666"));
        sub.setTextSize(12f);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, 60);
        root.addView(sub);

        // Ajratuvchi chiziq
        View div1 = new View(this);
        div1.setBackgroundColor(Color.parseColor("#1A1A1A"));
        div1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        root.addView(div1);

        // Status paneli
        LinearLayout statusPanel = new LinearLayout(this);
        statusPanel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(Color.parseColor("#111111"));
        panelBg.setCornerRadius(20);
        panelBg.setStroke(1, Color.parseColor("#222222"));
        statusPanel.setBackground(panelBg);
        statusPanel.setPadding(40, 30, 40, 30);
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        panelParams.setMargins(0, 30, 0, 30);
        statusPanel.setLayoutParams(panelParams);

        statusText = new TextView(this);
        statusText.setText("⏳ PUBG Mobile qidirilmoqda...");
        statusText.setTextColor(Color.YELLOW);
        statusText.setTextSize(16f);
        statusText.setPadding(0, 0, 0, 10);
        statusPanel.addView(statusText);

        detailText = new TextView(this);
        detailText.setText("");
        detailText.setTextColor(Color.parseColor("#888888"));
        detailText.setTextSize(12f);
        statusPanel.addView(detailText);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 20, 0, 0);
        statusPanel.addView(progressBar);

        root.addView(statusPanel);

        // Launch button
        launchBtn = new Button(this);
        launchBtn.setText("▶  LAUNCH ESP");
        launchBtn.setTextColor(Color.BLACK);
        launchBtn.setTextSize(18f);
        launchBtn.setEnabled(false);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#00FF00"));
        btnBg.setCornerRadius(15);
        launchBtn.setBackground(btnBg);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140);
        btnParams.setMargins(0, 20, 0, 0);
        launchBtn.setLayoutParams(btnParams);

        launchBtn.setOnClickListener(v -> {
            startESP();
        });
        root.addView(launchBtn);

        // Info labels
        String[] infoLines = {
            "• ROOT talab qilinmaydi",
            "• Virtual Space texnologiyasi",
            "• ESP Player / Loot / Box / Line",
            "• Barcha PUBG versiyalari qo'llab-quvvatlanadi"
        };

        LinearLayout infoPanel = new LinearLayout(this);
        infoPanel.setOrientation(LinearLayout.VERTICAL);
        infoPanel.setPadding(0, 40, 0, 0);
        for (String line : infoLines) {
            TextView tv = new TextView(this);
            tv.setText(line);
            tv.setTextColor(Color.parseColor("#444444"));
            tv.setTextSize(11f);
            tv.setPadding(0, 5, 0, 5);
            infoPanel.addView(tv);
        }
        root.addView(infoPanel);

        setContentView(root);

        // Avtomatik PUBG skanerlash
        new Handler(Looper.getMainLooper()).postDelayed(this::scanForPubg, 1000);
    }

    private void scanForPubg() {
        boolean found = virtualCore.detectPubg();

        if (found) {
            statusText.setText("✅ PUBG TOPILDI");
            statusText.setTextColor(Color.GREEN);
            detailText.setText(
                "Package: " + virtualCore.getDetectedPackage() + "\n" +
                "APK: " + virtualCore.getApkPath() + "\n" +
                "Libs: " + virtualCore.getNativeLibPath()
            );
            progressBar.setVisibility(View.GONE);

            // Virtual muhitni tayyorlash
            virtualCore.prepareVirtualEnv();

            launchBtn.setEnabled(true);
        } else {
            statusText.setText("❌ PUBG TOPILMADI");
            statusText.setTextColor(Color.RED);
            detailText.setText(
                "Qo'llab-quvvatlanuvchi versiyalar:\n" +
                "• com.tencent.ig (Global)\n" +
                "• com.pubg.krmobile (Korea)\n" +
                "• com.vng.pubgmobile (Vietnam)\n" +
                "• com.pubg.imobile (BGMI)\n\n" +
                "Iltimos, PUBG o'rnating va qayta urinib ko'ring."
            );
            progressBar.setVisibility(View.GONE);

            // Baribir ESP ni ishga tushirishga ruxsat berish (manual mode)
            launchBtn.setEnabled(true);
            launchBtn.setText("▶  LAUNCH ESP (MANUAL)");
        }
    }

    private void startESP() {
        // Overlay ruxsatni tekshirish
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 100);
            return;
        }

        // ESP servisini ishga tushirish
        Intent svc = new Intent(this, FloatingService.class);
        startService(svc);
        Toast.makeText(this, "NYX ESP ishga tushdi!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startESP();
            } else {
                Toast.makeText(this, "Overlay ruxsati kerak!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
