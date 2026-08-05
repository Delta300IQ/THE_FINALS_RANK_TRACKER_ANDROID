package com.example.finalsranktracker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
internal fun WrappedUI(
    entries: List<RankEntry>,
    palette: Palette,
    isEnglish: Boolean,
    playerProfile: PlayerProfile?,
    season: Int
) {
    if (entries.isEmpty()) return

    val startRank = entries.first().rank
    val endRank = entries.last().rank
    val totalMatches = entries.size - 1
    val deltas = entries.zipWithNext { a, b -> b.rank - a.rank }
    val wins = deltas.count { it > 0 }
    val losses = deltas.count { it < 0 }
    val winRate = if (totalMatches > 0) ((wins.toFloat() / totalMatches) * 100).toInt() else 0
    val bestStreak = longestStreak(deltas) { it > 0 }

    val tags = countMatchesByTag(entries, isEnglish)
    val topTag = tags.maxByOrNull { it.value }?.key ?: (if (isEnglish) "None" else "Aucun")

    val bgBrush = Brush.linearGradient(
        colors = listOf(palette.bg, palette.surfaceAlt, palette.accent.copy(alpha = 0.3f), palette.bg),
        start = Offset(0f, 0f),
        end = Offset(1080f, 1920f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(40.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "THE FINALS",
                    color = palette.accent,
                    fontFamily = BarlowCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 72.sp,
                    letterSpacing = 4.sp
                )
                Text(
                    text = if (isEnglish) "SEASON $season WRAPPED" else "RÉCAP SAISON $season",
                    color = Color.White,
                    fontFamily = BarlowCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    letterSpacing = 2.sp
                )
            }

            // PLAYER INFO
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = playerProfile?.name ?: (if (isEnglish) "PLAYER" else "JOUEUR"),
                    color = Color.White,
                    fontFamily = BarlowCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 60.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painterResource(id = rankLogoResFor(endRank, playerProfile?.globalRank)),
                    contentDescription = null,
                    modifier = Modifier.size(300.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = rankNameFor(endRank, playerProfile?.globalRank).uppercase(),
                    color = palette.accent,
                    fontFamily = BarlowCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 54.sp
                )
                Text(
                    text = "$endRank RS",
                    color = Color.White,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            }

            // STATS GRID
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .finalsCard(palette, baseColor = palette.surface.copy(alpha = 0.8f))
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    WrappedStat(if (isEnglish) "MATCHES" else "MATCHS", "$totalMatches", palette)
                    WrappedStat("WIN RATE", "$winRate%", palette)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    WrappedStat(if (isEnglish) "PROGRESS" else "PROGRESSION", "${if (endRank >= startRank) "+" else ""}${endRank - startRank}", palette)
                    WrappedStat(if (isEnglish) "BEST STREAK" else "MEILLEURE SÉRIE", "$bestStreak W", palette)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    WrappedStat(if (isEnglish) "TOP TAG" else "TAG FAVORI", topTag, palette)
                }
            }

            // FOOTER
            Text(
                text = "finals-tracker.com",
                color = palette.textMuted,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
private fun WrappedStat(label: String, value: String, palette: Palette) {
    Column {
        Text(
            text = label,
            color = palette.textMuted,
            fontFamily = BarlowCondensed,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp
        )
    }
}
