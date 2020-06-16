package com.quarkonium.qpocket.util;

import android.content.Context;
import android.text.TextUtils;

import com.quarkonium.qpocket.util.http.HttpUtils;

import okhttp3.Call;
import okhttp3.Response;

public class CountryUtils {

    //根据IP获取当前国家
    public static String getCountry(Context context) {
        String country = SharedPreferencesUtils.getCountryCode(context);
        if (TextUtils.isEmpty(country)) {
            String path = "https://ipapi.co/country/";
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(path)
                    .build();
            final Call call = HttpUtils.getOkHttp().newCall(request);
            try {
                Response response = call.execute();
                if (response.body() != null) {
                    country = response.body().string();
                    SharedPreferencesUtils.setCountryCode(context, country);
                }
            } catch (Exception e) {
                e.printStackTrace();
                country = "";
            }
        }
        return country;
    }
}
