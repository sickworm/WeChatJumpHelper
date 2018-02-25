package com.sickworm.wechat.jumphelper;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private String jumpExecPath;

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
        String jumpExecDir = context.getDir("exec", Context.MODE_PRIVATE).getPath();
        jumpExecPath = jumpExecDir + "/jump";

        // TODO 使用独立申请权限接口，此处仅作检测
        if (!getRecordPermission(context)) {
            return false;
        }
        imageReader = ImageReader.newInstance(screenSize.width,screenSize.height, PixelFormat.RGBA_8888, 2);
        projection.createVirtualDisplay("jumpHelper",
                screenSize.width, screenSize.height,
                densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                imageReader.getSurface(), null, null);

        // 测试 root 权限
        try {
            Process process = Runtime.getRuntime().exec("su -c ls /");
            process.waitFor();
            if (process.exitValue() != 0) {
                LogUtils.w("no root permission");
                return false;
            }
        } catch (Exception e) {
            LogUtils.w(e);
            return false;
        }

        // 拷贝快速 jump 执行文件到 apk 目录
        if (!copyJumpExecFromAssets(context)) {
            return false;
        }

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

    private boolean copyJumpExecFromAssets(Context context) {
        AssetManager manager = context.getResources().getAssets();
        try {
            String[] paths = manager.list("jumpexec");
            for (String abi : Build.SUPPORTED_ABIS) {
                for (String path : paths) {
                    if (abi.equals(path)) {
                        String fullPath = "jumpexec/" + path + "/jump";
                        return copyJumpExecFromAssets(context, fullPath);
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.w(e);
            return false;
        }
        return false;
    }

    private boolean copyJumpExecFromAssets(Context context, String path) {
        AssetManager manager = context.getResources().getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = manager.open(path);
            File des = new File(jumpExecPath);
            if (des.exists()) {
                if (!des.delete()) {
                    LogUtils.w("delete old jump file failed");
                    return false;
                }
            }
            out = new FileOutputStream(des);
            byte[] buffer = new byte[1024];
            int length;
            while((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            if (!des.setExecutable(true)) {
                LogUtils.w("set jump executable failed");
                return false;
            }
            return true;
        } catch (IOException e) {
            LogUtils.w(e);
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                LogUtils.w(e);
            }
        }
    }

    Bitmap getCurrentFrame() {
        long startTime = System.currentTimeMillis();
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
            long usedTime = System.currentTimeMillis() - startTime;
            LogUtils.i("get frame use %d ms", usedTime);
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
     * sendEvent 发送触摸事件，速度更快。目前仅兼容了 Nexus 5
     * TODO 通过查询 getevent 支持自动匹配
     */
    @SuppressWarnings("UnusedReturnValue")
    private boolean doPressSyncUsingSendEvent(int pressTimeMill) {
        try {
            Process process = Runtime.getRuntime().exec("su -c " + jumpExecPath + " " + pressTimeMill);
            process.waitFor();
            return process.exitValue() == 0;
        } catch (IOException e) {
            LogUtils.w(e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    void doPressAsync(final Point point, final int pressTimeMill) {
        singleExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (Build.MODEL.contains("HammerHead")) {
                    doPressSyncUsingSendEvent(pressTimeMill);
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
