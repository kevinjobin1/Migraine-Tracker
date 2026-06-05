package com.example

import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.airbnb.lottie.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.blur
import android.app.DatePickerDialog
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject


// Custom theme specification holder
data class CustomThemeSpecs(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerLow: Color,
    val onPrimary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val accent: Color,
    val fontFamily: FontFamily,
    val cardShape: RoundedCornerShape,
    val buttonShape: RoundedCornerShape,
    val displayName: String
)

enum class MyHapticStyle {
    LIGHT, MEDIUM, HEAVY, SUCCESS
}

fun triggerHaptic(context: Context, style: MyHapticStyle) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (style) {
                MyHapticStyle.LIGHT -> VibrationEffect.createOneShot(15, 60)
                MyHapticStyle.MEDIUM -> VibrationEffect.createOneShot(30, 110)
                MyHapticStyle.HEAVY -> VibrationEffect.createOneShot(55, 200)
                MyHapticStyle.SUCCESS -> VibrationEffect.createWaveform(
                    longArrayOf(0, 20, 60, 25),
                    intArrayOf(0, 80, 0, 130),
                    -1
                )
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            when (style) {
                MyHapticStyle.LIGHT -> vibrator.vibrate(15)
                MyHapticStyle.MEDIUM -> vibrator.vibrate(30)
                MyHapticStyle.HEAVY -> vibrator.vibrate(55)
                MyHapticStyle.SUCCESS -> {
                    vibrator.vibrate(25)
                    Thread.sleep(60)
                    vibrator.vibrate(35)
                }
            }
        }
    } catch (e: Exception) {
        // Fallback or ignore
    }
}

fun Modifier.onSwipeLeftOrRight(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier = this.pointerInput(Unit) {
    var accumulatedDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { accumulatedDrag = 0f },
        onDragEnd = {
            if (accumulatedDrag < -100f) { // Swiped left (next)
                onSwipeLeft()
            } else if (accumulatedDrag > 100f) { // Swiped right (previous)
                onSwipeRight()
            }
        },
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            accumulatedDrag += dragAmount
        }
    )
}

fun getThemeSpecs(themeId: String): CustomThemeSpecs {
    return when (themeId) {
        "soothing_night" -> CustomThemeSpecs(
            primary = Color(0xFF90B5E0), // Gentle, readable blue
            background = Color(0xFF0B0F19), // Deep calming night sky to reduce back-glow
            surface = Color(0xFF172033), // Soft matte gray-blue card surface
            surfaceContainer = Color(0xFF1E293B),
            surfaceContainerLow = Color(0xFF131B2E),
            onPrimary = Color(0xFF031B2E),
            onSurface = Color(0xFFE2E8F0), // Clear high-contrast dimmed silver body (no visual stress)
            onSurfaceVariant = Color(0xFF94A3B8), // Perfectly legible slate variant (exceeds 4.5:1)
            accent = Color(0xFFB0CEF5), // Serene ocean blue accent
            fontFamily = FontFamily.SansSerif,
            cardShape = RoundedCornerShape(8.dp),
            buttonShape = RoundedCornerShape(4.dp),
            displayName = "Soothing Night"
        )
        "forest_breath" -> CustomThemeSpecs(
            primary = Color(0xFF81C784), // Gentle sage green
            background = Color(0xFF0C140F), // Deep forest moss (extremely low glaring)
            surface = Color(0xFF1B2A1E), // Soft dark forest-green surface
            surfaceContainer = Color(0xFF233627),
            surfaceContainerLow = Color(0xFF132016),
            onPrimary = Color(0xFF061408),
            onSurface = Color(0xFFE8F5E9), // Highly readable off-white mint
            onSurfaceVariant = Color(0xFFA5D6A7), // Soft mint slate details
            accent = Color(0xFFA5D6A7), // Deep calming green accent
            fontFamily = FontFamily.SansSerif,
            cardShape = RoundedCornerShape(8.dp),
            buttonShape = RoundedCornerShape(4.dp),
            displayName = "Forest Breath"
        )
        "cosmic_lavender" -> CustomThemeSpecs(
            primary = Color(0xFFD1C4E9), // Gentle cosmic pale lavender
            background = Color(0xFF0D0B18), // Deep obsidian cosmic space back-glow
            surface = Color(0xFF1C132E), // Soft dark violet-indigo card surface
            surfaceContainer = Color(0xFF281C3F),
            surfaceContainerLow = Color(0xFF130E22),
            onPrimary = Color(0xFF1B003A),
            onSurface = Color(0xFFF3E5F5), // High-contrast amethyst silver body text
            onSurfaceVariant = Color(0xFFB39DDB), // Legible dusty lavender variations
            accent = Color(0xFFE040FB), // Electric violet/magenta highlight
            fontFamily = FontFamily.SansSerif,
            cardShape = RoundedCornerShape(8.dp),
            buttonShape = RoundedCornerShape(4.dp),
            displayName = "Cosmic Lavender"
        )
        "warm_terracotta" -> CustomThemeSpecs(
            primary = Color(0xFFD84315), // Deep terracotta orange
            background = Color(0xFFFBE9E7), // Cozy desert sand-orange minimal emission backdrop
            surface = Color(0xFFFFCCBC), // Light toasted almond card
            surfaceContainer = Color(0xFFFFAB91),
            surfaceContainerLow = Color(0xFFFBE9E7),
            onPrimary = Color(0xFFFFFFFF),
            onSurface = Color(0xFF3E2723), // Crisp comfortable deep bean brown typography
            onSurfaceVariant = Color(0xFF5D4037), // Clean readable cocoa secondary details
            accent = Color(0xFFD84315), // Earthy terracotta action highlight
            fontFamily = FontFamily.SansSerif,
            cardShape = RoundedCornerShape(8.dp),
            buttonShape = RoundedCornerShape(4.dp),
            displayName = "Warm Terracotta"
        )
        else -> CustomThemeSpecs( // "misty_day" as fallback/default Light
            primary = Color(0xFF1E293B), // High-contrast deep slate navy
            background = Color(0xFFE2E8F0), // Low-emission cool gray to avoid harsh white screen glare
            surface = Color(0xFFF1F5F9), // Very soft slate-grey card with comfortable contrast
            surfaceContainer = Color(0xFFCFD8DC),
            surfaceContainerLow = Color(0xFFE2E8F0),
            onPrimary = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A), // Crisp navy-slate elements (excellent readability > 14:1)
            onSurfaceVariant = Color(0xFF334155), // Highly eligible dark slate details
            accent = Color(0xFF2B5C8F), // Deep soothing ocean blue
            fontFamily = FontFamily.SansSerif,
            cardShape = RoundedCornerShape(8.dp),
            buttonShape = RoundedCornerShape(4.dp),
            displayName = "Misty Day"
        )
    }
}

