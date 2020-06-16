package com.quarkonium.qpocket.model.main.view;

import android.view.View;

public class ViewAnimationUtils {
    public static void expand(View view, int maxHeight) {
        DropAnim.getInstance().animateOpen(view, maxHeight);
    }

    public static void collapse(View view) {
        DropAnim.getInstance().animateClose(view);
    }
}
