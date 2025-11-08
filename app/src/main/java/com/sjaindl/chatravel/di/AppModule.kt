package com.sjaindl.chatravel.di

import androidx.room.Room
import com.sjaindl.chatravel.data.FcmTokenApi
import com.sjaindl.chatravel.data.MessageFetcher
import com.sjaindl.chatravel.data.MessagesApi
import com.sjaindl.chatravel.data.MessagesRepository
import com.sjaindl.chatravel.data.MessagesRepositoryImpl
import com.sjaindl.chatravel.data.UserApi
import com.sjaindl.chatravel.data.UserRepository
import com.sjaindl.chatravel.data.UserRepositoryImpl
import com.sjaindl.chatravel.data.fcm.TokenRepository
import com.sjaindl.chatravel.data.fcm.TokenRepositoryImpl
import com.sjaindl.chatravel.data.polling.LongPoller
import com.sjaindl.chatravel.data.polling.ShortPoller
import com.sjaindl.chatravel.data.prefs.UserSettingsRepository
import com.sjaindl.chatravel.data.room.ChatTravelDatabase
import com.sjaindl.chatravel.data.room.ChatTravelDatabase.Companion.DATABASE_NAME
import com.sjaindl.chatravel.data.sse.InterestMatchSseClient
import com.sjaindl.chatravel.data.websocket.WebSocketFetcher
import com.sjaindl.chatravel.data.websocket.WebSocketsMessagesApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
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
            api = get(),
            database = get(),
        )
    }

    single<UserRepository> {
        UserRepositoryImpl(
            context = androidApplication(),
            userApi = get(),
        )
    }

    single<TokenRepository> {
        TokenRepositoryImpl(
            fcmTokenApi = get(),
        )
    }

    single<UserSettingsRepository> {
        UserSettingsRepository(
            context = androidApplication(),
        )
    }

    single<MessagesApi> {
        MessagesApi(client = get())
    }

    single<UserApi> {
        UserApi(client = get())
    }

    single<FcmTokenApi> {
        FcmTokenApi(client = get())
    }

    single<HttpClient> {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                        classDiscriminator = "type"
                    }
                )
            }

            install(WebSockets)

            defaultRequest {
                url("http://10.0.2.2:8080")
            }
        }
    }

    single<Json> {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            classDiscriminator = "type"
        }
    }

    single<ShortPoller> {
        ShortPoller(
            messagesRepository = get(),
            scope = get(),
        )
    }

    single<LongPoller> {
        LongPoller(
            messagesRepository = get(),
            scope = get(),
        )
    }

    single<WebSocketFetcher> {
        WebSocketFetcher(
            messagesRepository = get(),
            webSocketsMessagesApi = get(),
            scope = get(),
            database = get(),
        )
    }

    single<MessageFetcher> {
        MessageFetcher(
            database = get(),
            shortPoller = get(),
            longPoller = get(),
            webSocketFetcher = get(),
            userRepository = get(),
            messagesRepository = get(),
            settingsRepository = get()
        )
    }

    single<WebSocketsMessagesApi> {
        WebSocketsMessagesApi(
            client = get(),
            json = get(),
        )
    }

    single<InterestMatchSseClient> {
        InterestMatchSseClient(
            client = get(),
            json = get(),
            scope = get(),
        )
    }

    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    single<ChatTravelDatabase> {
        Room.databaseBuilder(
            context = get(),
            klass = ChatTravelDatabase::class.java,
            name = DATABASE_NAME,
        ).build()
    }
}
