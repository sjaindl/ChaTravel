package com.sjaindl.chatravel.data.sse

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig.Companion.INFINITE_TIMEOUT_MS
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.Charset

@Serializable
data class InterestMatch(
    val userId: Long,
    val name: String,
    val interests: List<String>
)

class InterestMatchSseClient(
    private val client: HttpClient,
    private val json: Json,
    private val scope: CoroutineScope,
    private val baseUrl: String = "http://10.0.2.2:8080",
) {
    private var job: Job? = null

    private val _matches = MutableSharedFlow<InterestMatch>(extraBufferCapacity = 16)
    val matches: SharedFlow<InterestMatch> = _matches

    fun start(userId: Long) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            var attempt = 0
            while (isActive) {
                try {
                    client.prepareGet("$baseUrl/sse/interest-matches") {
                        accept(ContentType.Text.EventStream)
                        header(HttpHeaders.CacheControl, "no-cache")
                        header(HttpHeaders.Connection, "keep-alive")
                        url {
                            parameters.append("userId", userId.toString())
                        }
                        timeout {
                            requestTimeoutMillis = INFINITE_TIMEOUT_MS
                            socketTimeoutMillis  = INFINITE_TIMEOUT_MS
                        }
                    }.execute { response ->
                        if (!response.status.isSuccess()) error("SSE HTTP ${response.status}")

                        parseSse(response) { event, _, data ->
                            if (event == "match") {
                                runCatching {
                                    json.decodeFromString<InterestMatch>(data)
                                }.onSuccess {
                                    _matches.tryEmit(it)
                                }.onFailure {
                                    Napier.e("Could not parse event", it)
                                }
                            }
                        }
                    }

                    attempt = 0 // stream ended → reconnect
                } catch (t: Throwable) {
                    attempt++
                    delay((1000L * attempt).coerceAtMost(10_000L))
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun parseSse(
        resp: HttpResponse,
        onEvent: (event: String?, id: String?, data: String) -> Unit
    ) {
        val ch = resp.bodyAsChannel()
        var event: String? = null
        var id: String? = null
        val dataBuf = StringBuilder()
        val buf = ByteArray(1024)
        var carry = ""

        fun dispatch() {
            if (dataBuf.isNotEmpty()) {
                onEvent(event, id, dataBuf.toString().trimEnd('\n'))
                event = null; id = null; dataBuf.clear()
            }
        }

        while (!ch.isClosedForRead) {
            val n = ch.readAvailable(buf, 0, buf.size)
            if (n == -1) break
            val text = carry + String(buf, 0, n, Charset.forName("UTF-8"))
            val lines = text.split("\n")
            carry = if (text.endsWith("\n")) "" else lines.last()
            val iterable = if (carry.isEmpty()) lines else lines.dropLast(1)

            for (raw in iterable) {
                val line = raw.trimEnd('\r')
                if (line.isEmpty()) { dispatch(); continue }
                if (line.startsWith(":")) continue // heartbeat/comment

                val idx = line.indexOf(':')
                val field = if (idx == -1) line else line.substring(0, idx)
                val value = if (idx == -1) "" else line.substring(idx + 1).trimStart()

                when (field) {
                    "event" -> event = value
                    "id"    -> id = value
                    "data"  -> { dataBuf.append(value).append('\n') }
                }
            }
        }
        dispatch()
    }
}
