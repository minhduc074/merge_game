package com.bepngon.merge.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bepngon.merge.BuildConfig
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val Context.identityStore by preferencesDataStore(name = "bepngon_identity")

data class PlayerIdentity(
    val installId: String,
    val token: String,
    val playerId: String?,
    val nickname: String?,
)

class BackendRepository(private val context: Context) {
    private val installIdKey = stringPreferencesKey("install_id")
    private val tokenKey = stringPreferencesKey("player_token")
    private val playerIdKey = stringPreferencesKey("player_id")
    private val nicknameKey = stringPreferencesKey("nickname")
    private val configVersionKey = stringPreferencesKey("game_config_version")
    private val configJsonKey = stringPreferencesKey("game_config_json")
    private val analyticsQueueKey = stringPreferencesKey("analytics_queue")

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val apiBaseUrl = BuildConfig.API_BASE_URL.let { raw ->
        if (raw.endsWith("/")) raw else "$raw/"
    }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    private val api: BepNgonApi = Retrofit.Builder()
        .baseUrl(apiBaseUrl)
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
        .build()
        .create(BepNgonApi::class.java)

    private val bootstrapMutex = Mutex()
    private val analyticsMutex = Mutex()
    private val analyticsSessionId = UUID.randomUUID().toString()
    private var bootstrapFailedAtMs = 0L

    suspend fun ensurePlayer(): PlayerIdentity {
        val installId = ensureInstallId()
        val prefs = context.identityStore.data.first()
        val token = prefs[tokenKey]
        if (token != null) {
            return PlayerIdentity(installId, token, prefs[playerIdKey], prefs[nicknameKey])
        }
        val now = System.currentTimeMillis()
        if (now - bootstrapFailedAtMs < BOOTSTRAP_RETRY_BACKOFF_MS) {
            throw IOException("Backend bootstrap is temporarily unavailable")
        }
        return bootstrapMutex.withLock {
            val again = context.identityStore.data.first()
            again[tokenKey]?.let {
                return@withLock PlayerIdentity(installId, it, again[playerIdKey], again[nicknameKey])
            }
            val lockNow = System.currentTimeMillis()
            if (lockNow - bootstrapFailedAtMs < BOOTSTRAP_RETRY_BACKOFF_MS) {
                throw IOException("Backend bootstrap is temporarily unavailable")
            }
            try {
                val identity = bootstrap(installId, again[nicknameKey])
                bootstrapFailedAtMs = 0L
                identity
            } catch (error: Exception) {
                bootstrapFailedAtMs = System.currentTimeMillis()
                throw error
            }
        }
    }


    /** Queue one analytics event locally. The queue survives process death and is
     *  flushed in batches of 50 by [flushAnalytics]. */
    suspend fun trackAnalytics(
        eventName: String,
        payload: JsonObject? = null,
        configVersion: Int? = null,
    ) {
        val event = AnalyticsEventDto(
            eventName = eventName,
            payload = payload,
            sessionId = analyticsSessionId,
            appVersion = BuildConfig.VERSION_NAME,
            configVersion = configVersion,
            clientAt = System.currentTimeMillis(),
        )
        analyticsMutex.withLock {
            val queue = loadAnalyticsQueue()
            val next = (queue + event).takeLast(MAX_ANALYTICS_QUEUE)
            persistAnalyticsQueue(next)
        }
    }

    /** Flush up to one batch. Events stay queued when the network/backend fails. */
    suspend fun flushAnalytics(): Int = analyticsMutex.withLock {
        val queue = loadAnalyticsQueue()
        if (queue.isEmpty()) return@withLock 0
        val batch = queue.take(ANALYTICS_BATCH_SIZE)
        withAuthRetry { auth -> api.postAnalytics(auth, AnalyticsBatchRequest(batch)) }
        persistAnalyticsQueue(queue.drop(batch.size))
        batch.size
    }

    private suspend fun loadAnalyticsQueue(): List<AnalyticsEventDto> {
        val raw = context.identityStore.data.first()[analyticsQueueKey] ?: return emptyList()
        return runCatching { json.decodeFromString<List<AnalyticsEventDto>>(raw) }.getOrDefault(emptyList())
    }

