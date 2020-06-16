package com.quarkonium.qpocket.api.di;

import com.quarkonium.qpocket.MainApplication;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;

@Singleton
@Component(modules = {
        AndroidSupportInjectionModule.class,
        ToolsModule.class,
        RepositoriesModule.class,
        BuildersModule.class})
public interface AppComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(MainApplication app);

        AppComponent build();
    }

    void inject(MainApplication app);
}
