package fr.magiclockscreen.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

public class PeekOverlayView extends View {
    public interface ChangeListener { void onPeekChanged(PeekOverlayView view); }

    private Bitmap image;
    private String previewText = "Exemple";
    private float x = 0.12f, y = 0.70f, w = 0.76f, h = 0.14f;
    private float rotation = 0f;
    private int textSize = 46;
    private int textColor = Color.WHITE;
    private int opacity = 100;
    private boolean bold = true;
    private boolean italic = false;
    private String align = "center";
    private boolean shadow = true;
    private boolean selected = true;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rotatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF imageRect = new RectF();
    private RectF boxRect = new RectF();
    private RectF touchRect = new RectF();
    private int mode = 0;
    private float lastX, lastY;
    private float startTouchX, startTouchY;
    private float startX, startY, startW, startH, startRotation;
    private ChangeListener changeListener;

    private static final int MODE_NONE = 0;
    private static final int MODE_MOVE = 1;
    private static final int MODE_RESIZE_BR = 2;
    private static final int MODE_RESIZE_BL = 3;
    private static final int MODE_RESIZE_TR = 4;
    private static final int MODE_RESIZE_TL = 5;
    private static final int MODE_ROTATE = 6;

    public PeekOverlayView(Context context) {
        super(context);
        setMinimumHeight(dp(260));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(dp(2));
        boxPaint.setColor(Color.rgb(34, 211, 238));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(28, 34, 211, 238));
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setColor(Color.rgb(139, 92, 246));
        rotatePaint.setStyle(Paint.Style.FILL);
        rotatePaint.setColor(Color.rgb(34, 211, 238));
    }

    public void setChangeListener(ChangeListener listener) { this.changeListener = listener; }
    public void setImage(Bitmap image) { this.image = image; invalidate(); }
    public void setPreviewText(String text) { this.previewText = text == null || text.trim().isEmpty() ? "Exemple" : text.trim(); invalidate(); }
    public void setBox(float x, float y, float w, float h) { this.x = x; this.y = y; this.w = w; this.h = h; invalidate(); }
    public void setRotation(float rotation) { this.rotation = normalizeAngle(rotation); invalidate(); }
    public void setStyle(int textSize, int textColor, int opacity, boolean bold, boolean italic, String align, boolean shadow) {
        this.textSize = textSize; this.textColor = textColor; this.opacity = opacity; this.bold = bold; this.italic = italic;
        this.align = align == null ? "center" : align; this.shadow = shadow; invalidate();
    }
    public float getBoxX() { return x; }
    public float getBoxY() { return y; }
    public float getBoxW() { return w; }
    public float getBoxH() { return h; }
    public float getRotation() { return rotation; }

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
        canvas.save();
        canvas.rotate(rotation, boxRect.centerX(), boxRect.centerY());
        drawPreviewText(canvas, boxRect, previewText);
        if (selected) drawEditor(canvas);
        canvas.restore();
    }

    private void drawEditor(Canvas canvas) {
        canvas.drawRoundRect(boxRect, dp(8), dp(8), fillPaint);
        canvas.drawRoundRect(boxRect, dp(8), dp(8), boxPaint);
        float r = dp(9);
        canvas.drawCircle(boxRect.left, boxRect.top, r, handlePaint);
        canvas.drawCircle(boxRect.right, boxRect.top, r, handlePaint);
        canvas.drawCircle(boxRect.left, boxRect.bottom, r, handlePaint);
        canvas.drawCircle(boxRect.right, boxRect.bottom, r, handlePaint);
        float cx = boxRect.centerX();
        float cy = boxRect.top - dp(34);
        canvas.drawLine(cx, boxRect.top, cx, cy, boxPaint);
        canvas.drawCircle(cx, cy, dp(10), rotatePaint);
        paint.reset(); paint.setAntiAlias(true); paint.setColor(Color.WHITE); paint.setTextSize(dp(11)); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("↻", cx, cy + dp(4), paint);
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
                selected = true;
                lastX = ex; lastY = ey; startTouchX = ex; startTouchY = ey;
                startX = x; startY = y; startW = w; startH = h; startRotation = rotation;
                mode = hitMode(ex, ey);
                if (mode == MODE_NONE) {
                    moveCenterTo(ex, ey);
                    mode = MODE_MOVE;
                    notifyChanged();
                }
                invalidate(); return true;
            case MotionEvent.ACTION_MOVE:
                if (mode == MODE_ROTATE) rotateTo(ex, ey);
                else if (mode == MODE_MOVE) moveBy(ex - lastX, ey - lastY);
                else if (mode != MODE_NONE) resizeFromStart(ex, ey);
                lastX = ex; lastY = ey;
                notifyChanged(); invalidate(); return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (Math.abs(ex - startTouchX) < dp(4) && Math.abs(ey - startTouchY) < dp(4) && mode == MODE_MOVE) {
                    // Simple tap on the image: place and keep the text zone selected/fixed there.
                    moveCenterTo(ex, ey);
                    notifyChanged(); invalidate();
                }
                mode = MODE_NONE; return true;
        }
        return true;
    }

    private int hitMode(float px, float py) {
        float[] local = toLocal(px, py);
        float lx = local[0], ly = local[1];
        float d = dp(30);
        float rotateX = boxRect.centerX();
        float rotateY = boxRect.top - dp(34);
        if (distance(lx, ly, rotateX, rotateY) <= d) return MODE_ROTATE;
        if (distance(lx, ly, boxRect.right, boxRect.bottom) <= d) return MODE_RESIZE_BR;
        if (distance(lx, ly, boxRect.left, boxRect.bottom) <= d) return MODE_RESIZE_BL;
        if (distance(lx, ly, boxRect.right, boxRect.top) <= d) return MODE_RESIZE_TR;
        if (distance(lx, ly, boxRect.left, boxRect.top) <= d) return MODE_RESIZE_TL;
        touchRect.set(boxRect); touchRect.inset(-dp(14), -dp(14));
        if (touchRect.contains(lx, ly)) return MODE_MOVE;
        return MODE_NONE;
    }

    private float[] toLocal(float px, float py) {
        double a = Math.toRadians(-rotation);
        float cx = boxRect.centerX();
        float cy = boxRect.centerY();
        float dx = px - cx;
        float dy = py - cy;
        return new float[]{(float)(dx * Math.cos(a) - dy * Math.sin(a) + cx), (float)(dx * Math.sin(a) + dy * Math.cos(a) + cy)};
    }

    private void moveBy(float dxPx, float dyPx) {
        x = clamp(x + dxPx / Math.max(1f, imageRect.width()), 0f, 1f - w);
        y = clamp(y + dyPx / Math.max(1f, imageRect.height()), 0f, 1f - h);
    }

    private void resizeFromStart(float px, float py) {
        float[] local = toLocal(px, py);
        float lx = local[0], ly = local[1];
        float nx = startX, ny = startY, nw = startW, nh = startH;
        float relX = (lx - imageRect.left) / Math.max(1f, imageRect.width());
        float relY = (ly - imageRect.top) / Math.max(1f, imageRect.height());
        if (mode == MODE_RESIZE_BR || mode == MODE_RESIZE_TR) nw = clamp(relX - startX, 0.08f, 1f - startX);
        if (mode == MODE_RESIZE_BL || mode == MODE_RESIZE_TL) { nx = clamp(relX, 0f, startX + startW - 0.08f); nw = clamp(startX + startW - nx, 0.08f, 1f - nx); }
        if (mode == MODE_RESIZE_BR || mode == MODE_RESIZE_BL) nh = clamp(relY - startY, 0.04f, 1f - startY);
        if (mode == MODE_RESIZE_TR || mode == MODE_RESIZE_TL) { ny = clamp(relY, 0f, startY + startH - 0.04f); nh = clamp(startY + startH - ny, 0.04f, 1f - ny); }
        x = nx; y = ny; w = nw; h = nh;
    }

    private void rotateTo(float px, float py) {
        float cx = boxRect.centerX();
        float cy = boxRect.centerY();
        float startAngle = (float) Math.toDegrees(Math.atan2(startTouchY - cy, startTouchX - cx));
        float currentAngle = (float) Math.toDegrees(Math.atan2(py - cy, px - cx));
        rotation = normalizeAngle(startRotation + currentAngle - startAngle);
    }

    private void moveCenterTo(float px, float py) {
        x = clamp((px - imageRect.left) / Math.max(1f, imageRect.width()) - w / 2f, 0f, 1f - w);
        y = clamp((py - imageRect.top) / Math.max(1f, imageRect.height()) - h / 2f, 0f, 1f - h);
    }
    private float distance(float x1, float y1, float x2, float y2) { float dx = x1 - x2; float dy = y1 - y2; return (float)Math.sqrt(dx * dx + dy * dy); }
    private float normalizeAngle(float value) { while (value > 180f) value -= 360f; while (value < -180f) value += 360f; return value; }
    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private void notifyChanged() { if (changeListener != null) changeListener.onPeekChanged(this); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
