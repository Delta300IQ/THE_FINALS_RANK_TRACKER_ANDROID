package com.example.finalsranktracker

import android.content.Context
import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class Palette(
    val bg: Color, val surface: Color, val surfaceAlt: Color, val border: Color,
    val accent: Color, val accentOn: Color, val cyan: Color, val green: Color,
    val red: Color, val textPrimary: Color, val textMuted: Color
)

internal val DarkPalette = Palette(
    bg = Color(0xFF16140F), surface = Color(0xFF211D17), surfaceAlt = Color(0xFF2A251C),
    border = Color(0xFF3A3226), accent = Color(0xFFFF6A1A), accentOn = Color(0xFF1A1006),
    cyan = Color(0xFF4FC8D6), green = Color(0xFF33D17A), red = Color(0xFFFF4D5E),
    textPrimary = Color(0xFFF3EFE7), textMuted = Color(0xFF9C9284)
)

internal val LightPalette = Palette(
    bg = Color(0xFFFAF7F2), surface = Color(0xFFFFFFFF), surfaceAlt = Color(0xFFF0EBE2),
    border = Color(0xFFDDD5C7), accent = Color(0xFFE85D04), accentOn = Color(0xFFFFFFFF),
    cyan = Color(0xFF0C7C90), green = Color(0xFF1B9C5C), red = Color(0xFFFF4D5E),
    textPrimary = Color(0xFF211D17), textMuted = Color(0xFF837B6C)
)

internal fun lerpPalette(a: Palette, b: Palette, t: Float): Palette = Palette(
    bg = lerp(a.bg, b.bg, t), surface = lerp(a.surface, b.surface, t), surfaceAlt = lerp(a.surfaceAlt, b.surfaceAlt, t),
    border = lerp(a.border, b.border, t), accent = lerp(a.accent, b.accent, t), accentOn = lerp(a.accentOn, b.accentOn, t),
    cyan = lerp(a.cyan, b.cyan, t), green = lerp(a.green, b.green, t), red = lerp(a.red, b.red, t),
    textPrimary = lerp(a.textPrimary, b.textPrimary, t), textMuted = lerp(a.textMuted, b.textMuted, t)
)

internal data class Strings(
    val eyebrow: String, val title: String, val currentRankLabel: String, val nextRankPrefix: String, val rubyMax: String,
    val best: String, val worst: String, val matches: String, val detailedStats: String, val avgProgress: String,
    val avgGain: String, val avgLoss: String, val biggestGain: String, val biggestLoss: String, val emptyState: String,
    val noDataPeriod: String, val zoomOut: String, val zoomIn: String, val periodWeek: String, val periodMonth: String,
    val periodAll: String, val vsPrevious: String, val vsLowest: String, val vsHighest: String, val recordedAt: String,
    val startingRankPlaceholder: String, val newRankPlaceholder: String, val startButton: String, val saveButton: String,
    val undoLast: String, val historyShow: String, val historyHide: String, val deleteConfirm: String, val confirmWord: String,
    val cancelWord: String, val resetAll: String, val confirmResetAll: String, val season: String, val darkModeLabel: String,
    val lightModeLabel: String, val exportButton: String, val exportedToClipboard: String, val importButton: String,
    val importConfirmQuestion: String, val importFoundPrefix: String, val importSeasonsWord: String, val importMatchesWord: String,
    val importErrorMsg: String, val importSuccessMsg: String, val footer: String, val rsRemaining: String, val selectTags: String,
    val avgStatsByTag: String, val exportJsonButton: String, val importJsonButton: String, val exportedToJson: String,
    val importJsonErrorMsg: String, val winStreakLabel: String, val undoConfirmMsg: String, val top10kLabel: String,
    val bestWinStreakLabel: String, val bestLoseStreakLabel: String, val winRateLabel: String, val patchNotesLabel: String,
    val invalidScoreMsg: String, val typoDetectedMsg: String, val rankSavedMsg: String, val progressTodayLabel: String,
    val progressWeekLabel: String, val progressMonthLabel: String, val progressSeasonLabel: String, val rankGoalTitle: String,
    val rankGoalPlaceholder: String, val rankGoalSet: String, val rankGoalReached: String, val rankGoalNotEnoughData: String,
    val rankGoalEstimatePrefix: String, val rankGoalEstimateSuffix: String, val sortLabel: String, val filterLabel: String,
    val sortOldestFirst: String, val sortNewestFirst: String, val sortGainAsc: String, val sortGainDesc: String,
    val filterAll: String, val noMatchForFilter: String, val rankGoalAlreadyMax: String, val previousSeasonDesc: String,
    val nextSeasonDesc: String, val closeDesc: String, val editDesc: String, val removeDesc: String, val compareSeasonsLabel: String,
    val compareSeasonsPrompt: String, val compareSeasonsNoOthers: String, val performanceByTimeTitle: String,
    val dayOfWeekLabel: String, val timeOfDayLabel: String, val exportReminderMessage: String, val exportReminderDismiss: String,
    val addTagTitle: String, val addTagPlaceholder: String, val addWord: String,
    val deleteTagTitle: String, val deleteTagDesc: String
)

