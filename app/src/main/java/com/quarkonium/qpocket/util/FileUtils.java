//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.quarkonium.qpocket.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.text.TextUtils;

import com.orhanobut.logger.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;

public final class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    private FileUtils() {
    }

    public static boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            if (destFile.exists() && !destFile.delete()) {
                Logger.w(TAG, "Delete file failed!");
            }

            FileOutputStream e = new FileOutputStream(destFile);

            try {
                byte[] e1 = new byte[4096];

                int bytesRead;
                while ((bytesRead = inputStream.read(e1)) >= 0) {
                    e.write(e1, 0, bytesRead);
                }
            } catch (Exception var14) {
                var14.printStackTrace();
            } finally {
                e.flush();

                try {
                    e.getFD().sync();
                } catch (IOException var13) {
                    ;
                }

                e.close();
            }

            return true;
        } catch (IOException var16) {
            return false;
        }
    }

    public static void copySingleFile(String srcPath, String destPath) throws IOException {
        if (srcPath != null && destPath != null) {
            copySingleFile(new File(srcPath), new File(destPath));
        } else {
            throw new IOException("path is Null, srcPath=" + srcPath + ",destPath=" + destPath);
        }
    }

    public static void copySingleFile(File srcFile, File destFile) throws IOException {
        File parent = destFile.getParentFile();
        if (!checkFolder(parent)) {
            throw new IOException("Create Folder(" + parent.getAbsolutePath() + ") Failed!");
        } else {
            BufferedInputStream in = null;

            try {
                in = new BufferedInputStream(new FileInputStream(srcFile));
                BufferedOutputStream out = null;

                try {
                    out = new BufferedOutputStream(new FileOutputStream(destFile));
                    byte[] buffer = new byte[8192];
                    boolean len = true;

                    int len1;
                    while ((len1 = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len1);
                    }

                    out.flush();
                } finally {
                    close((OutputStream) out);
                }
            } finally {
                close((InputStream) in);
            }
        }
    }

    public static void copyFolder(File src, File dest) throws IOException {
        Logger.d(TAG, "Copy from: " + src.getAbsolutePath() + " to: " + dest.getAbsolutePath());
        if (src.isDirectory()) {
            checkFolder(dest);
            String[] files = src.list();
            if (null == files || files.length == 0) {
                Logger.d(TAG, "files is empty and can\'t do copy");
                return;
            }

            String[] arr$ = files;
            int len$ = files.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                String file = arr$[i$];
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                copyFolder(srcFile, destFile);
            }
        } else {
            Logger.d(TAG, "Copy file from: " + src.getAbsolutePath() + " to: " + dest.getAbsolutePath());
            copySingleFile(src, dest);
        }

    }

    public static byte[] getFileData(String filePath) throws IOException {
        return getFileData(new File(filePath));
    }

    public static byte[] getFileData(File file) throws IOException {
        BufferedInputStream in = null;

        byte[] var2;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            var2 = getStreamData(in);
        } finally {
            close((InputStream) in);
        }

        return var2;
    }

    public static byte[] getPkgFileData(Context context, String fileName) throws IOException {
        BufferedInputStream in = null;

        byte[] var3;
        try {
            in = new BufferedInputStream(context.openFileInput(fileName));
            var3 = getStreamData(in);
        } finally {
            close((InputStream) in);
        }

        return var3;
    }

    public static boolean checkFolder(String folderPath) {
        return folderPath == null ? false : checkFolder(new File(folderPath));
    }

    public static boolean checkFolder(File folder) {
        return folder == null ? false : (folder.isDirectory() ? true : folder.mkdirs());
    }

    public static String getFileContent(File file) throws IOException {
        BufferedReader in = null;
        long fileSize = file.length();
        if (fileSize > 32767L) {
            fileSize = 32767L;
        }

        StringBuilder sb = new StringBuilder((int) fileSize);

        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
            String line = null;

            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }

            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        } finally {
            if (in != null) {
                in.close();
                in = null;
            }

        }
    }

    public static void writeFileContent(File file, String content) throws Exception {
        FileOutputStream out = new FileOutputStream(file);

        try {
            out.write(content.getBytes("utf-8"));
            out.flush();
        } finally {
            close((OutputStream) out);
        }

    }

    public static void deleteFile(String path) {
        if (null != path && !"".equals(path)) {
            File file = new File(path);
            deleteFile(file);
        } else {
            Logger.e(TAG, "File path is null or not exist, delete file fail!");
        }
    }

    public static void deleteFile(File file) {
        if (null != file && file.exists()) {
            if (file.isDirectory()) {
                deleteFile(file.listFiles());
            }

            if (!file.delete()) {
                Logger.i(TAG, "delete (" + file.getPath() + ") failed!");
            }

        } else {
            Logger.e(TAG, "File is null or not exist, delete file fail!");
        }
    }

    public static void deleteFile(File[] files) {
        if (null != files && files.length != 0) {
            File[] arr$ = files;
            int len$ = files.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                File file = arr$[i$];
                deleteFile(file);
            }

        } else {
            Logger.e(TAG, "Files is null or empty, delete fail!");
        }
    }

    public static void close(InputStream in) throws IOException {
        if (in != null) {
            in.close();
            in = null;
        }

    }

    public static void close(OutputStream out) throws IOException {
        if (out != null) {
            out.close();
            out = null;
        }

    }

    public static byte[] getStreamData(InputStream in) throws IOException {
        ByteArrayOutputStream out = null;

        try {
            out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            boolean len = true;

            int len1;
            while ((len1 = in.read(buffer)) != -1) {
                out.write(buffer, 0, len1);
            }

            out.flush();
            byte[] var4 = out.toByteArray();
            return var4;
        } finally {
            close((OutputStream) out);
        }
    }

    public static boolean checkNomediaFile(String path) throws IOException {
        File nomedia = new File(path + File.separator + ".nomedia");
        return !nomedia.exists() ? nomedia.createNewFile() : true;
    }

    public static boolean saveJpeg(byte[] imgData, String path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path is null");
        } else {
            String tmpPath = path + ".tmp";
            saveFile(imgData, tmpPath);
            return (new File(tmpPath)).renameTo(new File(path));
        }
    }

    public static boolean saveBitmap(String path, Bitmap bitmap, int quality) throws IOException, IllegalArgumentException {
        if (!TextUtils.isEmpty(path) && bitmap != null) {
            boolean flag = false;
            FileOutputStream out = null;

            try {
                out = new FileOutputStream(path);
                if (bitmap.compress(CompressFormat.JPEG, quality, out)) {
                    out.flush();
                    flag = true;
                }
            } catch (FileNotFoundException var9) {
                var9.printStackTrace();
            } finally {
                close((OutputStream) out);
            }

            return flag;
        } else {
            return false;
        }
    }

    public static void saveFile(byte[] data, String path) throws IOException {
        if (data == null) {
            throw new IOException("data is null");
        } else if (path == null) {
            throw new IOException("path is null");
        } else {
            File parent = (new File(path)).getParentFile();
            if (!checkFolder(parent)) {
                throw new IOException("Create Folder(" + parent.getAbsolutePath() + ") Failed!");
            } else {
                BufferedOutputStream out = null;

                try {
                    out = new BufferedOutputStream(new FileOutputStream(path));
                    out.write(data);
                } finally {
                    close((OutputStream) out);
                }

            }
        }
    }

    public static int getLineNumber(File file) {
        LineNumberReader lnr = null;
        int count = 0;

        try {
            lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

            for (Object e = null; lnr.readLine() != null; ++count) {
                ;
            }
        } catch (FileNotFoundException var12) {
            var12.printStackTrace();
        } finally {
            try {
                if (null != lnr) {
                    lnr.close();
                }
            } catch (IOException var11) {
                var11.printStackTrace();
            }

            return count;
        }
    }

    /**
     * @deprecated
     */
    public static void fileScan(Context context, String file) {
        Uri data = Uri.parse("file://" + file);
        context.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", data));
    }

    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        copyStream(is, os, buffer, 8192);
    }

    public static void copyStream(InputStream is, OutputStream os, byte[] buffer, int bufferSize) throws IOException {
        try {
            while (true) {
                int e = is.read(buffer, 0, bufferSize);
                if (e == -1) {
                    return;
                }

                os.write(buffer, 0, e);
            }
        } catch (IOException var5) {
            throw var5;
        }
    }
}
