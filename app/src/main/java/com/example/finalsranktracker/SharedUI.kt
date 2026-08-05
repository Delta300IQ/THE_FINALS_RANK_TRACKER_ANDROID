package com.example.finalsranktracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun StatChip(
    label: String, value: String, valueColor: Color, palette: Palette, modifier: Modifier = Modifier,
    rankName: String? = null, isDarkMode: Boolean = true, valueBrush: Brush? = null, valueFontSize: TextUnit = 24.sp
) {
    Column(
        modifier = modifier
            .finalsCard(palette)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ligne accent en haut du chip
        Box(modifier = Modifier.width(20.dp).height(2.dp).background(valueColor.copy(alpha = 0.8f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label.uppercase(),
            color = palette.textMuted,
            fontFamily = BarlowCondensed,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 11.sp
        )
        if (rankName != null) {
            Text(rankName, color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 11.sp, letterSpacing = 0.5.sp, lineHeight = 11.sp)
        } else {
            Text(" ", fontFamily = BarlowCondensed, fontSize = 11.sp, lineHeight = 11.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (valueBrush != null) {
            Text(value, style = TextStyle(brush = valueBrush, fontSize = valueFontSize, fontWeight = FontWeight.Bold, fontFamily = BarlowCondensed))
        } else {
            Text(value, color = valueColor, fontFamily = BarlowCondensed, fontSize = valueFontSize, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
internal fun DetailLine(label: String, value: Int?, palette: Palette, labelColor: Color = palette.textMuted) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = labelColor, fontFamily = BarlowCondensed, fontSize = 13.sp, letterSpacing = 0.5.sp)
        val color = if (value == null) palette.textMuted else if (value >= 0) palette.green else palette.red
        Text(text = formatSigned(value), color = color, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun DetailLineText(label: String, value: String, valueColor: Color, palette: Palette) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 13.sp, letterSpacing = 0.5.sp)
        Text(text = value, color = valueColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// Label de section style The Finals
@Composable
internal fun SectionLabel(text: String, palette: Palette, forceWhite: Boolean = false, largeText: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(if (largeText) 20.dp else 14.dp).background(palette.accent, RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text.uppercase(),
            color = if (forceWhite) Color.White else palette.textMuted,
            fontFamily = BarlowCondensed,
            fontSize = if (largeText) 16.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}


@Composable
internal fun PeriodTab(label: String, selected: Boolean, palette: Palette, onClick: () -> Unit) {
    // Style exact The Finals : actif = blanc/noir, inactif = #2A3038/gris
    Box(
        modifier = Modifier
            .background(
                color = if (selected) Color.White else Color(0xFF2A3038),
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = Color(0xFF3A4250),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            color = if (selected) Color.Black else Color(0xFF8C9DAA),
            fontFamily = BarlowCondensed,
            fontWeight = FontWeight.Bold,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
internal fun PerformanceChartSection(
    chartPoints: List<ChartPoint>, chartType: ChartType, chartPeriod: ChartPeriod, zoomScale: Float, selectedIndex: Int?,
    currentSeasonEntries: List<RankEntry>, lowRank: Int?, peakRank: Int?, isEnglish: Boolean, isDarkMode: Boolean,
    palette: Palette, s: Strings, playerProfile: PlayerProfile?, onSelectPeriod: (ChartPeriod) -> Unit, onZoomIn: () -> Unit, onZoomOut: () -> Unit,
    onSelectPoint: (Int?) -> Unit, onChartEditClick: (Int) -> Unit
) {
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
                Text(text = s.zoomOut, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onZoomOut() }.padding(6.dp))
                Text(text = s.zoomIn, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onZoomIn() }.padding(6.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().finalsCard(palette).padding(8.dp)) {
            if (chartPoints.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(s.noDataPeriod, color = palette.textMuted, fontSize = 13.sp)
                }
            } else {
                PerformanceChart(
                    points = chartPoints, type = chartType, palette = palette, selectedIndex = selectedIndex,
                    onSelect = { idx -> onSelectPoint(idx) }, zoomScale = zoomScale,
                    isEnglish = isEnglish, playerProfile = playerProfile, modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (selectedIndex != null && chartPoints.isNotEmpty()) {
            val idx = selectedIndex.coerceIn(0, chartPoints.size - 1)
            val point = chartPoints[idx]
            val prevDelta = if (idx > 0) point.rank - chartPoints[idx - 1].rank else null
            val vsLow = if (lowRank != null) point.rank - lowRank else null
            val vsHigh = if (peakRank != null) point.rank - peakRank else null
            val correspondingEntry = currentSeasonEntries.getOrNull(point.absoluteIndex)

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().finalsCard(palette, accentColor = palette.cyan).padding(12.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "MATCH ${point.absoluteIndex + 1} · ${formatNum(point.rank)} RS", color = Color.White, fontFamily = BarlowCondensed, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "✎", color = palette.cyan, fontSize = 16.sp, modifier = Modifier.clickable { onChartEditClick(point.absoluteIndex) })
                            Text(text = "✕", color = palette.textMuted, fontSize = 16.sp, modifier = Modifier.clickable { onSelectPoint(null) })
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${s.recordedAt} ${formatDateTime(point.timestamp, isEnglish)}", color = palette.textMuted, fontSize = 13.sp)

                    if (correspondingEntry != null && correspondingEntry.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            val sortedTags = correspondingEntry.notes.map { translateTag(it, isEnglish) }.sortedWith(TAG_COMPARATOR)
                            sortedTags.forEach { tag ->
                                Text(
                                    text = tag, color = palette.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(palette.accent.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                                        .border(1.dp, palette.accent.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
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

@Composable
internal fun PerformanceChart(
    points: List<ChartPoint>, type: ChartType, palette: Palette, selectedIndex: Int?,
    onSelect: (Int) -> Unit, zoomScale: Float, isEnglish: Boolean, playerProfile: PlayerProfile?, modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val min = points.minOf { it.rank }.toFloat()
    val max = points.maxOf { it.rank }.toFloat()
    val sp = max - min
    val pad = if (sp == 0f) maxOf(min * 0.05f, 200f) else maxOf(sp * 0.25f, 100f)
    val dMin = (min - pad).coerceAtLeast(0f)
    val dMax = max + pad
    val dSpan = (dMax - dMin).coerceAtLeast(1f)
    val density = LocalDensity.current

    val textMaxW = (RANK_TIERS.filter { it.first.toFloat() in dMin..dMax }
        .map { getLocalizedRankName(it.second, isEnglish) }
        .maxOfOrNull { n -> android.graphics.Paint().apply { textSize = with(density) { 12.sp.toPx() }; isAntiAlias = true }.measureText(n) } ?: 0f)

    // Aucun nombre à virgule en Y
    val numberMaxW = (0..4).map { numberFormat.format((dMax - (dSpan / 4) * it).roundToInt()) }
        .maxOfOrNull { n -> android.graphics.Paint().apply { textSize = with(density) { 12.sp.toPx() }; isAntiAlias = true }.measureText(n) } ?: 0f

    val maxW = maxOf(textMaxW, numberMaxW)
    val lMargin = with(density) { maxW.toDp() + 10.dp }.coerceIn(45.dp, 160.dp)

    BoxWithConstraints(modifier = modifier) {
        val slot = (this.maxWidth - lMargin) / 20f * zoomScale
        val sState = rememberScrollState()
        LaunchedEffect(points.size) { sState.scrollTo(sState.maxValue) }

        val pointColors = remember(points) {
            points.mapIndexed { i, p ->
                if (i == 0) palette.cyan
                else {
                    val prev = points[i - 1].rank
                    if (p.rank >= prev) palette.green else palette.red
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lm = lMargin.toPx()
                val ch = size.height - 16.dp.toPx()
                fun y(v: Float) = ch - ((v - dMin) / dSpan * ch)
                val visible = RANK_TIERS.filter { it.first.toFloat() in dMin..dMax }

                if (visible.isNotEmpty()) {
                    var lastY = Float.POSITIVE_INFINITY
                    visible.forEach { t ->
                        val gy = y(t.first.toFloat())
                        drawLine(color = palette.textMuted.copy(alpha = 0.5f), start = Offset(lm, gy), end = Offset(size.width, gy), strokeWidth = 2f)
                        if (lastY - gy >= 18.dp.toPx()) {
                            drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(getLocalizedRankName(t.second, isEnglish), lm - 6.dp.toPx(), (gy + 4f).coerceIn(10f, ch), android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 12.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.RIGHT }) }
                            lastY = gy
                        }
                    }
                } else {
                    for (i in 0..4) {
                        val gy = ch / 4 * i
                        drawLine(color = palette.textMuted.copy(alpha = 0.15f), start = Offset(lm, gy), end = Offset(size.width, gy), strokeWidth = 2f)
                        drawIntoCanvas { canvas ->
                            val v = (dMax - (dSpan / 4) * i).roundToInt()
                            canvas.nativeCanvas.drawText(numberFormat.format(v), lm - 6.dp.toPx(), (gy + 4f).coerceIn(10f, ch), android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 12.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.RIGHT })
                        }
                    }
                }
                drawLine(color = palette.border, start = Offset(lm, 0f), end = Offset(lm, ch), strokeWidth = 2f)
            }

            Box(modifier = Modifier.fillMaxSize().padding(start = lMargin).horizontalScroll(sState)) {
                Canvas(modifier = Modifier.width(slot * points.size).fillMaxHeight().pointerInput(points.size, slot) {
                    detectTapGestures { o -> onSelect((o.x / slot.toPx()).toInt().coerceIn(0, points.size - 1)) }
                }) {
                    val ch = size.height - 16.dp.toPx()
                    val sl = size.width / points.size
                    val cW = (sl * 0.5f).coerceIn(4f, 40.dp.toPx())
                    fun y(v: Float) = ch - ((v - dMin) / dSpan * ch)

                    when (type) {
                        ChartType.CANDLESTICK -> {
                            points.forEachIndexed { i, p ->
                                val cx = sl * i + sl / 2f
                                val r = p.rank.toFloat()
                                val yT: Float; val yB: Float
                                if (i == 0) { val mH = dSpan * 0.015f; yT = y(r + mH / 2f); yB = y(r - mH / 2f) }
                                else { val pr = points[i - 1].rank.toFloat(); yT = y(maxOf(pr, r)); yB = y(minOf(pr, r)) }
                                drawRoundRect(color = pointColors[i], topLeft = Offset(cx - cW / 2f, yT), size = Size(cW, (yB - yT).coerceAtLeast(3f)), cornerRadius = CornerRadius(3f, 3f))
                            }
                        }
                        ChartType.STRAIGHT_LINE -> {
                            for (i in 1 until points.size) {
                                val cx0 = sl * (i - 1) + sl / 2f
                                val cy0 = y(points[i - 1].rank.toFloat())
                                val cx1 = sl * i + sl / 2f
                                val cy1 = y(points[i].rank.toFloat())

                                val lineColor = if (points[i].rank >= points[i - 1].rank) palette.green else palette.red
                                drawLine(color = lineColor, start = Offset(cx0, cy0), end = Offset(cx1, cy1), strokeWidth = 3.5f)
                            }
                            val isInflection = { i: Int ->
                                if (i == 0 || i == points.size - 1) true
                                else {
                                    val prevDelta = points[i].rank - points[i - 1].rank
                                    val nextDelta = points[i + 1].rank - points[i].rank
                                    (prevDelta >= 0 && nextDelta < 0) || (prevDelta <= 0 && nextDelta > 0)
                                }
                            }
                            points.forEachIndexed { i, p ->
                                if (isInflection(i)) {
                                    val cx = sl * i + sl / 2f
                                    val cy = y(p.rank.toFloat())
                                    drawCircle(color = palette.surface, radius = 3.5f, center = Offset(cx, cy))
                                    drawCircle(color = pointColors[i], radius = 2.5f, center = Offset(cx, cy))
                                }
                            }
                        }
                        ChartType.CURVED_LINE -> {
                            for (i in 1 until points.size) {
                                val cx0 = sl * (i - 1) + sl / 2f
                                val cy0 = y(points[i - 1].rank.toFloat())
                                val cx1 = sl * i + sl / 2f
                                val cy1 = y(points[i].rank.toFloat())

                                val lineColor = if (points[i].rank >= points[i - 1].rank) palette.green else palette.red

                                val path = Path().apply {
                                    moveTo(cx0, cy0)
                                    val ctrlX = cx0 + (cx1 - cx0) / 2f
                                    cubicTo(ctrlX, cy0, ctrlX, cy1, cx1, cy1)
                                }
                                drawPath(path = path, color = lineColor, style = Stroke(width = 4f))
                            }
                            val isInflection = { i: Int ->
                                if (i == 0 || i == points.size - 1) true
                                else {
                                    val prevDelta = points[i].rank - points[i - 1].rank
                                    val nextDelta = points[i + 1].rank - points[i].rank
                                    (prevDelta >= 0 && nextDelta < 0) || (prevDelta <= 0 && nextDelta > 0)
                                }
                            }
                            points.forEachIndexed { i, p ->
                                if (isInflection(i)) {
                                    val cx = sl * i + sl / 2f
                                    val cy = y(p.rank.toFloat())
                                    drawCircle(color = palette.surface, radius = 2.5f, center = Offset(cx, cy))
                                    drawCircle(color = pointColors[i], radius = 1.5f, center = Offset(cx, cy))
                                }
                            }
                        }
                    }

                    // Sélection Visuelle
                    if (selectedIndex != null && selectedIndex in points.indices) {
                        val cx = sl * selectedIndex + sl / 2f
                        val cy = y(points[selectedIndex].rank.toFloat())
                        drawLine(color = palette.cyan.copy(alpha = 0.5f), start = Offset(cx, 0f), end = Offset(cx, ch), strokeWidth = 2f)
                        drawCircle(color = palette.cyan, radius = 8f, center = Offset(cx, cy))
                    }

                    // Calcul d'espacement pour ne jamais superposer les numéros de matchs (espace minimum 36dp)
                    val skipStep = maxOf(1, (36.dp.toPx() / sl).toInt())
                    points.forEachIndexed { i, p ->
                        if (i % skipStep == 0) {
                            val cx = sl * i + sl / 2f
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
                val formatText = if (value != null) formatSigned(value.roundToInt()) else "—"
                val formatColor = when { value == null -> palette.textMuted; value >= 0 -> palette.green; else -> palette.red }
                Text(text = formatText, color = formatColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

internal val compareGraphColors = listOf(Color(0xFF4FC8D6), Color(0xFF33D17A), Color(0xFFFF4D5E), Color(0xFFFFCC00), Color(0xFF9C27B0), Color(0xFF3F51B5))

@Composable
internal fun SeasonComparisonChart(
    baseSeries: List<RankEntry>, baseSeasonNum: Int, compareSeriesMap: Map<Int, List<RankEntry>>,
    palette: Palette, isEnglish: Boolean, playerProfile: PlayerProfile?, modifier: Modifier = Modifier
) {
    val allRanks = baseSeries.map { it.rank }.toMutableList()
    compareSeriesMap.values.forEach { series -> allRanks.addAll(series.map { it.rank }) }
    if (allRanks.isEmpty()) return

    val min = allRanks.minOrNull()!!.toFloat()
    val max = allRanks.maxOrNull()!!.toFloat()
    val pad = ((max - min) * 0.12f).coerceAtLeast(50f)
    val dMin = (min - pad).coerceAtLeast(0f)
    val dMax = max + pad
    val span = (dMax - dMin).coerceAtLeast(1f)

    val maxC = maxOf(baseSeries.size, compareSeriesMap.values.maxOfOrNull { it.size } ?: 1, 1)
    val density = LocalDensity.current
    val maxW = (RANK_TIERS.filter { it.first.toFloat() in dMin..dMax }.map { getLocalizedRankName(it.second, isEnglish) }.maxOfOrNull { n -> android.graphics.Paint().apply { textSize = with(density) { 10.sp.toPx() }; isAntiAlias = true }.measureText(n) } ?: 0f)
    val lMargin = with(density) { maxW.toDp() + 12.dp }.coerceIn(40.dp, 120.dp)

    Canvas(modifier = modifier.fillMaxWidth().height(190.dp)) {
        val lm = lMargin.toPx()
        val bm = 18.dp.toPx()
        val cW = size.width - lm
        val ch = size.height - bm
        fun y(v: Float) = ch - ((v - dMin) / span * ch)
        fun x(i: Int) = if (maxC > 1) lm + cW * i / (maxC - 1).toFloat() else lm
        val visible = RANK_TIERS.filter { it.first.toFloat() in dMin..dMax }

        if (visible.isNotEmpty()) {
            var lastY = Float.POSITIVE_INFINITY
            visible.forEach { t ->
                val gy = y(t.first.toFloat())
                drawLine(color = palette.textMuted.copy(alpha = 0.15f), start = Offset(lm, gy), end = Offset(size.width, gy), strokeWidth = 1.5f)
                if (lastY - gy >= 16.dp.toPx()) {
                    drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(getLocalizedRankName(t.second, isEnglish), lm - 6.dp.toPx(), (gy + 4f).coerceIn(10f, ch), android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 10.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.RIGHT }) }
                    lastY = gy
                }
            }
        } else {
            for (i in 0..4) {
                val gy = ch / 4 * i
                drawLine(color = palette.textMuted.copy(alpha = 0.15f), start = Offset(lm, gy), end = Offset(size.width, gy), strokeWidth = 1.5f)
                drawIntoCanvas { canvas ->
                    val v = (dMax - (span / 4) * i).roundToInt()
                    canvas.nativeCanvas.drawText(numberFormat.format(v), lm - 6.dp.toPx(), (gy + 4f).coerceIn(10f, ch), android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 10.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.RIGHT })
                }
            }
        }

        drawLine(color = palette.border, start = Offset(lm, 0f), end = Offset(lm, ch), strokeWidth = 2f)

        val xLabelC = minOf(maxC, 5)
        if (maxC > 1) {
            for (k in 0 until xLabelC) {
                val i = (maxC - 1) * k / (xLabelC - 1).coerceAtLeast(1)
                drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText("${i + 1}", x(i), size.height - 2f, android.graphics.Paint().apply { color = palette.textMuted.toArgb(); textSize = 9.sp.toPx(); isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER }) }
            }
        }

        fun path(e: List<RankEntry>) = Path().apply {
            e.forEachIndexed { i, r -> val px = x(i); val py = y(r.rank.toFloat()); if (i == 0) moveTo(px, py) else lineTo(px, py) }
        }

        var colorIndex = 0
        compareSeriesMap.forEach { (_, series) ->
            if (series.isNotEmpty()) {
                drawPath(path(series), color = compareGraphColors[colorIndex % compareGraphColors.size], style = Stroke(width = 3.5f))
                colorIndex++
            }
        }
        if (baseSeries.isNotEmpty()) {
            drawPath(path(baseSeries), color = palette.accent, style = Stroke(width = 5.5f))
        }
    }
}

@Composable
internal fun SeasonComparisonTable(
    baseSeries: List<RankEntry>, baseSeason: Int, compareSeriesMap: Map<Int, List<RankEntry>>, palette: Palette, s: Strings
) {
    val allKeys = listOf(baseSeason) + compareSeriesMap.keys.sorted()
    val allSeries = listOf(baseSeries) + allKeys.drop(1).map { compareSeriesMap[it]!! }
    val colors = listOf(palette.accent) + compareSeriesMap.keys.sorted().mapIndexed { i, _ -> compareGraphColors[i % compareGraphColors.size] }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val sState = rememberScrollState()

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(" ", fontSize = 12.sp, modifier = Modifier.width(130.dp))
            Row(modifier = Modifier.weight(1f).horizontalScroll(sState)) {
                allKeys.forEachIndexed { idx, key ->
                    Text("S$key", color = colors[idx], fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                }
            }
        }

        class RawStat(val peak: Int, val low: Int, val winRate: Int?, val avgProg: Int?, val avgGain: Int?, val avgLoss: Int?, val bestStreak: Int, val worstStreak: Int, val count: Int)
        val rawStats = allSeries.map { entries ->
            val deltas = entries.map { it.rank }.zipWithNext { a, b -> b - a }
            val gains = deltas.filter { it > 0 }
            val losses = deltas.filter { it < 0 }

            RawStat(
                peak = entries.maxOfOrNull { it.rank } ?: 0,
                low = entries.minOfOrNull { it.rank } ?: 0,
                winRate = if (gains.size + losses.size > 0) (gains.size * 100f / (gains.size + losses.size)).roundToInt() else null,
                avgProg = if (deltas.isNotEmpty()) deltas.average().roundToInt() else null,
                avgGain = if (gains.isNotEmpty()) gains.average().roundToInt() else null,
                avgLoss = if (losses.isNotEmpty()) losses.average().roundToInt() else null,
                bestStreak = longestStreak(deltas) { it > 0 },
                worstStreak = longestStreak(deltas) { it < 0 },
                count = entries.size
            )
        }

        val getColor = { value: Int?, maxVal: Int?, minVal: Int?, reverse: Boolean ->
            if (value == null || maxVal == minVal) palette.textPrimary
            else if (value == (if (reverse) minVal else maxVal)) palette.green
            else if (value == (if (reverse) maxVal else minVal)) palette.red
            else palette.textPrimary
        }

        val stats = rawStats.map { r ->
            mapOf(
                "rank" to androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = getColor(r.peak, rawStats.maxOfOrNull { it.peak }, rawStats.minOfOrNull { it.peak }, false))) { append(formatNum(r.peak)) }
                    append("\n")
                    withStyle(androidx.compose.ui.text.SpanStyle(color = getColor(r.low, rawStats.maxOfOrNull { it.low }, rawStats.minOfOrNull { it.low }, false))) { append(formatNum(r.low)) }
                },
                "winrate" to androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = getColor(r.winRate, rawStats.mapNotNull { it.winRate }.maxOrNull(), rawStats.mapNotNull { it.winRate }.minOrNull(), false))) { append(if (r.winRate != null) "${r.winRate}%" else "—") }
                },
                "avgMatch" to androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = getColor(r.avgProg, rawStats.mapNotNull { it.avgProg }.maxOrNull(), rawStats.mapNotNull { it.avgProg }.minOrNull(), false))) { append(formatSigned(r.avgProg)) }
                },
                "gainLoss" to androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = getColor(r.avgGain, rawStats.mapNotNull { it.avgGain }.maxOrNull(), rawStats.mapNotNull { it.avgGain }.minOrNull(), false))) { append("+${r.avgGain ?: 0}") }
                    append("\n")
                    withStyle(androidx.compose.ui.text.SpanStyle(color = getColor(r.avgLoss, rawStats.mapNotNull { it.avgLoss }.maxOrNull(), rawStats.mapNotNull { it.avgLoss }.minOrNull(), false))) { append("${r.avgLoss ?: 0}") }
                },
                "streaks" to androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = getColor(r.bestStreak, rawStats.maxOfOrNull { it.bestStreak }, rawStats.minOfOrNull { it.bestStreak }, false))) { append("${r.bestStreak}W") }
                    withStyle(androidx.compose.ui.text.SpanStyle(color = Color.White)) { append(" / ") }
                    withStyle(androidx.compose.ui.text.SpanStyle(color = getColor(r.worstStreak, rawStats.maxOfOrNull { it.worstStreak }, rawStats.minOfOrNull { it.worstStreak }, true))) { append("${r.worstStreak}L") }
                },
                "count" to androidx.compose.ui.text.buildAnnotatedString { withStyle(androidx.compose.ui.text.SpanStyle(color = Color.White)) { append("${r.count}") } }
            )
        }

        val drawRow = @Composable { label: String, key: String ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(label, color = palette.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
                Row(modifier = Modifier.weight(1f).horizontalScroll(sState)) {
                    stats.forEachIndexed { idx, sMap ->
                        Text(sMap[key] as androidx.compose.ui.text.AnnotatedString, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        drawRow(s.compTableRank, "rank")
        drawRow(s.compTableWinrate, "winrate")
        drawRow(s.compTableAvgMatch, "avgMatch")
        drawRow(s.compTableGainLoss, "gainLoss")
        drawRow(s.compTableStreaks, "streaks")
        drawRow(s.compTableCount, "count")
    }
}

@Composable
internal fun ChartEditMatchDialog(
    idx: Int, initialRank: String, initialNotes: Set<String>, customTags: List<String>, tagGroups: List<List<String>>,
    palette: Palette, s: Strings, isEnglish: Boolean, onSave: (Int, Set<String>) -> Unit, onDismiss: () -> Unit,
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
                            if (inGroup != null) editNotes.filter { !inGroup.contains(it) }.toSet() + tag else editNotes + tag
                        }
                    },
                    onAddClick = { showAddDialog = true },
                    onLongClickCustomTag = onLongClickCustomTag
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = editValue.toIntOrNull()
                    if (parsed != null && parsed <= 85000) {
                        onSave(parsed, editNotes)
                    } else {
                        invalidScore = true
                    }
                }
            ) {
                Text(s.saveButton, color = palette.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancelWord, color = palette.textMuted)
            }
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
                }) {
                    Text(s.addWord, color = palette.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newTagText = ""
                }) {
                    Text(s.cancelWord, color = palette.textMuted)
                }
            }
        )
    }
}

@Composable
internal fun TagChipsSelector(
    tags: List<String>, selected: Set<String>, customTags: List<String>, tagGroups: List<List<String>>,
    palette: Palette, chipFontSize: TextUnit = 11.sp, onToggle: (String) -> Unit,
    onAddClick: (() -> Unit)? = null, onLongClickCustomTag: ((String) -> Unit)? = null
) {
    val r1 = tags.take(6)
    val r2 = tags.drop(6)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            r1.forEach { t ->
                TagChip(
                    tag = t, isSelected = selected.contains(t), palette = palette,
                    fontSize = chipFontSize, onClick = { onToggle(t) }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            r2.forEach { t ->
                TagChip(
                    tag = t, isSelected = selected.contains(t), palette = palette,
                    fontSize = chipFontSize, onClick = { onToggle(t) }
                )
            }
        }

        if (customTags.isNotEmpty() || onAddClick != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onAddClick != null) {
                    Box(
                        modifier = Modifier
                            .background(palette.surfaceAlt, RoundedCornerShape(16.dp))
                            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                            .clickable { onAddClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+", color = palette.textPrimary, fontSize = chipFontSize, fontWeight = FontWeight.Bold)
                    }
                }
                customTags.forEach { t ->
                    TagChip(
                        tag = t, isSelected = selected.contains(t), palette = palette,
                        fontSize = chipFontSize, onLongClick = { onLongClickCustomTag?.invoke(t) },
                        onClick = { onToggle(t) }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun TagChip(
    tag: String, isSelected: Boolean, palette: Palette, fontSize: TextUnit,
    onLongClick: (() -> Unit)? = null, onClick: () -> Unit
) {
    // Style identique à la carte export data : fond sombre, bordure
    val bgColor = if (isSelected) Color.White else Color(0xFF1A1F26)
    val borderColor = if (isSelected) Color.White else palette.border
    val textColor = if (isSelected) Color.Black else palette.textPrimary
    val textWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold

    Box(
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(4.dp))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 10.dp) // padding plus large comme la carte
    ) {
        Text(text = tag, color = textColor, fontFamily = BarlowCondensed, fontSize = fontSize, fontWeight = textWeight, letterSpacing = 0.5.sp)
    }
}

@Composable
internal fun MatchCard(
    entry: RankEntry, absoluteIndex: Int, previousRank: Int?, lowRank: Int?, peakRank: Int?, isExpanded: Boolean,
    isEnglish: Boolean, isDarkMode: Boolean, palette: Palette, s: Strings,
    onToggleExpand: () -> Unit, onEditClick: () -> Unit, onDeleteClick: () -> Unit
) {
    val delta = if (previousRank != null) entry.rank - previousRank else null
    val stripColor = when {
        delta == null -> palette.border
        delta > 0 -> palette.green
        else -> palette.red
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .finalsCard(palette)
            .clickable { onToggleExpand() }
    ) {
        // Barre colorée gauche style The Finals
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(stripColor)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MATCH ${absoluteIndex + 1}",
                        color = palette.textPrimary,
                        fontFamily = BarlowCondensed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    if (!isExpanded) {
                        Text(text = formatDateTime(entry.timestamp, isEnglish), color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 12.sp)
                    }
                }
                Text(
                    text = "${formatNum(entry.rank)} RS",
                    color = palette.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(82.dp),
                    textAlign = TextAlign.End
                )
                if (delta != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val color = if (delta >= 0) palette.green else palette.red
                    val symbol = if (delta >= 0) "▲+" else "▼-"
                    Text(
                        text = symbol + formatNum(abs(delta)),
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(66.dp),
                        textAlign = TextAlign.End
                    )
                } else {
                    Spacer(modifier = Modifier.width(74.dp))
                }
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.border))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${s.recordedAt} ${formatDateTime(entry.timestamp, isEnglish)}", color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "✎", color = palette.cyan, fontSize = 15.sp, modifier = Modifier.clickable { onEditClick() })
                        Text(text = "✕", color = palette.red, fontSize = 14.sp, modifier = Modifier.clickable { onDeleteClick() })
                    }
                }
                if (entry.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val sortedTags = entry.notes.map { translateTag(it, isEnglish) }.sortedWith(TAG_COMPARATOR)
                        sortedTags.forEach { tag ->
                            Text(
                                text = tag,
                                color = palette.textPrimary,
                                fontFamily = BarlowCondensed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .background(Color(0xFF1A1F26), RoundedCornerShape(4.dp))
                                    .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                val vsLow = if (lowRank != null) entry.rank - lowRank else null
                val vsHigh = if (peakRank != null) entry.rank - peakRank else null
                Column(modifier = Modifier.fillMaxWidth().finalsCard(palette, baseColor = palette.bg).padding(10.dp)) {
                    val lighterText = Color.White.copy(alpha = 0.9f)
                    DetailLine(s.vsPrevious, delta, palette, lighterText)
                    Spacer(modifier = Modifier.height(6.dp))
                    DetailLine(s.vsLowest, vsLow, palette, lighterText)
                    Spacer(modifier = Modifier.height(6.dp))
                    DetailLine(s.vsHighest, vsHigh, palette, lighterText)
                }
            }
        }
    }
}
