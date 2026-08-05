package com.example.finalsranktracker

import android.content.Context
import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

internal enum class AppTab { HOME, STATS, HISTORY }
internal enum class ChartType { CANDLESTICK, STRAIGHT_LINE, CURVED_LINE }
internal enum class HistorySortMode { OLDEST_FIRST, NEWEST_FIRST, GAIN_ASC, GAIN_DESC }
internal enum class ChartPeriod { WEEK, MONTH, ALL }

internal const val APP_VERSION = "1.0.0"

internal data class Palette(
    val bg: Color, val surface: Color, val surfaceAlt: Color, val border: Color,
    val accent: Color, val accentOn: Color, val cyan: Color, val green: Color,
    val red: Color, val textPrimary: Color, val textMuted: Color,
    // Settings screen — couleurs identiques au jeu The Finals
    val settingsBg: Color, val settingsSectionLabel: Color, val settingsRow: Color
)

internal val DarkPalette = Palette(
    // Fond principal — centre #9FB6CD, bords #606979 (dégradé radial appliqué dans MainActivity)
    bg = Color(0xFF606979),
    surface = Color(0xFF303039),           // Surface de carte sombre
    surfaceAlt = Color(0xFF484D58),        // Surface secondaire
    border = Color(0xFF6B7684),            // Bordure standard
    // Jaune doré #F7BB2A — CTA principal
    accent = Color(0xFFF7BB2A),
    accentOn = Color(0xFF000000),
    // Bleu-gris #808F9F — secondaire/highlight
    cyan = Color(0xFF808F9F),
    green = Color(0xFF4ADE80),
    red = Color(0xFFFF0033),
    textPrimary = Color(0xFFFFFFFF),
    textMuted = Color(0xFFBFC3CB),
    // Settings "Red Alert" — rouge solide #D11E3C, aucune transparence
    settingsBg = Color(0xFFD11E3C),
    settingsSectionLabel = Color(0xFFFFFFFF),   // Toujours blanc dans les settings
    settingsRow = Color(0xFF651C2B)             // Couleur des lignes de settings
)

// Pas de mode clair — LightPalette identique pour éviter erreur compile
internal val LightPalette = DarkPalette

internal fun lerpPalette(a: Palette, b: Palette, t: Float): Palette = Palette(
    bg = lerp(a.bg, b.bg, t), surface = lerp(a.surface, b.surface, t),
    surfaceAlt = lerp(a.surfaceAlt, b.surfaceAlt, t), border = lerp(a.border, b.border, t),
    accent = lerp(a.accent, b.accent, t), accentOn = lerp(a.accentOn, b.accentOn, t),
    cyan = lerp(a.cyan, b.cyan, t), green = lerp(a.green, b.green, t),
    red = lerp(a.red, b.red, t), textPrimary = lerp(a.textPrimary, b.textPrimary, t),
    textMuted = lerp(a.textMuted, b.textMuted, t),
    settingsBg = lerp(a.settingsBg, b.settingsBg, t),
    settingsSectionLabel = lerp(a.settingsSectionLabel, b.settingsSectionLabel, t),
    settingsRow = lerp(a.settingsRow, b.settingsRow, t)
)

