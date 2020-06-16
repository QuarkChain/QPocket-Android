package com.quarkonium.qpocket.util;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;

/**
 * 工具类
 */
public class ToolUtils {

    // 上次点击的时间
    private static long lastClickTime;

    public static int getViewWidth(View view) {
        int width = 0;
        try { //记得以前调用这个方法会在某些手机中抛异常，所以保险为主
            int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(w, h);
            width = view.getMeasuredWidth();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return width;
    }


    public static Bitmap getBitmap(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        return getRotateBitmap(bitmap, getRotatedDegree(path));
    }

    public static int getRotatedDegree(String path) {
        int rotate = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int result = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            switch (result) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }

    public static Bitmap getRotateBitmap(Bitmap oldBitmap, int degree) {

        if (oldBitmap != null && degree > 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate(degree);
            Bitmap rotateBitmap = Bitmap.createBitmap(oldBitmap, 0, 0, oldBitmap.getWidth(), oldBitmap.getHeight(),
                    matrix, true);
            if (rotateBitmap != null && rotateBitmap != oldBitmap) {
                oldBitmap.recycle();
                return rotateBitmap;
            }
        }
        return oldBitmap;
    }

    public static String getFileDir(Context context, String uniqueName) {
        String filePath = context.getFilesDir().getAbsolutePath();
        return filePath + File.separator + uniqueName;

    }

    public static int[] getColorArray(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        int[] argbArray = new int[bitmap.getWidth() * bitmap.getHeight()];

        bitmap.getPixels(argbArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        return argbArray;
    }

    public static Bitmap addTextOnBitmap(Bitmap bitmap, String text, int color, float textSize, float left, float top) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.SANS_SERIF);

        canvas.drawText(text, left, top, paint);
        return bitmap;

    }

