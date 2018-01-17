package com.sickworm.wechat.jumphelper;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Size;

/**
 * 获取屏幕信息
 * Created by wang on 2018/1/13.
 */

class ScreenUtils {

    static Size getScreenSize(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int navigationBarHeight = getNavigationBarHeight(context);
        return new Size(metrics.widthPixels, metrics.heightPixels + navigationBarHeight);
    }

    static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    static int getDensityDpi(Context context) {
        return context.getResources().getDisplayMetrics().densityDpi;
    }

    private static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height","dimen", "android");
        return resources.getDimensionPixelSize(resourceId);
    }
}