internal data class Strings(
    val eyebrow: String, val title: String, val statsTitle: String, val historyTitle: String,
    val currentRankLabel: String, val nextRankPrefix: String, val rubyMax: String,
    val best: String, val worst: String, val matches: String, val detailedStats: String,
    val avgProgress: String, val avgGain: String, val avgLoss: String, val biggestGain: String,
    val biggestLoss: String, val emptyState: String, val emptyStats: String, val emptyHistory: String,
    val emptyDashboardInfo: String, val noDataPeriod: String, val zoomOut: String, val zoomIn: String,
    val periodWeek: String, val periodMonth: String, val periodAll: String, val vsPrevious: String,
    val vsLowest: String, val vsHighest: String, val recordedAt: String,
    val startingRankPlaceholder: String, val newRankPlaceholder: String, val startButton: String,
    val saveButton: String, val undoLast: String, val historyShow: String, val historyHide: String,
    val deleteConfirm: String, val confirmWord: String, val cancelWord: String, val resetAll: String,
    val confirmResetAll: String, val season: String, val darkModeLabel: String, val lightModeLabel: String,
    val exportButton: String, val exportedToClipboard: String, val importButton: String,
    val importConfirmQuestion: String, val importFoundPrefix: String, val importSeasonsWord: String,
    val importMatchesWord: String, val importErrorMsg: String, val importSuccessMsg: String,
    val footer: String, val rsRemaining: String, val selectTags: String, val avgStatsByTag: String,
    val exportJsonButton: String, val importJsonButton: String, val exportedToJson: String,
    val importJsonErrorMsg: String, val winStreakLabel: String, val undoConfirmMsg: String,
    val top10kLabel: String, val bestWinStreakLabel: String, val bestLoseStreakLabel: String,
    val winRateLabel: String, val patchNotesLabel: String, val invalidScoreMsg: String,
    val typoDetectedMsg: String, val rankSavedMsg: String, val progressTodayLabel: String,
    val progressWeekLabel: String, val progressMonthLabel: String, val progressSeasonLabel: String,
    val rankGoalTitle: String, val rankGoalPlaceholder: String, val rankGoalSet: String,
    val rankGoalReached: String, val rankGoalNotEnoughData: String, val rankGoalEstimatePrefix: String,
    val rankGoalEstimateSuffix: String, val sortLabel: String, val filterLabel: String,
    val sortOldestFirst: String, val sortNewestFirst: String, val sortGainAsc: String,
    val sortGainDesc: String, val filterAll: String, val noMatchForFilter: String,
    val rankGoalAlreadyMax: String, val previousSeasonDesc: String, val nextSeasonDesc: String,
    val closeDesc: String, val editDesc: String, val removeDesc: String, val compareSeasonsLabel: String,
    val compareSeasonsPrompt: String, val compareSeasonsNoOthers: String, val performanceByTimeTitle: String,
    val dayOfWeekLabel: String, val timeOfDayLabel: String, val exportReminderMessage: String,
    val exportReminderDismiss: String, val addTagTitle: String, val addTagPlaceholder: String,
    val addWord: String, val deleteTagTitle: String, val deleteTagDesc: String,
    val showHiddenTags: String, val hideHiddenTags: String, val hideWord: String, val showWord: String,
    val tabHome: String, val tabStats: String, val tabHistory: String, val recentMatches: String,
    val addMatchTitle: String, val showAllMatches: String, val showLessMatches: String,
    val importWebButton: String, val importWebTitle1: String, val importWebTitle2: String,
    val importWebDesc: String, val importWebPlaceholder: String, val importWebLoading: String,
    val importWebNotFound: String, val importWebConfirm: String, val searchWord: String,
    val clipboardMenu: String, val jsonFileMenu: String, val settingsTitle: String,
    val appearanceLabel: String, val languageLabel: String, val disableSplashLabel: String,
    val autoUpdateLabel: String, val chartTypeLabel: String, val chartCandle: String,
    val chartLine: String, val chartCurve: String, val updateAvailableTitle: String,
    val updateAvailableDesc: String, val btnUpdate: String, val btnLater: String,
    val emptyHomeImportBtn: String, val topWorldPrefix: String, val compTableRank: String,
    val compTableWinrate: String, val compTableAvgMatch: String, val compTableGainLoss: String,
    val compTableStreaks: String, val compTableCount: String, val autoUpdateBtnDialog: String,
    val disclaimerTitle: String, val disclaimerDesc: String, val disclaimerBtn: String
)

