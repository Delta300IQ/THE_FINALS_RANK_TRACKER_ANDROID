package com.example.finalsranktracker

import android.content.Context
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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.unit.max
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Palette (sombre / clair) ----------
private data class Palette(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val accent: Color,
    val accentOn: Color,
    val cyan: Color,
    val green: Color,
    val red: Color,
    val textPrimary: Color,
    val textMuted: Color
)

private val DarkPalette = Palette(
    bg = Color(0xFF16140F),
    surface = Color(0xFF211D17),
    surfaceAlt = Color(0xFF2A251C),
    border = Color(0xFF3A3226),
    accent = Color(0xFFFF6A1A),
    accentOn = Color(0xFF1A1006),
    cyan = Color(0xFF4FC8D6),
    green = Color(0xFF33D17A),
    red = Color(0xFFFF4D5E),
    textPrimary = Color(0xFFF3EFE7),
    textMuted = Color(0xFF9C9284)
)

private val LightPalette = Palette(
    bg = Color(0xFFFAF7F2),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF0EBE2),
    border = Color(0xFFDDD5C7),
    accent = Color(0xFFE85D04),
    accentOn = Color(0xFFFFFFFF),
    cyan = Color(0xFF0C7C90),
    green = Color(0xFF1B9C5C),
    red = Color(0xFFFF4D5E),
    textPrimary = Color(0xFF211D17),
    textMuted = Color(0xFF837B6C)
)

private fun lerpPalette(a: Palette, b: Palette, t: Float): Palette = Palette(
    bg = lerp(a.bg, b.bg, t),
    surface = lerp(a.surface, b.surface, t),
    surfaceAlt = lerp(a.surfaceAlt, b.surfaceAlt, t),
    border = lerp(a.border, b.border, t),
    accent = lerp(a.accent, b.accent, t),
    accentOn = lerp(a.accentOn, b.accentOn, t),
    cyan = lerp(a.cyan, b.cyan, t),
    green = lerp(a.green, b.green, t),
    red = lerp(a.red, b.red, t),
    textPrimary = lerp(a.textPrimary, b.textPrimary, t),
    textMuted = lerp(a.textMuted, b.textMuted, t)
)

