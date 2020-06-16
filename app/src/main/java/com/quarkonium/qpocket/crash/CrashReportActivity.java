package com.quarkonium.qpocket.crash;


import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.base.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("checkstyle:membername")
public class CrashReportActivity extends BaseActivity {

    private String exception;
    private String mExcpPid;
    private String mExcpTid;
    private String mExcpTName;

    @Override
    protected int getLayoutResource() {
        return R.layout.layout_bug_report;
    }

    @Override
    public int getActivityTitle() {
        return 0;
    }

    @Override
    protected void onInitialization(Bundle bundle) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        View feedBackBtn = findViewById(R.id.bug_btn_feed_back);
        feedBackBtn.setOnClickListener(v -> finish());

        try {
            exception = exception((Throwable) getIntent().getExtras().get("Stacktrace"));
            mExcpPid = String.valueOf(getIntent().getLongExtra("ProcessId", 0));
            mExcpTid = String.valueOf(getIntent().getLongExtra("ThreadId", 0));
            mExcpTName = getIntent().getStringExtra("ThreadName");
            if (mExcpTName == null) {
                mExcpTName = "null";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    long time = System.currentTimeMillis();
                    store(buildExceptionData(), time + ".err");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

//    private void storeException(long time) {
//        String shell = "logcat -v time";
//        try {
//            Process process = Runtime.getRuntime().exec(shell);
//            InputStream inputStream = process.getInputStream();
//            File dir = getApplicationContext().getExternalFilesDir(null);
//            if (null == dir) {
//                return;
//            }
//
//            File file = new File(dir, time + "_logcat.txt");
//
//            byte[] buffer = new byte[1024];
//            int bytesLeft = 5 * 1024 * 1024;
//            try {
//                FileOutputStream fos = new FileOutputStream(file);
//                try {
//                    while (bytesLeft > 0) {
//                        int read = inputStream.read(buffer, 0, Math.min(bytesLeft,
//                                buffer.length));
//                        if (read == -1) {
//                            throw new EOFException("Unexpected end of data");
//                        }
//                        fos.write(buffer, 0, read);
//                        bytesLeft -= read;
//                    }
//                } finally {
//                    fos.close();
//                }
//            } finally {
//                inputStream.close();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private static String exception(Throwable t) throws IOException {

        if (t == null) {
            return null;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            t.printStackTrace(new PrintStream(stream));
        } finally {
            stream.close();
        }

        return stream.toString("UTF-8");
    }

    private Map<String, String> buildExceptionData() {

        PackageInfo packageInfo = null;
        try {
            PackageManager packageManager = getPackageManager();
            packageInfo = packageManager.getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String versionName = "";
        String versionCode = "";
        if (null != packageInfo) {
            versionName = packageInfo.versionName;
            versionCode = String.valueOf(packageInfo.versionCode);
        }

        Map<String, String> values = new LinkedHashMap<>();
        values.put("VERSION_NAME", versionName);
        values.put("VERSION_CODE", versionCode);
        values.put("PHONE_MODEL", Build.MODEL);
        values.put("PHONE_ROM", Build.VERSION.RELEASE);
        values.put("FINGERPRINT", Build.FINGERPRINT);
        values.put("SDK_VERSION", String.valueOf(Build.VERSION.SDK_INT));
        values.put("PROCESS_ID", mExcpPid);
        values.put("THREAD_ID", mExcpTid);
        values.put("THREAD_NAME", mExcpTName);
        values.put("STACK_TRACE", exception);

        return values;
    }

    private void store(Map<String, String> values, String fileName) throws IOException {

        File dir = getApplicationContext().getExternalFilesDir(null);
        if (null == dir) {
            return;
        }

        File file = new File(dir, fileName);
        OutputStream out;
        try {
            out = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            return;
        }

        OutputStreamWriter writer = null;
        try {
            final StringBuilder buffer = new StringBuilder(200);
            writer = new OutputStreamWriter(out, "ISO8859_1"); //$NON-NLS-1$

            for (final Map.Entry<String, String> entry : values.entrySet()) {
                final String key = entry.getKey();
                dumpString(buffer, key);
                buffer.append('=');
                dumpString(buffer, entry.getValue());
                buffer.append(LINE_SEPARATOR);
                writer.write(buffer.toString());
                buffer.setLength(0);
            }
            writer.flush();
        } finally {
            if (null != writer) {
                writer.close();
            }
            out.close();
        }
    }

    private static final String LINE_SEPARATOR = "\n";

    private void dumpString(StringBuilder buffer, String string) {
        buffer.append(string);
    }

//    private void dumpString_Old(StringBuilder buffer, String string, boolean key) {
//        int i = 0;
//        if (!key && i < string.length() && string.charAt(i) == ' ') {
//            buffer.append("\\ "); //$NON-NLS-1$
//            i++;
//        }
//
//        for (;
//             i < string.length();
//             i++) {
//            char ch = string.charAt(i);
//            switch (ch) {
//                case '\t':
//                    buffer.append("\\t"); //$NON-NLS-1$
//                    break;
//                case '\n':
//                    buffer.append("\\n"); //$NON-NLS-1$
//                    break;
//                case '\f':
//                    buffer.append("\\f"); //$NON-NLS-1$
//                    break;
//                case '\r':
//                    buffer.append("\\r"); //$NON-NLS-1$
//                    break;
//                default:
//                    if ("\\#!=:".indexOf(ch) >= 0 || (key && ch == ' ')) {
//                        buffer.append('\\');
//                    }
//                    if (ch >= ' ' && ch <= '~') {
//                        buffer.append(ch);
//                    } else {
//                        final String hex = Integer.toHexString(ch);
//                        buffer.append("\\u"); //$NON-NLS-1$
//                        for (int j = 0;
//                             j < 4 - hex.length();
//                             j++) {
//                            buffer.append("0"); //$NON-NLS-1$
//                        }
//                        buffer.append(hex);
//                    }
//            }
//        }
//    }
}

