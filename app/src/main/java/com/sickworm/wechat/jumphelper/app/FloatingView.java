package com.sickworm.wechat.jumphelper.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.sickworm.wechat.graph.Ellipse;
import com.sickworm.wechat.graph.Line;
import com.sickworm.wechat.graph.Point;

/**
 * 悬浮窗按钮
 *
 * Created by sickworm on 2017/12/31.
 */
@SuppressWarnings("FieldCanBeLocal")
public class FloatingView extends FrameLayout {
    private static final boolean OPEN_DEBUG_VIEW = true;
    private static final int ALIGN_X_DP = 48;
    private static final int START_Y_DP = 180;
    private View mFloatingView;
    private OverlayDebugView mOverlayDebugView;
    private WindowManager.LayoutParams mParams;
    private FloatingManager mFloatingManager;
    private int minXPx;
    private int startYPx;

    @SuppressLint({"ClickableViewAccessibility", "InflateParams"})
    public FloatingView(Context context) {
        super(context);
        LayoutInflater mLayoutInflater = LayoutInflater.from(context);
        mFloatingView = mLayoutInflater.inflate(R.layout.view_floating, null);
        mFloatingView.setOnTouchListener(mOnTouchListener);
        mFloatingView.setOnClickListener(mOnClickListener);
        minXPx = (int) (ALIGN_X_DP * context.getResources().getDisplayMetrics().density);
        startYPx = (int) (START_Y_DP * context.getResources().getDisplayMetrics().density);
        mOverlayDebugView = new OverlayDebugView(context);

        mFloatingManager = FloatingManager.getInstance(context);
    }

    public void show() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;
        //总是出现在应用程序窗口之上
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //设置图片格式，效果为背景透明
        params.format = PixelFormat.RGBA_8888;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.MATCH_PARENT;
        if (OPEN_DEBUG_VIEW) {
            mFloatingManager.addView(mOverlayDebugView, params);
        }

        mParams = new WindowManager.LayoutParams();

        mParams.gravity = Gravity.TOP | Gravity.START;
        mParams.x = 0;
        mParams.y = startYPx;
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        mParams.width = LayoutParams.WRAP_CONTENT;
        mParams.height = LayoutParams.WRAP_CONTENT;
        mFloatingManager.addView(mFloatingView, mParams);
    }

    public void hide() {
        mFloatingManager.removeView(mFloatingView);
        if (OPEN_DEBUG_VIEW) {
            mFloatingManager.removeView(mOverlayDebugView);
        }
    }

    public void addDebugLine(Line line) {
        mOverlayDebugView.addLine(line);
    }

    public void addDebugEllipse(Ellipse ellipse) {
        mOverlayDebugView.addEllipse(ellipse);
    }

    public void addDebugPoint(Point point) {
        mOverlayDebugView.addPoint(point);
    }

    public void clearDebugGraphs() {
        mOverlayDebugView.clearGraphs();
    }

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        private int mTouchStartX, mTouchStartY;
        private int mOriginX, mOriginY;
        private boolean moved;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    moved = false;
                    mTouchStartX = (int) event.getRawX();
                    mTouchStartY = (int) event.getRawY();
                    mOriginX = mParams.x;
                    mOriginY = mParams.y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    moved = true;
                    mParams.x = mOriginX - mTouchStartX + (int) event.getRawX();
                    mParams.y = mOriginY - mTouchStartY + (int) event.getRawY();
                    if (mParams.x <= minXPx) {
                        mParams.x = 0;
                    }
                    mFloatingManager.updateView(mFloatingView, mParams);
                    break;
                case MotionEvent.ACTION_UP:
                    if (!moved) {
                        performClick();
                    }
                    return moved;
            }
            return false;
        }
    };

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(FloatingView.this.getContext(), FloatingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            FloatingView.this.getContext().startActivity(intent);
        }
    };
}
