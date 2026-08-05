package com.example.finalsranktracker

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class HistoryDateFilter {
    ALL, TODAY, THIS_WEEK, THIS_MONTH, CUSTOM_RANGE
}

internal class HistoryWebInterface(private val onDataReceived: (String) -> Unit) {
    @JavascriptInterface
    fun processJSON(json: String) {
        onDataReceived(json)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
internal fun TabHistory(
    currentSeasonEntries: List<RankEntry>,
    allSeasons: Map<Int, List<RankEntry>>,
    selectedSeason: Int,
    isEnglish: Boolean,
    isDarkMode: Boolean,
    palette: Palette,
    s: Strings,
    customTags: List<String>,
    tagGroups: List<List<String>>,
    playerProfile: PlayerProfile?,
    onSavePlayerProfile: (PlayerProfile?) -> Unit,
    onSaveSeason: (List<RankEntry>) -> Unit,
    onSaveAllSeasons: (Map<Int, List<RankEntry>>) -> Unit,
    onAddCustomTag: (String) -> Unit,
    onDeleteCustomTag: (String) -> Unit,
    onEditClick: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    var showHistorySortMenu by remember { mutableStateOf(false) }
    var showHistoryFilterMenu by remember { mutableStateOf(false) }
    var historySortMode by remember { mutableStateOf(HistorySortMode.NEWEST_FIRST) }
    var historyDateFilter by remember { mutableStateOf(HistoryDateFilter.ALL) }
    var historyNoteFilter by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedHistoryIndex by remember { mutableStateOf<Int?>(null) }

    var showCustomDateDialog by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var deleteConfirmIndex by remember { mutableStateOf<Int?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showAllMatches by remember { mutableStateOf(false) }

    var exportMenuExpanded by remember { mutableStateOf(false) }
    var importMenuExpanded by remember { mutableStateOf(false) }
    var showExportConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Map<Int, List<RankEntry>>?>(null) }
    var importError by remember { mutableStateOf(false) }
    var showImportSuccess by remember { mutableStateOf(false) }

    var showWebImportDialog by remember { mutableStateOf(false) }
    var webPlayerInput by remember { mutableStateOf("") }
    var webExtractedName by remember { mutableStateOf("") }
    var isWebImporting by remember { mutableStateOf(false) }
    var webFetchedData by remember { mutableStateOf<ParsedDavG25Data?>(null) }
    var webUrlToLoad by remember { mutableStateOf<String?>(null) }

    val lowRank = remember(currentSeasonEntries) { currentSeasonEntries.minOfOrNull { it.rank } }
    val peakRank = remember(currentSeasonEntries) { currentSeasonEntries.maxOfOrNull { it.rank } }

    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val jsonString = buildAllSeasonsJson(allSeasons)
            val os = context.contentResolver.openOutputStream(uri)
            if (os != null) {
                try { os.write(jsonString.toByteArray()) } catch (e: Exception) { importError = true } finally { os.close() }
                showImportSuccess = false
                importError = false
                showExportConfirm = true
                saveLastExportTimestamp(context, System.currentTimeMillis())
            } else importError = true
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                try {
                    val parsed = parseAllSeasonsJson(String(inputStream.readBytes()))
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

    LaunchedEffect(showExportConfirm) { if (showExportConfirm) { delay(3500); showExportConfirm = false } }
    LaunchedEffect(showImportSuccess) { if (showImportSuccess) { delay(3500); showImportSuccess = false } }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {

        if (currentSeasonEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(id = R.drawable.namatama_history), contentDescription = "Empty History", modifier = Modifier.size(250.dp).padding(top = 40.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = s.emptyHistory, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = s.sortLabel.uppercase(), color = if (showHistorySortMenu) palette.accent else palette.cyan, fontFamily = BarlowCondensed, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.finalsCard(palette).clickable { showHistorySortMenu = !showHistorySortMenu; showHistoryFilterMenu = false }.padding(horizontal = 10.dp, vertical = 6.dp))
                Text(text = s.filterLabel.uppercase(), color = if (showHistoryFilterMenu || historyNoteFilter.isNotEmpty()) palette.accent else palette.cyan, fontFamily = BarlowCondensed, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.finalsCard(palette).clickable { showHistoryFilterMenu = !showHistoryFilterMenu; showHistorySortMenu = false }.padding(horizontal = 10.dp, vertical = 6.dp))
            }

            if (showHistorySortMenu) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(modifier = Modifier.fillMaxWidth().finalsCard(palette, baseColor = palette.surfaceAlt).padding(8.dp)) {
                    listOf(HistorySortMode.OLDEST_FIRST to s.sortOldestFirst, HistorySortMode.NEWEST_FIRST to s.sortNewestFirst, HistorySortMode.GAIN_ASC to s.sortGainAsc, HistorySortMode.GAIN_DESC to s.sortGainDesc).forEach { (mode, label) ->
                        Text(text = label, color = if (historySortMode == mode) palette.accent else palette.textPrimary, fontWeight = if (historySortMode == mode) FontWeight.Bold else FontWeight.SemiBold, fontFamily = BarlowCondensed, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clickable { historySortMode = mode; showHistorySortMenu = false }.padding(vertical = 6.dp))
                    }
                }
            }

            if (showHistoryFilterMenu) {
                Spacer(modifier = Modifier.height(6.dp))
                val availableFilterNotes = remember(currentSeasonEntries) { currentSeasonEntries.flatMap { it.notes }.map { translateTag(it, isEnglish) }.distinct().sortedWith(TAG_COMPARATOR) }
                Column(modifier = Modifier.fillMaxWidth().finalsCard(palette, baseColor = palette.surfaceAlt).padding(8.dp)) {
                    Text(text = if (isEnglish) "BY DATE" else "PAR DATE", color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val dateOptions = listOf(
                            HistoryDateFilter.ALL to (if (isEnglish) "All Time" else "Tout"),
                            HistoryDateFilter.TODAY to (if (isEnglish) "Today" else "Aujourd'hui"),
                            HistoryDateFilter.THIS_WEEK to (if (isEnglish) "This Week" else "Cette Semaine"),
                            HistoryDateFilter.THIS_MONTH to (if (isEnglish) "This Month" else "Ce Mois-ci"),
                            HistoryDateFilter.CUSTOM_RANGE to (if (isEnglish) "Custom" else "Personnalisé")
                        )
                        dateOptions.forEach { (filter, label) ->
                            val isSelected = historyDateFilter == filter
                            Text(
                                text = label.uppercase(),
                                color = if (isSelected) Color.Black else palette.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                fontFamily = BarlowCondensed,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .background(if (isSelected) Color.White else Color(0xFF1A1F26), RoundedCornerShape(4.dp))
                                    .border(1.dp, if (isSelected) Color.White else palette.border, RoundedCornerShape(4.dp))
                                    .clickable {
                                        if (filter == HistoryDateFilter.CUSTOM_RANGE) {
                                            showCustomDateDialog = true
                                        } else {
                                            historyDateFilter = filter
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = if (isEnglish) "BY TAG" else "PAR TAG", color = palette.textMuted, fontFamily = BarlowCondensed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    TagChipsSelector(tags = if (isEnglish) DEFAULT_TAGS_EN else DEFAULT_TAGS_FR, selected = historyNoteFilter, customTags = availableFilterNotes.filter { !DEFAULT_TAGS_FR.contains(it) && !DEFAULT_TAGS_EN.contains(it) }, tagGroups = tagGroups, palette = palette, onToggle = { tag -> historyNoteFilter = if (historyNoteFilter.contains(tag)) historyNoteFilter - tag else historyNoteFilter + tag })
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = s.filterAll, color = palette.cyan, fontFamily = BarlowCondensed, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { historyNoteFilter = emptySet(); historyDateFilter = HistoryDateFilter.ALL })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val indexedEntries = remember(currentSeasonEntries) { currentSeasonEntries.mapIndexed { i, e -> i to e } }
                val filteredEntries = remember(indexedEntries, historyNoteFilter, historyDateFilter, isEnglish) {
                    var filtered = indexedEntries
                    if (historyNoteFilter.isNotEmpty()) {
                        filtered = filtered.filter { (_, e) -> e.notes.any { historyNoteFilter.contains(translateTag(it, isEnglish)) } }
                    }
                    if (historyDateFilter != HistoryDateFilter.ALL) {
                        val cal = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
                        val todayStartMs = cal.timeInMillis
                        val cutOffMs = when (historyDateFilter) {
                            HistoryDateFilter.TODAY -> todayStartMs
                            HistoryDateFilter.THIS_WEEK -> { cal.add(java.util.Calendar.DAY_OF_YEAR, -6); cal.timeInMillis }
                            HistoryDateFilter.THIS_MONTH -> { cal.add(java.util.Calendar.DAY_OF_YEAR, -24); cal.timeInMillis }
                            else -> 0L
                        }
                        filtered = filtered.filter { (_, e) ->
                            if (historyDateFilter == HistoryDateFilter.CUSTOM_RANGE) {
                                val s = customStartDate ?: 0L
                                val eMs = customEndDate ?: Long.MAX_VALUE
                                e.timestamp in s..(eMs + 86400000L)
                            } else {
                                e.timestamp >= cutOffMs
                            }
                        }
                    }
                    filtered
                }
                val sortedEntries = remember(filteredEntries, historySortMode) {
                    when (historySortMode) {
                        HistorySortMode.OLDEST_FIRST -> filteredEntries
                        HistorySortMode.NEWEST_FIRST -> filteredEntries.reversed()
                        HistorySortMode.GAIN_ASC -> filteredEntries.sortedBy { (i, e) -> if (i > 0) e.rank - currentSeasonEntries[i - 1].rank else 0 }
                        HistorySortMode.GAIN_DESC -> filteredEntries.sortedByDescending { (i, e) -> if (i > 0) e.rank - currentSeasonEntries[i - 1].rank else 0 }
                    }
                }

                if (sortedEntries.isEmpty()) {
                    Text(text = s.noMatchForFilter, color = palette.textMuted, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                } else {
                    val displayCount = if (showAllMatches) sortedEntries.size else minOf(10, sortedEntries.size)
                    sortedEntries.take(displayCount).forEach { (idx, entry) ->
                        val prevRank = if (idx > 0) currentSeasonEntries[idx - 1].rank else null
                        MatchCard(entry = entry, absoluteIndex = idx, previousRank = prevRank, lowRank = lowRank, peakRank = peakRank, isExpanded = expandedHistoryIndex == idx, isEnglish = isEnglish, isDarkMode = isDarkMode, palette = palette, s = s, onToggleExpand = { expandedHistoryIndex = if (expandedHistoryIndex == idx) null else idx }, onEditClick = { onEditClick(idx) }, onDeleteClick = { deleteConfirmIndex = idx })
                    }
                    if (sortedEntries.size > 10) {
                        Text(text = if (showAllMatches) s.showLessMatches else s.showAllMatches, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { showAllMatches = !showAllMatches }.padding(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!showResetConfirm) {
                TextButton(onClick = { showResetConfirm = true }) { Text(s.resetAll, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            } else {
                Row(modifier = Modifier.fillMaxWidth().finalsCard(palette, baseColor = palette.surfaceAlt, accentColor = palette.red).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(s.confirmResetAll, color = palette.textPrimary, fontSize = 13.sp)
                    Row {
                        TextButton(onClick = { onSaveSeason(emptyList()); showResetConfirm = false }) { Text(s.confirmWord, color = palette.red, fontWeight = FontWeight.SemiBold) }
                        TextButton(onClick = { showResetConfirm = false }) { Text(s.cancelWord, color = palette.textMuted) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1F26), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF3A4250), RoundedCornerShape(4.dp))
                    .clickable { uriHandler.openUri("https://www.davg25.com/app/the-finals-leaderboard-tracker/") }
                    .padding(14.dp)
            ) {
                Text(text = s.top10kLabel, color = palette.textPrimary, fontFamily = BarlowCondensed, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth())
            }
        }



        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Bouton EXPORT — CTA jaune style The Finals
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { exportJsonLauncher.launch("finals_rank_tracker_export.json") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.Black),
                    shape = RoundedCornerShape(4.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        text = s.exportButton.uppercase(),
                        fontFamily = BarlowCondensed,
                        fontWeight = FontWeight.Bold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.Black
                    )
                }
            }
            // Bouton IMPORT — secondaire sombre style The Finals
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { importMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1F26), contentColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A4250))
                ) {
                    Text(
                        text = s.importButton.uppercase(),
                        fontFamily = BarlowCondensed,
                        fontWeight = FontWeight.Bold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                DropdownMenu(expanded = importMenuExpanded, onDismissRequest = { importMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text(s.jsonFileMenu, fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold) }, onClick = { importMenuExpanded = false; importJsonLauncher.launch(arrayOf("*/*")) })
                    DropdownMenuItem(text = { Text(s.importWebButton, color = Color.White, fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold) }, onClick = { importMenuExpanded = false; showWebImportDialog = true })
                }
            }
        }

        if (showExportConfirm) { Spacer(modifier = Modifier.height(6.dp)); Text(text = s.exportedToClipboard, color = palette.green, fontSize = 12.sp, modifier = Modifier.fillMaxWidth()) }
        if (importError) { Spacer(modifier = Modifier.height(6.dp)); Text(text = s.importErrorMsg, color = palette.red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth()) }

        if (showImportConfirm && pendingImport != null) {
            val seasonCount = pendingImport!!.size; val totalMatches = pendingImport!!.values.sumOf { it.size }
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().finalsCard(palette, baseColor = palette.surfaceAlt, accentColor = palette.accent).padding(12.dp)) {
                Text(text = "${s.importFoundPrefix} $seasonCount ${s.importSeasonsWord}, $totalMatches ${s.importMatchesWord}", color = palette.textPrimary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp)); Text(s.importConfirmQuestion, color = palette.textMuted, fontSize = 12.sp); Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text(text = s.confirmWord, color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clickable { val importData = pendingImport; if (importData != null) { onSaveAllSeasons(importData); showImportConfirm = false; pendingImport = null; showImportSuccess = true } }.padding(6.dp)); Spacer(modifier = Modifier.width(12.dp))
                    Text(text = s.cancelWord, color = palette.textMuted, fontSize = 13.sp, modifier = Modifier.clickable { showImportConfirm = false; pendingImport = null }.padding(6.dp))
                }
            }
        }

        if (showImportSuccess) { Spacer(modifier = Modifier.height(6.dp)); Text(text = s.importSuccessMsg, color = palette.green, fontSize = 12.sp, modifier = Modifier.fillMaxWidth()) }
    }

    if (showWebImportDialog) {
        // Timeout de sécurité 15 secondes
        LaunchedEffect(webUrlToLoad) {
            if (webUrlToLoad != null) {
                delay(15000)
                if (isWebImporting && webFetchedData == null) {
                    webFetchedData = ParsedDavG25Data(false, s.importWebNotFound, null, null, null, emptyMap())
                    isWebImporting = false
                    webUrlToLoad = null
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                if (!isWebImporting) {
                    showWebImportDialog = false
                    webPlayerInput = ""
                    webFetchedData = null
                }
            },
            containerColor = palette.surface,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(s.importWebTitle1, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Text(s.importWebTitle2, color = palette.cyan, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    if (isWebImporting && webFetchedData == null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = palette.accent, modifier = Modifier.size(50.dp), strokeWidth = 4.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(s.importWebLoading, color = palette.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (webFetchedData != null && webFetchedData!!.success) {
                        val seasonCount = webFetchedData!!.seasons.size
                        val totalMatches = webFetchedData!!.seasons.values.sumOf { it.size }
                        Column(modifier = Modifier.fillMaxWidth().finalsCard(palette).padding(12.dp)) {
                            Text(text = (if (isEnglish) "Player: " else "Joueur : ") + "${webFetchedData!!.name ?: "Inconnu"}", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${s.importFoundPrefix} $seasonCount ${s.importSeasonsWord}, $totalMatches ${s.importMatchesWord}", color = palette.textPrimary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(s.importWebConfirm, color = palette.textMuted, fontSize = 12.sp)
                        }
                    } else {
                        Text(s.importWebDesc, color = palette.textMuted, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = webPlayerInput,
                            onValueChange = { webPlayerInput = it },
                            placeholder = { Text(s.importWebPlaceholder, fontSize = 12.sp) },
                            singleLine = true,
                            enabled = !isWebImporting,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrect = false),
                            textStyle = TextStyle(color = palette.textPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (webFetchedData != null && !webFetchedData!!.success) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(webFetchedData!!.error ?: "Erreur inconnue", color = palette.red, fontSize = 12.sp)
                        }
                    }

                    // La WebView invisible (sécurisée sans assertion !!)
                    if (webUrlToLoad != null && webFetchedData == null) {
                        val jsInterface = remember {
                            HistoryWebInterface { jsonStr ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    if (isWebImporting) {
                                        val parsed = parseDavG25JsonMultipleSeasons(jsonStr)
                                        if (parsed.success || parsed.error != null) {
                                            webFetchedData = parsed
                                            isWebImporting = false
                                            webUrlToLoad = null
                                        }
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.size(1.dp).alpha(0f)) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                                        addJavascriptInterface(jsInterface, "Android")

                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                super.onPageStarted(view, url, favicon)

                                                val safeName = android.net.Uri.encode(webExtractedName)
                                                val interceptScript = """
                                                    javascript:(function() {
                                                        if (window.__scrapingStarted) return;
                                                        window.__scrapingStarted = true;
                                                        
                                                        async function fetchSeason(seasonId) {
                                                            const body = JSON.stringify({
                                                                meta: {
                                                                    id: decodeURIComponent('$safeName'),
                                                                    range: 15552000,
                                                                    time: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Paris'
                                                                },
                                                                stats: { extended: true, rename: true, latest: false, ban: true },
                                                                history: { name: true },
                                                                leagues: { unranked: false, elite: false }
                                                            });
                                                            const headers = {
                                                                'Accept': 'application/json',
                                                                'Content-Type': 'application/json',
                                                                'X-SeasonId': seasonId,
                                                                'X-Version': '1402'
                                                            };
                                                            const url = '/app/the-finals-leaderboard-tracker/api/vaiiya/player-overview/?stats=true&history=true&timestamps=true&seasonal=true&leagues=true&season=' + seasonId;
                                                            try {
                                                                const res = await fetch(url, { method: 'POST', body: body, headers: headers });
                                                                if (!res.ok) return null;
                                                                return await res.json();
                                                            } catch(e) { return null; }
                                                        }

                                                        async function scrapeAllSeasons() {
                                                            const results = { seasons: {} };
                                                            let playerStats = null;
                                                            let foundAny = false;

                                                            try {
                                                                const s11Data = await fetchSeason('s11');
                                                                if (s11Data) {
                                                                    if (s11Data.history && s11Data.history.length > 0) {
                                                                        results.seasons[11] = s11Data.history;
                                                                        foundAny = true;
                                                                    }
                                                                    if (s11Data.stats) playerStats = s11Data.stats;
                                                                }
                                                            } catch(e) {}

                                                            try {
                                                                const safeName = encodeURIComponent(decodeURIComponent('$safeName'));
                                                                const histUrl = '/app/the-finals-leaderboard-tracker/api/vaiiya/historical-search?query=' + safeName + '&mode=0';
                                                                const histRes = await fetch(histUrl, { headers: { 'Accept': 'application/json' } });
                                                                if (histRes.ok) {
                                                                    const histData = await histRes.json();
                                                                    if (Array.isArray(histData) && histData.length > 0 && histData[0].entry) {
                                                                        const entries = histData[0].entry;
                                                                        const nowStr = new Date().toISOString();
                                                                        // No longer fetching old seasons points here
                                                                    }
                                                                }
                                                            } catch(e) {}

                                                            if (foundAny) {
                                                                results.player = playerStats;
                                                                results.success = true;
                                                                window.Android.processJSON(JSON.stringify(results));
                                                            } else {
                                                                window.Android.processJSON(JSON.stringify({error: "Joueur introuvable. Assurez-vous d'être dans le Top 10 000."}));
                                                            }
                                                        }
                                                        
                                                        setTimeout(scrapeAllSeasons, 1500);
                                                    })();
                                                """.trimIndent()
                                                view?.evaluateJavascript(interceptScript, null)
                                            }
                                        }
                                    }
                                },
                                update = { view ->
                                    // Utilisation de ?.let au lieu de !! pour bloquer la NullPointerException
                                    webUrlToLoad?.let { url ->
                                        view.loadUrl(url)
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (webFetchedData != null && webFetchedData!!.success) {
                    TextButton(onClick = {
                        val merged = allSeasons.toMutableMap()
                        webFetchedData!!.seasons.forEach { (sNum, newEntries) ->
                            val existing = merged[sNum] ?: emptyList()
                            merged[sNum] = (existing + newEntries).distinctBy { it.timestamp }.sortedBy { it.timestamp }
                        }
                        onSaveAllSeasons(merged)
                        onSavePlayerProfile(PlayerProfile(webFetchedData!!.name ?: "Inconnu", webFetchedData!!.globalRank, webFetchedData!!.rankChange))
                        showWebImportDialog = false
                        showImportSuccess = true
                    }) { Text(s.confirmWord, color = palette.accent, fontWeight = FontWeight.Bold) }
                } else {
                    TextButton(
                        enabled = webPlayerInput.isNotBlank() && !isWebImporting,
                        onClick = {
                            isWebImporting = true
                            webFetchedData = null
                            val input = webPlayerInput.trim()

                            webExtractedName = if (input.contains("davg25.com") || input.startsWith("http")) {
                                val idMatch = Regex("""[?&]id=([^&]+)""").find(input)
                                if (idMatch != null) android.net.Uri.decode(idMatch.groupValues[1])
                                else {
                                    val pathMatch = Regex(""".*/(?:api|player|player-stats)/([^/?]+)""").find(input)
                                    if (pathMatch != null) android.net.Uri.decode(pathMatch.groupValues[1]) else input
                                }
                            } else input

                            webUrlToLoad = "https://www.davg25.com/app/the-finals-leaderboard-tracker/"
                        }
                    ) { Text(s.searchWord, color = if (isWebImporting || webPlayerInput.isBlank()) palette.textMuted else palette.accent, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isWebImporting,
                    onClick = {
                        showWebImportDialog = false
                        webPlayerInput = ""
                        webFetchedData = null
                        isWebImporting = false
                        webUrlToLoad = null
                    }
                ) { Text(s.cancelWord, color = if (isWebImporting) palette.textMuted.copy(alpha=0.5f) else palette.textMuted) }
            }
        )
    }

    if (deleteConfirmIndex != null) {
        val idx = deleteConfirmIndex!!
        AlertDialog(
            onDismissRequest = { deleteConfirmIndex = null }, containerColor = palette.surface,
            title = { Text(s.deleteConfirm, color = palette.textPrimary) },
            text = { Text("Match ${idx + 1}", color = palette.textMuted) },
            confirmButton = { TextButton(onClick = { onDeleteClick(idx); deleteConfirmIndex = null }) { Text(s.confirmWord, color = palette.red) } },
            dismissButton = { TextButton(onClick = { deleteConfirmIndex = null }) { Text(s.cancelWord, color = palette.textMuted) } }
        )
    }

    if (showCustomDateDialog) {
        val dateRangePickerState = rememberDateRangePickerState(initialSelectedStartDateMillis = customStartDate, initialSelectedEndDateMillis = customEndDate)
        androidx.compose.ui.window.Dialog(onDismissRequest = { showCustomDateDialog = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize().padding(16.dp), shape = RoundedCornerShape(8.dp), color = palette.surface) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showCustomDateDialog = false }) { Text("✕", color = palette.textMuted, fontSize = 20.sp) }
                        Text(text = if (isEnglish) "Select Dates" else "Sélectionner les dates", color = palette.textPrimary, fontFamily = BarlowCondensed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            customStartDate = dateRangePickerState.selectedStartDateMillis
                            customEndDate = dateRangePickerState.selectedEndDateMillis
                            if (customStartDate != null || customEndDate != null) {
                                historyDateFilter = HistoryDateFilter.CUSTOM_RANGE
                            }
                            showCustomDateDialog = false
                        }) { Text("✓", color = palette.accent, fontSize = 20.sp) }
                    }
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.weight(1f).padding(8.dp),
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(
                            containerColor = palette.surface,
                            titleContentColor = palette.textPrimary,
                            headlineContentColor = palette.textPrimary,
                            weekdayContentColor = palette.textMuted,
                            subheadContentColor = palette.textPrimary,
                            yearContentColor = palette.textPrimary,
                            currentYearContentColor = palette.accent,
                            selectedYearContentColor = Color.Black,
                            selectedYearContainerColor = palette.accent,
                            dayContentColor = palette.textPrimary,
                            disabledDayContentColor = palette.textMuted.copy(alpha = 0.3f),
                            selectedDayContentColor = Color.Black,
                            disabledSelectedDayContentColor = Color.Black.copy(alpha = 0.3f),
                            selectedDayContainerColor = palette.accent,
                            disabledSelectedDayContainerColor = palette.accent.copy(alpha = 0.3f),
                            todayContentColor = palette.cyan,
                            todayDateBorderColor = palette.cyan,
                            dayInSelectionRangeContentColor = palette.textPrimary,
                            dayInSelectionRangeContainerColor = palette.accent.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}