package com.sickworm.wechat.graph;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * 调试用图形类
 *
 * Created by wang on 2018/1/14.
 */
public abstract class Graph {
    public static final int TYPE_RED = 0xFFFF0000;
    public static final int TYPE_GREEN = 0xFF00FF00;
    public static final int TYPE_BLUE = 0xFF0000FF;

    public abstract void draw(Canvas canvas);
}
