package com.bepngon.merge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.data.remote.LeaderboardEntryDto
import com.bepngon.merge.data.remote.LeaderboardResponse
import com.bepngon.merge.data.remote.PlayerDto
import com.bepngon.merge.data.remote.PlayerIdentity
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.CreamBg
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import com.bepngon.merge.ui.theme.WarnRed
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private val Boards = listOf(
    "main" to "⭐ Tổng",
    "mini" to "🍉 Thả trái",
    "block" to "🧱 Xếp khối",
)

@Composable
fun LeaderboardScreen(
    loadTop: suspend (String) -> LeaderboardResponse,
    loadAroundMe: suspend (String) -> LeaderboardResponse,
    loadIdentity: suspend () -> PlayerIdentity,
    onUpdateNickname: suspend (String) -> PlayerDto,
    onClose: () -> Unit,
) {
    var board by remember { mutableStateOf("main") }
    var entries by remember { mutableStateOf<List<LeaderboardEntryDto>>(emptyList()) }
    var me by remember { mutableStateOf<LeaderboardEntryDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var editingName by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("") }
    var nameMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(board, refreshTick) {
        loading = true
        error = null
        val identityJob = async { runCatching { loadIdentity() }.getOrNull() }
        try {
            val topJob = async { runCatching { loadTop(board) } }
            val meJob = async { runCatching { loadAroundMe(board) }.getOrNull() }
            val topResult = topJob.await()
            me = meJob.await()?.me
            identityJob.await()?.nickname?.let { nickname = it }
            entries = topResult.getOrThrow().entries
            me?.nickname?.let { nickname = it }
        } catch (e: Exception) {
            identityJob.await()?.nickname?.let { nickname = it }
            error = e.message ?: "Không tải được bảng điểm"
            entries = emptyList()
            me = null
        } finally {
            loading = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(CreamBg),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "🏅 Bảng xếp hạng",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Cocoa,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "✕",
                    fontSize = 22.sp,
                    color = Cocoa,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFFE0B2))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                Boards.forEach { (id, label) ->
                    val active = board == id
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else Cocoa,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) MergeGreen else CellWhite)
                            .clickable { board = id }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CellWhite)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Tên hiển thị", fontSize = 10.sp, color = Cocoa.copy(alpha = 0.65f))
                    Text(
                        nickname.ifBlank { "Đang kết nối…" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Cocoa,
                    )
                }
                Text(
                    "Sửa ✏️",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MergeGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { editingName = true }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
            nameMessage?.let {
                Text(it, fontSize = 11.sp, color = WarnRed, modifier = Modifier.padding(top = 4.dp))
            }

            me?.let { mine ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .border(2.dp, Gold, RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF8E1), RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("#${mine.rank}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text("Vị trí của bạn", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Cocoa)
                        Text(mine.nickname, fontSize = 11.sp, color = Cocoa.copy(alpha = 0.7f))
                    }
                    Text("${mine.score}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MergeGreen)
                }
            }

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MergeGreen)
                }
                error != null -> Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CellWhite)
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("📡 Không kết nối được server", fontWeight = FontWeight.Bold, color = Cocoa)
                    Text(
                        error.orEmpty(),
                        fontSize = 12.sp,
                        color = Cocoa.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text(
                        "Thử lại",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MergeGreen)
                            .clickable { refreshTick++ }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(entries) { entry ->
                        LeaderboardRow(entry, highlight = entry.playerId == me?.playerId || entry.isMe == true)
                    }
                }
            }
        }
    }

    if (editingName) {
        var draft by remember { mutableStateOf(nickname) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("Đổi tên hiển thị") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(24) },
                    singleLine = true,
                    label = { Text("Nickname") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = draft.trim()
                    if (clean.length < 2) {
                        nameMessage = "Tên cần ít nhất 2 ký tự"
                        editingName = false
                        return@TextButton
                    }
                    editingName = false
                    scope.launch {
                        runCatching { onUpdateNickname(clean) }
                            .onSuccess {
                                nickname = it.nickname
                                nameMessage = null
                                refreshTick++
                            }
                            .onFailure { nameMessage = it.message ?: "Không đổi được tên" }
                    }
                }) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { editingName = false }) { Text("Huỷ") }
            },
        )
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntryDto, highlight: Boolean) {
    val medal = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${entry.rank}"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .border(
                2.dp,
                if (highlight) MergeGreen else Color(0xFFE8D5B7),
                RoundedCornerShape(13.dp),
            )
            .background(if (highlight) Color(0xFFE8F7EA) else CellWhite, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            medal,
            fontSize = if (entry.rank <= 3) 22.sp else 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (entry.rank <= 3) Cocoa else Gold,
            modifier = Modifier.padding(end = 10.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(entry.nickname, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cocoa)
            if (highlight) Text("Bạn", fontSize = 10.sp, color = MergeGreen)
        }
        Text("${entry.score}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MergeGreen)
    }
}
