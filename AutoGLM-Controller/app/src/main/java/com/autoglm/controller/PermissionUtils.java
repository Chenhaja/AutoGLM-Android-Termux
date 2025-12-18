package com.autoglm.controller;

import static androidx.core.app.ActivityCompat.requestPermissions;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    // 权限请求码
    public static final int PERMISSION_REQUEST_CODE = 1001;
    public static final int OVERLAY_PERMISSION_REQUEST_CODE = 1002;
    public static final int STORAGE_MANAGE_REQUEST_CODE = 1003;

    // 需要申请的基础权限
    private static final String[] BASE_PERMISSIONS = {
            android.Manifest.permission.RECORD_AUDIO
    };

    /**
     * 检查是否有悬浮窗权限
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    /**
     * 检查是否有存储管理权限（Android 11+）
     */
    public static boolean hasStorageManagePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    /**
     * 检查是否有通知权限（Android 11+）
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            return notificationManager != null && notificationManager.areNotificationsEnabled();
        }
        return true;
    }
    /**
     * 检查基础权限是否全部授予
     */
    public static boolean checkBasePermissions(Context context) {
        for (String permission : BASE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 申请基础权限
     */
    public static void requestBasePermissions(Activity activity) {
        requestPermissions(activity, BASE_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    /**
     * 跳转到悬浮窗权限设置页面
     */
    public static void requestOverlayPermission(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
    }

    /**
     * 跳转到存储管理权限设置页面（Android 11+）
     */
    public static void requestStorageManagePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, STORAGE_MANAGE_REQUEST_CODE);
        }
    }

    /**
     * 跳转到通知权限设置页面
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                activity.startActivity(intent);
            }
        }
    }
    /**
     * 检查并申请所有必要权限
     */
    public static boolean checkAndRequestAllPermissions(Activity activity) {
        boolean allGranted = true;

        // 检查基础权限
        if (!checkBasePermissions(activity)) {
            requestBasePermissions(activity);
            allGranted = false;
        }

        // 检查悬浮窗权限
        if (!hasOverlayPermission(activity)) {
            requestOverlayPermission(activity);
            allGranted = false;
        }

        // 检查存储权限
        if (!hasStorageManagePermission(activity)) {
            requestStorageManagePermission(activity);
            allGranted = false;
        }

        //检查通知权限
        if (!hasNotificationPermission(activity)) {
            requestNotificationPermission(activity);
            allGranted = false;
        }

        return allGranted;
    }
}