package com.quarkonium.qpocket.api.di;

import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.model.book.viewmodel.AddressBookViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class AddressBookModule {

    @Provides
    AddressBookViewModelFactory provideLoginViewModelFactory(MainApplication application) {
        return new AddressBookViewModelFactory(application);
    }

}
