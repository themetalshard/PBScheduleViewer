package com.metalshard.hyperiondev

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.metalshard.hyperion.R

@Composable
fun OnboardingScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    useDynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit,
    isDayMonthFormat: Boolean,
    onDateFormatChange: (Boolean) -> Unit,
    isHostMode: Boolean,
    onHostModeChange: (Boolean) -> Unit,
    onFinish: () -> Unit
) {
    var step by remember { mutableStateOf(1) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Text(
                    text = "Step $step of 3",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            when (step) {
                1 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        val context = LocalContext.current
                        val drawable = remember { ContextCompat.getDrawable(context, R.mipmap.ic_launcher) }
                        val bitmap = remember(drawable) {
                            if (drawable != null) {
                                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 256
                                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 256
                                val bmp = createBitmap(width, height)
                                val canvas = Canvas(bmp)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                                bmp
                            } else null
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "App Icon",
                                modifier = Modifier.size(96.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(96.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Welcome to PB Schedule Viewer!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "This app allows you to view schedules for the Roblox group Pinewood Builders.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Appearance Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, null)
                            Spacer(Modifier.width(16.dp))
                            Text("Dark Mode", Modifier.weight(1f))
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { onDarkModeChange(it) }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, null)
                            Spacer(Modifier.width(16.dp))
                            Text("Dynamic Colors (Material You)", Modifier.weight(1f))
                            Switch(
                                checked = useDynamicColors,
                                onCheckedChange = { onDynamicColorsChange(it) }
                            )
                        }
                    }
                }
                3 -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Preferences",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, null)
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = if (isDayMonthFormat) "Date Format: DD/MM" else "Date Format: MM/DD",
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = isDayMonthFormat,
                                onCheckedChange = { onDateFormatChange(it) }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null)
                            Spacer(Modifier.width(16.dp))
                            Text("Host Mode", Modifier.weight(1f))
                            Switch(
                                checked = isHostMode,
                                onCheckedChange = { onHostModeChange(it) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    OutlinedButton(onClick = { step-- }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (step < 3) {
                    Button(onClick = { step++ }) {
                        Text("Next")
                    }
                } else {
                    Button(onClick = { onFinish() }) {
                        Text("Finish")
                    }
                }
            }
        }
    }
}