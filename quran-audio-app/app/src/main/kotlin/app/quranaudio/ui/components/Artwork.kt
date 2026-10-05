package app.quranaudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quranaudio.ui.theme.ArtworkPalette

/**
 * Generated reciter artwork (gradient + initials). The audio sources provide no licensed photos,
 * so none are shown. Decorative: the reciter's name is always shown next to it as text.
 */
@Composable
fun ReciterArtwork(
    reciterId: String,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    val (start, end) = remember(reciterId) { ArtworkPalette.colorsFor(reciterId) }
    val initials = remember(name) { ArtworkPalette.initials(name) }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(start), Color(end)))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent))),
        )
        Text(
            text = initials,
            color = Color.White.copy(alpha = 0.95f),
            fontFamily = FontFamily.Serif,
            fontSize = (size.value * 0.32f).sp,
        )
    }
}

@Composable
fun SurahBadge(number: Int, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF262B55), Color(0xFF3A3478)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = number.toString(), color = Color.White, fontSize = 15.sp)
    }
}
