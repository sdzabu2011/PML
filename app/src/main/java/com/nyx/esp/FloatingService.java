package com.nyx.esp;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class FloatingService extends Service {
    private WindowManager windowManager;
    private View iconView;
    private View menuView;
    private ESPView espView;
    private boolean isMenuOpen = false;

    // ESP sozlamalari
    public boolean espPlayerEnabled = true;
    public boolean espLootEnabled = false;
    public boolean espBoxEnabled = true;
    public boolean espLineEnabled = true;
    public boolean espHealthEnabled = true;
    public boolean espDistanceEnabled = true;

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
        initNative();

        // ESP Overlay (shaffof, tegilmaydigan, butun ekran)
        espView = new ESPView(this, this);
        WindowManager.LayoutParams espParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        windowManager.addView(espView, espParams);

        // Dumaloq logo ikonka
        setupFloatingIcon();
    }

    // ==================== DUMALOQ DRAGGABLE LOGO ====================
    private void setupFloatingIcon() {
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.nyx_logo);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Dumaloq shaklga keltirish
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor("#00FF00"));
        circle.setStroke(4, Color.parseColor("#00CC00"));

        final WindowManager.LayoutParams iconParams = new WindowManager.LayoutParams(
                160, 160,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        iconParams.gravity = Gravity.TOP | Gravity.START;
        iconParams.x = 30;
        iconParams.y = 200;

        // DRAG (Qimirlatish) va CLICK mantiqini birga boshqarish
        icon.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = iconParams.x;
                        initialY = iconParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            moved = true;
                            iconParams.x = initialX + dx;
                            iconParams.y = initialY + dy;
                            windowManager.updateViewLayout(icon, iconParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            // Bu CLICK — menyu ochish/yopish
                            toggleMenu();
                        }
                        return true;
                }
                return false;
            }
        });

        iconView = icon;
        windowManager.addView(iconView, iconParams);
    }

    // ==================== MENYU PANELI ====================
    private void toggleMenu() {
        if (isMenuOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    private void openMenu() {
        isMenuOpen = true;

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);

        // Menyu foni (Qoramtir shaffof panel)
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(Color.parseColor("#DD111111"));
        panelBg.setCornerRadius(25);
        panelBg.setStroke(2, Color.parseColor("#00FF00"));
        panel.setBackground(panelBg);
        panel.setPadding(40, 30, 40, 30);

        // === Sarlavha ===
        TextView title = new TextView(this);
        title.setText("  N Y X   E S P  ");
        title.setTextColor(Color.parseColor("#00FF00"));
        title.setTextSize(20f);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        panel.addView(title);

        // === Ajratuvchi chiziq ===
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#33FF33"));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        panel.addView(divider);

        // === ESP PLAYER ===
        panel.addView(createToggle("ESP PLAYER", espPlayerEnabled, (on) -> {
            espPlayerEnabled = on;
            espView.isEspEnabled = on;
        }));

        // === ESP LOOT ===
        panel.addView(createToggle("ESP LOOT", espLootEnabled, (on) -> {
            espLootEnabled = on;
        }));

        // === ESP BOX ===
        panel.addView(createToggle("ESP BOX", espBoxEnabled, (on) -> {
            espBoxEnabled = on;
        }));

        // === SNAP LINE ===
        panel.addView(createToggle("SNAP LINE", espLineEnabled, (on) -> {
            espLineEnabled = on;
        }));

        // === HEALTH BAR ===
        panel.addView(createToggle("HEALTH BAR", espHealthEnabled, (on) -> {
            espHealthEnabled = on;
        }));

        // === DISTANCE ===
        panel.addView(createToggle("DISTANCE", espDistanceEnabled, (on) -> {
            espDistanceEnabled = on;
        }));

        // === YOPISH TUGMASI ===
        Button closeBtn = new Button(this);
        closeBtn.setText("✕ CLOSE");
        closeBtn.setTextColor(Color.RED);
        closeBtn.setBackgroundColor(Color.TRANSPARENT);
        closeBtn.setPadding(0, 20, 0, 0);
        closeBtn.setOnClickListener(v -> closeMenu());
        panel.addView(closeBtn);

        // Menyu joylashuvi
        WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
                550,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        menuParams.gravity = Gravity.CENTER;

        menuView = panel;
        windowManager.addView(menuView, menuParams);
    }

    private void closeMenu() {
        isMenuOpen = false;
        if (menuView != null) {
            windowManager.removeView(menuView);
            menuView = null;
        }
    }

    // ==================== TOGGLE YARATUVCHI ===========================
    private interface OnToggleChanged {
        void onChanged(boolean isOn);
    }

    private LinearLayout createToggle(String label, boolean defaultVal, OnToggleChanged callback) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 15, 0, 15);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(14f);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        // ON/OFF tugmasi
        Button toggle = new Button(this);
        toggle.setText(defaultVal ? "ON" : "OFF");
        toggle.setTextColor(defaultVal ? Color.GREEN : Color.RED);
        toggle.setTextSize(12f);
        GradientDrawable toggleBg = new GradientDrawable();
        toggleBg.setColor(Color.parseColor("#222222"));
        toggleBg.setCornerRadius(15);
        toggleBg.setStroke(1, defaultVal ? Color.GREEN : Color.RED);
        toggle.setBackground(toggleBg);
        toggle.setLayoutParams(new LinearLayout.LayoutParams(
                150, LinearLayout.LayoutParams.WRAP_CONTENT));

        final boolean[] state = {defaultVal};
        toggle.setOnClickListener(v -> {
            state[0] = !state[0];
            toggle.setText(state[0] ? "ON" : "OFF");
            toggle.setTextColor(state[0] ? Color.GREEN : Color.RED);
            GradientDrawable newBg = new GradientDrawable();
            newBg.setColor(Color.parseColor("#222222"));
            newBg.setCornerRadius(15);
            newBg.setStroke(1, state[0] ? Color.GREEN : Color.RED);
            toggle.setBackground(newBg);
            callback.onChanged(state[0]);
        });

        row.addView(toggle);
        return row;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (iconView != null) windowManager.removeView(iconView);
        if (menuView != null) windowManager.removeView(menuView);
        if (espView != null) windowManager.removeView(espView);
    }
}
