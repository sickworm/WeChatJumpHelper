package com.sickworm.wechat.jumphelper.app;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;

import com.apkfuns.logutils.LogUtils;

/**
 * 悬浮窗管理类
 *
 * Created by sickworm on 2017/12/30.
 */
class FloatingManager {
    private static FloatingManager mInstance;
    private WindowManager mWindowManager;

    static FloatingManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FloatingManager(context);
        }
        return mInstance;
    }

    private FloatingManager(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * 添加悬浮窗
     */
    void addView(View view, WindowManager.LayoutParams params) {
        try {
            mWindowManager.addView(view, params);
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }

    /**
     * 移除悬浮窗
     */
    void removeView(View view) {
        try {
            mWindowManager.removeView(view);
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }

    /**
     * 更新悬浮窗参数
     */
    void updateView(View view, WindowManager.LayoutParams params) {
        try {
            mWindowManager.updateViewLayout(view, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}