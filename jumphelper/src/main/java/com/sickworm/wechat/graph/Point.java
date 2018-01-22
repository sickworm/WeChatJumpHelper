package com.sickworm.wechat.graph;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Point extends Graph {
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
    public void draw(Canvas canvas, Paint paint) {
        canvas.drawPoint(x, y, paint);
    }
}
