package com.sickworm.wechat.jumphelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.Image;
import android.os.Environment;

import com.apkfuns.logutils.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * 跳一跳 SDK 接口类
 *
 * Created by sickworm on 2017/12/30.
 */
public class JumpHelper {
    private static final int STEP_DURATION_MILL = 1000;
    private static final double DEFAULT_CORRECTION_VALUE = 1;
    private static volatile JumpHelper instance;

    private JumpController jumpController;
    private double correctionValue;
    private Thread jumpControllerThread;
    private DeviceHelper deviceHelper;
    private OnStateChangedListener listener;

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
        this.deviceHelper = DeviceHelper.getInstance();
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
                if (!deviceHelper.start(context)) {
                    if (listener != null) {
                        listener.onError(Error.NO_PERMISSION);
                        return;
                    }
                }
                if (listener != null) {
                    listener.onStart();
                }
                // ImageReader 的截屏准备时间
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                    interrupt();
                }

                loop:
                while (!isInterrupted()) {
                    Image currentFrame = deviceHelper.getCurrentFrame();
//                    saveImage(currentFrame);

                    if (currentFrame == null) {
                        onError(Error.SCREEN_RECORD_FAILED);
                        return;
                    }
                    JumpController.Result result = jumpController.next(currentFrame);
                    switch (result.error) {
                        case OK:
                            if (!deviceHelper.doPress(result.pressPoint, result.pressTimeMill)) {
                                onError(Error.PRESS_FAILED);
                                break loop;
                            }
                            if (listener != null && !isInterrupted()) {
                                listener.onStep(result.pressPoint, result.destPoint, result.pressTimeMill);
                            }
                            try {
                                sleep(STEP_DURATION_MILL);
                            } catch (InterruptedException ignore) {
                                interrupt();
                            }
                            break;
                        case INTERRUPTED:
                            interrupt();
                        case NO_CHESS:
                            onError(Error.NO_CHESS);
                            break loop;
                        case NOT_STABLE:
                            onError(Error.NOT_STABLE);
                            break loop;
                        case NO_PLATFORM:
                            onError(Error.NO_PLATFORM);
                            break loop;
                        default:
                            break;
                    }
                }
                doStop();
            }
        };
        jumpControllerThread.start();
    }

    private void doStop() {
        jumpControllerThread = null;
        deviceHelper.stop();
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

    private void onError(Error error) {
        if (listener != null) {
            listener.onError(error);
            listener.onStop();
        }
    }

    public interface OnStateChangedListener {
        void onStart();
        void onStep(Point from, Point to, double pressTime);
        void onError(Error error);
        void onStop();
    }

    public enum Error {
        NO_PERMISSION, NO_CHESS, NO_PLATFORM, NOT_STABLE, SCREEN_RECORD_FAILED, PRESS_FAILED
    }

    private void saveImage(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.RGB_565);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap,0,0, width, height);
        File f = new File(Environment.getExternalStorageDirectory().getPath() + "/current_frame.png");
        if (f.exists()) {
            if (!f.delete()) {
                LogUtils.e("delete bitmap file failed");
                return;
            }
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            LogUtils.i("save bitmap succeed");
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }
}