    private suspend fun persistAnalyticsQueue(queue: List<AnalyticsEventDto>) {
        context.identityStore.edit {
            if (queue.isEmpty()) it.remove(analyticsQueueKey)
            else it[analyticsQueueKey] = json.encodeToString(queue)
        }
    }

    suspend fun cachedGameConfig(): Pair<Int, GameConfigPayloadDto>? {
        val prefs = context.identityStore.data.first()
        val version = prefs[configVersionKey]?.toIntOrNull() ?: return null
        val raw = prefs[configJsonKey] ?: return null
        val payload = runCatching { json.decodeFromString<GameConfigPayloadDto>(raw) }.getOrNull() ?: return null
        return version to payload
    }

    /** Returns null when the server config is unchanged, invalid for this app, or unavailable. */
    suspend fun fetchGameConfig(): Pair<Int, GameConfigPayloadDto>? {
        val cachedVersion = context.identityStore.data.first()[configVersionKey]?.toIntOrNull()
        val response = api.currentConfig(cachedVersion)
        if (response.notModified) return null
        val payload = response.payload ?: return null
        if (payload.schemaVersion !in 1..2 || payload.minAppVersionCode > BuildConfig.VERSION_CODE) return null
        context.identityStore.edit {
            it[configVersionKey] = response.version.toString()
            it[configJsonKey] = json.encodeToString(payload)
        }
        return response.version to payload
    }

    suspend fun leaderboard(board: String, limit: Int = 100): LeaderboardResponse =
        api.leaderboard(board, limit)

    suspend fun aroundMe(board: String, range: Int = 10): LeaderboardResponse =
        withAuthRetry { auth -> api.aroundMe(board, auth, range) }

    suspend fun submitScore(board: String, score: Int, configVersion: Int? = null): SubmitScoreResponse =
        withAuthRetry { auth ->
            api.submitScore(
                board = board,
                authorization = auth,
                body = SubmitScoreRequest(
                    score = score,
                    configVersion = configVersion,
                    appVersion = BuildConfig.VERSION_NAME,
                    debug = BuildConfig.DEBUG,
                ),
            )
        }

    suspend fun updateNickname(nickname: String): PlayerDto {
        val response = withAuthRetry { auth -> api.updateNickname(auth, NicknameRequest(nickname)) }
        context.identityStore.edit { it[nicknameKey] = response.player.nickname }
        return response.player
    }

    suspend fun localIdentitySnapshot(): PlayerIdentity? {
        val prefs = context.identityStore.data.first()
        val installId = prefs[installIdKey] ?: return null
        val token = prefs[tokenKey] ?: return null
        return PlayerIdentity(installId, token, prefs[playerIdKey], prefs[nicknameKey])
    }

    private suspend fun ensureInstallId(): String {
        val existing = context.identityStore.data.first()[installIdKey]
        if (existing != null) return existing
        val created = UUID.randomUUID().toString()
        context.identityStore.edit { it[installIdKey] = created }
        return created
    }

    private suspend fun bootstrap(installId: String, nickname: String?): PlayerIdentity {
        val response = api.bootstrap(BootstrapRequest(installId = installId, nickname = nickname))
        context.identityStore.edit {
            it[tokenKey] = response.token
            it[playerIdKey] = response.player.id
            it[nicknameKey] = response.player.nickname
        }
        return PlayerIdentity(installId, response.token, response.player.id, response.player.nickname)
    }

    private suspend fun <T> withAuthRetry(block: suspend (String) -> T): T {
        var identity = ensurePlayer()
        return try {
            block("Bearer ${identity.token}")
        } catch (e: HttpException) {
            if (e.code() != 401) throw e
            context.identityStore.edit { it.remove(tokenKey) }
            identity = bootstrap(identity.installId, identity.nickname)
            block("Bearer ${identity.token}")
        }
    }

    private companion object {
        const val BOOTSTRAP_RETRY_BACKOFF_MS = 15_000L
        const val ANALYTICS_BATCH_SIZE = 50
        const val MAX_ANALYTICS_QUEUE = 500
    }
}