internal val FR = Strings(
    eyebrow = "THE FINALS · CLASSÉ", title = "DASHBOARD", statsTitle = "STATISTIQUES",
    historyTitle = "HISTORIQUE", currentRankLabel = "RANG ACTUEL", nextRankPrefix = "avant",
    rubyMax = "Top Monde :", best = "MEILLEUR", worst = "PLUS BAS", matches = "MATCHS",
    detailedStats = "Statistiques détaillées", avgProgress = "Progression moyenne", avgGain = "Gain moyen",
    avgLoss = "Perte moyenne", biggestGain = "Plus gros gain", biggestLoss = "Plus grosse perte",
    emptyState = "Entre ton rang actuel pour démarrer le suivi.", emptyStats = "Pas de statistiques pour le moment.",
    emptyHistory = "Aucun match enregistré pour le moment.", emptyDashboardInfo = "Aucune donnée enregistrée.",
    noDataPeriod = "Aucune donnée sur cette période.", zoomOut = "− zoom", zoomIn = "zoom +", periodWeek = "7 jours",
    periodMonth = "30 jours", periodAll = "Saison", vsPrevious = "Vs match précédent", vsLowest = "Vs rang le plus bas",
    vsHighest = "Vs rang le plus haut", recordedAt = "Enregistré le", startingRankPlaceholder = "Rang de départ",
    newRankPlaceholder = "Nouveau rang", startButton = "Démarrer", saveButton = "Enregistrer",
    undoLast = "↺ Supprimer le dernier match", historyShow = "Historique ▼", historyHide = "Historique ▲",
    deleteConfirm = "Supprimer ce match ?", confirmWord = "Confirmer", cancelWord = "Annuler", resetAll = "Tout réinitialiser",
    confirmResetAll = "Effacer tout l'historique ?", season = "Saison", darkModeLabel = "Mode sombre",
    lightModeLabel = "Mode clair", exportButton = "Exporter les données", exportedToClipboard = "Export réalisé !",
    importButton = "Importer des données", importConfirmQuestion = "Remplacer toutes les données actuelles ?",
    importFoundPrefix = "Trouvé :", importSeasonsWord = "saison(s)", importMatchesWord = "match(s)",
    importErrorMsg = "Presse-papiers invalide.", importSuccessMsg = "Données importées !", footer = "Made by Delta300IQ\nfeedback on discord : delta8771",
    rsRemaining = "RS RESTANTS", selectTags = "Sélectionner des tags (optionnel) :", avgStatsByTag = "Moyenne par tag :",
    exportJsonButton = "Exporter .json", importJsonButton = "Importer .json", exportedToJson = "JSON exporté !",
    importJsonErrorMsg = "Impossible d'importer.", winStreakLabel = "WINSTREAK", undoConfirmMsg = "Supprimer le dernier match ?",
    top10kLabel = "Voir le Top 10 000", bestWinStreakLabel = "Meilleure série (W)", bestLoseStreakLabel = "Pire série (L)",
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
    removeDesc = "Supprimer", compareSeasonsLabel = "Comparer", compareSeasonsPrompt = "Saison(s) à comparer :",
    compareSeasonsNoOthers = "Aucune autre donnée.", performanceByTimeTitle = "Performance par créneau",
    dayOfWeekLabel = "Par jour de la semaine", timeOfDayLabel = "Par moment de la journée",
    exportReminderMessage = "Pensez à sauvegarder vos données !", exportReminderDismiss = "Plus tard",
    addTagTitle = "Nouveau tag", addTagPlaceholder = "Nom du tag", addWord = "Ajouter",
    deleteTagTitle = "Supprimer le tag", deleteTagDesc = "Voulez-vous supprimer ce tag personnalisé ?",
    showHiddenTags = "Afficher les tags masqués", hideHiddenTags = "Cacher les tags masqués",
    hideWord = "Masquer", showWord = "Afficher", tabHome = "Accueil", tabStats = "Statistiques",
    tabHistory = "Historique", recentMatches = "Derniers matchs", addMatchTitle = "AJOUTER UN MATCH",
    showAllMatches = "Afficher tout", showLessMatches = "Masquer",
    importWebButton = "TOP 10 000", importWebTitle1 = "AUTO IMPORT", importWebTitle2 = "(TOP 10 000)",
    importWebDesc = "Import auto depuis le top 10 000, vous devez être dans le top 10 000 pour utiliser cette fonctionnalité.",
    importWebPlaceholder = "Nom (ex: Balise#2431) ou LIEN depuis le top 10 000", importWebLoading = "Récupération des données en cours...",
    importWebNotFound = "Joueur introuvable. Assurez-vous qu'il est bien dans le Top 10 000.", importWebConfirm = "Importer toutes les saisons trouvées ?",
    searchWord = "Importer", clipboardMenu = "Presse-papiers", jsonFileMenu = "Fichier .json",
    settingsTitle = "PARAMÈTRES", appearanceLabel = "Apparence", languageLabel = "Langue",
    disableSplashLabel = "Désactiver l'écran de démarrage", autoUpdateLabel = "Mise à jour auto du rang au démarrage",
    chartTypeLabel = "Style de graphique", chartCandle = "Chandelier", chartLine = "Courbe Droite", chartCurve = "Courbe Arrondie",
    updateAvailableTitle = "Mise à jour disponible", updateAvailableDesc = "Une nouvelle version de The Finals Rank Tracker est disponible sur GitHub !",
    btnUpdate = "Mettre à jour", btnLater = "Plus tard", emptyHomeImportBtn = "Importer depuis le Top 10 000",
    topWorldPrefix = "Top Monde :", compTableRank = "Meilleur\nPire", compTableWinrate = "Win Rate",
    compTableAvgMatch = "Prog. Moyenne", compTableGainLoss = "Gain Moy.\nPerte Moy.", compTableStreaks = "Meilleure / Pire série",
    compTableCount = "Nb. Matchs", autoUpdateBtnDialog = "Mise à jour auto\n(Top 10 000)",
    disclaimerTitle = "DISCLAIMER", disclaimerDesc = "Cette application n'est pas officielle et n'est pas approuvée par Embark Studios. Elle est mise à disposition de la communauté THE FINALS gratuitement.", disclaimerBtn = "Fermer"
)

