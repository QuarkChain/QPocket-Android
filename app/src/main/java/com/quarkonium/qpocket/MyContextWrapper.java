package com.quarkonium.qpocket;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public class MyContextWrapper extends ContextWrapper {

    public MyContextWrapper(Context base) {
        super(base);
    }

    public static ContextWrapper wrap(Context context, Locale language) {
        Locale.setDefault(language);

        Configuration config = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(language);
            config.setLayoutDirection(language);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = language;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLayoutDirection(language);
            }
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
        return new MyContextWrapper(context);
    }
}
