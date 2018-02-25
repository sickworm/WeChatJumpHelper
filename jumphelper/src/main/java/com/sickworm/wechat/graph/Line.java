package com.sickworm.wechat.graph;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Line extends Graph {
    private static final Paint DEFAULT_PAINT;

    static {
        DEFAULT_PAINT = new Paint();
        DEFAULT_PAINT.setColor(0xFFFF0000);
        DEFAULT_PAINT.setStyle(Paint.Style.STROKE);
        DEFAULT_PAINT.setStrokeCap(Paint.Cap.ROUND);
        DEFAULT_PAINT.setStrokeWidth(8);
    }

    public Point start;
    public Point end;
    public int type;

    public Line(int startX, int startY, int endX, int endY) {
        start = new Point(startX, startY);
        end = new Point(endX, endY);
        type = Graph.TYPE_RED;
    }

    @Override
    public void draw(Canvas canvas) {
        DEFAULT_PAINT.setColor(type);
        canvas.drawLine(start.x, start.y, end.x, end.y, DEFAULT_PAINT);
    }
}