internal val EN = Strings(
    eyebrow = "THE FINALS · RANKED", title = "DASHBOARD", statsTitle = "STATISTICS",
    historyTitle = "HISTORY", currentRankLabel = "CURRENT RANK", nextRankPrefix = "to",
    rubyMax = "Global Top:", best = "BEST", worst = "LOWEST", matches = "MATCHES",
    detailedStats = "Detailed stats", avgProgress = "Average progression", avgGain = "Average gain",
    avgLoss = "Average loss", biggestGain = "Biggest gain", biggestLoss = "Biggest loss",
    emptyState = "Enter your current rank to start.", emptyStats = "No statistics available yet.",
    emptyHistory = "No matches recorded yet.", emptyDashboardInfo = "No data recorded.",
    noDataPeriod = "No data for this period.", zoomOut = "− zoom", zoomIn = "zoom +", periodWeek = "7 days",
    periodMonth = "30 days", periodAll = "Season", vsPrevious = "Vs previous match", vsLowest = "Vs lowest rank",
    vsHighest = "Vs highest rank", recordedAt = "Recorded on", startingRankPlaceholder = "Starting rank",
    newRankPlaceholder = "Add a new rank", startButton = "Start", saveButton = "Save",
    undoLast = "↺ Remove last match", historyShow = "History ▼", historyHide = "History ▲",
    deleteConfirm = "Delete this match ?", confirmWord = "Confirm", cancelWord = "Cancel", resetAll = "Reset all",
    confirmResetAll = "Clear all history ?", season = "Season", darkModeLabel = "Dark mode",
    lightModeLabel = "Light mode", exportButton = "Export data", exportedToClipboard = "Export successful !",
    importButton = "Import data", importConfirmQuestion = "Replace all current data ?",
    importFoundPrefix = "Found:", importSeasonsWord = "season(s)", importMatchesWord = "match(es)",
    importErrorMsg = "Invalid clipboard.", importSuccessMsg = "Data imported!", footer = "Made by Delta300IQ\nfeedback on discord : delta8771",
    rsRemaining = "RS REMAINING", selectTags = "Select tags (optional):", avgStatsByTag = "Average by tag:",
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
    removeDesc = "Remove", compareSeasonsLabel = "Compare", compareSeasonsPrompt = "Season(s) to compare:",
    compareSeasonsNoOthers = "No other data.", performanceByTimeTitle = "Performance by time slot",
    dayOfWeekLabel = "By day of week", timeOfDayLabel = "By time of day",
    exportReminderMessage = "Remember to back up your data!", exportReminderDismiss = "Later",
    addTagTitle = "New tag", addTagPlaceholder = "Tag name", addWord = "Add",
    deleteTagTitle = "Delete tag", deleteTagDesc = "Do you want to delete this custom tag?",
    showHiddenTags = "Show hidden tags", hideHiddenTags = "Hide hidden tags",
    hideWord = "Hide", showWord = "Show", tabHome = "Dashboard", tabStats = "Statistics",
    tabHistory = "History", recentMatches = "Recent matches", addMatchTitle = "ADD A MATCH",
    showAllMatches = "Show all", showLessMatches = "Show less",
    importWebButton = "TOP 10 000", importWebTitle1 = "AUTO IMPORT", importWebTitle2 = "(TOP 10 000)",
    importWebDesc = "Auto import from the top 10 000, you must be in the top 10 000 to auto import.",
    importWebPlaceholder = "Name (ex: Balise#2431) or LINK from the top 10 000", importWebLoading = "Fetching data...",
    importWebNotFound = "Player not found. Make sure you are in the Top 10k.", importWebConfirm = "Import all found seasons?",
    searchWord = "Import", clipboardMenu = "Clipboard", jsonFileMenu = ".json File",
    settingsTitle = "SETTINGS", appearanceLabel = "Appearance", languageLabel = "Language",
    disableSplashLabel = "Disable Splash Screen", autoUpdateLabel = "Auto-update rank on startup",
    chartTypeLabel = "Chart Style", chartCandle = "Candlestick", chartLine = "Straight Line",
    chartCurve = "Curved Line", updateAvailableTitle = "Update Available",
    updateAvailableDesc = "A new version of The Finals Rank Tracker is available on GitHub!",
    btnUpdate = "Update", btnLater = "Later", emptyHomeImportBtn = "Import from Top 10,000",
    topWorldPrefix = "Global Top:", compTableRank = "Best\nLowest", compTableWinrate = "Win Rate",
    compTableAvgMatch = "Avg. Prog.", compTableGainLoss = "Avg. Gain\nAvg. Loss",
    compTableStreaks = "Best/Worst streak", compTableCount = "Total Matches", autoUpdateBtnDialog = "Auto Update\n(Top 10 000)",
    disclaimerTitle = "DISCLAIMER", disclaimerDesc = "This application is not official and is not approved by Embark Studios. It is provided to the THE FINALS community for free.", disclaimerBtn = "Close"
)

