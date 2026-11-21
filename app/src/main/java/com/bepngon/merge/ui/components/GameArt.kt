package com.bepngon.merge.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val resCache = mutableMapOf<String, Int>()

fun artRes(context: Context, name: String): Int =
    resCache.getOrPut(name) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }

/**
 * Renders the sliced art asset named [name] (e.g. a defId like "fruit_4",
 * "gen_bakery_2", or an icon like "icon_coin") at [size].
 * Falls back to [emoji] text when no drawable exists for that name.
 */
@Composable
fun GameArt(
    name: String,
    emoji: String,
    size: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    emojiSize: TextUnit = (size.value * 0.62f).sp,
) {
    val ctx = LocalContext.current
    val res = remember(name) { artRes(ctx, name) }
    if (res != 0) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size).alpha(alpha),
        )
    } else {
        Text(emoji, fontSize = emojiSize, modifier = modifier.alpha(alpha))
    }
}
