package fr.magiclockscreen.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

public class PeekOverlayView extends View {
    private Bitmap image;
    private String previewText = "Exemple";
    private float x = 0.12f, y = 0.70f, w = 0.76f, h = 0.14f;
    private int textSize = 46;
    private int textColor = Color.WHITE;
    private int opacity = 100;
    private boolean bold = true;
    private boolean italic = false;
    private String align = "center";
    private boolean shadow = true;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF imageRect = new RectF();
    private RectF boxRect = new RectF();
    private int mode = 0;
    private float lastX, lastY;

    public PeekOverlayView(Context context) {
        super(context);
        setMinimumHeight(dp(260));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(dp(2));
        boxPaint.setColor(Color.rgb(34, 211, 238));
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setColor(Color.rgb(139, 92, 246));
    }

    public void setImage(Bitmap image) { this.image = image; invalidate(); }
    public void setPreviewText(String text) { this.previewText = text == null || text.trim().isEmpty() ? "Exemple" : text.trim(); invalidate(); }
    public void setBox(float x, float y, float w, float h) { this.x = x; this.y = y; this.w = w; this.h = h; invalidate(); }
    public void setStyle(int textSize, int textColor, int opacity, boolean bold, boolean italic, String align, boolean shadow) {
        this.textSize = textSize; this.textColor = textColor; this.opacity = opacity; this.bold = bold; this.italic = italic;
        this.align = align == null ? "center" : align; this.shadow = shadow; invalidate();
    }
    public float getBoxX() { return x; }
    public float getBoxY() { return y; }
    public float getBoxW() { return w; }
    public float getBoxH() { return h; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.rgb(11, 18, 38));
        computeImageRect();
        if (image != null) canvas.drawBitmap(image, null, imageRect, null);
        else {
            paint.setColor(Color.rgb(30, 41, 59)); paint.setStyle(Paint.Style.FILL); canvas.drawRoundRect(imageRect, dp(18), dp(18), paint);
            paint.setColor(Color.rgb(203, 213, 225)); paint.setTextSize(dp(15)); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(Typeface.DEFAULT_BOLD); paint.clearShadowLayer();
            canvas.drawText("Choisis une image Peek", imageRect.centerX(), imageRect.centerY(), paint);
        }
        computeBoxRect();
        drawPreviewText(canvas, boxRect, previewText);
        canvas.drawRoundRect(boxRect, dp(8), dp(8), boxPaint);
        float r = dp(8);
        canvas.drawCircle(boxRect.right, boxRect.bottom, r, handlePaint);
        canvas.drawCircle(boxRect.left, boxRect.top, r, handlePaint);
    }

    private void computeImageRect() {
        float viewW = getWidth();
        float viewH = getHeight();
        if (image == null) { imageRect.set(0, 0, viewW, viewH); return; }
        float imgRatio = image.getWidth() / (float) image.getHeight();
        float viewRatio = viewW / Math.max(1f, viewH);
        if (imgRatio > viewRatio) {
            float h = viewW / imgRatio;
            float top = (viewH - h) / 2f;
            imageRect.set(0, top, viewW, top + h);
        } else {
            float w2 = viewH * imgRatio;
            float left = (viewW - w2) / 2f;
            imageRect.set(left, 0, left + w2, viewH);
        }
    }

    private void computeBoxRect() {
        boxRect.set(imageRect.left + x * imageRect.width(), imageRect.top + y * imageRect.height(),
                imageRect.left + (x + w) * imageRect.width(), imageRect.top + (y + h) * imageRect.height());
    }

    private void drawPreviewText(Canvas canvas, RectF rect, String text) {
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 2.55f)));
        paint.reset(); paint.setAntiAlias(true); paint.setColor((textColor & 0x00FFFFFF) | (alpha << 24));
        paint.setTextSize(dp(textSize));
        int style = bold && italic ? Typeface.BOLD_ITALIC : bold ? Typeface.BOLD : italic ? Typeface.ITALIC : Typeface.NORMAL;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, style));
        if ("left".equalsIgnoreCase(align)) paint.setTextAlign(Paint.Align.LEFT);
        else if ("right".equalsIgnoreCase(align)) paint.setTextAlign(Paint.Align.RIGHT);
        else paint.setTextAlign(Paint.Align.CENTER);
        if (shadow) paint.setShadowLayer(dp(4), dp(2), dp(2), Color.argb(alpha, 0, 0, 0));
        Paint.FontMetrics fm = paint.getFontMetrics();
        float tx = paint.getTextAlign() == Paint.Align.LEFT ? rect.left + dp(8) : paint.getTextAlign() == Paint.Align.RIGHT ? rect.right - dp(8) : rect.centerX();
        float ty = rect.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.save(); canvas.clipRect(rect); canvas.drawText(text, tx, ty, paint); canvas.restore();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        computeImageRect(); computeBoxRect();
        float ex = event.getX(), ey = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = ex; lastY = ey;
                float d = dp(28);
                if (Math.abs(ex - boxRect.right) < d && Math.abs(ey - boxRect.bottom) < d) mode = 2;
                else if (boxRect.contains(ex, ey)) mode = 1;
                else { mode = 1; moveCenterTo(ex, ey); }
                invalidate(); return true;
            case MotionEvent.ACTION_MOVE:
                float dx = (ex - lastX) / Math.max(1f, imageRect.width());
                float dy = (ey - lastY) / Math.max(1f, imageRect.height());
                if (mode == 2) { w = clamp(w + dx, 0.08f, 1f - x); h = clamp(h + dy, 0.04f, 1f - y); }
                else if (mode == 1) { x = clamp(x + dx, 0f, 1f - w); y = clamp(y + dy, 0f, 1f - h); }
                lastX = ex; lastY = ey; invalidate(); return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mode = 0; return true;
        }
        return true;
    }

    private void moveCenterTo(float px, float py) {
        x = clamp((px - imageRect.left) / Math.max(1f, imageRect.width()) - w / 2f, 0f, 1f - w);
        y = clamp((py - imageRect.top) / Math.max(1f, imageRect.height()) - h / 2f, 0f, 1f - h);
    }
    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
