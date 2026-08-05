package com.example.finalsranktracker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun TabStats(
    currentSeasonEntries: List<RankEntry>, currentRank: Int?, animatedRankBrush: Brush, allSeasons: Map<Int, List<RankEntry>>, selectedSeason: Int,
    hiddenStatsTags: Set<String>, isEnglish: Boolean, isDarkMode: Boolean, palette: Palette, s: Strings, playerProfile: PlayerProfile?, chartType: ChartType,
    onToggleHideTag: (String) -> Unit, onSaveSeason: (List<RankEntry>) -> Unit, customTags: List<String>, tagGroups: List<List<String>>, onAddCustomTag: (String) -> Unit
) {
    var compareSeasonsEnabled by remember { mutableStateOf(false) }
    var compareSeasonIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isShowingHiddenStatsTags by remember { mutableStateOf(false) }
    var chartPeriod by remember { mutableStateOf(ChartPeriod.ALL) }
    var zoomScale by remember { mutableStateOf(1f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var chartEditingIndex by remember { mutableStateOf<Int?>(null) }

    val peakRank = remember(currentSeasonEntries) { currentSeasonEntries.maxOfOrNull { it.rank } }
    val lowRank = remember(currentSeasonEntries) { currentSeasonEntries.minOfOrNull { it.rank } }
    val peakRankName = remember(peakRank) { peakRank?.let { getLocalizedRankName(rankNameFor(it, playerProfile?.globalRank), isEnglish) } }
    val lowRankName = remember(lowRank) { lowRank?.let { getLocalizedRankName(rankNameFor(it, playerProfile?.globalRank), isEnglish) } }

    val deltas = remember(currentSeasonEntries) { currentSeasonEntries.map { it.rank }.zipWithNext { a, b -> b - a } }
    val gains = remember(deltas) { deltas.filter { it > 0 } }
    val losses = remember(deltas) { deltas.filter { it < 0 } }
    val avgChange = remember(deltas) { if (deltas.isNotEmpty()) deltas.average() else null }
    val avgGain = remember(gains) { if (gains.isNotEmpty()) gains.average() else null }
    val avgLoss = remember(losses) { if (losses.isNotEmpty()) losses.average() else null }
    val biggestGain = remember(gains) { gains.maxOrNull() }
    val biggestDrop = remember(losses) { losses.minOrNull() }
    val bestWinStreak = remember(deltas) { longestStreak(deltas) { it > 0 } }
    val bestLoseStreak = remember(deltas) { longestStreak(deltas) { it < 0 } }

    val allPoints = remember(currentSeasonEntries) { currentSeasonEntries.mapIndexed { i, e -> ChartPoint(i, e.rank, e.timestamp) } }
    val nowMillis = System.currentTimeMillis()
    val chartPoints = remember(chartPeriod, allPoints) {
        when (chartPeriod) {
            ChartPeriod.WEEK -> allPoints.filter { it.timestamp >= nowMillis - 7L * 24 * 3600 * 1000 }
            ChartPeriod.MONTH -> allPoints.filter { it.timestamp >= nowMillis - 30L * 24 * 3600 * 1000 }
            ChartPeriod.ALL -> allPoints
        }
    }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {

        if (currentSeasonEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(id = R.drawable.namatama_analytics), contentDescription = "Empty Analytics", modifier = Modifier.size(400.dp).padding(top = 40.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = s.emptyStats, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isPeak = currentRank != null && peakRank != null && currentRank == peakRank
                StatChip(s.best, formatNum(peakRank), palette.green, palette, Modifier.weight(1f).fillMaxHeight(), rankName = peakRankName, isDarkMode = isDarkMode, valueBrush = if (isPeak) animatedRankBrush else null, valueFontSize = 24.sp)
                StatChip(s.worst, formatNum(lowRank), palette.red, palette, Modifier.weight(1f).fillMaxHeight(), rankName = lowRankName, isDarkMode = isDarkMode, valueFontSize = 24.sp)
                StatChip(s.matches, currentSeasonEntries.size.toString(), palette.cyan, palette, Modifier.weight(1f).fillMaxHeight(), isDarkMode = isDarkMode, valueFontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            PerformanceChartSection(
                chartPoints = chartPoints, chartType = chartType, chartPeriod = chartPeriod, zoomScale = zoomScale, selectedIndex = selectedIndex,
                currentSeasonEntries = currentSeasonEntries, lowRank = lowRank, peakRank = peakRank, isEnglish = isEnglish, isDarkMode = isDarkMode,
                palette = palette, s = s, playerProfile = playerProfile,
                onSelectPeriod = { chartPeriod = it; selectedIndex = null },
                onZoomIn = { zoomScale = (zoomScale * 1.5f).coerceAtMost(100f) },
                onZoomOut = { zoomScale = (zoomScale / 1.5f).coerceAtLeast(0.1f) },
                onSelectPoint = { selectedIndex = it },
                onChartEditClick = { chartEditingIndex = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(s.detailedStats, palette, forceWhite = true, largeText = true)
                Text(text = if (compareSeasonsEnabled) "${s.compareSeasonsLabel} ▲" else "${s.compareSeasonsLabel} ▼", color = Color.White, fontFamily = BarlowCondensed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { compareSeasonsEnabled = !compareSeasonsEnabled; if (!compareSeasonsEnabled) compareSeasonIds = emptySet() })
            }
            if (compareSeasonsEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                val otherSeasonsWithData = allSeasons.filterKeys { it != selectedSeason }.filterValues { it.isNotEmpty() }.keys.sorted()

                Column(modifier = Modifier.fillMaxWidth().finalsCard(palette).padding(12.dp)) {
                    if (otherSeasonsWithData.isEmpty()) {
                        Text(s.compareSeasonsNoOthers, color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 12.sp)
                    } else {
                        Text(s.compareSeasonsPrompt, color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            otherSeasonsWithData.forEach { season ->
                                val isSelected = compareSeasonIds.contains(season)
                                Text(
                                    text = "${s.season} $season".uppercase(),
                                    color = if (isSelected) Color.Black else palette.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontFamily = BarlowCondensed,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier
                                        .background(if (isSelected) Color.White else palette.surfaceAlt, RoundedCornerShape(2.dp))
                                        .border(1.dp, if (isSelected) Color.White else palette.border, RoundedCornerShape(2.dp))
                                        .clickable { compareSeasonIds = if (isSelected) compareSeasonIds - season else compareSeasonIds + season }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                        if (compareSeasonIds.isNotEmpty()) {
                            val compareSeriesMap = compareSeasonIds.associateWith { allSeasons[it] ?: emptyList() }
                            Spacer(modifier = Modifier.height(10.dp))
                            SeasonComparisonChart(
                                baseSeries = currentSeasonEntries, baseSeasonNum = selectedSeason, compareSeriesMap = compareSeriesMap,
                                palette = palette, isEnglish = isEnglish, playerProfile = playerProfile
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SeasonComparisonTable(
                                baseSeries = currentSeasonEntries, baseSeason = selectedSeason, compareSeriesMap = compareSeriesMap,
                                palette = palette, s = s
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            var isEditingTags by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth().finalsCard(palette).padding(12.dp)) {
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

                val tagStats = remember(currentSeasonEntries, isEnglish) { calculateStatsByTag(currentSeasonEntries, isEnglish) }
                if (tagStats.isNotEmpty()) {
                    val tagMatchCounts = remember(currentSeasonEntries, isEnglish) { countMatchesByTag(currentSeasonEntries, isEnglish) }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = s.avgStatsByTag, color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "✎", color = palette.cyan, fontSize = 14.sp, modifier = Modifier.clickable { isEditingTags = !isEditingTags }.padding(4.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val sortedTagStats = remember(tagStats) { tagStats.entries.sortedByDescending { (_, pair) -> (pair.first ?: 0.0) + (pair.second ?: 0.0) } }
                    sortedTagStats.forEach { (tag, pair) ->
                        val isHidden = hiddenStatsTags.contains(tag)
                        if (isHidden && !isEditingTags && !isShowingHiddenStatsTags) return@forEach
                        val (avgG, avgL) = pair
                        val count = tagMatchCounts[tag] ?: 0
                        Row(modifier = Modifier.fillMaxWidth().alpha(if (isHidden) 0.5f else 1f).padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = " • $tag ($count)", color = palette.textMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (avgG != null) { Text(text = "+${avgG.roundToInt()}", color = palette.green, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                                if (avgL != null) { Text(text = "${avgL.roundToInt()}", color = palette.red, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                                if (isEditingTags) { Text(text = if (isHidden) s.showWord else s.hideWord, color = palette.textMuted.copy(alpha = 0.5f), fontSize = 9.sp, modifier = Modifier.clickable { onToggleHideTag(tag) }.padding(horizontal = 4.dp, vertical = 2.dp)) }
                            }
                        }
                    }
                    if (hiddenStatsTags.isNotEmpty() && !isEditingTags) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = if (isShowingHiddenStatsTags) s.hideHiddenTags else s.showHiddenTags, color = Color.White, fontSize = 11.sp, modifier = Modifier.clickable { isShowingHiddenStatsTags = !isShowingHiddenStatsTags })
                    }
                }

                val avgByDay = remember(currentSeasonEntries) { averageDeltaByDayOfWeek(currentSeasonEntries) }
                val avgByHour = remember(currentSeasonEntries) { averageDeltaByHourBucket(currentSeasonEntries) }
                if (avgByDay.any { it != null } || avgByHour.any { it != null }) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = s.performanceByTimeTitle, color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                    val todayStartMs = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
                    val weekStartMs = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0); val dayOfWeek = get(java.util.Calendar.DAY_OF_WEEK); if (dayOfWeek == java.util.Calendar.SUNDAY) { add(java.util.Calendar.DAY_OF_YEAR, -6) } else { add(java.util.Calendar.DAY_OF_YEAR, java.util.Calendar.MONDAY - dayOfWeek) } }.timeInMillis
                    val monthStartMs = java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis

                    val progressToday = remember(currentSeasonEntries) { getProgressForPeriod(currentSeasonEntries, todayStartMs) }
                    val progressWeek = remember(currentSeasonEntries) { getProgressForPeriod(currentSeasonEntries, weekStartMs) }
                    val progressMonth = remember(currentSeasonEntries) { getProgressForPeriod(currentSeasonEntries, monthStartMs) }
                    val progressSeason = remember(currentSeasonEntries) { if (currentSeasonEntries.size >= 2) { currentSeasonEntries.last().rank - currentSeasonEntries.first().rank } else null }

                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth().finalsCard(palette, baseColor = palette.surfaceAlt).padding(10.dp)) {
                        val lighterText = Color.White.copy(alpha = 0.9f)
                        DetailLine(s.progressTodayLabel, progressToday, palette, lighterText)
                        Spacer(modifier = Modifier.height(6.dp))
                        DetailLine(s.progressWeekLabel, progressWeek, palette, lighterText)
                        Spacer(modifier = Modifier.height(6.dp))
                        DetailLine(s.progressMonthLabel, progressMonth, palette, lighterText)
                        Spacer(modifier = Modifier.height(6.dp))
                        DetailLine(s.progressSeasonLabel, progressSeason, palette, lighterText)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(s.dayOfWeekLabel, color = palette.textMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TimePerformanceBarChart(labels = if (isEnglish) DAY_LABELS_EN else DAY_LABELS_FR, values = avgByDay, palette = palette)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(s.timeOfDayLabel, color = palette.textMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TimePerformanceBarChart(labels = if (isEnglish) HOUR_BUCKET_LABELS_EN else HOUR_BUCKET_LABELS_FR, values = avgByHour, palette = palette)

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = if (isEnglish) "Activity Heatmap" else "Activité (Heatmap)", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    CalendarHeatmap(entries = currentSeasonEntries, palette = palette, isEnglish = isEnglish, s = s)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (chartEditingIndex != null) {
                val idx = chartEditingIndex!!
                val entryToEdit = currentSeasonEntries.getOrNull(idx)
                if (entryToEdit != null) {
                    ChartEditMatchDialog(
                        idx = idx,
                        initialRank = entryToEdit.rank.toString(),
                        initialNotes = entryToEdit.notes.toSet(),
                        customTags = customTags,
                        tagGroups = tagGroups,
                        palette = palette,
                        s = s,
                        isEnglish = isEnglish,
                        onSave = { parsed, notes ->
                            val next = currentSeasonEntries.toMutableList()
                            next[idx] = next[idx].copy(rank = parsed, notes = notes.toList())
                            onSaveSeason(next)
                            chartEditingIndex = null
                        },
                        onDismiss = { chartEditingIndex = null },
                        onAddCustomTag = onAddCustomTag,
                        onLongClickCustomTag = { }
                    )
                } else {
                    chartEditingIndex = null
                }
            }
        }
    }
}

@Composable
internal fun CalendarHeatmap(entries: List<RankEntry>, palette: Palette, isEnglish: Boolean, s: Strings) {
    if (entries.size < 2) return
    val startMs = entries.first().timestamp
    
    val startCal = java.util.Calendar.getInstance().apply { timeInMillis = startMs; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
    val startTzOffset = startCal.timeZone.getOffset(startCal.timeInMillis)
    val startEpochDay = (startCal.timeInMillis + startTzOffset) / 86400000L

    val deltasByDay = mutableMapOf<Int, Int>()
    val matchesByDay = mutableMapOf<Int, Int>()
    for (i in 1 until entries.size) {
        val delta = entries[i].rank - entries[i - 1].rank
        val eTzOffset = java.util.TimeZone.getDefault().getOffset(entries[i].timestamp)
        val eEpochDay = (entries[i].timestamp + eTzOffset) / 86400000L
        val dayOffset = (eEpochDay - startEpochDay).toInt()
        if (dayOffset >= 0) {
            deltasByDay[dayOffset] = (deltasByDay[dayOffset] ?: 0) + delta
            matchesByDay[dayOffset] = (matchesByDay[dayOffset] ?: 0) + 1
        }
    }
    
    val endEpochDay = (entries.last().timestamp + java.util.TimeZone.getDefault().getOffset(entries.last().timestamp)) / 86400000L
    val endCal = java.util.Calendar.getInstance().apply { timeInMillis = entries.last().timestamp }
    
    val maxAbsDelta = deltasByDay.values.maxOfOrNull { abs(it) }?.let { if (it <= 0) 1 else it } ?: 1
    
    // Determine all months to display
    val months = mutableListOf<Pair<Int, Int>>()
    val curr = java.util.Calendar.getInstance().apply {
        timeInMillis = startCal.timeInMillis
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    while (curr.before(endCal) || (curr.get(java.util.Calendar.YEAR) == endCal.get(java.util.Calendar.YEAR) && curr.get(java.util.Calendar.MONTH) == endCal.get(java.util.Calendar.MONTH))) {
        months.add(curr.get(java.util.Calendar.YEAR) to curr.get(java.util.Calendar.MONTH))
        curr.add(java.util.Calendar.MONTH, 1)
    }
    
    var selectedDayOffset by remember { mutableStateOf<Int?>(null) }
    
    Column {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // Y-axis labels
            Column {
                Box(modifier = Modifier.height(20.dp)) // Matches month label height
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                        val daysEn = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val daysFr = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
                        val labels = if (isEnglish) daysEn else daysFr
                        for (d in 0 until 7) {
                            Box(modifier = Modifier.height(16.dp).padding(end = 4.dp), contentAlignment = Alignment.CenterEnd) {
                                Text(
                                    text = labels[d],
                                    color = palette.textMuted,
                                    fontSize = 10.sp,
                                    fontFamily = BarlowCondensed,
                                    fontWeight = FontWeight.Bold,
                                    style = androidx.compose.ui.text.TextStyle(
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            months.forEach { (year, month) ->
                Column {
                    val cal = java.util.Calendar.getInstance().apply { set(java.util.Calendar.YEAR, year); set(java.util.Calendar.MONTH, month); set(java.util.Calendar.DAY_OF_MONTH, 1) }
                    val monthName = java.text.SimpleDateFormat("MMMM yyyy", if (isEnglish) java.util.Locale.US else java.util.Locale.FRANCE).format(cal.time).uppercase()
                    
                    Box(modifier = Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.BottomCenter) {
                        Text(text = monthName, color = palette.textMuted, fontSize = 10.sp, fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Box(
                        modifier = Modifier
                            .background(palette.surfaceAlt.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, palette.border.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                        val firstDayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // 0 = Monday
                        val weeksInMonth = (daysInMonth + firstDayOfWeek + 6) / 7
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (w in 0 until weeksInMonth) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (d in 0 until 7) {
                                        val dayOfMonth = w * 7 + d - firstDayOfWeek + 1
                                        if (dayOfMonth in 1..daysInMonth) {
                                            val dayCal = java.util.Calendar.getInstance().apply { 
                                                set(java.util.Calendar.YEAR, year)
                                                set(java.util.Calendar.MONTH, month)
                                                set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                set(java.util.Calendar.MINUTE, 0)
                                                set(java.util.Calendar.SECOND, 0)
                                                set(java.util.Calendar.MILLISECOND, 0)
                                            }
                                            val tzOffset = dayCal.timeZone.getOffset(dayCal.timeInMillis)
                                            val epochDay = (dayCal.timeInMillis + tzOffset) / 86400000L
                                            val dayOffset = (epochDay - startEpochDay).toInt()
                                            
                                            val delta = deltasByDay[dayOffset] ?: 0
                                            val color = if (delta == 0) {
                                                palette.surfaceAlt
                                            } else if (delta > 0) {
                                                androidx.compose.ui.graphics.lerp(palette.green.copy(alpha = 0.2f), palette.green, (delta.toFloat() / maxAbsDelta).coerceIn(0f, 1f))
                                            } else {
                                                androidx.compose.ui.graphics.lerp(palette.red.copy(alpha = 0.2f), palette.red, (abs(delta).toFloat() / maxAbsDelta).coerceIn(0f, 1f))
                                            }
                                            Box(modifier = Modifier
                                                .size(16.dp)
                                                .background(color, RoundedCornerShape(2.dp))
                                                .border(1.dp, if (selectedDayOffset == dayOffset) Color.White else Color.Transparent, RoundedCornerShape(2.dp))
                                                .clickable { selectedDayOffset = if (selectedDayOffset == dayOffset) null else dayOffset })
                                        } else {
                                            // Draw a faint placeholder for days outside the month to keep the grid perfectly rectangular
                                            Box(modifier = Modifier
                                                .size(16.dp)
                                                .background(palette.surfaceAlt.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (selectedDayOffset != null) {
            val dOffset = selectedDayOffset!!
            val dateCal = java.util.Calendar.getInstance().apply { timeInMillis = startCal.timeInMillis + dOffset * 86400000L }
            val dateStr = java.text.SimpleDateFormat(if (isEnglish) "EEEE, MMMM dd, yyyy" else "EEEE dd MMMM yyyy", if (isEnglish) java.util.Locale.US else java.util.Locale.FRANCE).format(dateCal.time).replaceFirstChar { it.uppercase() }
            val dDelta = deltasByDay[dOffset] ?: 0
            val dMatches = matchesByDay[dOffset] ?: 0
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().background(palette.surfaceAlt, RoundedCornerShape(4.dp)).border(1.dp, palette.border, RoundedCornerShape(4.dp)).padding(12.dp)) {
                Column {
                    Text(text = dateStr, color = palette.textMuted, fontSize = 12.sp, fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "${if (isEnglish) "Matches:" else "Matchs:"} $dMatches", color = Color.White, fontSize = 14.sp)
                        Text(text = "${if (isEnglish) "Progress:" else "Progression:"} ${if (dDelta > 0) "+$dDelta" else "$dDelta"}", color = if (dDelta > 0) palette.green else if (dDelta < 0) palette.red else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}