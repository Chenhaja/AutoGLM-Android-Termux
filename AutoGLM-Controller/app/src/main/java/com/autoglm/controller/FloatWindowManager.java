package com.autoglm.controller;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.view.GestureDetectorCompat;

public class FloatWindowManager {
    private static final String TAG = "FloatWindowManager";
    private static FloatWindowManager instance;

    private final Context context;
    private final WindowManager windowManager;
    private final GestureDetectorCompat gestureDetector;

    private View floatView;
    private WindowManager.LayoutParams layoutParams;

    // 屏幕参数
    private int screenWidth;
    private int screenHeight;
    private int statusBarHeight;

    // 悬浮窗状态
    private boolean isRecording = false;
    private SpeechRecognizerHelper speechHelper;
    private ImageView recordAnimationView;

    private FloatWindowManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.gestureDetector = new GestureDetectorCompat(context, new FloatWindowGestureListener());

        // 获取屏幕参数
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        // 获取状态栏高度
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }

        initFloatView();
        initSpeechHelper();
    }

    public static FloatWindowManager getInstance(Context context) {
        if (instance == null) {
            synchronized (FloatWindowManager.class) {
                if (instance == null) {
                    instance = new FloatWindowManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化悬浮窗视图
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initFloatView() {
        // 加载悬浮窗布局
        floatView = View.inflate(context, R.layout.float_window, null);
        recordAnimationView = floatView.findViewById(R.id.record_animation);

        // 设置悬浮窗参数
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.x = 0; // 默认左侧
        layoutParams.y = screenHeight / 2; // 垂直居中

        // 设置触摸监听
        floatView.setOnTouchListener((v, event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 记录初始位置
                    layoutParams.x = (int) event.getRawX() - floatView.getWidth() / 2;
                    layoutParams.y = (int) event.getRawY() - floatView.getHeight() / 2 - statusBarHeight;
                    windowManager.updateViewLayout(floatView, layoutParams);
                    break;

                case MotionEvent.ACTION_MOVE:
                    // 更新位置
                    layoutParams.x = (int) event.getRawX() - floatView.getWidth() / 2;
                    layoutParams.y = (int) event.getRawY() - floatView.getHeight() / 2 - statusBarHeight;
                    windowManager.updateViewLayout(floatView, layoutParams);
                    break;

                case MotionEvent.ACTION_UP:
                    // 磁吸到边缘
                    magnetToEdge();
                    break;
            }
            return true;
        });
    }

    /**
     * 初始化语音识别助手
     */
    private void initSpeechHelper() {
        speechHelper = new SpeechRecognizerHelper(context);
        speechHelper.setOnRecognizeResultListener(new SpeechRecognizerHelper.OnRecognizeResultListener() {
            @Override
            public void onResult(String result) {
                isRecording = false;
                stopRecordAnimation();
                FileUtils.writeCommand(result);
                Toast.makeText(context, "已发送命令: " + result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMsg) {
                isRecording = false;
                stopRecordAnimation();
                Toast.makeText(context, "识别失败: " + errorMsg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRecordingStart() {
                isRecording = true;
                startRecordAnimation();
                Toast.makeText(context, "开始录音...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRecordingEnd() {
                Toast.makeText(context, "正在识别...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 磁吸到屏幕边缘
     */
    private void magnetToEdge() {
        ValueAnimator animator = ValueAnimator.ofInt(layoutParams.x,
                layoutParams.x < screenWidth / 2 ? 0 : screenWidth - floatView.getWidth());
        animator.setDuration(200);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            layoutParams.x = (int) animation.getAnimatedValue();
            windowManager.updateViewLayout(floatView, layoutParams);
        });
        animator.start();
    }

    /**
     * 启动录音动画
     */
    private void startRecordAnimation() {
        recordAnimationView.setVisibility(View.VISIBLE);
        ValueAnimator animator = ValueAnimator.ofFloat(0.5f, 1.0f);
        animator.setDuration(800);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            recordAnimationView.setScaleX(value);
            recordAnimationView.setScaleY(value);
        });
        animator.start();
    }

    /**
     * 停止录音动画
     */
    private void stopRecordAnimation() {
        recordAnimationView.setVisibility(View.GONE);
        recordAnimationView.clearAnimation();
        recordAnimationView.setScaleX(1.0f);
        recordAnimationView.setScaleY(1.0f);
    }

    /**
     * 显示悬浮窗
     */
    public void showFloatWindow() {
        if (floatView.getParent() == null) {
            windowManager.addView(floatView, layoutParams);
        }
    }

    /**
     * 隐藏悬浮窗
     */
    public void hideFloatWindow() {
        if (floatView.getParent() != null) {
            windowManager.removeView(floatView);
        }
    }

    /**
     * 手势监听
     */
    private class FloatWindowGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public void onLongPress(MotionEvent e) {
            // 长按开始录音
            if (!isRecording) {
                speechHelper.startListening();
            }

        }


//        @Override
//        public boolean onSingleTapUp(MotionEvent e) {
//            // 点击展开更多选项（此处简化，可扩展）
//            Toast.makeText(context, "当前状态: " + FileUtils.readStatus(), Toast.LENGTH_LONG).show();
//            return true;
//        }
    }

    /**
     * 释放资源
     */
    public void release() {
        hideFloatWindow();
        if (speechHelper != null) {
            speechHelper.destroy();
        }
        instance = null;
    }
}