@Keep internal data class RankEntry(val rank: Int, val timestamp: Long, val notes: List<String> = emptyList())
@Keep internal data class ChartPoint(val absoluteIndex: Int, val rank: Int, val timestamp: Long)
@Keep internal data class PlayerProfile(val name: String, val globalRank: Int?, val rankChange: Int?)
@Keep internal data class ParsedDavG25Data(
    val success: Boolean, val error: String?, val name: String?,
    val globalRank: Int?, val rankChange: Int?, val seasons: Map<Int, List<RankEntry>>
)

internal val RANK_TIERS = listOf(
    0 to "Bronze 4", 2500 to "Bronze 3", 5000 to "Bronze 2", 7500 to "Bronze 1",
    10000 to "Silver 4", 12500 to "Silver 3", 15000 to "Silver 2", 17500 to "Silver 1",
    20000 to "Gold 4", 22500 to "Gold 3", 25000 to "Gold 2", 27500 to "Gold 1",
    30000 to "Platinum 4", 32500 to "Platinum 3", 35000 to "Platinum 2", 37500 to "Platinum 1",
    40000 to "Diamond 4", 42500 to "Diamond 3", 45000 to "Diamond 2", 47500 to "Diamond 1", 55000 to "Ruby"
)

internal val DAY_LABELS_FR = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
internal val DAY_LABELS_EN = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
internal val HOUR_BUCKETS = listOf(0..5, 6..11, 12..17, 18..23)
internal val HOUR_BUCKET_LABELS_FR = listOf("Nuit", "Matin", "Aprem", "Soir")
internal val HOUR_BUCKET_LABELS_EN = listOf("Night", "Morning", "Afternoon", "Evening")

internal val DEFAULT_TAGS_FR = listOf(
    "SoloQ", "DuoQ", "Trio", "Seed 1/2", "Seed 3/5", "Seed 6/8", "J'ai bien joué",
    "J'ai mal joué", "Bons teammates", "Mauvais teammates", "Coup de chance",
    "Pas de chance", "Niveau trop élevé", "Leaderboard Import"
)
internal val DEFAULT_TAGS_EN = listOf(
    "SoloQ", "DuoQ", "Trio", "Seed 1/2", "Seed 3/5", "Seed 6/8", "I played well",
    "I played poorly", "Good teammates", "Bad teammates", "Lucky",
    "Unlucky", "Skill level too high", "Leaderboard Import"
)

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

internal val TAG_COMPARATOR = Comparator<String> { a, b ->
    val queueTags = listOf("SoloQ", "DuoQ", "Trio")
    val seedTags = listOf("Seed 1/2", "Seed 3/5", "Seed 6/8")

    val aQueue = queueTags.indexOf(a)
    val bQueue = queueTags.indexOf(b)
    if (aQueue != -1 && bQueue == -1) return@Comparator -1
    if (aQueue == -1 && bQueue != -1) return@Comparator 1
    if (aQueue != -1 && bQueue != -1) return@Comparator aQueue.compareTo(bQueue)

    val aSeed = seedTags.indexOf(a)
    val bSeed = seedTags.indexOf(b)
    if (aSeed != -1 && bSeed == -1) return@Comparator -1
    if (aSeed == -1 && bSeed != -1) return@Comparator 1
    if (aSeed != -1 && bSeed != -1) return@Comparator aSeed.compareTo(bSeed)

    a.compareTo(b, ignoreCase = true)
}

internal fun getTagGroups(isEnglish: Boolean) = if (isEnglish) TAG_GROUPS_EN else TAG_GROUPS_FR

