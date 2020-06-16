/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quarkonium.qpocket.util;

import android.media.ExifInterface;


import com.orhanobut.logger.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("checkstyle:membername")
public class Exif {
    private static final String TAG = "Exif";

    private static final int BUFFER_SIZE = 8192;
    private static final int M_SOI = 0xFFD8; // JPEG的文件头

    // Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
    public static int getOrientation(byte[] jpeg) {
        if (jpeg == null) {
            return 0;
        }

        int offset = 0;
        int length = 0;

        // ISO/IEC 10918-1:1993(E)
        while (offset + 3 < jpeg.length && (jpeg[offset++] & 0xFF) == 0xFF) {
            int marker = jpeg[offset] & 0xFF;

            // Check if the marker is a padding.
            if (marker == 0xFF) {
                continue;
            }
            offset++;

            // Check if the marker is SOI or TEM.
            if (marker == 0xD8 || marker == 0x01) {
                continue;
            }
            // Check if the marker is EOI or SOS.
            if (marker == 0xD9 || marker == 0xDA) {
                break;
            }

            // Get the length and check if it is reasonable.
            length = pack(jpeg, offset, 2, false);
            if (length < 2 || offset + length > jpeg.length) {
                Logger.i(TAG, "Invalid length");
                return 0;
            }

            // Break if the marker is EXIF in APP1.
            if (marker == 0xE1 && length >= 8 && pack(jpeg, offset + 2, 4, false) == 0x45786966
                    && pack(jpeg, offset + 6, 2, false) == 0) {
                offset += 8;
                length -= 8;
                break;
            }

            // Skip other markers.
            offset += length;
            length = 0;
        }

        // JEITA CP-3451 Exif Version 2.2
        if (length > 8) {
            // Identify the byte order.
            int tag = pack(jpeg, offset, 4, false);
            if (tag != 0x49492A00 && tag != 0x4D4D002A) {
                Logger.i(TAG, "Invalid byte order");
                return 0;
            }
            boolean littleEndian = (tag == 0x49492A00);

            // Get the offset and check if it is reasonable.
            int count = pack(jpeg, offset + 4, 4, littleEndian) + 2;
            if (count < 10 || count > length) {
                Logger.i(TAG, "Invalid offset");
                return 0;
            }
            offset += count;
            length -= count;

            // Get the count and go through all the elements.
            count = pack(jpeg, offset - 2, 2, littleEndian);
            while (count-- > 0 && length >= 12) {
                // Get the tag and check if it is orientation.
                tag = pack(jpeg, offset, 2, littleEndian);
                if (tag == 0x0112) {
                    // We do not really care about type and count, do we?
                    int orientation = pack(jpeg, offset + 8, 2, littleEndian);
                    return getOrientation(orientation);
                }
                offset += 12;
                length -= 12;
            }
        }

        Logger.i(TAG, "Orientation not found");
        return 0;
    }

    public static byte[] getExifData(byte[] jpeg) {
        FromTo fromTo = getExifDataFromTo(jpeg);
        if (fromTo == null) {
            return new byte[0];
        }
        byte[] exifdata = new byte[fromTo.length];
        System.arraycopy(jpeg, fromTo.from, exifdata, 0, fromTo.length);
        return exifdata;
    }

    public static byte[] getExifData(String jpegFile) {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(jpegFile));
            int b0 = in.read();
            if (b0 != 0xFF) {
                return new byte[0];
            }

            int b1 = in.read();
            if (b1 != 0xD8) {
                return new byte[0];
            }

