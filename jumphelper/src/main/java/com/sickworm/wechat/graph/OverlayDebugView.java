package com.sickworm.wechat.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.graph.Graph;
import com.sickworm.wechat.graph.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * 调试用显示线段的类
 *
 * Created by wang on 2018/1/13.
 */

public class OverlayDebugView extends View {
    private static volatile OverlayDebugView instance;
    private List<Graph> graphs = new ArrayList<>();

    /**
     * 使用该方法方便 SDK 层设置调试内容
     */
    public static OverlayDebugView init(Context context) {
        if (instance == null) {
            synchronized (OverlayDebugView.class) {
                if (instance == null) {
                    instance = new OverlayDebugView(context);
                }
            }
        }
        return instance;
    }

    /**
     * 不用时清空，防止内存泄露
     */
    public static void release() {
        instance = null;
    }

    public static OverlayDebugView getInstance() {
        return instance;
    }

    private OverlayDebugView(Context context) {
        super(context);
        init();
    }

    private void init() {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (graphs != null) {
            for (Graph g : graphs) {
                g.draw(canvas);
            }
        }
    }

    public void setGraphs(List<Graph> graphs) {
        this.graphs = graphs;
        postInvalidate();
    }
}
