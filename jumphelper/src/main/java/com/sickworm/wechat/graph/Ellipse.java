package com.sickworm.wechat.graph;

import android.graphics.*;
import android.util.Size;

public class Ellipse extends Graph {
    public  Point center;
    public Size size;
    public float angle;

    public Ellipse(int centerX, int centerY, int width, int height, float angle) {
        this.center = new Point(centerX, centerY);
        this.size = new Size(width, height);
        this.angle = angle;
    }

    public Ellipse(Point center, Size size, float angle) {
        this.center = center;
        this.size = size;
        this.angle = angle;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        // 忽略 angle，因为很难画。JNI 传来的角度是80 ~ 100度，所以width 和 height 交换
        canvas.drawOval(center.x - size.getHeight() / 2,
                center.y - size.getWidth() / 2,
                center.x + size.getHeight() / 2,
                center.y + size.getWidth() / 2,
                paint);
    }
}