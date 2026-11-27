package com.bepngon.merge.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    val id: String,
    val nickname: String,
    val banned: Boolean = false,
)

@Serializable
data class BootstrapRequest(
    val installId: String,
    val nickname: String? = null,
)

@Serializable
data class BootstrapResponse(
    val player: PlayerDto,
    val token: String,
)

@Serializable
data class NicknameRequest(val nickname: String)

@Serializable
data class NicknameResponse(val player: PlayerDto)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int,
    val playerId: String,
    val nickname: String,
    val score: Int,
    val updatedAt: String? = null,
    val isMe: Boolean? = null,
)

@Serializable
data class LeaderboardResponse(
    val board: String,
    val entries: List<LeaderboardEntryDto>,
    val me: LeaderboardEntryDto? = null,
)

@Serializable
data class SubmitScoreRequest(
    val score: Int,
    val configVersion: Int? = null,
    val appVersion: String? = null,
    val debug: Boolean? = null,
)

@Serializable
data class SubmitScoreResponse(
    val board: String,
    val submittedScore: Int,
    val accepted: Boolean,
    val reason: String? = null,
    val bestScore: Int? = null,
    val rank: Int? = null,
)

@Serializable
data class SpawnEntryConfigDto(
    val itemId: String,
    val weight: Int,
)

@Serializable
data class GeneratorConfigDto(
    val maxCharges: Int,
    val spawnTable: List<SpawnEntryConfigDto>,
)

@Serializable
data class GameConfigPayloadDto(
    val schemaVersion: Int,
    val minAppVersionCode: Int = 1,
    val generators: Map<String, GeneratorConfigDto> = emptyMap(),
)

@Serializable
data class GameConfigResponse(
    val notModified: Boolean = false,
    val version: Int,
    val status: String? = null,
    val payload: GameConfigPayloadDto? = null,
)
