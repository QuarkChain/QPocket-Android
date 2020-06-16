//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.quarkonium.qpocket.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {
    public MD5() {
    }

    public static byte[] PGMD5(String str) {
        MessageDigest messageDigest = null;

        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(str.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException var3) {
            System.exit(-1);
        } catch (UnsupportedEncodingException var4) {
            var4.printStackTrace();
        }

        byte[] byteArray = messageDigest.digest();
        return byteArray;
    }

    public static String pinguoMD5(String original, String key) throws UnsupportedEncodingException {
        byte[] byKeys = null;
        if(key != null && key.length() != 0) {
            byKeys = key.getBytes("UTF-8");
        }

        byte[] outBytes = PGMD5(original);
        StringBuffer sb = new StringBuffer();

        for(int i = 0; i < outBytes.length; ++i) {
            byte ch1 = outBytes[i];
            byte ch2 = byKeys[i % byKeys.length];
            byte ch = (byte)(ch1 ^ ch2);
            sb.append(String.format("%02x", new Object[]{Byte.valueOf(ch)}));
        }

        return sb.toString();
    }

    public static String getMD5(byte[] source, int start, int count) {
        String s = null;
        char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        try {
            MessageDigest e = MessageDigest.getInstance("MD5");
            e.update(source, start, count);
            byte[] tmp = e.digest();
            char[] str = new char[32];
            int k = 0;

            for(int i = 0; i < 16; ++i) {
                byte byte0 = tmp[i];
                str[k++] = hexDigits[byte0 >>> 4 & 15];
                str[k++] = hexDigits[byte0 & 15];
            }

            s = new String(str);
        } catch (Exception var11) {
            var11.printStackTrace();
        }

        return s;
    }
}
