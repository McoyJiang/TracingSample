package com.danny_jiang.tracinglibrary.util;

import android.content.Context;
import android.util.Log;

import com.danny_jiang.tracinglibrary.BuildConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Log Utils--used to replace android.util.Log
 * @author danny.jiang
 */
public class LogUtils {
    private static String LOG_TAG = "Tracing";
    private static char LOG_TYPE = 'v';

    /****************************
     * Warn
     *********************************/
    public static void w(Object msg) {
        w(LOG_TAG, msg);
    }

    public static void w(String tag, Object msg) {
        w(tag, msg, null);
    }

    public static void w(String tag, Object msg, Throwable tr) {
        log(tag, msg.toString(), tr, 'w');
    }

    /***************************
     * Error
     ********************************/
    public static void e(Object msg) {
        e(LOG_TAG, msg);
    }

    public static void e(String tag, Object msg) {
        e(tag, msg, null);
    }

    public static void e(String tag, Object msg, Throwable tr) {
        log(tag, msg.toString(), tr, 'e');
    }

    /***************************
     * Debug
     ********************************/
    public static void d(Object msg) {
        d(LOG_TAG, msg);
    }

    public static void d(String tag, Object msg) {// 调试信息
        d(tag, msg, null);
    }

    public static void d(String tag, Object msg, Throwable tr) {
        log(tag, msg.toString(), tr, 'd');
    }

    /****************************
     * Info
     *********************************/
    public static void i(Object msg) {
        i(LOG_TAG, msg);
    }

    public static void i(String tag, Object msg) {
        i(tag, msg, null);
    }

    public static void i(String tag, Object msg, Throwable tr) {
        log(tag, msg.toString(), tr, 'i');
    }

    /**************************
     * Verbose
     ********************************/
    public static void v(Object msg) {
        v(LOG_TAG, msg);
    }

    public static void v(String tag, Object msg) {
        v(tag, msg, null);
    }

    public static void v(String tag, Object msg, Throwable tr) {
        log(tag, msg.toString(), tr, 'v');
    }

    /**
     * @param tag
     * @param msg
     * @param level
     */
    private static void log(String tag, String msg, Throwable tr, char level) {
        if (BuildConfig.DEBUG) {
            if ('e' == level && ('e' == LOG_TYPE || 'v' == LOG_TYPE)) {
                Log.e(tag, createMessage(msg), tr);
            } else if ('w' == level && ('w' == LOG_TYPE || 'v' == LOG_TYPE)) {
                Log.w(tag, createMessage(msg), tr);
            } else if ('d' == level && ('d' == LOG_TYPE || 'v' == LOG_TYPE)) {
                Log.d(tag, createMessage(msg), tr);
            } else if ('i' == level && ('d' == LOG_TYPE || 'v' == LOG_TYPE)) {
                Log.i(tag, createMessage(msg), tr);
            } else {
                Log.v(tag, createMessage(msg), tr);
            }
        }
    }

    private static String getFunctionName() {
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();
        if (sts == null) {
            return null;
        }
        for (StackTraceElement st : sts) {
            if (st.isNativeMethod()) {
                continue;
            }
            if (st.getClassName().equals(Thread.class.getName())) {
                continue;
            }
            if (st.getFileName().equals("LogUtils.java")) {
                continue;
            }
            return "[" + Thread.currentThread().getName() + "("
                    + Thread.currentThread().getId() + "): " + st.getFileName()
                    + ":" + st.getLineNumber() + "]";
        }
        return null;
    }

    private static String createMessage(String msg) {
        String functionName = getFunctionName();
        String message = (functionName == null ? msg
                : (functionName + " - " + msg));
        return message;
    }

    public static void logToFile(Context context, String text) {
        File logFile = new File(context.getFilesDir() + File.separator + "log.file");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
