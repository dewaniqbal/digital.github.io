package app.quranaudio.ui.library

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.quranaudio.R
import app.quranaudio.domain.ThemeCollection

data class CollectionStyle(@StringRes val title: Int, @StringRes val description: Int, val icon: ImageVector, val gradient: List<Color>)

fun collectionStyle(c: ThemeCollection): CollectionStyle = when (c) {
    ThemeCollection.MOST_BEAUTIFUL -> CollectionStyle(R.string.collection_most_beautiful, R.string.collection_most_beautiful_desc, Icons.Filled.AutoAwesome, listOf(Color(0xFF3B2F7A), Color(0xFF7B5FC7)))
    ThemeCollection.SLEEP -> CollectionStyle(R.string.collection_sleep, R.string.collection_sleep_desc, Icons.Filled.Bedtime, listOf(Color(0xFF14204A), Color(0xFF3A4C9A)))
    ThemeCollection.FOCUS -> CollectionStyle(R.string.collection_focus, R.string.collection_focus_desc, Icons.Filled.CenterFocusStrong, listOf(Color(0xFF103B44), Color(0xFF2F7F86)))
    ThemeCollection.EMOTIONAL -> CollectionStyle(R.string.collection_emotional, R.string.collection_emotional_desc, Icons.Filled.Spa, listOf(Color(0xFF3F1F45), Color(0xFF8C4A8F)))
    ThemeCollection.MORNING -> CollectionStyle(R.string.collection_morning, R.string.collection_morning_desc, Icons.Filled.WbSunny, listOf(Color(0xFF4A3420), Color(0xFFB07A3F)))
    ThemeCollection.EVENING -> CollectionStyle(R.string.collection_evening, R.string.collection_evening_desc, Icons.Filled.WbTwilight, listOf(Color(0xFF2B1F4F), Color(0xFF8A5A8E)))
    ThemeCollection.RAMADAN -> CollectionStyle(R.string.collection_ramadan, R.string.collection_ramadan_desc, Icons.Filled.NightsStay, listOf(Color(0xFF1C2D4F), Color(0xFF4F6FA8)))
    ThemeCollection.FRIDAY -> CollectionStyle(R.string.collection_friday, R.string.collection_friday_desc, Icons.Filled.Mosque, listOf(Color(0xFF173A2E), Color(0xFF3F8A6B)))
}

val FavouritesGradient = listOf(Color(0xFF4A1F3A), Color(0xFFB0507A))
val RecentGradient = listOf(Color(0xFF1F2A44), Color(0xFF4F6A9A))
val UserPlaylistGradient = listOf(Color(0xFF22264A), Color(0xFF4B4F8F))
val FavouritesIcon: ImageVector = Icons.Filled.Favorite
val RecentIcon: ImageVector = Icons.Filled.History
val PlaylistIcon: ImageVector = Icons.AutoMirrored.Filled.QueueMusic
