package com.sickworm.wechat.graph;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Rect extends Graph {
    private static final Paint DEFAULT_PAINT;

    static {
        DEFAULT_PAINT = new Paint();
        DEFAULT_PAINT.setColor(0xFFFF0000);
        DEFAULT_PAINT.setStyle(Paint.Style.STROKE);
        DEFAULT_PAINT.setStrokeCap(Paint.Cap.ROUND);
        DEFAULT_PAINT.setStrokeWidth(8);
    }

    public Point origin;
    public Size size;

    public Rect(int originX, int originY, int width, int height) {
        this.origin = new Point(originX, originY);
        this.size = new Size(width, height);
    }

    public Rect(Point origin, Size size) {
        this.origin = origin;
        this.size = size;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(origin.x, origin.y,
                origin.x + size.width, origin.y + size.height,
                DEFAULT_PAINT);
    }
}