// ---------- Style "carte" néomorphique avec reflet spéculaire ----------
private fun Modifier.neumorphicCard(
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
        lerp(it, if (isDarkMode) Color.Black else palette.border, 0.16f)
    }
    val borderColors = if (accentColor != null) {
        listOf(
            lerp(accentColor, Color.White, if (isDarkMode) 0.3f else 0.65f),
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

// ---------- Traductions ----------
private data class Strings(
    val eyebrow: String,
    val title: String,
    val currentRankLabel: String,
    val nextRankPrefix: String,
    val rubyMax: String,
    val best: String,
    val worst: String,
    val matches: String,
    val detailedStats: String,
    val avgProgress: String,
    val avgGain: String,
    val avgLoss: String,
    val biggestGain: String,
    val biggestLoss: String,
    val emptyState: String,
    val noDataPeriod: String,
    val zoomOut: String,
    val zoomIn: String,
    val periodWeek: String,
    val periodMonth: String,
    val periodAll: String,
    val vsPrevious: String,
    val vsLowest: String,
    val vsHighest: String,
    val recordedAt: String,
    val startingRankPlaceholder: String,
    val newRankPlaceholder: String,
    val startButton: String,
    val saveButton: String,
    val undoLast: String,
    val historyShow: String,
    val historyHide: String,
    val deleteConfirm: String,
    val confirmWord: String,
    val cancelWord: String,
    val resetAll: String,
    val confirmResetAll: String,
    val season: String,
    val darkModeLabel: String,
    val lightModeLabel: String,
    val exportButton: String,
    val exportedToClipboard: String,
    val importButton: String,
    val importConfirmQuestion: String,
    val importFoundPrefix: String,
    val importSeasonsWord: String,
    val importMatchesWord: String,
    val importErrorMsg: String,
    val importSuccessMsg: String,
    val footer: String,
    val rsRemaining: String,
    val selectNotes: String,
    val avgStatsByNote: String,
    val exportJsonButton: String,
    val importJsonButton: String,
    val exportedToJson: String,
    val importJsonErrorMsg: String,
    val winStreakLabel: String,
    val undoConfirmMsg: String,
    val top10kLabel: String,
    val bestWinStreakLabel: String,
    val bestLoseStreakLabel: String,
    val winRateLabel: String,
    val patchNotesLabel: String,
    val invalidScoreMsg: String,
    val typoDetectedMsg: String,
    val rankSavedMsg: String,
    val progressTodayLabel: String,
    val progressWeekLabel: String,
    val progressMonthLabel: String,
    val progressSeasonLabel: String,
    val rankGoalTitle: String,
    val rankGoalPlaceholder: String,
    val rankGoalSet: String,
    val rankGoalReached: String,
    val rankGoalNotEnoughData: String,
    val rankGoalEstimatePrefix: String,
    val rankGoalEstimateSuffix: String,
    val sortLabel: String,
    val filterLabel: String,
    val sortOldestFirst: String,
    val sortNewestFirst: String,
    val sortGainAsc: String,
    val sortGainDesc: String,
    val filterAll: String,
    val noMatchForFilter: String,
    val rankGoalAlreadyMax: String,
    val previousSeasonDesc: String,
    val nextSeasonDesc: String,
    val closeDesc: String,
    val editDesc: String,
    val removeDesc: String,
    val compareSeasonsLabel: String,
    val compareSeasonsPrompt: String,
    val compareSeasonsNoOthers: String,
    val performanceByTimeTitle: String,
    val dayOfWeekLabel: String,
    val timeOfDayLabel: String,
    val exportReminderMessage: String,
    val exportReminderDismiss: String
)

private val FR = Strings(
    eyebrow = "THE FINALS · CLASSÉ",
    title = "SUIVI DU RANG",
    currentRankLabel = "TON RANG ACTUEL",
    nextRankPrefix = "avant",
    rubyMax = "Ruby voir le\nclassement mondial",
    best = "MEILLEUR",
    worst = "PLUS BAS",
    matches = "MATCHS",
    detailedStats = "Statistiques détaillées",
    avgProgress = "Progression moyenne / match",
    avgGain = "Gain moyen",
    avgLoss = "Perte moyenne",
    biggestGain = "Plus gros gain",
    biggestLoss = "Plus grosse perte",
    emptyState = "Entre ton rang actuel pour démarrer le suivi. Chaque nouveau rang entré ajoutera un nouveau chandelier au graphique.",
    noDataPeriod = "Aucune donnée sur cette période.",
    zoomOut = "− zoom",
    zoomIn = "zoom +",
    periodWeek = "7 jours",
    periodMonth = "30 jours",
    periodAll = "Saison",
    vsPrevious = "Vs match précédent",
    vsLowest = "Vs rang le plus bas",
    vsHighest = "Vs rang le plus haut",
    recordedAt = "Enregistré le",
    startingRankPlaceholder = "Rang de départ",
    newRankPlaceholder = "Nouveau rang",
    startButton = "Démarrer",
    saveButton = "Enregistrer",
    undoLast = "↺ Supprimer le dernier match",
    historyShow = "Historique ▼",
    historyHide = "Historique ▲",
    deleteConfirm = "Supprimer ce match ?",
    confirmWord = "Confirmer",
    cancelWord = "Annuler",
    resetAll = "Tout réinitialiser",
    confirmResetAll = "Effacer tout l'historique ?",
    season = "Saison",
    darkModeLabel = "Mode sombre",
    lightModeLabel = "Mode clair",
    exportButton = "Exporter les données",
    exportedToClipboard = "Copié dans le presse-papiers !",
    importButton = "Importer des données",
    importConfirmQuestion = "Remplacer toutes les données actuelles par les nouvelles ?",
    importFoundPrefix = "Trouvé :",
    importSeasonsWord = "saison(s)",
    importMatchesWord = "match(s)",
    importErrorMsg = "Impossible de lire des données valides depuis le presse-papiers.",
    importSuccessMsg = "Données importées !",
    footer = "Made by Delta300IQ\nsend me any feedback on discord :\ndelta8771",
    rsRemaining = "RS restants",
    selectNotes = "Sélectionner des notes (optionnel) :",
    avgStatsByNote = "Moyenne par note :",
    exportJsonButton = "Exporter .json",
    importJsonButton = "Importer .json",
    exportedToJson = "Données exportées en fichier JSON !",
    importJsonErrorMsg = "Impossible d'importer ce fichier !",
    winStreakLabel = "WINSTREAK",
    undoConfirmMsg = "Supprimer le dernier\nmatch enregistré ?",
    top10kLabel = "Voir le Top 10 000",
    bestWinStreakLabel = "Meilleure série de gains",
    bestLoseStreakLabel = "Pire série de pertes",
    winRateLabel = "Win rate",
    patchNotesLabel = "Voir le dernier patch notes",
    invalidScoreMsg = "Tu te prends pour qui au juste, imbécile va !",
    typoDetectedMsg = "Variation de plus de 3500 RS détectée, vérifie ton score",
    rankSavedMsg = "Nouveau rang enregistré !",
    progressTodayLabel = "Progression du jour",
    progressWeekLabel = "Progression de la semaine",
    progressMonthLabel = "Progression du mois",
    progressSeasonLabel = "Progression de la saison (totale)",
    rankGoalTitle = "Objectif de rang",
    rankGoalPlaceholder = "Choisir un objectif",
    rankGoalSet = "Définir",
    rankGoalReached = "Objectif atteint !",
    rankGoalNotEnoughData = "Impossible de faire une estimation",
    rankGoalEstimatePrefix = "À ce rythme, encore ~",
    rankGoalEstimateSuffix = "matchs avant",
    sortLabel = "⇅ Trier",
    filterLabel = "▤ Filtres",
    sortOldestFirst = "Plus ancien → récent",
    sortNewestFirst = "Plus récent → ancien",
    sortGainAsc = "Gain RS croissant",
    sortGainDesc = "Gain RS décroissant",
    filterAll = "Tous",
    noMatchForFilter = "Aucun match ne correspond à ce filtre.",
    rankGoalAlreadyMax = "Tu es déjà au rang maximum !",
    previousSeasonDesc = "Saison précédente",
    nextSeasonDesc = "Saison suivante",
    closeDesc = "Fermer",
    editDesc = "Modifier",
    removeDesc = "Supprimer",
    compareSeasonsLabel = "Comparer les saisons",
    compareSeasonsPrompt = "Choisis une saison à comparer :",
    compareSeasonsNoOthers = "Aucune autre saison avec des données.",
    performanceByTimeTitle = "Performance par créneau",
    dayOfWeekLabel = "Par jour de la semaine",
    timeOfDayLabel = "Par moment de la journée",
    exportReminderMessage = "Ça fait un moment que tu n'as pas exporté tes données. Pense à faire une sauvegarde !",
    exportReminderDismiss = "Plus tard"
)

private val EN = Strings(
    eyebrow = "THE FINALS · RANKED",
    title = "RANK TRACKER",
    currentRankLabel = "YOUR CURRENT RANK",
    nextRankPrefix = "to",
    rubyMax = "Ruby view\nworld leaderboard",
    best = "BEST",
    worst = "LOWEST",
    matches = "MATCHES",
    detailedStats = "Detailed stats",
    avgProgress = "Average progression / match",
    avgGain = "Average gain",
    avgLoss = "Average loss",
    biggestGain = "Biggest gain",
    biggestLoss = "Biggest loss",
    emptyState = "Enter your current rank to start the tracking. Each new entry will add a new candle to the graph.",
    noDataPeriod = "No data for this period.",
    zoomOut = "− zoom",
    zoomIn = "zoom +",
    periodWeek = "7 days",
    periodMonth = "30 days",
    periodAll = "Season",
    vsPrevious = "Vs previous match",
    vsLowest = "Vs lowest rank",
    vsHighest = "Vs highest rank",
    recordedAt = "Recorded on",
    startingRankPlaceholder = "Starting rank",
    newRankPlaceholder = "Add a new rank",
    startButton = "Start",
    saveButton = "Save",
    undoLast = "↺ Remove last match",
    historyShow = "History ▼",
    historyHide = "History ▲",
    deleteConfirm = "Delete this match ?",
    confirmWord = "Confirm",
    cancelWord = "Cancel",
    resetAll = "Reset all",
    confirmResetAll = "Clear all history ?",
    season = "Season",
    darkModeLabel = "Dark mode",
    lightModeLabel = "Light mode",
    exportButton = "Export data",
    exportedToClipboard = "Copied to clipboard !",
    importButton = "Import data",
    importConfirmQuestion = "Replace all current data with the new data ?",
    importFoundPrefix = "Found:",
    importSeasonsWord = "season(s)",
    importMatchesWord = "match(es)",
    importErrorMsg = "Couldn't read valid data from the clipboard.",
    importSuccessMsg = "Data imported!",
    footer = "Made by Delta300IQ\nsend me any feedback on discord :\ndelta8771",
    rsRemaining = "RS remaining",
    selectNotes = "Select notes (optional):",
    avgStatsByNote = "Average by note:",
    exportJsonButton = "Export .json",
    importJsonButton = "Import .json",
    exportedToJson = "JSON file exported successfully !",
    importJsonErrorMsg = "Could not import this file !",
    winStreakLabel = "WINSTREAK",
    undoConfirmMsg = "Remove the last\nrecorded match ?",
    top10kLabel = "View Top 10,000",
    bestWinStreakLabel = "Best win streak",
    bestLoseStreakLabel = "Worst lose streak",
    winRateLabel = "Win rate",
    patchNotesLabel = "View latest patch notes",
    invalidScoreMsg = "Who do you think you are, idiot !",
    typoDetectedMsg = "Change of more than 3500 RS detected, check your score",
    rankSavedMsg = "New rank recorded!",
    progressTodayLabel = "Today's progress",
    progressWeekLabel = "This week's progress",
    progressMonthLabel = "This month's progress",
    progressSeasonLabel = "Season progress (total)",
    rankGoalTitle = "Rank goal",
    rankGoalPlaceholder = "Choose a goal",
    rankGoalSet = "Set",
    rankGoalReached = "Goal reached !",
    rankGoalNotEnoughData = "Unable to do an estimate",
    rankGoalEstimatePrefix = "At this rate, about ~",
    rankGoalEstimateSuffix = "matches before",
    sortLabel = "⇅ Sort",
    filterLabel = "▤ Filters",
    sortOldestFirst = "Oldest → newest",
    sortNewestFirst = "Newest → oldest",
    sortGainAsc = "RS gain ascending",
    sortGainDesc = "RS gain descending",
    filterAll = "All",
    noMatchForFilter = "No match matches this filter.",
    rankGoalAlreadyMax = "You're already at the max rank!",
    previousSeasonDesc = "Previous season",
    nextSeasonDesc = "Next season",
    closeDesc = "Close",
    editDesc = "Edit",
    removeDesc = "Remove",
    compareSeasonsLabel = "Compare seasons",
    compareSeasonsPrompt = "Choose a season to compare:",
    compareSeasonsNoOthers = "No other season with data.",
    performanceByTimeTitle = "Performance by time slot",
    dayOfWeekLabel = "By day of week",
    timeOfDayLabel = "By time of day",
    exportReminderMessage = "It's been a while since your last export. Consider backing up your data!",
    exportReminderDismiss = "Later"
)

// ---------- Modèle de données ----------
internal data class RankEntry(
    val rank: Int,
    val timestamp: Long,
    val notes: List<String> = emptyList()
)

private data class ChartPoint(val absoluteIndex: Int, val rank: Int, val timestamp: Long)
private enum class ChartPeriod { WEEK, MONTH, ALL }
private enum class HistorySortMode { OLDEST_FIRST, NEWEST_FIRST, GAIN_ASC, GAIN_DESC }

// ---------- Paliers de rang (RS = Rank Score) ----------
private val RANK_TIERS = listOf(
    0 to "Bronze 4", 2500 to "Bronze 3", 5000 to "Bronze 2", 7500 to "Bronze 1",
    10000 to "Silver 4", 12500 to "Silver 3", 15000 to "Silver 2", 17500 to "Silver 1",
    20000 to "Gold 4", 22500 to "Gold 3", 25000 to "Gold 2", 27500 to "Gold 1",
    30000 to "Platinum 4", 32500 to "Platinum 3", 35000 to "Platinum 2", 37500 to "Platinum 1",
    40000 to "Diamond 4", 42500 to "Diamond 3", 45000 to "Diamond 2", 47500 to "Diamond 1",
    55000 to "Ruby"
)

// ---------- Créneaux temporels (pour les stats de performance) ----------
private val DAY_LABELS_FR = listOf("Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam")
private val DAY_LABELS_EN = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val HOUR_BUCKETS = listOf(0..5, 6..11, 12..17, 18..23)
private val HOUR_BUCKET_LABELS_FR = listOf("Nuit", "Matin", "Aprem", "Soir")
private val HOUR_BUCKET_LABELS_EN = listOf("Night", "Morning", "Afternoon", "Evening")

internal fun rankNameFor(rs: Int): String {
    val tier = RANK_TIERS.lastOrNull { rs >= it.first }
    return tier?.second ?: "Bronze 4"
}

internal fun getLocalizedRankName(rawName: String, isEnglish: Boolean): String {
    if (rawName == "Ruby") return "Ruby"
    val parts = rawName.split(" ")
    if (parts.size < 2) return rawName
    val base = parts[0]
    val num = parts[1]
    val roman = when (num) {
        "4" -> "IV"
        "3" -> "III"
        "2" -> "II"
        "1" -> "I"
        else -> num
    }
    val localizedBase = if (isEnglish) {
        base
    } else {
        when (base) {
            "Bronze" -> "Bronze"
            "Silver" -> "Argent"
            "Gold" -> "Or"
            "Platinum" -> "Platine"
            "Diamond" -> "Diamant"
            else -> base
        }
    }
    return "$localizedBase $roman"
}

internal fun rankLogoResFor(rs: Int): Int {
    val tier = RANK_TIERS.lastOrNull { rs >= it.first }
    return when (tier?.second) {
        "Bronze 4" -> R.drawable.ic_bronze_4
        "Bronze 3" -> R.drawable.ic_bronze_3
        "Bronze 2" -> R.drawable.ic_bronze_2
        "Bronze 1" -> R.drawable.ic_bronze_1
        "Silver 4" -> R.drawable.ic_silver_4
        "Silver 3" -> R.drawable.ic_silver_3
        "Silver 2" -> R.drawable.ic_silver_2
        "Silver 1" -> R.drawable.ic_silver_1
        "Gold 4" -> R.drawable.ic_gold_4
        "Gold 3" -> R.drawable.ic_gold_3
        "Gold 2" -> R.drawable.ic_gold_2
        "Gold 1" -> R.drawable.ic_gold_1
        "Platinum 4" -> R.drawable.ic_platinum_4
        "Platinum 3" -> R.drawable.ic_platinum_3
        "Platinum 2" -> R.drawable.ic_platinum_2
        "Platinum 1" -> R.drawable.ic_platinum_1
        "Diamond 4" -> R.drawable.ic_diamond_4
        "Diamond 3" -> R.drawable.ic_diamond_3
        "Diamond 2" -> R.drawable.ic_diamond_2
        "Diamond 1" -> R.drawable.ic_diamond_1
        "Ruby" -> R.drawable.ic_ruby
        else -> R.drawable.ic_bronze_4
    }
}

internal fun getProgressToNextRank(rs: Int): Triple<Int, Int, String>? {
    val currentTierIndex = RANK_TIERS.indexOfLast { rs >= it.first }
    if (currentTierIndex == -1) return null
    val currentTier = RANK_TIERS[currentTierIndex]
    val nextTier = RANK_TIERS.getOrNull(currentTierIndex + 1) ?: return null

    val range = nextTier.first - currentTier.first
    val progress = rs - currentTier.first
    val percentage = ((progress.toFloat() / range) * 100).roundToInt().coerceIn(0, 100)
    val remaining = nextTier.first - rs
    return Triple(percentage, remaining, nextTier.second)
}

private fun longestStreak(deltas: List<Int>, predicate: (Int) -> Boolean): Int {
    var maxStreak = 0
    var current = 0
    deltas.forEach { d ->
        if (predicate(d)) {
            current += 1
            maxStreak = maxOf(maxStreak, current)
        } else {
            current = 0
        }
    }
    return maxStreak
}

private fun getRankDifficultyMultiplier(currentRank: Int): Double {
    return when {
        currentRank >= 50000 -> 0.25
        currentRank >= 47500 -> 0.40
        currentRank >= 45000 -> 0.50
        currentRank >= 42500 -> 0.60
        currentRank >= 40000 -> 0.70
        currentRank >= 37500 -> 0.80
        currentRank >= 35000 -> 0.85
        currentRank >= 32500 -> 0.90
        currentRank >= 30000 -> 0.95
        else -> 1.5
    }
}

private fun estimateProgressRate(entries: List<RankEntry>, maxWindow: Int = 10): Double? {
    if (entries.size < 2) return null
    val window = entries.takeLast(maxWindow)
    val n = window.size
    val xMean = (n - 1) / 2.0
    val yMean = window.map { it.rank.toDouble() }.average()
    var num = 0.0
    var den = 0.0
    window.forEachIndexed { i, entry ->
        val dx = i - xMean
        num += dx * (entry.rank - yMean)
        den += dx * dx
    }
    return if (den != 0.0) num / den else null
}

private fun calculateStatsByNote(entries: List<RankEntry>): Map<String, Pair<Double?, Double?>> {
    if (entries.size < 2) return emptyMap()
    val noteGains = mutableMapOf<String, MutableList<Int>>()
    val noteLosses = mutableMapOf<String, MutableList<Int>>()

    for (i in 1 until entries.size) {
        val entry = entries[i]
        val prev = entries[i - 1]
        val delta = entry.rank - prev.rank
        entry.notes.forEach { note ->
            if (delta > 0) {
                noteGains.getOrPut(note) { mutableListOf() }.add(delta)
            } else if (delta < 0) {
                noteLosses.getOrPut(note) { mutableListOf() }.add(abs(delta))
            }
        }
    }
    val allNotes = (noteGains.keys + noteLosses.keys).toSet()
    return allNotes.associateWith { note ->
        val avgG = noteGains[note]?.average()
        val avgL = noteLosses[note]?.average()?.let { -it }
        Pair(avgG, avgL)
    }
}

private fun countMatchesByNote(entries: List<RankEntry>): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    entries.forEach { entry ->
        entry.notes.forEach { note ->
            counts[note] = (counts[note] ?: 0) + 1
        }
    }
    return counts
}

private fun averageDeltaByDayOfWeek(entries: List<RankEntry>): List<Double?> {
    val sums = DoubleArray(7)
    val counts = IntArray(7)
    for (i in 1 until entries.size) {
        val delta = (entries[i].rank - entries[i - 1].rank).toDouble()
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = entries[i].timestamp }
        val dayIdx = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
        sums[dayIdx] += delta
        counts[dayIdx] += 1
    }
    return (0 until 7).map { if (counts[it] > 0) sums[it] / counts[it] else null }
}

