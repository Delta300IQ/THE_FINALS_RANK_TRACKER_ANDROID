package com.example.finalsranktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun Modifier.neumorphicCard(
    palette: Palette,
    isDarkMode: Boolean,
    cornerRadius: Dp = 12.dp,
    baseColor: Color? = null,
    accentColor: Color? = null
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val shadowAlpha = if (isDarkMode) 0.7f else 0.30f
    val highlightAlpha = if (isDarkMode) 0.22f else 1f

    val bgTop = baseColor ?: palette.surfaceAlt
    val bgBottom = (baseColor ?: palette.surface).let {
        androidx.compose.ui.graphics.lerp(it, if (isDarkMode) Color.Black else palette.border, 0.16f)
    }

    val borderColors = if (accentColor != null) {
        listOf(
            androidx.compose.ui.graphics.lerp(accentColor, Color.White, if (isDarkMode) 0.3f else 0.65f),
            accentColor,
            accentColor.copy(alpha = 0.55f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = highlightAlpha),
            palette.border.copy(alpha = 0.5f),
            Color.Black.copy(alpha = if (isDarkMode) 0.5f else 0.12f)
        )
    }

    return this
        .shadow(
            elevation = 9.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = shadowAlpha),
            spotColor = Color.Black.copy(alpha = shadowAlpha),
            clip = false
        )
        .background(
            brush = Brush.linearGradient(
                colors = listOf(bgTop, bgBottom),
                start = Offset(0f, 0f),
                end = Offset(400f, 400f)
            ),
            shape = shape
        )
        .border(
            width = 1.3.dp,
            brush = Brush.linearGradient(
                colors = borderColors,
                start = Offset(0f, 0f),
                end = Offset(300f, 300f)
            ),
            shape = shape
        )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RankTrackerApp()
        }
    }
}