internal val FR = Strings(
    eyebrow = "THE FINALS · CLASSÉ", title = "SUIVI DU RANG", currentRankLabel = "RANG ACTUEL", nextRankPrefix = "avant",
    rubyMax = "Ruby voir le\nclassement mondial", best = "MEILLEUR", worst = "PLUS BAS", matches = "MATCHS",
    detailedStats = "Statistiques détaillées", avgProgress = "Progression moyenne / match", avgGain = "Gain moyen",
    avgLoss = "Perte moyenne", biggestGain = "Plus gros gain", biggestLoss = "Plus grosse perte",
    emptyState = "Entre ton rang actuel pour démarrer le suivi.", noDataPeriod = "Aucune donnée sur cette période.",
    zoomOut = "− zoom", zoomIn = "zoom +", periodWeek = "7 jours", periodMonth = "30 jours", periodAll = "Saison",
    vsPrevious = "Vs match précédent", vsLowest = "Vs rang le plus bas", vsHighest = "Vs rang le plus haut",
    recordedAt = "Enregistré le", startingRankPlaceholder = "Rang de départ", newRankPlaceholder = "Nouveau rang",
    startButton = "Démarrer", saveButton = "Enregistrer", undoLast = "↺ Supprimer le dernier match",
    historyShow = "Historique ▼", historyHide = "Historique ▲", deleteConfirm = "Supprimer ce match ?", confirmWord = "Confirmer",
    cancelWord = "Annuler", resetAll = "Tout réinitialiser", confirmResetAll = "Effacer tout l'historique ?", season = "Saison",
    darkModeLabel = "Mode sombre", lightModeLabel = "Mode clair", exportButton = "Exporter les données",
    exportedToClipboard = "Copié dans le presse-papiers !", importButton = "Importer des données",
    importConfirmQuestion = "Remplacer toutes les données actuelles ?", importFoundPrefix = "Trouvé :",
    importSeasonsWord = "saison(s)", importMatchesWord = "match(s)", importErrorMsg = "Presse-papiers invalide.",
    importSuccessMsg = "Données importées !", footer = "Made by Delta300IQ\nfeedback on discord : delta8771",
    rsRemaining = "RS restants", selectTags = "Sélectionner des tags (optionnel) :", avgStatsByTag = "Moyenne par tag :",
    exportJsonButton = "Exporter .json", importJsonButton = "Importer .json", exportedToJson = "JSON exporté !",
    importJsonErrorMsg = "Impossible d'importer.", winStreakLabel = "WINSTREAK", undoConfirmMsg = "Supprimer le dernier match ?",
    top10kLabel = "Voir le Top 10 000", bestWinStreakLabel = "Meilleure série de gains", bestLoseStreakLabel = "Pire série de pertes",
    winRateLabel = "Win rate", patchNotesLabel = "Voir le dernier patch notes", invalidScoreMsg = "Score invalide !",
    typoDetectedMsg = "Variation de plus de 3500 RS détectée, vérifie ton score", rankSavedMsg = "Nouveau rang enregistré !",
    progressTodayLabel = "Progression du jour", progressWeekLabel = "Progression de la semaine",
    progressMonthLabel = "Progression du mois", progressSeasonLabel = "Progression de la saison (totale)",
    rankGoalTitle = "OBJECTIF DE RANG", rankGoalPlaceholder = "Choisir un objectif", rankGoalSet = "Définir",
    rankGoalReached = "Objectif atteint !", rankGoalNotEnoughData = "Impossible de faire une estimation",
    rankGoalEstimatePrefix = "Encore ~", rankGoalEstimateSuffix = "matchs avant", sortLabel = "⇅ Trier",
    filterLabel = "▤ Filtres", sortOldestFirst = "Ancien → récent", sortNewestFirst = "Récent → ancien",
    sortGainAsc = "Gain RS croissant", sortGainDesc = "Gain RS décroissant", filterAll = "Tous",
    noMatchForFilter = "Aucun match correspondant", rankGoalAlreadyMax = "Tu es au rang max !",
    previousSeasonDesc = "Précédent", nextSeasonDesc = "Suivant", closeDesc = "Fermer", editDesc = "Modifier",
    removeDesc = "Supprimer", compareSeasonsLabel = "Comparer", compareSeasonsPrompt = "Saison à comparer :",
    compareSeasonsNoOthers = "Aucune autre donnée.", performanceByTimeTitle = "Performance par créneau",
    dayOfWeekLabel = "Par jour de la semaine", timeOfDayLabel = "Par moment de la journée",
    exportReminderMessage = "Pensez à sauvegarder vos données !", exportReminderDismiss = "Plus tard",
    addTagTitle = "Nouveau tag", addTagPlaceholder = "Nom du tag", addWord = "Ajouter",
    deleteTagTitle = "Supprimer le tag", deleteTagDesc = "Voulez-vous supprimer ce tag personnalisé ?"
)

