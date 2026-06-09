package com.nyx.esp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public class ESPView extends View {
    private Paint boxPaint;
    private Paint linePaint;
    private Paint textPaint;
    private Paint hpGreenPaint;
    private Paint hpRedPaint;
    private Paint hpBgPaint;
    private FloatingService service;

    public boolean isEspEnabled = true;

    public ESPView(Context context, FloatingService srv) {
        super(context);
        this.service = srv;

        // ===== Yashil ESP Box =====
        boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(Color.parseColor("#00FF00"));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3.0f);

        // ===== Snap Line (Ekran pastidan dushmanga chiziq) =====
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#00FF00"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.0f);
        linePaint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));

        // ===== Dushman nomi va masofasi =====
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28.0f);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(5.0f, 2.0f, 2.0f, Color.BLACK);

        // ===== HP Bar Yashil =====
        hpGreenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hpGreenPaint.setColor(Color.GREEN);
        hpGreenPaint.setStyle(Paint.Style.FILL);

        // ===== HP Bar Qizil =====
        hpRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hpRedPaint.setColor(Color.RED);
        hpRedPaint.setStyle(Paint.Style.FILL);

        // ===== HP Background =====
        hpBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hpBgPaint.setColor(Color.parseColor("#44000000"));
        hpBgPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isEspEnabled) {
            invalidate();
            return;
        }

        int screenW = getWidth();
        int screenH = getHeight();

        // C++ Native dan koordinatalarni olish
        float[] espData = null;
        try {
            espData = service.getESPData(screenW, screenH);
        } catch (Exception e) {
            // Native kutubxona xatolik bersa, xavfsiz o'tib ketamiz
        }

        if (espData != null && espData.length >= 5) {
            for (int i = 0; i + 4 < espData.length; i += 5) {
                float type = espData[i];       // 1 = Player, 2 = Loot
                float x = espData[i + 1];      // Ekrandagi X koordinata
                float y = espData[i + 2];      // Ekrandagi Y koordinata
                float dist = espData[i + 3];   // Masofa (metr)
                float hp = espData[i + 4];     // Sog'lik foizi (0-100)

                // Masofaga qarab box hajmini hisoblash
                float height = 15000.0f / Math.max(dist, 1.0f);
                float width = height / 2.2f;

                float left = x - (width / 2);
                float top = y - height;
                float right = x + (width / 2);
                float bottom = y;

                if (type == 1.0f && service.espPlayerEnabled) {
                    // ========== ESP BOX ==========
                    if (service.espBoxEnabled) {
                        // Burchaklari bor oddiy to'rtburchak
                        canvas.drawRect(left, top, right, bottom, boxPaint);

                        // Burchak chiziqlar (professional ko'rinish)
                        float cornerLen = width * 0.25f;
                        // Yuqori chap
                        canvas.drawLine(left, top, left + cornerLen, top, boxPaint);
                        canvas.drawLine(left, top, left, top + cornerLen, boxPaint);
                        // Yuqori o'ng
                        canvas.drawLine(right, top, right - cornerLen, top, boxPaint);
                        canvas.drawLine(right, top, right, top + cornerLen, boxPaint);
                        // Pastki chap
                        canvas.drawLine(left, bottom, left + cornerLen, bottom, boxPaint);
                        canvas.drawLine(left, bottom, left, bottom - cornerLen, boxPaint);
                        // Pastki o'ng
                        canvas.drawLine(right, bottom, right - cornerLen, bottom, boxPaint);
                        canvas.drawLine(right, bottom, right, bottom - cornerLen, boxPaint);
                    }

                    // ========== SNAP LINE (Ekran pastidan dushmanga) ==========
                    if (service.espLineEnabled) {
                        canvas.drawLine(screenW / 2.0f, screenH, x, bottom, linePaint);
                    }

                    // ========== HEALTH BAR (Chap tomonda) ==========
                    if (service.espHealthEnabled) {
                        float barWidth = 8;
                        float barLeft = left - 18;
                        // Fon (Qora shaffof)
                        canvas.drawRect(barLeft, top, barLeft + barWidth, bottom, hpBgPaint);
                        // Qizil to'liq
                        canvas.drawRect(barLeft, top, barLeft + barWidth, bottom, hpRedPaint);
                        // Yashil (HP %)
                        float hpHeight = (hp / 100.0f) * height;
                        canvas.drawRect(barLeft, bottom - hpHeight, barLeft + barWidth, bottom, hpGreenPaint);
                    }

                    // ========== DISTANCE va NOM ==========
                    if (service.espDistanceEnabled) {
                        String info = (int) dist + "m";
                        canvas.drawText(info, left, top - 12, textPaint);
                    }

                } else if (type == 2.0f && service.espLootEnabled) {
                    // ========== LOOT ESP ==========
                    Paint lootPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    lootPaint.setColor(Color.YELLOW);
                    lootPaint.setStyle(Paint.Style.STROKE);
                    lootPaint.setStrokeWidth(2.0f);
                    canvas.drawRect(left, top, right, bottom, lootPaint);

                    Paint lootText = new Paint(Paint.ANTI_ALIAS_FLAG);
                    lootText.setColor(Color.YELLOW);
                    lootText.setTextSize(22.0f);
                    lootText.setShadowLayer(3.0f, 1.0f, 1.0f, Color.BLACK);
                    canvas.drawText("LOOT " + (int) dist + "m", left, top - 8, lootText);
                }
            }
        }

        // Har kadrda yangilanish (60 FPS loop)
        invalidate();
    }
}
