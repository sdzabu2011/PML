package com.nyx.esp;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class FloatingService extends Service {
    private WindowManager windowManager;
    private View menuView;
    private ESPView espView;

    // Load cpp system
    static {
        System.loadLibrary("nyx_esp");
    }

    public native void initNative();
    public native float[] getESPData(int screenW, int screenH);

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        initNative(); // Init Memory Reader / pid fetcher

        // ESP Oynasini eng pastga (hamma narsani orqasiga yashirish) O'yinni ustida qilish
        espView = new ESPView(this, this);
        WindowManager.LayoutParams espParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        windowManager.addView(espView, espParams);

        // GUI / ImGui kabi menyu
        setupMenu();
    }

    private void setupMenu() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#151515"));
        bg.setCornerRadius(30);
        bg.setStroke(3, Color.GREEN);
        mainLayout.setBackground(bg);
        mainLayout.setPadding(30, 30, 30, 30);
        
        Button iconBtn = new Button(this);
        iconBtn.setText("NYX ESP");
        iconBtn.setBackgroundColor(Color.TRANSPARENT);
        iconBtn.setTextColor(Color.GREEN);
        
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setVisibility(View.GONE);
        
        Button btnEspPlayer = new Button(this);
        btnEspPlayer.setText("ESP PLAYER [ON]");
        btnEspPlayer.setOnClickListener(v -> {
            espView.isEspEnabled = !espView.isEspEnabled;
            btnEspPlayer.setText("ESP PLAYER [" + (espView.isEspEnabled ? "ON" : "OFF") + "]");
        });
        
        options.addView(btnEspPlayer);
        mainLayout.addView(iconBtn);
        mainLayout.addView(options);

        WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        menuParams.gravity = Gravity.TOP | Gravity.START;
        menuParams.x = 100;
        menuParams.y = 100;

        iconBtn.setOnClickListener(v -> {
            boolean isVisible = options.getVisibility() == View.VISIBLE;
            options.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });

        menuView = mainLayout;
        windowManager.addView(menuView, menuParams);
    }
}
