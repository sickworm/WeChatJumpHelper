package com.sickworm.wechat.jumphelper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;

import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.graph.Point;
import com.sickworm.wechat.graph.Size;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 设备操作类，包括获取屏幕内容和模拟点击
 *
 * Created by sickworm on 2017/12/30.
 */
public class DeviceHelper {
    private static DeviceHelper instance = null;
    private MediaProjection projection;
    private final Object lock = new Object();
    private boolean permissionGranted = false;
    private ImageReader imageReader;
    private Bitmap cache = null;
    private ExecutorService singleExecutor = Executors.newSingleThreadExecutor();

    public static DeviceHelper getInstance() {
        if (instance == null) {
            synchronized (DeviceHelper.class) {
                if (instance == null) {
                    instance = new DeviceHelper();
                }
            }
        }
        return instance;
    }

    private DeviceHelper() {
    }

    boolean start(Context context) {
        Size screenSize = ScreenUtils.getScreenSize(context);
        int densityDpi = ScreenUtils.getDensityDpi(context);

        // TODO 独立申请权限接口，此处仅检测
        if (!getRecordPermission(context)) {
            return false;
        }
        imageReader = ImageReader.newInstance(screenSize.width,screenSize.height, PixelFormat.RGBA_8888, 2);
        projection.createVirtualDisplay("jumpHelper",
                screenSize.width, screenSize.height,
                densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                imageReader.getSurface(), null, null);

        // ImageReader 的截屏准备时间
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LogUtils.e(e);
            return false;
        }
        Image image = imageReader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        cache = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        cache.copyPixelsFromBuffer(buffer);
        image.close();
        return true;
    }

    void stop() {
        if (projection != null) {
            projection.stop();
        }
    }

    private boolean getRecordPermission(Context context) {
        Intent requestPermissionIntent = new Intent(context, GetRecordPermissionActivity.class);
        requestPermissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(requestPermissionIntent);
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                LogUtils.w("getRecordPermission interrupted");
            }
        }
        return permissionGranted;
    }

    Bitmap getCurrentFrame() {
        if (imageReader == null) {
            return null;
        }
        try {
            Image image = imageReader.acquireLatestImage();
            // 界面静止的情况下 image 为 null
            if (image == null) {
                LogUtils.d("no new image");
                return cache;
            }
            imageToBitmap(image, cache);
            image.close();
            return cache;
        } catch (Exception e) {
            LogUtils.e("getCurrentFrame failed, e: " + e.getLocalizedMessage());
            return null;
        }
    }

    private static void imageToBitmap(Image image, Bitmap bitmap) {
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        bitmap.copyPixelsFromBuffer(buffer);
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean doPressSyncUsingInput(Point point, int pressTimeMill) {
        String command = String.format(Locale.CHINA, "su -c input swipe %d %d %d %d %d",
                point.x, point.y, point.x, point.y, pressTimeMill);
        try {
            LogUtils.d("start jump");
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            LogUtils.d("stop jump");
            return process.exitValue() == 0;
        } catch (IOException e) {
            LogUtils.e(e);
            return false;
        } catch (InterruptedException e) {
            LogUtils.w(e);
            Thread.currentThread().interrupt();
            return true;
        }
    }

    /**
     * sendEvent 发送触摸事件，速度更快，但兼容性不足，目前仅兼容了 Nexus 5
     * TODO 根据getEvent数据支持自动匹配
     */
    @SuppressWarnings("UnusedReturnValue")
    private boolean doPressSyncUsingSendEvent(Point point, int pressTimeMill) {
        String command = String.format(Locale.ENGLISH, "su -c " +
                "sendevent /dev/input/event1 3 57 62 && " +     // ABS_MT_TRACKING_ID，事件列表 ID，正常值为递增，随意赋值没有实际影响
                "sendevent /dev/input/event1 3 53 %d && " +     // ABS_MT_POSITION_X，x 坐标，若与上次一致则不发送
                "sendevent /dev/input/event1 3 54 %d && " +     // ABS_MT_POSITION_Y，y 坐标，若与上次一致则不发送
                "sendevent /dev/input/event1 3 58 46 && " +     // ABS_MT_PRESSURE，压力值，若与上次一致则不发送
                "sendevent /dev/input/event1 3 48 4 && " +      // ABS_MT_TOUCH_MAJOR，接触面积，若与上次一致则不发送
                "sendevent /dev/input/event1 0 0 0",        // 同步此次事件
                point.x, point.y);
        String command2 = "su -c " +
                        "sendevent /dev/input/event1 3 57 4294967295 && " + // ABS_MT_TRACKING_ID，此时为上一次按键的结束，值为 0xfffffff
                                                                            // 通过 sendevent 发送 0xffffffff，结果会为 7fffffff，但无实际影响
                        "sendevent /dev/input/event1 0 0 0";               // 同步此次事件
        try {
            LogUtils.d("start jump");
            long startTime = System.currentTimeMillis();
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            if (process.exitValue() != 0) {
                return false;
            }

            long stepTime = System.currentTimeMillis();
            LogUtils.d("command took " + (stepTime) + "ms");
            while(System.currentTimeMillis() - stepTime < pressTimeMill) {
                Thread.sleep(1);
            }
            LogUtils.d("pressed down");
            Process process2 = Runtime.getRuntime().exec(command2);
            process2.waitFor();
            LogUtils.d("stop jump");
            return process2.exitValue() == 0;
        } catch (IOException e) {
            LogUtils.e(e);
            return false;
        } catch (InterruptedException e) {
            LogUtils.w(e);
            Thread.currentThread().interrupt();
            return true;
        }
    }

    void doPressAsync(final Point point, final int pressTimeMill) {
        singleExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (Build.MODEL.contains("HammerHead")) {
                    doPressSyncUsingSendEvent(point, pressTimeMill);
                } else {
                    doPressSyncUsingInput(point, pressTimeMill);
                }
            }
        });
    }

    void onResult(boolean granted, MediaProjection projection) {
        permissionGranted = granted;
        if (granted) {
            this.projection = projection;
        }
        synchronized (lock) {
            lock.notify();
        }
    }
}
