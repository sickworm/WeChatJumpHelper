package com.sickworm.wechat.graph;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Locale;

public class Point extends Graph {
    private static final Paint DEFAULT_PAINT;

    static {
        DEFAULT_PAINT = new Paint();
        DEFAULT_PAINT.setColor(Graph.TYPE_RED);
        DEFAULT_PAINT.setStyle(Paint.Style.STROKE);
        DEFAULT_PAINT.setStrokeCap(Paint.Cap.ROUND);
        DEFAULT_PAINT.setStrokeWidth(16);
    }

    public int x;
    public int y;
    /**
     * 用于区分点类型，值为颜色
     */
    public int type;

    public Point(int x, int y) {
        this(x, y, Graph.TYPE_RED);
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
        DEFAULT_PAINT.setColor(type);
        canvas.drawPoint(x, y, DEFAULT_PAINT);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "(% 4d, % 4d)", x, y);
    }
}
