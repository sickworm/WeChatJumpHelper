package com.sickworm.wechat.jumphelper;

import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.apkfuns.logutils.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    static {
        LogUtils.getLogConfig().configShowBorders(false);
    }

    public ApplicationTest() {
        super(Application.class);
    }


    public void testDisplayHelper() {
        final Context context = getContext();
        final boolean[] args = new boolean[] {false, false};

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        assertTrue(windowManager != null);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        final DeviceHelper deviceHelper = DeviceHelper.getInstance();

        new Thread() {
            @Override
            public void run() {
                args[1] = deviceHelper.start(context);
                assertTrue(args[1]);
                int count = 5;
                while (count-- > 0) {
                    // 首次获取需要暂停，否则会得到 null
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        assertTrue(false);
                    }
                    Object frame = deviceHelper.getCurrentFrame();
                    LogUtils.i("frame: " + frame);
                    assertNotNull(frame);
                }
                deviceHelper.stop();
                args[0] = true;
            }
        }.start();

        while(!args[0]) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                assertTrue(false);
            }
        }
    }

    public void testInput() {
        try {
            Process process = Runtime.getRuntime().exec("ls /");
            InputStream is = process.getInputStream();
            Scanner scanner = new Scanner(is, "UTF-8");
            String text = scanner.useDelimiter("\\A").next();
            LogUtils.i(text);
            scanner.close();

            // 测试环境无法模拟
            Runtime.getRuntime().exec("input tap 400 400");
        } catch (IOException e) {
            LogUtils.e(e);
        }
    }
}