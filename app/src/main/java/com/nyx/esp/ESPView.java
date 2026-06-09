package com.nyx.esp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class ESPView extends View {
    private Paint playerBoxPaint;
    private Paint playerTextPaint;
    private Paint hpPaint;
    private FloatingService service;

    public boolean isEspEnabled = true;

    public ESPView(Context context, FloatingService srv) {
        super(context);
        this.service = srv;

        // Player Box UI (Agressiv Yashil)
        playerBoxPaint = new Paint();
        playerBoxPaint.setColor(Color.parseColor("#00FF00"));
        playerBoxPaint.setStyle(Paint.Style.STROKE);
        playerBoxPaint.setStrokeWidth(3.0f);

        // HP Stroke UI
        hpPaint = new Paint();
        hpPaint.setStyle(Paint.Style.FILL);

        // Distance Text
        playerTextPaint = new Paint();
        playerTextPaint.setColor(Color.WHITE);
        playerTextPaint.setTextSize(25.0f);
        playerTextPaint.setFakeBoldText(true);
        playerTextPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isEspEnabled) return;

        // Ekranni tozalab chizish va ma'lumotlarni o'qish (C++ layerni chaqirish)
        float[] espData = service.getESPData(getWidth(), getHeight());
        
        if (espData != null && espData.length >= 5) {
            for (int i = 0; i < espData.length; i += 5) {
                float type = espData[i];
                float x = espData[i + 1];
                float y = espData[i + 2];
                float dist = espData[i + 3];
                float hp = espData[i + 4];

                if (type == 1.0f) { // Player
                    // Ekranga chizish mantiqi - Dushman karobkasi
                    float height = 15000 / dist; // Uzoklikka qarab height
                    float width = height / 2;
                    
                    float left = x - (width / 2);
                    float top = y - height;
                    float right = x + (width / 2);
                    float bottom = y;

                    // Box
                    canvas.drawRect(left, top, right, bottom, playerBoxPaint);
                    
                    // HP Bar
                    hpPaint.setColor(Color.RED);
                    canvas.drawRect(left - 15, bottom - height, left - 5, bottom, hpPaint);
                    hpPaint.setColor(Color.GREEN);
                    float hpHeight = (hp / 100.0f) * height;
                    canvas.drawRect(left - 15, bottom - hpHeight, left - 5, bottom, hpPaint);

                    // Name va Distance markerni chizish
                    canvas.drawText((int)dist + "m", left, top - 10, playerTextPaint);
                }
            }
        }
        
        // Har doim ekran o'zini yangilab turishi uchun FPS
        invalidate();
    }
}
