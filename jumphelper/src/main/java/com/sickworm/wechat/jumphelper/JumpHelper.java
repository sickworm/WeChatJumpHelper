package com.sickworm.wechat.jumphelper;

import android.content.Context;

import com.apkfuns.logutils.LogLevel;
import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.graph.Point;

/**
 * 自动跳一跳助手 SDK 接口类
 *
 * Created by sickworm on 2017/12/30.
 */
public class JumpHelper {
    /**
     * 松手后跳跃动画等待时间
     */
    private static final int STEP_DURATION_MILL = 1000;
    private static final double DEFAULT_CORRECTION_VALUE = 1;
    private static volatile JumpHelper instance;

    private JumpController jumpController;
    private double correctionValue;
    private Thread jumpControllerThread;
    private OnStateChangedListener listener;

    static {
        LogUtils.getLogConfig().configShowBorders(false);
        // 大量 debug 日志影响性能
        LogUtils.getLogConfig().configLevel(LogLevel.TYPE_INFO);
    }

    public static JumpHelper getInstance() {
        if (instance == null) {
            synchronized (JumpHelper.class) {
                if (instance == null) {
                    instance = new JumpHelper();
                }
            }
        }
        return instance;
    }

    private JumpHelper() {
        this.correctionValue = DEFAULT_CORRECTION_VALUE;
    }

    /**
     * 设置跳一跳“距离->时间”系数修正值
     */
    @SuppressWarnings("unused")
    public void setDefaultCorrectionValue(double correctionValue) {
        this.correctionValue = correctionValue;
    }

    public void setOnStatusChangedListener(OnStateChangedListener listener) {
        this.listener = listener;
    }

    public void start(final Context context) {
        this.jumpController = new JumpController(context, correctionValue);
        jumpControllerThread = new Thread() {
            @Override
            public void run() {
                if (!jumpController.start()) {
                    onError(JumpError.NO_PERMISSION);
                    return;
                }
                if (listener != null) {
                    listener.onStart();
                }
                loop:
                while (!isInterrupted()) {
                    if (listener != null) {
                        listener.onStepStart();
                    }
                    JumpController.Result result = jumpController.next();
                    switch (result.error) {
                        case OK:
                            if (listener != null) {
                                listener.onStep(result.fromPoint, result.toPoint, result.pressTimeMill);
                            }
                            try {
                                sleep(result.pressTimeMill + STEP_DURATION_MILL);
                            } catch (InterruptedException ignore) {
                                interrupt();
                            }
                            break;
                        case INTERRUPTED:
                            interrupt();
                            break;
                        default:
                            onError(result.error);
                            break loop;
                    }
                }
                jumpController.stop();
                jumpControllerThread = null;
                if (listener != null) {
                    listener.onStop();
                }
            }
        };
        jumpControllerThread.start();
    }

    public void stop() {
        if (jumpControllerThread != null) {
            jumpControllerThread.interrupt();
        }
    }

    public boolean isRunning() {
        return jumpControllerThread != null;
    }

    private void onError(JumpError error) {
        if (listener != null) {
            listener.onError(error);
            listener.onStop();
        }
    }

    public interface OnStateChangedListener {
        void onStart();
        void onStepStart();
        void onStep(Point from, Point to, double pressTime);
        void onError(JumpError error);
        void onStop();
    }
}
