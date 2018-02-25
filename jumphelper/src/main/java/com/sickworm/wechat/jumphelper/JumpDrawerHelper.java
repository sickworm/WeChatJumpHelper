package com.sickworm.wechat.jumphelper;

import android.content.Context;
import android.graphics.Bitmap;

import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.graph.Graph;
import com.sickworm.wechat.graph.Line;
import com.sickworm.wechat.graph.NativeMat;
import com.sickworm.wechat.graph.OverlayDebugView;
import com.sickworm.wechat.graph.Point;
import com.sickworm.wechat.graph.Rect;
import com.sickworm.wechat.graph.Size;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 辅助跳一跳助手 SDK 接口类。
 * 该助手会根据按压画出辅助线
 * 暂时只兼容 Nexus 5
 *
 * Created by sickworm on 2018/2/23.
 */

public class JumpDrawerHelper {
    /**
     * 按压时间 s 转换为距离 dp 的系数
     */
    private static final double DEFAULT_SCALE = 1 / 8.5;
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

    private static volatile JumpDrawerHelper instance;

    private static final int STATUS_IDLE = 0;
    private static final int STATUS_DOWN = 1;
    private static final int STATUS_HAS_DOWN = 2;
    private static final int STATUS_UP = 3;
    private int status = STATUS_IDLE;
    private DeviceHelper deviceHelper;

    private NativeMat currentFrame;
    private NativeMat currentROIFrame;
    private Point chessPoint;
    private Size screenSize;
    private float density;
    private ExecutorService executorService;
    private OverlayDebugView debugView;
    private List<Graph> graphs = new ArrayList<>();

    private boolean needDetect;
    private long pressDownTime;
    private long pressTime;

    /**
     * 检测区域
     */
    private Rect roi;

    public static JumpDrawerHelper getInstance() {
        if (instance == null) {
            synchronized (JumpHelper.class) {
                if (instance == null) {
                    instance = new JumpDrawerHelper();
                }
            }
        }
        return instance;
    }

    private JumpDrawerHelper() {
        executorService = Executors.newSingleThreadExecutor();
        debugView = OverlayDebugView.getInstance();
        needDetect = false;
    }

