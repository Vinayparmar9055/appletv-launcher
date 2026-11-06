package com.vinay.appletvlauncher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class AppItem(val name: String, val packageName: String, val icon: Drawable)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("tvos_prefs", Context.MODE_PRIVATE)
        setContent {
            var currentScreen by remember { mutableStateOf("home") }
            var dockCount by remember { mutableStateOf(prefs.getInt("dock_count", 6)) }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF2C3E50), Color(0xFF000000))
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )

                when (currentScreen) {
                    "home" -> TvOSHomeScreen(
                        onOpenSettings = { currentScreen = "settings" },
                        onOpenControlCenter = { currentScreen = "control" }
                    )
                    "settings" -> TvOSSettingsMenu(
                        onBack = { currentScreen = "home" }
                    )
                    "control" -> TvOSControlCenterOverlay(
                        onBack = { currentScreen = "home" }
                    )
                }
            }
        }
    }
}

fun Modifier.tvosGlass(
    cornerRadius: Int = 24,
    isFocused: Boolean = false
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        Brush.verticalGradient(
            colors = if (isFocused) {
                listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.65f))
            } else {
                listOf(Color.White.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.5f))
            }
        )
    )
    .border(
        width = if (isFocused) 2.dp else 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isFocused) 1f else 0.4f),
                Color.White.copy(alpha = 0.05f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )

@Composable
fun TvOSHomeScreen(
    onOpenSettings: () -> Unit,
    onOpenControlCenter: () -> Unit
) {
    val context = LocalContext.current
    val apps = remember { loadInstalledApps(context) }
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            delay(10000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Good morning, Vinay", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                Text(currentTime, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Text("Thursday, 5 November", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassIconButton(icon = "⚙️") { onOpenSettings() }
                GlassIconButton(icon = "🦊") { onOpenControlCenter() }
            }
        }

        Spacer(Modifier.height(30.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 160.dp)
                    .tvosGlass(24),
                contentAlignment = Alignment.Center
            ) {
                Text("⏰ Analog Clock", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }

            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 160.dp)
                    .tvosGlass(24),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🖼️", fontSize = 28.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Wallpaper audio", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Custom photo active", color = Color.White.copy(0.6f), fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvosGlass(28)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            apps.take(5).forEach { app ->
                AppTile(app) { launchApp(context, app.packageName) }
            }
        }
    }
}

@Composable
fun GlassIconButton(icon: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(48.dp)
            .tvosGlass(24, isFocused = focused)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 20.sp)
    }
}

@Composable
fun TvOSSettingsMenu(onBack: () -> Unit) {
    BackHandler { onBack() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier.width(600.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsRow("Appearance", "Liquid Glass & UI style") { }
            SettingsRow("Top Shelf", "Configure widgets and banners") { }
            SettingsRow("Profile", "Bedroom TV 🦊") { }
            SettingsRow("Button Remap", "Configure Home button") { }
            SettingsRow("Default Launcher", "Off") { }
            SettingsRow("Back to Home", "Return to main screen") { onBack() }
        }
    }
}

@Composable
fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvosGlass(16, isFocused = focused)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun TvOSControlCenterOverlay(onBack: () -> Unit) {
    BackHandler { onBack() }
    Box(
        modifier = Modifier.fillMaxSize().clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .tvosGlass(32)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Control Center", color = Color.White.copy(0.7f), fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ControlTile("🕒 StandBy", Modifier.weight(1f)) {}
                ControlTile("📶 Wi-Fi\nChetan-5G", Modifier.weight(1f)) {
                    LocalContext.current.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ControlTile("🔹 Bluetooth\nOn", Modifier.weight(1f)) {}
                ControlTile("⚙️ Settings", Modifier.weight(1f)) {}
            }
        }
    }
}

@Composable
fun ControlTile(label: String, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .height(80.dp)
            .tvosGlass(20, isFocused = focused)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AppTile(app: AppItem, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.15f else 1f, tween(160))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
    ) {
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 60.dp)
                .tvosGlass(16, isFocused = focused)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = remember(app.packageName) { app.icon.toBitmap(80, 80).asImageBitmap() }
            Image(bmp, contentDescription = app.name, contentScale = ContentScale.Fit, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (focused) app.name else "",
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun loadInstalledApps(context: Context): List<AppItem> {
    val pm = context.packageManager
    val list = mutableListOf<AppItem>()
    val seen = mutableSetOf<String>()
    val intents = listOf(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    )
    for (intent in intents) {
        for (info in pm.queryIntentActivities(intent, 0)) {
            val pkg = info.activityInfo.packageName
            if (pkg != context.packageName && seen.add(pkg)) {
                list.add(AppItem(info.loadLabel(pm).toString(), pkg, info.loadIcon(pm)))
            }
        }
    }
    return list.sortedBy { it.name.lowercase() }
}

fun launchApp(context: Context, pkg: String) {
    val pm = context.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
    }
}
