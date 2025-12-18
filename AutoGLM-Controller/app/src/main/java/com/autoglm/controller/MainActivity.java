package com.autoglm.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button startServiceBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startServiceBtn = findViewById(R.id.start_service_btn);
        startServiceBtn.setOnClickListener(v -> startAutoGLMService());

        // 检查权限
        checkPermissions();
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        if (!PermissionUtils.checkAndRequestAllPermissions(this)) {
            Toast.makeText(this, "请授予必要权限以正常使用", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 启动服务
     */
    private void startAutoGLMService() {
        if (!PermissionUtils.checkBasePermissions(this)) {
            Toast.makeText(this, "基础权限未授予", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!PermissionUtils.hasOverlayPermission(this)) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
            PermissionUtils.requestOverlayPermission(this);
            return;
        }

        // 启动前台服务
        Intent serviceIntent = new Intent(this, AutoGLMService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show();
        finish(); // 关闭主界面
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PermissionUtils.OVERLAY_PERMISSION_REQUEST_CODE:
                if (PermissionUtils.hasOverlayPermission(this)) {
                    Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "悬浮窗权限被拒绝", Toast.LENGTH_SHORT).show();
                }
                break;

            case PermissionUtils.STORAGE_MANAGE_REQUEST_CODE:
                if (PermissionUtils.hasStorageManagePermission(this)) {
                    Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "存储权限被拒绝", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "基础权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "基础权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }
}