package com.autoglm.controller;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

public class FileUtils {
    // 通信路径
    private static final String BASE_PATH = Environment.getExternalStorageDirectory() + "/UbuntuAndroid/";
    private static final String COMMAND_FILE = BASE_PATH + "command.txt";
    private static final String STATUS_FILE = BASE_PATH + "status.txt";

    // 线程安全锁
    private static final ReentrantLock lock = new ReentrantLock();

    /**
     * 初始化目录
     */
    public static void initDirectory() {
        lock.lock();
        try {
            File dir = new File(BASE_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 写入命令文本
     */
    public static boolean writeCommand(String content) {
        lock.lock();
        try {
            File file = new File(COMMAND_FILE);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 读取状态文本
     */
    public static String readStatus() {
        lock.lock();
        try {
            File file = new File(STATUS_FILE);
            if (!file.exists()) {
                return "未检测到状态文件";
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "读取状态失败: " + e.getMessage();
        } finally {
            lock.unlock();
        }
    }
}