private fun averageDeltaByHourBucket(entries: List<RankEntry>): List<Double?> {
    val sums = DoubleArray(HOUR_BUCKETS.size)
    val counts = IntArray(HOUR_BUCKETS.size)
    for (i in 1 until entries.size) {
        val delta = (entries[i].rank - entries[i - 1].rank).toDouble()
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = entries[i].timestamp }
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val bucketIdx = HOUR_BUCKETS.indexOfFirst { hour in it }
        if (bucketIdx >= 0) {
            sums[bucketIdx] += delta
            counts[bucketIdx] += 1
        }
    }
    return HOUR_BUCKETS.indices.map { if (counts[it] > 0) sums[it] / counts[it] else null }
}

// Calcule la progression absolue sur une période donnée
private fun getProgressForPeriod(entries: List<RankEntry>, startMs: Long): Int? {
    if (entries.isEmpty()) return null
    val periodEntries = entries.filter { it.timestamp >= startMs }
    if (periodEntries.isEmpty()) return null
    val beforeEntry = entries.lastOrNull { it.timestamp < startMs }
    val startRank = beforeEntry?.rank ?: periodEntries.first().rank
    val endRank = periodEntries.last().rank
    return endRank - startRank
}

// ---------- Stockage local (SharedPreferences) ----------
private const val PREFS_NAME = "finals_rank_tracker"
private const val KEY_SEASONS = "seasons_json_v2"
private const val KEY_ENTRIES_LEGACY = "entries_json"
private const val KEY_DARK_MODE = "dark_mode"
private const val KEY_LANGUAGE = "is_english"
private const val KEY_SELECTED_SEASON = "selected_season"
private const val KEY_RANK_GOALS = "rank_goals_v1"
private const val KEY_LAST_EXPORT_TIMESTAMP = "last_export_timestamp"
private const val KEY_EXPORT_REMINDER_DISMISS_TIMESTAMP = "export_reminder_dismiss_timestamp"
private const val EXPORT_REMINDER_INTERVAL_MS = 14L * 24 * 3600 * 1000

internal fun loadAllSeasons(context: Context): Map<Int, List<RankEntry>> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_SEASONS, null)
    if (json != null) {
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<Int, List<RankEntry>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val season = k.toIntOrNull()
                if (season != null) {
                    val arr = obj.getJSONArray(k)
                    val list = (0 until arr.length()).map { i ->
                        val item = arr.getJSONObject(i)
                        val notes = if (item.has("notes")) {
                            val notesArr = item.getJSONArray("notes")
                            (0 until notesArr.length()).map { notesArr.getString(it) }
                        } else {
                            emptyList()
                        }
                        RankEntry(
                            rank = item.getInt("rank"),
                            timestamp = item.getLong("ts"),
                            notes = notes
                        )
                    }
                    result[season] = list
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    val oldJson = prefs.getString(KEY_ENTRIES_LEGACY, null)
    if (oldJson != null) {
        return try {
            val arr = JSONArray(oldJson)
            val now = System.currentTimeMillis()
            val migrated = (0 until arr.length()).map {
                RankEntry(arr.getInt(it), now, emptyList())
            }
            val map = mapOf(11 to migrated)
            saveAllSeasons(context, map)
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
    return emptyMap()
}

private fun saveAllSeasons(context: Context, seasons: Map<Int, List<RankEntry>>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val obj = JSONObject()
    seasons.forEach { (season, list) ->
        val arr = JSONArray()
        list.forEach { entry ->
            val item = JSONObject()
            item.put("rank", entry.rank)
            item.put("ts", entry.timestamp)
            val notesArr = JSONArray()
            entry.notes.forEach { notesArr.put(it) }
            item.put("notes", notesArr)
            arr.put(item)
        }
        obj.put(season.toString(), arr)
    }
    prefs.edit().putString(KEY_SEASONS, obj.toString()).apply()
}

private fun loadDarkMode(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_DARK_MODE, true)
}

private fun saveDarkMode(context: Context, isDark: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
}

internal fun loadLanguage(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_LANGUAGE, false)
}

private fun saveLanguage(context: Context, isEnglish: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_LANGUAGE, isEnglish).apply()
}

internal fun loadSelectedSeason(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_SELECTED_SEASON, 11)
}

private fun saveSelectedSeason(context: Context, season: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_SELECTED_SEASON, season).apply()
}

private fun loadRankGoals(context: Context): Map<Int, Int> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_RANK_GOALS, null) ?: return emptyMap()
    return try {
        val obj = JSONObject(json)
        val result = mutableMapOf<Int, Int>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val season = k.toIntOrNull()
            if (season != null) result[season] = obj.getInt(k)
        }
        result
    } catch (e: Exception) { emptyMap() }
}

private fun saveRankGoals(context: Context, goals: Map<Int, Int>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val obj = JSONObject()
    goals.forEach { (season, goal) -> obj.put(season.toString(), goal) }
    prefs.edit().putString(KEY_RANK_GOALS, obj.toString()).apply()
}

internal fun formatNum(n: Int?): String {
    if (n == null) return "—"
    return numberFormat.format(n.toLong())
}

private fun loadLastExportTimestamp(context: Context): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getLong(KEY_LAST_EXPORT_TIMESTAMP, 0L)
}

private fun saveLastExportTimestamp(context: Context, ts: Long) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putLong(KEY_LAST_EXPORT_TIMESTAMP, ts).apply()
}

private fun loadExportReminderDismissTimestamp(context: Context): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getLong(KEY_EXPORT_REMINDER_DISMISS_TIMESTAMP, 0L)
}

private fun saveExportReminderDismissTimestamp(context: Context, ts: Long) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putLong(KEY_EXPORT_REMINDER_DISMISS_TIMESTAMP, ts).apply()
}

private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)

private fun formatSigned(n: Int?): String {
    if (n == null) return "—"
    return (if (n >= 0) "+" else "") + formatNum(n)
}

private fun formatDateTime(timestamp: Long, isEnglish: Boolean): String {
    val pattern = if (isEnglish) "MM/dd/yyyy hh:mm a" else "dd/MM/yyyy HH:mm"
    val locale = if (isEnglish) Locale.US else Locale.FRANCE
    val sdf = SimpleDateFormat(pattern, locale)
    return sdf.format(Date(timestamp))
}

private fun buildAllSeasonsJson(allSeasons: Map<Int, List<RankEntry>>): String {
    val obj = JSONObject()
    allSeasons.forEach { (season, list) ->
        val arr = JSONArray()
        list.forEach { entry ->
            val item = JSONObject()
            item.put("rank", entry.rank)
            item.put("ts", entry.timestamp)
            val notesArr = JSONArray()
            entry.notes.forEach { notesArr.put(it) }
            item.put("notes", notesArr)
            arr.put(item)
        }
        obj.put(season.toString(), arr)
    }
    return obj.toString(4)
}

private fun parseAllSeasonsJson(jsonStr: String): Map<Int, List<RankEntry>>? {
    return try {
        val obj = JSONObject(jsonStr)
        val result = mutableMapOf<Int, List<RankEntry>>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val season = k.toIntOrNull()
            if (season != null) {
                val arr = obj.getJSONArray(k)
                val list = (0 until arr.length()).map { i ->
                    val item = arr.getJSONObject(i)
                    val notes = if (item.has("notes")) {
                        val notesArr = item.getJSONArray("notes")
                        (0 until notesArr.length()).map { notesArr.getString(it) }
                    } else {
                        emptyList()
                    }
                    RankEntry(
                        rank = item.getInt("rank"),
                        timestamp = item.getLong("ts"),
                        notes = notes
                    )
                }
                result[season] = list
            }
        }
        result
    } catch (e: Exception) {
        null
    }
}

private fun buildExportText(allSeasons: Map<Int, List<RankEntry>>, isEnglish: Boolean): String {
    val sb = StringBuilder()
    sb.append(if (isEnglish) "THE FINALS - Rank tracker export\n" else "THE FINALS - Export du suivi de rang\n")
    sb.append(if (isEnglish) "Exported on " else "Exporté le ")
    sb.append(formatDateTime(System.currentTimeMillis(), isEnglish))
    sb.append("\n\n")
    allSeasons.toSortedMap().forEach { (season, list) ->
        sb.append("=== ")
        sb.append(if (isEnglish) "Season " else "Saison ")
        sb.append(season)
        sb.append(" ===\n")
        if (list.isEmpty()) {
            sb.append(if (isEnglish) "(no data)\n" else "(aucune donnée)\n")
        } else {
            list.forEachIndexed { idx, entry ->
                val delta = if (idx > 0) entry.rank - list[idx - 1].rank else null
                val deltaStr = if (delta != null) " [" + (if (delta >= 0) "+" else "") + delta + "]" else ""
                val notesStr = if (entry.notes.isNotEmpty()) " {" + entry.notes.joinToString(",") + "}" else ""
                sb.append("Match ")
                sb.append(idx + 1)
                sb.append(" - ")
                sb.append(formatDateTime(entry.timestamp, isEnglish))
                sb.append(" - ")
                sb.append(entry.rank)
                sb.append(" RS")
                sb.append(deltaStr)
                sb.append(notesStr)
                sb.append("\n")
            }
        }
        sb.append("\n")
    }
    return sb.toString()
}

private val SEASON_HEADER_REGEX = Regex("""===\s*(?:Saison|Season)\s+(\d+)\s*===""")
private val MATCH_LINE_REGEX = Regex("""Match\s+\d+\s*-\s*(.+?)\s*-\s*(\d+)\s*RS(?:\s*\[[+-]?\d+\])?(?:\s*\{(.*?)\})?""")

private fun parseImportDateTime(text: String): Long? {
    val candidates = listOf(
        "dd/MM/yyyy HH:mm" to Locale.FRANCE,
        "MM/dd/yyyy hh:mm a" to Locale.US
    )
    for ((pattern, locale) in candidates) {
        try {
            val sdf = SimpleDateFormat(pattern, locale)
            sdf.isLenient = false
            val date = sdf.parse(text.trim())
            if (date != null) return date.time
        } catch (e: Exception) {
            // Suivant
        }
    }
    return null
}

private fun parseImportText(text: String): Map<Int, List<RankEntry>>? {
    val result = mutableMapOf<Int, MutableList<RankEntry>>()
    var currentSeason: Int? = null
    var foundAny = false
    text.lines().forEach { rawLine ->
        val line = rawLine.trim()
        val seasonMatch = SEASON_HEADER_REGEX.find(line)
        if (seasonMatch != null) {
            val season = seasonMatch.groupValues[1].toIntOrNull()
            if (season != null) {
                currentSeason = season
                if (!result.containsKey(season)) {
                    result[season] = mutableListOf()
                }
            }
            return@forEach
        }
        val activeSeason = currentSeason
        if (activeSeason != null) {
            val matchLine = MATCH_LINE_REGEX.find(line)
            if (matchLine != null) {
                val dateStr = matchLine.groupValues[1]
                val rankStr = matchLine.groupValues[2]
                val notesRaw = if (matchLine.groupValues.size > 3) matchLine.groupValues[3] else ""
                val rank = rankStr.toIntOrNull()
                val timestamp = parseImportDateTime(dateStr)
                val notes = if (notesRaw.isNotEmpty()) {
                    notesRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }
                if (rank != null && timestamp != null) {
                    result.getOrPut(activeSeason) { mutableListOf() }.add(RankEntry(rank, timestamp, notes))
                    foundAny = true
                }
            }
        }
    }
    if (!foundAny) return null
    return result.mapValues { it.value.toList() }
}

private val DEFAULT_NOTES_FR = listOf(
    "SoloQ", "DuoQ", "Trio", "Seed 1/2", "Seed 3/5", "Seed 6/8",
    "J'ai bien joué", "J'ai mal joué", "Bons teammates", "Mauvais teammates", "Coup de chance", "Pas de chance", "Niveau trop élevé"
)
private val DEFAULT_NOTES_EN = listOf(
    "SoloQ", "DuoQ", "Trio", "Seed 1/2", "Seed 3/5", "Seed 6/8",
    "I played well", "I played poorly", "Good teammates", "Bad teammates", "Lucky", "Unlucky", "Skill level too high"
)

