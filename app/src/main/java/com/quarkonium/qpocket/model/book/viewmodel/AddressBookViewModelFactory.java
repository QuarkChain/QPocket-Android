package com.quarkonium.qpocket.model.book.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.quarkonium.qpocket.MainApplication;

public class AddressBookViewModelFactory implements ViewModelProvider.Factory {

    private MainApplication mApplication;

    public AddressBookViewModelFactory(MainApplication application) {
        mApplication = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new AddressBookViewModel(mApplication);
    }
}
