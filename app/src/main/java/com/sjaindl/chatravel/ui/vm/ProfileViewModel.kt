package com.sjaindl.chatravel.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sjaindl.chatravel.data.UserDto
import com.sjaindl.chatravel.data.UserRepository
import com.sjaindl.chatravel.data.prefs.UserSettingsRepository
import com.sjaindl.chatravel.ui.profile.Interest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random

class ProfileViewModel: ViewModel(), KoinComponent {

    sealed class UserState {
        data object Initial: UserState()

        data object Loading: UserState()

        data class Content(val user: UserDto?): UserState()

        data class Error(val throwable: Throwable): UserState()
    }

    private val userRepository: UserRepository by inject<UserRepository>()

    private val notificationRepository: UserSettingsRepository by inject<UserSettingsRepository>()

    val notifyUser = notificationRepository.prefs.map {
        it.userInterestNotify
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
        initialValue = false,
    )

    private var _userState: MutableStateFlow<UserState> = MutableStateFlow<UserState>(UserState.Initial)

    val userState: StateFlow<UserState> = _userState.asStateFlow().onStart {
        runCatching {
            userRepository.getCurrentUser()
        }.onSuccess { user ->
            _userState.emit(UserState.Content(user))
        }.onFailure {
            _userState.emit(UserState.Error(it))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
        initialValue = UserState.Initial,
    )

    fun onUserSelected(userName: String, interests: List<Interest>) = viewModelScope.launch {
        val currentUser = userRepository.getCurrentUser()

        _userState.value = UserState.Loading

        try {
            if (currentUser != null && currentUser != UserDto.Empty) {
                val user = currentUser.copy(interests = interests.map { it.name })
                val updatedUser = userRepository.updateUser(userId = user.userId, name = userName, interests = interests)
                _userState.value = UserState.Content(updatedUser)
            } else {
                val userId = Random.nextLong(from = 0, until = Long.MAX_VALUE)
                val user = userRepository.addUser(userId = userId, name = userName, interests = interests)
                _userState.value = UserState.Content(user)
            }
        } catch (throwable: Throwable) {
            _userState.value = UserState.Error(throwable)
        }
    }

    suspend fun loadUsers(interest: Interest): List<UserDto> {
        return userRepository.getUsers(interest = interest).users.filter {
            it.userId != userRepository.getCurrentUser()?.userId
        }
    }

    fun setNotify(notify: Boolean) = viewModelScope.launch {
        notificationRepository.setNotify(notify)
    }
}
