package com.sickworm.wechat.graph;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Line extends Graph {
    public Point start;
    public Point end;

    public Line(int startX, int startY, int endX, int endY) {
        start = new Point(startX, startY);
        end = new Point(endX, endY);
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    }
}
