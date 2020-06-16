//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.quarkonium.qpocket.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.text.TextUtils;

import com.orhanobut.logger.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class BitmapUtils {
    private static final String TAG = BitmapUtils.class.getSimpleName();
    private static final float PRECISION = 0.01F;
    public static final int QUALITY_MAX = 100;

    private BitmapUtils() {
    }

    public static byte[] bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 95, baos);
        return baos.toByteArray();
    }

    public static Options getDecodeOptions(String jpegPath, int maxLength) {
        Options options = new Options();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(jpegPath, options);
        if (!options.mCancel && options.outWidth != -1 && options.outHeight != -1) {
            options.inSampleSize = getSampleSize(options, maxLength, true);
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inPreferredConfig = Config.ARGB_8888;
            return options;
        } else {
            return null;
        }
    }

    public static Options getDecodeOptions(int orgWidth, int orgHeight, int maxLength) {
        Options options = new Options();
        int orgMaxLength = orgWidth;
        if (orgWidth < orgHeight) {
            orgMaxLength = orgHeight;
        }

        if (orgMaxLength < maxLength) {
            return options;
        } else {
            float scale = (float) maxLength / (float) orgMaxLength;
            int outWidth = (int) ((float) orgWidth * scale);
            int outHeight = (int) ((float) orgHeight * scale);
            options.outWidth = outWidth;
            options.outHeight = outHeight;
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inPreferredConfig = Config.ARGB_8888;
            return options;
        }
    }

    public static Bitmap scalePicture(String jpegPath, int maxLength, boolean readExifOrientation) {
        if (maxLength < 10 || maxLength > 5000) {
            throw new IllegalArgumentException("length must between [10,5000],but value is:" + maxLength);
        } else {
            Bitmap bitmap;
            try {
                Options orientation = new Options();
                orientation.inSampleSize = 1;
                orientation.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(jpegPath, orientation);
                if (orientation.mCancel || orientation.outWidth == -1 || orientation.outHeight == -1) {
                    return null;
                }

                orientation.inSampleSize = getSampleSize(orientation, maxLength, true);
                orientation.inJustDecodeBounds = false;
                orientation.inDither = false;
                orientation.inPreferredConfig = Config.ARGB_8888;
                bitmap = BitmapFactory.decodeFile(jpegPath, orientation);
            } catch (OutOfMemoryError var8) {
                Logger.e(TAG, var8);
                return null;
            }

            int orientation1 = 0;
            if (readExifOrientation) {
                try {
                    ExifInterface e = new ExifInterface(jpegPath);
                    int oriTag = e.getAttributeInt("Orientation", 1);
                    orientation1 = Exif.getOrientation(oriTag);
                } catch (IOException var7) {
                    orientation1 = 0;
                }
            }

            return scaleBitmap(bitmap, maxLength, orientation1);
        }
    }

    public static Bitmap scalePicture(byte[] jpegData, int maxLength, boolean readExifOrientation) {
        if (jpegData == null) {
            return null;
        } else if (maxLength < 10 || maxLength > 5000) {
            throw new IllegalArgumentException("length must between [10,5000],but value is:" + maxLength);
        } else {
            Bitmap bitmap;
            try {
                Options orientation = new Options();
                orientation.inSampleSize = 1;
                orientation.inJustDecodeBounds = true;
                ByteArrayInputStream in01 = new ByteArrayInputStream(jpegData);
                BitmapFactory.decodeStream(in01, (Rect) null, orientation);
                if (orientation.mCancel || orientation.outWidth == -1 || orientation.outHeight == -1) {
                    return null;
                }

                orientation.inSampleSize = getSampleSize(orientation, maxLength, true);
                orientation.inJustDecodeBounds = false;
                orientation.inDither = false;
                orientation.inPreferredConfig = Config.ARGB_8888;
                ByteArrayInputStream in02 = new ByteArrayInputStream(jpegData);
                bitmap = BitmapFactory.decodeStream(in02, (Rect) null, orientation);
            } catch (OutOfMemoryError var8) {
                Logger.e(TAG, var8);
                return null;
            }

            int orientation1 = 0;
            if (readExifOrientation) {
                orientation1 = Exif.getOrientation(jpegData);
            }

            return scaleBitmap(bitmap, maxLength, orientation1);
        }
    }

    public static Bitmap scaleBitmap(String path, int screenWidth, int screenHeight) {
        Options ops = new Options();
        ops.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, ops);
        int originalWidth = ops.outWidth;
        int originalHeight = ops.outHeight;
        int orgPix = originalWidth * originalHeight;
        int dstPix = screenWidth * screenHeight;
        float fScale = (float) Math.sqrt((double) orgPix * 1.0D / (double) dstPix);
        ops.inSampleSize = (int) ((double) fScale + 0.8D);
        ops.inJustDecodeBounds = false;
        ops.inDither = false;
        ops.inPreferredConfig = Config.ARGB_8888;
        return BitmapFactory.decodeFile(path, ops);
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int maxLength, int orientation) {
        if (maxLength < 10 || maxLength > 5000) {
            throw new IllegalArgumentException("length must between [10,5000],but value is:" + maxLength);
        } else if (bitmap == null) {
            return null;
        } else {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale;
            if (width >= height) {
                scale = (float) maxLength / (float) width;
            } else {
                scale = (float) maxLength / (float) height;
            }

            return Math.abs(scale - 1.0F) < 0.01F ? bitmap : scaleBitmap(bitmap, scale, scale, orientation);
        }
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, float sx, float sy) {
        if (bitmap == null) {
            return null;
        } else {
            Matrix matrix = new Matrix();
            matrix.postScale(sx, sy);
            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (newBitmap != bitmap) {
                bitmap.recycle();
            }

            return newBitmap;
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        if (bitmap != null && orientation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate((float) orientation);
            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (newBitmap != bitmap) {
                bitmap.recycle();
            }

            return newBitmap;
        } else {
            return bitmap;
        }
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, float sx, float sy, int orientation) {
        if (bitmap == null) {
            return null;
        } else {
            Matrix matrix = new Matrix();
            matrix.postScale(sx, sy);
            if (orientation != 0) {
                matrix.postRotate((float) orientation);
            }

            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (newBitmap != bitmap) {
                bitmap.recycle();
            }

            return newBitmap;
        }
    }

    public static Bitmap zoomAndRotate(Bitmap org, int w, int h, int orientation) {
        int orgW = org.getWidth();
        int orgH = org.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = (float) w / (float) orgW;
        float scaleHeight = (float) h / (float) orgH;
        matrix.reset();
        if (orientation != 0) {
            matrix.postRotate((float) orientation);
        }

        matrix.postScale(scaleWidth, scaleHeight);
        if (orgH > 0 && orgW > 0) {
            try {
                return Bitmap.createBitmap(org, 0, 0, orgW, orgH, matrix, true);
            } catch (Exception var10) {
                Logger.e(TAG, var10);
                throw new IllegalStateException("orgSize : " + orgW + "x" + orgH + ", size : " + w + "x" + h + ", matrix : " + matrix.toShortString());
            }
        } else {
            throw new IllegalArgumentException("Width or Heigth < 0:" + orgH + "/" + orgW + "/" + matrix.toString() + "/" + w + "/" + h + "/" + orientation);
        }
    }

    private static int getSampleSize(Options options, int size, boolean useMaxSize) {
        int maxSize = options.outWidth;
        if (useMaxSize && options.outWidth < options.outHeight) {
            maxSize = options.outHeight;
        } else if (!useMaxSize && options.outWidth > options.outHeight) {
            maxSize = options.outHeight;
        }

        int sampleSize;
        for (sampleSize = 1; maxSize / size >= 2; maxSize >>= 1) {
            sampleSize <<= 1;
        }

        return sampleSize;
    }

    public static Bitmap makeTextBitmap(String textString, float fontSize) {
        byte fontColor = -1;
        String fontName = "黑体";
        Rect rcText = new Rect();
        Typeface font = Typeface.create(fontName, 0);
        Paint paint = new Paint(1);
        paint.setColor(fontColor);
        paint.setTypeface(font);
        paint.setTextSize(fontSize);
        paint.setShadowLayer(0.5F, 1.0F, 1.0F, -16777216);
        paint.getTextBounds(textString, 0, textString.length(), rcText);
        int w = rcText.width() + 6;
        int h = rcText.height() + 6;
        Bitmap bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas canvasTemp = new Canvas(bmp);
        canvasTemp.drawColor(0);
        canvasTemp.drawARGB(0, 255, 255, 255);
        canvasTemp.drawText(textString, 2.0F, (float) (h - 3), paint);
        return bmp;
    }

    public static Bitmap getRotateBitmap(Bitmap oldBitmap, String path) {
        short rotate = 0;

        try {
            ExifInterface matrix = new ExifInterface(path);
            int rotateBitmap = matrix.getAttributeInt("Orientation", 0);
            switch (rotateBitmap) {
                case 3:
                    rotate = 180;
                    break;
                case 6:
                    rotate = 90;
                    break;
                case 8:
                    rotate = 270;
            }
        } catch (Exception var5) {
            var5.printStackTrace();
        }

        if (oldBitmap != null && rotate > 0) {
            Matrix matrix1 = new Matrix();
            matrix1.setRotate((float) rotate);
            Bitmap rotateBitmap1 = Bitmap.createBitmap(oldBitmap, 0, 0, oldBitmap.getWidth(), oldBitmap.getHeight(), matrix1, true);
            if (rotateBitmap1 != null && rotateBitmap1 != oldBitmap) {
                oldBitmap.recycle();
                return rotateBitmap1;
            }
        }

        return oldBitmap;
    }

    public static Bitmap getMirrorBitmap(Bitmap bmp) {
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        Bitmap bitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        Camera camera = new Camera();
        Matrix matrix = new Matrix();
        camera.rotateY(180.0F);
        camera.getMatrix(matrix);
        matrix.postTranslate((float) bmp.getWidth(), 0.0F);
        canvas.save();
        canvas.drawBitmap(bmp, matrix, paint);
        canvas.restore();
        return bitmap;
    }

    public static Bitmap getRotateBitmap(Bitmap oldBitmap, int rotate) {
        if (oldBitmap != null && rotate > 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate((float) rotate);
            Bitmap rotateBitmap = Bitmap.createBitmap(oldBitmap, 0, 0, oldBitmap.getWidth(), oldBitmap.getHeight(), matrix, true);
            return rotateBitmap;
        } else {
            return oldBitmap;
        }
    }

    /**
     * 保存图片至文件
     */
    public static boolean saveBitmap2JpegFile(final Bitmap bmp, final String filename, boolean isJpeg) {
        if (isJpeg) {
            return saveBitmap2JpegFile(bmp, filename, Bitmap.CompressFormat.JPEG, 95);
        } else {
            return saveBitmap2JpegFile(bmp, filename, Bitmap.CompressFormat.PNG, 100);
        }
    }

    /**
     * 保存图片至文件
     */
    public static boolean saveBitmap2JpegFile(final Bitmap bmp, final String filename, Bitmap.CompressFormat format, int quality) {
        if (bmp == null || TextUtils.isEmpty(filename)) {
            return false;
        }

        boolean ret = false;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            bmp.compress(format, quality, out);
            out.flush();
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }
}