internal val EN = Strings(
    eyebrow = "THE FINALS · RANKED", title = "RANK TRACKER", currentRankLabel = "CURRENT RANK", nextRankPrefix = "to",
    rubyMax = "Ruby view\nworld leaderboard", best = "BEST", worst = "LOWEST", matches = "MATCHES",
    detailedStats = "Detailed stats", avgProgress = "Average progression / match", avgGain = "Average gain",
    avgLoss = "Average loss", biggestGain = "Biggest gain", biggestLoss = "Biggest loss",
    emptyState = "Enter your current rank to start.", noDataPeriod = "No data for this period.",
    zoomOut = "− zoom", zoomIn = "zoom +", periodWeek = "7 days", periodMonth = "30 days", periodAll = "Season",
    vsPrevious = "Vs previous match", vsLowest = "Vs lowest rank", vsHighest = "Vs highest rank",
    recordedAt = "Recorded on", startingRankPlaceholder = "Starting rank", newRankPlaceholder = "Add a new rank",
    startButton = "Start", saveButton = "Save", undoLast = "↺ Remove last match",
    historyShow = "History ▼", historyHide = "History ▲", deleteConfirm = "Delete this match ?", confirmWord = "Confirm",
    cancelWord = "Cancel", resetAll = "Reset all", confirmResetAll = "Clear all history ?", season = "Season",
    darkModeLabel = "Dark mode", lightModeLabel = "Light mode", exportButton = "Export data",
    exportedToClipboard = "Copied to clipboard !", importButton = "Import data",
    importConfirmQuestion = "Replace all current data ?", importFoundPrefix = "Found:",
    importSeasonsWord = "season(s)", importMatchesWord = "match(es)", importErrorMsg = "Invalid clipboard.",
    importSuccessMsg = "Data imported!", footer = "Made by Delta300IQ\nfeedback on discord : delta8771",
    rsRemaining = "RS remaining", selectTags = "Select tags (optional):", avgStatsByTag = "Average by tag:",
    exportJsonButton = "Export .json", importJsonButton = "Import .json", exportedToJson = "JSON exported!",
    importJsonErrorMsg = "Import failed.", winStreakLabel = "WINSTREAK", undoConfirmMsg = "Remove last match ?",
    top10kLabel = "View Top 10,000", bestWinStreakLabel = "Best win streak", bestLoseStreakLabel = "Worst lose streak",
    winRateLabel = "Win rate", patchNotesLabel = "View latest patch notes", invalidScoreMsg = "Who do you think you are, idiot !",
    typoDetectedMsg = "Change of more than 3500 RS detected, check your score", rankSavedMsg = "New rank recorded!",
    progressTodayLabel = "Today's progress", progressWeekLabel = "This week's progress",
    progressMonthLabel = "This month's progress", progressSeasonLabel = "Season progress (total)",
    rankGoalTitle = "RANK GOAL", rankGoalPlaceholder = "Choose a goal", rankGoalSet = "Set",
    rankGoalReached = "Goal reached !", rankGoalNotEnoughData = "Unable to estimate",
    rankGoalEstimatePrefix = "About ~", rankGoalEstimateSuffix = "matches before", sortLabel = "⇅ Sort",
    filterLabel = "▤ Filters", sortOldestFirst = "Oldest → newest", sortNewestFirst = "Newest → oldest",
    sortGainAsc = "RS gain ascending", sortGainDesc = "RS gain descending", filterAll = "All",
    noMatchForFilter = "No matches found", rankGoalAlreadyMax = "You are at max rank!",
    previousSeasonDesc = "Prev", nextSeasonDesc = "Next", closeDesc = "Close", editDesc = "Edit",
    removeDesc = "Remove", compareSeasonsLabel = "Compare", compareSeasonsPrompt = "Season to compare:",
    compareSeasonsNoOthers = "No other data.", performanceByTimeTitle = "Performance by time slot",
    dayOfWeekLabel = "By day of week", timeOfDayLabel = "By time of day",
    exportReminderMessage = "Remember to back up your data!", exportReminderDismiss = "Later",
    addTagTitle = "New tag", addTagPlaceholder = "Tag name", addWord = "Add",
    deleteTagTitle = "Delete tag", deleteTagDesc = "Do you want to delete this custom tag?"
)

