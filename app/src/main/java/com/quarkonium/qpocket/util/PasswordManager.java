package com.quarkonium.qpocket.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.service.AccountKeystoreService;
import com.quarkonium.qpocket.api.service.GethKeystoreAccountService;
import com.quarkonium.qpocket.tron.TronKeystoreAccountService;
import com.quarkonium.qpocket.tron.keystore.CipherException;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.R;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class PasswordManager {

    public static boolean shouldRequestPasswordBySavingStateAndTipIfWrong(Context context, QWWallet wallet) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("QuarkWallet_PasswordWrongState", Context.MODE_PRIVATE);
        int wrongCount = sharedPreferences.getInt("wrongCount" + wallet.getId(), 0);
        if (wrongCount < 3) {
            return true;
        }

        String wrongTimeStampString = sharedPreferences.getString("wrongTime" + wallet.getId(), "");
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        long timeStamp = System.currentTimeMillis();
        try {
            Date currentDate = new Date(timeStamp);
            Date lastWrongDate = dateFormatter.parse(wrongTimeStampString);
            long timeDiffInSeconds = (currentDate.getTime() - lastWrongDate.getTime()) / 1000;
            long freezeSecondsLeft = (long) (timeDiffInSeconds - Math.pow(10, wrongCount + 1 - 3));
            if (freezeSecondsLeft < 0) {
                String passwordTryAfter = context.getResources().getString(R.string.password_tryAfter);
                MyToast.showSingleToastShort(context, String.format(passwordTryAfter, -freezeSecondsLeft));
                return false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return true;

    }

    public static void clearPasswordWrongTimeStatesIfExist(Context context, QWWallet wallet) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("QuarkWallet_PasswordWrongState", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("wrongCount" + wallet.getId());
        editor.remove("wrongTime" + wallet.getId());
        editor.apply();
    }

    //清除钱包keystore
    public static void removeKeystoreWallet(Context context, String address) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(address + "-keystore");
        editor.apply();
    }

    public final static Single<String> checkPassword(Context context, QWWallet wallet, String password) {
        Context contextFinal = context.getApplicationContext();
        String address = wallet.getCurrentAddress();
        return Single.fromCallable(() -> {
            QWAccountDao dao = new QWAccountDao(contextFinal);
            QWAccount account = dao.queryAllParamsByAddress(address);
            int type = account.getType();
            if (type == Constant.ACCOUNT_TYPE_TRX) {
                AccountKeystoreService tronAccountKeystoreService = new TronKeystoreAccountService(contextFinal);
                String pv = tronAccountKeystoreService.exportPrivateKey(account, password, password).blockingGet();
                if (TextUtils.isEmpty(pv)) {
                    throw new CipherException("password Error");
                }
            } else {
                File file = new File(context.getFilesDir(), "keystore/keystore");
                GethKeystoreAccountService service = new GethKeystoreAccountService(file);
                String keystore = service.exportKeystore(account, password, password).blockingGet();
                if (TextUtils.isEmpty(keystore)) {
                    throw new CipherException("password Error");
                }
            }
            return password;
        }).subscribeOn(Schedulers.io());
    }
}
