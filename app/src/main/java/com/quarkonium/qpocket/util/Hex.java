package com.quarkonium.qpocket.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigInteger;

public class Hex {
    public static int hexToInteger(String input, int def) {
        Integer value = hexToInteger(input);
        return value == null ? def : value;
    }

    @Nullable
    public static Integer hexToInteger(String input) {
        try {
            return Integer.decode(input);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static long hexToLong(String input, int def) {
        Long value = hexToLong(input);
        return value == null ? def : value;
    }

    @Nullable
    public static Long hexToLong(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }
        try {
            return Long.decode(input);
        } catch (Exception ex) {
            return null;
        }
    }

    @Nullable
    public static BigInteger hexToBigInteger(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }
        try {
            boolean isHex = containsHexPrefix(input);
            if (isHex) {
                input = cleanHexPrefix(input);
            }
            return new BigInteger(input, isHex ? 16 : 10);
        } catch (NullPointerException | NumberFormatException ex) {
            return null;
        }
    }

    @NonNull
    public static BigInteger hexToBigInteger(String input, BigInteger def) {
        BigInteger value = hexToBigInteger(input);
        return value == null ? def : value;
    }

    public static boolean containsHexPrefix(String input) {
        return input.length() > 1 && input.charAt(0) == '0' && input.charAt(1) == 'x';
    }

    @Nullable
    public static String cleanHexPrefix(@Nullable String input) {
        if (input != null && containsHexPrefix(input)) {
            input = input.substring(2);
        }
        return input;
    }

    @NonNull
    public static byte[] hexStringToByteArray(@Nullable String input) {
        String cleanInput = cleanHexPrefix(input);
        if (TextUtils.isEmpty(cleanInput)) {
            return new byte[]{};
        }
        int len = cleanInput.length();
        byte[] data;
        int startIdx;
        if (len % 2 != 0) {
            data = new byte[(len / 2) + 1];
            data[0] = (byte) Character.digit(cleanInput.charAt(0), 16);
            startIdx = 1;
        } else {
            data = new byte[len / 2];
            startIdx = 0;
        }

        for (int i = startIdx; i < len; i += 2) {
            data[(i + 1) / 2] = (byte) ((Character.digit(cleanInput.charAt(i), 16) << 4)
                    + Character.digit(cleanInput.charAt(i + 1), 16));
        }
        return data;
    }
}