// ---------- Activité principale ----------
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

    val coroutineScope = rememberCoroutineScope()
    val saveButtonScale = remember { Animatable(1f) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = buildAllSeasonsJson(allSeasons)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(jsonString.toByteArray())
                }
                showImportSuccess = false
                importError = false
                showExportConfirm = true
                saveLastExportTimestamp(context, System.currentTimeMillis())
                showExportReminder = false
            } catch (e: Exception) {
                importError = true
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
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
                }
            } catch (e: Exception) {
                importError = true
                showImportConfirm = false
            }
        }
    }

    LaunchedEffect(showExportConfirm) {
        if (showExportConfirm) {
            delay(3500)
            showExportConfirm = false
        }
    }

    LaunchedEffect(showImportSuccess) {
        if (showImportSuccess) {
            delay(3500)
            showImportSuccess = false
        }
    }

    LaunchedEffect(allSeasons, showSplash) {
        if (!showSplash) {
            val hasAnyData = allSeasons.values.any { it.isNotEmpty() }
            if (hasAnyData) {
                val now = System.currentTimeMillis()
                val lastExport = loadLastExportTimestamp(context)
                val lastDismiss = loadExportReminderDismissTimestamp(context)
                val sinceExport = now - lastExport
                val sinceDismiss = now - lastDismiss
                showExportReminder = sinceExport >= EXPORT_REMINDER_INTERVAL_MS && sinceDismiss >= EXPORT_REMINDER_INTERVAL_MS
            } else {
                showExportReminder = false
            }
        }
    }

    LaunchedEffect(showInvalidScoreMsg) {
        if (showInvalidScoreMsg) {
            delay(3500)
            showInvalidScoreMsg = false
        }
    }

    LaunchedEffect(showTypoErrorMsg) {
        if (showTypoErrorMsg) {
            delay(3500)
            showTypoErrorMsg = false
        }
    }

    LaunchedEffect(showSavedMsg) {
        if (showSavedMsg) {
            delay(3000)
            showSavedMsg = false
        }
    }

    var seasonTransitionAlpha by remember { mutableStateOf(1f) }
    LaunchedEffect(selectedSeason) {
        seasonTransitionAlpha = 0f
        delay(100)
        seasonTransitionAlpha = 1f
    }
    val contentAlpha by animateFloatAsState(
        targetValue = seasonTransitionAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "Content Alpha Anim"
    )

    val themeProgress by animateFloatAsState(
        targetValue = if (isDarkMode) 0f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "Theme transition"
    )
    val palette = lerpPalette(DarkPalette, LightPalette, themeProgress)
    val s = if (isEnglish) EN else FR

    fun resetSelections() {
        selectedIndex = null
        editingIndex = null
        editNotes = emptySet()
        editInvalidScore = false
        deleteConfirmIndex = null
        expandedHistoryIndex = null
        showResetConfirm = false
        showUndoConfirm = false
        showGoalSelector = false
        historyNoteFilter = emptySet()
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
    val winRate = if (totalGainRS + totalLossRS > 0) (totalGainRS.toFloat() / (totalGainRS + totalLossRS) * 100).roundToInt() else null
    val goalValue = rankGoals[selectedSeason]
    val progressRate = estimateProgressRate(currentSeasonEntries)
    val estimatedMatchesToGoal: Int? = if (goalValue != null && currentRank != null && progressRate != null && progressRate > 0 && goalValue > currentRank) {
        val baseRate = progressRate
        val difficultyMultiplier = getRankDifficultyMultiplier(currentRank)
        val targetDifficultyMultiplier = getRankDifficultyMultiplier(goalValue)
        val averageMultiplier = (difficultyMultiplier + targetDifficultyMultiplier) / 2.0
        val adjustedRate = (baseRate * averageMultiplier).coerceAtLeast(1.0)
        kotlin.math.ceil((goalValue - currentRank).toDouble() / adjustedRate).toInt()
    } else null
    val allPoints = currentSeasonEntries.mapIndexed { i, e -> ChartPoint(i, e.rank, e.timestamp) }
    val nowMillis = System.currentTimeMillis()
    val chartPoints = when (chartPeriod) {
        ChartPeriod.WEEK -> allPoints.filter { it.timestamp >= nowMillis - 7L * 24 * 3600 * 1000 }
        ChartPeriod.MONTH -> allPoints.filter { it.timestamp >= nowMillis - 30L * 24 * 3600 * 1000 }
        ChartPeriod.ALL -> allPoints
    }

    val animatedRankScore by animateIntAsState(
        targetValue = currentRank ?: 0,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "Compteur Animé de RS"
    )

    val deltaScale by animateFloatAsState(
        targetValue = if (delta != null && delta >= 0) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "Pulse du gain"
    )

    val streakFlameScale = remember { Animatable(1f) }
    LaunchedEffect(winStreak) {
        if (winStreak >= 2) {
            streakFlameScale.snapTo(1.5f)
            streakFlameScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        } else {
            streakFlameScale.snapTo(1f)
        }
    }
    val streakIntensity = ((winStreak - 2).coerceIn(0, 8)) / 8f
    val streakColor = lerp(Color(0xFFFFA726), palette.red, streakIntensity)
    val flameTransition = rememberInfiniteTransition(label = "flame flicker")
    val flameFlicker by flameTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame flicker scale"
    )
    val flameGlowAlpha by flameTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame glow alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "rank shimmer")
    val offset by infiniteTransition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer offset"
    )

    val rankName = rankNameFor(currentRank ?: 0)
    val shimmerColors = when {
        rankName.contains("Bronze") -> listOf(
            Color(0xFF7A4B28), Color(0xFFCD7F32), Color(0xFFF0C294),
            Color(0xFFCD7F32), Color(0xFF7A4B28)
        )
        rankName.contains("Silver") || rankName.contains("Argent") -> listOf(
            Color(0xFF707B7C), Color(0xFFBDC3C7), Color(0xFFF2F4F4),
            Color(0xFFBDC3C7), Color(0xFF707B7C)
        )
        rankName.contains("Gold") || rankName.contains("Or") -> listOf(
            Color(0xFF9A7B31), Color(0xFFE6C667), Color(0xFFFFF5D1),
            Color(0xFFE6C667), Color(0xFF9A7B31)
        )
        rankName.contains("Platinum") || rankName.contains("Platine") -> listOf(
            Color(0xFF3A506B), Color(0xFF64DFDF), Color(0xFFE0FAFF),
            Color(0xFF64DFDF), Color(0xFF3A506B)
        )
        rankName.contains("Diamond") || rankName.contains("Diamant") -> listOf(
            Color(0xFF480CA8), Color(0xFF4CC9F0), Color(0xFFEAF8FF),
            Color(0xFF4CC9F0), Color(0xFF480CA8)
        )
        rankName.contains("Ruby") || (currentRank ?: 0) >= 50000 -> listOf(
            Color(0xFF800020), Color(0xFFFF0F50), Color(0xFFFFCCD5),
            Color(0xFFFF0F50), Color(0xFF800020)
        )
        else -> listOf(palette.textPrimary, palette.textPrimary, palette.textPrimary)
    }

    val animatedRankBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(offset, 0f),
        end = Offset(offset + 300f, 300f)
    )

    val noteGroups = if (isEnglish) {
        listOf(
            listOf("SoloQ", "DuoQ", "Trio"),
            listOf("Seed 1/2", "Seed 3/5", "Seed 6/8"),
            listOf("Unlucky", "Lucky"),
            listOf("I played poorly", "I played well")
        )
    } else {
        listOf(
            listOf("SoloQ", "DuoQ", "Trio"),
            listOf("Seed 1/2", "Seed 3/5", "Seed 6/8"),
            listOf("Pas de chance", "Coup de chance"),
            listOf("J'ai mal joué", "J'ai bien joué")
        )
    }

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
            // Main App Container (Animated crossfade and slide up)
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
                        // En-tête
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = s.eyebrow, color = palette.accent, fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = s.title, color = palette.textPrimary, fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isDarkMode) s.darkModeLabel else s.lightModeLabel,
                                color = palette.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    isDarkMode = !isDarkMode
                                    saveDarkMode(context, isDarkMode)
                                }.padding(4.dp)
                            )
                        }

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
                                Text(
                                    text = s.exportReminderMessage,
                                    color = palette.textPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = s.exportReminderDismiss,
                                    color = palette.cyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        saveExportReminderDismissTimestamp(context, System.currentTimeMillis())
                                        showExportReminder = false
                                    }.padding(4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sélecteur de Saison + Langue
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "◀ ",
                                    color = if (selectedSeason > 1) palette.accent else palette.border,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .clickable(enabled = selectedSeason > 1) {
                                            selectedSeason -= 1
                                            saveSelectedSeason(context, selectedSeason)
                                            resetSelections()
                                        }
                                        .padding(6.dp)
                                )
                                Text(
                                    text = "${s.season} $selectedSeason",
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = " ▶",
                                    color = palette.accent,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .clickable {
                                            selectedSeason += 1
                                            saveSelectedSeason(context, selectedSeason)
                                            resetSelections()
                                        }
                                        .padding(6.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FR",
                                    color = if (!isEnglish) palette.accent else palette.textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        isEnglish = false
                                        saveLanguage(context, false)
                                    }.padding(4.dp)
                                )
                                Text(text = "/", color = palette.textMuted, fontSize = 12.sp)
                                Text(
                                    text = "EN",
                                    color = if (isEnglish) palette.accent else palette.textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        isEnglish = true
                                        saveLanguage(context, true)
                                    }.padding(4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Carte "rang actuel"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neumorphicCard(palette, isDarkMode, 12.dp)
                                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = s.currentRankLabel,
                                        color = palette.textMuted, fontSize = 15.sp,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatNum(animatedRankScore),
                                        style = TextStyle(
                                            brush = animatedRankBrush,
                                            fontSize = 45.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                    if (delta != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = (if (delta >= 0) "▲ +" else "▼ -") + formatNum(abs(delta)),
                                            color = if (delta >= 0) palette.green else palette.red,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.scale(deltaScale)
                                        )
                                    }
                                    if (winRate != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "${s.winRateLabel}: $winRate%",
                                            color = palette.accent,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (winStreak >= 2) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.scale(streakFlameScale.value * flameFlicker)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Box(
                                                    modifier = Modifier
                                                        .size((22 + streakIntensity * 10).dp)
                                                        .background(
                                                            brush = Brush.radialGradient(
                                                                colors = listOf(streakColor.copy(alpha = flameGlowAlpha), Color.Transparent)
                                                            ),
                                                            shape = CircleShape
                                                        )
                                                )
                                                // Reversion to File 1.43.0 original text-emoji streak
                                                Text(text = "🔥", fontSize = (14 + streakIntensity * 8).sp)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${s.winStreakLabel} x$winStreak",
                                                color = streakColor,
                                                fontSize = (13 + streakIntensity * 3).sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                if (currentRank != null) {
                                    val progressInfo = getProgressToNextRank(currentRank)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = rankLogoResFor(currentRank)),
                                            contentDescription = rankNameFor(currentRank),
                                            modifier = Modifier
                                                .size(130.dp)
                                                .offset(y = (-5).dp)
                                        )
                                        if (progressInfo != null) {
                                            val (percentage, remaining, nextName) = progressInfo
                                            val localizedNextName = getLocalizedRankName(nextName, isEnglish)

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                            ) {
                                                LinearProgressIndicator(
                                                    progress = percentage / 100f,
                                                    modifier = Modifier
                                                        .width(100.dp)
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(4.dp)),
                                                    color = palette.accent,
                                                    trackColor = palette.surfaceAlt
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "$percentage%",
                                                    color = palette.accent,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Text(
                                                text = "${formatNum(remaining)} ${s.rsRemaining} ${s.nextRankPrefix} $localizedNextName",
                                                color = palette.textMuted,
                                                fontSize = 11.sp,
                                                letterSpacing = 1.0.sp,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                            ) {
                                                LinearProgressIndicator(
                                                    progress = 1f,
                                                    modifier = Modifier
                                                        .width(100.dp)
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(4.dp)),
                                                    color = palette.accent,
                                                    trackColor = palette.surfaceAlt
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "100%",
                                                    color = palette.accent,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Text(
                                                text = s.rubyMax,
                                                color = palette.cyan,
                                                fontSize = 11.sp,
                                                letterSpacing = 1.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Carte "objectif de rang"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neumorphicCard(palette, isDarkMode, 12.dp)
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = s.rankGoalTitle,
                                        color = palette.textMuted,
                                        fontSize = 15.sp,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (goalValue != null && !showGoalSelector) {
                                        Row {
                                            Text(
                                                text = "✎",
                                                color = palette.cyan,
                                                fontSize = 13.sp,
                                                modifier = Modifier.clickable { showGoalSelector = true }.padding(4.dp)
                                            )
                                            Text(
                                                text = "✕",
                                                color = palette.red,
                                                fontSize = 13.sp,
                                                modifier = Modifier.clickable {
                                                    val updated = rankGoals.toMutableMap()
                                                    updated.remove(selectedSeason)
                                                    rankGoals = updated
                                                    saveRankGoals(context, updated)
                                                }.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                if (goalValue == null || showGoalSelector) {
                                    val qualifyingTiers = RANK_TIERS.filter { currentRank == null || it.first > currentRank }
                                    Text(
                                        text = s.rankGoalPlaceholder,
                                        color = palette.cyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { showGoalSelector = !showGoalSelector }
                                    )
                                    if (showGoalSelector) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (qualifyingTiers.isEmpty()) {
                                            Text(s.rankGoalAlreadyMax, color = palette.textMuted, fontSize = 12.sp)
                                        } else {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 200.dp)
                                                    .verticalScroll(rememberScrollState())
                                                    .neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt)
                                                    .padding(8.dp)
                                            ) {
                                                qualifyingTiers.forEach { (threshold, name) ->
                                                    Text(
                                                        text = "${getLocalizedRankName(name, isEnglish)} · ${formatNum(threshold)} RS",
                                                        color = palette.textPrimary,
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                val updated = rankGoals.toMutableMap()
                                                                updated[selectedSeason] = threshold
                                                                rankGoals = updated
                                                                saveRankGoals(context, updated)
                                                                showGoalSelector = false
                                                            }
                                                            .padding(vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val goalRankName = getLocalizedRankName(rankNameFor(goalValue), isEnglish)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${formatNum(goalValue)} RS · $goalRankName",
                                                color = palette.textPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            when {
                                                currentRank != null && currentRank >= goalValue -> {
                                                    Text(s.rankGoalReached, color = palette.green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                estimatedMatchesToGoal != null -> {
                                                    Text(
                                                        text = "${s.rankGoalEstimatePrefix}$estimatedMatchesToGoal ${s.rankGoalEstimateSuffix} $goalRankName",
                                                        color = palette.accent,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                else -> {
                                                    Text(s.rankGoalNotEnoughData, color = palette.textMuted, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Image(
                                            painter = painterResource(id = rankLogoResFor(goalValue)),
                                            contentDescription = goalRankName,
                                            modifier = Modifier.size(80.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats principales
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatChip(s.best, formatNum(peakRank), palette.green, palette, Modifier.weight(1f), rankName = peakRankName, isDarkMode = isDarkMode)
                            StatChip(s.worst, formatNum(lowRank), palette.red, palette, Modifier.weight(1f), rankName = lowRankName, isDarkMode = isDarkMode)
                            StatChip(s.matches, currentSeasonEntries.size.toString(), palette.cyan, palette, Modifier.weight(1f), isDarkMode = isDarkMode)
                        }

                        // Statistiques détaillées
                        if (deltas.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showStats) "${s.detailedStats} ▲" else "${s.detailedStats} ▼",
                                    color = palette.cyan,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { showStats = !showStats }
                                )
                                Text(
                                    text = if (compareSeasonsEnabled) "${s.compareSeasonsLabel} ▲" else "${s.compareSeasonsLabel} ▼",
                                    color = palette.cyan,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable {
                                        compareSeasonsEnabled = !compareSeasonsEnabled
                                        if (!compareSeasonsEnabled) compareSeasonId = null
                                    }
                                )
                            }

                            if (compareSeasonsEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val otherSeasonsWithData = allSeasons
                                    .filterKeys { it != selectedSeason }
                                    .filterValues { it.isNotEmpty() }
                                    .keys.sorted()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neumorphicCard(palette, isDarkMode, 10.dp)
                                        .padding(12.dp)
                                ) {
                                    if (otherSeasonsWithData.isEmpty()) {
                                        Text(s.compareSeasonsNoOthers, color = palette.textMuted, fontSize = 12.sp)
                                    } else {
                                        Text(s.compareSeasonsPrompt, color = palette.textMuted, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            otherSeasonsWithData.forEach { season ->
                                                val isSelected = compareSeasonId == season
                                                Text(
                                                    text = "${s.season} $season",
                                                    color = if (isSelected) palette.accent else palette.textPrimary,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier
                                                        .background(if (isSelected) palette.accent.copy(alpha = 0.2f) else palette.surfaceAlt, RoundedCornerShape(16.dp))
                                                        .border(1.dp, if (isSelected) palette.accent else palette.border, RoundedCornerShape(16.dp))
                                                        .clickable { compareSeasonId = if (isSelected) null else season }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
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
                                            SeasonComparisonChart(
                                                seriesA = currentSeasonEntries,
                                                seriesB = compareEntries,
                                                colorA = palette.accent,
                                                colorB = palette.cyan,
                                                palette = palette,
                                                isEnglish = isEnglish
                                            )
                                        }
                                    }
                                }
                            }

                            if (showStats) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neumorphicCard(palette, isDarkMode, 10.dp)
                                        .padding(12.dp)
                                ) {
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

                                    val noteStats = calculateStatsByNote(currentSeasonEntries)
                                    if (noteStats.isNotEmpty()) {
                                        val noteMatchCounts = countMatchesByNote(currentSeasonEntries)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = s.avgStatsByNote,
                                            color = palette.accent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        noteStats.entries
                                            .sortedByDescending { (_, pair) -> (pair.first ?: 0.0) + (pair.second ?: 0.0) }
                                            .forEach { (note, pair) ->
                                                val (avgG, avgL) = pair
                                                val count = noteMatchCounts[note] ?: 0
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(text = " • $note ($count)", color = palette.textMuted, fontSize = 11.sp)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        if (avgG != null) {
                                                            Text(
                                                                text = "+${avgG.roundToInt()}",
                                                                color = palette.green,
                                                                fontSize = 11.sp,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                        if (avgL != null) {
                                                            Text(
                                                                text = "${avgL.roundToInt()}",
                                                                color = palette.red,
                                                                fontSize = 11.sp,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
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
                                        Text(
                                            text = s.performanceByTimeTitle,
                                            color = palette.accent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        // ---------- 4 NOUVELLES STATISTIQUES DE PROGRESSION ----------
                                        val todayStartMs = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.timeInMillis

                                        val weekStartMs = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
                                        val monthStartMs = System.currentTimeMillis() - 30L * 24 * 3600 * 1000

                                        val progressToday = getProgressForPeriod(currentSeasonEntries, todayStartMs)
                                        val progressWeek = getProgressForPeriod(currentSeasonEntries, weekStartMs)
                                        val progressMonth = getProgressForPeriod(currentSeasonEntries, monthStartMs)
                                        val progressSeason = if (currentSeasonEntries.size >= 2) {
                                            currentSeasonEntries.last().rank - currentSeasonEntries.first().rank
                                        } else null

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt)
                                                .padding(10.dp)
                                        ) {
                                            DetailLine(s.progressTodayLabel, progressToday, palette)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            DetailLine(s.progressWeekLabel, progressWeek, palette)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            DetailLine(s.progressMonthLabel, progressMonth, palette)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            DetailLine(s.progressSeasonLabel, progressSeason, palette)
                                        }
                                        // -------------------------------------------------------------

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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Graphique ou état vide
                        if (currentSeasonEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphicCard(palette, isDarkMode, 12.dp)
                                    .padding(24.dp)
                            ) {
                                Text(text = s.emptyState, color = palette.textMuted, fontSize = 14.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    PeriodTab(s.periodWeek, chartPeriod == ChartPeriod.WEEK, palette) {
                                        chartPeriod = ChartPeriod.WEEK
                                        selectedIndex = null
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    PeriodTab(s.periodMonth, chartPeriod == ChartPeriod.MONTH, palette) {
                                        chartPeriod = ChartPeriod.MONTH
                                        selectedIndex = null
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    PeriodTab(s.periodAll, chartPeriod == ChartPeriod.ALL, palette) {
                                        chartPeriod = ChartPeriod.ALL
                                        selectedIndex = null
                                    }
                                }
                                Row {
                                    Text(
                                        text = s.zoomOut, color = palette.cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .clickable { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.5f) }
                                            .padding(6.dp)
                                    )
                                    Text(
                                        text = s.zoomIn, color = palette.cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .clickable { zoomScale = (zoomScale * 1.25f).coerceAtMost(3f) }
                                            .padding(6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphicCard(palette, isDarkMode, 12.dp)
                                    .padding(8.dp)
                            ) {
                                if (chartPoints.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(s.noDataPeriod, color = palette.textMuted, fontSize = 13.sp)
                                    }
                                } else {
                                    CandlestickChart(
                                        points = chartPoints,
                                        palette = palette,
                                        selectedIndex = selectedIndex,
                                        onSelect = { idx -> selectedIndex = idx },
                                        zoomScale = zoomScale,
                                        isEnglish = isEnglish,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            if (selectedIndex != null && chartPoints.isNotEmpty()) {
                                val idx = selectedIndex!!.coerceIn(0, chartPoints.size - 1)
                                val point = chartPoints[idx]
                                val prevDelta = if (idx > 0) point.rank - chartPoints[idx - 1].rank else null
                                val vsLow = if (lowRank != null) point.rank - lowRank else null
                                val vsHigh = if (peakRank != null) point.rank - peakRank else null

                                val correspondingEntry = currentSeasonEntries.getOrNull(point.absoluteIndex)

                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neumorphicCard(palette, isDarkMode, 10.dp, baseColor = palette.surfaceAlt, accentColor = palette.cyan)
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "MATCH ${point.absoluteIndex + 1} · ${formatNum(point.rank)} RS",
                                                color = palette.textPrimary, fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "✕", color = palette.textMuted,
                                                modifier = Modifier.clickable { selectedIndex = null }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${s.recordedAt} ${formatDateTime(point.timestamp, isEnglish)}",
                                            color = palette.textMuted,
                                            fontSize = 11.sp
                                        )

                                        if (correspondingEntry != null && correspondingEntry.notes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.horizontalScroll(rememberScrollState())
                                            ) {
                                                correspondingEntry.notes.forEach { note ->
                                                    Text(
                                                        text = note,
                                                        color = palette.accent,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .neumorphicCard(palette, isDarkMode, 6.dp, baseColor = palette.surface, accentColor = palette.accent.copy(alpha = 0.6f))
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Saisie du Score
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputValue,
                                onValueChange = { new -> inputValue = new.filter { it.isDigit() } },
                                placeholder = { Text(if (currentSeasonEntries.isEmpty()) s.startingRankPlaceholder else s.newRankPlaceholder) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val parsed = inputValue.toIntOrNull()
                                    if (parsed != null) {
                                        val lastRank = currentSeasonEntries.lastOrNull()?.rank
                                        if (parsed > 85000) {
                                            showInvalidScoreMsg = true
                                            showTypoErrorMsg = false
                                            showSavedMsg = false
                                        } else if (lastRank != null && abs(parsed - lastRank) > 3500) {
                                            showTypoErrorMsg = true
                                            showInvalidScoreMsg = false
                                            showSavedMsg = false
                                        } else {
                                            val newEntry = RankEntry(
                                                rank = parsed,
                                                timestamp = System.currentTimeMillis(),
                                                notes = selectedNotes.toList()
                                            )
                                            persistSeason(currentSeasonEntries + newEntry)
                                            inputValue = ""
                                            selectedNotes = emptySet()
                                            showInvalidScoreMsg = false
                                            showTypoErrorMsg = false
                                            showSavedMsg = true
                                            coroutineScope.launch {
                                                saveButtonScale.snapTo(0.85f)
                                                saveButtonScale.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                            }
                                        }
                                    }
                                },
                                enabled = inputValue.toIntOrNull() != null,
                                modifier = Modifier.scale(saveButtonScale.value)
                            ) {
                                Text(if (currentSeasonEntries.isEmpty()) s.startButton else s.saveButton, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (showInvalidScoreMsg) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = s.invalidScoreMsg,
                                color = palette.red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (showTypoErrorMsg) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = s.typoDetectedMsg,
                                color = palette.red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (showSavedMsg) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = s.rankSavedMsg,
                                color = palette.green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (inputValue.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = s.selectNotes,
                                color = palette.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val defaultNotes = if (isEnglish) DEFAULT_NOTES_EN else DEFAULT_NOTES_FR
                            NoteChipsSelector(
                                notes = defaultNotes,
                                selected = selectedNotes,
                                noteGroups = noteGroups,
                                palette = palette
                            ) { note ->
                                val isSelected = selectedNotes.contains(note)
                                selectedNotes = if (isSelected) {
                                    selectedNotes - note
                                } else {
                                    val group = noteGroups.find { it.contains(note) }
                                    if (group != null) selectedNotes.filter { !group.contains(it) }.toSet() + note
                                    else selectedNotes + note
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neumorphicCard(palette, isDarkMode, 10.dp)
                                .clickable { uriHandler.openUri("https://www.davg25.com/app/the-finals-leaderboard-tracker/") }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = s.top10kLabel,
                                color = palette.cyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neumorphicCard(palette, isDarkMode, 10.dp)
                                .clickable { uriHandler.openUri("https://www.reachthefinals.com/patchnotes") }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = s.patchNotesLabel,
                                color = palette.cyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions secondaires
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { showUndoConfirm = true },
                                enabled = currentSeasonEntries.isNotEmpty()
                            ) {
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt, accentColor = palette.red)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.undoConfirmMsg, color = palette.textPrimary, fontSize = 13.sp)
                                Row {
                                    TextButton(onClick = { persistSeason(currentSeasonEntries.dropLast(1)) }) {
                                        Text(s.confirmWord, color = palette.red, fontWeight = FontWeight.SemiBold)
                                    }
                                    TextButton(onClick = { showUndoConfirm = false }) {
                                        Text(s.cancelWord, color = palette.textMuted)
                                    }
                                }
                            }
                        }
                        if (showHistory && currentSeasonEntries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = s.sortLabel,
                                    color = if (showHistorySortMenu) palette.accent else palette.cyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .neumorphicCard(palette, isDarkMode, 8.dp)
                                        .clickable { showHistorySortMenu = !showHistorySortMenu; showHistoryFilterMenu = false }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                                Text(
                                    text = s.filterLabel,
                                    color = if (showHistoryFilterMenu || historyNoteFilter.isNotEmpty()) palette.accent else palette.cyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .neumorphicCard(palette, isDarkMode, 8.dp)
                                        .clickable { showHistoryFilterMenu = !showHistoryFilterMenu; showHistorySortMenu = false }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            if (showHistorySortMenu) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt)
                                        .padding(8.dp)
                                ) {
                                    val sortOptions = listOf(
                                        HistorySortMode.OLDEST_FIRST to s.sortOldestFirst,
                                        HistorySortMode.NEWEST_FIRST to s.sortNewestFirst,
                                        HistorySortMode.GAIN_ASC to s.sortGainAsc,
                                        HistorySortMode.GAIN_DESC to s.sortGainDesc
                                    )
                                    sortOptions.forEach { (mode, label) ->
                                        Text(
                                            text = label,
                                            color = if (historySortMode == mode) palette.accent else palette.textPrimary,
                                            fontWeight = if (historySortMode == mode) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { historySortMode = mode; showHistorySortMenu = false }
                                                .padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            if (showHistoryFilterMenu) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val availableFilterNotes = currentSeasonEntries.flatMap { it.notes }.distinct()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = s.filterAll,
                                        color = if (historyNoteFilter.isEmpty()) palette.accent else palette.textPrimary,
                                        fontWeight = if (historyNoteFilter.isEmpty()) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .background(if (historyNoteFilter.isEmpty()) palette.accent.copy(alpha = 0.2f) else palette.surface, RoundedCornerShape(16.dp))
                                            .border(1.dp, if (historyNoteFilter.isEmpty()) palette.accent else palette.border, RoundedCornerShape(16.dp))
                                            .clickable { historyNoteFilter = emptySet() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                    availableFilterNotes.forEach { note ->
                                        val isSelected = historyNoteFilter.contains(note)
                                        Text(
                                            text = note,
                                            color = if (isSelected) palette.accent else palette.textPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .background(if (isSelected) palette.accent.copy(alpha = 0.2f) else palette.surface, RoundedCornerShape(16.dp))
                                                .border(1.dp, if (isSelected) palette.accent else palette.border, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    historyNoteFilter = if (isSelected) historyNoteFilter - note else historyNoteFilter + note
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val historyScrollState = rememberScrollState()
                            LaunchedEffect(showHistory, currentSeasonEntries.size, historySortMode) {
                                if (showHistory) {
                                    if (historySortMode == HistorySortMode.NEWEST_FIRST) {
                                        historyScrollState.scrollTo(0)
                                    } else {
                                        historyScrollState.scrollTo(historyScrollState.maxValue)
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(historyScrollState)
                                    .imePadding()
                                    .neumorphicCard(palette, isDarkMode, 8.dp)
                                    .padding(8.dp)
                            ) {
                                val indexedEntries = currentSeasonEntries.mapIndexed { i, e -> i to e }
                                val filteredEntries = if (historyNoteFilter.isNotEmpty()) {
                                    indexedEntries.filter { (_, e) -> e.notes.any { historyNoteFilter.contains(it) } }
                                } else indexedEntries
                                val sortedEntries = when (historySortMode) {
                                    HistorySortMode.OLDEST_FIRST -> filteredEntries
                                    HistorySortMode.NEWEST_FIRST -> filteredEntries.reversed()
                                    HistorySortMode.GAIN_ASC -> filteredEntries.sortedBy { (i, e) -> if (i > 0) e.rank - currentSeasonEntries[i - 1].rank else 0 }
                                    HistorySortMode.GAIN_DESC -> filteredEntries.sortedByDescending { (i, e) -> if (i > 0) e.rank - currentSeasonEntries[i - 1].rank else 0 }
                                }
                                if (sortedEntries.isEmpty()) {
                                    Text(
                                        text = s.noMatchForFilter,
                                        color = palette.textMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                sortedEntries.forEach { (idx, entry) ->
                                    when {
                                        editingIndex == idx -> {
                                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        "M${idx + 1}", color = palette.textMuted,
                                                        fontSize = 12.sp, fontFamily = FontFamily.Monospace
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    OutlinedTextField(
                                                        value = editValue,
                                                        onValueChange = { new -> editValue = new.filter { it.isDigit() } },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f).height(52.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "✓", color = palette.green, fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.clickable {
                                                            val parsed = editValue.toIntOrNull()
                                                            if (parsed != null) {
                                                                if (parsed > 85000) {
                                                                    editInvalidScore = true
                                                                } else {
                                                                    val next = currentSeasonEntries.toMutableList()
                                                                    next[idx] = next[idx].copy(rank = parsed, notes = editNotes.toList())
                                                                    persistSeason(next)
                                                                }
                                                            } else {
                                                                editingIndex = null
                                                            }
                                                        }.padding(6.dp)
                                                    )
                                                    Text(
                                                        text = "✕", color = palette.textMuted,
                                                        modifier = Modifier.clickable { editingIndex = null; editInvalidScore = false }.padding(6.dp)
                                                    )
                                                }
                                                if (editInvalidScore) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = s.invalidScoreMsg,
                                                        color = palette.red,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val defaultNotes = if (isEnglish) DEFAULT_NOTES_EN else DEFAULT_NOTES_FR
                                                NoteChipsSelector(
                                                    notes = defaultNotes,
                                                    selected = editNotes,
                                                    noteGroups = noteGroups,
                                                    palette = palette,
                                                    chipFontSize = 10.sp
                                                ) { note ->
                                                    val isSelected = editNotes.contains(note)
                                                    editNotes = if (isSelected) {
                                                        editNotes - note
                                                    } else {
                                                        val group = noteGroups.find { it.contains(note) }
                                                        if (group != null) editNotes.filter { !group.contains(it) }.toSet() + note
                                                        else editNotes + note
                                                    }
                                                }
                                            }
                                        }
                                        deleteConfirmIndex == idx -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(s.deleteConfirm, color = palette.textPrimary, fontSize = 12.sp)
                                                Row {
                                                    Text(
                                                        text = s.confirmWord, color = palette.red, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.clickable {
                                                            persistSeason(currentSeasonEntries.filterIndexed { i, _ -> i != idx })
                                                        }.padding(6.dp)
                                                    )
                                                    Text(
                                                        text = s.cancelWord, color = palette.textMuted, fontSize = 12.sp,
                                                        modifier = Modifier.clickable { deleteConfirmIndex = null }.padding(6.dp)
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            val entryDelta = if (idx > 0) entry.rank - currentSeasonEntries[idx - 1].rank else null
                                            val isExpanded = expandedHistoryIndex == idx
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        expandedHistoryIndex = if (isExpanded) null else idx
                                                    }
                                                    .padding(vertical = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "Match ${idx + 1}", color = palette.textMuted,
                                                        fontSize = 12.sp, fontFamily = FontFamily.Monospace
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            formatNum(entry.rank) + " RS", color = palette.textPrimary,
                                                            fontSize = 13.sp, fontFamily = FontFamily.Monospace
                                                        )
                                                        if (entryDelta != null) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = (if (entryDelta >= 0) "▲+" else "▼-") + formatNum(abs(entryDelta)),
                                                                color = if (entryDelta >= 0) palette.green else palette.red,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                    Row {
                                                        Text(
                                                            text = "✎", color = palette.cyan, fontSize = 13.sp,
                                                            modifier = Modifier.clickable {
                                                                editingIndex = idx
                                                                editValue = entry.rank.toString()
                                                                editNotes = entry.notes.toSet()
                                                                editInvalidScore = false
                                                            }.padding(horizontal = 6.dp)
                                                        )
                                                        Text(
                                                            text = "✕", fontSize = 14.sp, color = palette.red,
                                                            modifier = Modifier.clickable { deleteConfirmIndex = idx
                                                            }.padding(horizontal = 6.dp)
                                                        )
                                                    }
                                                }
                                                if (entry.notes.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier
                                                            .padding(start = 8.dp)
                                                            .horizontalScroll(rememberScrollState())
                                                    ) {
                                                        entry.notes.forEach { note ->
                                                            Text(
                                                                text = note,
                                                                color = palette.textMuted,
                                                                fontSize = 9.sp,
                                                                modifier = Modifier
                                                                    .background(palette.surfaceAlt, RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                if (isExpanded) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    val vsLow = if (lowRank != null) entry.rank - lowRank else null
                                                    val vsHigh = if (peakRank != null) entry.rank - peakRank else null
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt, accentColor = palette.cyan)
                                                            .padding(10.dp)
                                                    ) {
                                                        Text(
                                                            text = "${s.recordedAt} ${formatDateTime(entry.timestamp, isEnglish)}",
                                                            color = palette.textMuted,
                                                            fontSize = 11.sp
                                                        )
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

                        Spacer(modifier = Modifier.height(8.dp))

                        if (currentSeasonEntries.isNotEmpty()) {
                            if (!showResetConfirm) {
                                TextButton(onClick = { showResetConfirm = true }) {
                                    Text(s.resetAll, color = Color.Red, fontSize = 12.sp)
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neumorphicCard(palette, isDarkMode, 8.dp, baseColor = palette.surfaceAlt, accentColor = palette.red)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(s.confirmResetAll, color = palette.textPrimary, fontSize = 13.sp)
                                    Row {
                                        TextButton(onClick = { persistSeason(emptyList()); showHistory = false }) {
                                            Text(s.confirmWord, color = palette.red, fontWeight = FontWeight.SemiBold)
                                        }
                                        TextButton(onClick = { showResetConfirm = false }) {
                                            Text(s.cancelWord, color = palette.textMuted)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Export / Import des données avec menus
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { exportMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(s.exportButton, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                DropdownMenu(
                                    expanded = exportMenuExpanded,
                                    onDismissRequest = { exportMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (isEnglish) "To Clipboard" else "Presse-papiers") },
                                        onClick = {
                                            exportMenuExpanded = false
                                            clipboardManager.setText(AnnotatedString(buildExportText(allSeasons, isEnglish)))
                                            showExportConfirm = true
                                            importError = false
                                            showImportConfirm = false
                                            pendingImport = null
                                            saveLastExportTimestamp(context, System.currentTimeMillis())
                                            showExportReminder = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isEnglish) "As .json File" else "Fichier .json") },
                                        onClick = {
                                            exportMenuExpanded = false
                                            exportJsonLauncher.launch("finals_rank_tracker_export.json")
                                        }
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { importMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(s.importButton, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                DropdownMenu(
                                    expanded = importMenuExpanded,
                                    onDismissRequest = { importMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (isEnglish) "From Clipboard" else "Presse-papiers") },
                                        onClick = {
                                            importMenuExpanded = false
                                            val clipText = clipboardManager.getText()?.text
                                            val parsed = if (clipText != null) parseImportText(clipText) else null
                                            showExportConfirm = false
                                            if (parsed != null) {
                                                pendingImport = parsed
                                                showImportConfirm = true
                                                importError = false
                                            } else {
                                                importError = true
                                                showImportConfirm = false
                                                pendingImport = null
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isEnglish) "From .json File" else "Fichier .json") },
                                        onClick = {
                                            importMenuExpanded = false
                                            importJsonLauncher.launch(arrayOf("*/*"))
                                        }
                                    )
                                }
                            }
                        }

                        if (showExportConfirm) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = s.exportedToClipboard,
                                color = palette.green,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (importError) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = s.importErrorMsg,
                                color = palette.red,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (showImportConfirm && pendingImport != null) {
                            val importData = pendingImport!!
                            val seasonCount = importData.size
                            val totalMatches = importData.values.sumOf { it.size }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neumorphicCard(palette, isDarkMode, 10.dp, baseColor = palette.surfaceAlt, accentColor = palette.accent)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "${s.importFoundPrefix} $seasonCount ${s.importSeasonsWord}, $totalMatches ${s.importMatchesWord}",
                                    color = palette.textPrimary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(s.importConfirmQuestion, color = palette.textMuted, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = s.confirmWord,
                                        color = palette.accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.clickable {
                                            allSeasons = importData
                                            saveAllSeasons(context, importData)
                                            resetSelections()
                                            showImportConfirm = false
                                            pendingImport = null
                                            showImportSuccess = true
                                        }.padding(6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = s.cancelWord,
                                        color = palette.textMuted,
                                        fontSize = 13.sp,
                                        modifier = Modifier.clickable {
                                            showImportConfirm = false
                                            pendingImport = null
                                        }.padding(6.dp)
                                    )
                                }
                            }
                        }

                        if (showImportSuccess) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = s.importSuccessMsg,
                                color = palette.green,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = s.footer,
                            color = palette.textMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    uriHandler.openUri("https://discord.gg/ZjnAMKnc")
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }

            // High Fidelity Game-Show Splash Screen (Nettoyé, compact et ultra-rebondissant)
            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(200)
                )
            ) {
                SplashUI(
                    palette = palette,
                    isEnglish = isEnglish,
                    currentRank = currentRank,
                    animatedRankBrush = animatedRankBrush,
                    onDismiss = { showSplash = false }
                )
            }
        }
    }
}

// ---------- Composants du Splash Screen ----------

@Composable
private fun SplashUI(
    palette: Palette,
    isEnglish: Boolean,
    currentRank: Int?,
    animatedRankBrush: Brush,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        // Cascade temporelle des composants
        var finalsAlpha by remember { mutableStateOf(0f) }
        var finalsScale by remember { mutableStateOf(0.8f) }
        var counterAlpha by remember { mutableStateOf(0f) }
        var counterOffsetY by remember { mutableStateOf(40.dp) }
        var logoScale by remember { mutableStateOf(0f) }
        var nameAlpha by remember { mutableStateOf(0f) }

        // Animation de descente physique du logo (effet de chute lourde + rebonds)
        var logoOffsetY by remember { mutableStateOf((-100).dp) }

        val animFinalsAlpha by animateFloatAsState(targetValue = finalsAlpha, animationSpec = tween(400))
        val animFinalsScale by animateFloatAsState(targetValue = finalsScale, animationSpec = tween(400, easing = FastOutSlowInEasing))
        val animCounterAlpha by animateFloatAsState(targetValue = counterAlpha, animationSpec = tween(400))
        val animCounterOffsetY by animateDpAsState(targetValue = counterOffsetY, animationSpec = tween(400, easing = FastOutSlowInEasing))

        // Double ressort élastique prononcé sur le logo (Taille + Chute)
        val animLogoScale by animateFloatAsState(
            targetValue = logoScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        val animLogoOffsetY by animateDpAsState(
            targetValue = logoOffsetY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        val animNameAlpha by animateFloatAsState(targetValue = nameAlpha, animationSpec = tween(400))

        // Compteur "Slot Machine" pour le Rang
        var isRolling by remember { mutableStateOf(true) }
        var rollValue by remember { mutableStateOf(0) }
        var targetRank by remember { mutableStateOf(0) }
        val animatedSplashRank by animateIntAsState(
            targetValue = targetRank,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            label = "Splash Rank Anim"
        )

        LaunchedEffect(Unit) {
            // Lancement des paliers
            finalsAlpha = 1f
            finalsScale = 1f

            delay(300)
            counterAlpha = 1f
            counterOffsetY = 0.dp

            delay(300)
            logoScale = 1.0f
            logoOffsetY = 0.dp // Pop & chute élastique

            delay(300)
            nameAlpha = 1f
        }

        LaunchedEffect(Unit) {
            val finalValue = currentRank ?: 0
            if (finalValue > 0) {
                val rollStartTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - rollStartTime < 800) {
                    rollValue = (1000..55000).random()
                    delay(40)
                }
                isRolling = false
                targetRank = finalValue
            } else {
                isRolling = false
            }
        }

        // Fermeture automatique après 3 secondes
        LaunchedEffect(Unit) {
            delay(3000)
            onDismiss()
        }

        val displayValue = if (isRolling) rollValue else animatedSplashRank

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Titre "THE FINALS" géant, compact et épais
            Text(
                text = "THE FINALS",
                color = palette.accent,
                style = TextStyle(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-3).sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .alpha(animFinalsAlpha)
                    .scale(animFinalsScale)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Compteur Slot Machine
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(animCounterAlpha)
                    .offset(y = animCounterOffsetY)
            ) {
                Text(
                    text = formatNum(displayValue),
                    style = TextStyle(
                        brush = animatedRankBrush,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            if (currentRank != null) {
                Spacer(modifier = Modifier.height(32.dp))

                // Logo du rang avec double ressort physique
                Box(
                    modifier = Modifier
                        .scale(animLogoScale)
                        .offset(y = animLogoOffsetY)
                ) {
                    Image(
                        painter = painterResource(id = rankLogoResFor(currentRank)),
                        contentDescription = rankNameFor(currentRank),
                        modifier = Modifier.size(200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nom du rang
                Text(
                    text = getLocalizedRankName(rankNameFor(currentRank), isEnglish),
                    color = palette.cyan,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alpha(animNameAlpha)
                )
            }
        }
    }
}

// ---------- Fin des composants du Splash Screen ----------

@Composable
private fun StatChip(label: String, value: String, valueColor: Color, palette: Palette, modifier: Modifier = Modifier, rankName: String? = null, isDarkMode: Boolean = true) {
    Column(
        modifier = modifier
            .neumorphicCard(palette, isDarkMode, 8.dp)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label, color = palette.textMuted, fontSize = 13.sp, letterSpacing = 0.5.sp, fontFamily = FontFamily.Monospace,
            lineHeight = 13.sp
        )
        if (rankName != null) {
            Text(
                rankName, color = palette.textMuted, fontSize = 13.sp, letterSpacing = 0.5.sp, fontFamily = FontFamily.Monospace,
                lineHeight = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(0.5.dp))
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun DetailLine(label: String, value: Int?, palette: Palette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = palette.textMuted, fontSize = 12.sp)
        Text(
            text = formatSigned(value),
            color = if (value == null) palette.textMuted else if (value >= 0) palette.green else palette.red,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DetailLineText(label: String, value: String, valueColor: Color, palette: Palette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = palette.textMuted, fontSize = 12.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PeriodTab(label: String, selected: Boolean, palette: Palette, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) palette.accentOn else palette.textMuted,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .background(if (selected) palette.accent else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun NoteChipsSelector(
    notes: List<String>,
    selected: Set<String>,
    noteGroups: List<List<String>>,
    palette: Palette,
    chipFontSize: TextUnit = 11.sp,
    onToggle: (String) -> Unit
) {
    val row1 = notes.take(6)
    val row2 = notes.drop(6)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row1.forEach { note ->
                NoteChip(note, selected.contains(note), palette, chipFontSize) { onToggle(note) }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row2.forEach { note ->
                NoteChip(note, selected.contains(note), palette, chipFontSize) { onToggle(note) }
            }
        }
    }
}

@Composable
private fun NoteChip(note: String, isSelected: Boolean, palette: Palette, fontSize: TextUnit, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) palette.accent.copy(alpha = 0.2f) else palette.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) palette.accent else palette.border,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = note,
            color = if (isSelected) palette.accent else palette.textPrimary,
            fontSize = fontSize,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CandlestickChart(
    points: List<ChartPoint>, palette: Palette,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    zoomScale: Float,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val minV = points.minOf { it.rank }.toFloat()
    val maxV = points.maxOf { it.rank }.toFloat()
    val spread = maxV - minV
    val pad = if (spread == 0f) maxOf(minV * 0.05f, 200f) else maxOf(spread * 0.25f, 100f)
    val domainMin = (minV - pad).coerceAtLeast(0f)
    val domainMax = maxV + pad
    val domainSpan = (domainMax - domainMin).coerceAtLeast(1f)

    val visibleTierNames = RANK_TIERS.filter { it.first.toFloat() in domainMin..domainMax }
        .map { getLocalizedRankName(it.second, isEnglish) }
    val density = LocalDensity.current
    val maxLabelWidthPx = visibleTierNames.maxOfOrNull { name ->
        android.graphics.Paint().apply {
            textSize = with(density) { 12.sp.toPx() }
            isAntiAlias = true
        }.measureText(name)
    } ?: 0f
    val leftMarginDp = with(density) { maxLabelWidthPx.toDp() + 5.dp }.coerceIn(30.dp, 140.dp)
    val labelSpaceDp = 16.dp

    BoxWithConstraints(modifier = modifier) {
        val availableWidth = this.maxWidth - leftMarginDp
        val baseSlot = availableWidth / 20f
        val slotDp = baseSlot * zoomScale

        val contentWidth = maxOf(this.maxWidth, slotDp * maxOf(points.size, 20) + leftMarginDp)
        val scrollState = rememberScrollState()
        LaunchedEffect(points.size) {
            scrollState.scrollTo(scrollState.maxValue)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val leftMarginPx = leftMarginDp.toPx()
                val labelSpacePx = labelSpaceDp.toPx()
                val chartH = size.height - labelSpacePx

                fun yFor(value: Float): Float {
                    val t = (value - domainMin) / domainSpan
                    return chartH - (t * chartH)
                }

                fun drawYLabel(text: String, x: Float, y: Float, color: Color) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            this.color = color.toArgb()
                            textSize = 12.sp.toPx()
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                        canvas.nativeCanvas.drawText(text, x, y, paint)
                    }
                }

                val visibleTiers = RANK_TIERS.filter { it.first.toFloat() in domainMin..domainMax }

                if (visibleTiers.isNotEmpty()) {
                    val minLabelGapPx = 18.dp.toPx()
                    var lastLabelY = Float.POSITIVE_INFINITY
                    visibleTiers.forEach { tier ->
                        val gy = yFor(tier.first.toFloat())
                        drawLine(
                            color = palette.textMuted.copy(alpha = 0.5f),
                            start = Offset(leftMarginPx, gy),
                            end = Offset(size.width, gy),
                            strokeWidth = 2f
                        )
                        if (lastLabelY - gy >= minLabelGapPx) {
                            val labelText = getLocalizedRankName(tier.second, isEnglish)
                            drawYLabel(
                                text = labelText,
                                x = leftMarginPx - 6.dp.toPx(),
                                y = (gy + 4f).coerceIn(10f, chartH),
                                color = palette.textMuted
                            )
                            lastLabelY = gy
                        }
                    }
                } else {
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val gy = chartH / gridLines * i
                        drawLine(
                            color = palette.textMuted.copy(alpha = 0.15f),
                            start = Offset(leftMarginPx, gy),
                            end = Offset(size.width, gy),
                            strokeWidth = 2f
                        )
                        val value = domainMax - (domainSpan / gridLines) * i
                        drawYLabel(
                            text = numberFormat.format(value.toLong()),
                            x = leftMarginPx - 6.dp.toPx(),
                            y = (gy + 4f).coerceIn(10f, chartH),
                            color = palette.textMuted
                        )
                    }
                }

                drawLine(
                    color = palette.border,
                    start = Offset(leftMarginPx, 0f),
                    end = Offset(leftMarginPx, chartH),
                    strokeWidth = 2f
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = leftMarginDp)
                    .horizontalScroll(scrollState)
            ) {
                val scrollableWidth = slotDp * points.size
                Canvas(
                    modifier = Modifier
                        .width(scrollableWidth)
                        .fillMaxHeight()
                        .pointerInput(points.size, slotDp) {
                            detectTapGestures { offset ->
                                val slotPx = slotDp.toPx()
                                val idx = (offset.x / slotPx).toInt().coerceIn(0, points.size - 1)
                                onSelect(idx)
                            }
                        }
                ) {
                    val labelSpacePx = labelSpaceDp.toPx()
                    val chartH = size.height - labelSpacePx
                    val n = points.size
                    val slot = size.width / n
                    val minLabelSpacingPx = 26.dp.toPx()
                    val labelInterval = maxOf(1, (minLabelSpacingPx / slot).toInt())
                    val candleWidth = (slot * 0.5f).coerceAtMost(40.dp.toPx()).coerceAtLeast(4f)
                    fun yFor(value: Float): Float {
                        val t = (value - domainMin) / domainSpan
                        return chartH - (t * chartH)
                    }

                    fun drawXLabel(text: String, x: Float, y: Float, color: Color) {
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                this.color = color.toArgb()
                                textSize = 9.sp.toPx()
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            canvas.nativeCanvas.drawText(text, x, y, paint)
                        }
                    }

                    points.forEachIndexed { i, point ->
                        val cx = slot * i + slot / 2f
                        val isSelected = selectedIndex == i
                        val rank = point.rank
                        val yTop: Float
                        val yBottom: Float
                        val fillColor: Color

                        if (i == 0) {
                            val markerHeight = domainSpan * 0.015f
                            yTop = yFor(rank + markerHeight / 2f)
                            yBottom = yFor(rank - markerHeight / 2f)
                            fillColor = palette.cyan
                        } else {
                            val prev = points[i - 1].rank
                            yTop = yFor(maxOf(prev, rank).toFloat())
                            yBottom = yFor(minOf(prev, rank).toFloat())
                            fillColor = when {
                                rank > prev -> palette.green
                                rank < prev -> palette.red
                                else -> palette.textMuted
                            }
                        }

                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(cx - candleWidth / 2f, yTop),
                            size = Size(candleWidth, (yBottom - yTop).coerceAtLeast(3f)),
                            cornerRadius = CornerRadius(3f, 3f)
                        )

                        if (isSelected) {
                            drawRoundRect(
                                color = palette.cyan,
                                topLeft = Offset(cx - candleWidth / 2f - 3f, yTop - 3f),
                                size = Size(candleWidth + 6f, (yBottom - yTop + 6f).coerceAtLeast(3f)),
                                cornerRadius = CornerRadius(5f, 5f),
                                style = Stroke(width = 3f)
                            )
                        }

                        if (i % labelInterval == 0) {
                            drawXLabel(
                                text = "${point.absoluteIndex + 1}",
                                x = cx,
                                y = size.height - 3f,
                                color = palette.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePerformanceBarChart(
    labels: List<String>,
    values: List<Double?>,
    palette: Palette,
    modifier: Modifier = Modifier
) {
    val maxAbs = (values.filterNotNull().maxOfOrNull { abs(it) } ?: 0.0).let { if (it <= 0.0) 1.0 else it }
    val barAreaHeight = 32.dp
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val value = values.getOrNull(i)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zone des gains
                Box(
                    modifier = Modifier.fillMaxWidth().height(barAreaHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (value != null && value > 0) {
                        val h = barAreaHeight * (value / maxAbs).toFloat().coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(h)
                                .background(palette.green, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.border))
                // Zone des pertes
                Box(
                    modifier = Modifier.fillMaxWidth().height(barAreaHeight),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (value != null && value < 0) {
                        val h = barAreaHeight * (abs(value) / maxAbs).toFloat().coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(h)
                                .background(palette.red, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = label, color = palette.textMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = if (value != null) formatSigned(value.roundToInt()) else "—",
                    color = when {
                        value == null -> palette.textMuted
                        value >= 0 -> palette.green
                        else -> palette.red
                    },
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun SeasonComparisonChart(
    seriesA: List<RankEntry>,
    seriesB: List<RankEntry>,
    colorA: Color,
    colorB: Color,
    palette: Palette,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    if (seriesA.isEmpty() && seriesB.isEmpty()) return
    val allValues = seriesA.map { it.rank } + seriesB.map { it.rank }
    val minV = (allValues.minOrNull() ?: 0).toFloat()
    val maxV = (allValues.maxOrNull() ?: 1).toFloat()
    val pad = ((maxV - minV) * 0.12f).coerceAtLeast(50f)
    val domainMin = (minV - pad).coerceAtLeast(0f)
    val domainMax = maxV + pad
    val span = (domainMax - domainMin).coerceAtLeast(1f)
    val maxCount = maxOf(seriesA.size, seriesB.size, 1)

    val visibleTierNames = RANK_TIERS.filter { it.first.toFloat() in domainMin..domainMax }
        .map { getLocalizedRankName(it.second, isEnglish) }
    val density = LocalDensity.current
    val maxLabelWidthPx = visibleTierNames.maxOfOrNull { name ->
        android.graphics.Paint().apply {
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }.measureText(name)
    } ?: 0f
    val leftMarginDp = with(density) { maxLabelWidthPx.toDp() + 12.dp }.coerceIn(40.dp, 120.dp)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        val leftMarginPx = leftMarginDp.toPx()
        val bottomMarginPx = 18.dp.toPx()
        val chartW = size.width - leftMarginPx
        val chartH = size.height - bottomMarginPx

        fun yFor(value: Float) = chartH - ((value - domainMin) / span) * chartH
        fun xFor(i: Int) = if (maxCount > 1) leftMarginPx + chartW * i / (maxCount - 1).toFloat() else leftMarginPx

        fun drawYLabel(text: String, y: Float) {
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = palette.textMuted.toArgb()
                    textSize = 10.sp.toPx()
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                canvas.nativeCanvas.drawText(text, leftMarginPx - 6.dp.toPx(), (y + 4f).coerceIn(10f, chartH), paint)
            }
        }

        fun drawXLabel(text: String, x: Float) {
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = palette.textMuted.toArgb()
                    textSize = 9.sp.toPx()
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(text, x, size.height - 2f, paint)
            }
        }

        val visibleTiers = RANK_TIERS.filter { it.first.toFloat() in domainMin..domainMax }
        if (visibleTiers.isNotEmpty()) {
            val minLabelGapPx = 16.dp.toPx()
            var lastLabelY = Float.POSITIVE_INFINITY
            visibleTiers.forEach { tier ->
                val gy = yFor(tier.first.toFloat())
                drawLine(
                    color = palette.textMuted.copy(alpha = 0.15f),
                    start = Offset(leftMarginPx, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = 1.5f
                )
                if (lastLabelY - gy >= minLabelGapPx) {
                    drawYLabel(getLocalizedRankName(tier.second, isEnglish), gy)
                    lastLabelY = gy
                }
            }
        } else {
            val gridLines = 4
            for (i in 0..gridLines) {
                val gy = chartH / gridLines * i
                drawLine(
                    color = palette.textMuted.copy(alpha = 0.15f),
                    start = Offset(leftMarginPx, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = 1.5f
                )
                val value = domainMax - (span / gridLines) * i
                drawYLabel(numberFormat.format(value.toLong()), gy)
            }
        }

        drawLine(
            color = palette.border,
            start = Offset(leftMarginPx, 0f),
            end = Offset(leftMarginPx, chartH),
            strokeWidth = 2f
        )

        val xLabelCount = minOf(maxCount, 5)
        if (maxCount > 1) {
            for (k in 0 until xLabelCount) {
                val i = (maxCount - 1) * k / (xLabelCount - 1).coerceAtLeast(1)
                drawXLabel("${i + 1}", xFor(i))
            }
        } else {
            drawXLabel("1", xFor(0))
        }

        fun pathFor(entries: List<RankEntry>): androidx.compose.ui.graphics.Path {
            val path = androidx.compose.ui.graphics.Path()
            entries.forEachIndexed { i, e ->
                val x = xFor(i)
                val y = yFor(e.rank.toFloat())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        if (seriesA.isNotEmpty()) {
            drawPath(pathFor(seriesA), color = colorA, style = Stroke(width = 4f))
        }
        if (seriesB.isNotEmpty()) {
            drawPath(pathFor(seriesB), color = colorB, style = Stroke(width = 4f))
        }
    }
}