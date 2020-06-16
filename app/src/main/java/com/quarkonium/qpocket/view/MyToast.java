package com.quarkonium.qpocket.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.quarkonium.qpocket.R;

public class MyToast {

    public static Toast makeToast(Context context, int stringId, int duration) {
        return makeToast(context, context.getString(stringId), duration);
    }

    public static Toast makeToast(Context context, String str, int duration) {
        Toast toast = new Toast(context);
        toast.setDuration(duration);
        ViewGroup toastLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.my_toast_layout, null, false);
        toast.setView(toastLayout);
        //toast.setGravity(Gravity.CENTER, 0, 0);
        TextView textView = toastLayout.findViewById(R.id.text);
        textView.setText(str);
        return toast;
    }

    private static Toast sToast;

    private static Toast makeSingleToast(Context context, int stringId, int duration) {
        if (sToast != null) {
            sToast.cancel();
            sToast = null;
        }
        return sToast = makeToast(context.getApplicationContext(), context.getString(stringId), duration);
    }

    private static Toast makeSingleToast(Context context, String text, int duration) {
        if (sToast != null) {
            sToast.cancel();
            sToast = null;
        }
        return sToast = makeToast(context.getApplicationContext(), text, duration);
    }

    public static void showSingleToastShort(Context context, int stringId) {
        makeSingleToast(context, stringId, Toast.LENGTH_SHORT).show();
    }

    public static void showSingleToastShort(Context context, String text) {
        makeSingleToast(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showSingleToastLong(Context context, int stringId) {
        makeSingleToast(context, stringId, Toast.LENGTH_LONG).show();
    }

    public static void showSingleToastLong(Context context, String text) {
        makeSingleToast(context, text, Toast.LENGTH_LONG).show();
    }

    public static void cancel() {
        if (sToast != null) {
            sToast.cancel();
            sToast = null;
        }
    }


    //带IconToast
    private static Toast makeIconToast(Context context, int backgroundColor, int iconID, String str, int duration) {
        Toast toast = new Toast(context);
        toast.setDuration(duration);
        ViewGroup toastLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.my_img_toast_layout, null, false);
        toast.setView(toastLayout);

        TextView textView = toastLayout.findViewById(R.id.toast_text);
        textView.setText(str);

        ImageView imageView = toastLayout.findViewById(R.id.toast_icon);
        imageView.setImageResource(iconID);

        Drawable drawable = tint9PatchDrawableFrame(context, backgroundColor);
        toastLayout.setBackground(drawable);
        return toast;
    }

    private static Drawable tint9PatchDrawableFrame(@NonNull Context context, @ColorInt int tintColor) {
        final NinePatchDrawable toastDrawable = (NinePatchDrawable) AppCompatResources.getDrawable(context, R.drawable.toast_frame);
        toastDrawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        return toastDrawable;
    }

    private static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static void showTopToast(Context context, int stringId) {
        showTopToast(context, context.getString(stringId));
    }

    public static void showTopToast(Context context, String text) {
        Toast toast = makeIconToast(context, Color.parseColor("#FFFFA34E"), R.drawable.ic_user_task_point, text, Toast.LENGTH_SHORT);
        if (sToast != null) {
            sToast.cancel();
            sToast = null;
        }
        sToast = toast;

        // 这里给了一个1/4屏幕高度的y轴偏移量
        int height = getStatusBarHeight(context);
        if (height == 0) {
            height = context.getResources().getDisplayMetrics().heightPixels / 8;
        }
        toast.setGravity(Gravity.TOP, 0, height);
        toast.show();

    }
}
