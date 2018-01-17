package com.sickworm.wechat.jumphelper;

import android.content.Context;
import android.graphics.Point;
import android.media.Image;
import android.util.DisplayMetrics;
import android.util.Size;

import com.apkfuns.logutils.LogUtils;

import org.opencv.core.Mat;

/**
 * 跳一跳流程逻辑类
 *
 * Created by sickworm on 2017/12/30.
 */
class JumpController {
    private static final int STABLE_MAX_WAIT_MILL = 5000;
    private static final int STABLE_WAIT_DURATION = 50;
    private static final int STABLE_WAIT_COUNT = STABLE_MAX_WAIT_MILL / STABLE_WAIT_DURATION;
    private static final int MIN_JUMP_TIME_MILL = 10;

    /**
     * 距离 dp 转换为按压时间 s 的系数
     */
    private static final double DEFAULT_SCALE = 1.38 * 3;

    private JumpCVDetector jumpCVDetector;
    /**
     * 外部 距离——按压时间 偏差修正
     */
    private double correctionValue;
    private DisplayMetrics metrics;

    static {
        LogUtils.getLogConfig().configShowBorders(false);
    }

    JumpController(Context context, double correctionValue) {
        metrics = context.getResources().getDisplayMetrics();
        Size screenSize = ScreenUtils.getScreenSize(context);
        float density = ScreenUtils.getDensity(context);
        jumpCVDetector = new JumpCVDetector(
                screenSize.getWidth(), screenSize.getHeight(), density);
        this.correctionValue = correctionValue;
    }

    Result next(Image currentFrameImage) {
        int count = STABLE_WAIT_COUNT;
        Mat currentFrame = JumpCVDetector.imageToMat(currentFrameImage);
        Mat lastFrame = null;
        while (count-- > 0) {
            if (jumpCVDetector.isScreenStabled(currentFrame, lastFrame)) {
                break;
            }
            lastFrame = currentFrame;
            try {
                Thread.sleep(STABLE_WAIT_DURATION);
            } catch (InterruptedException e) {
                return new Result(Error.INTERRUPTED);
            }
        }
        if (count < 0) {
            return new Result(Error.NOT_STABLE);
        }

        Point chessPoint = jumpCVDetector.getChessPosition(currentFrame);
        if (chessPoint == null) {
            return new Result(Error.NO_CHESS);
        }

        Point platformPoint = jumpCVDetector.getNextPlatformPosition(currentFrame);
        if (platformPoint == null) {
            return new Result(Error.NO_PLATFORM);
        }

        double distance = jumpCVDetector.calculateDistanceDp(chessPoint, platformPoint, metrics.density);
        int pressTimeMill = (int) (distance * DEFAULT_SCALE * correctionValue + MIN_JUMP_TIME_MILL);

        return new Result(chessPoint, platformPoint, pressTimeMill);
    }

    static class Result {
        Error error;
        Point pressPoint;
        Point destPoint;
        int pressTimeMill;

        Result(Error error) {
            if (error == Error.OK) {
                throw new IllegalArgumentException("Error.OK must have pressTime");
            }
            this.error = error;
        }

        Result(Point pressPoint, Point destPoint, int pressTimeMill) {
            this.error = Error.OK;
            this.pressPoint = pressPoint;
            this.destPoint = destPoint;
            this.pressTimeMill = pressTimeMill;
        }
    }

    enum Error {
        OK, NOT_STABLE, NO_CHESS, NO_PLATFORM, INTERRUPTED
    }
}