@Composable
fun RankTrackerApp() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    var exportMenuExpanded by remember { mutableStateOf(false) }
    var importMenuExpanded by remember { mutableStateOf(false) }

    var allSeasons by remember { mutableStateOf(loadAllSeasons(context)) }
    var selectedSeason by remember { mutableStateOf(loadSelectedSeason(context)) }

    var isDarkMode by remember { mutableStateOf(loadDarkMode(context)) }
    var isEnglish by remember { mutableStateOf(loadLanguage(context)) }

    var inputValue by remember { mutableStateOf("") }
    var showInvalidScoreMsg by remember { mutableStateOf(false) }
    var showTypoErrorMsg by remember { mutableStateOf(false) }
    var showSavedMsg by remember { mutableStateOf(false) }

    var showHistory by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showUndoConfirm by remember { mutableStateOf(false) }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var chartEditingIndex by remember { mutableStateOf<Int?>(null) }
    var editValue by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf(setOf<String>()) }
    var editInvalidScore by remember { mutableStateOf(false) }
    var deleteConfirmIndex by remember { mutableStateOf<Int?>(null) }
    var expandedHistoryIndex by remember { mutableStateOf<Int?>(null) }

    var showHistorySortMenu by remember { mutableStateOf(false) }
    var showHistoryFilterMenu by remember { mutableStateOf(false) }
    var historySortMode by remember { mutableStateOf(HistorySortMode.NEWEST_FIRST) }
    var historyNoteFilter by remember { mutableStateOf<Set<String>>(emptySet()) }

    var rankGoals by remember { mutableStateOf(loadRankGoals(context)) }
    var showGoalSelector by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableStateOf(1f) }
    var chartPeriod by remember { mutableStateOf(ChartPeriod.ALL) }

    var showExportConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Map<Int, List<RankEntry>>?>(null) }
    var importError by remember { mutableStateOf(false) }
    var showImportSuccess by remember { mutableStateOf(false) }

    var showSplash by remember { mutableStateOf(true) }
    var showExportReminder by remember { mutableStateOf(false) }

    var compareSeasonsEnabled by remember { mutableStateOf(false) }
    var compareSeasonId by remember { mutableStateOf<Int?>(null) }

    var selectedNotes by remember { mutableStateOf(setOf<String>()) }
    var customTags by remember { mutableStateOf(loadCustomTags(context)) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val saveButtonScale = remember { Animatable(1f) }

    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val jsonString = buildAllSeasonsJson(allSeasons)
            val os = context.contentResolver.openOutputStream(uri)
            if (os != null) {
                try {
                    os.write(jsonString.toByteArray())
                } catch (e: Exception) {
                    importError = true
                } finally {
                    os.close()
                }
                showImportSuccess = false
                importError = false
                showExportConfirm = true
                saveLastExportTimestamp(context, System.currentTimeMillis())
                showExportReminder = false
            } else {
                importError = true
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                try {
                    val bytes = inputStream.readBytes()
                    val jsonString = String(bytes)
                    val parsed = parseAllSeasonsJson(jsonString)
                    if (parsed != null) {
                        pendingImport = parsed
                        showImportConfirm = true
                        importError = false
                        showExportConfirm = false
                    } else {
                        importError = true
                        showImportConfirm = false
                    }
                } catch (e: Exception) {
                    importError = true
                    showImportConfirm = false
                } finally {
                    inputStream.close()
                }
            } else {
                importError = true
                showImportConfirm = false
            }
        }
    }

    LaunchedEffect(showExportConfirm) {
        if (showExportConfirm) { delay(3500); showExportConfirm = false }
    }

    LaunchedEffect(showImportSuccess) {
        if (showImportSuccess) { delay(3500); showImportSuccess = false }
    }

    LaunchedEffect(allSeasons, showSplash) {
        if (!showSplash) {
            if (allSeasons.values.any { it.isNotEmpty() }) {
                val now = System.currentTimeMillis()
                val lastExport = loadLastExportTimestamp(context)
                val lastDismiss = loadExportReminderDismissTimestamp(context)
                showExportReminder = (now - lastExport >= EXPORT_REMINDER_INTERVAL_MS && now - lastDismiss >= EXPORT_REMINDER_INTERVAL_MS)
            } else {
                showExportReminder = false
            }
        }
    }

    LaunchedEffect(showInvalidScoreMsg) {
        if (showInvalidScoreMsg) { delay(3500); showInvalidScoreMsg = false }
    }

    LaunchedEffect(showTypoErrorMsg) {
        if (showTypoErrorMsg) { delay(3500); showTypoErrorMsg = false }
    }

    LaunchedEffect(showSavedMsg) {
        if (showSavedMsg) { delay(3000); showSavedMsg = false }
    }

    var seasonTransitionAlpha by remember { mutableStateOf(1f) }
    LaunchedEffect(selectedSeason) {
        seasonTransitionAlpha = 0f
        delay(100)
        seasonTransitionAlpha = 1f
    }
    val contentAlpha by animateFloatAsState(targetValue = seasonTransitionAlpha, animationSpec = tween(300), label = "ContentAlpha")
    val themeProgress by animateFloatAsState(targetValue = if (isDarkMode) 0f else 1f, animationSpec = tween(400), label = "Theme")
    val palette = lerpPalette(DarkPalette, LightPalette, themeProgress)
    val s = if (isEnglish) EN else FR
    val tagGroups = getTagGroups(isEnglish)

    fun resetSelections() {
        selectedIndex = null; editingIndex = null; editNotes = emptySet(); editInvalidScore = false
        deleteConfirmIndex = null; expandedHistoryIndex = null; showResetConfirm = false; showUndoConfirm = false
        showGoalSelector = false; historyNoteFilter = emptySet(); chartEditingIndex = null; tagToDelete = null
    }

    fun persistSeason(next: List<RankEntry>) {
        val updated = allSeasons.toMutableMap()
        updated[selectedSeason] = next
        allSeasons = updated
        saveAllSeasons(context, updated)
        resetSelections()
    }

    val currentSeasonEntries = allSeasons[selectedSeason] ?: emptyList()
    val currentRank = currentSeasonEntries.lastOrNull()?.rank
    val previousRank = if (currentSeasonEntries.size > 1) currentSeasonEntries[currentSeasonEntries.size - 2].rank else null
    val delta = if (currentRank != null && previousRank != null) currentRank - previousRank else null

    val peakRank = currentSeasonEntries.maxOfOrNull { it.rank }
    val lowRank = currentSeasonEntries.minOfOrNull { it.rank }
    val peakRankName = peakRank?.let { getLocalizedRankName(rankNameFor(it), isEnglish) }
    val lowRankName = lowRank?.let { getLocalizedRankName(rankNameFor(it), isEnglish) }

    val deltas = currentSeasonEntries.map { it.rank }.zipWithNext { a, b -> b - a }
    val gains = deltas.filter { it > 0 }
    val losses = deltas.filter { it < 0 }
    val winStreak = deltas.asReversed().takeWhile { it > 0 }.size

    val avgChange = if (deltas.isNotEmpty()) deltas.average() else null
    val avgGain = if (gains.isNotEmpty()) gains.average() else null
    val avgLoss = if (losses.isNotEmpty()) losses.average() else null
    val biggestGain = gains.maxOrNull()
    val biggestDrop = losses.minOrNull()
    val bestWinStreak = longestStreak(deltas) { it > 0 }
    val bestLoseStreak = longestStreak(deltas) { it < 0 }

    val totalGainRS = gains.sum()
    val totalLossRS = losses.sumOf { abs(it) }
    val winRate = if (totalGainRS + totalLossRS > 0) ((totalGainRS.toFloat() / (totalGainRS + totalLossRS)) * 100).roundToInt() else null

    val goalValue = rankGoals[selectedSeason]

    val estimatedMatchesToGoal: Int? = if (goalValue != null && currentRank != null && goalValue > currentRank) {
        estimateMatchesToGoal(currentSeasonEntries, currentRank, goalValue)
    } else null

    val allPoints = currentSeasonEntries.mapIndexed { i, e -> ChartPoint(i, e.rank, e.timestamp) }
    val nowMillis = System.currentTimeMillis()
    val chartPoints = when (chartPeriod) {
        ChartPeriod.WEEK -> allPoints.filter { it.timestamp >= nowMillis - 7L * 24 * 3600 * 1000 }
        ChartPeriod.MONTH -> allPoints.filter { it.timestamp >= nowMillis - 30L * 24 * 3600 * 1000 }
        ChartPeriod.ALL -> allPoints
    }

    val animatedRankScore by animateIntAsState(targetValue = currentRank ?: 0, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "RSScore")
    val deltaScale by animateFloatAsState(targetValue = if (delta != null && delta >= 0) 1.1f else 1.0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "DeltaPulse")

    val streakFlameScale = remember { Animatable(1f) }
    LaunchedEffect(winStreak) {
        if (winStreak >= 2) {
            streakFlameScale.snapTo(1.5f)
            streakFlameScale.animateTo(targetValue = 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        } else {
            streakFlameScale.snapTo(1f)
        }
    }
    val streakIntensity = ((winStreak - 2).coerceIn(0, 8)) / 8f
    val streakColor = androidx.compose.ui.graphics.lerp(Color(0xFFFFA726), palette.red, streakIntensity)

    val flameTransition = rememberInfiniteTransition(label = "Flicker")
    val flameFlicker by flameTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "FlameFlicker"
    )
    val flameGlowAlpha by flameTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(animation = tween(500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "FlameGlow"
    )

    val rankName = rankNameFor(currentRank ?: 0)

    val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")
    val offset by infiniteTransition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(animation = tween(4500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "Offset"
    )

    val animatedRankBrush = Brush.linearGradient(
        colors = when {
            rankName.contains("Bronze") -> listOf(Color(0xFF7A4B28), Color(0xFFCD7F32), Color(0xFFF0C294), Color(0xFFCD7F32), Color(0xFF7A4B28))
            rankName.contains("Silver") || rankName.contains("Argent") -> listOf(Color(0xFF707B7C), Color(0xFFBDC3C7), Color(0xFFF2F4F4), Color(0xFFBDC3C7), Color(0xFF707B7C))
            rankName.contains("Gold") || rankName.contains("Or") -> listOf(Color(0xFF9A7B31), Color(0xFFE6C667), Color(0xFFFFF5D1), Color(0xFFE6C667), Color(0xFF9A7B31))
            rankName.contains("Platinum") || rankName.contains("Platine") -> listOf(Color(0xFF3A506B), Color(0xFF64DFDF), Color(0xFFE0FAFF), Color(0xFF64DFDF), Color(0xFF3A506B))
            rankName.contains("Diamond") || rankName.contains("Diamant") -> listOf(Color(0xFF480CA8), Color(0xFF4CC9F0), Color(0xFFEAF8FF), Color(0xFF4CC9F0), Color(0xFF480CA8))
            rankName.contains("Ruby") || (currentRank ?: 0) >= 50000 -> listOf(Color(0xFF800020), Color(0xFFFF0F50), Color(0xFFFFCCD5), Color(0xFFFF0F50), Color(0xFF800020))
            else -> listOf(palette.textPrimary, palette.textPrimary, palette.textPrimary)
        },
        start = Offset(offset, 0f),
        end = Offset(offset + 300f, 300f)
    )

    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = palette.accent, onPrimary = palette.accentOn,
            background = palette.bg, onBackground = palette.textPrimary,
            surface = palette.surface, onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceAlt, onSurfaceVariant = palette.textMuted,
            error = palette.red, outline = palette.border
        )
    } else {
        lightColorScheme(
            primary = palette.accent, onPrimary = palette.accentOn,
            background = palette.bg, onBackground = palette.textPrimary,
            surface = palette.surface, onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceAlt, onSurfaceVariant = palette.textMuted,
            error = palette.red, outline = palette.border
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
        ) {
            AnimatedVisibility(
                visible = !showSplash,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                    initialOffsetY = { it / 8 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.bg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            .alpha(contentAlpha)
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {

                        HeaderSection(
                            eyebrow = s.eyebrow, title = s.title, isDarkMode = isDarkMode,
                            darkModeLabel = s.darkModeLabel, lightModeLabel = s.lightModeLabel,
                            palette = palette,
                            onToggleDarkMode = {
                                isDarkMode = !isDarkMode
                                saveDarkMode(context, isDarkMode)
                            }
                        )

                        if (showExportReminder) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphicCard(palette, isDarkMode, 10.dp, baseColor = palette.surfaceAlt, accentColor = palette.accent)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = s.exportReminderMessage, color = palette.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = s.exportReminderDismiss, color = palette.cyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        saveExportReminderDismissTimestamp(context, System.currentTimeMillis())
                                        showExportReminder = false
                                    }.padding(4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SeasonLanguageSelector(
                            selectedSeason = selectedSeason, isEnglish = isEnglish, seasonLabel = s.season, palette = palette,
                            onPreviousSeason = {
                                if (selectedSeason > 1) {
                                    selectedSeason -= 1
                                    saveSelectedSeason(context, selectedSeason)
                                    resetSelections()
                                }
                            },
                            onNextSeason = {
                                selectedSeason += 1
                                saveSelectedSeason(context, selectedSeason)
                                resetSelections()
                            },
                            onToggleLanguage = { lang ->
                                isEnglish = lang
                                saveLanguage(context, lang)
                                resetSelections()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CurrentRankCard(
                            currentRank = currentRank, animatedRankScore = animatedRankScore, animatedRankBrush = animatedRankBrush,
                            delta = delta, deltaScale = deltaScale, winRate = winRate, winStreak = winStreak,
                            streakFlameScale = streakFlameScale.value, flameFlicker = flameFlicker, flameGlowAlpha = flameGlowAlpha,
                            streakColor = streakColor, rsRemaining = s.rsRemaining, nextRankPrefix = s.nextRankPrefix, rubyMax = s.rubyMax,
                            isEnglish = isEnglish, isDarkMode = isDarkMode, palette = palette, s = s
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        RankGoalCard(
                            currentRank = currentRank, selectedSeason = selectedSeason, goalValue = goalValue,
                            showGoalSelector = showGoalSelector, estimatedMatchesToGoal = estimatedMatchesToGoal,
                            isEnglish = isEnglish, palette = palette, isDarkMode = isDarkMode, s = s,
                            onToggleSelector = { showGoalSelector = !showGoalSelector },
                            onSelectGoal = { threshold ->
                                val updated = rankGoals.toMutableMap()
                                updated[selectedSeason] = threshold
                                rankGoals = updated
                                saveRankGoals(context, updated)
                                showGoalSelector = false
                            },
                            onClearGoal = {
                                val updated = rankGoals.toMutableMap()
                                updated.remove(selectedSeason)
                                rankGoals = updated
                                saveRankGoals(context, updated)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        StatsSection(
                            currentSeasonEntries = currentSeasonEntries, peakRank = peakRank, peakRankName = peakRankName,
                            lowRank = lowRank, lowRankName = lowRankName, avgChange = avgChange, avgGain = avgGain, avgLoss = avgLoss,
                            biggestGain = biggestGain, biggestDrop = biggestDrop, bestWinStreak = bestWinStreak, bestLoseStreak = bestLoseStreak,
                            showStats = showStats, compareSeasonsEnabled = compareSeasonsEnabled, compareSeasonId = compareSeasonId,
                            allSeasons = allSeasons, selectedSeason = selectedSeason, isEnglish = isEnglish, isDarkMode = isDarkMode,
                            palette = palette, s = s,
                            onToggleStats = { showStats = !showStats },
                            onToggleCompare = {
                                compareSeasonsEnabled = !compareSeasonsEnabled
                                if (!compareSeasonsEnabled) compareSeasonId = null
                            },
                            onSelectCompareSeason = { season -> compareSeasonId = season }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CandlestickChartSection(
                            chartPoints = chartPoints, chartPeriod = chartPeriod, zoomScale = zoomScale,
                            selectedIndex = selectedIndex, currentSeasonEntries = currentSeasonEntries,
                            lowRank = lowRank, peakRank = peakRank, isEnglish = isEnglish, isDarkMode = isDarkMode,
                            palette = palette, s = s,
                            onSelectPeriod = { period ->
                                chartPeriod = period
                                selectedIndex = null
                            },
                            onZoomIn = { zoomScale = (zoomScale * 1.25f).coerceAtMost(3f) },
                            onZoomOut = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.5f) },
                            onSelectPoint = { point -> selectedIndex = point },
                            onChartEditClick = { idx -> chartEditingIndex = idx }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        InputSection(
                            inputValue = inputValue, currentSeasonEntries = currentSeasonEntries, selectedNotes = selectedNotes,
                            customTags = customTags, tagGroups = tagGroups, showInvalidScoreMsg = showInvalidScoreMsg,
                            showTypoErrorMsg = showTypoErrorMsg, showSavedMsg = showSavedMsg, isEnglish = isEnglish,
                            isDarkMode = isDarkMode, palette = palette, s = s, saveButtonScale = saveButtonScale.value,
                            onValueChange = { value -> inputValue = value },
                            onSave = { parsed ->
                                val lastRank = currentSeasonEntries.lastOrNull()?.rank
                                if (parsed > 85000) {
                                    showInvalidScoreMsg = true; showTypoErrorMsg = false; showSavedMsg = false
                                } else if (lastRank != null && abs(parsed - lastRank) > 3500) {
                                    showTypoErrorMsg = true; showInvalidScoreMsg = false; showSavedMsg = false
                                } else {
                                    val newEntry = RankEntry(
                                        rank = parsed, timestamp = System.currentTimeMillis(), notes = selectedNotes.toList()
                                    )
                                    persistSeason(currentSeasonEntries + newEntry)
                                    inputValue = ""; selectedNotes = emptySet(); showInvalidScoreMsg = false
                                    showTypoErrorMsg = false; showSavedMsg = true
                                    coroutineScope.launch {
                                        saveButtonScale.snapTo(0.85f)
                                        saveButtonScale.animateTo(targetValue = 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            },
                            onToggleTag = { tag ->
                                val isSelected = selectedNotes.contains(tag)
                                selectedNotes = if (isSelected) {
                                    selectedNotes - tag
                                } else {
                                    val inGroup = tagGroups.find { it.contains(tag) }
                                    if (inGroup != null) selectedNotes.filter { !inGroup.contains(it) }.toSet() + tag
                                    else selectedNotes + tag
                                }
                            },
                            onAddCustomTag = { newTag ->
                                if (!customTags.contains(newTag)) {
                                    val updated = customTags + newTag
                                    customTags = updated
                                    saveCustomTags(context, updated)
                                }
                            },
                            onLongClickCustomTag = { tagToDelete = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 10.dp)
                                .clickable { uriHandler.openUri("https://www.davg25.com/app/the-finals-leaderboard-tracker/") }
                                .padding(12.dp)
                        ) {
                            Text(text = s.top10kLabel, color = palette.cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 10.dp)
                                .clickable { uriHandler.openUri("https://www.reachthefinals.com/patchnotes") }
                                .padding(12.dp)
                        ) {
                            Text(text = s.patchNotesLabel, color = palette.cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { showUndoConfirm = true }, enabled = currentSeasonEntries.isNotEmpty()) {
                                Text(s.undoLast, color = Color.Red, fontSize = 13.sp)
                            }
                            if (currentSeasonEntries.isNotEmpty()) {
                                TextButton(onClick = { showHistory = !showHistory }) {
                                    Text(if (showHistory) s.historyHide else s.historyShow, color = palette.cyan, fontSize = 13.sp)
                                }
                            }
                        }

                        if (showUndoConfirm) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt, accentColor = palette.red).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(s.undoConfirmMsg, color = palette.textPrimary, fontSize = 13.sp)
                                Row {
                                    TextButton(onClick = { persistSeason(currentSeasonEntries.dropLast(1)); showUndoConfirm = false }) { Text(s.confirmWord, color = palette.red, fontWeight = FontWeight.SemiBold) }
                                    TextButton(onClick = { showUndoConfirm = false }) { Text(s.cancelWord, color = palette.textMuted) }
                                }
                            }
                        }

                        HistoryEntriesList(
                            currentSeasonEntries = currentSeasonEntries, showHistory = showHistory, showHistorySortMenu = showHistorySortMenu,
                            showHistoryFilterMenu = showHistoryFilterMenu, historySortMode = historySortMode, historyNoteFilter = historyNoteFilter,
                            editingIndex = editingIndex, editValue = editValue, editNotes = editNotes, customTags = customTags, tagGroups = tagGroups,
                            editInvalidScore = editInvalidScore, deleteConfirmIndex = deleteConfirmIndex, expandedHistoryIndex = expandedHistoryIndex,
                            lowRank = lowRank, peakRank = peakRank, isEnglish = isEnglish, isDarkMode = isDarkMode, palette = palette, s = s,
                            onToggleSortMenu = { showHistorySortMenu = !showHistorySortMenu; showHistoryFilterMenu = false },
                            onToggleFilterMenu = { showHistoryFilterMenu = !showHistoryFilterMenu; showHistorySortMenu = false },
                            onSelectSortMode = { mode -> historySortMode = mode; showHistorySortMenu = false },
                            onToggleFilterNote = { tag -> val isSelected = historyNoteFilter.contains(tag); historyNoteFilter = if (isSelected) historyNoteFilter - tag else historyNoteFilter + tag },
                            onClearFilters = { historyNoteFilter = emptySet() },
                            onStartEdit = { idx, value, tags -> editingIndex = idx; editValue = value; editNotes = tags; editInvalidScore = false },
                            onEditValueChange = { value -> editValue = value },
                            onToggleEditTag = { tag ->
                                val isSelected = editNotes.contains(tag)
                                editNotes = if (isSelected) { editNotes - tag } else { val inGroup = tagGroups.find { it.contains(tag) }; if (inGroup != null) editNotes.filter { !inGroup.contains(it) }.toSet() + tag else editNotes + tag }
                            },
                            onAddCustomTag = { newTag -> if (!customTags.contains(newTag)) { val updated = customTags + newTag; customTags = updated; saveCustomTags(context, updated) } },
                            onLongClickCustomTag = { tagToDelete = it },
                            onSaveEdit = { idx, parsed, notes ->
                                val next = currentSeasonEntries.toMutableList()
                                next[idx] = next[idx].copy(rank = parsed, notes = notes.toList())
                                persistSeason(next)
                                editingIndex = null
                            },
                            onCancelEdit = { editingIndex = null; editInvalidScore = false },
                            onStartDelete = { deleteConfirmIndex = it },
                            onConfirmDelete = { idx -> persistSeason(currentSeasonEntries.filterIndexed { i, _ -> i != idx }); deleteConfirmIndex = null },
                            onCancelDelete = { deleteConfirmIndex = null },
                            onToggleExpandHistory = { idx -> expandedHistoryIndex = if (expandedHistoryIndex == idx) null else idx }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        BottomActionsSection(
                            currentSeasonEntries = currentSeasonEntries, showResetConfirm = showResetConfirm, exportMenuExpanded = exportMenuExpanded,
                            importMenuExpanded = importMenuExpanded, showExportConfirm = showExportConfirm, importError = importError,
                            showImportConfirm = showImportConfirm, pendingImport = pendingImport, showImportSuccess = showImportSuccess,
                            palette = palette, isDarkMode = isDarkMode, s = s,
                            onToggleReset = { showResetConfirm = true },
                            onConfirmReset = { persistSeason(emptyList()); showHistory = false; showResetConfirm = false },
                            onCancelReset = { showResetConfirm = false },
                            onExportMenuClick = { exportMenuExpanded = true },
                            onExportDismiss = { exportMenuExpanded = false },
                            onExportClipboard = {
                                exportMenuExpanded = false; clipboardManager.setText(AnnotatedString(buildExportText(allSeasons, isEnglish)))
                                showExportConfirm = true; importError = false; showImportConfirm = false; pendingImport = null
                                saveLastExportTimestamp(context, System.currentTimeMillis()); showExportReminder = false
                            },
                            onExportJson = { exportMenuExpanded = false; exportJsonLauncher.launch("finals_rank_tracker_export.json") },
                            onImportMenuClick = { importMenuExpanded = true },
                            onImportDismiss = { importMenuExpanded = false },
                            onImportClipboard = {
                                importMenuExpanded = false; val clipText = clipboardManager.getText()?.text; val parsed = if (clipText != null) parseImportText(clipText) else null
                                showExportConfirm = false
                                if (parsed != null) { pendingImport = parsed; showImportConfirm = true; importError = false }
                                else { importError = true; showImportConfirm = false; pendingImport = null }
                            },
                            onImportJson = { importMenuExpanded = false; importJsonLauncher.launch(arrayOf("*/*")) },
                            onConfirmImport = { val importData = pendingImport; if (importData != null) { allSeasons = importData; saveAllSeasons(context, importData); resetSelections(); showImportConfirm = false; pendingImport = null; showImportSuccess = true } },
                            onCancelImport = { showImportConfirm = false; pendingImport = null },
                            onFooterClick = { uriHandler.openUri("https://discord.gg/ZjnAMKnc") }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200))
            ) {
                SplashUI(palette = palette, isEnglish = isEnglish, currentRank = currentRank, animatedRankBrush = animatedRankBrush, onDismiss = { showSplash = false })
            }

            // --- Dialogues Globaux ---

            if (tagToDelete != null) {
                AlertDialog(
                    onDismissRequest = { tagToDelete = null },
                    containerColor = palette.surface,
                    title = { Text(s.deleteTagTitle, color = palette.textPrimary) },
                    text = { Text("${s.deleteTagDesc}\n\"$tagToDelete\"", color = palette.textMuted) },
                    confirmButton = {
                        TextButton(onClick = {
                            val updated = customTags - tagToDelete!!
                            customTags = updated
                            saveCustomTags(context, updated)
                            selectedNotes = selectedNotes - tagToDelete!!
                            historyNoteFilter = historyNoteFilter - tagToDelete!!
                            editNotes = editNotes - tagToDelete!!
                            tagToDelete = null
                        }) { Text(s.confirmWord, color = palette.red) }
                    },
                    dismissButton = {
                        TextButton(onClick = { tagToDelete = null }) { Text(s.cancelWord, color = palette.textMuted) }
                    }
                )
            }

            if (chartEditingIndex != null) {
                val idx = chartEditingIndex!!
                val entryToEdit = currentSeasonEntries.getOrNull(idx)
                if (entryToEdit != null) {
                    ChartEditMatchDialog(
                        idx = idx, initialRank = entryToEdit.rank.toString(), initialNotes = entryToEdit.notes.toSet(),
                        customTags = customTags, tagGroups = tagGroups, palette = palette, s = s, isEnglish = isEnglish,
                        onSave = { parsed, notes ->
                            val next = currentSeasonEntries.toMutableList()
                            next[idx] = next[idx].copy(rank = parsed, notes = notes.toList())
                            persistSeason(next)
                            chartEditingIndex = null
                        },
                        onDismiss = { chartEditingIndex = null },
                        onAddCustomTag = { newTag ->
                            if (!customTags.contains(newTag)) {
                                val updated = customTags + newTag
                                customTags = updated
                                saveCustomTags(context, updated)
                            }
                        },
                        onLongClickCustomTag = { tagToDelete = it }
                    )
                } else {
                    chartEditingIndex = null
                }
            }

        }
    }
}

// ---------- COMPOSANTS EXTRAITS ----------

@Composable
internal fun HeaderSection(eyebrow: String, title: String, isDarkMode: Boolean, darkModeLabel: String, lightModeLabel: String, palette: Palette, onToggleDarkMode: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column {
            Text(text = eyebrow, color = palette.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            Text(text = title, color = palette.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = if (isDarkMode) darkModeLabel else lightModeLabel, color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onToggleDarkMode() }.padding(4.dp))
    }
}

@Composable
internal fun SeasonLanguageSelector(selectedSeason: Int, isEnglish: Boolean, seasonLabel: String, palette: Palette, onPreviousSeason: () -> Unit, onNextSeason: () -> Unit, onToggleLanguage: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "◀ ", color = if (selectedSeason > 1) palette.accent else palette.border, fontSize = 14.sp, modifier = Modifier.clickable(enabled = selectedSeason > 1) { onPreviousSeason() }.padding(6.dp))
            Text(text = "$seasonLabel $selectedSeason", color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            Text(text = " ▶", color = palette.accent, fontSize = 14.sp, modifier = Modifier.clickable { onNextSeason() }.padding(6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "FR", color = if (!isEnglish) palette.accent else palette.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onToggleLanguage(false) }.padding(4.dp))
            Text(text = "/", color = palette.textMuted, fontSize = 12.sp)
            Text(text = "EN", color = if (isEnglish) palette.accent else palette.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onToggleLanguage(true) }.padding(4.dp))
        }
    }
}

@Composable
internal fun CurrentRankCard(currentRank: Int?, animatedRankScore: Int, animatedRankBrush: Brush, delta: Int?, deltaScale: Float, winRate: Int?, winStreak: Int, streakFlameScale: Float, flameFlicker: Float, flameGlowAlpha: Float, streakColor: Color, rsRemaining: String, nextRankPrefix: String, rubyMax: String, isEnglish: Boolean, isDarkMode: Boolean, palette: Palette, s: Strings) {
    val rankName = rankNameFor(currentRank ?: 0)
    Box(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 12.dp).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = s.currentRankLabel, color = palette.textMuted, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = formatNum(animatedRankScore), style = TextStyle(brush = animatedRankBrush, fontSize = 45.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                if (delta != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = (if (delta >= 0) "▲ +" else "▼ -") + formatNum(abs(delta)), color = if (delta >= 0) palette.green else palette.red, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, modifier = Modifier.scale(deltaScale))
                }
                if (winRate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${s.winRateLabel}: $winRate%", color = palette.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                }
                if (winStreak >= 2) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val streakIntensity = ((winStreak - 2).coerceIn(0, 8)) / 8f
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.scale(streakFlameScale * flameFlicker)) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size((22 + streakIntensity * 10).dp).background(brush = Brush.radialGradient(colors = listOf(streakColor.copy(alpha = flameGlowAlpha), Color.Transparent)), shape = CircleShape))
                            Text(text = "🔥", fontSize = (14 + streakIntensity * 8).sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${s.winStreakLabel} x$winStreak", color = streakColor, fontSize = (13 + streakIntensity * 3).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            if (currentRank != null) {
                val progressInfo = getProgressToNextRank(currentRank)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Image(painter = painterResource(id = rankLogoResFor(currentRank)), contentDescription = rankName, modifier = Modifier.size(130.dp).offset(y = (-5).dp))
                    if (progressInfo != null) {
                        val (percentage, remaining, nextName) = progressInfo
                        val localizedNextName = getLocalizedRankName(nextName, isEnglish)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            LinearProgressIndicator(progress = percentage / 100f, modifier = Modifier.width(100.dp).height(8.dp).clip(RoundedCornerShape(4.dp)), color = palette.accent, trackColor = palette.surfaceAlt)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "$percentage%", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Text(text = "${formatNum(remaining)} $rsRemaining $nextRankPrefix $localizedNextName", color = palette.textMuted, fontSize = 11.sp, letterSpacing = 1.0.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            LinearProgressIndicator(progress = 1f, modifier = Modifier.width(100.dp).height(8.dp).clip(RoundedCornerShape(4.dp)), color = palette.accent, trackColor = palette.surfaceAlt)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "100%", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Text(text = rubyMax, color = palette.cyan, fontSize = 11.sp, letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RankGoalCard(currentRank: Int?, selectedSeason: Int, goalValue: Int?, showGoalSelector: Boolean, estimatedMatchesToGoal: Int?, isEnglish: Boolean, palette: Palette, isDarkMode: Boolean, s: Strings, onToggleSelector: () -> Unit, onSelectGoal: (Int) -> Unit, onClearGoal: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 12.dp).padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = s.rankGoalTitle, color = palette.textMuted, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
                if (goalValue != null && !showGoalSelector) {
                    Row {
                        Text(text = "✎", color = palette.cyan, fontSize = 13.sp, modifier = Modifier.clickable { onToggleSelector() }.padding(4.dp))
                        Text(text = "✕", color = palette.red, fontSize = 13.sp, modifier = Modifier.clickable { onClearGoal() }.padding(4.dp))
                    }
                }
            }
            if (goalValue == null || showGoalSelector) {
                Spacer(modifier = Modifier.height(2.dp))
                val qualifyingTiers = RANK_TIERS.filter { currentRank == null || it.first > currentRank }
                Text(text = s.rankGoalPlaceholder, color = palette.cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onToggleSelector() })
                if (showGoalSelector) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (qualifyingTiers.isEmpty()) {
                        Text(s.rankGoalAlreadyMax, color = palette.textMuted, fontSize = 12.sp)
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()).neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt).padding(8.dp)) {
                            qualifyingTiers.forEach { (threshold, name) ->
                                Text(text = "${getLocalizedRankName(name, isEnglish)} · ${formatNum(threshold)} RS", color = palette.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth().clickable { onSelectGoal(threshold) }.padding(vertical = 6.dp))
                            }
                        }
                    }
                }
            } else {
                val goalRankName = getLocalizedRankName(rankNameFor(goalValue), isEnglish)
                Row(modifier = Modifier.fillMaxWidth().offset(y = (-4).dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${formatNum(goalValue)} RS · $goalRankName", color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        when {
                            currentRank != null && currentRank >= goalValue -> { Text(s.rankGoalReached, color = palette.green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            estimatedMatchesToGoal != null -> { Text(text = "${s.rankGoalEstimatePrefix}$estimatedMatchesToGoal ${s.rankGoalEstimateSuffix} $goalRankName", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            else -> { Text(s.rankGoalNotEnoughData, color = palette.textMuted, fontSize = 12.sp) }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(painter = painterResource(id = rankLogoResFor(goalValue)), contentDescription = goalRankName, modifier = Modifier.size(80.dp))
                }
            }
        }
    }
}

@Composable
internal fun StatsSection(currentSeasonEntries: List<RankEntry>, peakRank: Int?, peakRankName: String?, lowRank: Int?, lowRankName: String?, avgChange: Double?, avgGain: Double?, avgLoss: Double?, biggestGain: Int?, biggestDrop: Int?, bestWinStreak: Int, bestLoseStreak: Int, showStats: Boolean, compareSeasonsEnabled: Boolean, compareSeasonId: Int?, allSeasons: Map<Int, List<RankEntry>>, selectedSeason: Int, isEnglish: Boolean, isDarkMode: Boolean, palette: Palette, s: Strings, onToggleStats: () -> Unit, onToggleCompare: () -> Unit, onSelectCompareSeason: (Int?) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(s.best, formatNum(peakRank), palette.green, palette, Modifier.weight(1f), rankName = peakRankName, isDarkMode = isDarkMode)
            StatChip(s.worst, formatNum(lowRank), palette.red, palette, Modifier.weight(1f), rankName = lowRankName, isDarkMode = isDarkMode)
            StatChip(s.matches, currentSeasonEntries.size.toString(), palette.cyan, palette, Modifier.weight(1f), isDarkMode = isDarkMode)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (showStats) "${s.detailedStats} ▲" else "${s.detailedStats} ▼", color = palette.cyan, fontSize = 13.sp, modifier = Modifier.clickable { onToggleStats() })
            Text(text = if (compareSeasonsEnabled) "${s.compareSeasonsLabel} ▲" else "${s.compareSeasonsLabel} ▼", color = palette.cyan, fontSize = 13.sp, modifier = Modifier.clickable { onToggleCompare() })
        }
        if (compareSeasonsEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            val otherSeasonsWithData = allSeasons.filterKeys { it != selectedSeason }.filterValues { it.isNotEmpty() }.keys.sorted()
            Column(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 10.dp).padding(12.dp)) {
                if (otherSeasonsWithData.isEmpty()) {
                    Text(s.compareSeasonsNoOthers, color = palette.textMuted, fontSize = 12.sp)
                } else {
                    Text(s.compareSeasonsPrompt, color = palette.textMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        otherSeasonsWithData.forEach { season ->
                            val isSelected = compareSeasonId == season
                            Text(text = "${s.season} $season", color = if (isSelected) palette.accent else palette.textPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.background(if (isSelected) palette.accent.copy(alpha = 0.2f) else palette.surfaceAlt, RoundedCornerShape(16.dp)).border(1.dp, if (isSelected) palette.accent else palette.border, RoundedCornerShape(16.dp)).clickable { onSelectCompareSeason(if (isSelected) null else season) }.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                    val compareEntries = compareSeasonId?.let { allSeasons[it] }
                    if (compareEntries != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(palette.accent, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${s.season} $selectedSeason", color = palette.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.size(8.dp).background(palette.cyan, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${s.season} $compareSeasonId", color = palette.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SeasonComparisonChart(seriesA = currentSeasonEntries, seriesB = compareEntries, colorA = palette.accent, colorB = palette.cyan, palette = palette, isEnglish = isEnglish)
                    }
                }
            }
        }
        if (showStats) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 10.dp).padding(12.dp)) {
                DetailLine(s.avgProgress, avgChange?.roundToInt(), palette)
                Spacer(modifier = Modifier.height(6.dp))
                DetailLine(s.avgGain, avgGain?.roundToInt(), palette)
                Spacer(modifier = Modifier.height(6.dp))
                DetailLine(s.avgLoss, avgLoss?.roundToInt(), palette)
                Spacer(modifier = Modifier.height(6.dp))
                DetailLine(s.biggestGain, biggestGain, palette)
                Spacer(modifier = Modifier.height(6.dp))
                DetailLine(s.biggestLoss, biggestDrop, palette)
                Spacer(modifier = Modifier.height(6.dp))
                DetailLineText(s.bestWinStreakLabel, if (bestWinStreak > 0) "x$bestWinStreak" else "—", palette.green, palette)
                Spacer(modifier = Modifier.height(6.dp))
                DetailLineText(s.bestLoseStreakLabel, if (bestLoseStreak > 0) "x$bestLoseStreak" else "—", palette.red, palette)

                val tagStats = calculateStatsByTag(currentSeasonEntries, isEnglish)
                if (tagStats.isNotEmpty()) {
                    val tagMatchCounts = countMatchesByTag(currentSeasonEntries, isEnglish)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = s.avgStatsByTag, color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    tagStats.entries.sortedByDescending { (_, pair) -> (pair.first ?: 0.0) + (pair.second ?: 0.0) }.forEach { (tag, pair) ->
                        val (avgG, avgL) = pair; val count = tagMatchCounts[tag] ?: 0
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = " • $tag ($count)", color = palette.textMuted, fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (avgG != null) { Text(text = "+${avgG.roundToInt()}", color = palette.green, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                                if (avgL != null) { Text(text = "${avgL.roundToInt()}", color = palette.red, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                            }
                        }
                    }
                }

                val dayOfWeekLabels = if (isEnglish) DAY_LABELS_EN else DAY_LABELS_FR
                val hourBucketLabels = if (isEnglish) HOUR_BUCKET_LABELS_EN else HOUR_BUCKET_LABELS_FR
                val avgByDay = averageDeltaByDayOfWeek(currentSeasonEntries)
                val avgByHour = averageDeltaByHourBucket(currentSeasonEntries)
                if (avgByDay.any { it != null } || avgByHour.any { it != null }) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = s.performanceByTimeTitle, color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                    val todayStartMs = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
                    val weekStartMs = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0); val dayOfWeek = get(java.util.Calendar.DAY_OF_WEEK); if (dayOfWeek == java.util.Calendar.SUNDAY) { add(java.util.Calendar.DAY_OF_YEAR, -6) } else { add(java.util.Calendar.DAY_OF_YEAR, java.util.Calendar.MONDAY - dayOfWeek) } }.timeInMillis
                    val monthStartMs = java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis

                    val progressToday = getProgressForPeriod(currentSeasonEntries, todayStartMs)
                    val progressWeek = getProgressForPeriod(currentSeasonEntries, weekStartMs)
                    val progressMonth = getProgressForPeriod(currentSeasonEntries, monthStartMs)
                    val progressSeason = if (currentSeasonEntries.size >= 2) { currentSeasonEntries.last().rank - currentSeasonEntries.first().rank } else null

                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt).padding(10.dp)) {
                        DetailLine(s.progressTodayLabel, progressToday, palette)
                        Spacer(modifier = Modifier.height(6.dp))
                        DetailLine(s.progressWeekLabel, progressWeek, palette)
                        Spacer(modifier = Modifier.height(6.dp))
                        DetailLine(s.progressMonthLabel, progressMonth, palette)
                        Spacer(modifier = Modifier.height(6.dp))
                        DetailLine(s.progressSeasonLabel, progressSeason, palette)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(s.dayOfWeekLabel, color = palette.textMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TimePerformanceBarChart(labels = dayOfWeekLabels, values = avgByDay, palette = palette)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(s.timeOfDayLabel, color = palette.textMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TimePerformanceBarChart(labels = hourBucketLabels, values = avgByHour, palette = palette)
                }
            }
        }
    }
}