@Keep
internal data class RankEntry(val rank: Int, val timestamp: Long, val notes: List<String> = emptyList())
internal data class ChartPoint(val absoluteIndex: Int, val rank: Int, val timestamp: Long)
internal enum class ChartPeriod { WEEK, MONTH, ALL }
internal enum class HistorySortMode { OLDEST_FIRST, NEWEST_FIRST, GAIN_ASC, GAIN_DESC }

internal val RANK_TIERS = listOf(
    0 to "Bronze 4", 2500 to "Bronze 3", 5000 to "Bronze 2", 7500 to "Bronze 1",
    10000 to "Silver 4", 12500 to "Silver 3", 15000 to "Silver 2", 17500 to "Silver 1",
    20000 to "Gold 4", 22500 to "Gold 3", 25000 to "Gold 2", 27500 to "Gold 1",
    30000 to "Platinum 4", 32500 to "Platinum 3", 35000 to "Platinum 2", 37500 to "Platinum 1",
    40000 to "Diamond 4", 42500 to "Diamond 3", 45000 to "Diamond 2", 47500 to "Diamond 1", 55000 to "Ruby"
)

internal val DAY_LABELS_FR = listOf("Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam")
internal val DAY_LABELS_EN = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
internal val HOUR_BUCKETS = listOf(0..5, 6..11, 12..17, 18..23)
internal val HOUR_BUCKET_LABELS_FR = listOf("Nuit", "Matin", "Aprem", "Soir")
internal val HOUR_BUCKET_LABELS_EN = listOf("Night", "Morning", "Afternoon", "Evening")

internal val DEFAULT_TAGS_FR = listOf("SoloQ", "DuoQ", "Trio", "Seed 1/2", "Seed 3/5", "Seed 6/8", "J'ai bien joué", "J'ai mal joué", "Bons teammates", "Mauvais teammates", "Coup de chance", "Pas de chance", "Niveau trop élevé")
internal val DEFAULT_TAGS_EN = listOf("SoloQ", "DuoQ", "Trio", "Seed 1/2", "Seed 3/5", "Seed 6/8", "I played well", "I played poorly", "Good teammates", "Bad teammates", "Lucky", "Unlucky", "Skill level too high")

