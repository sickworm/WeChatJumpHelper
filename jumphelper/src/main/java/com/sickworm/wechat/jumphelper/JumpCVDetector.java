package com.sickworm.wechat.jumphelper;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.Image;

import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.graph.Graph;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.List;


/**
 *
 * 微信跳一跳 OpenCV 识别 API
 *
 * Created by sickworm on 2017/12/30.
 */
class JumpCVDetector {
    private long nativeObj;

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("jump-cv");
    }

    JumpCVDetector(int width, int height, float density) {
        nativeObj = newInstance(width, height, density);
    }

    @Override
    protected void finalize() {
        deleteInstance(nativeObj);
    }

    /**
     * 判断画面是否稳定，稳定后方可做下一步
     */
    boolean isScreenStabled(Mat newFrame, Mat oldFrame) {
        if (newFrame == null || oldFrame == null) {
            return false;
        }
        if (newFrame == oldFrame) {
            return true;
        }

        Point newPosition = getChessPosition(newFrame);
        Point oldPosition = getChessPosition(oldFrame);
        if (newPosition != null && newPosition.equals(oldPosition)) {
            LogUtils.i("chess is stabled");
            newPosition = getNextPlatformPosition(newFrame);
            oldPosition = getNextPlatformPosition(oldFrame);
            if (newPosition != null && newPosition.equals(oldPosition)) {
                LogUtils.i("chess and platform are stabled");
                return true;
            }
        }
        return false;
    }

    /**
     * 获取棋子位置，找不到返回  null
     */
    Point getChessPosition(Mat currentFrame) {
        if (currentFrame == null) {
            return null;
        }
        return findChess(nativeObj, currentFrame.nativeObj);
    }

    /**
     * 获取下一个跳台的位置，找不到返回  null
     */
    Point getNextPlatformPosition(Mat currentFrame) {
        if (currentFrame == null) {
            return null;
        }
        return findPlatform(nativeObj, currentFrame.nativeObj);
    }

    /**
     * 计算两点间距离 dp
     */
    double calculateDistanceDp(Point a, Point b, float density) {
        int deltaX = a.x - b.x;
        int deltaY = a.y - b.y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY) / density;
    }

    void clearDebugGraphs() {
        clearDebugGraphs(nativeObj);
    }

    List<Graph> getDebugGraphs() {
        return getDebugGraphs(nativeObj);
    }

    static Mat imageToMat(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.RGB_565);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        Mat mat = new Mat(width + rowPadding / pixelStride, height, CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat, true);
        return mat;
    }

    private static native long newInstance(int width, int height, float density);
    private static native long deleteInstance(long instance);
    private static native Point findChess(long instance, long mat);
    private static native Point findPlatform(long instance, long mat);
    private static native List<Graph> clearDebugGraphs(long instance);
    private static native List<Graph> getDebugGraphs(long instance);
}