            while (in.read() == 0xFF) {
                int type = in.read();
                if (type == -1) {
                    return new byte[0];
                }

                int left = in.read();
                if (left == -1) {
                    return new byte[0];
                }
                int right = in.read();
                if (right == -1) {
                    return new byte[0];
                }

                int appLen = getLen(left, right);
                if (type == 0xE1) {
                    byte[] exif = getExifAppData(in, left, right, appLen);
                    if (exif != null) {
                        return exif;
                    }
                } else if (type >= 0xE0 || type <= 0xEF) {
                    if (!skip(in, appLen - 2)) {
                        return new byte[0];
                    }
                } else {
                    return new byte[0];
                }
            }
            return new byte[0];
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        } finally {
            if (in != null) {
                try {
                    in.close();
                    in = null;
                } catch (IOException e) {
                    Logger.e(TAG, e);
                }
            }
        }
    }

    private static byte[] getExifAppData(InputStream in, int left, int right, int appLen) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        out.write(0xFF);
        out.write(0xE1);
        out.write(left);
        out.write(right);
        int rm = appLen - 2; // app长度
        byte[] buffer = new byte[8 * 1024];
        int rl = -1; // buffer读取长度
        int rc = 0; // 当前读取数量
        int remain = 0;
        while ((remain = rm - rc) > 0) {
            if (remain > 8 * 1024) {
                rl = in.read(buffer);
            } else {
                rl = in.read(buffer, 0, remain);
            }

            if (rl < 0) {
                return new byte[0];
            }
            out.write(buffer, 0, rl);
            rc += rl;
        }

        if (rc != rm) {
            return new byte[0];
        }

        byte[] newExif = out.toByteArray();
        if (newExif.length > 64 * 1024 + 2) {
            return new byte[0];
        }

        if (pack(newExif, 4, 4, false) == 0x45786966 && pack(newExif, 8, 2, false) == 0) {
            return newExif;
        }
        return null;
    }

    private static int getLen(int left, int right) {
        return left << 8 | right;
    }

    private static boolean skip(InputStream in, long count) throws IOException {
        if (count <= 0) {
            return false;
        }

        long sum = 0;
        long remain = 0;
        while ((remain = (count - sum)) > 0) {
            long skip = in.skip(remain);
            if (skip <= 0) {
                return false;
            }
            sum += skip;
        }
        return sum == count;
    }


    public static byte[] exifToJpegData(byte[] jpeg, byte[] exifData) {
        if (!isJpeg(jpeg)) {
            throw new IllegalArgumentException("jpeg data 数据不正确," + jpeg == null ? "jpeg is null!" : String.valueOf(jpeg.length));
        }

        int jpgLength = jpeg.length;

        int offset = 2;
        int length = 0;
        int app0End = offset;
        // 查找Exif字段，如果有则替换
        while (offset + 2 < jpgLength && (jpeg[offset++] & 0xFF) == 0xFF) {
            int marker = jpeg[offset] & 0xFF;
            if (marker == 0xFF) {
                continue;
            }

            if (marker == 0xD8 || marker == 0xD9) {
                break;
            }

            // 在正常范围之外的直接退出
            if (marker < 0xC0 || marker >= 0xFF) {
                break;
            }

            offset++;

            length = pack(jpeg, offset, 2, false);

            if (length < 2 || offset + length > jpgLength) {
                Logger.i(TAG, "Invalid length");
                return jpeg;
            }

            // Exif字段
            if (marker == 0xE1) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(exifData.length + jpeg.length);
                out.write(jpeg, 0, offset - 2);
                out.write(exifData, 0, exifData.length);
                out.write(jpeg, offset + length, jpeg.length - offset - length);
                return out.toByteArray();
            }

            offset += length;
            if (marker == 0xE1) {
                app0End = offset;
            }
            continue;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(exifData.length + jpeg.length);
        out.write(jpeg, 0, app0End);
        out.write(exifData, 0, exifData.length);
        out.write(jpeg, app0End, jpeg.length - app0End);
        return out.toByteArray();
    }

    public static void exifToJpegFile(String jpegFile, String newJpegFile, byte[] exifData) throws IOException {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(jpegFile), BUFFER_SIZE);
            BufferedOutputStream out = null;
            try {
                out = new BufferedOutputStream(new FileOutputStream(newJpegFile), BUFFER_SIZE);
                exifToJpegStream(in, out, exifData);
            } finally {
                if (out != null) {
                    out.close();
                    out = null;
                }
            }
        } finally {
            if (in != null) {
                in.close();
                in = null;
            }
        }
    }

    public static int getOrientationFlag(int degree) {
        int temp = (degree + 45) / 90;
        degree = temp * 90;
        switch (degree) {
            case 0:
                return ExifInterface.ORIENTATION_NORMAL;
            case 90:
                return ExifInterface.ORIENTATION_ROTATE_90;
            case 180:
                return ExifInterface.ORIENTATION_ROTATE_180;
            case 270:
                return ExifInterface.ORIENTATION_ROTATE_270;
            default:
                return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    public static int getOrientation(int orientationFlag) {
        switch (orientationFlag) {
            case ExifInterface.ORIENTATION_NORMAL:
                return 0;
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    public static boolean isJpeg(byte[] jpegData) {
        if (jpegData == null) {
            return false;
        }

        int len = jpegData.length;
        return !(len < 1024 || marker(jpegData, 0) != M_SOI);

    }

    public static int getExifOrientationFlag(String filename) throws IOException {
        ExifInterface exif = new ExifInterface(filename);
        return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    }

    public static int getPhotoOrientation(String filename) {
        try {
            int flag = getExifOrientationFlag(filename);
            return getOrientation(flag);
        } catch (IOException e) {
            Logger.i(TAG, e);
            return 0;
        }
    }

    protected static int pack(byte[] bytes, int offset, int length, boolean littleEndian) {
        int step = 1;
        if (littleEndian) {
            offset += length - 1;
            step = -1;
        }

        int value = 0;
        while (length-- > 0) {
            value = (value << 8) | (bytes[offset] & 0xFF);
            offset += step;
        }
        return value;
    }

    protected static int marker(byte[] bytes, int offset) {
        int value1 = bytes[offset] & 0xFF;
        int value2 = bytes[++offset] & 0xFF;
        return (value1 << 8 | value2);
    }

    protected static byte[] unpack(int value, int len, boolean littleEndian) {
        byte[] bytes = new byte[len];
        if (littleEndian) {
            for (int i = 0;
                 i < len;
                 i++) {
                bytes[i] = (byte) (value & 0xFF);
                value = value >> 8;
            }
        } else {
            for (int i = len - 1;
                 i >= 0;
                 i--) {
                bytes[i] = (byte) (value & 0xFF);
                value = value >> 8;
            }
        }

        return bytes;
    }

    protected static void unpack(byte[] bytes, int value, int len, int offset, boolean littleEndian) {
        if (littleEndian) {
            for (int i = 0;
                 i < len;
                 i++) {
                bytes[offset + i] = (byte) (value & 0xFF);
                value = value >> 8;
            }
        } else {
            for (int i = len - 1;
                 i >= 0;
                 i--) {
                bytes[offset + i] = (byte) (value & 0xFF);
                value = value >> 8;
            }
        }
    }

    private static FromTo getExifDataFromTo(byte[] jpeg) {
        if (!isJpeg(jpeg)) {
            throw new IllegalArgumentException("jpeg data 数据不正确");
        }
        int jpgLength = jpeg.length;

        int offset = 2;
        int length = 0;
        while (offset + 2 < jpgLength && (jpeg[offset++] & 0xFF) == 0xFF) {
            int marker = jpeg[offset] & 0xFF;
            if (marker == 0xFF) {
                continue;
            }

            if (marker == 0xD8 || marker == 0xD9) {
                break;
            }

            // 在正常范围之外的直接退出
            if (marker < 0xC0 || marker >= 0xFF) {
                break;
            }

            offset++;

            length = pack(jpeg, offset, 2, false);

            if (length < 2 || offset + length > jpgLength) {
                Logger.i(TAG, "Invalid length");
                return null;
            }

            // 如果为Exif字段
            if (marker == 0xE1 && length >= 8 && pack(jpeg, offset + 2, 4, false) == 0x45786966
                    && pack(jpeg, offset + 6, 2, false) == 0) {
                FromTo fromTo = new FromTo();
                fromTo.from = offset - 2;
                fromTo.length = length + 2;
                return fromTo;
            }

            offset += length;
            continue;
        }

        return null;
    }

    @SuppressWarnings("checkstyle:membername")
    private static class FromTo {
        private int from;
        private int length;
    }

    /**
     * 只针对特效图片的处理,基本不占用内存
     *
     * @param in
     * @param out
     * @param exifData
     */
    private static void exifToJpegStream(InputStream in, OutputStream out, byte[] exifData) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len = in.read(buffer);
        if (len == -1) {
            return;
        }

        int total = len;
        if (len < BUFFER_SIZE) {
            while ((len = in.read(buffer, total, BUFFER_SIZE - total)) != -1 && total < BUFFER_SIZE) {
                total += len;
            }
        }

        // 检验代码
        // jpeg Data验证
        int soi = marker(buffer, 0);
        if (soi != M_SOI) {
            throw new IllegalArgumentException("jpeg data 数据不正确");
        }

        int offset = 2;
        int app0End = offset;
        boolean haveExif = false;
        long skip = 0;
        while (offset + 3 <= total && (buffer[offset++] & 0xFF) == 0xFF) {
            int mark = buffer[offset++] & 0xFF;
            if (mark >= 0xE0 && mark <= 0xEF) {
                if (mark == 0xE1) {
                    int appSize = pack(buffer, offset, 2, false);
                    haveExif = true;
                    out.write(buffer, 0, offset - 2);
                    out.write(exifData, 0, exifData.length);
                    if (offset + appSize > total) {
                        skip = offset + appSize - total;
                    } else if (offset + appSize < total) {
                        out.write(buffer, offset + appSize, total - appSize - offset);
                    }
                    break;
                } else {
                    int appSize = pack(buffer, offset, 2, false);
                    offset += appSize;
                    if (mark == 0xE0) {
                        app0End = offset;
                    }
                }
            } else {
                break;
            }
        }

        if (!haveExif) {
            out.write(buffer, 0, app0End);
            out.write(exifData, 0, exifData.length);
            if (app0End < total) {
                out.write(buffer, app0End, total - app0End);
            }
        }

        if (skip > 0) {
            int tSkip = 0;
            while (skip > tSkip) {
                long s = in.skip(skip - tSkip);
                tSkip += s;
            }
        }

        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

//    public static byte[] getDefaultPGExifData(PictureInfo pictureInfo, int orientation) {
//        PGExifInfo exifInfo = new PGExifInfo(pictureInfo.getExifData());
//        exifInfo.setMake(Build.BRAND);
//        exifInfo.setModel(Build.MODEL);
//        exifInfo.setOrientation(orientation);
//        exifInfo.setTimestamp(pictureInfo.getTakenTime());
//        return exifInfo.getExifData();
//    }
}
