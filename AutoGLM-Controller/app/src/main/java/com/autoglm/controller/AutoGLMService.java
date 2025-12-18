package com.autoglm.controller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class AutoGLMService extends Service {
    private static final String TAG = "AutoGLMService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "AutoGLM_CHANNEL";
    private static final long POLL_INTERVAL = 1000; // 轮询间隔1秒

    private Handler handler;
    private Runnable statusPollingRunnable;
    private FloatWindowManager floatWindowManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "服务创建");
        // 检查录音权限
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "未获得录音权限，无法启动服务");
            stopSelf(); // 停止服务
            return;
        }

        // 初始化文件目录
        FileUtils.initDirectory();

        // 初始化悬浮窗
        floatWindowManager = FloatWindowManager.getInstance(this);
        floatWindowManager.showFloatWindow();

        // 创建通知渠道
        createNotificationChannel();

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification("服务启动中..."));

        // 初始化轮询
        initStatusPolling();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "服务启动");
        return START_STICKY; // 服务被杀死后自动重启
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "服务销毁");

        // 停止轮询
        if (handler != null && statusPollingRunnable != null) {
            handler.removeCallbacks(statusPollingRunnable);
        }

        // 释放悬浮窗
        if (floatWindowManager != null) {
            floatWindowManager.release();
        }
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AutoGLM控制器",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("AutoGLM服务状态通知");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * 创建通知
     */
    private Notification createNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AutoGLM控制器")
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    /**
     * 更新通知内容
     */
    private void updateNotification(String content) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, createNotification(content));
    }

    /**
     * 初始化状态轮询
     */
    private void initStatusPolling() {
        handler = new Handler(Looper.getMainLooper());
        statusPollingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // 读取状态并更新通知
                    String status = FileUtils.readStatus();
                    updateNotification(status);
                } catch (Exception e) {
                    Log.e(TAG, "轮询状态失败", e);
                    updateNotification("轮询失败: " + e.getMessage());
                } finally {
                    // 循环执行
                    handler.postDelayed(this, POLL_INTERVAL);
                }
            }
        };

        // 启动轮询
        handler.post(statusPollingRunnable);
    }
}