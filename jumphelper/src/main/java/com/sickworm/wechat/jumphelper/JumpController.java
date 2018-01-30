package com.sickworm.wechat.jumphelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Size;

import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.graph.Graph;
import com.sickworm.wechat.graph.Point;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * 跳一跳流程逻辑类
 *
 * Created by sickworm on 2017/12/30.
 */
class JumpController {
    private static final int STABLE_MAX_WAIT_MILL = 3000;
    private static final int STABLE_WAIT_DURATION = 100;
    private static final int STABLE_WAIT_COUNT = STABLE_MAX_WAIT_MILL / STABLE_WAIT_DURATION;
    private static final int MIN_JUMP_TIME_MILL = 10;
    private static final boolean STORE_FRAME = false;

    /**
     * 距离 dp 转换为按压时间 s 的系数1
     */
    private static final double DEFAULT_SCALE = 4.14;
    /**
     * 距离 dp 转换为按压时间 s 的系数2
     */
    private static final double DEFAULT_SCALE2 = 120000;

    private JumpCVDetector jumpCVDetector;
    /**
     * 外部调节用，距离—>按压时间 系数的偏差修正
     */
    private double correctionValue;
    private DisplayMetrics metrics;
    private DeviceHelper deviceHelper;
    private Context context;

    JumpController(Context context, double correctionValue) {
        this.context = context.getApplicationContext();
        metrics = context.getResources().getDisplayMetrics();
        Size screenSize = ScreenUtils.getScreenSize(context);
        float density = ScreenUtils.getDensity(context);
        jumpCVDetector = new JumpCVDetector(
                screenSize.getWidth(), screenSize.getHeight(), density);
        this.correctionValue = correctionValue;
        this.deviceHelper = DeviceHelper.getInstance();
    }

    boolean start() {
        return deviceHelper.start(context);
    }

    void stop() {
        deviceHelper.stop();
    }

    Result next() {
        int count = STABLE_WAIT_COUNT;

        Mat lastFrame = new Mat();
        Mat currentFrame = new Mat();
        if (!getScreenMat(lastFrame)) {
            return new Result(JumpError.SCREEN_RECORD_FAILED);
        }
        while (count-- > 0) {
            if (!getScreenMat(currentFrame)) {
                return new Result(JumpError.SCREEN_RECORD_FAILED);
            }
            jumpCVDetector.clearDebugGraphs();
            if (jumpCVDetector.isScreenStabled(currentFrame, lastFrame)) {
                break;
            }
            Mat t = currentFrame;
            currentFrame = lastFrame;
            lastFrame = t;
            try {
                Thread.sleep(STABLE_WAIT_DURATION);
            } catch (InterruptedException e) {
                return new Result(JumpError.INTERRUPTED);
            }
        }

        Point chessPoint = jumpCVDetector.getLastChessPosition();
        if (chessPoint == null) {
            return new Result(JumpError.NO_CHESS);
        }

        Point platformPoint = jumpCVDetector.getLastPlatformPosition();
        if (platformPoint == null) {
            return new Result(JumpError.NO_PLATFORM);
        }

        if (count < 0) {
            return new Result(JumpError.NOT_STABLE);
        }

        double distance = jumpCVDetector.calculateDistanceDp(chessPoint, platformPoint, metrics.density);
        int pressTimeMill = (int) (distance * DEFAULT_SCALE * correctionValue
                - distance * distance / DEFAULT_SCALE2
                + MIN_JUMP_TIME_MILL);

        LogUtils.i("jump from (%d, %d) to (%d, %d) for %d mill",
                chessPoint.x, chessPoint.y, platformPoint.x, platformPoint.y, pressTimeMill);
        deviceHelper.doPressAsync(chessPoint, pressTimeMill);

        return new Result(chessPoint, platformPoint, pressTimeMill);
    }

    private boolean getScreenMat(Mat frame) {
        Bitmap currentFrameBitmap = deviceHelper.getCurrentFrame();
        if (currentFrameBitmap == null) {
            return false;
        }
        Utils.bitmapToMat(currentFrameBitmap, frame);
        return true;
    }

    List<Graph> getDebugGraphs() {
        return jumpCVDetector.getDebugGraphs();
    }

    static class Result {
        JumpError error;
        Point fromPoint;
        Point toPoint;
        int pressTimeMill;

        Result(JumpError error) {
            if (error == JumpError.OK) {
                throw new IllegalArgumentException("JumpError.OK must have pressTime");
            }
            this.error = error;
        }

        Result(Point fromPoint, Point toPoint, int pressTimeMill) {
            this.error = JumpError.OK;
            this.fromPoint = fromPoint;
            this.toPoint = toPoint;
            this.pressTimeMill = pressTimeMill;
        }
    }

    private void saveMat(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(mat, bitmap);
        File f = new File(Environment.getExternalStorageDirectory().getPath() + "/current_frame.png");
        if (f.exists()) {
            if (!f.delete()) {
                LogUtils.e("delete bitmap file failed");
                return;
            }
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            LogUtils.i("save bitmap succeed");
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }
}
