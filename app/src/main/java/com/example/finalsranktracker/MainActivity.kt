package com.example.finalsranktracker

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// Barlow Condensed — ajouter dans app/src/main/res/font/ :
// barlow_condensed_bold.ttf, barlow_condensed_semibold.ttf, barlow_condensed_regular.ttf
// puis dans themes.xml ou directement via FontFamily ci-dessous.
// Si les fichiers ne sont pas encore ajoutés, remplacer BarlowCondensed par FontFamily.Default.
internal val BarlowCondensed = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    androidx.compose.ui.text.font.Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(R.font.barlow_condensed_regular, FontWeight.Normal)
)

// Design plat The Finals — surface sombre
internal fun Modifier.finalsCard(
    palette: Palette,
    cornerRadius: androidx.compose.ui.unit.Dp = 4.dp,
    baseColor: Color? = null,
    accentColor: Color? = null
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val bg = baseColor ?: palette.surface
    val borderColor = accentColor ?: palette.border
    return this
        .background(bg, shape)
        .border(1.dp, borderColor, shape)
}

internal class GlobalWebInterface(private val onDataReceived: (String) -> Unit) {
    @JavascriptInterface
    fun processJSON(json: String) {
        onDataReceived(json)
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
internal fun DataFetcherWebView(playerName: String, onResult: (ParsedDavG25Data) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val urlToLoad = "https://www.davg25.com/app/the-finals-leaderboard-tracker/"

    val jsInterface = remember {
        GlobalWebInterface { jsonStr ->
            coroutineScope.launch(Dispatchers.Main) {
                val parsed = parseDavG25JsonMultipleSeasons(jsonStr)
                if (parsed.success || parsed.error != null) {
                    onResult(parsed)
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
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            val safeName = android.net.Uri.encode(playerName)

                            // Script Séquentiel (Saisons 1 à 11)
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
                                        const seasons = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
                                        const results = { seasons: {} };
                                        let playerStats = null;
                                        let foundAny = false;

                                        let s11History = null;
                                        let seasonalData = null;
                                        try {
                                            const s11Data = await fetchSeason('s11');
                                            if (s11Data) {
                                                if (s11Data.history) s11History = s11Data.history;
                                                if (s11Data.seasonal) seasonalData = s11Data.seasonal;
                                                if (s11Data.stats) playerStats = s11Data.stats;
                                            }
                                        } catch(e) {}

                                        for (let s of seasons) {
                                            const sid = 's' + s;
                                            let data = null;
                                            try {
                                                data = await fetchSeason(sid);
                                            } catch(e) {}
                                            
                                            let historyArray = data ? (data.history || []) : [];
                                            let isCopy = false;
                                            if (data && s !== 11 && s11History && historyArray.length === s11History.length) {
                                                if (historyArray.length === 0) {
                                                    isCopy = true;
                                                } else {
                                                    let firstSame = historyArray[0].date === s11History[0].date && historyArray[0].rank === s11History[0].rank;
                                                    let lastSame = historyArray[historyArray.length - 1].date === s11History[s11History.length - 1].date && historyArray[historyArray.length - 1].rank === s11History[s11History.length - 1].rank;
                                                    if (firstSame && lastSame) isCopy = true;
                                                }
                                            }

                                            if (!data || isCopy || historyArray.length === 0) {
                                                let finalRank = null;
                                                if (data && data.stats && data.stats.league) {
                                                    finalRank = data.stats.league.points;
                                                    if (finalRank == null) finalRank = data.stats.league.rank;
                                                }
                                                if (finalRank == null && seasonalData) {
                                                    let sObj = seasonalData[sid] || seasonalData[s] || (Array.isArray(seasonalData) ? seasonalData.find(x => x.id === sid || x.season === sid || x.seasonId === sid || x.name === sid) : null);
                                                    if (sObj) {
                                                        finalRank = sObj.points;
                                                        if (finalRank == null) finalRank = sObj.rank;
                                                        if (finalRank == null && sObj.league) {
                                                            finalRank = sObj.league.points;
                                                            if (finalRank == null) finalRank = sObj.league.rank;
                                                        }
                                                    }
                                                }

                                                if (finalRank != null) {
                                                    let dateStr = new Date().toISOString().split('.')[0];
                                                    results.seasons[s] = [ { date: dateStr, rank: finalRank } ];
                                                    foundAny = true;
                                                }
                                            } else {
                                                results.seasons[s] = historyArray;
                                                foundAny = true;
                                            }

                                            if (s === 11 && data && data.stats) {
                                                playerStats = data.stats;
                                            } else if (!playerStats && data && data.stats) {
                                                playerStats = data.stats;
                                            }
                                            
                                            await new Promise(resolve => setTimeout(resolve, 300));
                                        }

                                        if (foundAny) {
                                            results.player = playerStats;
                                            results.success = true;
                                            window.Android.processJSON(JSON.stringify(results));
                                        } else {
                                            window.Android.processJSON(JSON.stringify({error: "Joueur introuvable. Assurez-vous d'être dans le Top 10 000."}));
                                        }
                                    }
                                    
                                    setTimeout(scrapeAllSeasons, 500);
                                })();
                            """.trimIndent()

                            view?.evaluateJavascript(interceptScript, null)
                        }
                    }
                }
            },
            update = { view ->
                view.loadUrl(urlToLoad)
            }
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock en Portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            RankTrackerApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankTrackerApp() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var currentTab by remember { mutableStateOf(AppTab.HOME) }

    var allSeasons by remember { mutableStateOf(loadAllSeasons(context)) }
    var selectedSeason by remember { mutableStateOf(loadSelectedSeason(context)) }
    var isDarkMode by remember { mutableStateOf(true) }   // Mode sombre forcé — toujours actif
    var isEnglish by remember { mutableStateOf(loadLanguage(context)) }
    var disableSplash by remember { mutableStateOf(loadDisableSplash(context)) }
    var autoUpdateStartup by remember { mutableStateOf(loadAutoUpdate(context)) }
    var chartType by remember { mutableStateOf(loadChartType(context)) }

    var rankGoals by remember { mutableStateOf(loadRankGoals(context)) }
    var customTags by remember { mutableStateOf(loadCustomTags(context)) }
    var hiddenStatsTags by remember { mutableStateOf(loadHiddenStatsTags(context)) }
    var playerProfile by remember { mutableStateOf(loadPlayerProfile(context)) }

    var showSplash by remember { mutableStateOf(!disableSplash) }
    var showSettings by remember { mutableStateOf(false) }
    var updateAvailableUrl by remember { mutableStateOf<String?>(null) }
    var autoUpdateInProgress by remember { mutableStateOf(false) }

    val themeProgress by animateFloatAsState(targetValue = if (isDarkMode) 0f else 1f, animationSpec = tween(400), label = "Theme")
    val palette = lerpPalette(DarkPalette, LightPalette, themeProgress)
    val s = if (isEnglish) EN else FR
    val tagGroups = getTagGroups(isEnglish)

    fun persistSeason(next: List<RankEntry>) {
        val updated = allSeasons.toMutableMap()
        updated[selectedSeason] = next
        allSeasons = updated
        saveAllSeasons(context, updated)
    }

    val currentSeasonEntries = allSeasons[selectedSeason] ?: emptyList()
    val currentRank = currentSeasonEntries.lastOrNull()?.rank

    val animatedRankScore by animateIntAsState(targetValue = currentRank ?: 0, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "RSScore")
    val rankName = rankNameFor(currentRank ?: 0, playerProfile?.globalRank)

    val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")
    val offset by infiniteTransition.animateFloat(
        initialValue = -600f, targetValue = 1200f,
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
            rankName == "Ruby" -> listOf(Color(0xFF800020), Color(0xFFFF0F50), Color(0xFFFFCCD5), Color(0xFFFF0F50), Color(0xFF800020))
            else -> listOf(palette.textPrimary, palette.textPrimary, palette.textPrimary)
        },
        start = Offset(offset, 0f), end = Offset(offset + 300f, 300f)
    )



    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val fabScaleTransition = rememberInfiniteTransition()
    val fabPulse by if (currentSeasonEntries.isEmpty() && !showSplash) {
        fabScaleTransition.animateFloat(
            initialValue = 1f, targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse)
        )
    } else remember { mutableStateOf(1f) }

    val fabInteractionSource = remember { MutableInteractionSource() }
    val isFabPressed by fabInteractionSource.collectIsPressedAsState()
    val fabClickScale by animateFloatAsState(targetValue = if (isFabPressed) 0.85f else 1f)

    // Animation de couleur (gauche vers droite) pour le bouton FAB
    val fabOffset by fabScaleTransition.animateFloat(
        initialValue = -300f, targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart)
    )
    val animatedFabBrush = Brush.linearGradient(
        colors = listOf(palette.accent, Color(0xFFFFD54F), Color(0xFFFFF59D), Color(0xFFFFD54F), palette.accent),
        start = Offset(fabOffset, 0f), end = Offset(fabOffset + 150f, 150f)
    )

    var editingDialogIndex by remember { mutableStateOf<Int?>(null) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }
    var showDisclaimer by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var tempUsername by remember { mutableStateOf("") }

    // GitHub Update Check (1 fois par jour avec 5 sec de délai)
    LaunchedEffect(Unit) {
        delay(5000)
        val lastCheck = loadLastGithubCheck(context)
        val snoozed = loadSnoozeGithub(context)
        val now = System.currentTimeMillis()

        if (now - lastCheck > 24L * 3600 * 1000 && now - snoozed > 24L * 3600 * 1000) {
            val latestVer = checkGithubUpdate(context)
            if (latestVer != null) {
                updateAvailableUrl = "https://github.com/Delta300IQ/THE_FINALS_RANK_TRACKER_ANDROID/releases"
            }
            saveLastGithubCheck(context, now)
        }
    }

    // Auto-update Start
    LaunchedEffect(Unit) {
        if (autoUpdateStartup && playerProfile != null) {
            autoUpdateInProgress = true
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = palette.accent, background = palette.bg, surface = palette.surface)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF9FB6CD), Color(0xFF606979)),
                        radius = 1200f // Dégradé ajusté pour voir les bords sombres
                    )
                )
        ) {
            AnimatedVisibility(visible = !showSplash, enter = fadeIn(tween(600)), exit = fadeOut()) {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        // Bottom nav — style The Finals exact
                        // Actif : rectangle BLANC, texte NOIR gras italic
                        // Inactif : fond #2A3038, texte gris
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A1F26))
                        ) {
                            // Ligne séparatrice top
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF3A4250)))
                            val tabs = listOf(
                                AppTab.HOME to s.tabHome,
                                AppTab.STATS to s.tabStats,
                                AppTab.HISTORY to s.tabHistory
                            )
                            val selectedIndex = tabs.indexOfFirst { it.first == currentTab }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                val tabWidth = maxWidth / tabs.size
                                val indicatorOffset by animateDpAsState(
                                    targetValue = tabWidth * selectedIndex,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                )

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    tabs.forEach { _ ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 3.dp)
                                                .background(Color(0xFF2A3038), RoundedCornerShape(4.dp))
                                                .height(38.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(x = indicatorOffset)
                                        .width(tabWidth)
                                        .padding(horizontal = 3.dp)
                                        .background(Color.White, RoundedCornerShape(4.dp))
                                        .height(38.dp)
                                )

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    tabs.forEach { (tab, label) ->
                                        val isSelected = currentTab == tab
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clickable { currentTab = tab },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label.uppercase(),
                                                color = if (isSelected) Color.Black else Color(0xFF8C9DAA),
                                                fontFamily = BarlowCondensed,
                                                fontWeight = FontWeight.Bold,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                fontSize = 13.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        Box(
                            modifier = Modifier
                                .scale(fabPulse * fabClickScale)
                                .size(56.dp)
                                .background(animatedFabBrush, RoundedCornerShape(4.dp))
                                .clickable(
                                    interactionSource = fabInteractionSource,
                                    indication = null, // l'animation de scale suffit
                                    onClick = { showBottomSheet = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontFamily = BarlowCondensed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = Color.Black
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
                        Spacer(modifier = Modifier.height(24.dp))
                        val currentHeaderTitle = when(currentTab) {
                            AppTab.HOME -> s.title
                            AppTab.STATS -> s.statsTitle
                            AppTab.HISTORY -> s.historyTitle
                        }
                        HeaderSection(s.eyebrow, currentHeaderTitle, playerProfile, palette) {
                            showSettings = true
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        SeasonLanguageSelector(selectedSeason, isEnglish, s.season, palette,
                            onPreviousSeason = { if (selectedSeason > 1) { selectedSeason -= 1; saveSelectedSeason(context, selectedSeason) } },
                            onNextSeason = { selectedSeason += 1; saveSelectedSeason(context, selectedSeason) },
                            onToggleLanguage = { isEnglish = it; saveLanguage(context, isEnglish) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        AnimatedContent(
                            targetState = Triple(currentTab, selectedSeason, isEnglish),
                            transitionSpec = {
                                val direction = if (targetState.second != initialState.second) {
                                    if (targetState.second > initialState.second) 1 else -1
                                } else {
                                    if (targetState.first.ordinal > initialState.first.ordinal) 1 else -1
                                }
                                slideInHorizontally(tween(400)) { width -> direction * width } + fadeIn(tween(400)) togetherWith
                                        slideOutHorizontally(tween(400)) { width -> -direction * width } + fadeOut(tween(400))
                            }, label = "tab_anim"
                        ) { target ->
                            val (tab, season, _) = target
                            val entries = allSeasons[season] ?: emptyList()
                            val rank = entries.lastOrNull()?.rank

                            when (tab) {
                                AppTab.HOME -> TabHome(
                                    currentSeasonEntries = entries, currentRank = rank, animatedRankScore = animatedRankScore,
                                    animatedRankBrush = animatedRankBrush, rankGoals = rankGoals, selectedSeason = season,
                                    isEnglish = isEnglish, isDarkMode = isDarkMode, palette = palette, s = s,
                                    playerProfile = playerProfile,
                                    onSaveGoal = { threshold -> val updated = rankGoals.toMutableMap(); updated[season] = threshold; rankGoals = updated; saveRankGoals(context, updated) },
                                    onClearGoal = { val updated = rankGoals.toMutableMap(); updated.remove(season); rankGoals = updated; saveRankGoals(context, updated) },
                                    onEditClick = { editingDialogIndex = it },
                                    onDeleteClick = { idx -> persistSeason(entries.filterIndexed { i, _ -> i != idx }) },
                                    onNavigateToHistory = { currentTab = AppTab.HISTORY }
                                )
                                AppTab.STATS -> TabStats(
                                    currentSeasonEntries = entries, currentRank = rank, animatedRankBrush = animatedRankBrush,
                                    allSeasons = allSeasons, selectedSeason = season, hiddenStatsTags = hiddenStatsTags,
                                    isEnglish = isEnglish, isDarkMode = isDarkMode, palette = palette, s = s,
                                    playerProfile = playerProfile, chartType = chartType,
                                    onToggleHideTag = { tag -> val updated = if (hiddenStatsTags.contains(tag)) hiddenStatsTags - tag else hiddenStatsTags + tag; hiddenStatsTags = updated; saveHiddenStatsTags(context, updated) },
                                    onSaveSeason = { persistSeason(it) }, customTags = customTags, tagGroups = tagGroups,
                                    onAddCustomTag = { newTag -> if (!customTags.contains(newTag)) { val updated = customTags + newTag; customTags = updated; saveCustomTags(context, updated) } }
                                )
                                AppTab.HISTORY -> TabHistory(
                                    currentSeasonEntries = entries, allSeasons = allSeasons, selectedSeason = season,
                                    isEnglish = isEnglish, isDarkMode = isDarkMode, palette = palette, s = s,
                                    customTags = customTags, tagGroups = tagGroups,
                                    playerProfile = playerProfile,
                                    onSavePlayerProfile = { p -> playerProfile = p; savePlayerProfile(context, p) },
                                    onSaveSeason = { persistSeason(it) },
                                    onSaveAllSeasons = { allSeasons = it; saveAllSeasons(context, it) },
                                    onAddCustomTag = { newTag -> if (!customTags.contains(newTag)) { val updated = customTags + newTag; customTags = updated; saveCustomTags(context, updated) } },
                                    onDeleteCustomTag = { tag -> val updated = customTags - tag; customTags = updated; saveCustomTags(context, updated) },
                                    onEditClick = { editingDialogIndex = it },
                                    onDeleteClick = { idx -> persistSeason(entries.filterIndexed { i, _ -> i != idx }) }
                                )
                            }
                        }
                    }
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = palette.surface
                ) {
                    InputSection(
                        currentSeasonEntries = currentSeasonEntries, customTags = customTags, tagGroups = tagGroups,
                        isEnglish = isEnglish, isDarkMode = isDarkMode, palette = palette, s = s,
                        playerProfile = playerProfile,
                        onTriggerAutoUpdate = {
                            showBottomSheet = false
                            autoUpdateInProgress = true
                        },
                        onSave = { parsed, notes ->
                            persistSeason(currentSeasonEntries + RankEntry(parsed, System.currentTimeMillis(), notes.toList()))
                            showBottomSheet = false
                        },
                        onAddCustomTag = { newTag -> if (!customTags.contains(newTag)) { val updated = customTags + newTag; customTags = updated; saveCustomTags(context, updated) } },
                        onLongClickCustomTag = { tagToDelete = it }
                    )
                }
            }

            if (editingDialogIndex != null) {
                val idx = editingDialogIndex!!
                val entryToEdit = currentSeasonEntries.getOrNull(idx)
                if (entryToEdit != null) {
                    ChartEditMatchDialog(
                        idx = idx, initialRank = entryToEdit.rank.toString(), initialNotes = entryToEdit.notes.toSet(),
                        customTags = customTags, tagGroups = tagGroups, palette = palette, s = s, isEnglish = isEnglish,
                        onSave = { parsed, notes ->
                            val next = currentSeasonEntries.toMutableList()
                            next[idx] = next[idx].copy(rank = parsed, notes = notes.toList())
                            persistSeason(next)
                            editingDialogIndex = null
                        },
                        onDismiss = { editingDialogIndex = null },
                        onAddCustomTag = { newTag -> if (!customTags.contains(newTag)) { val updated = customTags + newTag; customTags = updated; saveCustomTags(context, updated) } },
                        onLongClickCustomTag = { tagToDelete = it }
                    )
                } else editingDialogIndex = null
            }

            if (tagToDelete != null) {
                AlertDialog(
                    onDismissRequest = { tagToDelete = null }, containerColor = palette.surface,
                    title = { Text(s.deleteTagTitle, color = palette.textPrimary) },
                    text = { Text("${s.deleteTagDesc}\n\"$tagToDelete\"", color = palette.textMuted) },
                    confirmButton = { TextButton(onClick = { val updated = customTags - tagToDelete!!; customTags = updated; saveCustomTags(context, updated); tagToDelete = null }) { Text(s.confirmWord, color = palette.red) } },
                    dismissButton = { TextButton(onClick = { tagToDelete = null }) { Text(s.cancelWord, color = palette.textMuted) } }
                )
            }

            if (updateAvailableUrl != null) {
                AlertDialog(
                    onDismissRequest = { updateAvailableUrl = null },
                    containerColor = palette.surface,
                    title = { Text(s.updateAvailableTitle, color = palette.accent, fontWeight = FontWeight.Bold) },
                    text = { Text(s.updateAvailableDesc, color = palette.textPrimary) },
                    confirmButton = {
                        Button(
                            onClick = {
                                uriHandler.openUri(updateAvailableUrl!!)
                                updateAvailableUrl = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                        ) {
                            Text(s.btnUpdate)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            saveSnoozeGithub(context, System.currentTimeMillis())
                            updateAvailableUrl = null
                        }) {
                            Text(s.btnLater, color = palette.textMuted)
                        }
                    }
                )
            }

            if (showDisclaimer) {
                AlertDialog(
                    onDismissRequest = { showDisclaimer = false },
                    containerColor = palette.surface,
                    title = { Text(s.disclaimerTitle, color = palette.accent, fontWeight = FontWeight.Bold, fontFamily = BarlowCondensed, fontSize = 20.sp) },
                    text = { Text(s.disclaimerDesc, color = palette.textPrimary, fontSize = 14.sp) },
                    confirmButton = {
                        TextButton(onClick = { showDisclaimer = false }) { Text(s.disclaimerBtn, color = palette.accent, fontWeight = FontWeight.Bold) }
                    }
                )
            }

            // Settings overlay — style The Finals Settings screen
            BackHandler(enabled = showSettings) {
                showSettings = false
            }
            AnimatedVisibility(
                visible = showSettings,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(colors = listOf(Color(0xFFD21F3C), Color(0xFFBE2E34)), start = Offset.Zero, end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                ) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Spacer(modifier = Modifier.height(40.dp))

                        // Header style jeu
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.settingsTitle.uppercase(),
                                color = Color.White,
                                fontFamily = BarlowCondensed,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                    .clickable { showSettings = false }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "BACK" else "RETOUR",
                                    color = Color.White,
                                    fontFamily = BarlowCondensed,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        // Ligne décorative sous le titre
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp).background(Color.White.copy(alpha = 0.25f)))
                        Spacer(modifier = Modifier.height(24.dp))

                        // Section PROFILE
                        SettingsSectionLabel(if (isEnglish) "PROFILE" else "PROFIL", palette)
                        Spacer(modifier = Modifier.height(4.dp))
                        SettingsRow(palette, clickable = {
                            tempUsername = playerProfile?.name ?: ""
                            showUsernameDialog = true
                        }) {
                            Text(if (isEnglish) "Player Name" else "Pseudo", color = Color.White, fontFamily = BarlowCondensed, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(text = playerProfile?.name ?: if (isEnglish) "Not Set" else "Non défini", color = palette.textMuted, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        // Section APPARENCE — sans mode sombre
                        SettingsSectionLabel(if (isEnglish) "DISPLAY" else "AFFICHAGE", palette)
                        Spacer(modifier = Modifier.height(4.dp))

                        SettingsRow(palette) {
                            Text(s.disableSplashLabel, color = Color.White, fontFamily = BarlowCondensed, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Switch(
                                checked = disableSplash,
                                onCheckedChange = { disableSplash = it; saveDisableSplash(context, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF9A6D78),
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                    uncheckedTrackColor = Color(0xFF491B26)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Section LANGUE
                        SettingsSectionLabel(s.languageLabel.uppercase(), palette)
                        Spacer(modifier = Modifier.height(4.dp))

                        SettingsRow(palette) {
                            Text(s.languageLabel, color = Color.White, fontFamily = BarlowCondensed, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "FR",
                                    color = if (!isEnglish) Color.White else Color.White.copy(alpha = 0.45f),
                                    fontFamily = BarlowCondensed,
                                    fontWeight = if (!isEnglish) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 16.sp,
                                    modifier = Modifier.clickable { isEnglish = false; saveLanguage(context, false) }.padding(8.dp)
                                )
                                Text(" / ", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                                Text(
                                    "EN",
                                    color = if (isEnglish) Color.White else Color.White.copy(alpha = 0.45f),
                                    fontFamily = BarlowCondensed,
                                    fontWeight = if (isEnglish) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 16.sp,
                                    modifier = Modifier.clickable { isEnglish = true; saveLanguage(context, true) }.padding(8.dp)
                                )
                            }
                        }

                        if (playerProfile != null) {
                            Spacer(modifier = Modifier.height(20.dp))
                            SettingsSectionLabel(if (isEnglish) "AUTO-UPDATE" else "MISE À JOUR", palette)
                            Spacer(modifier = Modifier.height(4.dp))
                            SettingsRow(palette) {
                                Text(s.autoUpdateLabel, color = Color.White, fontFamily = BarlowCondensed, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = autoUpdateStartup,
                                    onCheckedChange = { autoUpdateStartup = it; saveAutoUpdate(context, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF9A6D78),
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                        uncheckedTrackColor = Color(0xFF491B26)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Section STYLE DE GRAPHIQUE
                        SettingsSectionLabel(s.chartTypeLabel, palette)
                        Spacer(modifier = Modifier.height(4.dp))

                        listOf(
                            ChartType.CANDLESTICK to s.chartCandle,
                            ChartType.STRAIGHT_LINE to s.chartLine,
                            ChartType.CURVED_LINE to s.chartCurve
                        ).forEach { (t, label) ->
                            val isSelected = chartType == t
                            val rowModifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color.White else Color(0xFF651C2B))
                                .clickable { chartType = t; saveChartType(context, t) }
                                .padding(horizontal = 20.dp, vertical = 14.dp)

                            Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontFamily = BarlowCondensed,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF491B26)))
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                        SettingsRow(palette, clickable = { showDisclaimer = true }) {
                            Text(s.disclaimerTitle, color = Color.White, fontFamily = BarlowCondensed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                        Text(
                            text = s.footer,
                            color = Color.White, // Blanc pur
                            fontFamily = BarlowCondensed,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.namatama_feedback),
                                contentDescription = "Mascot",
                                modifier = Modifier.size(120.dp).alpha(0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }


            if (autoUpdateInProgress && playerProfile != null) {
                DataFetcherWebView(playerProfile!!.name) { data ->
                    if (data.success) {
                        val merged = (allSeasons.toMutableMap())
                        data.seasons.forEach { (sNum, newEntries) ->
                            val current = merged[sNum] ?: emptyList()
                            val combined = (current + newEntries).distinctBy { it.timestamp }.sortedBy { it.timestamp }
                            val filtered = mutableListOf<RankEntry>()
                            for (entry in combined) {
                                if (filtered.isEmpty() || filtered.last().rank != entry.rank) {
                                    filtered.add(entry)
                                }
                            }
                            merged[sNum] = filtered
                        }
                        allSeasons = merged
                        saveAllSeasons(context, merged)

                        val updatedProfile = PlayerProfile(
                            data.name ?: playerProfile!!.name,
                            data.globalRank ?: playerProfile!!.globalRank,
                            data.rankChange ?: playerProfile!!.rankChange
                        )
                        playerProfile = updatedProfile
                        savePlayerProfile(context, updatedProfile)
                    }
                    autoUpdateInProgress = false
                }
            }

            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(),
                exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200))
            ) {
                SplashUI(palette, isEnglish, currentRank, animatedRankBrush) { showSplash = false }
            }

            if (showUsernameDialog) {
                AlertDialog(
                    onDismissRequest = { showUsernameDialog = false },
                    containerColor = palette.surface,
                    title = { Text(if (isEnglish) "Player Name" else "Pseudo", color = palette.textPrimary) },
                    text = {
                        OutlinedTextField(
                            value = tempUsername,
                            onValueChange = { if (it.length <= 30) tempUsername = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary,
                                focusedBorderColor = palette.accent,
                                unfocusedBorderColor = palette.border
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val p = playerProfile ?: PlayerProfile(name = "", globalRank = null, rankChange = null)
                            val newP = p.copy(name = tempUsername.trim())
                            playerProfile = newP
                            savePlayerProfile(context, newP)
                            showUsernameDialog = false
                        }) { Text(s.confirmWord, color = palette.accent) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUsernameDialog = false }) { Text(s.cancelWord, color = palette.textMuted) }
                    }
                )
            }
        }
    }
}

@Composable
internal fun SettingsSectionLabel(text: String, palette: Palette) {
    // Label de section simple, blanc pur, plus grand, sans fond rouge foncé
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = text,
            color = Color.White,
            fontFamily = BarlowCondensed,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
internal fun SettingsRow(
    palette: Palette,
    clickable: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val baseModifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF651C2B))   // Couleur de ligne settings exacte, sans transparence
        .then(if (clickable != null) Modifier.clickable { clickable() } else Modifier)
        .padding(horizontal = 20.dp, vertical = 14.dp)
    Row(
        modifier = baseModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF491B26)))
}

@Composable
internal fun HeaderSection(eyebrow: String, title: String, playerProfile: PlayerProfile?, palette: Palette, onSettingsClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column {
            Text(
                text = eyebrow.uppercase(),
                color = Color.White,
                fontFamily = BarlowCondensed,
                fontSize = 16.sp, // Plus grand
                fontWeight = FontWeight.Bold, // Gras
                letterSpacing = 2.sp
            )
            Text(
                text = title,
                color = palette.accent,
                fontFamily = BarlowCondensed,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (playerProfile != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(text = playerProfile.name, color = Color.White, fontFamily = BarlowCondensed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (playerProfile.globalRank != null && playerProfile.globalRank > 0) {
                        Text(text = " · TOP ${formatNum(playerProfile.globalRank)}", color = Color.White, fontFamily = BarlowCondensed, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp), letterSpacing = 0.5.sp)
                        if (playerProfile.rankChange != null) {
                            val changeColor = if (playerProfile.rankChange >= 0) palette.green else palette.red
                            val symbol = if (playerProfile.rankChange >= 0) "▲+" else "▼"
                            Text(text = " ($symbol${abs(playerProfile.rankChange)})", color = changeColor, fontFamily = BarlowCondensed, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
        // Icône paramètres pure white
        Box(
            modifier = Modifier
                .border(1.dp, palette.border, RoundedCornerShape(2.dp))
                .clickable { onSettingsClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "⚙",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}


@Composable
internal fun SeasonLanguageSelector(selectedSeason: Int, isEnglish: Boolean, seasonLabel: String, palette: Palette, onPreviousSeason: () -> Unit, onNextSeason: () -> Unit, onToggleLanguage: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "◀", color = if (selectedSeason > 1) palette.accent else palette.border, fontFamily = BarlowCondensed, fontSize = 16.sp, modifier = Modifier.clickable(enabled = selectedSeason > 1) { onPreviousSeason() }.padding(6.dp))
            Text(text = "$seasonLabel $selectedSeason".uppercase(), color = palette.textPrimary, fontFamily = BarlowCondensed, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(text = "▶", color = palette.accent, fontFamily = BarlowCondensed, fontSize = 16.sp, modifier = Modifier.clickable { onNextSeason() }.padding(6.dp))
        }
    }
}

@Composable
internal fun InputSection(
    currentSeasonEntries: List<RankEntry>, customTags: List<String>, tagGroups: List<List<String>>,
    isEnglish: Boolean, isDarkMode: Boolean, palette: Palette, s: Strings, playerProfile: PlayerProfile?,
    onTriggerAutoUpdate: () -> Unit, onSave: (Int, Set<String>) -> Unit, onAddCustomTag: (String) -> Unit, onLongClickCustomTag: (String) -> Unit
) {
    var inputValue by remember { mutableStateOf("") }
    var selectedNotes by remember { mutableStateOf(setOf<String>()) }
    var showInvalidScoreMsg by remember { mutableStateOf(false) }
    var showTypoErrorMsg by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val saveButtonScale = remember { Animatable(1f) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).imePadding()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(s.addMatchTitle, color = palette.textPrimary, fontFamily = BarlowCondensed, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            if (playerProfile != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isEnglish) "AUTO UPDATE" else "MISE À JOUR AUTO",
                        color = palette.accent,
                        fontFamily = BarlowCondensed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.clickable { onTriggerAutoUpdate() }
                    )
                    Text(
                        text = "(Top 10 000)",
                        color = palette.textMuted,
                        fontFamily = BarlowCondensed,
                        fontSize = 10.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it.filter { c -> c.isDigit() } },
            placeholder = { Text(if (currentSeasonEntries.isEmpty()) s.startingRankPlaceholder else s.newRankPlaceholder) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (showInvalidScoreMsg) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(s.invalidScoreMsg, color = palette.red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        if (showTypoErrorMsg) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(s.typoDetectedMsg, color = palette.red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        if (inputValue.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(s.selectTags, color = palette.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            TagChipsSelector(
                tags = if (isEnglish) DEFAULT_TAGS_EN else DEFAULT_TAGS_FR,
                selected = selectedNotes, customTags = customTags, tagGroups = tagGroups, palette = palette,
                onToggle = { tag ->
                    val isSelected = selectedNotes.contains(tag)
                    selectedNotes = if (isSelected) selectedNotes - tag else {
                        val inGroup = tagGroups.find { it.contains(tag) }
                        if (inGroup != null) selectedNotes.filter { !inGroup.contains(it) }.toSet() + tag else selectedNotes + tag
                    }
                },
                onAddClick = { showAddDialog = true },
                onLongClickCustomTag = onLongClickCustomTag
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val parsed = inputValue.toIntOrNull()
                if (parsed != null) {
                    val lastRank = currentSeasonEntries.lastOrNull()?.rank
                    if (parsed > 85000) { showInvalidScoreMsg = true; showTypoErrorMsg = false }
                    else if (lastRank != null && abs(parsed - lastRank) > 3500) { showTypoErrorMsg = true; showInvalidScoreMsg = false }
                    else if (lastRank != null && parsed == lastRank) { inputValue = "" }
                    else {
                        coroutineScope.launch {
                            saveButtonScale.snapTo(0.85f)
                            saveButtonScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        onSave(parsed, selectedNotes)
                    }
                }
            },
            enabled = inputValue.toIntOrNull() != null,
            modifier = Modifier.fillMaxWidth().height(50.dp).scale(saveButtonScale.value),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.accentOn)
        ) {
            Text(if (currentSeasonEntries.isEmpty()) s.startButton else s.saveButton, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = palette.surface, titleContentColor = palette.textPrimary, textContentColor = palette.textMuted,
            title = { Text(s.addTagTitle) },
            text = {
                OutlinedTextField(
                    value = newTagText, onValueChange = { newTagText = it },
                    placeholder = { Text(s.addTagPlaceholder) }, singleLine = true,
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
                TextButton(onClick = { showAddDialog = false; newTagText = "" }) { Text(s.cancelWord, color = palette.textMuted) }
            }
        )
    }
}