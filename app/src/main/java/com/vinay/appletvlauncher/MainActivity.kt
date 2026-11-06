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
            var currentScreen by remember { mutableStateOf("home") } // "home", "settings", "control"
            var dockCount by remember { mutableStateOf(prefs.getInt("dock_count", 6)) }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // CUSTOM PERSONAL WALLPAPER BACKGROUND (Movie background ki jagah clean custom background)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF2C3E50), Color(0xFF000000))
                            )
                        )
                )

                // Dark Vignette Overlay
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

// Apple tvOS Glassmorphism Modifier
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
        // Top Bar (Weather, Clock & Profile/Settings Icon)
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

        // Top Shelf Widgets (Analog Clock & Custom Wallpaper Preview Widget)
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
                
