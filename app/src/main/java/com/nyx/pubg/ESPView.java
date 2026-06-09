package com.nyx.pubg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class ESPView extends View {
    private Paint boxPaint;
    private Paint textPaint;
    
    public boolean isEspEnabled = true;

    public ESPView(Context context) {
        super(context);
        
        // Yashil box stili
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3.0f);

        // Text stili
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30.0f);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (!isEspEnabled) return;

        // Native libdan koordinatalarni o'qib bu yerda chizamiz
        // Simulyatsiya misoli: Ekranda yashil quti va dushman masofasi
        
        // Dummy data for visual
        int x = 500;
        int y = 400;
        int width = 150;
        int height = 300;
        
        // Box chizish
        canvas.drawRect(x, y, x + width, y + height, boxPaint);
        
        // HP Line chizish
        boxPaint.setColor(Color.RED);
        canvas.drawLine(x - 10, y + height, x - 10, y, boxPaint);
        
        // Ma'lumot matni
        canvas.drawText("[Enemy] 124m", x, y - 10, textPaint);
        
        // Yashil rangga qaytarish
        boxPaint.setColor(Color.GREEN);

        // Har freymda yangilanish uchun
        invalidate();
    }
}
