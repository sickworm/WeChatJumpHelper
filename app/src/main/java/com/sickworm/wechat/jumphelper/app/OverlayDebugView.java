package com.sickworm.wechat.jumphelper.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.sickworm.wechat.graph.Graph;

import java.util.ArrayList;
import java.util.List;

/**
 * 调试用显示线段的类
 *
 * Created by wang on 2018/1/13.
 */

public class OverlayDebugView extends View {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private Paint paint = new Paint();
    private List<Graph> graphs = new ArrayList<>();

    public OverlayDebugView(Context context) {
        super(context);
        init();
    }

    public OverlayDebugView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayDebugView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(getResources().getColor(R.color.red));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(getResources().getDimension(R.dimen.paint_width));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (graphs != null) {
            for (Graph g : graphs) {
                g.draw(canvas, paint);
            }
        }
    }

    void setGraphs(List<Graph> graphs) {
        this.graphs = graphs;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }
}
