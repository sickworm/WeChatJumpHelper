package com.sickworm.wechat.jumphelper.app;

import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LogUtils.getLogConfig().configShowBorders(false);

        Button showButton = findViewById(R.id.btn_show);
        Button hideButton = findViewById(R.id.btn_hide);
        Button checkRootButton = findViewById(R.id.btn_check_root);
        Button checkFloatingWindowButton = findViewById(R.id.btn_check_floating_window);

        showButton.setOnClickListener(this);
        hideButton.setOnClickListener(this);
        checkRootButton.setOnClickListener(this);
        checkFloatingWindowButton.setOnClickListener(this);

        if (BuildConfig.QUICK_TEST) {
            showButton.performClick();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                LogUtils.d("UP %f %f", event.getRawX(), event.getRawY());
                break;
            case MotionEvent.ACTION_DOWN:
                LogUtils.d("DOWN %f %f", event.getRawX(), event.getRawY());
                break;
            case MotionEvent.ACTION_MOVE:
                LogUtils.d("MOVE %f %f", event.getRawX(), event.getRawY());
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, MyService.class);
        switch (v.getId()) {
            case R.id.btn_show:
                intent.putExtra(MyService.ACTION, MyService.SHOW);
                startService(intent);
                finish();
                break;
            case R.id.btn_hide:
                intent.putExtra(MyService.ACTION, MyService.HIDE);
                startService(intent);
                finish();
                break;
            case R.id.btn_check_root:
                boolean gotRootPermission = checkRoot();
                TextView rootStatusTextView = findViewById(R.id.tv_root_status);
                if (gotRootPermission) {
                    rootStatusTextView.setText(R.string.ok);
                    rootStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.light_green));
                } else {
                    toast(R.string.get_root_failed);
                    rootStatusTextView.setText(R.string.failed);
                    rootStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.light_red));
                }
                break;
            case R.id.btn_check_floating_window:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
                        Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        startActivity(myIntent);
                        break;
                    } else {
                        TextView floatingStatusTextView = findViewById(R.id.tv_floating_status);
                        floatingStatusTextView.setText(R.string.ok);
                        floatingStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.light_green));
                    }
                }
                break;
            default:
                break;
        }
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