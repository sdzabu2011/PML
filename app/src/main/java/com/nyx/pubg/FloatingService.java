package com.nyx.pubg;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

public class FloatingService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private boolean isMenuOpen = false;

    // Load native lib
    static {
        System.loadLibrary("nyx_esp");
    }

    public native float[] getPlayerData();

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not bound service
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Asosiy menyu layouti
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        
        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.parseColor("#151515"));
        bgShape.setCornerRadius(30);
        bgShape.setStroke(2, Color.parseColor("#00FF00"));
        mainLayout.setBackground(bgShape);
        mainLayout.setPadding(20, 20, 20, 20);

        // Dumaloq logo vazifasini o'tovchi tugma
        Button logoBtn = new Button(this);
        logoBtn.setText("NYX");
        logoBtn.setTextColor(Color.BLACK);
        
        GradientDrawable btnShape = new GradientDrawable();
        btnShape.setShape(GradientDrawable.OVAL);
        btnShape.setColor(Color.parseColor("#00FF00"));
        logoBtn.setBackground(btnShape);
        
        // Menyu ichidagi opsiylar containeri
        LinearLayout optsLayout = new LinearLayout(this);
        optsLayout.setOrientation(LinearLayout.VERTICAL);
        optsLayout.setVisibility(View.GONE); // Default yopiq

        TextView title = new TextView(this);
        title.setText("NYX ESP MENU");
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        optsLayout.addView(title);

        Button espPlayerBtn = new Button(this);
        espPlayerBtn.setText("ESP PLAYER [OFF]");
        optsLayout.addView(espPlayerBtn);

        Button espLootBtn = new Button(this);
        espLootBtn.setText("ESP LOOT [OFF]");
        optsLayout.addView(espLootBtn);

        mainLayout.addView(logoBtn);
        mainLayout.addView(optsLayout);

        floatingView = mainLayout;

        // Window parameterlari
        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 100;

        // Logo bosilganda menyuni ochish/yopish
        logoBtn.setOnClickListener(v -> {
            if (isMenuOpen) {
                optsLayout.setVisibility(View.GONE);
                isMenuOpen = false;
            } else {
                optsLayout.setVisibility(View.VISIBLE);
                isMenuOpen = true;
            }
        });

        windowManager.addView(floatingView, params);
        
        // Overlay ESP View ni ham qo'shish
        addESPView();
    }
    
    private void addESPView() {
        ESPView espView = new ESPView(this);
        WindowManager.LayoutParams espParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        );
        windowManager.addView(espView, espParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
    }
}