internal val TAG_GROUPS_FR = listOf(
    listOf("SoloQ", "DuoQ", "Trio"),
    listOf("Seed 1/2", "Seed 3/5", "Seed 6/8"),
    listOf("Pas de chance", "Coup de chance"),
    listOf("J'ai mal joué", "J'ai bien joué")
)

internal val TAG_GROUPS_EN = listOf(
    listOf("SoloQ", "DuoQ", "Trio"),
    listOf("Seed 1/2", "Seed 3/5", "Seed 6/8"),
    listOf("Unlucky", "Lucky"),
    listOf("I played poorly", "I played well")
)

internal fun getTagGroups(isEnglish: Boolean) = if (isEnglish) TAG_GROUPS_EN else TAG_GROUPS_FR

internal fun translateTag(tag: String, toEnglish: Boolean): String {
    val idxFr = DEFAULT_TAGS_FR.indexOf(tag)
    if (idxFr != -1) return if (toEnglish) DEFAULT_TAGS_EN[idxFr] else DEFAULT_TAGS_FR[idxFr]

    val idxEn = DEFAULT_TAGS_EN.indexOf(tag)
    if (idxEn != -1) return if (toEnglish) DEFAULT_TAGS_EN[idxEn] else DEFAULT_TAGS_FR[idxEn]

    return tag
}

internal fun rankNameFor(rs: Int): String {
    return RANK_TIERS.lastOrNull { rs >= it.first }?.second ?: "Bronze 4"
}

internal fun getLocalizedRankName(rawName: String, isEnglish: Boolean): String {
    if (rawName == "Ruby") return "Ruby"
    val parts = rawName.split(" ")
    if (parts.size < 2) return rawName
    val roman = when (parts[1]) {
        "4" -> "IV"
        "3" -> "III"
        "2" -> "II"
        "1" -> "I"
        else -> parts[1]
    }
    val base = if (isEnglish) {
        parts[0]
    } else {
        when (parts[0]) {
            "Bronze" -> "Bronze"
            "Silver" -> "Argent"
            "Gold" -> "Or"
            "Platinum" -> "Platine"
            "Diamond" -> "Diamant"
            else -> parts[0]
        }
    }
    return "$base $roman"
}

