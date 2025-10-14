package com.sjaindl.chatravel.di

import com.sjaindl.chatravel.data.MessagesApi
import com.sjaindl.chatravel.data.MessagesRepository
import com.sjaindl.chatravel.data.MessagesRepositoryImpl
import com.sjaindl.chatravel.data.ShortPoller
import com.sjaindl.chatravel.data.UserApi
import com.sjaindl.chatravel.data.UserRepository
import com.sjaindl.chatravel.data.UserRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val appModule = module {
    single<MessagesRepository> {
        MessagesRepositoryImpl(
            api = get()
        )
    }

    single<UserRepository> {
        UserRepositoryImpl(
            context = androidApplication(),
            userApi = get(),
        )
    }

    single<MessagesApi> {
        MessagesApi(client = get())
    }

    single<UserApi> {
        UserApi(client = get())
    }

    single<HttpClient> {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
                )
            }

            defaultRequest {
                //url("http://10.0.2.2:8080")
                url("http://0.0.0.0:8080")
            }
        }
    }

    single<Json> {
        Json {
            ignoreUnknownKeys = true
        }
    }

    single<ShortPoller> {
        ShortPoller(
            messagesRepository = get(),
            scope = get(),
        )
    }

    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
