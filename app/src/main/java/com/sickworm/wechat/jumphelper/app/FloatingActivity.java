package com.sickworm.wechat.jumphelper.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.sickworm.wechat.jumphelper.JumpHelper;

/**
 * 悬浮窗点击界面
 *
 * Created by sickworm on 2017/12/31.
 */

public class FloatingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floating);

        final JumpHelper jumpHelper = JumpHelper.getInstance();

        if (jumpHelper.isRunning()) {
            jumpHelper.stop();
            toast(R.string.stopped);
        }

        findViewById(R.id.button_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpHelper.start(FloatingActivity.this);
                finish();
            }
        });
    }

    private void toast(final int msgId) {
        toast(getString(msgId));
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FloatingActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