internal fun rankLogoResFor(rs: Int): Int {
    val name = RANK_TIERS.lastOrNull { rs >= it.first }?.second ?: "Bronze 4"
    return when (name) {
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
    val idx = RANK_TIERS.indexOfLast { rs >= it.first }
    if (idx == -1 || idx + 1 >= RANK_TIERS.size) return null
    val current = RANK_TIERS[idx]
    val next = RANK_TIERS[idx + 1]
    val pct = (((rs - current.first).toFloat() / (next.first - current.first)) * 100).roundToInt().coerceIn(0, 100)
    return Triple(pct, next.first - rs, next.second)
}

internal fun longestStreak(deltas: List<Int>, predicate: (Int) -> Boolean): Int {
    var max = 0
    var curr = 0
    deltas.forEach {
        if (predicate(it)) {
            curr++
            max = maxOf(max, curr)
        } else {
            curr = 0
        }
    }
    return max
}

internal fun getRankDifficultyMultiplier(rank: Int): Double {
    return when {
        rank >= 50000 -> 0.15
        rank >= 47500 -> 0.30
        rank >= 45000 -> 0.40
        rank >= 42500 -> 0.50
        rank >= 40000 -> 0.60
        rank >= 37500 -> 0.70
        rank >= 35000 -> 0.75
        rank >= 32500 -> 0.80
        rank >= 30000 -> 0.90
        rank >= 20000 -> 1.00
        else -> 1.20
    }
}

internal fun estimateProgressRate(entries: List<RankEntry>, maxWindow: Int = 10): Double? {
    if (entries.size < 2) return null
    val w = entries.takeLast(maxWindow)
    val n = w.size
    val xm = (n - 1) / 2.0
    val ym = w.map { it.rank.toDouble() }.average()
    var num = 0.0
    var den = 0.0
    w.forEachIndexed { i, e ->
        val dx = i - xm
        num += dx * (e.rank - ym)
        den += dx * dx
    }
    return if (den != 0.0) num / den else null
}

internal fun estimateMatchesToGoal(entries: List<RankEntry>, currentRank: Int, goalValue: Int): Int? {
    if (entries.size < 2) return null
    val recentEntries = entries.takeLast(20)
    if (recentEntries.size < 2) return null

    val deltas = recentEntries.map { it.rank }.zipWithNext { a, b -> b - a }
    val gains = deltas.filter { it > 0 }
    val losses = deltas.filter { it < 0 }
    if (gains.isEmpty()) return null

    val avgGain = gains.average()
    val avgLoss = if (losses.isNotEmpty()) losses.average() else 0.0
    val realWinRate = gains.size.toDouble() / deltas.size
    val optimisticWinRate = minOf(0.95, realWinRate + 0.04)
    val lossRate = 1.0 - optimisticWinRate
    val momentum = estimateProgressRate(recentEntries, 10) ?: 0.0

    var expectedRsAtCurrentRank = (optimisticWinRate * avgGain) + (lossRate * avgLoss)
    if (momentum > 0) {
        expectedRsAtCurrentRank = (expectedRsAtCurrentRank * 0.6) + (momentum * 0.4)
    }
    if (expectedRsAtCurrentRank <= 1.0) {
        expectedRsAtCurrentRank = maxOf(avgGain * 0.15, 8.0)
    }

    val currentMultiplier = getRankDifficultyMultiplier(currentRank)
    val baseRawExpectedRs = expectedRsAtCurrentRank / currentMultiplier

    var currentSimulatedRank = currentRank.toDouble()
    var matches = 0
    val safeMaxMatches = 3000

    while (currentSimulatedRank < goalValue && matches < safeMaxMatches) {
        val multiplier = getRankDifficultyMultiplier(currentSimulatedRank.toInt())
        val gainForThisMatch = baseRawExpectedRs * multiplier
        if (gainForThisMatch <= 0.1) return null
        currentSimulatedRank += gainForThisMatch
        matches++
    }
    return if (matches >= safeMaxMatches) null else matches
}

internal fun calculateStatsByTag(entries: List<RankEntry>, isEnglish: Boolean): Map<String, Pair<Double?, Double?>> {
    if (entries.size < 2) return emptyMap()
    val gains = mutableMapOf<String, MutableList<Int>>()
    val losses = mutableMapOf<String, MutableList<Int>>()

    for (i in 1 until entries.size) {
        val delta = entries[i].rank - entries[i - 1].rank
        entries[i].notes.forEach { raw ->
            val tag = translateTag(raw, isEnglish)
            if (delta > 0) {
                gains.getOrPut(tag) { mutableListOf() }.add(delta)
            } else if (delta < 0) {
                losses.getOrPut(tag) { mutableListOf() }.add(abs(delta))
            }
        }
    }
    val allKeys = (gains.keys + losses.keys).toSet()
    return allKeys.associateWith { key ->
        Pair(gains[key]?.average(), losses[key]?.average()?.let { -it })
    }
}

internal fun countMatchesByTag(entries: List<RankEntry>, isEnglish: Boolean): Map<String, Int> {
    val map = mutableMapOf<String, Int>()
    entries.forEach { e ->
        e.notes.forEach { raw ->
            val tag = translateTag(raw, isEnglish)
            map[tag] = (map[tag] ?: 0) + 1
        }
    }
    return map
}

internal fun averageDeltaByDayOfWeek(entries: List<RankEntry>): List<Double?> {
    val sums = DoubleArray(7)
    val counts = IntArray(7)
    for (i in 1 until entries.size) {
        val delta = (entries[i].rank - entries[i - 1].rank).toDouble()
        val day = java.util.Calendar.getInstance().apply { timeInMillis = entries[i].timestamp }.get(java.util.Calendar.DAY_OF_WEEK) - 1
        sums[day] += delta
        counts[day] += 1
    }
    return (0..6).map { if (counts[it] > 0) sums[it] / counts[it] else null }
}

internal fun averageDeltaByHourBucket(entries: List<RankEntry>): List<Double?> {
    val sums = DoubleArray(HOUR_BUCKETS.size)
    val counts = IntArray(HOUR_BUCKETS.size)
    for (i in 1 until entries.size) {
        val delta = (entries[i].rank - entries[i - 1].rank).toDouble()
        val hr = java.util.Calendar.getInstance().apply { timeInMillis = entries[i].timestamp }.get(java.util.Calendar.HOUR_OF_DAY)
        val b = HOUR_BUCKETS.indexOfFirst { hr in it }
        if (b >= 0) {
            sums[b] += delta
            counts[b] += 1
        }
    }
    return HOUR_BUCKETS.indices.map { if (counts[it] > 0) sums[it] / counts[it] else null }
}

internal fun getProgressForPeriod(entries: List<RankEntry>, startMs: Long): Int? {
    if (entries.isEmpty()) return null
    val pe = entries.filter { it.timestamp >= startMs }
    if (pe.isEmpty()) return null
    val start = entries.lastOrNull { it.timestamp < startMs }?.rank ?: pe.first().rank
    return pe.last().rank - start
}

internal const val PREFS_NAME = "finals_rank_tracker"
internal const val KEY_SEASONS = "seasons_json_v2"
internal const val KEY_SEASONS_BACKUP = "seasons_json_backup"
internal const val KEY_ENTRIES_LEGACY = "entries_json"
internal const val KEY_DARK_MODE = "dark_mode"
internal const val KEY_LANGUAGE = "is_english"
internal const val KEY_SELECTED_SEASON = "selected_season"
internal const val KEY_RANK_GOALS = "rank_goals_v1"
internal const val KEY_CUSTOM_TAGS = "custom_tags"
internal const val KEY_LAST_EXPORT_TIMESTAMP = "last_export_timestamp"
internal const val KEY_EXPORT_REMINDER_DISMISS_TIMESTAMP = "export_reminder_dismiss_timestamp"
internal const val EXPORT_REMINDER_INTERVAL_MS = 14L * 24 * 3600 * 1000

internal fun loadCustomTags(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_CUSTOM_TAGS, "[]")
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) { emptyList() }
}

