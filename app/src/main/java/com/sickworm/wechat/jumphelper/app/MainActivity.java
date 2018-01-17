package com.sickworm.wechat.jumphelper.app;

import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.sickworm.wechat.jumphelper.JumpHelper;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mBtnShow = findViewById(R.id.btn_show);
        Button mBtnHide = findViewById(R.id.btn_hide);

        mBtnShow.setOnClickListener(this);
        mBtnHide.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, MyService.class);
        switch (v.getId()) {
            case R.id.btn_show:
                intent.putExtra(MyService.ACTION, MyService.SHOW);
                finish();
                break;
            case R.id.btn_hide:
                intent.putExtra(MyService.ACTION, MyService.HIDE);
                finish();
            case R.id.btn_check_root:
                if (!checkRoot()) {
                    toast(R.string.get_root_failed);
                }
                break;
            case R.id.btn_check_floating_window:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
                        Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        startActivity(myIntent);
                    }
                }
                break;
            default:
                break;
        }
        startService(intent);
    }

    private boolean checkRoot() {
        try {
            Process process = Runtime.getRuntime().exec("su -c ls /");
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            LogUtils.e(e);
            return false;
        }
    }

    private void toast(final int msgId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, getString(msgId), Toast.LENGTH_SHORT).show();
            }
        });
    }

}