    public static Bitmap addTextOnBitmapWidthCopy(Bitmap bitmap, String text, int color, float textSize, float left, float top) {
        return addTextOnBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, true), text, color, textSize, left, top);
    }

    public static Bitmap addTextOnBitmap(int drawable, Context context, String text, int color, float textSize, float left, float top) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawable);
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        return addTextOnBitmap(bitmap, text, color, textSize, left, top);
    }

    private static String removeSpaceFromString(final String str) {
        if (str == null) {
            return "";
        }
        return str.replaceAll("\\s", "");
    }

    public static String getTagString(String[] tagArray) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String tag : tagArray) {
            stringBuilder.append(tag).append(",");
        }
        stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
        return stringBuilder.toString();

    }

    public static boolean isWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetworkInfo.isConnected();

    }

    public static byte[] getByteArray(String path) {
        InputStream is = null;
        URL url;
        try {
            url = new URL(path);
            URLConnection urlConnection = url.openConnection();
            is = urlConnection.getInputStream();
            return getByteArray(is);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    private static byte[] getByteArray(InputStream is) {
        byte[] array = null;
        ByteArrayOutputStream baos = null;
        try {
            int count;
            byte[] bytearray = new byte[1024];
            baos = new ByteArrayOutputStream();
            while ((count = is.read(bytearray)) != -1) {
                baos.write(bytearray, 0, count);
            }
            array = baos.toByteArray();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (array != null && array.length == 0) {
            return null;
        }
        return array;
    }

    /**
     * 是否是快速点击
     *
     * @return true 快速点击 false 常规点击
     */
    public static boolean isFastDoubleClick() {
        return isFastDoubleClick(600);
    }

    public static boolean isFastDoubleClick(long fastTime) {
        long time = System.currentTimeMillis();
        if (Math.abs(time - lastClickTime) > fastTime) {
            lastClickTime = time;
            return false;
        }
        return true;
    }


    public static boolean fileExists(final String path) {
        File file = new File(path);
        return file.exists();
    }

//    public static void openApp(Context context, String packageName, String url) {
//        // 通过包名获取要跳转的app，创建intent对象
//        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
//        PackageInfo packageinfo = null;
//        try {
//            packageinfo = context.getPackageManager().getPackageInfo(packageName, 0);
//        } catch (PackageManager.NameNotFoundException e) {
//            JumpUtils.openExplore(context, url);
//            e.printStackTrace();
//            return;
//        }
//        if (packageinfo == null) {
//            JumpUtils.openExplore(context, url);
//            return;
//        }
//        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageinfo.packageName);
//        // 这里如果intent为空，就说名没有安装要跳转的应用嘛
//        if (intent != null) {
//            // 这里跟Activity传递参数一样的嘛，不要担心怎么传递参数，还有接收参数也是跟Activity和Activity传参数一样
//            context.startActivity(intent);
//        } else {
//            JumpUtils.openExplore(context, url);
//        }
//    }

    private static String getAppLanguage(Context context) {
        return SharedPreferencesUtils.getCurrentLanguages(context);
    }

    public static boolean isZh(Context context) {
        String localeLanguage = getAppLanguage(context);
        if (ConstantLanguages.AUTO.equals(localeLanguage)) {
            String language = context.getString(R.string.cn_current_language);
            return "中文".equals(language);
        } else {
            return ConstantLanguages.SIMPLIFIED_CHINESE.equals(localeLanguage);
        }
    }

    public static boolean isKo(Context context) {
        return isLanguage(context, "ko");
    }

    public static boolean isRu(Context context) {
        return isLanguage(context, "ru");
    }

    public static boolean isIn(Context context) {
        return isLanguage(context, "in");
    }

    public static boolean isVi(Context context) {
        return isLanguage(context, "vi");
    }

    private static boolean isLanguage(Context context, String languageStr) {
        String localeLanguage = getAppLanguage(context);
        if (ConstantLanguages.AUTO.equals(localeLanguage)) {
            Locale locale;
            Configuration conf = context.getResources().getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locale = conf.getLocales().get(0);
            } else {
                locale = conf.locale;
            }
            String language = locale.getLanguage();
            return language.toLowerCase().equals(languageStr);
        } else {
            return languageStr.equals(localeLanguage);
        }
    }

    //是否超过一天的时间限制
    public static boolean isLongDayTime(long time) {
        return Math.abs(time - System.currentTimeMillis()) > Constant.ONE_DAY_TIME;
    }

    //*****************************数字价格**********************************************
    public static String getCoinSymbol(String coin) {
        switch (coin) {
            case "btc":
                return "Ƀ";
            case "eth":
                return "Ξ";
            case "usd":
                return "$"; //美元
            case "cny":
                return "¥"; //人民币
            case "eur":
                return "€"; //欧元
            case "krw":
                return "₩"; //韩元

            case "aed":
                return "د.إ"; //阿联酋迪拉姆
            case "ars":
                return "$";//阿根廷比索
            case "aud":
                return "$";//澳大利亚元
            case "bdt":
                return "৳";//孟加拉塔卡
            case "bhd":
                return "ب.د";//巴林第纳尔
            case "bmd":
                return "$";//百慕大美元
            case "brl":
                return "R$";//巴西雷亚尔
            case "cad":
                return "$";//加拿大元
            case "chf":
                return "CHF";
            case "clp":
                return "$";
            case "czk":
                return "Kč";
            case "dkk":
                return "kr.";
            case "gbp":
                return "£"; //英镑
            case "hkd":
                return "$";
            case "huf":
                return "Ft";
            case "idr":
                return "Rp";
            case "ils":
                return "₪";
            case "inr":
                return "₹";
            case "jpy":
                return "¥"; //日元
            case "kwd":
                return "د.ك";
            case "lkr":
                return "₨";
            case "mmk":
                return "K";
            case "mxn":
                return "$";
            case "myr":
                return "RM";
            case "nok":
                return "kr";
            case "nzd":
                return "$";
            case "php":
                return "₱";
            case "pkr":
                return "₨";
            case "pln":
                return "zł";
            case "rub":
                return "\u20BD";
            case "sar":
                return "ر.س";
            case "sek":
                return "kr";
            case "sgd":
                return "$";
            case "thb":
                return "฿";
            case "try":
                return "₺";
            case "twd":
                return "$";
            case "uah":
                return "₴";
            case "vef":
                return "BS.F"; //委内瑞拉 玻利瓦尔
            case "vnd":
                return "₫";
            case "xag":
                return "oz t";
            case "xau":
                return "oz t";
            case "xdr":
                return "SDR";
            case "zar":
                return "R";
        }
        return "¤";
    }

    /**
     * 数字加分隔
     *
     * @param numStr:字符串格式的数字
     * @param divider:分隔的字符
     * @param num:分隔的位数
     */
    private static String addDivider(String numStr, String divider, int num) {
        if (TextUtils.isEmpty(numStr)) {
            return null;
        }
        String[] strs = null;
        StringBuilder sb1;
        if (numStr.contains(".")) {
            strs = numStr.split("\\.");
            sb1 = new StringBuilder(strs[0]);
        } else {
            sb1 = new StringBuilder(numStr);
        }

        StringBuilder sb2 = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        for (int i = 0; i < sb1.length(); i = 0) {

            if (sb1.length() > num) {
                temp.append(divider);
                temp.append(sb1.substring(sb1.length() - num, sb1.length()));
                sb2.insert(0, temp);
                sb1.delete(sb1.length() - num, sb1.length());
            } else {
                sb2.insert(0, sb1);
                break;
            }
            temp.delete(0, temp.length());

        }

        if (strs != null) {
            return sb2.append(".").append(strs[1]).toString();
        } else {
            return sb2.toString();
        }
    }

    //保留两位有效小数
    public static String format2Number(String num) {
        if (TextUtils.isEmpty(num)) {
            return "0";
        }
        BigDecimal value = new BigDecimal(num).setScale(2, BigDecimal.ROUND_HALF_UP);
        return value.compareTo(BigDecimal.ZERO) == 0 ? "0" : value.stripTrailingZeros().toPlainString();
    }

    //保留8位有效小数
    private static String formatNumber(String num) {
        if (TextUtils.isEmpty(num)) {
            return "0";
        }
        double n = Double.parseDouble(num);
        if (Double.isInfinite(n)) {
            return "0";
        }
        if (Math.abs(n) > 1) {
            //大于1只保留8位有效小数
            return format2Number(num);
        } else {
            //小于1保留8位有效小数
            BigDecimal value = new BigDecimal(num).setScale(8, BigDecimal.ROUND_HALF_UP);
            return value.compareTo(BigDecimal.ZERO) == 0 ? "0" : value.stripTrailingZeros().toPlainString();
        }
    }

    //Coin 市值详情 进行缩写
    public static String getCurrentSpiltPrice(Context context, String price) {
        String coin = SharedPreferencesUtils.getCurrentMarketCoin(context);
        if (!TextUtils.isEmpty(price)) {
            double value = Double.parseDouble(price);
            if (ToolUtils.isZh(context)) {
                if (value >= 100000000) {
                    int v = (int) (value / 1000000f);
                    price = new BigDecimal(v + "").multiply(new BigDecimal("0.01")).stripTrailingZeros().toPlainString() + "亿";
                } else if (value > 10000) {
                    int v = (int) (value / 100f);
                    price = new BigDecimal(v + "").multiply(new BigDecimal("0.01")).stripTrailingZeros().toPlainString() + "万";
                } else {
                    price = new BigDecimal(price).setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString();
                }
            } else {
                if (value >= 1000000) {
                    int v = (int) (value / 10000f);
                    price = new BigDecimal(v + "").multiply(new BigDecimal("0.01")).stripTrailingZeros().toPlainString();
                    price = addDivider(price, ",", 3) + "M";
                } else if (value > 1000) {
                    int v = (int) (value);
                    price = new BigDecimal(v + "").multiply(new BigDecimal("0.01")).stripTrailingZeros().toPlainString();
                    price = addDivider(price, ",", 3) + "k";
                } else {
                    price = new BigDecimal(value + "").setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString();
                    price = addDivider(price, ",", 3);
                }
            }
        } else {
            price = "0";
        }
        return getCoinSymbol(coin) + price;
    }

    //Coin 市值详情 进行缩写
    public static String getSpiltNumber(Context context, String number) {
        if (TextUtils.isEmpty(number)) {
            return "0";
        }
        double value = Double.parseDouble(number);
        if (ToolUtils.isZh(context)) {
            if (value >= 100000000) {
                int v = (int) (value / 1000000f);
                return new BigDecimal(v + "").multiply(new BigDecimal("0.01")).stripTrailingZeros().toPlainString() + "亿";
            } else if (value > 10000) {
                int v = (int) (value / 100f);
                return new BigDecimal(v + "").multiply(new BigDecimal("0.01")).stripTrailingZeros().toPlainString() + "万";
            } else {
                return new BigDecimal(number).setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString();
            }
        } else {
            if (value >= 1000000) {
                int v = (int) (value / 10000f);
                number = new BigDecimal(v + "").multiply(new BigDecimal("0.01")).stripTrailingZeros().toPlainString();
                return addDivider(number, ",", 3) + "M";
            } else if (value > 1000) {
                int v = (int) (value);
                number = new BigDecimal(v + "").multiply(new BigDecimal("0.01")).stripTrailingZeros().toPlainString();
                return addDivider(number, ",", 3) + "k";
            } else {
                number = new BigDecimal(value + "").setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString();
                return addDivider(number, ",", 3);
            }
        }
    }

    //当前Coin价格
    public static String getCurrentPrice(Context context, String price) {
        String coin = SharedPreferencesUtils.getCurrentMarketCoin(context);
        if (!TextUtils.isEmpty(price)) {
            price = formatNumber(price);
            if (!ToolUtils.isZh(context)) {
                price = addDivider(price, ",", 3);
            }
        } else {
            price = "0";
        }
        return getCoinSymbol(coin) + price;
    }

    //********钱包相关界面价格*********
    public static boolean isTestNetwork(String address) {
        if (WalletUtils.isQKCValidAddress(address)) {
            return Constant.sNetworkId.intValue() != Constant.QKC_PUBLIC_MAIN_INDEX;
        } else if (WalletUtils.isValidAddress(address)) {
            return Constant.sETHNetworkId != Constant.ETH_PUBLIC_PATH_MAIN_INDEX;
        }
        return false;
    }

    public static String getTokenCurrentCoinPriceText(Context context, boolean isTestNetwork, String tokenSymbol, String count) {
        //获取当前选定法币
        String currentPriceCoin = SharedPreferencesUtils.getCurrentMarketCoin(context);
        //获取法币简称符号
        String coinSymbol = ToolUtils.getCoinSymbol(currentPriceCoin);

        float price = 0;
        if (!TextUtils.equals("0", count)) {
            if (!isTestNetwork) {
                //获取当前法币价格
                price = SharedPreferencesUtils.getCoinPrice(context, tokenSymbol.toLowerCase(), currentPriceCoin);
            }
            price = Float.parseFloat(count) * price;
        }

        return Constant.PRICE_ABOUT + coinSymbol + ToolUtils.format8Number(price);
    }

    public static String getTokenCurrentCoinPriceText(Context context, String walletAddress, String tokenSymbol, String count) {
        return getTokenCurrentCoinPriceText(context, isTestNetwork(walletAddress), tokenSymbol, count);
    }


    public static String getQKCTokenCurrentCoinPriceText(Context context, String tokenSymbol, String count) {
        return getTokenCurrentCoinPriceText(context, Constant.sNetworkId.intValue() != Constant.QKC_PUBLIC_MAIN_INDEX, tokenSymbol, count);
    }

    public static String getETHTokenCurrentCoinPriceText(Context context, String tokenSymbol, String count) {
        return getTokenCurrentCoinPriceText(context, Constant.sETHNetworkId != Constant.ETH_PUBLIC_PATH_MAIN_INDEX, tokenSymbol, count);
    }

    public static String getTRXTokenCurrentCoinPriceText(Context context, String tokenSymbol, String count) {
        return getTokenCurrentCoinPriceText(context, false, tokenSymbol, count);
    }

    public static String getBTCTokenCurrentCoinPriceText(Context context, String tokenSymbol, String count) {
        return getTokenCurrentCoinPriceText(context, false, tokenSymbol, count);
    }

    //保留8位有效小数
    public static String format8Number(float num) {
        if (num == 0) {
            return "0";
        }
        if (Float.isInfinite(num)) {
            return "0";
        }
        String numStr;
        if (Math.abs(num) > 1) {
            numStr = format2Number(num + "");
        } else {
            BigDecimal value = new BigDecimal(num).setScale(8, BigDecimal.ROUND_HALF_UP);
            numStr = value.compareTo(BigDecimal.ZERO) == 0 ? "0" : value.stripTrailingZeros().toPlainString();
        }
        if (!ToolUtils.isZh(MainApplication.getContext())) {
            return addDivider(numStr, ",", 3);
        } else {
            return numStr;
        }
    }
    //*****************************数字价格**********************************************

    //非官方渠道
    public static boolean isNotOfficialChannel(Context context) {
        String channel = Utils.getChannel(context);
        return !TextUtils.equals("official", channel);
    }


    public static boolean checkSslErrorForIDMISMATCH(SslError sslError) {
        try {
            // Use reflection to access the private mX509Certificate field of SslCertificate
            SslCertificate sslCert = sslError.getCertificate();
            if (sslError.hasError(SslError.SSL_UNTRUSTED)) {
                // Check if Cert-Domain equals the Uri-Domain
                String certDomain = sslCert.getIssuedTo().getCName();
                if (certDomain.equals(new URL(sslError.getUrl()).getHost())) {
                    return true;
                }
            }

            Certificate cert = getX509Certificate(sslCert);
            if (cert != null) {
                return true;
            }

            Field f = sslCert.getClass().getDeclaredField("mX509Certificate");
            f.setAccessible(true);
            X509Certificate x509Certificate = (X509Certificate) f.get(sslCert);
            if (x509Certificate != null) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static Certificate getX509Certificate(SslCertificate sslCertificate) {
        Bundle bundle = SslCertificate.saveState(sslCertificate);
        byte[] bytes = bundle.getByteArray("x509-certificate");
        if (bytes == null) {
            return null;
        } else {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                return certFactory.generateCertificate(new ByteArrayInputStream(bytes));
            } catch (CertificateException e) {
                return null;
            }
        }
    }

    //蓝牙是否打开
    public static boolean isCloseBlue() {
        BluetoothAdapter blueadapter = BluetoothAdapter.getDefaultAdapter();
        return !blueadapter.isEnabled();
    }
}