internal fun saveCustomTags(context: Context, tags: List<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val arr = JSONArray()
    tags.forEach { arr.put(it) }
    prefs.edit().putString(KEY_CUSTOM_TAGS, arr.toString()).apply()
}

internal fun loadAllSeasons(context: Context): Map<Int, List<RankEntry>> {
    val mainFile = File(context.filesDir, "rank_data.json")
    if (mainFile.exists()) {
        try {
            val parsed = parseAllSeasonsJson(mainFile.readText())
            if (parsed != null) return parsed
        } catch (e: Exception) {}
    }

    val backupFile = File(context.filesDir, "rank_data_backup.json")
    if (backupFile.exists()) {
        try {
            val parsed = parseAllSeasonsJson(backupFile.readText())
            if (parsed != null) return parsed
        } catch (e: Exception) {}
    }

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val prefsBackup = prefs.getString(KEY_SEASONS_BACKUP, null)
    if (prefsBackup != null) {
        val parsed = parseAllSeasonsJson(prefsBackup)
        if (parsed != null) return parsed
    }

    val prefsLegacy = prefs.getString(KEY_SEASONS, null)
    if (prefsLegacy != null) {
        val parsed = parseAllSeasonsJson(prefsLegacy)
        if (parsed != null) {
            saveAllSeasons(context, parsed)
            return parsed
        }
    }

    val oldJson = prefs.getString(KEY_ENTRIES_LEGACY, null)
    if (oldJson != null) {
        try {
            val arr = JSONArray(oldJson)
            val now = System.currentTimeMillis()
            val migrated = (0 until arr.length()).map { RankEntry(arr.getInt(it), now, emptyList()) }
            val map = mapOf(11 to migrated)
            saveAllSeasons(context, map)
            return map
        } catch (e: Exception) {}
    }
    return emptyMap()
}