internal fun translateTag(tag: String, toEnglish: Boolean): String {
    val idxFr = DEFAULT_TAGS_FR.indexOf(tag)
    if (idxFr != -1) return if (toEnglish) DEFAULT_TAGS_EN[idxFr] else DEFAULT_TAGS_FR[idxFr]
    val idxEn = DEFAULT_TAGS_EN.indexOf(tag)
    if (idxEn != -1) return if (toEnglish) DEFAULT_TAGS_EN[idxEn] else DEFAULT_TAGS_FR[idxEn]
    return tag
}

internal fun rankNameFor(rs: Int, globalRank: Int? = null): String {
    val base = RANK_TIERS.lastOrNull { rs >= it.first }?.second ?: "Bronze 4"
    if (base == "Ruby") {
        return if (globalRank != null && globalRank <= 500) "Ruby" else "Diamond 1"
    }
    return base
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

internal fun rankLogoResFor(rs: Int, globalRank: Int? = null): Int {
    val name = rankNameFor(rs, globalRank)
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

internal fun getProgressToNextRank(rs: Int, globalRank: Int? = null): Triple<Int, Int, String>? {
    val currentName = rankNameFor(rs, globalRank)
    if (currentName == "Ruby") return null
    val idx = RANK_TIERS.indexOfLast { rs >= it.first }
    if (idx == -1 || idx + 1 >= RANK_TIERS.size) return null
    val current = RANK_TIERS[idx]
    val next = RANK_TIERS[idx + 1]

    if (next.second == "Ruby" && (globalRank == null || globalRank > 500)) return null

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
        val dayOfWeek = java.util.Calendar.getInstance().apply { timeInMillis = entries[i].timestamp }.get(java.util.Calendar.DAY_OF_WEEK)
        val day = (dayOfWeek + 5) % 7
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

// Vérification de mise à jour GitHub dynamique
internal suspend fun checkGithubUpdate(context: Context): String? = withContext(Dispatchers.IO) {
    try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersion = packageInfo.versionName?.replace("v", "") ?: "1.0.0"

        val url = URL("https://api.github.com/repos/Delta300IQ/THE_FINALS_RANK_TRACKER_ANDROID/releases/latest")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "FinalsRankTracker")

        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val githubVersion = json.getString("tag_name").replace("v", "").trim()

            val vLocal = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val vGithub = githubVersion.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(vLocal.size, vGithub.size)) {
                val numLocal = vLocal.getOrElse(i) { 0 }
                val numGithub = vGithub.getOrElse(i) { 0 }
                if (numGithub > numLocal) return@withContext githubVersion
                if (numGithub < numLocal) return@withContext null
            }
        }
    } catch (e: Exception) {}
    return@withContext null
}

// Extraction du JSON DavG25 (Supporte Saisons 1 à 11 ET format direct 1 saison)
internal fun parseDavG25JsonMultipleSeasons(jsonStr: String): ParsedDavG25Data {
    try {
        val root = JSONObject(jsonStr)
        if (root.has("error")) {
            return ParsedDavG25Data(false, root.getString("error"), null, null, null, emptyMap())
        }

        val playerObj = root.optJSONObject("player") ?: root.optJSONObject("stats")
        val name = playerObj?.optString("id") ?: playerObj?.optString("name")
        val globalRank = playerObj?.optInt("rank")
        val rankChange = if (playerObj != null && playerObj.has("rankChange")) playerObj.optInt("rankChange") else playerObj?.optInt("change")

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val sdfFallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

        val resultMap = mutableMapOf<Int, List<RankEntry>>()

        fun parseArray(arr: JSONArray): List<RankEntry> {
            val entries = mutableListOf<RankEntry>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val rs = item.optInt("points", -1)
                if (rs == -1) continue

                val dateStr = item.optString("timestamp")
                var timestamp = System.currentTimeMillis()
                try {
                    timestamp = sdf.parse(dateStr)?.time ?: timestamp
                } catch (e: Exception) {
                    try { timestamp = sdfFallback.parse(dateStr)?.time ?: timestamp } catch(e2: Exception) {}
                }

                entries.add(RankEntry(rank = rs, timestamp = timestamp, notes = listOf("Leaderboard Import")))
            }
            val sorted = entries.distinctBy { it.timestamp }.sortedBy { it.timestamp }
            val filtered = mutableListOf<RankEntry>()
            for (e in sorted) {
                if (filtered.isEmpty() || filtered.last().rank != e.rank) {
                    filtered.add(e)
                }
            }
            return filtered
        }

        // Cas 1 : Format multi-saisons { "seasons": { "11": [...], "10": [...] } }
        val seasonsObj = root.optJSONObject("seasons")
        if (seasonsObj != null) {
            val keys = seasonsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val seasonNum = k.toIntOrNull() ?: continue

                // On importe toutes les saisons (1 à 20 pour compatibilité future)
                if (seasonNum in 1..20) {
                    val arr = seasonsObj.getJSONArray(k)
                    val filteredList = parseArray(arr)
                    if (filteredList.isNotEmpty()) resultMap[seasonNum] = filteredList
                }
            }
        }
        // Cas 2 : Format saison unique direct { "history": [...] }
        else if (root.has("history")) {
            val arr = root.getJSONArray("history")
            val filteredList = parseArray(arr)
            // Utiliser la saison la plus récente connue, ou 11 par défaut
            val currentSeason = resultMap.keys.maxOrNull() ?: 11
            if (filteredList.isNotEmpty()) resultMap[currentSeason] = filteredList
        }

        if (resultMap.isEmpty()) {
            return ParsedDavG25Data(false, "Aucune donnée de saison (1 à 20) trouvée.", name, globalRank, rankChange, emptyMap())
        }

        return ParsedDavG25Data(true, null, name, globalRank, rankChange, resultMap)
    } catch(e: Exception) {
        return ParsedDavG25Data(false, e.message, null, null, null, emptyMap())
    }
}

