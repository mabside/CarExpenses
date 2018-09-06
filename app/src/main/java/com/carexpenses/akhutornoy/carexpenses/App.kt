package com.carexpenses.akhutornoy.carexpenses

import com.carexpenses.akhutornoy.carexpenses.di.app.DaggerAppComponent
import com.carexpenses.akhutornoy.carexpenses.di.app.bins.AppModule
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication

class App : DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .build()
    }
}