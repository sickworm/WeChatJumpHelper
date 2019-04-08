package com.sickworm.wechat.jumphelper.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.sickworm.wechat.graph.OverlayDebugView;
import com.sickworm.wechat.jumphelper.JumpHelper;

/**
 * 悬浮窗按钮
 *
 * Created by sickworm on 2017/12/31.
 */
@SuppressWarnings("FieldCanBeLocal")
public class FloatingView extends FrameLayout {
    private static final int ALIGN_X_DP = 48;
    private static final int START_Y_DP = 180;
    private View mFloatingView;
    private WindowManager.LayoutParams params;
    private FloatingManager floatingManager;
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

        floatingManager = FloatingManager.getInstance(context);
    }

    public void show() {
        WindowManager.LayoutParams debugViewParams = new WindowManager.LayoutParams();
        debugViewParams.gravity = Gravity.TOP | Gravity.START;
        debugViewParams.x = 0;
        debugViewParams.y = 0;
        //总是出现在应用程序窗口之上
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            debugViewParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            debugViewParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        } else {
            debugViewParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        //设置图片格式，效果为背景透明
        debugViewParams.format = PixelFormat.RGBA_8888;
        debugViewParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        debugViewParams.width = LayoutParams.MATCH_PARENT;
        debugViewParams.height = LayoutParams.MATCH_PARENT;
        floatingManager.addView(OverlayDebugView.init(getContext()), debugViewParams);

        params = new WindowManager.LayoutParams();

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = startYPx;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        params.format = PixelFormat.RGBA_8888;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        params.width = LayoutParams.WRAP_CONTENT;
        params.height = LayoutParams.WRAP_CONTENT;
        floatingManager.addView(mFloatingView, params);

        if (BuildConfig.QUICK_TEST) {
            JumpHelper.getInstance().start(getContext());
        }
    }

    public void hide() {
        floatingManager.removeView(mFloatingView);
        floatingManager.removeView(OverlayDebugView.init(getContext()));
        OverlayDebugView.release();
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
                    mOriginX = params.x;
                    mOriginY = params.y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (event.getRawX() == mTouchStartX && event.getRawY() == mTouchStartY) {
                        break;
                    }
                    moved = true;
                    params.x = mOriginX - mTouchStartX + (int) event.getRawX();
                    params.y = mOriginY - mTouchStartY + (int) event.getRawY();
                    if (params.x <= minXPx) {
                        params.x = 0;
                    }
                    floatingManager.updateView(mFloatingView, params);
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
