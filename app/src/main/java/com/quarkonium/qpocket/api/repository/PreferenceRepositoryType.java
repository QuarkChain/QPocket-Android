package com.quarkonium.qpocket.api.repository;


public interface PreferenceRepositoryType {
    String getCurrentWalletKey();

    void setCurrentWalletKey(String address);
}