@Composable
internal fun CandlestickChartSection(chartPoints: List<ChartPoint>, chartPeriod: ChartPeriod, zoomScale: Float, selectedIndex: Int?, currentSeasonEntries: List<RankEntry>, lowRank: Int?, peakRank: Int?, isEnglish: Boolean, isDarkMode: Boolean, palette: Palette, s: Strings, onSelectPeriod: (ChartPeriod) -> Unit, onZoomIn: () -> Unit, onZoomOut: () -> Unit, onSelectPoint: (Int?) -> Unit, onChartEditClick: (Int) -> Unit) {
    if (currentSeasonEntries.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 12.dp).padding(24.dp)) {
            Text(text = s.emptyState, color = palette.textMuted, fontSize = 14.sp)
        }
    } else {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row {
                    PeriodTab(s.periodWeek, chartPeriod == ChartPeriod.WEEK, palette) { onSelectPeriod(ChartPeriod.WEEK) }
                    Spacer(modifier = Modifier.width(4.dp))
                    PeriodTab(s.periodMonth, chartPeriod == ChartPeriod.MONTH, palette) { onSelectPeriod(ChartPeriod.MONTH) }
                    Spacer(modifier = Modifier.width(4.dp))
                    PeriodTab(s.periodAll, chartPeriod == ChartPeriod.ALL, palette) { onSelectPeriod(ChartPeriod.ALL) }
                }
                Row {
                    Text(text = s.zoomOut, color = palette.cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.clickable { onZoomOut() }.padding(6.dp))
                    Text(text = s.zoomIn, color = palette.cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.clickable { onZoomIn() }.padding(6.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 8.dp).padding(8.dp)) {
                if (chartPoints.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(s.noDataPeriod, color = palette.textMuted, fontSize = 13.sp)
                    }
                } else {
                    CandlestickChart(points = chartPoints, palette = palette, selectedIndex = selectedIndex, onSelect = { idx -> onSelectPoint(idx) }, zoomScale = zoomScale, isEnglish = isEnglish, modifier = Modifier.fillMaxWidth())
                }
            }
            if (selectedIndex != null && chartPoints.isNotEmpty()) {
                val idx = selectedIndex.coerceIn(0, chartPoints.size - 1); val point = chartPoints[idx]
                val prevDelta = if (idx > 0) point.rank - chartPoints[idx - 1].rank else null
                val vsLow = if (lowRank != null) point.rank - lowRank else null
                val vsHigh = if (peakRank != null) point.rank - peakRank else null
                val correspondingEntry = currentSeasonEntries.getOrNull(point.absoluteIndex)

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 10.dp, baseColor = palette.surfaceAlt, accentColor = palette.cyan).padding(12.dp)) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "MATCH ${point.absoluteIndex + 1} · ${formatNum(point.rank)} RS", color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "✎", color = palette.cyan, fontSize = 16.sp, modifier = Modifier.clickable { onChartEditClick(point.absoluteIndex) })
                                Text(text = "✕", color = palette.textMuted, fontSize = 16.sp, modifier = Modifier.clickable { onSelectPoint(null) })
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${s.recordedAt} ${formatDateTime(point.timestamp, isEnglish)}", color = palette.textMuted, fontSize = 11.sp)

                        if (correspondingEntry != null && correspondingEntry.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                correspondingEntry.notes.forEach { rawTag ->
                                    val tag = translateTag(rawTag, isEnglish)
                                    Text(text = tag, color = palette.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.neumorphicCard(palette, isDarkMode, 6.dp, baseColor = palette.surface, accentColor = palette.accent.copy(alpha = 0.6f)).padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailLine(s.vsPrevious, prevDelta, palette)
                        Spacer(modifier = Modifier.height(6.dp))
                        DetailLine(s.vsLowest, vsLow, palette)
                        Spacer(modifier = Modifier.height(6.dp))
                        DetailLine(s.vsHighest, vsHigh, palette)
                    }
                }
            }
        }
    }
}