// Préférences
internal const val PREFS_NAME = "finals_rank_tracker"
internal const val KEY_SEASONS = "seasons_json_v2"
internal const val KEY_SEASONS_BACKUP = "seasons_json_backup"
internal const val KEY_ENTRIES_LEGACY = "entries_json"
internal const val KEY_DARK_MODE = "dark_mode"
internal const val KEY_LANGUAGE = "is_english"
internal const val KEY_SELECTED_SEASON = "selected_season"
internal const val KEY_RANK_GOALS = "rank_goals_v1"
internal const val KEY_CUSTOM_TAGS = "custom_tags"
internal const val KEY_HIDDEN_STATS_TAGS = "hidden_stats_tags"
internal const val KEY_LAST_EXPORT_TIMESTAMP = "last_export_timestamp"
internal const val KEY_EXPORT_REMINDER_DISMISS_TIMESTAMP = "export_reminder_dismiss_timestamp"
internal const val KEY_PLAYER_PROFILE = "player_profile_v2"
internal const val KEY_DISABLE_SPLASH = "disable_splash"
internal const val KEY_AUTO_UPDATE = "auto_update_startup"
internal const val KEY_CHART_TYPE = "chart_type"
internal const val KEY_LAST_GITHUB_CHECK = "last_github_check"
internal const val KEY_SNOOZE_GITHUB = "snooze_github"

internal fun loadPlayerProfile(context: Context): PlayerProfile? {
    val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PLAYER_PROFILE, null) ?: return null
    return try {
        val obj = JSONObject(json)
        PlayerProfile(
            name = obj.getString("name"),
            globalRank = if (obj.has("globalRank")) obj.getInt("globalRank") else null,
            rankChange = if (obj.has("rankChange")) obj.getInt("rankChange") else null
        )
    } catch (e: Exception) { null }
}

internal fun savePlayerProfile(context: Context, profile: PlayerProfile?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    if (profile == null) {
        prefs.remove(KEY_PLAYER_PROFILE).apply()
    } else {
        val obj = JSONObject()
        obj.put("name", profile.name)
        profile.globalRank?.let { obj.put("globalRank", it) }
        profile.rankChange?.let { obj.put("rankChange", it) }
        prefs.putString(KEY_PLAYER_PROFILE, obj.toString()).apply()
    }
}

internal fun loadDisableSplash(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_DISABLE_SPLASH, false)
internal fun saveDisableSplash(context: Context, b: Boolean) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_DISABLE_SPLASH, b).apply()
internal fun loadAutoUpdate(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_UPDATE, false)
internal fun saveAutoUpdate(context: Context, b: Boolean) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_AUTO_UPDATE, b).apply()
internal fun loadChartType(context: Context): ChartType {
    val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_CHART_TYPE, ChartType.CANDLESTICK.name)
    return ChartType.valueOf(name!!)
}
internal fun saveChartType(context: Context, t: ChartType) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_CHART_TYPE, t.name).apply()
internal fun loadLastGithubCheck(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_LAST_GITHUB_CHECK, 0L)
internal fun saveLastGithubCheck(context: Context, t: Long) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_GITHUB_CHECK, t).apply()
internal fun loadSnoozeGithub(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_SNOOZE_GITHUB, 0L)
internal fun saveSnoozeGithub(context: Context, t: Long) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putLong(KEY_SNOOZE_GITHUB, t).apply()
internal fun loadLastExportTimestamp(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_LAST_EXPORT_TIMESTAMP, 0L)
internal fun saveLastExportTimestamp(context: Context, ts: Long) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_EXPORT_TIMESTAMP, ts).apply()

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

