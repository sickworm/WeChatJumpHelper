package com.sickworm.wechat.graph;

import android.graphics.Bitmap;


/**
 * OpenCV 自带的 Bitmap 转 Mat 的内存优化不好，所以自己实现一个
 *
 * Created by sickworm on 2018/2/3.
 */

public class NativeMat {
    public long nativeObj;

    public static void bitmapToMat(Bitmap bitmap, NativeMat mat) {
        bitmapToMat(bitmap, mat.nativeObj);
    }

    public static void matToBitmap(NativeMat mat, Bitmap bitmap) {
        matToBitmap(mat.nativeObj, bitmap);
    }

    public NativeMat() {
        nativeObj = newMat();
    }

    public void release() {
        releaseMat(nativeObj);
        nativeObj = 0;
    }

    public int width() {
        return width(nativeObj);
    }

    public int height() {
        return height(nativeObj);
    }

    private static native long newMat();
    private static native void releaseMat(long mat);
    private static native int width(long mat);
    private static native int height(long mat);

    private static native void bitmapToMat(Bitmap bitmap, long mat);
    private static native void matToBitmap(long mat, Bitmap bitmap);
}
