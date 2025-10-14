package com.sjaindl.chatravel.di

import org.koin.core.context.startKoin

fun initKoin(){
    startKoin {
        modules(appModule)
    }
}
