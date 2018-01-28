package com.sickworm.wechat.graph;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Point extends Graph {
    private static final Paint DEFAULT_PAINT;

    static {
        DEFAULT_PAINT = new Paint();
        DEFAULT_PAINT.setColor(0xFFFF0000);
        DEFAULT_PAINT.setStyle(Paint.Style.STROKE);
        DEFAULT_PAINT.setStrokeCap(Paint.Cap.ROUND);
        DEFAULT_PAINT.setStrokeWidth(16);
    }

    public int x;
    public int y;
    /**
     * 用于区分点类型，暂时没用用上
     */
    public int type;

    public Point(int x, int y) {
        this(x, y, 0);
    }

    public Point(int x, int y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Point)) {
            return false;
        }
        Point point = (Point) obj;
        return x == point.x && y == point.y;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPoint(x, y, DEFAULT_PAINT);
    }
}
