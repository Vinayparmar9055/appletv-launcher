package com.vinay.appletvlauncher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
        setContent {
            MainAppContent()
        }
    }
}

@Composable
fun MainAppContent() {
    var screen by remember { mutableStateOf("home") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
    ) {
        when (screen) {
            "home" -> HomeScreen(
                onSettings = { screen = "settings" },
                onControl = { screen = "control" }
            )
            "settings" -> SettingsScreen(onBack = { screen = "home" })
            "control" -> ControlScreen(onBack = { screen = "home" })
        }
    }
}

fun Modifier.boxStyle(focused: Boolean): Modifier = this
    .clip(RoundedCornerShape(20.dp))
    .background(if (focused) Color.White.copy(0.3f) else Color.White.copy(0.12f))
    .border(
        width = if (focused) 2.dp else 1.dp,
        brush = Brush.verticalGradient(listOf(Color.White.copy(0.6f), Color.White.copy(0.1f))),
        shape = RoundedCornerShape(20.dp)
    )

@Composable
fun HomeScreen(onSettings: () -> Unit, onControl: () -> Unit) {
    val context = LocalContext.current
    val apps = remember { loadUserApps(context) }
    var timeText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            delay(10000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Good morning, Vinay", color = Color.White.copy(0.7f), fontSize = 16.sp)
                Text(timeText, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MenuButton("⚙️ Settings") { onSettings() }
                MenuButton("🦊 Control") { onControl() }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .boxStyle(false)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            apps.take(5).forEach { app ->
                AppTile(app) { launchUserApp(context, app.packageName) }
            }
        }
    }
}

@Composable
fun MenuButton(title: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .boxStyle(focused)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppTile(app: AppItem, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (focused) 1.15f else 1f, tween(150))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(sc)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
    ) {
        Box(
            modifier = Modifier
                .size(width = 90.dp, height = 55.dp)
                .boxStyle(focused),
            contentAlignment = Alignment.Center
        ) {
            val bmp = remember(app.packageName) { app.icon.toBitmap(64, 64).asImageBitmap() }
            Image(bmp, contentDescription = app.name, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (focused) app.name else "",
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    Box(modifier = Modifier.fillMaxSize().padding(50.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Text("Settings Menu", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            MenuButton("Back to Home") { onBack() }
        }
    }
}

@Composable
fun ControlScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    Box(modifier = Modifier.fillMaxSize().padding(50.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Text("Control Center", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            MenuButton("Open WiFi Settings") {
                LocalContext.current.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            Spacer(modifier = Modifier.height(10.dp))
            MenuButton("Back to Home") { onBack() }
        }
    }
}

fun loadUserApps(context: Context): List<AppItem> {
    val pm = context.packageManager
    val list = mutableListOf<AppItem>()
    val seen = mutableSetOf<String>()
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    for (info in pm.queryIntentActivities(intent, 0)) {
        val pkg = info.activityInfo.packageName
        if (pkg != context.packageName && seen.add(pkg)) {
            list.add(AppItem(info.loadLabel(pm).toString(), pkg, info.loadIcon(pm)))
        }
    }
    return list.sortedBy { it.name.lowercase() }
}

fun launchUserApp(context: Context, pkg: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
    }
}
