package com.sjaindl.chatravelserver

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object MessageBus {
    private val _events = MutableSharedFlow<Message>(replay = 0, extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    suspend fun emitNewMessage(message: Message) {
        _events.emit(message)
    }
}
