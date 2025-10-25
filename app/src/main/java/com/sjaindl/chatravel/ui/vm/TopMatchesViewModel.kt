package com.sjaindl.chatravel.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.sjaindl.chatravel.gql.TopMatchesQuery
import com.sjaindl.chatravel.ui.profile.Interest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class TopMatchesViewModel : ViewModel(), KoinComponent {

    data class TopMatch(
        val id: Long,
        val name: String,
        val avatarUrl: String?,
        val score: Int,
        val commonInterests: List<Interest>,
        val activeConversations: Int,
    )

    val apollo = ApolloClient.Builder()
        .serverUrl("http://10.0.2.2:8080/graphql")
        .build()

    private var _contentState: MutableStateFlow<List<TopMatch>> = MutableStateFlow(emptyList())
    val contentState: StateFlow<List<TopMatch>> = _contentState.asStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    fun start(userId: Long) {
        viewModelScope.launch {
            val resp = apollo.query(
                TopMatchesQuery(
                    userId = userId.toString(),
                    limit = Optional.present(20)
                )
            ).execute()

            val data = resp.data?.topMatches?.map { match ->
                val user = match.user
                TopMatch(
                    id = user.userId.toLong(),
                    name = user.name,
                    avatarUrl = user.avatarUrl,
                    score = match.score,
                    commonInterests = match.commonInterests.map {
                        Interest.valueOf(it)
                    },
                    activeConversations = match.activeConversations,
                )
            }.orEmpty()

            _contentState.update {
                data
            }
        }
    }
}