// Compose UI composition helper for active theme specifications
val LocalAppTheme = staticCompositionLocalOf {
    getThemeSpecs("misty_day")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = MigraineRepository(database.migraineDao())
        val factory = MigraineViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[MigraineViewModel::class.java]

        setContent {
            val context = LocalContext.current
            val savedTheme = remember {
                context.getSharedPreferences("migraine_settings", android.content.Context.MODE_PRIVATE)
                    .getString("theme", "auto") ?: "auto"
            }
            LaunchedEffect(savedTheme) {
                viewModel.setAppTheme(savedTheme)
            }
            val savedLang = remember {
                context.getSharedPreferences("migraine_settings", android.content.Context.MODE_PRIVATE)
                    .getString("language", "en") ?: "en"
            }
            LaunchedEffect(savedLang) {
                viewModel.setAppLanguage(savedLang)
            }

            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val isDark = isSystemInDarkTheme()

            val themeId = when (appTheme) {
                "auto" -> if (isDark) "soothing_night" else "misty_day"
                else -> appTheme
            }
            val themeSpecs = getThemeSpecs(themeId)

            CompositionLocalProvider(LocalAppTheme provides themeSpecs) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = themeSpecs.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    var showLanguagePicker by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        MigraineTrackerApp(viewModel = viewModel)

                        if (showLanguagePicker) {
                            val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
                            LanguageSelectionOverlay(
                                theme = themeSpecs,
                                currentLang = lang,
                                onLanguageChosen = { chosenLang ->
                                    viewModel.setAppLanguage(chosenLang)
                                    context.getSharedPreferences("migraine_settings", android.content.Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("language", chosenLang)
                                        .putBoolean("language_selected", true)
                                        .apply()
                                    showLanguagePicker = false
                                    
                                    val seen = context.getSharedPreferences("migraine_settings", android.content.Context.MODE_PRIVATE)
                                        .getBoolean("tutorial_seen", false)
                                    if (!seen) {
                                        viewModel.setOnboardingActive(true)
                                    }
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = showSplash,
                            enter = fadeIn(),
                            exit = fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 1.15f, animationSpec = tween(500, easing = FastOutSlowInEasing))
                        ) {
                            val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
                            MigraineSplashScreen(
                                onDismiss = { 
                                    showSplash = false 
                                    val langSelected = context.getSharedPreferences("migraine_settings", android.content.Context.MODE_PRIVATE)
                                        .getBoolean("language_selected", false)
                                    if (!langSelected) {
                                        showLanguagePicker = true
                                    } else {
                                        val seen = context.getSharedPreferences("migraine_settings", android.content.Context.MODE_PRIVATE)
                                            .getBoolean("tutorial_seen", false)
                                        if (!seen) {
                                            viewModel.setOnboardingActive(true)
                                        }
                                    }
                                },
                                theme = themeSpecs,
                                lang = lang
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class AppScreen {
    DASHBOARD, HISTORY, EXPORT, SETTINGS
}

// ViewModel to manage UI state, Calendar selection, and Room logs
class MigraineViewModel(private val repository: MigraineRepository) : ViewModel() {

    private val _appTheme = MutableStateFlow("auto")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    fun setAppTheme(theme: String) {
        _appTheme.value = theme
    }

    private val _currentCalendar = MutableStateFlow(Calendar.getInstance())
    val currentCalendar: StateFlow<Calendar> = _currentCalendar.asStateFlow()

    private val _selectedLogDate = MutableStateFlow<String?>(null) // Format: "YYYY-MM-DD"
    val selectedLogDate: StateFlow<String?> = _selectedLogDate.asStateFlow()

    private val _showAllEpisodes = MutableStateFlow(false)
    val showAllEpisodes: StateFlow<Boolean> = _showAllEpisodes.asStateFlow()

    private val _appLanguage = MutableStateFlow("en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
    }

    private val _isOnboardingActive = MutableStateFlow(false)
    val isOnboardingActive: StateFlow<Boolean> = _isOnboardingActive.asStateFlow()

    fun setOnboardingActive(active: Boolean) {
        _isOnboardingActive.value = active
    }

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun setCurrentScreen(screen: AppScreen) {
        _currentScreen.value = screen
        _showAllEpisodes.value = (screen == AppScreen.HISTORY)
    }

    fun setShowAllEpisodes(show: Boolean) {
        _showAllEpisodes.value = show
        _currentScreen.value = if (show) AppScreen.HISTORY else AppScreen.DASHBOARD
    }

    // Temporary editor inputs
    val editorNote = MutableStateFlow("")
    val editorIntensity = MutableStateFlow(5)
    val editorSymptoms = MutableStateFlow<Set<String>>(emptySet())
    val editorTriggers = MutableStateFlow<Set<String>>(emptySet())
    val isEditorVisible = MutableStateFlow(false)
    val isReadOnly = MutableStateFlow(true)

    // Log selected for editing directly (nullable)
    private val _editingLogId = MutableStateFlow<Long?>(null)
    val editingLogId: StateFlow<Long?> = _editingLogId.asStateFlow()

    // Flow of all logs registered in the database
    val allLogs: StateFlow<List<MigraineLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate with typical screenshot mock-data on first launch
        viewModelScope.launch {
            repository.allLogs.first().let { logs ->
                if (logs.isEmpty()) {
                    repository.insertLog(
                        MigraineLog(
                            dateString = "2026-01-24",
                            year = 2026,
                            month = 1,
                            day = 24,
                            note = "Feeling a bit of pressure in the temples...",
                            intensity = 4,
                            symptoms = "Temple Pressure",
                            triggers = "Stress"
                        )
                    )
                    repository.insertLog(
                        MigraineLog(
                            dateString = "2026-01-12",
                            year = 2026,
                            month = 1,
                            day = 12,
                            note = "Standard aura preceded the headache. Took medication...",
                            intensity = 7,
                            symptoms = "Aura, Light Sensitivity",
                            triggers = "Lack of Sleep"
                        )
                    )
                }
            }
        }
    }

    fun navigateMonth(offset: Int) {
        val nextCal = Calendar.getInstance().apply {
            time = _currentCalendar.value.time
            add(Calendar.MONTH, offset)
        }
        _currentCalendar.value = nextCal
    }

    fun navigateWeek(offset: Int) {
        val nextCal = Calendar.getInstance().apply {
            time = _currentCalendar.value.time
            add(Calendar.WEEK_OF_YEAR, offset)
        }
        _currentCalendar.value = nextCal
    }

    fun navigateDay(offset: Int) {
        val nextCal = Calendar.getInstance().apply {
            time = _currentCalendar.value.time
            add(Calendar.DAY_OF_YEAR, offset)
        }
        _currentCalendar.value = nextCal
    }

    fun navigateYear(offset: Int) {
        val nextCal = Calendar.getInstance().apply {
            time = _currentCalendar.value.time
            add(Calendar.YEAR, offset)
        }
        _currentCalendar.value = nextCal
    }

    fun selectDateForLog(dateString: String, existingLog: MigraineLog?) {
        _selectedLogDate.value = dateString
        isReadOnly.value = true
        if (existingLog != null) {
            _editingLogId.value = existingLog.id
            editorNote.value = existingLog.note
            editorIntensity.value = existingLog.intensity
            editorSymptoms.value = existingLog.symptoms.split(",").filter { it.isNotBlank() }.toSet()
            editorTriggers.value = existingLog.triggers.split(",").filter { it.isNotBlank() }.toSet()
            isEditorVisible.value = true
        } else {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val yr = parts[0].toInt()
                val mo = parts[1].toInt()
                val dy = parts[2].toInt()
                viewModelScope.launch {
                    val log = MigraineLog(
                        dateString = dateString,
                        year = yr,
                        month = mo,
                        day = dy,
                        note = "",
                        intensity = 5,
                        symptoms = "",
                        triggers = ""
                    )
                    val newId = repository.insertLog(log)
                    _editingLogId.value = newId
                    editorNote.value = ""
                    editorIntensity.value = 5
                    editorSymptoms.value = emptySet()
                    editorTriggers.value = emptySet()
                    isEditorVisible.value = true
                }
            }
        }
    }

    fun toggleSymptom(symptom: String) {
        val current = editorSymptoms.value
        editorSymptoms.value = if (current.contains(symptom)) {
            current - symptom
        } else {
            current + symptom
        }
    }

    fun toggleTrigger(trigger: String) {
        val current = editorTriggers.value
        editorTriggers.value = if (current.contains(trigger)) {
            current - trigger
        } else {
            current + trigger
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLogById(id)
            if (_editingLogId.value == id) {
                isEditorVisible.value = false
            }
        }
    }

    fun saveActiveNote() {
        val dateStr = _selectedLogDate.value ?: return
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val yr = parts[0].toInt()
            val mo = parts[1].toInt()
            val dy = parts[2].toInt()

            viewModelScope.launch {
                val log = MigraineLog(
                    id = _editingLogId.value ?: 0,
                    dateString = dateStr,
                    year = yr,
                    month = mo,
                    day = dy,
                    note = editorNote.value,
                    intensity = editorIntensity.value,
                    symptoms = editorSymptoms.value.joinToString(","),
                    triggers = editorTriggers.value.joinToString(",")
                )
                repository.insertLog(log)
                isEditorVisible.value = false
            }
        }
    }

    fun closeEditor() {
        isEditorVisible.value = false
    }
}

class MigraineViewModelFactory(private val repository: MigraineRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MigraineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MigraineViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

enum class CalendarDisplayMode {
    MONTH, WEEK, DAY, YEAR
}

// Pre-defined available tracker selections
val SYMPTOM_LIST = listOf("Aura", "Temple Pressure", "Nausea", "Light Sensitivity", "Sound Sensitivity", "Throbbing Pain", "Blurred Vision")
val TRIGGER_LIST = listOf("Lack of Sleep", "Bright Lights", "Stress", "Caffeine", "Skipped Meal", "Weather Change", "Dehydration")

// Primary Screen Component
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MigraineTrackerApp(viewModel: MigraineViewModel) {
    val theme = LocalAppTheme.current
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val currentCal by viewModel.currentCalendar.collectAsStateWithLifecycle()
    val editorVisible by viewModel.isEditorVisible.collectAsStateWithLifecycle()
    val activeSLogDate by viewModel.selectedLogDate.collectAsStateWithLifecycle()
    val showAllEpisodes by viewModel.showAllEpisodes.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val savedLang = remember {
        context.getSharedPreferences("migraine_settings", android.content.Context.MODE_PRIVATE)
            .getString("language", "en") ?: "en"
    }
    LaunchedEffect(savedLang) {
        viewModel.setAppLanguage(savedLang)
    }

    var calendarDisplayMode by remember { mutableStateOf(CalendarDisplayMode.MONTH) }

    val currentMonth = currentCal.get(Calendar.MONTH) + 1
    val currentYear = currentCal.get(Calendar.YEAR)

    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    // Filter logs for calendar indicators in the current month
    val logsThisMonth = allLogs.filter { it.year == currentYear && it.month == currentMonth }
    val loggedDaysSet = remember(logsThisMonth) { logsThisMonth.map { it.day }.toSet() }

    // Search and filter state variables for All Episodes list
    var searchQuery by remember { mutableStateOf("") }
    var selectedSymptomFilters by remember { mutableStateOf(emptySet<String>()) }
    var startDateFilter by remember { mutableStateOf<String?>(null) }
    var endDateFilter by remember { mutableStateOf<String?>(null) }
    var filterPanelExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val parseDate: (String) -> Date? = remember {
        { dateStr ->
            try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) } catch (e: Exception) { null }
        }
    }

    val filteredLogs = remember(allLogs, searchQuery, selectedSymptomFilters, startDateFilter, endDateFilter) {
        allLogs.filter { log ->
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                log.note.contains(searchQuery, ignoreCase = true) ||
                log.symptoms.contains(searchQuery, ignoreCase = true) ||
                log.triggers.contains(searchQuery, ignoreCase = true) ||
                log.dateString.contains(searchQuery, ignoreCase = true)
            }

            val matchesSymptoms = if (selectedSymptomFilters.isEmpty()) {
                true
            } else {
                val logSymptoms = log.symptoms.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
                selectedSymptomFilters.any { it.trim().lowercase() in logSymptoms }
            }

            val matchesDateRange = run {
                val logDate = parseDate(log.dateString)
                if (logDate == null) {
                    true
                } else {
                    val start = startDateFilter?.let { parseDate(it) }
                    val end = endDateFilter?.let { parseDate(it) }
                    when {
                        start != null && end != null -> !logDate.before(start) && !logDate.after(end)
                        start != null -> !logDate.before(start)
                        end != null -> !logDate.after(end)
                        else -> true
                    }
                }
            }

            matchesQuery && matchesSymptoms && matchesDateRange
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (editorVisible) 10.dp else 0.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = theme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = theme.surfaceContainer,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.DASHBOARD,
                        onClick = {
                            triggerHaptic(context, MyHapticStyle.LIGHT)
                            viewModel.setCurrentScreen(AppScreen.DASHBOARD)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = Translate.t("dashboard_tab", lang)
                            )
                        },
                        label = {
                            Text(
                                text = Translate.t("dashboard_tab", lang),
                                fontFamily = theme.fontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.primary,
                            selectedTextColor = theme.primary,
                            indicatorColor = theme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = theme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = theme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HISTORY,
                        onClick = {
                            triggerHaptic(context, MyHapticStyle.LIGHT)
                            viewModel.setCurrentScreen(AppScreen.HISTORY)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = Translate.t("history_tab", lang)
                            )
                        },
                        label = {
                            Text(
                                text = Translate.t("history_tab", lang),
                                fontFamily = theme.fontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.primary,
                            selectedTextColor = theme.primary,
                            indicatorColor = theme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = theme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = theme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.EXPORT,
                        onClick = {
                            triggerHaptic(context, MyHapticStyle.LIGHT)
                            viewModel.setCurrentScreen(AppScreen.EXPORT)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = Translate.t("export_tab", lang)
                            )
                        },
                        label = {
                            Text(
                                text = Translate.t("export_tab", lang),
                                fontFamily = theme.fontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.primary,
                            selectedTextColor = theme.primary,
                            indicatorColor = theme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = theme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = theme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.SETTINGS,
                        onClick = {
                            triggerHaptic(context, MyHapticStyle.LIGHT)
                            viewModel.setCurrentScreen(AppScreen.SETTINGS)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = Translate.t("settings_tab", lang)
                            )
                        },
                        label = {
                            Text(
                                text = Translate.t("settings_tab", lang),
                                fontFamily = theme.fontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = theme.primary,
                            selectedTextColor = theme.primary,
                            indicatorColor = theme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = theme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = theme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        val direction = if (initialState.ordinal < targetState.ordinal) {
                            AnimatedContentTransitionScope.SlideDirection.Left
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.Right
                        }
                        (slideIntoContainer(direction, animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(400)))
                            .togetherWith(slideOutOfContainer(direction, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(300)))
                    },
                    label = "ScreenTransition"
                ) { targetScreen ->
                    when (targetScreen) {
                AppScreen.HISTORY -> {
                // FULL SCREEN VIEW OF ALL MIGRAINE EPISODES AND NOTES RECORDED
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Header layout with generous padding and 48dp target back button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setShowAllEpisodes(false) },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = Translate.t("back_to_dashboard", lang),
                                tint = theme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = Translate.t("all_episodes_title", lang),
                            fontFamily = theme.fontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (allLogs.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AppLottiePlayer(
                                url = "https://assets9.lottiefiles.com/packages/lf20_myejio3g.json",
                                modifier = Modifier
                                    .size(180.dp)
                                    .padding(bottom = 16.dp),
                                fallback = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = Translate.t("desc_no_journals", lang),
                                        tint = theme.primary.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .size(64.dp)
                                            .padding(bottom = 16.dp)
                                    )
                                }
                            )
                            Text(
                                text = Translate.t("no_journals_logged_yet", lang),
                                fontFamily = theme.fontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = theme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // 1. Search Bar & Filter Panel Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            // Search Row containing OutlinedTextField and expand toggle button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text(
                                            text = Translate.t("search_placeholder", lang),
                                            fontFamily = theme.fontFamily,
                                            fontSize = 13.sp,
                                            color = theme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = Translate.t("desc_search", lang),
                                            tint = theme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = { searchQuery = "" },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = Translate.t("desc_clear_search", lang),
                                                    tint = theme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = theme.cardShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = theme.surfaceContainerLow,
                                        unfocusedContainerColor = theme.surfaceContainerLow,
                                        focusedBorderColor = theme.primary,
                                        unfocusedBorderColor = theme.onSurfaceVariant.copy(alpha = 0.2f),
                                        focusedTextColor = theme.onSurface,
                                        unfocusedTextColor = theme.onSurface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Filter options toggle button
                                val activeFiltersCount = (if (selectedSymptomFilters.isNotEmpty()) 1 else 0) +
                                                        (if (startDateFilter != null || endDateFilter != null) 1 else 0)

                                Box(contentAlignment = Alignment.TopEnd) {
                                    IconButton(
                                        onClick = { filterPanelExpanded = !filterPanelExpanded },
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(theme.cardShape)
                                            .background(
                                                if (filterPanelExpanded || activeFiltersCount > 0) theme.primary.copy(alpha = 0.15f)
                                                else theme.surfaceContainerLow
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (filterPanelExpanded) theme.primary else theme.onSurfaceVariant.copy(alpha = 0.15f),
                                                shape = theme.cardShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = Translate.t("desc_filters", lang),
                                            tint = if (filterPanelExpanded || activeFiltersCount > 0) theme.primary else theme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    if (activeFiltersCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = 2.dp, y = (-2).dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(theme.accent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = activeFiltersCount.toString(),
                                                fontFamily = theme.fontFamily,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Expandable detailed filters panel
                            AnimatedVisibility(
                                visible = filterPanelExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .clip(theme.cardShape)
                                        .background(theme.surfaceContainerLow)
                                        .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), theme.cardShape)
                                        .padding(14.dp)
                                ) {
                                    // Symptom multi-select filter title
                                    Text(
                                        text = Translate.t("filter_by_symptom", lang),
                                        fontFamily = theme.fontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SYMPTOM_LIST.forEach { sym ->
                                            val isSelected = selectedSymptomFilters.contains(sym)
                                            val activeColor = theme.accent
                                            val chipStrokeColor = if (isSelected) activeColor else theme.onSurfaceVariant.copy(alpha = 0.15f)
                                            val chipBg = if (isSelected) activeColor.copy(alpha = 0.1f) else Color.Transparent

                                            Box(
                                                modifier = Modifier
                                                    .border(width = 1.dp, color = chipStrokeColor, shape = theme.buttonShape)
                                                    .background(chipBg, shape = theme.buttonShape)
                                                    .clickable {
                                                        selectedSymptomFilters = if (isSelected) {
                                                            selectedSymptomFilters - sym
                                                        } else {
                                                            selectedSymptomFilters + sym
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = Translate.t("tag_" + sym.lowercase().replace(" ", "_"), lang),
                                                    fontFamily = theme.fontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) activeColor else theme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Date Range selection section
                                    Text(
                                        text = Translate.t("filter_by_daterange", lang),
                                        fontFamily = theme.fontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val context = LocalContext.current

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Start Date Button
                                        Button(
                                            onClick = {
                                                val currentCal = Calendar.getInstance()
                                                startDateFilter?.let {
                                                    parseDate(it)?.let { date -> currentCal.time = date }
                                                }
                                                DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        startDateFilter = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                                    },
                                                    currentCal.get(Calendar.YEAR),
                                                    currentCal.get(Calendar.MONTH),
                                                    currentCal.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            },
                                            shape = theme.buttonShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (startDateFilter != null) theme.primary.copy(alpha = 0.1f) else theme.surfaceContainer,
                                                contentColor = if (startDateFilter != null) theme.primary else theme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = Translate.t("desc_start_date", lang),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = startDateFilter ?: Translate.t("from_date", lang),
                                                fontFamily = theme.fontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = if (startDateFilter != null) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        Text(
                                            text = Translate.t("to", lang),
                                            fontFamily = theme.fontFamily,
                                            fontSize = 11.sp,
                                            color = theme.onSurfaceVariant
                                        )

                                        // End Date Button
                                        Button(
                                            onClick = {
                                                val currentCal = Calendar.getInstance()
                                                endDateFilter?.let {
                                                    parseDate(it)?.let { date -> currentCal.time = date }
                                                }
                                                DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        endDateFilter = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                                    },
                                                    currentCal.get(Calendar.YEAR),
                                                    currentCal.get(Calendar.MONTH),
                                                    currentCal.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            },
                                            shape = theme.buttonShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (endDateFilter != null) theme.primary.copy(alpha = 0.1f) else theme.surfaceContainer,
                                                contentColor = if (endDateFilter != null) theme.primary else theme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = Translate.t("desc_end_date", lang),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = endDateFilter ?: Translate.t("to_date", lang),
                                                fontFamily = theme.fontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = if (endDateFilter != null) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }

                                    // Quick preset selectors or Clear All
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Preset: Last 7 Days
                                        OutlinedButton(
                                            onClick = {
                                                val cal = Calendar.getInstance()
                                                val endStr = dateFormat.format(cal.time)
                                                cal.add(Calendar.DAY_OF_YEAR, -7)
                                                val startStr = dateFormat.format(cal.time)
                                                startDateFilter = startStr
                                                endDateFilter = endStr
                                            },
                                            shape = theme.buttonShape,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary)
                                        ) {
                                            Text(Translate.t("last_7d", lang), fontSize = 10.sp, fontFamily = theme.fontFamily)
                                        }

                                        // Preset: Last 30 Days
                                        OutlinedButton(
                                            onClick = {
                                                val cal = Calendar.getInstance()
                                                val endStr = dateFormat.format(cal.time)
                                                cal.add(Calendar.DAY_OF_YEAR, -30)
                                                val startStr = dateFormat.format(cal.time)
                                                startDateFilter = startStr
                                                endDateFilter = endStr
                                            },
                                            shape = theme.buttonShape,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary)
                                        ) {
                                            Text(Translate.t("last_30d", lang), fontSize = 10.sp, fontFamily = theme.fontFamily)
                                        }

                                        // Clear Preset
                                        if (startDateFilter != null || endDateFilter != null || selectedSymptomFilters.isNotEmpty() || searchQuery.isNotEmpty()) {
                                            Button(
                                                onClick = {
                                                    searchQuery = ""
                                                    selectedSymptomFilters = emptySet()
                                                    startDateFilter = null
                                                    endDateFilter = null
                                                },
                                                shape = theme.buttonShape,
                                                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(Translate.t("reset_all", lang), fontSize = 10.sp, fontFamily = theme.fontFamily, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Results Rendering or Empty Filtered State
                        if (filteredLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    AppLottiePlayer(
                                        url = "https://assets5.lottiefiles.com/packages/lf20_t9gkkhz4.json",
                                        modifier = Modifier
                                            .size(130.dp)
                                            .padding(bottom = 8.dp),
                                        fallback = {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = Translate.t("desc_no_results", lang),
                                                tint = theme.onSurfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = Translate.t("no_matching_episodes", lang),
                                        fontFamily = theme.fontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = Translate.t("adjust_filters_tip", lang),
                                        fontFamily = theme.fontFamily,
                                        fontSize = 13.sp,
                                        color = theme.onSurfaceVariant.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            searchQuery = ""
                                            selectedSymptomFilters = emptySet()
                                            startDateFilter = null
                                            endDateFilter = null
                                        },
                                        shape = theme.buttonShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                                    ) {
                                        Text(Translate.t("clear_filters_btn", lang), fontFamily = theme.fontFamily, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 120.dp)
                            ) {
                                items(filteredLogs, key = { it.id }) { log ->
                                val friendlyDate = try {
                                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(log.dateString)
                                    SimpleDateFormat("MMMM dd, yyyy", if (lang == "fr") Locale.FRENCH else Locale.US).format(date!!)
                                } catch (e: Exception) {
                                    log.dateString
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(theme.cardShape)
                                        .background(theme.surfaceContainerLow)
                                        .clickable {
                                            viewModel.selectDateForLog(log.dateString, log)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.deleteLog(log.id) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .padding(end = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = Translate.t("desc_delete_entry", lang),
                                            tint = theme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = friendlyDate,
                                                fontFamily = theme.fontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = theme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(theme.accent.copy(alpha = 0.2f))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${Translate.t("pain_scale_label", lang)} ${log.intensity}/10",
                                                    fontFamily = theme.fontFamily,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.accent
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = log.note.ifBlank { Translate.t("no_description_entered", lang) },
                                            fontFamily = theme.fontFamily,
                                            fontSize = 14.sp,
                                            color = theme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (log.symptoms.isNotBlank() || log.triggers.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val details = mutableListOf<String>()
                                            if (log.symptoms.isNotBlank()) {
                                                details.add("${Translate.t("symptom", lang)}: ${Translate.translateCsvTags(log.symptoms, lang)}")
                                            }
                                            if (log.triggers.isNotBlank()) {
                                                details.add("${Translate.t("trigger", lang)}: ${Translate.translateCsvTags(log.triggers, lang)}")
                                            }
                                            Text(
                                                text = details.joinToString(" | "),
                                                fontFamily = theme.fontFamily,
                                                fontSize = 11.sp,
                                                color = theme.onSurfaceVariant.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(theme.accent)
                                            .padding(start = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        AppScreen.EXPORT -> {
            ExportScreen(viewModel = viewModel)
        }
        AppScreen.SETTINGS -> {
            SettingsScreen(viewModel = viewModel)
        }
        AppScreen.DASHBOARD -> {
                // DASHBOARD SCREEN WITH LANDING CALENDAR AND HORIZONTAL RECENT EPISODES LIST
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Selector and Segmented Controls for Calendar displays
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp)
                            .clip(theme.cardShape)
                            .background(theme.surfaceContainerLow)
                            .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), theme.cardShape)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val modes = listOf(
                            CalendarDisplayMode.MONTH to Translate.t("calendar_mode_month", lang),
                            CalendarDisplayMode.WEEK to Translate.t("calendar_mode_week", lang),
                            CalendarDisplayMode.DAY to Translate.t("calendar_mode_day", lang),
                            CalendarDisplayMode.YEAR to Translate.t("calendar_mode_year", lang)
                        )
                        modes.forEach { (mode, label) ->
                            val isSelected = calendarDisplayMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(theme.buttonShape)
                                    .background(if (isSelected) theme.primary else Color.Transparent)
                                    .clickable {
                                        triggerHaptic(context, MyHapticStyle.LIGHT)
                                        calendarDisplayMode = mode
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = theme.fontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) theme.onPrimary else theme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Dynamically display selected Calendar View Mode
                    AnimatedContent(
                        targetState = calendarDisplayMode,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "CalendarViewTransition"
                    ) { targetMode ->
                        when (targetMode) {
                        CalendarDisplayMode.MONTH -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Month header section
                                Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val monthName = remember(currentCal, lang) {
                                    SimpleDateFormat("MMMM yyyy", if (lang == "fr") Locale.FRENCH else Locale.US).format(currentCal.time)
                                }

                                Text(
                                    text = monthName,
                                    fontFamily = theme.fontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primary
                                )

                                Row {
                                    IconButton(
                                        onClick = { viewModel.navigateMonth(-1) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = Translate.t("desc_prev_month", lang),
                                            tint = theme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.navigateMonth(1) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = Translate.t("desc_next_month", lang),
                                            tint = theme.primary
                                        )
                                    }
                                }
                            }

                            // Weekday initials
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                val days = if (lang == "fr") listOf("D", "L", "M", "M", "J", "V", "S") else listOf("S", "M", "T", "W", "T", "F", "S")
                                days.forEach { day ->
                                    Text(
                                        text = day,
                                        fontFamily = theme.fontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = theme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Calendar Numbers Section
                            val calendarCells = remember(currentCal) { calculateCalendarCells(currentCal) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSwipeLeftOrRight(
                                        onSwipeLeft = {
                                            triggerHaptic(context, MyHapticStyle.MEDIUM)
                                            viewModel.navigateMonth(1)
                                        },
                                        onSwipeRight = {
                                            triggerHaptic(context, MyHapticStyle.MEDIUM)
                                            viewModel.navigateMonth(-1)
                                        }
                                    )
                            ) {
                                val rowsCount = calendarCells.size / 7
                                for (r in 0 until rowsCount) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        for (c in 0 until 7) {
                                            val dayNum = calendarCells[r * 7 + c]
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(2.dp)
                                            ) {
                                                if (dayNum > 0) {
                                                    val hasMigraine = loggedDaysSet.contains(dayNum)
                                                    val dateStrString = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, dayNum)
                                                    val isSelected = activeSLogDate == dateStrString && editorVisible

                                                    val isToday = (currentYear == todayYear && currentMonth == todayMonth && dayNum == todayDay)
                                                    val isPast = (currentYear < todayYear) || 
                                                                 (currentYear == todayYear && currentMonth < todayMonth) || 
                                                                 (currentYear == todayYear && currentMonth == todayMonth && dayNum < todayDay)
                                                    val isFuture = (currentYear > todayYear) || 
                                                                   (currentYear == todayYear && currentMonth > todayMonth) || 
                                                                   (currentYear == todayYear && currentMonth == todayMonth && dayNum > todayDay)

                                                    // Apply beautiful shape (today's cell receives circular emphasize)
                                                    val cellShape = if (isToday) CircleShape else theme.cardShape

                                                    // Set specific background colors based on dates selection and today
                                                    val cellBackground = when {
                                                        isToday -> if (isSelected) theme.primary.copy(alpha = 0.22f) else theme.accent.copy(alpha = 0.15f)
                                                        isSelected -> theme.primary.copy(alpha = 0.12f)
                                                        else -> Color.Transparent
                                                    }

                                                    // Configure borders per design guidelines
                                                    val cellBorderColor = when {
                                                        isToday -> theme.accent
                                                        isSelected -> theme.primary
                                                        else -> Color.Transparent
                                                    }
                                                    val cellBorderWidth = if (isToday) 2.dp else 1.dp

                                                    // Mute future dates with a softer visual opacity
                                                    val cellAlpha = if (isFuture) 0.4f else 1.0f

                                                    val fontW = when {
                                                        isToday -> FontWeight.ExtraBold
                                                        hasMigraine -> FontWeight.Bold
                                                        else -> FontWeight.Normal
                                                    }
                                                    val textCol = when {
                                                        isToday -> theme.accent
                                                        hasMigraine -> theme.accent
                                                        isPast -> theme.onSurface
                                                        else -> theme.onSurfaceVariant
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .graphicsLayer(alpha = cellAlpha)
                                                            .clip(cellShape)
                                                            .background(cellBackground)
                                                            .border(
                                                                width = cellBorderWidth,
                                                                color = cellBorderColor,
                                                                shape = cellShape
                                                            )
                                                            .clickable {
                                                                val existing = logsThisMonth.find { it.day == dayNum }
                                                                viewModel.selectDateForLog(dateStrString, existing)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center,
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            Text(
                                                                text = dayNum.toString(),
                                                                fontFamily = theme.fontFamily,
                                                                fontSize = 15.sp,
                                                                fontWeight = fontW,
                                                                color = textCol
                                                            )
                                                            if (hasMigraine) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .padding(top = 4.dp)
                                                                        .size(6.dp)
                                                                        .clip(CircleShape)
                                                                        .background(theme.accent)
                                                                )
                                                            } else {
                                                                Spacer(modifier = Modifier.height(10.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            }
                        }

                        CalendarDisplayMode.WEEK -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                            val weekDays = remember(currentCal) {
                                val result = ArrayList<Calendar>()
                                val temp = Calendar.getInstance().apply {
                                    time = currentCal.time
                                }
                                val currentDayOfWeek = temp.get(Calendar.DAY_OF_WEEK)
                                temp.add(Calendar.DAY_OF_YEAR, -(currentDayOfWeek - 1))
                                for (i in 0 until 7) {
                                    result.add(Calendar.getInstance().apply { time = temp.time })
                                    temp.add(Calendar.DAY_OF_YEAR, 1)
                                }
                                result
                            }

                            // Week header section
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val weekStart = remember(weekDays, lang) {
                                    SimpleDateFormat("MMM dd", if (lang == "fr") Locale.FRENCH else Locale.US).format(weekDays[0].time)
                                }
                                val weekEnd = remember(weekDays, lang) {
                                    SimpleDateFormat("MMM dd, yyyy", if (lang == "fr") Locale.FRENCH else Locale.US).format(weekDays[6].time)
                                }

                                Text(
                                    text = "$weekStart - $weekEnd",
                                    fontFamily = theme.fontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primary
                                )

                                Row {
                                    IconButton(
                                        onClick = { viewModel.navigateWeek(-1) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = Translate.t("desc_prev_week", lang),
                                            tint = theme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.navigateWeek(1) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = Translate.t("desc_next_week", lang),
                                            tint = theme.primary
                                        )
                                    }
                                }
                            }

                            // Horizontally displayed 7 days capsules
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSwipeLeftOrRight(
                                        onSwipeLeft = {
                                            triggerHaptic(context, MyHapticStyle.MEDIUM)
                                            viewModel.navigateWeek(1)
                                        },
                                        onSwipeRight = {
                                            triggerHaptic(context, MyHapticStyle.MEDIUM)
                                            viewModel.navigateWeek(-1)
                                        }
                                    )
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                weekDays.forEach { dayCal ->
                                    val dNum = dayCal.get(Calendar.DAY_OF_MONTH)
                                    val dayIndex = dayCal.get(Calendar.DAY_OF_WEEK) - 1
                                    val dayLetter = listOf("S", "M", "T", "W", "T", "F", "S")[dayIndex]

                                    val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dayCal.time)
                                    val daySelectedLog = allLogs.find { it.dateString == dStr }
                                    val hasMigraine = daySelectedLog != null

                                    val isToday = dayCal.get(Calendar.YEAR) == todayYear &&
                                                  (dayCal.get(Calendar.MONTH) + 1) == todayMonth &&
                                                  dayCal.get(Calendar.DAY_OF_MONTH) == todayDay

                                    val isSelected = activeSLogDate == dStr && editorVisible

                                    val cellShape = theme.cardShape
                                    val cellBackground = when {
                                        isToday -> if (isSelected) theme.primary.copy(alpha = 0.22f) else theme.accent.copy(alpha = 0.15f)
                                        isSelected -> theme.primary.copy(alpha = 0.12f)
                                        else -> theme.surfaceContainerLow
                                    }
                                    val cellBorderColor = when {
                                        isToday -> theme.accent
                                        isSelected -> theme.primary
                                        else -> theme.onSurfaceVariant.copy(alpha = 0.12f)
                                    }
                                    val cellBorderWidth = if (isToday || isSelected) 2.dp else 1.dp

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(cellShape)
                                            .background(cellBackground)
                                            .border(cellBorderWidth, cellBorderColor, cellShape)
                                            .clickable {
                                                viewModel.selectDateForLog(dStr, daySelectedLog)
                                            }
                                            .padding(vertical = 12.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayLetter,
                                            fontFamily = theme.fontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = theme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dNum.toString(),
                                            fontFamily = theme.fontFamily,
                                            fontSize = 15.sp,
                                            fontWeight = if (isToday || hasMigraine) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isToday) theme.accent else theme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (hasMigraine) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(theme.accent)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }
                                }
                            }
                            }
                        }

                        CalendarDisplayMode.DAY -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                            val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(currentCal.time)
                            val dayCalFormatted = SimpleDateFormat("EEEE, MMMM dd, yyyy", if (lang == "fr") Locale.FRENCH else Locale.US).format(currentCal.time)
                            val logForDay = allLogs.find { it.dateString == dStr }

                            // Day header navigation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("MMMM dd, yyyy", if (lang == "fr") Locale.FRENCH else Locale.US).format(currentCal.time),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primary
                                )

                                Row {
                                    IconButton(
                                        onClick = { viewModel.navigateDay(-1) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = Translate.t("desc_prev_day", lang),
                                            tint = theme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.navigateDay(1) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = Translate.t("desc_next_day", lang),
                                            tint = theme.primary
                                        )
                                    }
                                }
                            }

                            // Day Focused Card report
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(theme.cardShape)
                                    .background(theme.surfaceContainerLow)
                                    .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), theme.cardShape)
                                    .onSwipeLeftOrRight(
                                        onSwipeLeft = {
                                            triggerHaptic(context, MyHapticStyle.MEDIUM)
                                            viewModel.navigateDay(1)
                                        },
                                        onSwipeRight = {
                                            triggerHaptic(context, MyHapticStyle.MEDIUM)
                                            viewModel.navigateDay(-1)
                                        }
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = dayCalFormatted,
                                    fontFamily = theme.fontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                if (logForDay != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(theme.accent.copy(alpha = 0.15f))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "${Translate.t("intensity", lang)}: ${logForDay.intensity}/10",
                                                fontFamily = theme.fontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = theme.accent
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { viewModel.selectDateForLog(dStr, logForDay) },
                                                shape = theme.buttonShape,
                                                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = Translate.t("desc_edit_tracker", lang),
                                                    modifier = Modifier.size(18.dp),
                                                    tint = theme.onPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(Translate.t("edit", lang), fontSize = 14.sp, fontFamily = theme.fontFamily)
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteLog(logForDay.id) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(theme.accent.copy(alpha = 0.1f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = Translate.t("desc_delete_tracker", lang),
                                                    modifier = Modifier.size(20.dp),
                                                    tint = theme.accent
                                                )
                                            }
                                        }
                                    }

                                    if (logForDay.symptoms.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = Translate.t("symptoms_detected", lang),
                                            fontFamily = theme.fontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            logForDay.symptoms.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { symptom ->
                                                Box(
                                                    modifier = Modifier
                                                        .background(theme.onSurfaceVariant.copy(alpha = 0.05f), shape = theme.buttonShape)
                                                        .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), shape = theme.buttonShape)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = Translate.t("tag_" + symptom.lowercase().replace(" ", "_"), lang),
                                                        fontFamily = theme.fontFamily,
                                                        fontSize = 11.sp,
                                                        color = theme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (logForDay.triggers.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = Translate.t("suspected_triggers", lang),
                                            fontFamily = theme.fontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            logForDay.triggers.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { trigger ->
                                                Box(
                                                    modifier = Modifier
                                                        .background(theme.onSurfaceVariant.copy(alpha = 0.05f), shape = theme.buttonShape)
                                                        .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), shape = theme.buttonShape)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = Translate.t("tag_" + trigger.lowercase().replace(" ", "_"), lang),
                                                        fontFamily = theme.fontFamily,
                                                        fontSize = 11.sp,
                                                        color = theme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (logForDay.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = Translate.t("notes", lang),
                                            fontFamily = theme.fontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = logForDay.note,
                                            fontFamily = theme.fontFamily,
                                            fontSize = 13.sp,
                                            color = theme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = Translate.t("desc_no_records", lang),
                                            tint = theme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = Translate.t("no_entry_this_day", lang),
                                            fontFamily = theme.fontFamily,
                                            fontSize = 13.sp,
                                            color = theme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = { viewModel.selectDateForLog(dStr, null) },
                                            shape = theme.buttonShape,
                                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(Translate.t("log_episode_btn", lang), fontFamily = theme.fontFamily, fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                            }
                        }

                        CalendarDisplayMode.YEAR -> {
                            val targetYear = currentCal.get(Calendar.YEAR)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSwipeLeftOrRight(
                                        onSwipeLeft = {
                                            triggerHaptic(context, MyHapticStyle.MEDIUM)
                                            viewModel.navigateYear(1)
                                        },
                                        onSwipeRight = {
                                            triggerHaptic(context, MyHapticStyle.MEDIUM)
                                            viewModel.navigateYear(-1)
                                        }
                                    )
                            ) {
                            // Main Year header with slider navigation 
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${Translate.t("calendar_mode_year", lang)} $targetYear",
                                    fontFamily = theme.fontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primary
                                )

                                Row {
                                    IconButton(
                                        onClick = {
                                            triggerHaptic(context, MyHapticStyle.LIGHT)
                                            viewModel.navigateYear(-1)
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = Translate.t("prev_year", lang),
                                            tint = theme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            triggerHaptic(context, MyHapticStyle.LIGHT)
                                            viewModel.navigateYear(1)
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = Translate.t("next_year", lang),
                                            tint = theme.primary
                                        )
                                    }
                                }
                            }

                            // Loop over all 12 stacked simplified calendars for the selected year
                            for (monthIdx in 0..11) {
                                val targetCal = remember(currentCal, monthIdx) {
                                    Calendar.getInstance().apply {
                                        set(Calendar.YEAR, targetYear)
                                        set(Calendar.MONTH, monthIdx)
                                        set(Calendar.DAY_OF_MONTH, 1)
                                    }
                                }

                                val targetLabel = remember(targetCal, lang) {
                                    SimpleDateFormat("MMMM yyyy", if (lang == "fr") Locale.FRENCH else Locale.US).format(targetCal.time)
                                }
                                val targetYr = targetCal.get(Calendar.YEAR)
                                val targetMo = targetCal.get(Calendar.MONTH) + 1

                                val targetLogsList = allLogs.filter { it.year == targetYr && it.month == targetMo }
                                val targetLoggedDaysSet = targetLogsList.map { it.day }.toSet()
                                val targetCalendarCells = calculateCalendarCells(targetCal)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = targetLabel,
                                        fontFamily = theme.fontFamily,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    // Week initials
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                    ) {
                                        val days = if (lang == "fr") listOf("D", "L", "M", "M", "J", "V", "S") else listOf("S", "M", "T", "W", "T", "F", "S")
                                        days.forEach { day ->
                                            Text(
                                                text = day,
                                                fontFamily = theme.fontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = theme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // Grid of targets
                                    val rowsCount = targetCalendarCells.size / 7
                                    for (r in 0 until rowsCount) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            for (c in 0 until 7) {
                                                val dayNum = targetCalendarCells[r * 7 + c]
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1.2f)
                                                        .padding(1.dp)
                                                ) {
                                                    if (dayNum > 0) {
                                                        val hasMigraine = targetLoggedDaysSet.contains(dayNum)
                                                        val dateStrString = String.format(Locale.US, "%04d-%02d-%02d", targetYr, targetMo, dayNum)
                                                        val isSelected = activeSLogDate == dateStrString && editorVisible
                                                        val isToday = (targetYr == todayYear && targetMo == todayMonth && dayNum == todayDay)
                                                        val isFuture = (targetYr > todayYear) || 
                                                                       (targetYr == todayYear && targetMo > todayMonth) || 
                                                                       (targetYr == todayYear && targetMo == todayMonth && dayNum > todayDay)

                                                        val cellShape = if (isToday) CircleShape else theme.cardShape
                                                        val cellBackground = when {
                                                            isToday -> if (isSelected) theme.primary.copy(alpha = 0.22f) else theme.accent.copy(alpha = 0.15f)
                                                            isSelected -> theme.primary.copy(alpha = 0.12f)
                                                            else -> Color.Transparent
                                                        }
                                                        val cellBorderColor = when {
                                                            isToday -> theme.accent
                                                            isSelected -> theme.primary
                                                            else -> Color.Transparent
                                                        }
                                                        val cellBorderWidth = if (isToday) 2.dp else 1.dp
                                                        val cellAlpha = if (isFuture) 0.4f else 1.0f

                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .graphicsLayer(alpha = cellAlpha)
                                                                .clip(cellShape)
                                                                .background(cellBackground)
                                                                .border(
                                                                    width = cellBorderWidth,
                                                                    color = cellBorderColor,
                                                                    shape = cellShape
                                                                )
                                                                .clickable {
                                                                    val existing = targetLogsList.find { it.day == dayNum }
                                                                    viewModel.selectDateForLog(dateStrString, existing)
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.Center,
                                                                modifier = Modifier.fillMaxSize()
                                                            ) {
                                                                Text(
                                                                    text = dayNum.toString(),
                                                                    fontFamily = theme.fontFamily,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = if (isToday || hasMigraine) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isToday || hasMigraine) theme.accent else theme.onSurface
                                                                )
                                                                if (hasMigraine) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .padding(top = 2.dp)
                                                                            .size(4.dp)
                                                                            .clip(CircleShape)
                                                                            .background(theme.accent)
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
                            }
                            }
                        }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = theme.onSurfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Recent Episodes Header (Row with Right arrowSee all)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Translate.t("recent_episodes", lang),
                            fontFamily = theme.fontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary
                        )

                        Row(
                            modifier = Modifier
                                .clickable { viewModel.setShowAllEpisodes(true) }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = Translate.t("see_all", lang),
                                fontFamily = theme.fontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.accent
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = Translate.t("desc_see_all_episodes", lang),
                                tint = theme.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (allLogs.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AppLottiePlayer(
                                url = "https://assets5.lottiefiles.com/packages/lf20_t9gkkhz4.json",
                                modifier = Modifier
                                    .size(160.dp)
                                    .padding(bottom = 16.dp),
                                fallback = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = Translate.t("desc_no_journals", lang),
                                        tint = theme.primary.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .size(64.dp)
                                            .padding(bottom = 16.dp)
                                    )
                                }
                            )
                            Text(
                                text = Translate.t("no_journals_logged_yet", lang),
                                fontFamily = theme.fontFamily,
                                fontSize = 14.sp,
                                color = theme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        // Horizontal scrollable list
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            allLogs.forEach { log ->
                                key(log.id) {
                                    val friendlyDate = try {
                                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(log.dateString)
                                        SimpleDateFormat("MMMM dd, yyyy", if (lang == "fr") Locale.FRENCH else Locale.US).format(date!!)
                                    } catch (e: Exception) {
                                        log.dateString
                                    }

                                    Row(
                                        modifier = Modifier
                                            .width(280.dp)
                                            .clip(theme.cardShape)
                                            .background(theme.surfaceContainerLow)
                                            .clickable {
                                                viewModel.selectDateForLog(log.dateString, log)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.deleteLog(log.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .padding(end = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = Translate.t("desc_delete_entry", lang),
                                                tint = theme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = friendlyDate,
                                                    fontFamily = theme.fontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = theme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(theme.accent.copy(alpha = 0.2f))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${Translate.t("pain_scale_label", lang)} ${log.intensity}/10",
                                                        fontFamily = theme.fontFamily,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = theme.accent
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = log.note.ifBlank { Translate.t("no_description_entered", lang) },
                                                fontFamily = theme.fontFamily,
                                                fontSize = 14.sp,
                                                color = theme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (log.symptoms.isNotBlank() || log.triggers.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val details = mutableListOf<String>()
                                                if (log.symptoms.isNotBlank()) {
                                                    details.add("${Translate.t("symptom", lang)}: ${Translate.translateCsvTags(log.symptoms, lang)}")
                                                }
                                                if (log.triggers.isNotBlank()) {
                                                    details.add("${Translate.t("trigger", lang)}: ${Translate.translateCsvTags(log.triggers, lang)}")
                                                }
                                                Text(
                                                    text = details.joinToString(" | "),
                                                    fontFamily = theme.fontFamily,
                                                    fontSize = 11.sp,
                                                    color = theme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(theme.accent)
                                                .padding(start = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Generous bottom spacer for scroll safety
                Spacer(modifier = Modifier.height(140.dp))
            }
            }
                } // end of when (targetScreen)
            } // end of AnimatedContent
        }

        // Dim background overlay behind the bottom sheet
        AnimatedVisibility(
            visible = editorVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.closeEditor()
                    }
            )
        }

        // Bottom drawer edit sheet
        AnimatedVisibility(
            visible = editorVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            EditorBottomSheet(viewModel = viewModel)
        }

        val isOnboardingActive by viewModel.isOnboardingActive.collectAsStateWithLifecycle()
        if (isOnboardingActive) {
            OnboardingTutorialOverlay(
                lang = lang,
                theme = theme,
                viewModel = viewModel,
                onComplete = {
                    viewModel.setOnboardingActive(false)
                    context.getSharedPreferences("migraine_settings", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("tutorial_seen", true)
                        .apply()
                }
            )
        }
    }
}

// Side-up note management sheet
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorBottomSheet(viewModel: MigraineViewModel) {
    val theme = LocalAppTheme.current
    val context = LocalContext.current
    val editorDate by viewModel.selectedLogDate.collectAsStateWithLifecycle()
    val noteVal by viewModel.editorNote.collectAsStateWithLifecycle()
    val intensityVal by viewModel.editorIntensity.collectAsStateWithLifecycle()
    val selectedSymptoms by viewModel.editorSymptoms.collectAsStateWithLifecycle()
    val selectedTriggers by viewModel.editorTriggers.collectAsStateWithLifecycle()
    val activeLogId by viewModel.editingLogId.collectAsStateWithLifecycle()
    val isReadOnly by viewModel.isReadOnly.collectAsStateWithLifecycle()

    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val friendlyDate = remember(editorDate, lang) {
        if (editorDate != null) {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(editorDate!!)
                SimpleDateFormat("MMMM dd, yyyy", if (lang == "fr") Locale.FRENCH else Locale.US).format(parsed!!)
            } catch (e: Exception) {
                editorDate!!
            }
        } else ""
    }

    // Collapsible states defaulted to false (collapsed) and reset when editorDate changes
    var isPainExpanded by remember(editorDate) { mutableStateOf(false) }
    var isSymptomsExpanded by remember(editorDate) { mutableStateOf(false) }
    var isTriggersExpanded by remember(editorDate) { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }

    // Animation orchestration triggers on entry
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "headerAlpha"
    )
    val headerTranslationY by animateFloatAsState(
        targetValue = if (animationTriggered) 0f else 20f,
        animationSpec = tween(durationMillis = 400, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "headerY"
    )

    val noteAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "noteAlpha"
    )
    val noteTranslationY by animateFloatAsState(
        targetValue = if (animationTriggered) 0f else 20f,
        animationSpec = tween(durationMillis = 400, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "noteY"
    )

    val painAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 160, easing = FastOutSlowInEasing),
        label = "painAlpha"
    )
    val painTranslationY by animateFloatAsState(
        targetValue = if (animationTriggered) 0f else 20f,
        animationSpec = tween(durationMillis = 400, delayMillis = 160, easing = FastOutSlowInEasing),
        label = "painY"
    )

    val symptomsAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 240, easing = FastOutSlowInEasing),
        label = "symptomsAlpha"
    )
    val symptomsTranslationY by animateFloatAsState(
        targetValue = if (animationTriggered) 0f else 20f,
        animationSpec = tween(durationMillis = 400, delayMillis = 240, easing = FastOutSlowInEasing),
        label = "symptomsY"
    )

    val triggersAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 320, easing = FastOutSlowInEasing),
        label = "triggersAlpha"
    )
    val triggersTranslationY by animateFloatAsState(
        targetValue = if (animationTriggered) 0f else 20f,
        animationSpec = tween(durationMillis = 400, delayMillis = 320, easing = FastOutSlowInEasing),
        label = "triggersY"
    )

    val buttonAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "buttonAlpha"
    )
    val buttonTranslationY by animateFloatAsState(
        targetValue = if (animationTriggered) 0f else 20f,
        animationSpec = tween(durationMillis = 400, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "buttonY"
    )

    val heightModifier = if (isFullScreen) Modifier.fillMaxHeight() else Modifier.wrapContentHeight()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(heightModifier)
            .navigationBarsPadding() // Ensures safety on modern navigation devices
            .shadowShadow(elevation = 16.dp, color = theme.primary.copy(alpha = 0.1f)),
        shape = if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface)
    ) {
        // Drag Handle and Resize Gesture Area
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 4.dp)
                .width(42.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(theme.onSurfaceVariant.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y < -15) {
                            if (!isFullScreen) isFullScreen = true
                        } else if (dragAmount.y > 15) {
                            if (isFullScreen) isFullScreen = false
                        }
                    }
                }
                .clickable {
                    isFullScreen = !isFullScreen
                }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isFullScreen) Modifier.weight(1f) else Modifier.wrapContentHeight())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = headerAlpha
                        translationY = headerTranslationY
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Translate.t("notes_for", lang).replace("%s", friendlyDate),
                    fontFamily = theme.fontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            activeLogId?.let { id ->
                                viewModel.deleteLog(id)
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = Translate.t("desc_delete_note", lang),
                            tint = theme.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { isFullScreen = !isFullScreen },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isFullScreen) Translate.t("desc_minimize_sheet", lang) else Translate.t("desc_maximize_sheet", lang),
                            tint = theme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.closeEditor() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = Translate.t("desc_close_editor", lang),
                            tint = theme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode indicator banner
            AnimatedContent(
                targetState = isReadOnly,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "modeBanner"
            ) { readOnlyActive ->
                if (readOnlyActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(theme.cardShape)
                            .background(theme.primary.copy(alpha = 0.06f))
                            .border(1.dp, theme.primary.copy(alpha = 0.2f), theme.cardShape)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = Translate.t("desc_read_only_mode", lang),
                            tint = theme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Translate.t("viewing_mode_desc", lang),
                            fontFamily = theme.fontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(theme.cardShape)
                            .background(theme.accent.copy(alpha = 0.08f))
                            .border(1.dp, theme.accent.copy(alpha = 0.25f), theme.cardShape)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = Translate.t("desc_editing_mode", lang),
                            tint = theme.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Translate.t("editing_mode_desc", lang),
                            fontFamily = theme.fontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Textarea input Column with animated subtle layers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = noteAlpha
                        translationY = noteTranslationY
                    }
            ) {
                TextField(
                    value = noteVal,
                    onValueChange = { viewModel.editorNote.value = it },
                    enabled = !isReadOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .border(
                            width = 1.dp,
                            color = if (isReadOnly) theme.onSurfaceVariant.copy(alpha = 0.1f) else theme.primary.copy(alpha = 0.3f),
                            shape = theme.cardShape
                        ),
                    placeholder = {
                        Text(
                            text = if (isReadOnly) {
                                Translate.t("no_description_entered", lang)
                            } else {
                                Translate.t("describe_feeling", lang)
                            },
                            fontFamily = theme.fontFamily,
                            fontSize = 14.sp,
                            color = theme.onSurfaceVariant.copy(alpha = if (isReadOnly) 0.35f else 0.5f)
                        )
                    },
                    shape = theme.cardShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = theme.surfaceContainerLow,
                        unfocusedContainerColor = theme.surfaceContainerLow,
                        disabledContainerColor = theme.surfaceContainerLow.copy(alpha = 0.6f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = theme.onSurface,
                        unfocusedTextColor = theme.onSurface,
                        disabledTextColor = theme.onSurface.copy(alpha = 0.85f)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = theme.fontFamily,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pain Scale Column with animated subtle layers and interactive custom slider
            val severityText = when (intensityVal) {
                in 1..3 -> Translate.t("severity_mild", lang)
                in 4..6 -> Translate.t("severity_moderate", lang)
                in 7..8 -> Translate.t("severity_severe", lang)
                else -> Translate.t("severity_debilitating", lang)
            }
            val severityColor = when (intensityVal) {
                in 1..3 -> theme.onSurfaceVariant.copy(alpha = 0.8f)
                in 4..6 -> theme.accent
                in 7..8 -> theme.primary
                else -> theme.onSurface
            }

            CollapsibleSection(
                title = Translate.t("pain_intensity", lang),
                badgeText = "$intensityVal/10 • $severityText",
                badgeColor = severityColor.copy(alpha = 0.15f),
                badgeTextColor = severityColor,
                isExpanded = isPainExpanded,
                onToggle = { isPainExpanded = !isPainExpanded },
                lang = lang,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = painAlpha
                        translationY = painTranslationY
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Visual feedback card with dynamic severity description and matching emoji
                    val (severityEmoji, severityDesc) = when (intensityVal) {
                        1, 2 -> "😌" to Translate.t("severity_desc_1_2", lang)
                        3, 4 -> "😐" to Translate.t("severity_desc_3_4", lang)
                        5, 6 -> "😣" to Translate.t("severity_desc_5_6", lang)
                        7, 8 -> "😫" to Translate.t("severity_desc_7_8", lang)
                        else -> "🤯" to Translate.t("severity_desc_9_10", lang)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(theme.cardShape)
                            .background(theme.surfaceContainerLow)
                            .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.08f), theme.cardShape)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = severityEmoji,
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = severityDesc,
                            fontFamily = theme.fontFamily,
                            fontSize = 12.sp,
                            color = theme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val sliderColor = when (intensityVal) {
                        in 1..3 -> theme.onSurfaceVariant.copy(alpha = 0.8f)
                        in 4..6 -> theme.accent
                        in 7..8 -> theme.primary
                        else -> theme.onSurface
                    }

                    Slider(
                        value = intensityVal.toFloat(),
                        onValueChange = { viewModel.editorIntensity.value = it.roundToInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        enabled = !isReadOnly,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isReadOnly) sliderColor.copy(alpha = 0.5f) else sliderColor,
                            activeTrackColor = if (isReadOnly) sliderColor.copy(alpha = 0.5f) else sliderColor,
                            inactiveTrackColor = theme.surfaceContainer,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                            disabledThumbColor = sliderColor.copy(alpha = 0.4f),
                            disabledActiveTrackColor = sliderColor.copy(alpha = 0.3f),
                            disabledInactiveTrackColor = theme.surfaceContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Custom 1 to 10 scale step reference indicators (tappable)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 1..10) {
                            val isCurrent = i == intensityVal
                            val numColor = if (isCurrent) {
                                if (isReadOnly) sliderColor.copy(alpha = 0.7f) else sliderColor
                            } else {
                                theme.onSurfaceVariant.copy(alpha = if (isReadOnly) 0.2f else 0.5f)
                            }
                            val numWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal
                            val numSize = if (isCurrent) 14.sp else 11.sp

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable(
                                        enabled = !isReadOnly,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { viewModel.editorIntensity.value = i },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = i.toString(),
                                    fontFamily = theme.fontFamily,
                                    fontSize = if (isCurrent) 16.sp else 13.sp,
                                    fontWeight = numWeight,
                                    color = numColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Symptoms selections group Column with animated subtle layers
            val hasSymptoms = selectedSymptoms.isNotEmpty()
            CollapsibleSection(
                title = Translate.t("symptoms_experienced", lang),
                badgeText = if (hasSymptoms) "${selectedSymptoms.size} ${Translate.t("active", lang)}" else null,
                badgeColor = theme.accent.copy(alpha = 0.15f),
                badgeTextColor = theme.accent,
                isExpanded = isSymptomsExpanded,
                onToggle = { isSymptomsExpanded = !isSymptomsExpanded },
                lang = lang,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = symptomsAlpha
                        translationY = symptomsTranslationY
                    }
            ) {
                if (isReadOnly) {
                    if (selectedSymptoms.isEmpty()) {
                        Text(
                            text = Translate.t("no_symptoms_recorded", lang),
                            fontFamily = theme.fontFamily,
                            fontSize = 12.sp,
                            color = theme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 4.dp),
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedSymptoms.forEach { sym ->
                                Box(
                                    modifier = Modifier
                                        .border(width = 1.dp, color = theme.accent, shape = theme.buttonShape)
                                        .background(theme.accent.copy(alpha = 0.08f), shape = theme.buttonShape)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = Translate.t("tag_" + sym.lowercase().replace(" ", "_"), lang),
                                        fontFamily = theme.fontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = theme.accent
                                    )
                                }
                            }
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SYMPTOM_LIST.forEach { sym ->
                            val isSelected = selectedSymptoms.contains(sym)
                            val chipStrokeColor = if (isSelected) theme.accent else theme.onSurfaceVariant.copy(alpha = 0.15f)
                            val chipContentBg = if (isSelected) theme.accent.copy(alpha = 0.1f) else Color.Transparent
                            val textColor = if (isSelected) theme.accent else theme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .border(width = 1.2.dp, color = chipStrokeColor, shape = theme.buttonShape)
                                    .background(chipContentBg, shape = theme.buttonShape)
                                    .clickable {
                                        triggerHaptic(context, MyHapticStyle.LIGHT)
                                        viewModel.toggleSymptom(sym)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Translate.t("tag_" + sym.lowercase().replace(" ", "_"), lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Triggers selections group Column with animated subtle layers
            val hasTriggers = selectedTriggers.isNotEmpty()
            CollapsibleSection(
                title = Translate.t("suspected_triggers", lang),
                badgeText = if (hasTriggers) "${selectedTriggers.size} ${Translate.t("active", lang)}" else null,
                badgeColor = theme.primary.copy(alpha = 0.15f),
                badgeTextColor = theme.primary,
                isExpanded = isTriggersExpanded,
                onToggle = { isTriggersExpanded = !isTriggersExpanded },
                lang = lang,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = triggersAlpha
                        translationY = triggersTranslationY
                    }
            ) {
                if (isReadOnly) {
                    if (selectedTriggers.isEmpty()) {
                        Text(
                            text = Translate.t("no_triggers_recorded", lang),
                            fontFamily = theme.fontFamily,
                            fontSize = 12.sp,
                            color = theme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 4.dp),
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedTriggers.forEach { trig ->
                                Box(
                                    modifier = Modifier
                                        .border(width = 1.dp, color = theme.primary, shape = theme.buttonShape)
                                        .background(theme.primary.copy(alpha = 0.08f), shape = theme.buttonShape)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = Translate.t("tag_" + trig.lowercase().replace(" ", "_"), lang),
                                        fontFamily = theme.fontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = theme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TRIGGER_LIST.forEach { trig ->
                            val isSelected = selectedTriggers.contains(trig)
                            val chipStrokeColor = if (isSelected) theme.accent else theme.onSurfaceVariant.copy(alpha = 0.15f)
                            val chipContentBg = if (isSelected) theme.accent.copy(alpha = 0.1f) else Color.Transparent
                            val textColor = if (isSelected) theme.accent else theme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .border(width = 1.2.dp, color = chipStrokeColor, shape = theme.buttonShape)
                                    .background(chipContentBg, shape = theme.buttonShape)
                                    .clickable {
                                        triggerHaptic(context, MyHapticStyle.LIGHT)
                                        viewModel.toggleTrigger(trig)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Translate.t("tag_" + trig.lowercase().replace(" ", "_"), lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // Save row Column with animated subtle layers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = buttonAlpha
                        translationY = buttonTranslationY
                    }
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = isReadOnly,
                    transitionSpec = {
                        slideInVertically(initialOffsetY = { 40 }) + fadeIn() togetherWith
                        slideOutVertically(targetOffsetY = { -40 }) + fadeOut()
                    },
                    label = "bottomActionButton"
                ) { readOnlyActive ->
                    if (readOnlyActive) {
                        Button(
                            onClick = { viewModel.isReadOnly.value = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = theme.buttonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.accent,
                                contentColor = theme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = Translate.t("edit_note", lang),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Translate.t("edit_note", lang),
                                fontFamily = theme.fontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                triggerHaptic(context, MyHapticStyle.SUCCESS)
                                viewModel.saveActiveNote()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = theme.buttonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primary,
                                contentColor = theme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = Translate.t("save_note", lang),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Translate.t("save_note", lang),
                                fontFamily = theme.fontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Reusable Collapsible/Expandable UI container for clean modularity
@Composable
fun CollapsibleSection(
    title: String,
    badgeText: String? = null,
    badgeColor: Color = Color.Transparent,
    badgeTextColor: Color = Color.Unspecified,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    lang: String = "en",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(theme.cardShape)
            .background(theme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = theme.onSurfaceVariant.copy(alpha = 0.12f),
                shape = theme.cardShape
            )
    ) {
        // Collapsible Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontFamily = theme.fontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.onSurface
                )
                
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontFamily = theme.fontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeTextColor
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) Translate.t("desc_collapse", lang) else Translate.t("desc_expand", lang),
                tint = theme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = rotationState
                    }
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                HorizontalDivider(
                    color = theme.onSurfaceVariant.copy(alpha = 0.08f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                content()
            }
        }
    }
}

// JVM Calendar calculation helpers for standard grid representation
fun calculateCalendarCells(calendar: Calendar): List<Int> {
    val tempCal = Calendar.getInstance().apply {
        time = calendar.time
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.

    val list = mutableListOf<Int>()
    val offset = startDayOfWeek - 1 // How many empty slots at beginning (0 for Sunday, 1 Monday, etc.)

    for (i in 0 until offset) {
        list.add(0)
    }
    for (day in 1..daysInMonth) {
        list.add(day)
    }

    // Pad end of list to multiples of 7 for clean rows calculation
    while (list.size % 7 != 0) {
        list.add(0)
    }
    return list
}

// Shadow modifier helper for custom visual depth without relying on Android default dark elevations
fun Modifier.shadowShadow(elevation: androidx.compose.ui.unit.Dp, color: Color): Modifier {
    return this.background(
        color = Color.Transparent
    )
}

fun generatePdfReport(context: Context, logs: List<MigraineLog>, dateRangeStr: String, lang: String): File {
    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) exportDir.mkdirs()
    val file = File(exportDir, "Migraine_Medical_Report.pdf")
    if (file.exists()) file.delete()
    
    val document = PdfDocument()
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas
    
    val titlePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    val subTitlePaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }
    
    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    
    val boldTextPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val headerPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    val linePaint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        strokeWidth = 1f
    }
    
    var y = 50f
    canvas.drawText(Translate.t("pdf_report_title", lang), 40f, y, titlePaint)
    y += 20f
    canvas.drawText(Translate.t("pdf_date_range", lang).replace("%s", dateRangeStr), 40f, y, subTitlePaint)
    y += 15f
    val generatedOnStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    canvas.drawText(Translate.t("pdf_generated_on", lang).replace("%s", generatedOnStr), 40f, y, subTitlePaint)
    y += 25f
    
    val avgPain = if (logs.isNotEmpty()) String.format(Locale.US, "%.1f", logs.map { it.intensity }.average()) else "0.0"
    val crisesCountStr = Translate.t("pdf_episodes_count", lang).replace("%d", logs.size.toString())
    val avgPainStr = Translate.t("pdf_average_pain_val", lang).replace("%s", avgPain)
    canvas.drawText("$crisesCountStr   |   $avgPainStr", 40f, y, boldTextPaint)
    y += 20f
    
    canvas.drawLine(40f, y, 555f, y, linePaint)
    y += 20f
    
    val colX = floatArrayOf(40f, 110f, 150f, 260f, 380f)
    canvas.drawText(Translate.t("pdf_header_date", lang), colX[0], y, headerPaint)
    canvas.drawText(Translate.t("pdf_header_pain", lang), colX[1], y, headerPaint)
    canvas.drawText(Translate.t("pdf_header_symptoms", lang), colX[2], y, headerPaint)
    canvas.drawText(Translate.t("pdf_header_triggers", lang), colX[3], y, headerPaint)
    canvas.drawText(Translate.t("pdf_header_notes", lang), colX[4], y, headerPaint)
    y += 8f
    canvas.drawLine(40f, y, 555f, y, linePaint)
    y += 18f
    
    for (log in logs) {
        if (y > 780f) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
            
            canvas.drawText(Translate.t("pdf_header_date", lang), colX[0], y, headerPaint)
            canvas.drawText(Translate.t("pdf_header_pain", lang), colX[1], y, headerPaint)
            canvas.drawText(Translate.t("pdf_header_symptoms", lang), colX[2], y, headerPaint)
            canvas.drawText(Translate.t("pdf_header_triggers", lang), colX[3], y, headerPaint)
            canvas.drawText(Translate.t("pdf_header_notes", lang), colX[4], y, headerPaint)
            y += 8f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 18f
        }
        
        canvas.drawText(log.dateString, colX[0], y, textPaint)
        canvas.drawText("${log.intensity}/10", colX[1], y, textPaint)
        
        val translatedSymptoms = Translate.translateCsvTags(log.symptoms, lang)
        val symText = if (translatedSymptoms.length > 22) translatedSymptoms.substring(0, 19) + "..." else translatedSymptoms
        canvas.drawText(symText, colX[2], y, textPaint)
        
        val translatedTriggers = Translate.translateCsvTags(log.triggers, lang)
        val trigText = if (translatedTriggers.length > 22) translatedTriggers.substring(0, 19) + "..." else translatedTriggers
        canvas.drawText(trigText, colX[3], y, textPaint)
        
        val noteText = if (log.note.length > 32) log.note.substring(0, 29) + "..." else log.note
        canvas.drawText(noteText, colX[4], y, textPaint)
        
        y += 18f
        canvas.drawLine(40f, y - 5f, 555f, y - 5f, Paint().apply { color = android.graphics.Color.rgb(240, 240, 240); strokeWidth = 1f })
    }
    
    document.finishPage(page)
    val fos = FileOutputStream(file)
    document.writeTo(fos)
    document.close()
    fos.close()
    return file
}

fun generateCsvReport(context: Context, logs: List<MigraineLog>, dateRangeStr: String, lang: String): File {
    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) exportDir.mkdirs()
    val file = File(exportDir, "Migraine_Medical_Report.csv")
    if (file.exists()) file.delete()
    
    fun escapeCsv(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
    
    val csvHeader = listOf(
        Translate.t("csv_header_date", lang),
        Translate.t("csv_header_pain", lang),
        Translate.t("csv_header_symptoms", lang),
        Translate.t("csv_header_triggers", lang),
        Translate.t("csv_header_notes", lang)
    ).joinToString(",")
    
    val sb = java.lang.StringBuilder()
    sb.append(csvHeader).append("\n")
    for (log in logs) {
        sb.append(log.dateString).append(",")
            .append("${log.intensity}/10").append(",")
            .append(escapeCsv(Translate.translateCsvTags(log.symptoms, lang))).append(",")
            .append(escapeCsv(Translate.translateCsvTags(log.triggers, lang))).append(",")
            .append(escapeCsv(log.note)).append("\n")
    }
    
    val fos = FileOutputStream(file)
    fos.write(sb.toString().toByteArray())
    fos.close()
    return file
}

fun generateTxtReport(context: Context, logs: List<MigraineLog>, dateRangeStr: String, lang: String): File {
    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) exportDir.mkdirs()
    val file = File(exportDir, "Migraine_Medical_Report.txt")
    if (file.exists()) file.delete()
    
    val sb = java.lang.StringBuilder()
    val generatedOnStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    
    val title = Translate.t("txt_report_title", lang)
    sb.append(title).append("\n")
    sb.append("=".repeat(title.length)).append("\n")
    sb.append(Translate.t("txt_date_range", lang)).append(dateRangeStr).append("\n")
    sb.append(Translate.t("txt_generated_on", lang)).append(generatedOnStr).append("\n")
    sb.append(Translate.t("txt_total_episodes", lang)).append(logs.size).append("\n")
    val avgPain = if (logs.isNotEmpty()) logs.map { it.intensity }.average() else 0.0
    sb.append(Translate.t("txt_average_pain", lang)).append(String.format(Locale.US, "%.1f", avgPain)).append("/10\n\n")
    
    val detailTitle = Translate.t("txt_detail_title", lang)
    sb.append(detailTitle).append("\n")
    sb.append("-".repeat(detailTitle.length)).append("\n\n")
    for (log in logs) {
        sb.append(Translate.t("txt_date", lang)).append(log.dateString).append("\n")
        sb.append(Translate.t("txt_intensity", lang)).append(log.intensity).append("/10\n")
        if (log.symptoms.isNotBlank()) {
            sb.append(Translate.t("txt_symptoms", lang)).append(Translate.translateCsvTags(log.symptoms, lang)).append("\n")
        }
        if (log.triggers.isNotBlank()) {
            sb.append(Translate.t("txt_triggers", lang)).append(Translate.translateCsvTags(log.triggers, lang)).append("\n")
        }
        if (log.note.isNotBlank()) {
            sb.append(Translate.t("txt_notes", lang)).append(log.note).append("\n")
        }
        sb.append("\n")
    }
    
    val fos = FileOutputStream(file)
    fos.write(sb.toString().toByteArray())
    fos.close()
    return file
}

fun generateJsonReport(context: Context, logs: List<MigraineLog>, dateRangeStr: String, lang: String): File {
    val exportDir = File(context.cacheDir, "exports")
    if (!exportDir.exists()) exportDir.mkdirs()
    val file = File(exportDir, "Migraine_Medical_Report.json")
    if (file.exists()) file.delete()
    
    val jsonArray = JSONArray()
    for (log in logs) {
        val jsonObject = JSONObject().apply {
            put("id", log.id)
            put("dateString", log.dateString)
            put("year", log.year)
            put("month", log.month)
            put("day", log.day)
            put("intensity", log.intensity)
            put("symptoms", log.symptoms)
            put("triggers", log.triggers)
            put("note", log.note)
            put("timestamp", log.timestamp)
        }
        jsonArray.put(jsonObject)
    }
    
    val fos = FileOutputStream(file)
    fos.write(jsonArray.toString(2).toByteArray())
    fos.close()
    return file
}

fun shareFile(context: Context, file: File, mimeType: String, lang: String) {
    try {
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, Translate.t("share_sheet_title", lang))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, Translate.t("share_sheet_title", lang)))
    } catch (e: Exception) {
        Toast.makeText(context, "${Translate.t("error_sharing_file", lang)}: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun ExportScreen(viewModel: MigraineViewModel) {
    val theme = LocalAppTheme.current
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()

    var selectedFormat by remember { mutableStateOf("pdf") } // "pdf", "csv", "txt", "json"
    var startDateFilter by remember { mutableStateOf<String?>(null) }
    var endDateFilter by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val parseDate: (String) -> Date? = remember {
        { dateStr ->
            try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) } catch (e: Exception) { null }
        }
    }

    val filteredLogs = remember(allLogs, startDateFilter, endDateFilter) {
        allLogs.filter { log ->
            val logDate = parseDate(log.dateString)
            if (logDate == null) {
                true
            } else {
                val start = startDateFilter?.let { parseDate(it) }
                val end = endDateFilter?.let { parseDate(it) }
                when {
                    start != null && end != null -> !logDate.before(start) && !logDate.after(end)
                    start != null -> !logDate.before(start)
                    end != null -> !logDate.after(end)
                    else -> true
                }
            }
        }.sortedByDescending { it.dateString }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = Translate.t("export_title", lang),
            fontFamily = theme.fontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = theme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = Translate.t("export_subtitle", lang),
            fontFamily = theme.fontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = theme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = Translate.t("export_desc", lang),
            fontFamily = theme.fontFamily,
            fontSize = 12.sp,
            color = theme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), theme.cardShape),
            shape = theme.cardShape,
            colors = CardDefaults.cardColors(containerColor = theme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = Translate.t("desc_format_selection", lang),
                        tint = theme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Translate.t("format_selection", lang),
                        fontFamily = theme.fontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = theme.onSurfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                val formats = listOf(
                    "pdf" to Translate.t("format_pdf", lang),
                    "csv" to Translate.t("format_csv", lang),
                    "txt" to Translate.t("format_report", lang),
                    "json" to Translate.t("format_json", lang)
                )

                formats.forEach { (fmt, label) ->
                    val isSelected = selectedFormat == fmt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = fmt }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedFormat = fmt },
                            colors = RadioButtonDefaults.colors(selectedColor = theme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontFamily = theme.fontFamily,
                            fontSize = 14.sp,
                            color = theme.onSurface
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), theme.cardShape),
            shape = theme.cardShape,
            colors = CardDefaults.cardColors(containerColor = theme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = Translate.t("desc_date_range_selection", lang),
                        tint = theme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Translate.t("date_range_selection", lang),
                        fontFamily = theme.fontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = theme.onSurfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val currentCal = Calendar.getInstance()
                            startDateFilter?.let {
                                parseDate(it)?.let { date -> currentCal.time = date }
                            }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    startDateFilter = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                },
                                currentCal.get(Calendar.YEAR),
                                currentCal.get(Calendar.MONTH),
                                currentCal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = theme.buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (startDateFilter != null) theme.primary.copy(alpha = 0.15f) else theme.surfaceContainer,
                            contentColor = if (startDateFilter != null) theme.primary else theme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = startDateFilter ?: Translate.t("from_date", lang),
                            fontFamily = theme.fontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (startDateFilter != null) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Text(
                        text = Translate.t("to", lang),
                        fontFamily = theme.fontFamily,
                        fontSize = 13.sp,
                        color = theme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val currentCal = Calendar.getInstance()
                            endDateFilter?.let {
                                parseDate(it)?.let { date -> currentCal.time = date }
                            }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    endDateFilter = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                },
                                currentCal.get(Calendar.YEAR),
                                currentCal.get(Calendar.MONTH),
                                currentCal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = theme.buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (endDateFilter != null) theme.primary.copy(alpha = 0.15f) else theme.surfaceContainer,
                            contentColor = if (endDateFilter != null) theme.primary else theme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = endDateFilter ?: Translate.t("to_date", lang),
                            fontFamily = theme.fontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (endDateFilter != null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            val endStr = dateFormat.format(cal.time)
                            cal.add(Calendar.DAY_OF_YEAR, -7)
                            val startStr = dateFormat.format(cal.time)
                            startDateFilter = startStr
                            endDateFilter = endStr
                        },
                        shape = theme.buttonShape,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary)
                    ) {
                        Text(Translate.t("last_7d", lang), fontSize = 11.sp, fontFamily = theme.fontFamily)
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            val endStr = dateFormat.format(cal.time)
                            cal.add(Calendar.DAY_OF_YEAR, -30)
                            val startStr = dateFormat.format(cal.time)
                            startDateFilter = startStr
                            endDateFilter = endStr
                        },
                        shape = theme.buttonShape,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary)
                    ) {
                        Text(Translate.t("last_30d", lang), fontSize = 11.sp, fontFamily = theme.fontFamily)
                    }

                    OutlinedButton(
                        onClick = {
                            startDateFilter = null
                            endDateFilter = null
                        },
                        shape = theme.buttonShape,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary)
                    ) {
                        Text(Translate.t("reset_all", lang), fontSize = 11.sp, fontFamily = theme.fontFamily)
                    }
                }
            }
        }

        val count = filteredLogs.size
        val dateRangeString = when {
            startDateFilter != null && endDateFilter != null -> "$startDateFilter ${Translate.t("to", lang)} $endDateFilter"
            startDateFilter != null -> "${Translate.t("from_date_label", lang)} $startDateFilter"
            endDateFilter != null -> "${Translate.t("until_date_label", lang)} $endDateFilter"
            else -> Translate.t("all_history", lang)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .border(1.dp, theme.primary.copy(alpha = 0.2f), theme.cardShape),
            shape = theme.cardShape,
            colors = CardDefaults.cardColors(containerColor = theme.primary.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = String.format(Locale.getDefault(), Translate.t("logs_count_found", lang), count),
                    fontFamily = theme.fontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${Translate.t("date_range_selection", lang).replace("2. ", "")}: $dateRangeString",
                    fontFamily = theme.fontFamily,
                    fontSize = 12.sp,
                    color = theme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Button(
            onClick = {
                triggerHaptic(context, MyHapticStyle.HEAVY)
                if (filteredLogs.isEmpty()) {
                    Toast.makeText(context, Translate.t("export_no_data", lang), Toast.LENGTH_LONG).show()
                } else {
                    val exportFile = when (selectedFormat) {
                        "pdf" -> generatePdfReport(context, filteredLogs, dateRangeString, lang)
                        "csv" -> generateCsvReport(context, filteredLogs, dateRangeString, lang)
                        "txt" -> generateTxtReport(context, filteredLogs, dateRangeString, lang)
                        "json" -> generateJsonReport(context, filteredLogs, dateRangeString, lang)
                        else -> null
                    }
                    if (exportFile != null && exportFile.exists()) {
                        val mimeType = when (selectedFormat) {
                            "pdf" -> "application/pdf"
                            "csv" -> "text/csv"
                            "txt" -> "text/plain"
                            "json" -> "application/json"
                            else -> "*/*"
                        }
                        shareFile(context, exportFile, mimeType, lang)
                        Toast.makeText(context, Translate.t("export_success", lang), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, Translate.t("error_generating_report", lang), Toast.LENGTH_LONG).show()
                    }
                }
            },
            shape = theme.buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.accent,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = Translate.t("desc_share", lang),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Translate.t("btn_generate_export", lang),
                fontFamily = theme.fontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(60.dp))
    }
}


@Composable
fun SettingsScreen(viewModel: MigraineViewModel) {
    val theme = LocalAppTheme.current
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Main Screen Header styled elegantly
        Text(
            text = Translate.t("settings_title", lang),
            fontFamily = theme.fontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = theme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. Language Toggle Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), theme.cardShape),
            shape = theme.cardShape,
            colors = CardDefaults.cardColors(containerColor = theme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = Translate.t("desc_language_settings", lang),
                        tint = theme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Translate.t("language_section", lang),
                        fontFamily = theme.fontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = theme.onSurfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = Translate.t("default_locale", lang),
                    fontFamily = theme.fontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.onSurface
                )
                Text(
                    text = Translate.t("select_locale_desc", lang),
                    fontFamily = theme.fontFamily,
                    fontSize = 12.sp,
                    color = theme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Selectable Language buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("en", "fr").forEach { localeCode ->
                        val isSelected = lang == localeCode
                        val activeColor = theme.accent
                        val borderColor = if (isSelected) activeColor else theme.onSurfaceVariant.copy(alpha = 0.15f)
                        val backgroundBrush = if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(theme.buttonShape)
                                .background(backgroundBrush)
                                .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = theme.buttonShape)
                                .clickable {
                                    viewModel.setAppLanguage(localeCode)
                                    context.getSharedPreferences("migraine_settings", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("language", localeCode)
                                        .apply()
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = Translate.t("desc_selected", lang),
                                        tint = theme.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (localeCode == "en") Translate.t("english", lang) else Translate.t("french", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) theme.accent else theme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Active Visual Preferences Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), theme.cardShape),
            shape = theme.cardShape,
            colors = CardDefaults.cardColors(containerColor = theme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = Translate.t("desc_visual_settings", lang),
                        tint = theme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Translate.t("theme_section", lang),
                        fontFamily = theme.fontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = theme.onSurfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = Translate.t("theme_p", lang),
                    fontFamily = theme.fontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.onSurface
                )
                Text(
                    text = Translate.t("theme_desc", lang),
                    fontFamily = theme.fontFamily,
                    fontSize = 12.sp,
                    color = theme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Interactive theme selection list with live brand palettes
                val currentSavedTheme by viewModel.appTheme.collectAsStateWithLifecycle()
                val isDark = isSystemInDarkTheme()
                val themeList = listOf("auto", "misty_day", "soothing_night", "forest_breath", "cosmic_lavender", "warm_terracotta")

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    themeList.forEach { themeId ->
                        val isSelected = currentSavedTheme == themeId
                        val activeColor = theme.accent
                        val targetSpecs = if (themeId == "auto") {
                            getThemeSpecs(if (isDark) "soothing_night" else "misty_day")
                        } else {
                            getThemeSpecs(themeId)
                        }

                        val rowBorderColor = if (isSelected) activeColor else theme.onSurfaceVariant.copy(alpha = 0.12f)
                        val rowBg = if (isSelected) activeColor.copy(alpha = 0.08f) else theme.surfaceContainerLow

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(theme.cardShape)
                                .background(rowBg)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = rowBorderColor,
                                    shape = theme.cardShape
                                )
                                .clickable {
                                    viewModel.setAppTheme(themeId)
                                    context.getSharedPreferences("migraine_settings", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("theme", themeId)
                                        .apply()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) activeColor else theme.onSurfaceVariant.copy(alpha = 0.5f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(activeColor)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = Translate.t("theme_$themeId", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = theme.onSurface
                                )
                            }

                            // Theme palette dot preview
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (themeId == "auto") {
                                    Text(
                                        text = Translate.t("system", lang),
                                        fontSize = 11.sp,
                                        fontFamily = theme.fontFamily,
                                        color = theme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                // Circle 1: background
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(targetSpecs.background)
                                        .border(0.5.dp, theme.onSurface.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                                )
                                // Circle 2: surface
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(targetSpecs.surface)
                                        .border(0.5.dp, theme.onSurface.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                                )
                                // Circle 3: primary
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(targetSpecs.primary)
                                        .border(0.5.dp, theme.onSurface.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Interactive Tutorial Onboarding Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), theme.cardShape),
            shape = theme.cardShape,
            colors = CardDefaults.cardColors(containerColor = theme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = Translate.t("desc_interactive_tutorial_icon", lang),
                        tint = theme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Translate.t("tutorial_btn", lang),
                        fontFamily = theme.fontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = theme.onSurfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = Translate.t("tutorial_desc", lang),
                    fontFamily = theme.fontFamily,
                    fontSize = 12.sp,
                    color = theme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        triggerHaptic(context, MyHapticStyle.MEDIUM)
                        viewModel.setCurrentScreen(AppScreen.DASHBOARD)
                        viewModel.setOnboardingActive(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                    shape = theme.buttonShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = Translate.t("tutorial_btn_replay", lang),
                        fontFamily = theme.fontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.surface
                    )
                }
            }
        }

        // 4. About Migraine Journal card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 110.dp) // generous spacer to avoid overlapping the bottom navigation
                .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.12f), theme.cardShape),
            shape = theme.cardShape,
            colors = CardDefaults.cardColors(containerColor = theme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = Translate.t("about_app", lang),
                    fontFamily = theme.fontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = Translate.t("about_desc", lang),
                    fontFamily = theme.fontFamily,
                    fontSize = 13.sp,
                    color = theme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = theme.onSurfaceVariant.copy(alpha = 0.08f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Translate.t("developed_by", lang),
                        fontFamily = theme.fontFamily,
                        fontSize = 11.sp,
                        color = theme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = Translate.t("version", lang),
                        fontFamily = theme.fontFamily,
                        fontSize = 11.sp,
                        color = theme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = Translate.t("streak_label", lang),
                    fontFamily = theme.fontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = theme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

object Translate {
    private val translations = mapOf(
        "en" to mapOf(
            "app_title" to "Migraine Tracker",
            "recent_episodes" to "Recent Episodes",
            "see_all" to "See all",
            "no_journals_logged_yet" to "No migraine journals logged yet.\nTap any date on the calendar to begin tracking.",
            "pain_scale_label" to "Pain",
            "no_comments_entered" to "No detailed comments entered.",
            "symptom" to "Symptom",
            "trigger" to "Trigger",
            "back_to_dashboard" to "Back to Dashboard",
            "all_episodes_title" to "All Episodes & Notes",
            "search_placeholder" to "Search notes, symptoms, triggers...",
            "filter_by_symptom" to "Filter by Symptom Tags",
            "filter_by_daterange" to "Filter by Date Range",
            "from_date" to "From Date",
            "to_date" to "To Date",
            "to" to "to",
            "last_7d" to "Last 7d",
            "last_30d" to "Last 30d",
            "reset_all" to "Reset All",
            "no_matching_episodes" to "No matching episodes found.",
            "adjust_filters_tip" to "Try adjusting your search queries or filter tags.",
            "clear_filters_btn" to "Clear Active Filters",
            "editor_title" to "Journal Editor",
            "intensity" to "Intensity",
            "pain_level" to "Pain level",
            "symptoms" to "Symptoms",
            "triggers" to "Triggers",
            "notes_observations" to "Notes & Observations",
            "notes_placeholder" to "Add description here...",
            "close" to "Close",
            "save_entry" to "Save Entry",
            "active" to "active",
            "edit" to "Edit",
            "delete" to "Delete",
            "is_read_only" to "Read Only - Tap Edit to change",
            "is_editing_mode" to "Editing Mode",
            "settings_title" to "Global App Settings",
            "app_language" to "App Language",
            "theme_p" to "Visual Theme",
            "lang_selection_desc" to "Change default language",
            "english" to "English",
            "french" to "Français",
            "developed_by" to "Developed by Kevin Jobin",
            "about_desc" to "Quick and painless migraine journaling.",
            "save" to "Save",
            "dashboard_tab" to "Dashboard",
            "history_tab" to "History",
            "export_tab" to "Export",
            "settings_tab" to "Settings",
            "export_title" to "Export Medical Journal",
            "export_subtitle" to "Share your symptom chronicles with your doctor.",
            "export_desc" to "Select a starting and ending date range, pick your preferred medical document format, and generate a customized summary sheet to share instantly with your physician.",
            "format_selection" to "1. Select Export Format",
            "date_range_selection" to "2. Select Date Range",
            "btn_generate_export" to "Generate & Share Report",
            "export_success" to "Report generated successfully!",
            "export_no_data" to "No migraine logs found during this range.",
            "all_history" to "All History (Default)",
            "format_pdf" to "Doctor's PDF Report (.pdf)",
            "format_csv" to "Spreadsheet CSV Table (.csv)",
            "format_report" to "Doctor's Report Text Summary (.txt)",
            "format_json" to "Raw Portable Backup Dataset (.json)",
            "share_sheet_title" to "Share Migraine Report",
            "logs_count_found" to "%d active entries found in selected range.",
            "language_section" to "Language Settings",
            "default_locale" to "Default Locale",
            "select_locale_desc" to "Configure default interface locale",
            "theme_section" to "Visual Tone Preference",
            "theme_desc" to "Active styling theme based on system defaults",
            "system_theme" to "System Auto-Theme",
            "theme_auto" to "System Auto",
            "theme_misty_day" to "Misty Day (Light)",
            "theme_soothing_night" to "Soothing Night (Dark)",
            "theme_forest_breath" to "Forest Breath (Dark Green)",
            "theme_cosmic_lavender" to "Cosmic Lavender (Dark Purple)",
            "theme_warm_terracotta" to "Warm Terracotta (Earthy Orange)",
            "about_app" to "About Migraine Journal",
            "version" to "Version 1.2.0",
            "streak_label" to "Consistent logs and tracking help identify headache triggers accurately.",
            "tutorial_btn" to "Interactive Tutorial",
            "tutorial_desc" to "Learn how to track, filter, and customize your migraine logging.",
            "tutorial_btn_replay" to "Replay App Tutorial",
            "tut_welcome" to "Welcome to Migraine Tracker",
            "tut_welcome_desc" to "Let's take a quick 5-step interactive tour to master your symptom and trigger tracking.",
            "tut_step1_title" to "1. Log Symptoms & Triggers",
            "tut_step1_desc" to "Practice adjusting pain levels, symptoms, and notes in this live interactive sandbox:",
            "tut_step1_sub" to "Tap 'Test Save' to preview how logging works!",
            "tut_step2_title" to "2. Screen Navigation",
            "tut_step2_desc" to "Tap the tabs on this interactive preview to explore the Dashboard, History, and Settings functions:",
            "tut_step3_title" to "3. Search & Advanced Filtering",
            "tut_step3_desc" to "Try typing search terms (e.g., 'Aura') to test the real-time History Filter system live:",
            "tut_step4_title" to "4. Visual Customization & Themes",
            "tut_step4_desc" to "Tap any circle below to instantly update the entire application to your favorite theme look & feel:",
            "tut_try_save" to "Test Save Log",
            "tut_save_success" to "Simulated Log Saved! 📅",
            "btn_skip" to "Skip",
            "btn_next" to "Next",
            "btn_back" to "Back",
            "btn_finish" to "Finish Tour",
            "lang_select_title" to "Preferred Language",
            "lang_select_desc" to "Select your language to customize your tracking and reports.",
            "lang_english" to "English",
            "lang_french" to "Français",
            "lang_confirm" to "Let's Begin",
            "symptoms_detected" to "Symptoms Detected",
            "suspected_triggers" to "Suspected Triggers",
            "notes" to "Notes",
            "no_entry_this_day" to "No migraine entry registered on this day",
            "log_episode_btn" to "+ Log Episode",
            "symptoms_experienced" to "Symptoms Experienced",
            "no_symptoms_recorded" to "No symptoms recorded for this episode.",
            "no_triggers_recorded" to "No suspected triggers recorded.",
            "edit_note" to "Edit Note",
            "save_note" to "Save Note",
            "no_description_entered" to "No description entered.",
            "error_sharing_file" to "Error sharing file",
            "error_generating_report" to "Failed to generate report",
            "no_sleep" to "No Sleep",
            "severity_mild" to "Mild",
            "severity_moderate" to "Moderate",
            "severity_severe" to "Severe",
            "severity_debilitating" to "Debilitating",
            "pain_intensity" to "Pain Intensity",
            "severity_desc_1_2" to "Very mild head pressure. Easy to ignore without medication.",
            "severity_desc_3_4" to "Noticeable ache. Able to work/study but active distraction present.",
            "severity_desc_5_6" to "Moderately strong pain. Difficult to concentrate or stay active.",
            "severity_desc_7_8" to "Severe, throbbing head pain. Limits physical and mental activities.",
            "severity_desc_9_10" to "Debilitating, intense pain. Requires immediate rest in a dark room.",
            "describe_feeling" to "Describe how are you feeling?",
            "notes_for" to "Notes for %s",
            "viewing_mode_desc" to "Viewing Mode — Tap 'Edit Note' at the bottom to edit.",
            "editing_mode_desc" to "Editing Mode — Make changes & click 'Save Note'.",
            "from_date_label" to "From",
            "until_date_label" to "Until",
            "splash_tagline" to "A quiet space for mindful logging & clarity",
            "splash_connecting" to "Connecting...",
            "tut_info_tip_dashboard" to "📅 Dashboard: Shows color indicators on migraine dates. Double tap to check current tracking streak stats!",
            "tut_info_tip_history" to "🔍 History Feed: Your scrollable timelines. Perfect for examining specific past triggers or exporting summaries.",
            "tut_info_tip_settings" to "⚙️ Settings Panel: Swap language locales, backup local SQLite logs, or replay this tutorial anytime.",
            "tut_search_placeholder" to "Type to test search filtering...",
            "tut_no_results" to "No matching results",
            "theme_label_misty_day" to "Misty",
            "theme_label_soothing_night" to "Calm",
            "theme_label_forest_breath" to "Forest",
            "theme_label_cosmic_lavender" to "Cosmic",
            "theme_label_warm_terracotta" to "Terracotta",
            "tag_aura" to "Aura",
            "tag_temple_pressure" to "Temple Pressure",
            "tag_nausea" to "Nausea",
            "tag_light_sensitivity" to "Light Sensitivity",
            "tag_sound_sensitivity" to "Sound Sensitivity",
            "tag_throbbing_pain" to "Throbbing Pain",
            "tag_blurred_vision" to "Blurred Vision",
            "tag_lack_of_sleep" to "Lack of Sleep",
            "tag_bright_lights" to "Bright Lights",
            "tag_stress" to "Stress",
            "tag_caffeine" to "Caffeine",
            "tag_skipped_meal" to "Skipped Meal",
            "tag_weather_change" to "Weather Change",
            "tag_dehydration" to "Dehydration",
            "calendar_mode_month" to "Month",
            "calendar_mode_week" to "Week",
            "calendar_mode_day" to "Day",
            "calendar_mode_year" to "Year",
            "desc_no_journals" to "No journals registered",
            "desc_search" to "Search",
            "desc_clear_search" to "Clear search",
            "desc_filters" to "Filters",
            "desc_start_date" to "Start Date",
            "desc_end_date" to "End Date",
            "desc_no_results" to "No results",
            "desc_delete_entry" to "Delete entry",
            "desc_prev_month" to "Previous Month",
            "desc_next_month" to "Next Month",
            "desc_prev_week" to "Previous Week",
            "desc_next_week" to "Next Week",
            "desc_prev_day" to "Previous Day",
            "desc_next_day" to "Next Day",
            "desc_edit_tracker" to "Edit Tracker",
            "desc_delete_tracker" to "Delete Tracker",
            "desc_no_records" to "No records",
            "desc_see_all_episodes" to "See all episodes",
            "desc_delete_note" to "Delete note",
            "desc_minimize_sheet" to "Minimize sheet",
            "desc_maximize_sheet" to "Maximize sheet",
            "desc_close_editor" to "Close editor",
            "desc_read_only_mode" to "Read-Only Mode Indicator",
            "desc_editing_mode" to "Editing Mode Indicator",
            "desc_collapse" to "Collapse",
            "desc_expand" to "Expand",
            "desc_format_selection" to "Format selection",
            "desc_date_range_selection" to "Date range selection",
            "desc_share" to "Share",
            "desc_language_settings" to "Language settings",
            "desc_selected" to "Selected",
            "desc_visual_settings" to "Visual settings",
            "desc_interactive_tutorial_icon" to "Interactive Tutorial icon",
            "prev_year" to "Previous Year",
            "next_year" to "Next Year",
            "system" to "System",
            "pdf_report_title" to "Migraine Medical Report",
            "pdf_date_range" to "Date Range: %s",
            "pdf_generated_on" to "Generated on: %s",
            "pdf_episodes_count" to "Total Episodes: %d",
            "pdf_average_pain_val" to "Average Pain: %s/10",
            "pdf_header_date" to "Date",
            "pdf_header_pain" to "Pain",
            "pdf_header_symptoms" to "Symptoms",
            "pdf_header_triggers" to "Triggers",
            "pdf_header_notes" to "Notes",
            "csv_header_date" to "Date",
            "csv_header_pain" to "Pain Intensity",
            "csv_header_symptoms" to "Symptoms",
            "csv_header_triggers" to "Triggers",
            "csv_header_notes" to "Notes",
            "txt_report_title" to "MIGRAINE MEDICAL REPORT",
            "txt_date_range" to "Date Range: ",
            "txt_generated_on" to "Generated on: ",
            "txt_total_episodes" to "Total Episodes: ",
            "txt_average_pain" to "Average Pain: ",
            "txt_detail_title" to "EPISODE LOGS:",
            "txt_date" to "Date: ",
            "txt_intensity" to "Pain Intensity: ",
            "txt_symptoms" to "Symptoms: ",
            "txt_triggers" to "Triggers: ",
            "txt_notes" to "Notes: "
        ),
        "fr" to mapOf(
            "app_title" to "Journal de Migraine",
            "recent_episodes" to "Épisodes Récents",
            "see_all" to "Voir tout",
            "no_journals_logged_yet" to "Aucun journal de migraine enregistré.\nAppuyez sur une date du calendrier pour commencer à suivre.",
            "pain_scale_label" to "Douleur",
            "no_comments_entered" to "Aucun commentaire détaillé saisi.",
            "symptom" to "Symptôme",
            "trigger" to "Déclencheur",
            "back_to_dashboard" to "Tableau de Bord",
            "all_episodes_title" to "Tous les Épisodes & Notes",
            "search_placeholder" to "Rechercher notes, symptômes, déclencheurs...",
            "filter_by_symptom" to "Filtrer par symptômes",
            "filter_by_daterange" to "Filtrer par plage de dates",
            "from_date" to "Date de début",
            "to_date" to "Date de fin",
            "to" to "à",
            "last_7d" to "7 derniers j.",
            "last_30d" to "30 derniers j.",
            "reset_all" to "Réinitialiser",
            "no_matching_episodes" to "Aucun épisode correspondant.",
            "adjust_filters_tip" to "Essayez d'ajuster votre recherche ou vos filtres.",
            "clear_filters_btn" to "Effacer les filtres",
            "editor_title" to "Éditeur de Journal",
            "intensity" to "Intensité",
            "pain_level" to "Niveau de douleur",
            "symptoms" to "Symptômes",
            "triggers" to "Déclencheurs",
            "notes_observations" to "Notes & Observations",
            "notes_placeholder" to "Ajoutez des notes ou observations ici...",
            "close" to "Fermer",
            "save_entry" to "Enregistrer l'entrée",
            "active" to "actifs",
            "edit" to "Modifier",
            "delete" to "Supprimer",
            "is_read_only" to "Lecture seule - Appuyez sur Modifier pour changer",
            "is_editing_mode" to "Mode Édition",
            "settings_title" to "Paramètres Généraux",
            "app_language" to "Langue de l'app",
            "theme_p" to "Thème Visuel",
            "lang_selection_desc" to "Changer la langue de l'interface",
            "english" to "Anglais",
            "french" to "Français",
            "developed_by" to "Développé par Kevin Jobin",
            "about_desc" to "Journalisation simple et rapide des migraines.",
            "save" to "Enregistrer",
            "dashboard_tab" to "Tableau de Bord",
            "history_tab" to "Historique",
            "export_tab" to "Exporter",
            "settings_tab" to "Paramètres",
            "export_title" to "Exporter le Journal Médical",
            "export_subtitle" to "Partagez votre historique de symptômes avec votre médecin.",
            "export_desc" to "Sélectionnez une période, choisissez votre format de rapport préféré et générez un état récapitulatif à partager immédiatement avec votre médecin.",
            "format_selection" to "1. Sélectionner le Format",
            "date_range_selection" to "2. Sélectionner la Période",
            "btn_generate_export" to "Générer & Partager",
            "export_success" to "Rapport généré avec succès !",
            "export_no_data" to "Aucun journal de migraine trouvé sur cette période.",
            "all_history" to "Tout l'historique (Par défaut)",
            "format_pdf" to "Rapport PDF pour le médecin (.pdf)",
            "format_csv" to "Tableur CSV (.csv)",
            "format_report" to "Rapport Médical Structuré (.txt)",
            "format_json" to "Données Brutes Réutilisables (.json)",
            "share_sheet_title" to "Partager le Rapport",
            "logs_count_found" to "%d entrées de migraine trouvées pour l'export.",
            "language_section" to "Paramètres de Langue",
            "default_locale" to "Option Régionale",
            "select_locale_desc" to "Configurer la langue de l'interface",
            "theme_section" to "Ton Visuel Préféré",
            "theme_desc" to "Le thème s'adapte automatiquement à votre système.",
            "system_theme" to "Thème Automatique",
            "theme_auto" to "Thème Automatique",
            "theme_misty_day" to "Misty Day (Lumineux)",
            "theme_soothing_night" to "Soothing Night (Sombre)",
            "theme_forest_breath" to "Forest Breath (Vert Forêt)",
            "theme_cosmic_lavender" to "Cosmic Lavender (Pourpre Lunaire)",
            "theme_warm_terracotta" to "Warm Terracotta (Terre Cuite)",
            "about_app" to "À Propos de l'App",
            "version" to "Version 1.2.0",
            "streak_label" to "Un suivi régulier aide à identifier précisément les déclencheurs de maux de tête.",
            "tutorial_btn" to "Tutoriel Interactif",
            "tutorial_desc" to "Découvrez comment suivre, filtrer et personnaliser votre journal.",
            "tutorial_btn_replay" to "Rejouer le Tutoriel",
            "tut_welcome" to "Bienvenue dans le Journal de Migraine",
            "tut_welcome_desc" to "Faisons un tour de 5 étapes interactif pour maîtriser l'enregistrement de vos symptômes et crises.",
            "tut_step1_title" to "1. Journaliser des Symptômes",
            "tut_step1_desc" to "Ajustez le curseur de douleur, cochez des symptômes ou saisissez des notes dans ce bac à sable d'essai :",
            "tut_step1_sub" to "Appuyez sur 'Enregistrer l'essai' pour voir comment fonctionne la saisie !",
            "tut_step2_title" to "2. Navigation entre Ecrans",
            "tut_step2_desc" to "Cliquez sur les onglets de ce menu fictif pour comprendre l'organisation de l'application :",
            "tut_step3_title" to "3. Recherche & Filtrage d'Historique",
            "tut_step3_desc" to "Saisissez un terme de recherche (ex: 'Aura') pour tester le filtre instantané interactif :",
            "tut_step4_title" to "4. Thématisation & Couleurs",
            "tut_step4_desc" to "Sélectionnez une couleur pour tester en direct le changement de thème de l'application :",
            "tut_try_save" to "Enregistrer l'essai",
            "tut_save_success" to "Log simulé enregistré ! 📅",
            "btn_skip" to "Passer",
            "btn_next" to "Suivant",
            "btn_back" to "Retour",
            "btn_finish" to "Terminer",
            "lang_select_title" to "Langue Préférée",
            "lang_select_desc" to "Sélectionnez votre langue pour personnaliser votre suivi et vos rapports.",
            "lang_english" to "English",
            "lang_french" to "Français",
            "lang_confirm" to "Commencer",
            "symptoms_detected" to "Symptômes Détectés",
            "suspected_triggers" to "Déclencheurs Suspectés",
            "notes" to "Notes",
            "no_entry_this_day" to "Aucun épisode enregistré ce jour",
            "log_episode_btn" to "+ Enregistrer crise",
            "symptoms_experienced" to "Symptômes Rencontrés",
            "no_symptoms_recorded" to "Aucun symptôme enregistré pour cet épisode.",
            "no_triggers_recorded" to "Aucun déclencheur suspecté enregistré.",
            "edit_note" to "Modifier la Note",
            "save_note" to "Enregistrer la Note",
            "no_description_entered" to "Aucune description saisie.",
            "error_sharing_file" to "Erreur lors du partage du fichier",
            "error_generating_report" to "Échec de la génération du rapport",
            "no_sleep" to "Sommeil",
            "severity_mild" to "Légère",
            "severity_moderate" to "Modérée",
            "severity_severe" to "Sévère",
            "severity_debilitating" to "Handicapante",
            "pain_intensity" to "Intensité de la douleur",
            "severity_desc_1_2" to "Pression crânienne très légère. Facile à ignorer sans traitement.",
            "severity_desc_3_4" to "Douleur perceptible. Capable de travailler/étudier mais distraction active présente.",
            "severity_desc_5_6" to "Douleur modérément forte. Difficile de se concentrer ou de rester actif.",
            "severity_desc_7_8" to "Douleur intense et lancinante. Limite les activités physiques et mentales.",
            "severity_desc_9_10" to "Douleur invalidante et intense. Nécessite un repos immédiat dans une pièce sombre.",
            "describe_feeling" to "Décrivez comment vous vous sentez ?",
            "notes_for" to "Notes pour %s",
            "viewing_mode_desc" to "Mode Lecture — Appuyez sur 'Modifier' en bas pour modifier.",
            "editing_mode_desc" to "Mode Édition — Faites des modifications et cliquez sur 'Enregistrer'.",
            "from_date_label" to "Depuis",
            "until_date_label" to "Jusqu'au",
            "splash_tagline" to "Prenez soin de vous au quotidien",
            "splash_connecting" to "Se connecter...",
            "tut_info_tip_dashboard" to "📅 Tableau de Bord : Affiche les jours avec migraines. Appuyez deux fois pour voir vos statistiques de suivi !",
            "tut_info_tip_history" to "🔍 Historique : Journal chronologique complet. Idéal pour faire défiler vos saisies et consulter vos rapports.",
            "tut_info_tip_settings" to "⚙️ Paramètres : Changez la langue de l'app, exportez des sauvegardes ou rejouez ce guide.",
            "tut_search_placeholder" to "Saisissez pour filtrer...",
            "tut_no_results" to "Aucun résultat",
            "theme_label_misty_day" to "Brume",
            "theme_label_soothing_night" to "Calme",
            "theme_label_forest_breath" to "Forêt",
            "theme_label_cosmic_lavender" to "Cosmique",
            "theme_label_warm_terracotta" to "Terracotta",
            "tag_aura" to "Aura",
            "tag_temple_pressure" to "Pression temporale",
            "tag_nausea" to "Nausée",
            "tag_light_sensitivity" to "Sensibilité lumière",
            "tag_sound_sensitivity" to "Sensibilité bruit",
            "tag_throbbing_pain" to "Douleur lancinante",
            "tag_blurred_vision" to "Vision floue",
            "tag_lack_of_sleep" to "Manque de sommeil",
            "tag_bright_lights" to "Lumières vives",
            "tag_stress" to "Stress",
            "tag_caffeine" to "Caféine",
            "tag_skipped_meal" to "Repas sauté",
            "tag_weather_change" to "Changement météo",
            "tag_dehydration" to "Déshydratation",
            "calendar_mode_month" to "Mois",
            "calendar_mode_week" to "Semaine",
            "calendar_mode_day" to "Jour",
            "calendar_mode_year" to "Année",
            "desc_no_journals" to "Aucun journal enregistré",
            "desc_search" to "Rechercher",
            "desc_clear_search" to "Effacer la recherche",
            "desc_filters" to "Filtres",
            "desc_start_date" to "Date de début",
            "desc_end_date" to "Date de fin",
            "desc_no_results" to "Aucun résultat",
            "desc_delete_entry" to "Supprimer l'entrée",
            "desc_prev_month" to "Mois précédent",
            "desc_next_month" to "Mois suivant",
            "desc_prev_week" to "Semaine précédente",
            "desc_next_week" to "Semaine suivante",
            "desc_prev_day" to "Jour précédent",
            "desc_next_day" to "Jour suivant",
            "desc_edit_tracker" to "Modifier le suivi",
            "desc_delete_tracker" to "Supprimer le suivi",
            "desc_no_records" to "Aucun enregistrement",
            "desc_see_all_episodes" to "Voir tous les épisodes",
            "desc_delete_note" to "Supprimer la note",
            "desc_minimize_sheet" to "Réduire le volet",
            "desc_maximize_sheet" to "Agrandir le volet",
            "desc_close_editor" to "Fermer l'éditeur",
            "desc_read_only_mode" to "Indicateur de mode lecture seule",
            "desc_editing_mode" to "Indicateur de mode édition",
            "desc_collapse" to "Réduire",
            "desc_expand" to "Développer",
            "desc_format_selection" to "Sélection du format",
            "desc_date_range_selection" to "Sélection de la période",
            "desc_share" to "Partager",
            "desc_language_settings" to "Paramètres de langue",
            "desc_selected" to "Sélectionné",
            "desc_visual_settings" to "Paramètres visuels",
            "desc_interactive_tutorial_icon" to "Icône du tutoriel interactif",
            "prev_year" to "Année précédente",
            "next_year" to "Année suivante",
            "system" to "Système",
            "pdf_report_title" to "Rapport Médical de Migraine",
            "pdf_date_range" to "Période : %s",
            "pdf_generated_on" to "Généré le : %s",
            "pdf_episodes_count" to "Nombre de crises : %d",
            "pdf_average_pain_val" to "Douleur moyenne : %s/10",
            "pdf_header_date" to "Date",
            "pdf_header_pain" to "Douleur",
            "pdf_header_symptoms" to "Symptômes",
            "pdf_header_triggers" to "Déclencheurs",
            "pdf_header_notes" to "Notes",
            "csv_header_date" to "Date",
            "csv_header_pain" to "Intensité de douleur",
            "csv_header_symptoms" to "Symptômes",
            "csv_header_triggers" to "Déclencheurs",
            "csv_header_notes" to "Notes",
            "txt_report_title" to "RAPPORT MÉDICAL DE MIGRAINE",
            "txt_date_range" to "Période : ",
            "txt_generated_on" to "Généré le : ",
            "txt_total_episodes" to "Nombre total de crises : ",
            "txt_average_pain" to "Douleur moyenne : ",
            "txt_detail_title" to "DÉTAIL DES CRISES :",
            "txt_date" to "Date : ",
            "txt_intensity" to "Intensité : ",
            "txt_symptoms" to "Symptômes : ",
            "txt_triggers" to "Déclencheurs : ",
            "txt_notes" to "Notes : "
        )
    )

    fun t(key: String, lang: String): String {
        val dict = translations[lang] ?: translations["en"]!!
        return dict[key] ?: (translations["en"]!![key] ?: key)
    }

    fun translateCsvTags(tagsString: String, lang: String): String {
        if (tagsString.isBlank()) return ""
        return tagsString.split(",").map { it.trim() }.filter { it.isNotBlank() }
            .map { t("tag_" + it.lowercase().replace(" ", "_"), lang) }
            .joinToString(", ")
    }
}

@Composable
fun AppLottiePlayer(
    url: String,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit = {}
) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Url(url))
    val composition = compositionResult.value

    if (compositionResult.isLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = LocalAppTheme.current.accent.copy(alpha = 0.5f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    } else if (composition != null) {
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier
        )
    } else {
        fallback()
    }
}

@Composable
fun MigraineSplashScreen(
    onDismiss: () -> Unit,
    theme: CustomThemeSpecs,
    lang: String
) {
    var animateStart by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateStart = true
        kotlinx.coroutines.delay(2600) // 2.6 seconds of beautiful calm branding
        onDismiss()
    }

    // Infinite breathing cycle animation for calming effect
    val infiniteTransition = rememberInfiniteTransition(label = "SplashBreath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathScale"
    )
    val breathGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathGlow"
    )

    // Entry animations
    val logoScale by animateFloatAsState(
        targetValue = if (animateStart) 1f else 0.75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (animateStart) 1f else 0f,
        animationSpec = tween(700, easing = EaseOutQuad),
        label = "LogoAlpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (animateStart) 1f else 0f,
        animationSpec = tween(800, delayMillis = 300, easing = EaseOutQuad),
        label = "TextAlpha"
    )
    val textYOffset by animateDpAsState(
        targetValue = if (animateStart) 0.dp else 24.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "TextYOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .clickable(
                onClick = onDismiss,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        // Soft glowing ambient background layers
        Box(
            modifier = Modifier
                .size(320.dp)
                .graphicsLayer {
                    scaleX = breathScale * 1.5f
                    scaleY = breathScale * 1.5f
                    alpha = breathGlowAlpha
                }
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(theme.accent, Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Pulsing Calm Logo Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = logoScale * breathScale
                        scaleY = logoScale * breathScale
                        alpha = logoAlpha
                    }
                    .size(130.dp)
                    .background(theme.surfaceContainer, shape = CircleShape)
                    .border(1.5.dp, theme.primary.copy(alpha = 0.25f), shape = CircleShape)
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .border(1.dp, theme.primary.copy(alpha = 0.15f), shape = CircleShape)
                )

                // Beautiful lottie calming core or custom drawn heart fallback
                AppLottiePlayer(
                    url = "https://assets5.lottiefiles.com/packages/lf20_t9gkkhz4.json",
                    modifier = Modifier.size(90.dp),
                    fallback = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = theme.accent,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Animated Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = textAlpha
                        translationY = textYOffset.toPx()
                    }
            ) {
                Text(
                    text = Translate.t("app_title", lang),
                    fontFamily = theme.fontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = Translate.t("splash_tagline", lang),
                    fontFamily = theme.fontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Pulse loading dot / bottom indicator
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = textAlpha * 0.8f }
            ) {
                Text(
                    text = Translate.t("splash_connecting", lang),
                    fontFamily = theme.fontFamily,
                    fontSize = 12.sp,
                    color = theme.primary.copy(alpha = 0.4f),
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun OnboardingTutorialOverlay(
    lang: String,
    theme: CustomThemeSpecs,
    viewModel: MigraineViewModel,
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 5 // 0: Welcome, 1: Create Log Sandbox, 2: Screen Nav, 3: Search Sandbox, 4: Live Theme Select
    val context = LocalContext.current

    // Pulsing animations for pointers
    val infiniteTransition = rememberInfiniteTransition(label = "TutorialGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // State variables for Step 1: Sandbox Form
    var step1Intensity by remember { mutableStateOf(6f) }
    var step1AuraChecked by remember { mutableStateOf(true) }
    var step1SleepChecked by remember { mutableStateOf(false) }
    var step1Notes by remember { mutableStateOf("") }
    var step1Saved by remember { mutableStateOf(false) }

    // State variables for Step 2: Custom Nav Tabs
    var step2ActiveTab by remember { mutableStateOf(0) }

    // State variables for Step 3: Search
    var step3Query by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)) // semi-transparent dim backdrop
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Prevent click through to underlying screen
            }
    ) {
        // Step indicator pointer based on step context
        // During Step 2, if activeTab is specified, let's point to bottom nav zones!
        if (currentStep == 2) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            translationY = (1f - pulseScale) * 15f
                        }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Left navigation area (Dashboard)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                        if (step2ActiveTab == 0) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = pulseScale * 1.3f
                                        scaleY = pulseScale * 1.3f
                                        alpha = 1f - pulseScale
                                    }
                                    .size(54.dp)
                                    .background(theme.accent.copy(alpha = 0.5f), shape = CircleShape)
                            )
                        }
                    }
                    // Middle navigation area (History)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                        if (step2ActiveTab == 1) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = pulseScale * 1.3f
                                        scaleY = pulseScale * 1.3f
                                        alpha = 1f - pulseScale
                                    }
                                    .size(54.dp)
                                    .background(theme.accent.copy(alpha = 0.5f), shape = CircleShape)
                            )
                        }
                    }
                    // Right navigation area (Settings)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                        if (step2ActiveTab == 2) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = pulseScale * 1.3f
                                        scaleY = pulseScale * 1.3f
                                        alpha = 1f - pulseScale
                                    }
                                    .size(54.dp)
                                    .background(theme.accent.copy(alpha = 0.5f), shape = CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // Dialog Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    shadowElevation = 18f
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with step index
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentStep == 0) "" else "Step $currentStep of 4",
                        fontFamily = theme.fontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.primary.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )

                    // Skip Tour Link
                    Text(
                        text = Translate.t("btn_skip", lang),
                        fontFamily = theme.fontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.accent,
                        modifier = Modifier
                            .clickable {
                                triggerHaptic(context, MyHapticStyle.LIGHT)
                                onComplete()
                            }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Animated step content switcher
                androidx.compose.animation.AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 80)) + scaleIn(initialScale = 0.94f, animationSpec = tween(220, delayMillis = 80)))
                            .togetherWith(fadeOut(animationSpec = tween(80)))
                    },
                    label = "InteractiveTutorialStep"
                ) { step ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (step) {
                            0 -> {
                                // STEP 0: WELCOME & LOGO
                                AppLottiePlayer(
                                    url = "https://assets1.lottiefiles.com/packages/lf20_myejio3g.json",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .padding(bottom = 12.dp),
                                    fallback = {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = theme.accent,
                                            modifier = Modifier.size(54.dp)
                                        )
                                    }
                                )
                                Text(
                                    text = Translate.t("tut_welcome", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = Translate.t("tut_welcome_desc", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 13.sp,
                                    color = theme.onSurfaceVariant.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                            1 -> {
                                // STEP 1: INTERACTIVE LOG SENSE LAB
                                Text(
                                    text = Translate.t("tut_step1_title", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Translate.t("tut_step1_desc", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 12.sp,
                                    color = theme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Simulator Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(theme.surface)
                                        .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        // Slider Pain intensity Indicator
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val emojiLabel = when {
                                                step1Intensity < 3.5f -> "😊"
                                                step1Intensity < 6.5f -> "🤕"
                                                step1Intensity < 8.5f -> "😫"
                                                else -> "🔥"
                                            }
                                            Text(
                                                text = "${Translate.t("pain_level", lang)}: ${step1Intensity.toInt()}/10",
                                                fontFamily = theme.fontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = theme.primary
                                            )
                                            Text(text = emojiLabel, fontSize = 16.sp)
                                        }

                                        Slider(
                                            value = step1Intensity,
                                            onValueChange = { step1Intensity = it },
                                            valueRange = 1f..10f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = theme.accent,
                                                activeTrackColor = theme.accent
                                            ),
                                            modifier = Modifier.height(28.dp)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Checkboxes Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Mock Aura
                                            Surface(
                                                onClick = { step1AuraChecked = !step1AuraChecked },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (step1AuraChecked) theme.primary.copy(alpha = 0.1f) else theme.surfaceContainer,
                                                border = BorderStroke(1.dp, if (step1AuraChecked) theme.primary else theme.onSurfaceVariant.copy(alpha = 0.1f)),
                                                modifier = Modifier.weight(1f)
                                                    .height(34.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (step1AuraChecked) theme.primary else Color.Transparent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(Translate.t("tag_aura", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                                                }
                                            }

                                            // Mock Sleep
                                            Surface(
                                                onClick = { step1SleepChecked = !step1SleepChecked },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (step1SleepChecked) theme.primary.copy(alpha = 0.1f) else theme.surfaceContainer,
                                                border = BorderStroke(1.dp, if (step1SleepChecked) theme.primary else theme.onSurfaceVariant.copy(alpha = 0.1f)),
                                                modifier = Modifier.weight(1f)
                                                    .height(34.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (step1SleepChecked) theme.primary else Color.Transparent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(Translate.t("no_sleep", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Small interactive Notes field
                                        OutlinedTextField(
                                            value = step1Notes,
                                            onValueChange = { step1Notes = it },
                                            placeholder = { Text(Translate.t("notes_placeholder", lang), fontSize = 11.sp) },
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = theme.fontFamily),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = theme.accent,
                                                unfocusedBorderColor = theme.onSurfaceVariant.copy(alpha = 0.2f),
                                                focusedContainerColor = theme.surface,
                                                unfocusedContainerColor = theme.surface
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // simulated save action button
                                        androidx.compose.animation.AnimatedVisibility(visible = step1Saved) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Green,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = Translate.t("tut_save_success", lang),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Green
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                triggerHaptic(context, MyHapticStyle.MEDIUM)
                                                step1Saved = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                                            shape = theme.buttonShape,
                                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                        ) {
                                            Text(Translate.t("tut_try_save", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // STEP 2: NAVIGATION MODE TAB SIMULATION
                                Text(
                                    text = Translate.t("tut_step2_title", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Translate.t("tut_step2_desc", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 12.sp,
                                    color = theme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Interactive Navigation Mock Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(theme.surface)
                                        .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        // Menu options
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(theme.surfaceContainerLow)
                                                .padding(3.dp),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            val menuList = listOf(
                                                0 to Translate.t("dashboard_tab", lang),
                                                1 to Translate.t("history_tab", lang),
                                                2 to Translate.t("settings_tab", lang)
                                            )
                                            menuList.forEach { (index, title) ->
                                                val isSelected = step2ActiveTab == index
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(34.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSelected) theme.accent else Color.Transparent)
                                                        .clickable {
                                                            triggerHaptic(context, MyHapticStyle.LIGHT)
                                                            step2ActiveTab = index
                                                        }
                                                ) {
                                                    Text(
                                                        text = title,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else theme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Simulated layout output details
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(theme.surfaceContainer)
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val infoTip = when (step2ActiveTab) {
                                                0 -> Translate.t("tut_info_tip_dashboard", lang)
                                                1 -> Translate.t("tut_info_tip_history", lang)
                                                else -> Translate.t("tut_info_tip_settings", lang)
                                            }
                                            Text(
                                                text = infoTip,
                                                fontSize = 11.sp,
                                                fontFamily = theme.fontFamily,
                                                color = theme.primary,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            3 -> {
                                // STEP 3: SEARCH & FILTER SYSTEM
                                Text(
                                    text = Translate.t("tut_step3_title", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Translate.t("tut_step3_desc", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 12.sp,
                                    color = theme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Search sandbox Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(theme.surface)
                                        .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        // Mock Search Bar
                                        OutlinedTextField(
                                            value = step3Query,
                                            onValueChange = { step3Query = it },
                                            placeholder = { Text(Translate.t("tut_search_placeholder", lang), fontSize = 11.sp) },
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = theme.fontFamily),
                                            singleLine = true,
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = theme.accent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth().height(42.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = theme.accent,
                                                unfocusedBorderColor = theme.onSurfaceVariant.copy(alpha = 0.2f),
                                                focusedContainerColor = theme.surfaceContainerLow,
                                                unfocusedContainerColor = theme.surfaceContainerLow
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Mini List display
                                        val mockEntries = if (lang == "fr") listOf(
                                            "Aura de stress" to "25 Mai",
                                            "Déshydratation" to "18 Mai",
                                            "Manque de sommeil Pulsation" to "09 Mai"
                                        ) else listOf(
                                            "Stress Aura" to "May 25",
                                            "Dehydration" to "May 18",
                                            "Lack of Sleep Throbbing" to "May 09"
                                        )
                                        val filteredMock = mockEntries.filter {
                                            step3Query.isEmpty() || it.first.contains(step3Query, ignoreCase = true)
                                        }

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (filteredMock.isEmpty()) {
                                                Text(
                                                    text = Translate.t("tut_no_results", lang),
                                                    fontSize = 10.sp,
                                                    color = theme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                                )
                                            } else {
                                                filteredMock.forEach { (text, date) ->
                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(theme.surfaceContainerLow)
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(text = text, fontSize = 10.sp, color = theme.primary)
                                                        Text(text = date, fontSize = 9.sp, color = theme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            4 -> {
                                // STEP 4: INSTANT LIVE THEME SELECTION
                                Text(
                                    text = Translate.t("tut_step4_title", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Translate.t("tut_step4_desc", lang),
                                    fontFamily = theme.fontFamily,
                                    fontSize = 12.sp,
                                    color = theme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Theme selection row
                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(theme.surface)
                                        .border(1.dp, theme.onSurfaceVariant.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                        .padding(vertical = 12.dp, horizontal = 6.dp)
                                ) {
                                    val availableThemes = listOf(
                                        "misty_day" to Color(0xFF2B5C8F),
                                        "soothing_night" to Color(0xFF90B5E0),
                                        "forest_breath" to Color(0xFF81C784),
                                        "cosmic_lavender" to Color(0xFFD1C4E9),
                                        "warm_terracotta" to Color(0xFFD84315)
                                    )

                                    availableThemes.forEach { (id, indicatorColor) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable {
                                                triggerHaptic(context, MyHapticStyle.HEAVY)
                                                // Live theme update
                                                viewModel.setAppTheme(id)
                                                context.getSharedPreferences("migraine_settings", android.content.Context.MODE_PRIVATE)
                                                    .edit()
                                                    .putString("theme", id)
                                                    .apply()
                                            }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(indicatorColor)
                                                    .border(2.dp, Color.White, CircleShape)
                                            ) {
                                                val isActive = (id == "misty_day" && theme.displayName == "Misty Day") ||
                                                        (id == "soothing_night" && theme.displayName == "Soothing Night") ||
                                                        (id == "forest_breath" && theme.displayName == "Forest Breath") ||
                                                        (id == "cosmic_lavender" && theme.displayName == "Cosmic Lavender") ||
                                                        (id == "warm_terracotta" && theme.displayName == "Warm Terracotta")

                                                if (isActive) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = Translate.t("theme_label_$id", lang),
                                                fontSize = 9.sp,
                                                fontFamily = theme.fontFamily,
                                                color = theme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dots indicator representing totalSteps
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalSteps) {
                        val active = i == currentStep
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (active) 16.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (active) theme.accent else theme.onSurfaceVariant.copy(alpha = 0.2f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Navigation Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = {
                                triggerHaptic(context, MyHapticStyle.LIGHT)
                                currentStep -= 1
                            },
                            border = BorderStroke(1.dp, theme.onSurfaceVariant.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primary),
                            shape = theme.buttonShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = Translate.t("btn_back", lang),
                                fontFamily = theme.fontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // Next/Finish button
                    Button(
                        onClick = {
                            triggerHaptic(context, MyHapticStyle.MEDIUM)
                            if (currentStep < totalSteps - 1) {
                                currentStep += 1
                            } else {
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        shape = theme.buttonShape,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = if (currentStep == totalSteps - 1) Translate.t("btn_finish", lang) else Translate.t("btn_next", lang),
                            fontFamily = theme.fontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionOverlay(
    theme: CustomThemeSpecs,
    currentLang: String,
    onLanguageChosen: (String) -> Unit
) {
    var selectedLang by remember { mutableStateOf(currentLang) }
    val context = LocalContext.current

    // Infinite pulsing visual ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "LangPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)) // dark rich dimmed backdrop
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {} // click blocker
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    shadowElevation = 20f
                },
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Calm glowing indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(70.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .background(theme.accent.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info, // Globe/Info placeholder
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title - Dynamic bilingual header or based on selection
                Text(
                    text = Translate.t("lang_select_title", selectedLang),
                    fontFamily = theme.fontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = Translate.t("lang_select_desc", selectedLang),
                    fontFamily = theme.fontFamily,
                    fontSize = 12.sp,
                    color = theme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Options
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // English Button Option
                    val isEng = selectedLang == "en"
                    Surface(
                        onClick = {
                            triggerHaptic(context, MyHapticStyle.LIGHT)
                            selectedLang = "en"
                        },
                        shape = theme.buttonShape,
                        color = if (isEng) theme.accent.copy(alpha = 0.12f) else theme.surface,
                        border = BorderStroke(
                            width = if (isEng) 2.dp else 1.dp,
                            color = if (isEng) theme.accent else theme.onSurfaceVariant.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "🇺🇸   " + Translate.t("lang_english", "en"),
                                fontFamily = theme.fontFamily,
                                fontSize = 14.sp,
                                fontWeight = if (isEng) FontWeight.Bold else FontWeight.Medium,
                                color = if (isEng) theme.primary else theme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (isEng) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = Translate.t("desc_selected", selectedLang),
                                    tint = theme.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // French Button Option
                    val isFr = selectedLang == "fr"
                    Surface(
                        onClick = {
                            triggerHaptic(context, MyHapticStyle.LIGHT)
                            selectedLang = "fr"
                        },
                        shape = theme.buttonShape,
                        color = if (isFr) theme.accent.copy(alpha = 0.12f) else theme.surface,
                        border = BorderStroke(
                            width = if (isFr) 2.dp else 1.dp,
                            color = if (isFr) theme.accent else theme.onSurfaceVariant.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "🇫🇷   " + Translate.t("lang_french", "fr"),
                                fontFamily = theme.fontFamily,
                                fontSize = 14.sp,
                                fontWeight = if (isFr) FontWeight.Bold else FontWeight.Medium,
                                color = if (isFr) theme.primary else theme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (isFr) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = Translate.t("desc_selected", selectedLang),
                                    tint = theme.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Confirm Action Button
                Button(
                    onClick = {
                        triggerHaptic(context, MyHapticStyle.HEAVY)
                        onLanguageChosen(selectedLang)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                    shape = theme.buttonShape ?: RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = Translate.t("lang_confirm", selectedLang),
                        fontFamily = theme.fontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

