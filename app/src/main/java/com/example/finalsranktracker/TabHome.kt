package com.example.finalsranktracker

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun TabHome(
    currentSeasonEntries: List<RankEntry>, currentRank: Int?, animatedRankScore: Int, animatedRankBrush: Brush,
    rankGoals: Map<Int, Int>, selectedSeason: Int, isEnglish: Boolean, isDarkMode: Boolean, palette: Palette, s: Strings,
    playerProfile: PlayerProfile?, onSaveGoal: (Int) -> Unit, onClearGoal: () -> Unit, onEditClick: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit, onNavigateToHistory: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val previousRank = if (currentSeasonEntries.size > 1) currentSeasonEntries[currentSeasonEntries.size - 2].rank else null
    val delta = if (currentRank != null && previousRank != null) currentRank - previousRank else null

    val deltas = remember(currentSeasonEntries) { currentSeasonEntries.map { it.rank }.zipWithNext { a, b -> b - a } }
    val winStreak = remember(deltas) { deltas.asReversed().takeWhile { it > 0 }.size }
    val totalGainRS = remember(deltas) { deltas.filter { it > 0 }.sum() }
    val totalLossRS = remember(deltas) { deltas.filter { it < 0 }.sumOf { abs(it) } }
    val winRate = remember(totalGainRS, totalLossRS) {
        if (totalGainRS + totalLossRS > 0) ((totalGainRS.toFloat() / (totalGainRS + totalLossRS)) * 100).roundToInt() else null
    }

    val deltaScale by animateFloatAsState(targetValue = if (delta != null && delta >= 0) 1.1f else 1.0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "DeltaPulse")
    val streakFlameScale = remember { Animatable(1f) }
    LaunchedEffect(winStreak) {
        if (winStreak >= 2) {
            streakFlameScale.snapTo(1.5f)
            streakFlameScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        } else {
            streakFlameScale.snapTo(1f)
        }
    }

    val flameTransition = rememberInfiniteTransition(label = "Flicker")
    val flameFlicker by flameTransition.animateFloat(initialValue = 0.9f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "FlameFlicker")
    val flameGlowAlpha by flameTransition.animateFloat(initialValue = 0.35f, targetValue = 0.65f, animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "FlameGlow")

    var showGoalSelector by remember { mutableStateOf(false) }
    var isGoalExpanded by remember { mutableStateOf(false) }
    val goalValue = rankGoals[selectedSeason]
    val estimatedMatchesToGoal = remember(goalValue, currentRank, currentSeasonEntries) {
        if (goalValue != null && currentRank != null && goalValue > currentRank) estimateMatchesToGoal(currentSeasonEntries, currentRank, goalValue) else null
    }

    var expandedMatchIndex by remember { mutableStateOf<Int?>(null) }
    var deleteConfirmIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {

        if (currentSeasonEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().finalsCard(palette).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = s.emptyDashboardInfo, color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onNavigateToHistory() },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            text = s.emptyHomeImportBtn.uppercase(),
                            fontFamily = BarlowCondensed,
                            fontWeight = FontWeight.Bold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp,
                            color = Color.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(id = R.drawable.namatama_dashboard), contentDescription = "Empty Dashboard", modifier = Modifier.size(250.dp))
            }
        } else {
            CurrentRankCard(currentRank, playerProfile?.globalRank, animatedRankScore, animatedRankBrush, delta, deltaScale, winRate, winStreak, streakFlameScale.value, flameFlicker, flameGlowAlpha, palette, s, isEnglish, isDarkMode)

            val tiltCount = deltas.asReversed().takeWhile { it < 0 }.size
            val isTilting = tiltCount >= 3
            if (isTilting) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.red.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, palette.red, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.namatama_stop),
                            contentDescription = "Stop",
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) "TILT WARNING" else "ALERTE TILT",
                                color = Color.White,
                                fontFamily = BarlowCondensed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (isEnglish) "You've lost $tiltCount matches in a row. Consider taking a break to preserve your rank!" else "Vous avez perdu $tiltCount matchs d'affilée. Faites une pause pour préserver votre rang !",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().finalsCard(palette).clickable { isGoalExpanded = !isGoalExpanded }.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = s.rankGoalTitle, color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        if (goalValue != null && !showGoalSelector && isGoalExpanded) {
                            Row {
                                Text(text = "✎", color = palette.cyan, fontSize = 15.sp, modifier = Modifier.clickable { showGoalSelector = true }.padding(4.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "✕", color = palette.red, fontSize = 16.sp, modifier = Modifier.clickable { onClearGoal(); isGoalExpanded = false }.padding(4.dp))
                            }
                        }
                    }
                    if (goalValue == null || showGoalSelector) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val qualifyingTiers = RANK_TIERS.filter { currentRank == null || it.first > currentRank }
                        Text(text = s.rankGoalPlaceholder, color = palette.cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { showGoalSelector = true })

                        if (showGoalSelector) {
                            Spacer(modifier = Modifier.height(6.dp))
                            if (qualifyingTiers.isEmpty()) {
                                Text(s.rankGoalAlreadyMax, color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 12.sp)
                            } else {
                                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()).finalsCard(palette, baseColor = palette.surfaceAlt).padding(8.dp)) {
                                    qualifyingTiers.forEach { (threshold, name) ->
                                        Text(text = "${getLocalizedRankName(name, isEnglish)} · ${formatNum(threshold)} RS", color = palette.textPrimary, fontFamily = BarlowCondensed, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clickable { onSaveGoal(threshold); showGoalSelector = false }.padding(vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        val goalRankName = getLocalizedRankName(rankNameFor(goalValue), isEnglish)
                        val isReached = currentRank != null && currentRank >= goalValue
                        Row(modifier = Modifier.fillMaxWidth().offset(y = (-4).dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "${formatNum(goalValue)} RS · $goalRankName", color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                when {
                                    isReached -> { Text(s.rankGoalReached, color = palette.green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                    estimatedMatchesToGoal != null -> {
                                        val matchesPerDay = if (currentSeasonEntries.size > 1) {
                                            val days = (currentSeasonEntries.last().timestamp - currentSeasonEntries.first().timestamp) / (1000.0 * 60 * 60 * 24)
                                            if (days >= 0.1) currentSeasonEntries.size / days else 0.0
                                        } else 0.0
                                        val dateStr = if (matchesPerDay > 0.5) {
                                            val daysRem = (estimatedMatchesToGoal / matchesPerDay).roundToInt()
                                            if (daysRem == 0) " · " + if (isEnglish) "Today" else "Aujourd'hui"
                                            else " · $daysRem " + if (isEnglish) (if (daysRem > 1) "days" else "day") else (if (daysRem > 1) "jours" else "jour")
                                        } else ""
                                        Text(text = "${s.rankGoalEstimatePrefix}$estimatedMatchesToGoal ${s.rankGoalEstimateSuffix} $goalRankName$dateStr", color = palette.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    else -> { Text(s.rankGoalNotEnoughData, color = palette.textMuted, fontSize = 12.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isReached || (currentRank != null && currentRank >= 55000)) {
                                Image(painter = painterResource(id = R.drawable.namatama_objectif), contentDescription = "Goal Reached", modifier = Modifier.size(130.dp))
                            } else {
                                Image(painter = painterResource(id = rankLogoResFor(goalValue)), contentDescription = goalRankName, modifier = Modifier.size(130.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel(if (isEnglish) "RECENT TREND (LAST 10)" else "TENDANCE RÉCENTE (10 DERNIERS)", palette, forceWhite = true, largeText = true)
            Spacer(modifier = Modifier.height(8.dp))

            val sparklineEntries = remember(currentSeasonEntries) { currentSeasonEntries.takeLast(10) }
            if (sparklineEntries.size >= 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .finalsCard(palette)
                        .padding(16.dp)
                ) {
                    SparklineChart(entries = sparklineEntries, palette = palette)
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().finalsCard(palette).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(s.noDataPeriod, color = palette.textMuted, fontSize = 13.sp, fontFamily = BarlowCondensed)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1F26), RoundedCornerShape(4.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                    .clickable { uriHandler.openUri("https://www.reachthefinals.com/patchnotes") }
                    .padding(14.dp)
            ) {
                Text(text = s.patchNotesLabel, color = palette.textPrimary, fontFamily = BarlowCondensed, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1F26), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF3A4250), RoundedCornerShape(4.dp))
                    .clickable { uriHandler.openUri("https://www.davg25.com/app/the-finals-leaderboard-tracker/") }
                    .padding(14.dp)
            ) {
                Text(text = s.top10kLabel, color = Color.White, fontFamily = BarlowCondensed, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (deleteConfirmIndex != null) {
        val idx = deleteConfirmIndex!!
        AlertDialog(
            onDismissRequest = { deleteConfirmIndex = null },
            containerColor = palette.surface,
            title = { Text(s.deleteConfirm, color = palette.textPrimary) },
            text = { Text("Match ${idx + 1}", color = palette.textMuted) },
            confirmButton = { TextButton(onClick = { onDeleteClick(idx); deleteConfirmIndex = null }) { Text(s.confirmWord, color = palette.red) } },
            dismissButton = { TextButton(onClick = { deleteConfirmIndex = null }) { Text(s.cancelWord, color = palette.textMuted) } }
        )
    }
}

@Composable
internal fun CurrentRankCard(
    currentRank: Int?, globalRank: Int?, animatedRankScore: Int, animatedRankBrush: Brush, delta: Int?, deltaScale: Float,
    winRate: Int?, winStreak: Int, streakFlameScale: Float, flameFlicker: Float, flameGlowAlpha: Float,
    palette: Palette, s: Strings, isEnglish: Boolean, isDarkMode: Boolean
) {
    val rankName = rankNameFor(currentRank ?: 0, globalRank)
    val isRuby = rankName == "Ruby"

    val rankColors = remember(rankName, palette) {
        when {
            rankName.contains("Bronze") -> listOf(Color(0xFF7A4B28), Color(0xFFCD7F32), Color(0xFFF0C294), Color(0xFFCD7F32), Color(0xFF7A4B28))
            rankName.contains("Silver") || rankName.contains("Argent") -> listOf(Color(0xFF707B7C), Color(0xFFBDC3C7), Color(0xFFF2F4F4), Color(0xFFBDC3C7), Color(0xFF707B7C))
            rankName.contains("Gold") || rankName.contains("Or") -> listOf(Color(0xFF9A7B31), Color(0xFFE6C667), Color(0xFFFFF5D1), Color(0xFFE6C667), Color(0xFF9A7B31))
            rankName.contains("Platinum") || rankName.contains("Platine") -> listOf(Color(0xFF3A506B), Color(0xFF64DFDF), Color(0xFFE0FAFF), Color(0xFF64DFDF), Color(0xFF3A506B))
            rankName.contains("Diamond") || rankName.contains("Diamant") -> listOf(Color(0xFF480CA8), Color(0xFF4CC9F0), Color(0xFFEAF8FF), Color(0xFF4CC9F0), Color(0xFF480CA8))
            isRuby -> listOf(Color(0xFF800020), Color(0xFFFF0F50), Color(0xFFFFCCD5), Color(0xFFFF0F50), Color(0xFF800020))
            else -> listOf(palette.accent, palette.accentOn, palette.accent)
        }
    }

    val progressTransition = rememberInfiniteTransition(label = "ProgressShimmer")
    val progressOffsetY by progressTransition.animateFloat(initialValue = 800f, targetValue = -400f, animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "ProgressOffset")
    val verticalProgressBrush = Brush.verticalGradient(colors = rankColors, startY = progressOffsetY + 400f, endY = progressOffsetY)

    Box(modifier = Modifier.fillMaxWidth().finalsCard(palette, cornerRadius = 4.dp).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.currentRankLabel,
                    color = palette.textMuted,
                    fontFamily = BarlowCondensed,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(1.dp))

                if (isRuby && globalRank != null) {
                    Text(
                        text = "${s.topWorldPrefix} $globalRank",
                        style = TextStyle(brush = animatedRankBrush, fontFamily = BarlowCondensed, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text(
                        text = formatNum(animatedRankScore),
                        style = TextStyle(brush = animatedRankBrush, fontFamily = BarlowCondensed, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                    )
                }

                if (delta != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = (if (delta >= 0) "▲ +" else "▼ -") + formatNum(abs(delta)),
                        color = if (delta >= 0) palette.green else palette.red,
                        fontFamily = BarlowCondensed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.scale(deltaScale)
                    )
                }

                if (winRate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${s.winRateLabel}: $winRate%",
                        color = palette.accent,
                        fontFamily = BarlowCondensed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (winStreak >= 2) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val streakIntensity = ((winStreak - 2).coerceIn(0, 8)) / 8f
                    val streakColor = androidx.compose.ui.graphics.lerp(Color(0xFFFFA726), palette.red, streakIntensity)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.scale(streakFlameScale * flameFlicker)) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size((22 + streakIntensity * 10).dp).background(brush = Brush.radialGradient(colors = listOf(streakColor.copy(alpha = flameGlowAlpha), Color.Transparent)), shape = CircleShape))
                            Text(text = "🔥", fontSize = (14 + streakIntensity * 8).sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${s.winStreakLabel} x$winStreak", color = streakColor, fontSize = (13 + streakIntensity * 3).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (currentRank != null) {
                val progressInfo = getProgressToNextRank(currentRank, globalRank)

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(end = 8.dp)) {
                    Image(painter = painterResource(id = rankLogoResFor(currentRank, globalRank)), contentDescription = rankName, modifier = Modifier.size(140.dp))

                    if (progressInfo != null) {
                        val (_, remaining, nextName) = progressInfo
                        val localizedNextName = getLocalizedRankName(nextName, isEnglish)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${formatNum(remaining)} ${s.rsRemaining}\n${s.nextRankPrefix} $localizedNextName", color = palette.textMuted, fontSize = 12.sp, letterSpacing = 0.5.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                    } else if (isRuby && globalRank != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${formatNum(currentRank)} RS", color = palette.cyan, fontSize = 13.sp, letterSpacing = 1.0.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = s.rubyMax, color = palette.cyan, fontSize = 12.sp, letterSpacing = 1.0.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                    }
                }

                if (progressInfo != null) {
                    val (percentage, _, _) = progressInfo
                    Box(modifier = Modifier.width(12.dp).fillMaxHeight().padding(vertical = 12.dp).clip(RoundedCornerShape(6.dp)).background(palette.surfaceAlt)) {
                        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = percentage / 100f).align(Alignment.BottomCenter).clip(RoundedCornerShape(6.dp)).background(verticalProgressBrush))
                    }
                } else {
                    Box(modifier = Modifier.width(12.dp).fillMaxHeight().padding(vertical = 12.dp).clip(RoundedCornerShape(6.dp)).background(verticalProgressBrush))
                }
            }
        }
    }
}

@Composable
internal fun SparklineChart(entries: List<RankEntry>, palette: Palette) {
    if (entries.isEmpty()) return
    val min = entries.minOf { it.rank }.toFloat()
    val max = entries.maxOf { it.rank }.toFloat()
    val pad = maxOf((max - min) * 0.15f, 50f)
    val dMin = (min - pad).coerceAtLeast(0f)
    val dMax = max + pad
    val span = (dMax - dMin).coerceAtLeast(1f)

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stepX = w / (entries.size - 1).coerceAtLeast(1)
        fun y(v: Float) = h - ((v - dMin) / span * h)
        fun x(i: Int) = stepX * i

        val pointColors = entries.mapIndexed { i, p ->
            if (i == 0) palette.cyan
            else if (p.rank >= entries[i - 1].rank) palette.green else palette.red
        }

        val path = androidx.compose.ui.graphics.Path().apply {
            entries.forEachIndexed { i, p ->
                val px = x(i)
                val py = y(p.rank.toFloat())
                if (i == 0) moveTo(px, py)
                else {
                    val prevX = x(i - 1)
                    val prevY = y(entries[i - 1].rank.toFloat())
                    val ctrlX = prevX + (px - prevX) / 2f
                    cubicTo(ctrlX, prevY, ctrlX, py, px, py)
                }
            }
        }

        val isOverallGain = entries.last().rank >= entries.first().rank
        val gradientColor = if (isOverallGain) palette.green else palette.red
        val brush = androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(gradientColor.copy(alpha = 0.5f), gradientColor.copy(alpha = 0.0f)),
            startY = 0f,
            endY = h
        )

        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = fillPath, brush = brush)
        
        drawPath(path = path, color = gradientColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

        entries.forEachIndexed { i, p ->
            val cx = x(i)
            val cy = y(p.rank.toFloat())
            drawCircle(color = palette.surface, radius = 4f, center = androidx.compose.ui.geometry.Offset(cx, cy))
            drawCircle(color = pointColors[i], radius = 2.5f, center = androidx.compose.ui.geometry.Offset(cx, cy))
        }
    }
}