internal fun saveAllSeasons(context: Context, seasons: Map<Int, List<RankEntry>>) {
    val jsonStr = buildAllSeasonsJson(seasons)
    if (parseAllSeasonsJson(jsonStr) == null) return

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val mainFile = File(context.filesDir, "rank_data.json")
    val backupFile = File(context.filesDir, "rank_data_backup.json")
    val tempFile = File(context.filesDir, "rank_data_temp.json")

    val currentContent = if (mainFile.exists()) {
        try { mainFile.readText() } catch (e: Exception) { null }
    } else {
        prefs.getString(KEY_SEASONS, null)
    }

    if (currentContent != null) {
        prefs.edit().putString(KEY_SEASONS_BACKUP, currentContent).apply()
        try { backupFile.writeText(currentContent) } catch (e: Exception) {}
    }

    try {
        tempFile.writeText(jsonStr)
        if (tempFile.exists()) {
            tempFile.renameTo(mainFile)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal fun loadDarkMode(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_DARK_MODE, true)
internal fun saveDarkMode(context: Context, isDark: Boolean) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK_MODE, isDark).apply()
internal fun loadLanguage(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_LANGUAGE, true)
internal fun saveLanguage(context: Context, isEnglish: Boolean) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_LANGUAGE, isEnglish).apply()
internal fun loadSelectedSeason(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_SELECTED_SEASON, 11)
internal fun saveSelectedSeason(context: Context, s: Int) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_SELECTED_SEASON, s).apply()

internal fun loadRankGoals(context: Context): Map<Int, Int> {
    val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_RANK_GOALS, null) ?: return emptyMap()
    return try {
        val obj = JSONObject(json); val res = mutableMapOf<Int, Int>(); val keys = obj.keys()
        while (keys.hasNext()) { val k = keys.next(); val s = k.toIntOrNull(); if (s != null) res[s] = obj.getInt(k) }; res
    } catch (e: Exception) { emptyMap() }
}

internal fun saveRankGoals(context: Context, goals: Map<Int, Int>) {
    val obj = JSONObject(); goals.forEach { (s, g) -> obj.put(s.toString(), g) }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_RANK_GOALS, obj.toString()).apply()
}

internal fun loadLastExportTimestamp(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_LAST_EXPORT_TIMESTAMP, 0L)
internal fun saveLastExportTimestamp(context: Context, ts: Long) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_EXPORT_TIMESTAMP, ts).apply()
internal fun loadExportReminderDismissTimestamp(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_EXPORT_REMINDER_DISMISS_TIMESTAMP, 0L)
internal fun saveExportReminderDismissTimestamp(context: Context, ts: Long) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putLong(KEY_EXPORT_REMINDER_DISMISS_TIMESTAMP, ts).apply()

internal val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)

internal fun formatNum(n: Int?): String {
    if (n == null) return "—"
    return numberFormat.format(n.toLong())
}

internal fun formatSigned(n: Int?): String {
    if (n == null) return "—"
    return (if (n >= 0) "+" else "") + formatNum(n)
}

internal fun formatDateTime(timestamp: Long, isEnglish: Boolean): String {
    val pattern = if (isEnglish) "MM/dd/yyyy hh:mm a" else "dd/MM/yyyy HH:mm"
    val locale = if (isEnglish) Locale.US else Locale.FRANCE
    val sdf = SimpleDateFormat(pattern, locale)
    return sdf.format(Date(timestamp))
}

internal fun buildAllSeasonsJson(allSeasons: Map<Int, List<RankEntry>>): String {
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

internal fun parseAllSeasonsJson(jsonStr: String): Map<Int, List<RankEntry>>? {
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

internal fun buildExportText(allSeasons: Map<Int, List<RankEntry>>, isEnglish: Boolean): String {
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
                val translatedTags = entry.notes.map { translateTag(it, isEnglish) }
                val notesStr = if (translatedTags.isNotEmpty()) " {" + translatedTags.joinToString(",") + "}" else ""
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

internal val SEASON_HEADER_REGEX = Regex("""===\s*(?:Saison|Season)\s+(\d+)\s*===""")
internal val MATCH_LINE_REGEX = Regex("""Match\s+\d+\s*-\s*(.+?)\s*-\s*(\d+)\s*RS(?:\s*\[[+-]?\d+\])?(?:\s*\{(.*?)\})?""")

internal fun parseImportDateTime(text: String): Long? {
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
        } catch (e: Exception) { }
    }
    return null
}

internal fun parseImportText(text: String): Map<Int, List<RankEntry>>? {
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