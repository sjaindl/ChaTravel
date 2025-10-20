package com.sjaindl.chatravelserver.sse

import com.sjaindl.chatravelserver.Interest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class DiscoverableUserEvent(
    val userId: Long,
    val name: String,
    val interests: List<Interest>
)

object InterestMatchBus {
    private val _events = MutableSharedFlow<DiscoverableUserEvent>(replay = 1, extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    suspend fun emit(event: DiscoverableUserEvent) {
        _events.emit(event)
    }
}
