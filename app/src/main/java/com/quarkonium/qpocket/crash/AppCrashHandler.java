package com.quarkonium.qpocket.crash;

import android.content.Context;
import android.content.Intent;
import android.os.Process;

import com.orhanobut.logger.Logger;

/**
 * 全局崩溃处理器
 * User: yingyu
 * Date: 12/2/13
 * Time: 2:43 PM
 */
public class AppCrashHandler implements Thread.UncaughtExceptionHandler {
    private Context mContext;

    public AppCrashHandler(Context context) {
        mContext = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {

        Logger.e(throwable, "Fatal");

        String threadName = thread.getName();
        if (threadName == null) {
            threadName = "null";
        }

        Intent intent = new Intent(mContext, CrashReportActivity.class);
        intent.putExtra("Stacktrace", throwable);
        intent.putExtra("ProcessId", (long) Process.myPid());
        intent.putExtra("ThreadId", thread.getId());
        intent.putExtra("ThreadName", threadName);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            mContext.startActivity(intent);
        } catch (SecurityException e) {
            // Do nothing
        }

        Process.killProcess(Process.myPid());
        System.exit(10);
    }
}
