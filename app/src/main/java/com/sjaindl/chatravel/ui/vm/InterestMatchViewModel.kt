package com.sjaindl.chatravel.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sjaindl.chatravel.data.sse.InterestMatch
import com.sjaindl.chatravel.data.sse.InterestMatchSseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InterestMatchViewModel() : ViewModel(), KoinComponent {

    private val sse: InterestMatchSseClient by inject<InterestMatchSseClient>()

    private val _events = MutableStateFlow<List<InterestMatch>>(emptyList())
    val events: StateFlow<List<InterestMatch>> = _events

    fun start(userId: Long) {
        sse.start(userId)
        viewModelScope.launch {
            sse.matches.collect { match ->
                _events.update { it + match }
            }
        }
    }
    override fun onCleared() { sse.stop(); super.onCleared() }
}
