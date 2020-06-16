package com.quarkonium.qpocket.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.quarkonium.qpocket.MainApplication;

/**
 * Created by mr on 14-7-29.
 */
public class UiUtils {

    private static float mDensity = -1;

    public static float dpToPixel(float dp) {
        return getDensity() * dp;
    }

    public static float getDensity() {
        if (mDensity == -1) {
            mDensity = MainApplication.getContext().getResources().getDisplayMetrics().density;
        }
        return mDensity;
    }

    public static float dpToPixel(Context ctx, float dp) {
        return getDensity(ctx) * dp;
    }

    private static float getDensity(Context ctx) {
        if (mDensity == -1) {
            mDensity = ctx.getResources().getDisplayMetrics().density;
        }
        return mDensity;
    }

    public static int sp2px(float sp) {
        final float scale = MainApplication.getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (sp * scale + 0.5f);
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
