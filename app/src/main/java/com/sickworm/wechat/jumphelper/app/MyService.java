package com.sickworm.wechat.jumphelper.app;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.graph.Point;
import com.sickworm.wechat.jumphelper.JumpError;
import com.sickworm.wechat.jumphelper.JumpHelper;

/**
 * 控制悬浮窗的 Service
 *
 * Created by sickworm on 2017/12/30.
 */
public class MyService extends Service {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static final String ACTION="action";
    public static final String SHOW = "show";
    public static final String HIDE = "hide";
    private FloatingView floatingView;
    private JumpHelper jumpHelper;

    static {
        LogUtils.getLogConfig().configShowBorders(false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        floatingView = new FloatingView(this);
        jumpHelper = JumpHelper.getInstance();
        jumpHelper.setOnStatusChangedListener(listener);
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra(ACTION);
            if (action == null) {
                return super.onStartCommand(intent, flags, startId);
            }
            switch (action) {
                case SHOW:
                    floatingView.show();
                    break;
                case HIDE:
                    floatingView.hide();
                    break;
                default:
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private JumpHelper.OnStateChangedListener listener = new JumpHelper.OnStateChangedListener() {
        private int count = 1;
        @Override
        public void onStart() {
            toast(R.string.jump_begin);
        }

        @Override
        public void onStepStart() {
            LogUtils.i("step " + count);
            floatingView.setDebugGraphs(null);
        }

        @Override
        public void onStep(Point from, Point to, double pressTime) {
            floatingView.setDebugGraphs(jumpHelper.getDebugGraphs());
            if (count++ % 10 == 0) {
                toast(String.format(getString(R.string.already_steps), count));
            }
        }

        @Override
        public void onError(JumpError error) {
            switch (error) {
                case NO_CHESS:
                    toast(R.string.no_chess);
                    break;
                case NOT_STABLE:
                    toast(R.string.not_stable);
                    break;
                case NO_PLATFORM:
                    toast(R.string.no_platform);
                    break;
                case PRESS_FAILED:
                    toast(R.string.press_failed);
                    break;
                case NO_PERMISSION:
                    toast(R.string.no_permission);
                    break;
                case SCREEN_RECORD_FAILED:
                    toast(R.string.screen_record_failed);
                    break;
            }
            floatingView.clearDebugGraphs();
        }

        @Override
        public void onStop() {
        }
    };

    private void toast(int msgId) {
        toast(getString(msgId));
    }

    private void toast(final String msg) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MyService.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
