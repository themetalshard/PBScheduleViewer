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
            var isDarkMode by remember { mutableStateOf(true) }
            var useDynamicColors by remember { mutableStateOf(true) }
            var isDayMonthFormat by remember { mutableStateOf(true) }

            HyperionTheme(darkTheme = isDarkMode, dynamicColor = useDynamicColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScheduleScreen(
                        isDarkMode = isDarkMode,
                        onDarkModeChange = { isDarkMode = it },
                        useDynamicColors = useDynamicColors,
                        onDynamicColorsChange = { useDynamicColors = it },
                        isDayMonthFormat = isDayMonthFormat,
                        onDateFormatChange = { isDayMonthFormat = it }
                    )
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
    onDateFormatChange: (Boolean) -> Unit
) {
    val schedule by vm.schedule.collectAsState()
    val isCalendarView by vm.isCalendarView
    val activeGroup = vm.activeGroup.value
    val selectedEvent = vm.selectedEvent.value
    var showSettings by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navItems = listOf(
                    Triple("PBST", "Shield", Icons.Filled.Shield),
                    Triple("PET", "Fire", Icons.Filled.MedicalServices),
                    Triple("TMS", "Explosion", Icons.Filled.LocalFireDepartment),
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
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (vm.isLoading.value) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                val currentGroupData = schedule[activeGroup] ?: schedule[activeGroup.lowercase()] ?: emptyList()
                if (isCalendarView) {
                    MultiColumnContent(vm, currentGroupData, isDayMonthFormat)
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(currentGroupData.sortedBy { it.time }) { event ->
                            EventCardItem(event, isDayMonthFormat) { vm.selectedEvent.value = event }
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
                    onDateFormatChange = onDateFormatChange
                )
            }
        }
    }

    selectedEvent?.let { event ->
        EventDetailPopup(event, onDismiss = { vm.selectedEvent.value = null })
    }
}

@Composable
fun SettingsContent(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    useDynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit,
    isDayMonthFormat: Boolean,
    onDateFormatChange: (Boolean) -> Unit
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
            Icon(Icons.Default.DateRange, null)
            Spacer(Modifier.width(16.dp))
            Text(
                text = if (isDayMonthFormat) "Date Format: DD/MM" else "Date Format: MM/DD",
                modifier = Modifier.weight(1f)
            )
            Switch(checked = isDayMonthFormat, onCheckedChange = onDateFormatChange)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("Credits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val credits = "TheMetalShard (Dev)\nLunarThePr0t0g3n (Tester)\nKyguy329 (Mac port)\nTheSkout001 (For discovering how to get event schedules)"
        Text(
            text = credits,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MultiColumnContent(vm: ScheduleViewModel, events: List<ScheduleEvent>, isDayMonthFormat: Boolean) {
    val pattern = if (isDayMonthFormat) "EEE dd/MM" else "EEE MM/dd"
    val dayFormatter = DateTimeFormatter.ofPattern(pattern)
    val scrollState = rememberScrollState()
    val eventsByDate = events.groupBy {
        Instant.ofEpochSecond(it.time).atZone(ZoneId.systemDefault()).toLocalDate()
    }.toSortedMap()

    Row(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState).padding(16.dp)) {
        eventsByDate.forEach { (date, dayEvents) ->
            Column(modifier = Modifier.width(280.dp).fillMaxHeight().padding(end = 16.dp)) {
                Text(
                    text = date.format(dayFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dayEvents.sortedBy { it.time }) { event ->
                        CompactCard(event) { vm.selectedEvent.value = event }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactCard(event: ScheduleEvent, onClick: () -> Unit) {
    val color = event.eventColor?.let { Color(it[0], it[1], it[2]) } ?: Color.Gray
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text(timeFormatter.format(Instant.ofEpochSecond(event.time)), style = MaterialTheme.typography.labelSmall)
            }
            Text(event.eventType, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Host: ${event.trainer}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun EventCardItem(event: ScheduleEvent, isDayMonthFormat: Boolean, onClick: () -> Unit) {
    val color = event.eventColor?.let { Color(it[0], it[1], it[2]) } ?: Color.Gray

    val datePattern = if (isDayMonthFormat) "dd/MM" else "MM/dd"
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm $datePattern").withZone(ZoneId.systemDefault())

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp).height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.fillMaxHeight().width(4.dp), color = color, shape = RoundedCornerShape(2.dp)) {}
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = timeFormatter.format(Instant.ofEpochSecond(event.time)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(event.eventType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Host: ${event.trainer}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun EventDetailPopup(event: ScheduleEvent, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val instant = Instant.ofEpochSecond(event.time)

    val localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault())
    val utcFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"))

    val localString = localFormatter.format(instant)
    val utcString = utcFormatter.format(instant)
    val unixString = event.time.toString()

    val cleanNotes = event.notes?.replace(Regex("<:[a-zA-Z0-9_]+:[0-9]+>"), "")
        ?.replace("**", "") ?: "No notes provided."

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
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(
            text = "$label: $value",
            style = if (isSmall) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
            color = if (isSmall) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}