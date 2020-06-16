package com.quarkonium.qpocket.api.di;

import android.content.Context;

import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.api.repository.PasswordStore;
import com.quarkonium.qpocket.api.repository.QuarkPasswordStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
class ToolsModule {
    @Provides
    Context provideContext(MainApplication application) {
        return application.getApplicationContext();
    }

    @Singleton
    @Provides
    PasswordStore passwordStore(Context context) {
        return new QuarkPasswordStore(context);
    }
}
