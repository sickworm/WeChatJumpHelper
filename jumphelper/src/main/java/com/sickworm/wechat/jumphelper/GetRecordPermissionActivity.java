package com.sickworm.wechat.jumphelper;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * 申请录屏权限 Activity，结果回调至 DeviceHelper.onResult()
 *
 * Created by sickworm on 2017/12/31.
 */
public class GetRecordPermissionActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private MediaProjectionManager manager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            DeviceHelper.getInstance().onResult(false, null);
            finish();
            return;
        }
        Intent intent = manager.createScreenCaptureIntent();
        startActivityForResult(intent, PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                MediaProjection projection = manager.getMediaProjection(resultCode, data);
                DeviceHelper.getInstance().onResult(true, projection);
                finish();
            } else {
                DeviceHelper.getInstance().onResult(false, null);
                finish();
            }
        }
    }
}