internal fun loadHiddenStatsTags(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_HIDDEN_STATS_TAGS, emptySet()) ?: emptySet()
}

internal fun saveHiddenStatsTags(context: Context, tags: Set<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(KEY_HIDDEN_STATS_TAGS, tags).apply()
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
        val obj = JSONObject(json)
        val res = mutableMapOf<Int, Int>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val s = k.toIntOrNull()
            if (s != null) res[s] = obj.getInt(k)
        }
        res
    } catch (e: Exception) { emptyMap() }
}

internal fun saveRankGoals(context: Context, goals: Map<Int, Int>) {
    val obj = JSONObject()
    goals.forEach { (s, g) -> obj.put(s.toString(), g) }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_RANK_GOALS, obj.toString()).apply()
}

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

internal fun exportDatabaseToJson(
    allSeasons: Map<Int, List<RankEntry>>,
    profile: PlayerProfile?,
    goals: Map<Int, Int>,
    customTags: List<String>
): String {
    val root = JSONObject()
    
    val seasonsObj = JSONObject()
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
        seasonsObj.put(season.toString(), arr)
    }
    root.put("seasons", seasonsObj)
    
    if (profile != null) {
        val pObj = JSONObject()
        pObj.put("name", profile.name)
        profile.globalRank?.let { pObj.put("globalRank", it) }
        profile.rankChange?.let { pObj.put("rankChange", it) }
        root.put("profile", pObj)
    }
    
    val goalsObj = JSONObject()
    goals.forEach { (s, g) -> goalsObj.put(s.toString(), g) }
    root.put("goals", goalsObj)
    
    val tagsArr = JSONArray()
    customTags.forEach { tagsArr.put(it) }
    root.put("customTags", tagsArr)
    
    return root.toString(4)
}

internal data class ParsedDatabase(
    val seasons: Map<Int, List<RankEntry>>,
    val profile: PlayerProfile?,
    val goals: Map<Int, Int>,
    val customTags: List<String>
)

internal fun importDatabaseFromJson(jsonStr: String): ParsedDatabase? {
    try {
        val root = JSONObject(jsonStr)
        
        val seasonsResult = mutableMapOf<Int, List<RankEntry>>()
        if (root.has("seasons")) {
            val sObj = root.getJSONObject("seasons")
            val keys = sObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val season = k.toIntOrNull()
                if (season != null) {
                    val arr = sObj.getJSONArray(k)
                    val list = (0 until arr.length()).map { i ->
                        val item = arr.getJSONObject(i)
                        val notes = if (item.has("notes")) {
                            val notesArr = item.getJSONArray("notes")
                            (0 until notesArr.length()).map { notesArr.getString(it) }
                        } else {
                            emptyList()
                        }
                        RankEntry(item.getInt("rank"), item.getLong("ts"), notes)
                    }
                    seasonsResult[season] = list
                }
            }
        }
        
        var profile: PlayerProfile? = null
        if (root.has("profile")) {
            val pObj = root.getJSONObject("profile")
            profile = PlayerProfile(
                name = pObj.getString("name"),
                globalRank = if (pObj.has("globalRank")) pObj.getInt("globalRank") else null,
                rankChange = if (pObj.has("rankChange")) pObj.getInt("rankChange") else null
            )
        }
        
        val goals = mutableMapOf<Int, Int>()
        if (root.has("goals")) {
            val gObj = root.getJSONObject("goals")
            val keys = gObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                k.toIntOrNull()?.let { goals[it] = gObj.getInt(k) }
            }
        }
        
        val tags = mutableListOf<String>()
        if (root.has("customTags")) {
            val tArr = root.getJSONArray("customTags")
            for (i in 0 until tArr.length()) {
                tags.add(tArr.getString(i))
            }
        }
        
        return ParsedDatabase(seasonsResult, profile, goals, tags)
    } catch (e: Exception) {
        return null
    }
}