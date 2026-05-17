package com.metalshard.hyperion

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.metalshard.hyperion.model.ScheduleEvent
import com.metalshard.hyperion.ui.ScheduleViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

            var isFirstTime by remember { mutableStateOf(prefs.getBoolean("is_first_time", true)) }
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", true)) }
            var useDynamicColors by remember { mutableStateOf(prefs.getBoolean("use_dynamic_colors", true)) }
            var isDayMonthFormat by remember { mutableStateOf(prefs.getBoolean("is_day_month_format", true)) }
            var isHostMode by remember { mutableStateOf(prefs.getBoolean("is_host_mode", true)) }
            var showLiveIndicator by remember { mutableStateOf(prefs.getBoolean("show_live_indicator", true)) }

            LaunchedEffect(isDarkMode, useDynamicColors, isDayMonthFormat, isHostMode, showLiveIndicator) {
                prefs.edit()
                    .putBoolean("is_dark_mode", isDarkMode)
                    .putBoolean("use_dynamic_colors", useDynamicColors)
                    .putBoolean("is_day_month_format", isDayMonthFormat)
                    .putBoolean("is_host_mode", isHostMode)
                    .putBoolean("show_live_indicator", showLiveIndicator)
                    .apply()
            }

            HyperionTheme(darkTheme = isDarkMode, dynamicColor = useDynamicColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isFirstTime) {
                        OnboardingScreen(
                            isDarkMode = isDarkMode,
                            onDarkModeChange = { isDarkMode = it },
                            useDynamicColors = useDynamicColors,
                            onDynamicColorsChange = { useDynamicColors = it },
                            showLiveIndicator = showLiveIndicator,
                            onLiveIndicatorChange = { showLiveIndicator = it },
                            isDayMonthFormat = isDayMonthFormat,
                            onDateFormatChange = { isDayMonthFormat = it },
                            isHostMode = isHostMode,
                            onHostModeChange = { isHostMode = it },
                            onFinish = {
                                prefs.edit().putBoolean("is_first_time", false).apply()
                                isFirstTime = false
                            }
                        )
                    } else {
                        ScheduleScreen(
                            isDarkMode = isDarkMode,
                            onDarkModeChange = { isDarkMode = it },
                            useDynamicColors = useDynamicColors,
                            onDynamicColorsChange = { useDynamicColors = it },
                            isDayMonthFormat = isDayMonthFormat,
                            onDateFormatChange = { isDayMonthFormat = it },
                            isHostMode = isHostMode,
                            onHostModeChange = { isHostMode = it },
                            showLiveIndicator = showLiveIndicator,
                            onLiveIndicatorChange = { showLiveIndicator = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HyperionTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(primary = Color(0xFFD0BCFF))
        else -> lightColorScheme(primary = Color(0xFF6750A4))
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    vm: ScheduleViewModel = viewModel(),
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    useDynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit,
    isDayMonthFormat: Boolean,
    onDateFormatChange: (Boolean) -> Unit,
    isHostMode: Boolean,
    onHostModeChange: (Boolean) -> Unit,
    showLiveIndicator: Boolean,
    onLiveIndicatorChange: (Boolean) -> Unit
) {
    val schedule by vm.schedule.collectAsState()
    val isCalendarView by vm.isCalendarView
    val activeGroup = vm.activeGroup.value
    val selectedEvent = vm.selectedEvent.value
    var showSettings by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(vm.isLoading.value) {
        if (!vm.isLoading.value) {
            isRefreshing = false
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navItems = listOf(
                    Triple("PBST", "Shield", Icons.Filled.Shield),
                    Triple("PET", "MedicalServices", Icons.Filled.MedicalServices),
                    Triple("TMS", "Fire", Icons.Filled.LocalFireDepartment),
                    Triple("PBM", "Camera", Icons.Filled.PhotoCamera)
                )
                navItems.forEach { (id, label, icon) ->
                    NavigationBarItem(
                        selected = activeGroup == id,
                        onClick = { vm.activeGroup.value = id },
                        label = { Text(id) },
                        icon = { Icon(icon, contentDescription = label) }
                    )
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                SmallFloatingActionButton(
                    onClick = { showSettings = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                FloatingActionButton(onClick = { vm.isCalendarView.value = !isCalendarView }) {
                    Icon(if (isCalendarView) Icons.AutoMirrored.Filled.List else Icons.Default.CalendarViewWeek, null)
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                vm.refresh()
            },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (vm.isLoading.value && !isRefreshing) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    val currentGroupData = schedule[activeGroup] ?: schedule[activeGroup.lowercase()] ?: emptyList()

                    if (currentGroupData.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            StateMessageScreen(message = "No schedules for this subgroup")
                        }
                    } else {
                        if (isCalendarView) {
                            MultiColumnContent(vm, currentGroupData, isDayMonthFormat, showLiveIndicator)
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(currentGroupData.sortedBy { it.time }) { event ->
                                    EventCardItem(event, isDayMonthFormat, showLiveIndicator) { vm.selectedEvent.value = event }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = sheetState
            ) {
                SettingsContent(
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    useDynamicColors = useDynamicColors,
                    onDynamicColorsChange = onDynamicColorsChange,
                    isDayMonthFormat = isDayMonthFormat,
                    onDateFormatChange = onDateFormatChange,
                    isHostMode = isHostMode,
                    onHostModeChange = onHostModeChange,
                    showLiveIndicator = showLiveIndicator,
                    onLiveIndicatorChange = onLiveIndicatorChange
                )
            }
        }
    }

    selectedEvent?.let { event ->
        EventDetailPopup(
            event = event,
            activeGroup = activeGroup,
            isHostMode = isHostMode,
            onDismiss = { vm.selectedEvent.value = null }
        )
    }
}

@Composable
fun StateMessageScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SentimentDissatisfied,
            contentDescription = "Empty state icon",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SettingsContent(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    useDynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit,
    isDayMonthFormat: Boolean,
    onDateFormatChange: (Boolean) -> Unit,
    isHostMode: Boolean,
    onHostModeChange: (Boolean) -> Unit,
    showLiveIndicator: Boolean,
    onLiveIndicatorChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DarkMode, null)
            Spacer(Modifier.width(16.dp))
            Text("Dark Mode", Modifier.weight(1f))
            Switch(checked = isDarkMode, onCheckedChange = onDarkModeChange)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Palette, null)
            Spacer(Modifier.width(16.dp))
            Text("Dynamic Colors (Material You)", Modifier.weight(1f))
            Switch(checked = useDynamicColors, onCheckedChange = onDynamicColorsChange)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, null)
            Spacer(Modifier.width(16.dp))
            Text("Highlight Live Events", Modifier.weight(1f))
            Switch(checked = showLiveIndicator, onCheckedChange = onLiveIndicatorChange)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, null)
            Spacer(Modifier.width(16.dp))
            Text(
                text = if (isDayMonthFormat) "Date Format: DD/MM" else "Date Format: MM/DD",
                modifier = Modifier.weight(1f)
            )
            Switch(checked = isDayMonthFormat, onCheckedChange = onDateFormatChange)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null)
            Spacer(Modifier.width(16.dp))
            Text("Host Mode", Modifier.weight(1f))
            Switch(checked = isHostMode, onCheckedChange = onHostModeChange)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("Credits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val credits = "TheMetalShard (Dev)\nAllTheTimeGamingSCP (PB Website creator)\nBliss_god28 (Logo designer)\nLunarThePr0t0g3n (Tester)\nTheSkout001 (For the event card time idea)\nKyguy329 (Mac tester)"
        Text(
            text = credits,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MultiColumnContent(vm: ScheduleViewModel, events: List<ScheduleEvent>, isDayMonthFormat: Boolean, showLiveIndicator: Boolean) {
    val pattern = if (isDayMonthFormat) "EEE dd/MM" else "EEE MM/dd"
    val dayFormatter = DateTimeFormatter.ofPattern(pattern)
    val scrollState = rememberScrollState()
    val eventsByDate = events.groupBy {
        Instant.ofEpochSecond(it.time).atZone(ZoneId.systemDefault()).toLocalDate()
    }.toSortedMap()

    Row(modifier = Modifier
        .fillMaxSize()
        .horizontalScroll(scrollState)
        .padding(16.dp)) {
        eventsByDate.forEach { (date, dayEvents) ->
            Column(modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .padding(end = 16.dp)) {
                Text(
                    text = date.format(dayFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dayEvents.sortedBy { it.time }) { event ->
                        CompactCard(event, showLiveIndicator) { vm.selectedEvent.value = event }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactCard(event: ScheduleEvent, showLiveIndicator: Boolean, onClick: () -> Unit) {
    val color = event.eventColor?.let { Color(it[0], it[1], it[2]) } ?: Color.Gray

    val startTime = Instant.ofEpochSecond(event.time)
    val endTime = Instant.ofEpochSecond(event.time + (event.duration * 60))
    val now = Instant.now()
    val isRunning = showLiveIndicator && now.isAfter(startTime) && now.isBefore(endTime)
    val isDark = isSystemInDarkTheme()

    val runningBgColor = if (isDark) Color(0xFF423D00) else Color(0xFFB8860B)
    val runningContentColor = if (isDark) Color(0xFFFFF9C4) else Color.White

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val timeRangeText = "${timeFormatter.format(startTime)} - ${timeFormatter.format(endTime)}"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isRunning) runningBgColor else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (isRunning) runningContentColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isRunning) 2.dp else 1.dp,
                color = if (isRunning) Color(0xFFFFD700) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(color, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "● LIVE: $timeRangeText" else timeRangeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isRunning) FontWeight.Bold else FontWeight.Normal
                )
            }
            Text(event.eventType, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Host: ${event.trainer}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun EventCardItem(event: ScheduleEvent, isDayMonthFormat: Boolean, showLiveIndicator: Boolean, onClick: () -> Unit) {
    val color = event.eventColor?.let { Color(it[0], it[1], it[2]) } ?: Color.Gray

    val startTime = Instant.ofEpochSecond(event.time)
    val endTime = Instant.ofEpochSecond(event.time + (event.duration * 60))
    val now = Instant.now()
    val isRunning = showLiveIndicator && now.isAfter(startTime) && now.isBefore(endTime)
    val isDark = isSystemInDarkTheme()

    val runningBgColor = if (isDark) Color(0xFF332E00) else Color(0xFFB8860B)
    val runningContentColor = if (isDark) Color(0xFFFFD700) else Color.White

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val datePattern = if (isDayMonthFormat) "dd/MM" else "MM/dd"
    val dateFormatter = DateTimeFormatter.ofPattern(datePattern).withZone(ZoneId.systemDefault())
    val timeText = "${timeFormatter.format(startTime)} - ${timeFormatter.format(endTime)} ${dateFormatter.format(startTime)}"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (isRunning) runningBgColor else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isRunning) runningContentColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isRunning) Modifier.border(2.dp, Color(0xFFFFD700), RoundedCornerShape(24.dp))
                else Modifier
            )
    ) {
        Row(modifier = Modifier
            .padding(16.dp)
            .height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp),
                color = if (isRunning) Color.White else color,
                shape = RoundedCornerShape(2.dp)
            ) {}
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isRunning) "● LIVE:" else timeText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isRunning) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (isRunning) runningContentColor else MaterialTheme.colorScheme.primary
                )
                if (isRunning) {
                    Text(timeText, style = MaterialTheme.typography.labelSmall)
                }
                Text(event.eventType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Host: ${event.trainer}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun EventDetailPopup(
    event: ScheduleEvent,
    activeGroup: String,
    isHostMode: Boolean,
    onDismiss: () -> Unit
) {
    GroupData(
        event = event,
        activeGroup = activeGroup,
        isHostMode = isHostMode,
        onDismiss = onDismiss
    )
}

@Composable
fun GroupData(
    event: ScheduleEvent,
    activeGroup: String,
    isHostMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val instant = Instant.ofEpochSecond(event.time)

    val localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault())
    val utcFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"))

    val localString = localFormatter.format(instant)
    val utcString = utcFormatter.format(instant)
    val unixString = event.time.toString()

    val cleanNotes = event.notes?.replace(Regex("<:[a-zA-Z0-9_]+:[0-9]+>"), "")
        ?.replace("**", "") ?: "No notes provided."

    var showNotesDialog by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<ScheduleAction?>(null) }
    var generatedCommand by remember { mutableStateOf("") }
    var showCommandDialog by remember { mutableStateOf(false) }

    val actions = EventScheduler.getSupportedActions(event.eventType, activeGroup)

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Schedule Info", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(event.eventType, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                DetailItem("Local start", localString, onClick = { copyToClipboard(localString) })
                DetailItem("UTC start", utcString, onClick = { copyToClipboard(utcString) })
                DetailItem("Unix timestamp", unixString, onClick = { copyToClipboard(unixString) })
                DetailItem("Duration", "${event.duration} minutes")

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                DetailItem("Host", event.trainer ?: "N/A")
                Text(text = "Notes: $cleanNotes", style = MaterialTheme.typography.bodyMedium)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                DetailItem("UUID", event.uuid ?: "N/A", isSmall = true, onClick = { copyToClipboard(event.uuid ?: "") })
                DetailItem("Trainer ID", event.trainerId?.toString() ?: "N/A", isSmall = true)
                DetailItem("Discord ID", event.discordId ?: "N/A", isSmall = true)

                if (actions.isNotEmpty() && isHostMode) {
                    actions.forEach { action ->
                        Button(
                            onClick = {
                                selectedAction = action
                                showNotesDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(action.label)
                        }
                    }
                }
            }
        }
    )

    if (showNotesDialog) {
        EnterNotesDialog(
            onDismiss = { showNotesDialog = false },
            onConfirm = { notes ->
                selectedAction?.let { action ->
                    generatedCommand = EventScheduler.generateCommand(
                        event = event,
                        activeGroup = activeGroup,
                        eventType = event.eventType,
                        actionType = action.actionType,
                        notes = notes
                    )
                    showCommandDialog = true
                }
            }
        )
    }

    if (showCommandDialog) {
        CommandDialog(
            command = generatedCommand,
            onDismiss = { showCommandDialog = false },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Command", generatedCommand)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun EnterNotesDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(notes); onDismiss() }) { Text("Generate") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Enter Notes") },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                singleLine = true
            )
        }
    )
}

@Composable
fun CommandDialog(command: String, onDismiss: () -> Unit, onCopy: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onCopy(); onDismiss() }) { Text("Copy") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Command string") },
        text = {
            Column {
                Text("Here is your formatted schedule command:")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    isSmall: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = (if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}