@Composable
internal fun InputSection(inputValue: String, currentSeasonEntries: List<RankEntry>, selectedNotes: Set<String>, customTags: List<String>, tagGroups: List<List<String>>, showInvalidScoreMsg: Boolean, showTypoErrorMsg: Boolean, showSavedMsg: Boolean, isEnglish: Boolean, isDarkMode: Boolean, palette: Palette, s: Strings, saveButtonScale: Float, onValueChange: (String) -> Unit, onSave: (Int) -> Unit, onToggleTag: (String) -> Unit, onAddCustomTag: (String) -> Unit, onLongClickCustomTag: (String) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
                placeholder = { Text(if (currentSeasonEntries.isEmpty()) s.startingRankPlaceholder else s.newRankPlaceholder) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { val parsed = inputValue.toIntOrNull(); if (parsed != null) onSave(parsed) },
                enabled = inputValue.toIntOrNull() != null,
                modifier = Modifier.scale(saveButtonScale)
            ) { Text(if (currentSeasonEntries.isEmpty()) s.startButton else s.saveButton, fontWeight = FontWeight.SemiBold) }
        }
        if (showInvalidScoreMsg) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = s.invalidScoreMsg, color = palette.red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        }
        if (showTypoErrorMsg) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = s.typoDetectedMsg, color = palette.red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        }
        if (showSavedMsg) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = s.rankSavedMsg, color = palette.green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        }
        if (inputValue.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = s.selectTags, color = palette.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            val defaultTags = if (isEnglish) DEFAULT_TAGS_EN else DEFAULT_TAGS_FR
            TagChipsSelector(
                tags = defaultTags, selected = selectedNotes, customTags = customTags, tagGroups = tagGroups, palette = palette,
                onToggle = { tag -> onToggleTag(tag) },
                onAddClick = { showAddDialog = true },
                onLongClickCustomTag = onLongClickCustomTag
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = palette.surface, titleContentColor = palette.textPrimary, textContentColor = palette.textMuted,
            title = { Text(s.addTagTitle) },
            text = { OutlinedTextField(value = newTagText, onValueChange = { newTagText = it }, placeholder = { Text(s.addTagPlaceholder) }, singleLine = true, textStyle = TextStyle(color = palette.textPrimary)) },
            confirmButton = { TextButton(onClick = { if (newTagText.isNotBlank()) onAddCustomTag(newTagText.trim()); newTagText = ""; showAddDialog = false }) { Text(s.addWord, color = palette.accent) } },
            dismissButton = { TextButton(onClick = { showAddDialog = false; newTagText = "" }) { Text(s.cancelWord, color = palette.textMuted) } }
        )
    }
}

