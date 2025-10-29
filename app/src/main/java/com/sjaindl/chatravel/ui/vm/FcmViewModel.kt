package com.sjaindl.chatravel.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.sjaindl.chatravel.data.fcm.TokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FcmViewModel : ViewModel(), KoinComponent {

    private val tokenRepository: TokenRepository by inject()

    fun registerToken(userId: Long) {
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result ?: return@addOnCompleteListener

            viewModelScope.launch(Dispatchers.IO) {
                tokenRepository.registerToken(userId = userId, token = token)
            }
        }
    }
}