    public void start(final Context context) {
        screenSize = ScreenUtils.getScreenSize(context);
        density = ScreenUtils.getDensity(context);
        roi = new Rect(
                (int) (screenSize.width * ROI_TOP_X_SCALE),
                (int) (screenSize.height * ROI_TOP_Y_SCALE),
                (int) (screenSize.width * (ROI_BOTTOM_X_SCALE - ROI_TOP_X_SCALE)),
                (int) (screenSize.height * (ROI_BOTTOM_Y_SCALE - ROI_TOP_Y_SCALE)));

        // 监听触摸
        new Thread() {
            @Override
            public void run() {
                try {
                    Process process = Runtime.getRuntime().exec("su -c cat /dev/input/event1");
                    InputStream in = process.getInputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while((length = in.read(buffer)) != -1) {
                        LogUtils.d("got " + length + ", " + toHexString(buffer, length));
                        int offset = 0;
                        while(offset < length) {
                            try {
                                InputEvent inputEvent = InputEvent.parse(buffer, offset);
                                if (inputEvent.type == InputEvent.ABS_MT_TRACKING_ID) {
                                    if (status == STATUS_IDLE) {
                                        status = STATUS_DOWN;
                                    } else if (status == STATUS_HAS_DOWN) {
                                        status = STATUS_UP;
                                    }
                                } else if (inputEvent.type == InputEvent.SYNC) {
                                    if (status == STATUS_DOWN) {
                                        status = STATUS_HAS_DOWN;
                                        pressDown();
                                    } else if (status == STATUS_UP) {
                                        status = STATUS_IDLE;
                                        pressUp();
                                    }
                                }
                                LogUtils.d(inputEvent);
                            } catch (Exception e) {
                                LogUtils.w(e);
                                break;
                            }
                            offset += InputEvent.BYTES_LENGTH;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // 检测棋子位置
        final JumpCVDetector jumpCVDetector = new JumpCVDetector(
                screenSize.width,
                screenSize.height,
                density);
        new Thread() {
            @Override
            public void run() {
                deviceHelper = DeviceHelper.getInstance();
                if (!deviceHelper.start(context)) {
                    LogUtils.e("fuck");
                    return;
                }
                final OverlayDebugView debugView = OverlayDebugView.getInstance();
                currentFrame = new NativeMat();
                currentROIFrame = new NativeMat();

                Point lastChessPoint = chessPoint;
                boolean isStable = false;
                while (true) {
                    // 检测间隔延时，节省性能
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LogUtils.w(e);
                        break;
                    }

                    // 屏幕按压状态下不检测
                    if (status != STATUS_IDLE) {
                        continue;
                    }

                    // 屏幕刚释放，延时后（跳过跳跃动画）开始检测
                    if (needDetect) {
                        LogUtils.i("restart detection");
                        needDetect = false;
                        isStable = false;
                        if (debugView != null) {
                            debugView.setGraphs(null);
                        }
                        try {
                            Thread.sleep(pressTime + 300);
                        } catch (InterruptedException e) {
                            LogUtils.w(e);
                            break;
                        }
                    }
                    // 屏幕已稳定，不检测
                    if (isStable) {
                        continue;
                    }

                    if (!getNextROIScreenMat()) {
                        LogUtils.e("fuck2");
                    }
                    // 检测棋子位置
                    graphs.clear();
                    chessPoint = jumpCVDetector.getChessPosition(currentROIFrame);
                    if (chessPoint != null) {
                        chessPoint.x += roi.origin.x;
                        chessPoint.y += roi.origin.y;
                        chessPoint.type = Graph.TYPE_RED;
                        if (chessPoint.equals(lastChessPoint)) {
                            chessPoint.type = Graph.TYPE_GREEN;
                            isStable = true;
                            LogUtils.i("found stable chessPoint " + chessPoint);
                        }
                        graphs.add(chessPoint);
                    }
                    if (debugView != null) {
                        debugView.setGraphs(graphs);
                    }
                    lastChessPoint = chessPoint;
                }
            }
        }.start();
    }

    /**
     * 只取屏幕部分作为检测区域
     */
    private boolean getNextROIScreenMat() {
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

    private void pressDown() {
        if (chessPoint == null) {
            return;
        }
        final Line line = new Line(chessPoint.x, chessPoint.y, chessPoint.x, chessPoint.y);
        line.type = Graph.TYPE_GREEN;
        graphs.add(line);
        final boolean toTheRight = chessPoint.x < screenSize.width / 2;
        pressDownTime = System.currentTimeMillis();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (status != STATUS_IDLE) {
                    long deltaTime = System.currentTimeMillis() - pressDownTime;
                    line.end.y = line.start.y - (int) (deltaTime * DEFAULT_SCALE * density);
                    line.end.x = line.start.x + (int) ((toTheRight? 1 : -1) * deltaTime * density * DEFAULT_SCALE * 2);
                    debugView.setGraphs(graphs);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        LogUtils.w(e);
                        break;
                    }
                }
            }
        });
    }

    private void pressUp() {
        needDetect = true;
        pressTime = System.currentTimeMillis() - pressDownTime;
    }

    private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private String toHexString(byte[] bytes, int length) {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < length; i++) {
            builder.append(HEX[(bytes[i] & 0xff) >> 4]);
            builder.append(HEX[(bytes[i] & 0xff) % 16]);
        }
        return builder.toString();
    }

    @SuppressWarnings("unused")
    private static class InputEvent {

        static final int SYNC = 0x00;
        static final int ABS_MT_TRACKING_ID = 0x39;
        static final int ABS_MT_POSITION_X = 0x35;
        static final int ABS_MT_POSITION_Y = 0x36;
        static final int ABS_MT_PRESSURE = 0x3a;
        static final int ABS_MT_TOUCH_MAJOR = 0x30;

        static final int BYTES_LENGTH = 16;

        long timeInteger;
        long timeDecimal;
        short event;
        short type;
        int value;

        private static InputEvent parse(byte[] bytes, int offset) {
            if (offset + 16 >= bytes.length) {
                throw new IllegalArgumentException();
            }
            // 小端，先读到的是低位
            InputEvent inputEvent = new InputEvent();
            inputEvent.timeInteger = bytesToInt(bytes, offset);
            inputEvent.timeDecimal = bytesToInt(bytes, offset + 4);
            inputEvent.event = bytesToShort(bytes, offset + 8);
            inputEvent.type = bytesToShort(bytes, offset + 10);
            inputEvent.value = bytesToInt(bytes, offset + 12);
            return inputEvent;
        }

        private static short bytesToShort(byte[] bytes, int offset) {
            short value = 0;
            value += bytes[offset] & 0xff;
            value += (bytes[offset + 1] & 0xff) << 8;
            return value;
        }

        private static int bytesToInt(byte[] bytes, int offset) {
            int value = 0;
            value += bytes[offset] & 0xff;
            value += (bytes[offset + 1] & 0xff) << 8;
            value += (bytes[offset + 2] & 0xff) << 16;
            value += (bytes[offset + 3] & 0xff) << 24;
            return value;
        }

        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "[% 8d.%d]: %04x %04x %08x", timeInteger, timeDecimal, event, type, value);
        }
    }
}
