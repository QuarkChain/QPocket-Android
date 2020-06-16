package com.quarkonium.qpocket.api.interact;

import android.content.Context;
import android.util.Patterns;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWDApp;
import com.quarkonium.qpocket.api.db.table.QWGameDApp;
import com.quarkonium.qpocket.util.CountryUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;

public class DAppInteract {

    public Single<ArrayList<QWDApp>> searchDApp(Context context, String key, int coinType) {
        return Single.fromCallable(() -> {
            ArrayList<QWDApp> list = new ArrayList<>();

            String head = "(.*)(";
            String tail = ")(.*)";
            String match = head + key + tail;

            ArrayList<AVQuery<AVObject>> queryList = new ArrayList<>();
            AVQuery<AVObject> nameQuery = new AVQuery<>("DAppS3");
            nameQuery.whereMatches("name", match, "gi");
            queryList.add(nameQuery);
            AVQuery<AVObject> urlQuery = new AVQuery<>("DAppS3");
            urlQuery.whereMatches("URL", match, "gi");
            queryList.add(urlQuery);
            //如果是网页 匹配域名
            if (Patterns.WEB_URL.matcher(key).matches()) {
                URL url = new URL(key);
                String host = url.getHost();
                String hostMatch = head + host + tail;
                AVQuery<AVObject> hostQuery = new AVQuery<>("DAppS3");
                hostQuery.whereMatches("URL", hostMatch, "gi");
                queryList.add(hostQuery);
            }

            //根据IP获取当前国家
            String country = CountryUtils.getCountry(context);

            String language = ToolUtils.isZh(context) ? "zh-Hans" : "en";
            //不支持国家 拉取英文
            if (Constant.NOT_SUPPORT_DAPP_COUNTRY.equals(country)) {
                country = Constant.NOT_SUPPORT_DAPP_COUNTRY;
                language = "en";
            } else {
                country = Constant.NONE;
            }

            AVQuery<AVObject> avQuery = AVQuery.or(queryList);
            avQuery.whereEqualTo("localization", language);
            avQuery.whereEqualTo("region", country);
            avQuery.whereEqualTo("coinType", coinType);
            List<AVObject> query = null;
            try {
                query = avQuery.find();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (query != null && !query.isEmpty()) {
                for (AVObject object : query) {
                    String url = object.getString("URL");
                    if ("https://dragonereum.io/".equals(url)) {
                        continue;
                    }
                    QWDApp dApp = new QWGameDApp();
                    dApp.setUrl(object.getString("URL"));
                    dApp.setName(object.getString("name"));
                    dApp.setIconUrl(object.getString("iconURL"));
                    dApp.setDescription(object.getString("description"));
                    dApp.setLocalization(object.getString("localization"));
                    dApp.setCoinType(object.getInt("coinType"));
                    dApp.setOrder(object.getInt("order"));
                    dApp.setObjectId(object.getObjectId());
                    list.add(dApp);
                }
            }
            return list;
        });
    }

    public Single<ArrayList<QWDApp>> getHotDApp(Context context, int coinType, boolean mustRefresh) {
        return Single.fromCallable(() -> {
            ArrayList<QWDApp> list = new ArrayList<>();

            //获取当前所在国家
            String country = CountryUtils.getCountry(context);
            //不支持国家 只拉取英文热搜
            String language = ToolUtils.isZh(context) ? "zh-Hans" : "en";
            if (Constant.NOT_SUPPORT_DAPP_COUNTRY.equals(country)) {
                country = Constant.NOT_SUPPORT_DAPP_COUNTRY;
                language = "en";
            } else {
                country = Constant.NONE;
            }
            if (ToolUtils.isLongDayTime(SharedPreferencesUtils.getDAppHotTime(context, language + coinType)) || mustRefresh) {
                AVQuery<AVObject> avQuery;
                if (ToolUtils.isNotOfficialChannel(context)) {
                    avQuery = new AVQuery<>("HottestGPDApps");
                    avQuery.whereEqualTo("localization", language);
                    avQuery.whereEqualTo("coinType", coinType);
                } else {
                    avQuery = new AVQuery<>("HottestDApps");
                    avQuery.whereEqualTo("localization", language);
                    avQuery.whereEqualTo("coinType", coinType);
                    avQuery.whereEqualTo("region", country);
                }
                avQuery.orderByDescending("count");
                avQuery.limit(6);
                avQuery.include("dApp");
                List<AVObject> query = null;
                try {
                    query = avQuery.find();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (query != null && !query.isEmpty()) {
                    for (AVObject object : query) {
                        AVObject tokenItem = object.getAVObject("dApp");
                        String URL = tokenItem.getString("URL");
                        if ("https://dragonereum.io/".equals(URL)) {
                            continue;
                        }
                        QWDApp dApp = new QWGameDApp();
                        dApp.setUrl(tokenItem.getString("URL"));
                        dApp.setName(tokenItem.getString("name"));
                        dApp.setIconUrl(tokenItem.getString("iconURL"));
                        dApp.setDescription(tokenItem.getString("description"));
                        dApp.setLocalization(tokenItem.getString("localization"));
                        dApp.setCoinType(tokenItem.getInt("coinType"));
                        dApp.setOrder(tokenItem.getInt("order"));
                        dApp.setObjectId(tokenItem.getObjectId());
                        list.add(dApp);
                    }

                    SharedPreferencesUtils.setDAppHotTime(context, language + coinType);
                }
            }

            if (list.size() == 6) {
                list.remove(5);
            }
            return list;
        });
    }

    public Single<Boolean> pushHotCount(String objectId) {
        return Single.fromCallable(() -> {
            AVObject product = new AVObject("DAppSearchRecords");
            product.put("timestamp", System.currentTimeMillis() + "");

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("__type", "Pointer");
                jsonObject.put("className", "DAppS3");
                jsonObject.put("objectId", objectId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            product.put("dApp", jsonObject);

            try {
                product.save();
            } catch (AVException e) {
                e.printStackTrace();
            }
            return true;
        });
    }
}