@Composable
internal fun HistoryEntriesList(currentSeasonEntries: List<RankEntry>, showHistory: Boolean, showHistorySortMenu: Boolean, showHistoryFilterMenu: Boolean, historySortMode: HistorySortMode, historyNoteFilter: Set<String>, editingIndex: Int?, editValue: String, editNotes: Set<String>, customTags: List<String>, tagGroups: List<List<String>>, editInvalidScore: Boolean, deleteConfirmIndex: Int?, expandedHistoryIndex: Int?, lowRank: Int?, peakRank: Int?, isEnglish: Boolean, isDarkMode: Boolean, palette: Palette, s: Strings, onToggleSortMenu: () -> Unit, onToggleFilterMenu: () -> Unit, onSelectSortMode: (HistorySortMode) -> Unit, onToggleFilterNote: (String) -> Unit, onClearFilters: () -> Unit, onStartEdit: (Int, String, Set<String>) -> Unit, onEditValueChange: (String) -> Unit, onToggleEditTag: (String) -> Unit, onAddCustomTag: (String) -> Unit, onLongClickCustomTag: (String) -> Unit, onSaveEdit: (Int, Int, Set<String>) -> Unit, onCancelEdit: () -> Unit, onStartDelete: (Int) -> Unit, onConfirmDelete: (Int) -> Unit, onCancelDelete: () -> Unit, onToggleExpandHistory: (Int) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    if (showHistory && currentSeasonEntries.isNotEmpty()) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = s.sortLabel, color = if (showHistorySortMenu) palette.accent else palette.cyan,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.neumorphicCard(palette, isDarkMode, 8.dp).clickable { onToggleSortMenu() }.padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Text(
                    text = s.filterLabel, color = if (showHistoryFilterMenu || historyNoteFilter.isNotEmpty()) palette.accent else palette.cyan,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.neumorphicCard(palette, isDarkMode, 8.dp).clickable { onToggleFilterMenu() }.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            if (showHistorySortMenu) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt).padding(8.dp)) {
                    val sortOptions = listOf(HistorySortMode.OLDEST_FIRST to s.sortOldestFirst, HistorySortMode.NEWEST_FIRST to s.sortNewestFirst, HistorySortMode.GAIN_ASC to s.sortGainAsc, HistorySortMode.GAIN_DESC to s.sortGainDesc)
                    sortOptions.forEach { (mode, label) ->
                        Text(text = label, color = if (historySortMode == mode) palette.accent else palette.textPrimary, fontWeight = if (historySortMode == mode) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().clickable { onSelectSortMode(mode) }.padding(vertical = 6.dp))
                    }
                }
            }
            if (showHistoryFilterMenu) {
                Spacer(modifier = Modifier.height(6.dp))
                val availableFilterNotes = currentSeasonEntries.flatMap { it.notes }.map { translateTag(it, isEnglish) }.distinct()
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = s.filterAll, color = if (historyNoteFilter.isEmpty()) palette.accent else palette.textPrimary, fontWeight = if (historyNoteFilter.isEmpty()) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp, modifier = Modifier.background(if (historyNoteFilter.isEmpty()) palette.accent.copy(alpha = 0.2f) else palette.surface, RoundedCornerShape(16.dp)).border(1.dp, if (historyNoteFilter.isEmpty()) palette.accent else palette.border, RoundedCornerShape(16.dp)).clickable { onClearFilters() }.padding(horizontal = 12.dp, vertical = 6.dp))
                    availableFilterNotes.forEach { tag ->
                        val isSelected = historyNoteFilter.contains(tag)
                        Text(text = tag, color = if (isSelected) palette.accent else palette.textPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp, modifier = Modifier.background(if (isSelected) palette.accent.copy(alpha = 0.2f) else palette.surface, RoundedCornerShape(16.dp)).border(1.dp, if (isSelected) palette.accent else palette.border, RoundedCornerShape(16.dp)).clickable { onToggleFilterNote(tag) }.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val historyScrollState = rememberScrollState()
            LaunchedEffect(showHistory, currentSeasonEntries.size, historySortMode) {
                if (showHistory) {
                    if (historySortMode == HistorySortMode.NEWEST_FIRST) historyScrollState.scrollTo(0)
                    else historyScrollState.scrollTo(historyScrollState.maxValue)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(historyScrollState).imePadding().neumorphicCard(palette, isDarkMode, 8.dp).padding(8.dp)) {
                val indexedEntries = currentSeasonEntries.mapIndexed { i, e -> i to e }
                val filteredEntries = if (historyNoteFilter.isNotEmpty()) {
                    indexedEntries.filter { (_, e) -> e.notes.any { rawTag -> val translated = translateTag(rawTag, isEnglish); historyNoteFilter.contains(translated) } }
                } else indexedEntries
                val sortedEntries = when (historySortMode) {
                    HistorySortMode.OLDEST_FIRST -> filteredEntries
                    HistorySortMode.NEWEST_FIRST -> filteredEntries.reversed()
                    HistorySortMode.GAIN_ASC -> filteredEntries.sortedBy { (i, e) -> if (i > 0) e.rank - currentSeasonEntries[i - 1].rank else 0 }
                    HistorySortMode.GAIN_DESC -> filteredEntries.sortedByDescending { (i, e) -> if (i > 0) e.rank - currentSeasonEntries[i - 1].rank else 0 }
                }
                if (sortedEntries.isEmpty()) {
                    Text(text = s.noMatchForFilter, color = palette.textMuted, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                }
                sortedEntries.forEach { (idx, entry) ->
                    when {
                        editingIndex == idx -> {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "M${idx + 1}", color = palette.textMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = editValue, onValueChange = { onEditValueChange(it) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                                        modifier = Modifier.weight(1f).height(52.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "✓", color = palette.green, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { val parsed = editValue.toIntOrNull(); if (parsed != null && parsed <= 85000) onSaveEdit(idx, parsed, editNotes) }.padding(6.dp))
                                    Text(text = "✕", color = palette.textMuted, modifier = Modifier.clickable { onCancelEdit() }.padding(6.dp))
                                }
                                if (editInvalidScore) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = s.invalidScoreMsg, color = palette.red, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val defaultTags = if (isEnglish) DEFAULT_TAGS_EN else DEFAULT_TAGS_FR
                                TagChipsSelector(
                                    tags = defaultTags, selected = editNotes, customTags = customTags, tagGroups = tagGroups, palette = palette, chipFontSize = 10.sp,
                                    onToggle = { tag -> onToggleEditTag(tag) },
                                    onAddClick = { showAddDialog = true },
                                    onLongClickCustomTag = onLongClickCustomTag
                                )
                            }
                        }
                        deleteConfirmIndex == idx -> {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(s.deleteConfirm, color = palette.textPrimary, fontSize = 12.sp)
                                Row {
                                    Text(text = s.confirmWord, color = palette.red, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onConfirmDelete(idx) }.padding(6.dp))
                                    Text(text = s.cancelWord, color = palette.textMuted, fontSize = 12.sp, modifier = Modifier.clickable { onCancelDelete() }.padding(6.dp))
                                }
                            }
                        }
                        else -> {
                            val entryDelta = if (idx > 0) entry.rank - currentSeasonEntries[idx - 1].rank else null
                            val isExpanded = expandedHistoryIndex == idx
                            Column(modifier = Modifier.fillMaxWidth().clickable { onToggleExpandHistory(idx) }.padding(vertical = 6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Match ${idx + 1}", color = palette.textMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = formatNum(entry.rank) + " RS", color = palette.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                        if (entryDelta != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = (if (entryDelta >= 0) "▲+" else "▼-") + formatNum(abs(entryDelta)), color = if (entryDelta >= 0) palette.green else palette.red, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Row {
                                        Text(text = "✎", color = palette.cyan, fontSize = 13.sp, modifier = Modifier.clickable { onStartEdit(idx, entry.rank.toString(), entry.notes.toSet()) }.padding(horizontal = 6.dp))
                                        Text(text = "✕", fontSize = 14.sp, color = palette.red, modifier = Modifier.clickable { onStartDelete(idx) }.padding(horizontal = 6.dp))
                                    }
                                }
                                if (entry.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 8.dp).horizontalScroll(rememberScrollState())) {
                                        entry.notes.forEach { rawTag -> val tag = translateTag(rawTag, isEnglish); Text(text = tag, color = palette.textMuted, fontSize = 9.sp, modifier = Modifier.background(palette.surfaceAlt, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) }
                                    }
                                }
                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val vsLow = if (lowRank != null) entry.rank - lowRank else null
                                    val vsHigh = if (peakRank != null) entry.rank - peakRank else null
                                    Column(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt, accentColor = palette.cyan).padding(10.dp)) {
                                        Text(text = "${s.recordedAt} ${formatDateTime(entry.timestamp, isEnglish)}", color = palette.textMuted, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        DetailLine(s.vsPrevious, entryDelta, palette)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        DetailLine(s.vsLowest, vsLow, palette)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        DetailLine(s.vsHighest, vsHigh, palette)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = palette.surface, titleContentColor = palette.textPrimary, textContentColor = palette.textMuted,
            title = { Text(s.addTagTitle) },
            text = { OutlinedTextField(value = newTagText, onValueChange = { newTagText = it }, placeholder = { Text(s.addTagPlaceholder) }, singleLine = true, textStyle = TextStyle(color = palette.textPrimary)) },
            confirmButton = { TextButton(onClick = { if (newTagText.isNotBlank()) onAddCustomTag(newTagText.trim()); newTagText = ""; showAddDialog = false }) { Text(s.addWord, color = palette.accent) } },
            dismissButton = { TextButton(onClick = { showAddDialog = false; newTagText = "" }) { Text(s.cancelWord, color = palette.textMuted) } }
        )
    }
}

@Composable
internal fun BottomActionsSection(currentSeasonEntries: List<RankEntry>, showResetConfirm: Boolean, exportMenuExpanded: Boolean, importMenuExpanded: Boolean, showExportConfirm: Boolean, importError: Boolean, showImportConfirm: Boolean, pendingImport: Map<Int, List<RankEntry>>?, showImportSuccess: Boolean, palette: Palette, isDarkMode: Boolean, s: Strings, onToggleReset: () -> Unit, onConfirmReset: () -> Unit, onCancelReset: () -> Unit, onExportMenuClick: () -> Unit, onExportDismiss: () -> Unit, onExportClipboard: () -> Unit, onExportJson: () -> Unit, onImportMenuClick: () -> Unit, onImportDismiss: () -> Unit, onImportClipboard: () -> Unit, onImportJson: () -> Unit, onConfirmImport: () -> Unit, onCancelImport: () -> Unit, onFooterClick: () -> Unit) {
    Column {
        if (currentSeasonEntries.isNotEmpty()) {
            if (!showResetConfirm) {
                TextButton(onClick = { onToggleReset() }) { Text(s.resetAll, color = Color.Red, fontSize = 12.sp) }
            } else {
                Row(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt, accentColor = palette.red).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(s.confirmResetAll, color = palette.textPrimary, fontSize = 13.sp)
                    Row {
                        TextButton(onClick = { onConfirmReset() }) { Text(s.confirmWord, color = palette.red, fontWeight = FontWeight.SemiBold) }
                        TextButton(onClick = { onCancelReset() }) { Text(s.cancelWord, color = palette.textMuted) }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                Button(onClick = { onExportMenuClick() }, modifier = Modifier.fillMaxWidth()) { Text(s.exportButton, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                DropdownMenu(expanded = exportMenuExpanded, onDismissRequest = { onExportDismiss() }) {
                    DropdownMenuItem(text = { Text("Presse-papiers") }, onClick = { onExportClipboard() })
                    DropdownMenuItem(text = { Text("Fichier .json") }, onClick = { onExportJson() })
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                Button(onClick = { onImportMenuClick() }, modifier = Modifier.fillMaxWidth()) { Text(s.importButton, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                DropdownMenu(expanded = importMenuExpanded, onDismissRequest = { onImportDismiss() }) {
                    DropdownMenuItem(text = { Text("Presse-papiers") }, onClick = { onImportClipboard() })
                    DropdownMenuItem(text = { Text("Fichier .json") }, onClick = { onImportJson() })
                }
            }
        }
        if (showExportConfirm) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = s.exportedToClipboard, color = palette.green, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        }
        if (importError) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = s.importErrorMsg, color = palette.red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        }
        if (showImportConfirm && pendingImport != null) {
            val seasonCount = pendingImport.size
            val totalMatches = pendingImport.values.sumOf { it.size }
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().neumorphicCard(palette, isDarkMode, 10.dp, baseColor = palette.surfaceAlt, accentColor = palette.accent).padding(12.dp)) {
                Text(text = "${s.importFoundPrefix} $seasonCount ${s.importSeasonsWord}, $totalMatches ${s.importMatchesWord}", color = palette.textPrimary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(s.importConfirmQuestion, color = palette.textMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text(text = s.confirmWord, color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clickable { onConfirmImport() }.padding(6.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = s.cancelWord, color = palette.textMuted, fontSize = 13.sp, modifier = Modifier.clickable { onCancelImport() }.padding(6.dp))
                }
            }
        }
        if (showImportSuccess) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = s.importSuccessMsg, color = palette.green, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = s.footer, color = palette.textMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { onFooterClick() }.padding(8.dp))
    }
}

// ---------- COMPOSANTS GRAPHIQUES DE BASE ----------

@Composable
internal fun StatChip(label: String, value: String, valueColor: Color, palette: Palette, modifier: Modifier = Modifier, rankName: String? = null, isDarkMode: Boolean = true) {
    Column(modifier = modifier.neumorphicCard(palette, isDarkMode, 8.dp).padding(vertical = 8.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = palette.textMuted, fontSize = 13.sp, letterSpacing = 0.5.sp, fontFamily = FontFamily.Monospace, lineHeight = 13.sp)
        if (rankName != null) Text(rankName, color = palette.textMuted, fontSize = 13.sp, letterSpacing = 0.5.sp, fontFamily = FontFamily.Monospace, lineHeight = 13.sp)
        Spacer(modifier = Modifier.height(0.5.dp))
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
internal fun DetailLine(label: String, value: Int?, palette: Palette) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = palette.textMuted, fontSize = 12.sp)
        Text(text = formatSigned(value), color = if (value == null) palette.textMuted else if (value >= 0) palette.green else palette.red, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun DetailLineText(label: String, value: String, valueColor: Color, palette: Palette) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = palette.textMuted, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun PeriodTab(label: String, selected: Boolean, palette: Palette, onClick: () -> Unit) {
    Text(text = label, color = if (selected) palette.accentOn else palette.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.background(if (selected) palette.accent else Color.Transparent, RoundedCornerShape(6.dp)).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp))
}

@Composable
internal fun TagChipsSelector(tags: List<String>, selected: Set<String>, customTags: List<String>, tagGroups: List<List<String>>, palette: Palette, chipFontSize: TextUnit = 11.sp, onToggle: (String) -> Unit, onAddClick: (() -> Unit)? = null, onLongClickCustomTag: ((String) -> Unit)? = null) {
    val r1 = tags.take(6); val r2 = tags.drop(6)
    Column {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { r1.forEach { t -> TagChip(t, selected.contains(t), palette, chipFontSize) { onToggle(t) } } }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { r2.forEach { t -> TagChip(t, selected.contains(t), palette, chipFontSize) { onToggle(t) } } }

        if (customTags.isNotEmpty() || onAddClick != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Le bouton + est désormais toujours à gauche
                if (onAddClick != null) {
                    Box(modifier = Modifier.background(palette.surfaceAlt, RoundedCornerShape(16.dp)).border(1.dp, palette.border, RoundedCornerShape(16.dp)).clickable { onAddClick() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("+", color = palette.textPrimary, fontSize = chipFontSize, fontWeight = FontWeight.Bold)
                    }
                }
                customTags.forEach { t -> TagChip(t, selected.contains(t), palette, chipFontSize, onLongClick = { onLongClickCustomTag?.invoke(t) }) { onToggle(t) } }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun TagChip(tag: String, isSelected: Boolean, palette: Palette, fontSize: TextUnit, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color = if (isSelected) palette.accent.copy(alpha = 0.2f) else palette.surface, shape = RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = if (isSelected) palette.accent else palette.border, shape = RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = tag, color = if (isSelected) palette.accent else palette.textPrimary, fontSize = fontSize, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
internal fun CandlestickChart(points: List<ChartPoint>, palette: Palette, selectedIndex: Int?, onSelect: (Int) -> Unit, zoomScale: Float, isEnglish: Boolean, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val min = points.minOf { it.rank }.toFloat(); val max = points.maxOf { it.rank }.toFloat(); val sp = max - min
    val pad = if (sp == 0f) maxOf(min * 0.05f, 200f) else maxOf(sp * 0.25f, 100f)
    val dMin = (min - pad).coerceAtLeast(0f); val dMax = max + pad; val dSpan = (dMax - dMin).coerceAtLeast(1f)
    val density = LocalDensity.current
    val maxW = (RANK_TIERS.filter { it.first.toFloat() in dMin..dMax }.map { getLocalizedRankName(it.second, isEnglish) }.maxOfOrNull { n -> android.graphics.Paint().apply { textSize = with(density) { 12.sp.toPx() }; isAntiAlias = true }.measureText(n) } ?: 0f)
    val lMargin = with(density) { maxW.toDp() + 5.dp }.coerceIn(30.dp, 140.dp)

    BoxWithConstraints(modifier = modifier) {
        val slot = (this.maxWidth - lMargin) / 20f * zoomScale
        val contentW = maxOf(this.maxWidth, slot * maxOf(points.size, 20) + lMargin)
        val sState = rememberScrollState()
        LaunchedEffect(points.size) { sState.scrollTo(sState.maxValue) }
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lm = lMargin.toPx(); val ch = size.height - 16.dp.toPx()
                fun y(v: Float) = ch - ((v - dMin) / dSpan * ch)
                val visible = RANK_TIERS.filter { it.first.toFloat() in dMin..dMax }
                if (visible.isNotEmpty()) {
                    var lastY = Float.POSITIVE_INFINITY
                    visible.forEach { t ->
                        val gy = y(t.first.toFloat()); drawLine(color = palette.textMuted.copy(alpha = 0.5f), start = Offset(lm, gy), end = Offset(size.width, gy), strokeWidth = 2f)
                        if (lastY - gy >= 18.dp.toPx()) {
                            drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(getLocalizedRankName(t.second, isEnglish), lm - 6.dp.toPx(), (gy + 4f).coerceIn(10f, ch), android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 12.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.RIGHT }) }
                            lastY = gy
                        }
                    }
                } else {
                    for (i in 0..4) {
                        val gy = ch / 4 * i; drawLine(color = palette.textMuted.copy(alpha = 0.15f), start = Offset(lm, gy), end = Offset(size.width, gy), strokeWidth = 2f)
                        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(numberFormat.format(dMax - (dSpan / 4) * i), lm - 6.dp.toPx(), (gy + 4f).coerceIn(10f, ch), android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 12.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.RIGHT }) }
                    }
                }
                drawLine(color = palette.border, start = Offset(lm, 0f), end = Offset(lm, ch), strokeWidth = 2f)
            }
            Box(modifier = Modifier.fillMaxSize().padding(start = lMargin).horizontalScroll(sState)) {
                Canvas(modifier = Modifier.width(slot * points.size).fillMaxHeight().pointerInput(points.size, slot) { detectTapGestures { o -> onSelect((o.x / slot.toPx()).toInt().coerceIn(0, points.size - 1)) } }) {
                    val ch = size.height - 16.dp.toPx(); val n = points.size; val sl = size.width / n; val cW = (sl * 0.5f).coerceIn(4f, 40.dp.toPx())
                    fun y(v: Float) = ch - ((v - dMin) / dSpan * ch)
                    points.forEachIndexed { i, p ->
                        val cx = sl * i + sl / 2f; val isSel = selectedIndex == i; val r = p.rank
                        val yT: Float; val yB: Float; val col: Color
                        if (i == 0) { val mH = dSpan * 0.015f; yT = y(r + mH / 2f); yB = y(r - mH / 2f); col = palette.cyan }
                        else { val pr = points[i - 1].rank; yT = y(maxOf(pr, r).toFloat()); yB = y(minOf(pr, r).toFloat()); col = when { r > pr -> palette.green; r < pr -> palette.red; else -> palette.textMuted } }
                        drawRoundRect(color = col, topLeft = Offset(cx - cW / 2f, yT), size = Size(cW, (yB - yT).coerceAtLeast(3f)), cornerRadius = CornerRadius(3f, 3f))
                        if (isSel) drawRoundRect(color = palette.cyan, topLeft = Offset(cx - cW / 2f - 3f, yT - 3f), size = Size(cW + 6f, (yB - yT + 6f).coerceAtLeast(3f)), cornerRadius = CornerRadius(5f, 5f), style = Stroke(width = 3f))
                        if (i % maxOf(1, (26.dp.toPx() / sl).toInt()) == 0) {
                            drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText("${p.absoluteIndex + 1}", cx, size.height - 3f, android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 9.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TimePerformanceBarChart(labels: List<String>, values: List<Double?>, palette: Palette, modifier: Modifier = Modifier) {
    val maxAbs = (values.filterNotNull().maxOfOrNull { abs(it) } ?: 0.0).let { if (it <= 0.0) 1.0 else it }
    val barAreaHeight = 32.dp
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            val value = values.getOrNull(i)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth().height(barAreaHeight), contentAlignment = Alignment.BottomCenter) {
                    if (value != null && value > 0) {
                        val h = barAreaHeight * (value / maxAbs).toFloat().coerceIn(0f, 1f)
                        Box(modifier = Modifier.width(16.dp).height(h).background(palette.green, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.border))
                Box(modifier = Modifier.fillMaxWidth().height(barAreaHeight), contentAlignment = Alignment.TopCenter) {
                    if (value != null && value < 0) {
                        val h = barAreaHeight * (abs(value) / maxAbs).toFloat().coerceIn(0f, 1f)
                        Box(modifier = Modifier.width(16.dp).height(h).background(palette.red, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = label, color = palette.textMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(text = if (value != null) formatSigned(value.roundToInt()) else "—", color = when { value == null -> palette.textMuted; value >= 0 -> palette.green; else -> palette.red }, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
internal fun SeasonComparisonChart(seriesA: List<RankEntry>, seriesB: List<RankEntry>, colorA: Color, colorB: Color, palette: Palette, isEnglish: Boolean, modifier: Modifier = Modifier) {
    if (seriesA.isEmpty() && seriesB.isEmpty()) return
    val all = seriesA.map { it.rank } + seriesB.map { it.rank }; val min = (all.minOrNull() ?: 0).toFloat(); val max = (all.maxOrNull() ?: 1).toFloat(); val pad = ((max - min) * 0.12f).coerceAtLeast(50f)
    val dMin = (min - pad).coerceAtLeast(0f); val dMax = max + pad; val span = (dMax - dMin).coerceAtLeast(1f); val maxC = maxOf(seriesA.size, seriesB.size, 1)
    val density = LocalDensity.current
    val maxW = (RANK_TIERS.filter { it.first.toFloat() in dMin..dMax }.map { getLocalizedRankName(it.second, isEnglish) }.maxOfOrNull { n -> android.graphics.Paint().apply { textSize = with(density) { 10.sp.toPx() }; isAntiAlias = true }.measureText(n) } ?: 0f)
    val lMargin = with(density) { maxW.toDp() + 12.dp }.coerceIn(40.dp, 120.dp)
    Canvas(modifier = modifier.fillMaxWidth().height(190.dp)) {
        val lm = lMargin.toPx(); val bm = 18.dp.toPx(); val cW = size.width - lm; val ch = size.height - bm
        fun y(v: Float) = ch - ((v - dMin) / span * ch)
        fun x(i: Int) = if (maxC > 1) lm + cW * i / (maxC - 1).toFloat() else lm
        val visible = RANK_TIERS.filter { it.first.toFloat() in dMin..dMax }
        if (visible.isNotEmpty()) {
            var lastY = Float.POSITIVE_INFINITY
            visible.forEach { t ->
                val gy = y(t.first.toFloat()); drawLine(color = palette.textMuted.copy(alpha = 0.15f), start = Offset(lm, gy), end = Offset(size.width, gy), strokeWidth = 1.5f)
                if (lastY - gy >= 16.dp.toPx()) {
                    drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(getLocalizedRankName(t.second, isEnglish), lm - 6.dp.toPx(), (gy + 4f).coerceIn(10f, ch), android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 10.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.RIGHT }) }
                    lastY = gy
                }
            }
        } else {
            for (i in 0..4) {
                val gy = ch / 4 * i; drawLine(color = palette.textMuted.copy(alpha = 0.15f), start = Offset(lm, gy), end = Offset(size.width, gy), strokeWidth = 1.5f)
                drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(numberFormat.format(dMax - (span / 4) * i), lm - 6.dp.toPx(), (gy + 4f).coerceIn(10f, ch), android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 10.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.RIGHT }) }
            }
        }
        drawLine(color = palette.border, start = Offset(lm, 0f), end = Offset(lm, ch), strokeWidth = 2f)
        val xLabelC = minOf(maxC, 5)
        if (maxC > 1) { for (k in 0 until xLabelC) { val i = (maxC - 1) * k / (xLabelC - 1).coerceAtLeast(1); drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText("${i + 1}", x(i), size.height - 2f, android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 9.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER }) } } }
        else { drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText("1", x(0), size.height - 2f, android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 9.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER }) } }
        fun path(e: List<RankEntry>) = androidx.compose.ui.graphics.Path().apply { e.forEachIndexed { i, r -> val px = x(i); val py = y(r.rank.toFloat()); if (i == 0) moveTo(px, py) else lineTo(px, py) } }
        if (seriesA.isNotEmpty()) drawPath(path(seriesA), color = colorA, style = Stroke(width = 4f))
        if (seriesB.isNotEmpty()) drawPath(path(seriesB), color = colorB, style = Stroke(width = 4f))
    }
}

@Composable
internal fun ChartEditMatchDialog(
    idx: Int, initialRank: String, initialNotes: Set<String>,
    customTags: List<String>, tagGroups: List<List<String>>,
    palette: Palette, s: Strings, isEnglish: Boolean,
    onSave: (Int, Set<String>) -> Unit, onDismiss: () -> Unit,
    onAddCustomTag: (String) -> Unit, onLongClickCustomTag: (String) -> Unit
) {
    var editValue by remember { mutableStateOf(initialRank) }
    var editNotes by remember { mutableStateOf(initialNotes) }
    var invalidScore by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text("${s.editDesc} Match ${idx + 1}", color = palette.textPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(color = palette.textPrimary)
                )
                if (invalidScore) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(s.invalidScoreMsg, color = palette.red, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TagChipsSelector(
                    tags = if (isEnglish) DEFAULT_TAGS_EN else DEFAULT_TAGS_FR,
                    selected = editNotes,
                    customTags = customTags,
                    tagGroups = tagGroups,
                    palette = palette,
                    onToggle = { tag ->
                        val isSelected = editNotes.contains(tag)
                        editNotes = if (isSelected) {
                            editNotes - tag
                        } else {
                            val inGroup = tagGroups.find { it.contains(tag) }
                            if (inGroup != null) editNotes.filter { !inGroup.contains(it) }.toSet() + tag
                            else editNotes + tag
                        }
                    },
                    onAddClick = { showAddDialog = true },
                    onLongClickCustomTag = onLongClickCustomTag
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = editValue.toIntOrNull()
                if (parsed != null && parsed <= 85000) {
                    onSave(parsed, editNotes)
                } else {
                    invalidScore = true
                }
            }) { Text(s.saveButton, color = palette.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancelWord, color = palette.textMuted) }
        }
    )

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = palette.surface,
            title = { Text(s.addTagTitle, color = palette.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    placeholder = { Text(s.addTagPlaceholder) },
                    singleLine = true,
                    textStyle = TextStyle(color = palette.textPrimary)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTagText.isNotBlank()) onAddCustomTag(newTagText.trim())
                    newTagText = ""
                    showAddDialog = false
                }) { Text(s.addWord, color = palette.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newTagText = "" }) {
                    Text(s.cancelWord, color = palette.textMuted)
                }
            }
        )
    }
}