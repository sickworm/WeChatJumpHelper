package com.sickworm.wechat.jumphelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.graph.Ellipse;
import com.sickworm.wechat.graph.Graph;
import com.sickworm.wechat.graph.Line;
import com.sickworm.wechat.graph.NativeMat;
import com.sickworm.wechat.graph.OverlayDebugView;
import com.sickworm.wechat.graph.Point;
import com.sickworm.wechat.graph.Rect;
import com.sickworm.wechat.graph.Size;

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
    private static final int STABLE_WAIT_DURATION = 200;
    private static final int STABLE_WAIT_COUNT = STABLE_MAX_WAIT_MILL / STABLE_WAIT_DURATION;
    private static final int MIN_JUMP_TIME_MILL = 10;
    private static final boolean STORE_FRAME = false;

    /**
     * 距离 dp 转换为按压时间 s 的系数1
     */
//    private static final double DEFAULT_SCALE = 4.14; // nexus 5
    private static final double DEFAULT_SCALE = 3.60; // one plus 6T

    /**
     * 距离 dp 转换为按压时间 s 的系数2
     */
//    private static final int DEFAULT_SCALE2 = 120000; // nexus 5
    private static final int DEFAULT_SCALE2 = 100000; // one plus 6T
    /**
     * 检测区域 x 上限
     */
    private static final double ROI_TOP_X_SCALE = 0;
    /**
     * 检测区域 x 下限
     */
    private static final double ROI_BOTTOM_X_SCALE = 1;
    /**
     * 检测区域 y 上限
     */
    private static final double ROI_TOP_Y_SCALE = 0.3;
    /**
     * 检测区域 y 下限
     */
    private static final double ROI_BOTTOM_Y_SCALE = 0.7;

    private JumpCVDetector jumpCVDetector;
    private DeviceHelper deviceHelper;
    private Context context;

    private NativeMat currentFrame;
    private NativeMat lastROIFrame;
    private NativeMat currentROIFrame;

    /**
     * 外部调节用，距离—>按压时间 系数的偏差修正
     */
    private double correctionValue;
    /**
     * 检测区域
     */
    private Rect roi;

    JumpController(Context context, double correctionValue) {
        this.context = context.getApplicationContext();
        Size screenSize = ScreenUtils.getScreenSize(context);
        float density = ScreenUtils.getDensity(context);
        jumpCVDetector = new JumpCVDetector(screenSize.width, screenSize.height, density);
        this.correctionValue = correctionValue;
        this.deviceHelper = DeviceHelper.getInstance();

        currentFrame = new NativeMat();
        currentROIFrame = new NativeMat();
        lastROIFrame = new NativeMat();

        roi = new Rect(
                (int) (screenSize.width * ROI_TOP_X_SCALE),
                (int) (screenSize.height * ROI_TOP_Y_SCALE),
                (int) (screenSize.width * (ROI_BOTTOM_X_SCALE - ROI_TOP_X_SCALE)),
                (int) (screenSize.height * (ROI_BOTTOM_Y_SCALE - ROI_TOP_Y_SCALE)));
    }

    boolean start() {
        return deviceHelper.start(context);
    }

    void stop() {
        deviceHelper.stop();
    }

    Result next() {
        int count = STABLE_WAIT_COUNT;
        OverlayDebugView debugView = OverlayDebugView.getInstance();

        if (debugView != null) {
            debugView.setGraphs(null);
        }
        if (!getNextROIScreenMat()) {
            return new Result(JumpError.SCREEN_RECORD_FAILED);
        }
        while (count-- > 0) {
            if (!getNextROIScreenMat()) {
                return new Result(JumpError.SCREEN_RECORD_FAILED);
            }

            if (STORE_FRAME) {
                saveMat(currentROIFrame);
            }
            jumpCVDetector.clearDebugGraphs();

            if (jumpCVDetector.isScreenStabled(currentROIFrame, lastROIFrame)) {
                break;
            }
            try {
                Thread.sleep(STABLE_WAIT_DURATION);
            } catch (InterruptedException e) {
                return new Result(JumpError.INTERRUPTED);
            }
        }
        if (debugView != null) {
            debugView.setGraphs(getCorrectedDebugGraphs());
        }

        Point chessPoint = jumpCVDetector.getLastChessPosition();
        if (chessPoint == null) {
            if (debugView != null) {
                debugView.setGraphs(null);
            }
            return new Result(JumpError.NO_CHESS);
        }

        Point platformPoint = jumpCVDetector.getLastPlatformPosition();
        if (platformPoint == null) {
            if (debugView != null) {
                debugView.setGraphs(null);
            }
            return new Result(JumpError.NO_PLATFORM);
        }

        if (count < 0) {
            if (debugView != null) {
                debugView.setGraphs(null);
            }
            return new Result(JumpError.NOT_STABLE);
        }

        double distance = jumpCVDetector.calculateDistanceDp(chessPoint, platformPoint);
        int pressTimeMill = (int) (distance * DEFAULT_SCALE * correctionValue
                - distance * distance / DEFAULT_SCALE2
                + MIN_JUMP_TIME_MILL);

        LogUtils.i("jump from (%d, %d) to (%d, %d) for %d mill",
                chessPoint.x, chessPoint.y, platformPoint.x, platformPoint.y, pressTimeMill);
        deviceHelper.doPressAsync(chessPoint, pressTimeMill);

        return new Result(chessPoint, platformPoint, pressTimeMill);
    }

    /**
     * 只取屏幕部分作为检测区域
     */
    private boolean getNextROIScreenMat() {
        NativeMat m = lastROIFrame;
        lastROIFrame = currentROIFrame;
        currentROIFrame = m;
        if (!getNextScreenMat()) {
            return false;
        }
        NativeMat.matROI(currentFrame, currentROIFrame,
                roi.origin.x, roi.origin.y, roi.size.width, roi.size.height);
        return true;
    }

    private boolean getNextScreenMat() {
        Bitmap currentFrameBitmap = deviceHelper.getCurrentFrame();
        if (currentFrameBitmap == null) {
            return false;
        }
        NativeMat.bitmapToMat(currentFrameBitmap, currentFrame);
        return true;
    }

    /**
     * 将检测区域点的相对坐标恢复成原屏幕的坐标
     */
    private List<Graph> getCorrectedDebugGraphs() {
        List<Graph> graphs = jumpCVDetector.getDebugGraphs();
        for(Graph graph : graphs) {
            if (graph instanceof Point) {
                Point point = (Point) graph;
                point.x += roi.origin.x;
                point.y += roi.origin.y;
            } else if (graph instanceof Ellipse) {
                Ellipse ellipse = (Ellipse) graph;
                ellipse.center.x += roi.origin.x;
                ellipse.center.y += roi.origin.y;
            } else if (graph instanceof Line) {
                Line line = (Line) graph;
                line.start.x += roi.origin.x;
                line.start.y += roi.origin.y;
                line.end.x += roi.origin.x;
                line.end.y += roi.origin.y;
            } else if (graph instanceof Rect) {
                Rect rect = (Rect) graph;
                rect.origin.x += roi.origin.x;
                rect.origin.y += roi.origin.y;
            }
        }
        graphs.add(roi);
        return graphs;
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

    private void saveMat(NativeMat mat) {
        int width = mat.width();
        int height = mat.height();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        NativeMat.matToBitmap(mat, bitmap);
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
            LogUtils.d("save bitmap succeed");
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }
}
