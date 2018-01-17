package com.sickworm.wechat.jumphelper;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.util.Size;

import com.apkfuns.logutils.LogUtils;

import java.io.IOException;
import java.util.Locale;


/**
 * 设备操作类，包括获取屏幕内容和模拟点击
 *
 * Created by sickworm on 2017/12/30.
 */
class DeviceHelper {
    private static DeviceHelper instance = null;
    private MediaProjection projection;
    private final Object lock = new Object();
    private boolean permissionGranted = false;
    private ImageReader imageReader;
    private Image cache;

    static DeviceHelper getInstance() {
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

        if (!getRecordPermission(context)) {
            return false;
        }
        imageReader = ImageReader.newInstance(screenSize.getWidth(),screenSize.getHeight(), PixelFormat.RGB_565, 2);
        projection.createVirtualDisplay("jumpHelper",
                screenSize.getWidth(), screenSize.getHeight(),
                densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                imageReader.getSurface(), null, null);
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

    Image getCurrentFrame() {
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
            cache = image;
            return image;
        } catch (Exception e) {
            LogUtils.e("getCurrentFrame failed, e: " + e.getLocalizedMessage());
            return null;
        }
    }

    boolean doPress(Point point, int pressTimeMill) {
        // TODO 改为 sendEvent 速度更快
        String command = String.format(Locale.CHINA, "su -c input swipe %d %d %d %d %d",
                point.x, point.y, point.x, point.y, pressTimeMill);
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
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
