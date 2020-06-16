package com.quarkonium.qpocket.api.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPreferenceRepository implements PreferenceRepositoryType {

    private static final String CURRENT_ACCOUNT_ADDRESS_KEY = "current_account_key";
    private static final String IS_CLEAR_BAD_WALLET = "is_clear_bad_wallet";

    private final SharedPreferences pref;

    public SharedPreferenceRepository(Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public String getCurrentWalletKey() {
        return pref.getString(CURRENT_ACCOUNT_ADDRESS_KEY, null);
    }

    @Override
    public void setCurrentWalletKey(String key) {
        pref.edit().putString(CURRENT_ACCOUNT_ADDRESS_KEY, key).apply();
    }

    public boolean isClearBadWallet() {
        return pref.getBoolean(IS_CLEAR_BAD_WALLET, false);
    }

    public void setClearBadWallet() {
        pref.edit().putBoolean(IS_CLEAR_BAD_WALLET, true).apply();
    }
}
