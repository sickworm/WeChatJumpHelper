package com.sickworm.wechat.jumphelper;

import android.content.Context;

import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.graph.Graph;
import com.sickworm.wechat.graph.Point;

import java.util.List;

/**
 * 跳一跳 SDK 接口类
 *
 * Created by sickworm on 2017/12/30.
 */
public class JumpHelper {
    private static final int STEP_DURATION_MILL = 2000;
    private static final double DEFAULT_CORRECTION_VALUE = 1;
    private static volatile JumpHelper instance;

    private JumpController jumpController;
    private double correctionValue;
    private Thread jumpControllerThread;
    private OnStateChangedListener listener;

    static {
        LogUtils.getLogConfig().configShowBorders(false);
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

    public void setDetaultCorrectionValue(double correctionValue) {
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
                if (!doStart()) {
                    return;
                }
                loop:
                while (!isInterrupted()) {
                    if (listener != null) {
                        listener.onStepStart();
                    }
                    JumpController.Result result = jumpController.next();
                    switch (result.error) {
                        case OK:
                            if (listener != null && !isInterrupted()) {
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
                doStop();
            }
        };
        jumpControllerThread.start();
    }

    private boolean doStart() {
        if (!jumpController.start()) {
            return false;
        }
        if (listener != null) {
            listener.onStart();
        }
        return true;
    }

    private void doStop() {
        jumpController.stop();
        jumpControllerThread = null;
        if (listener != null) {
            listener.onStop();
        }
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
