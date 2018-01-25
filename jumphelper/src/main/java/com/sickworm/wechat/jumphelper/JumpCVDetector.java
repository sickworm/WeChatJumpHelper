package com.sickworm.wechat.jumphelper;

import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.graph.Graph;
import com.sickworm.wechat.graph.Point;

import org.opencv.core.Mat;

import java.util.List;


/**
 *
 * 微信跳一跳 OpenCV 识别 API
 *
 * Created by sickworm on 2017/12/30.
 */
@SuppressWarnings("WeakerAccess")
class JumpCVDetector {
    private long nativeObj;
    private Mat lastFrame = null;
    private Point lastChessPosition = null;
    private Point lastPlatformPosition = null;

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
        nativeObj = 0;
    }

    /**
     * 判断画面是否稳定，稳定后方可做下一步
     */
    boolean isScreenStabled(Mat newFrame, Mat oldFrame) {
        if (newFrame == null || oldFrame == null) {
            return false;
        }
        if (newFrame == oldFrame) {
            LogUtils.d("two frames are the same object");
            return true;
        }

        Point newChessPosition = getChessPosition(newFrame);
        if (newChessPosition == null) {
            return false;
        }
        LogUtils.i("new chess point (%d, %d)", newChessPosition.x, newChessPosition.y);
        Point oldChessPosition = lastFrame == oldFrame? lastChessPosition : getChessPosition(oldFrame);
        lastChessPosition = newChessPosition;
        if (newChessPosition.equals(oldChessPosition)) {
            LogUtils.i("chess is stabled");
            Point newPlatformPosition = getNextPlatformPosition(newFrame);
            if (newPlatformPosition == null) {
                return false;
            }
            LogUtils.i("new platform point (%d, %d)", newPlatformPosition.x, newPlatformPosition.y);
            Point oldPlatformPosition = lastFrame == oldFrame? lastPlatformPosition : getChessPosition(oldFrame);
            lastPlatformPosition = newPlatformPosition;
            if (newPlatformPosition.equals(oldPlatformPosition)) {
                LogUtils.i("chess and platform are stabled");
                lastFrame = newFrame;
                return true;
            }
        }
        lastFrame = newFrame;
        return false;
    }

    Point getLastChessPosition() {
        return lastChessPosition;
    }

    Point getLastPlatformPosition() {
        return lastPlatformPosition;
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

    private static native long newInstance(int width, int height, float density);
    private static native long deleteInstance(long instance);
    private static native Point findChess(long instance, long mat);
    private static native Point findPlatform(long instance, long mat);
    private static native void clearDebugGraphs(long instance);
    private static native List<Graph> getDebugGraphs(long instance);
}
