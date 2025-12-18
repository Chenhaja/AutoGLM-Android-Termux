package com.autoglm.controller;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechConstant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SpeechRecognizerHelper {
    private static final String TAG = "SpeechRecognizerHelper";
    private final Context context;
    private SpeechRecognizer speechRecognizer;
    private OnRecognizeResultListener listener;

    public interface OnRecognizeResultListener {
        void onResult(String result);
        void onError(String errorMsg);
        void onRecordingStart();
        void onRecordingEnd();
    }

    public SpeechRecognizerHelper(Context context) {
        this.context = context;
        initRecognizer();
    }

    /**
     * 初始化讯飞语音识别器
     */
    private void initRecognizer() {
        // 初始化识别对象
        speechRecognizer = SpeechRecognizer.createRecognizer(context, mInitListener);

        // 配置离线识别参数
        setParam();
    }

    /**
     * 初始化监听器。
     */
    private final InitListener mInitListener = code -> {
        Log.d(TAG, "SpeechRecognizer init() code = " + code);
        if (code != ErrorCode.SUCCESS) {
            Log.e(TAG, "初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    };

    /**
     * 设置识别参数
     */
    private void setParam() {
        //设置语法ID和 SUBJECT 为空，以免因之前有语法调用而设置了此参数；或直接清空所有参数，具体可参考 DEMO 的示例。
        speechRecognizer.setParameter( SpeechConstant.CLOUD_GRAMMAR, null );
        speechRecognizer.setParameter( SpeechConstant.SUBJECT, null );
//设置返回结果格式，目前支持json,xml以及plain 三种格式，其中plain为纯听写文本内容
        speechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
//此处engineType为“cloud”
        speechRecognizer.setParameter( SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
//设置语音输入语言，zh_cn为简体中文
        speechRecognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
//设置结果返回语言
        speechRecognizer.setParameter(SpeechConstant.ACCENT, "mandarin");
// 设置语音前端点:静音超时时间，单位ms，即用户多长时间不说话则当做超时处理
//取值范围{1000～10000}
        speechRecognizer.setParameter(SpeechConstant.VAD_BOS, "4000");
//设置语音后端点:后端点静音检测时间，单位ms，即用户停止说话多长时间内即认为不再输入，
//自动停止录音，范围{0~10000}
        speechRecognizer.setParameter(SpeechConstant.VAD_EOS, "1000");
//设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        speechRecognizer.setParameter(SpeechConstant.ASR_PTT,"1");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
//        speechRecognizer.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
//        speechRecognizer.setParameter(SpeechConstant.ASR_AUDIO_PATH,
//                Environment.getExternalStorageDirectory() + "/msc/asr.wav");
    }

    /**
     * 开始语音识别
     */
    public void startListening() {
        if (speechRecognizer == null) {
            initRecognizer();
        }

        int ret = speechRecognizer.startListening(recognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            if (listener != null) {
                listener.onError("识别启动失败，错误码：" + ret);
            }
        }
    }

    /**
     * 停止语音识别
     */
    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    /**
     * 销毁识别器
     */
    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    public void setOnRecognizeResultListener(OnRecognizeResultListener listener) {
        this.listener = listener;
    }

    /**
     * 识别监听器
     */
    private RecognizerListener recognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 开始录音
            Log.d(TAG, "开始录音");
            if (listener != null) {
                listener.onRecordingStart();
            }
        }

        @Override
        public void onError(SpeechError error) {
            // 识别出错
            String errorMsg = error.getPlainDescription(true);
            Log.e(TAG, "识别错误: " + errorMsg);
            if (listener != null) {
                listener.onError(errorMsg);
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 结束录音
            Log.d(TAG, "结束录音");
            if (listener != null) {
                listener.onRecordingEnd();
            }
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, "识别结果: " + results.getResultString());

            String text = parseIatResult(results.getResultString());
            if (listener != null && isLast) {
                listener.onResult(text);
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            // 音量变化
            Log.d(TAG, "音量变化: " + volume);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 其他事件
        }
    };

    /**
     * 解析讯飞返回的JSON结果
     */
    private String parseIatResult(String json) {
        StringBuffer ret = new StringBuffer();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                // 转写结果词，默认使用第一个结果
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                JSONObject obj = items.getJSONObject(0);
                ret.append(obj.getString("w"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret.toString();
    }
}