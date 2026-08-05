package com.example.finalsranktracker

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

@Composable
internal fun SplashUI(
    palette: Palette,
    isEnglish: Boolean,
    currentRank: Int?,
    animatedRankBrush: Brush,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(Color(0xFF9FB6CD), Color(0xFF606979)), radius = 1200f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        var finalsAlpha by remember { mutableStateOf(0f) }
        var finalsScale by remember { mutableStateOf(0.8f) }
        var logoScale by remember { mutableStateOf(0f) }
        var logoOffsetY by remember { mutableStateOf((-100).dp) }
        var progressTarget by remember { mutableStateOf(0f) }

        val context = LocalContext.current
        val selectedMascot = remember {
            val possibleNames = listOf(
                "namatama_splash",
                "namatama_38",
                "namatama_36",
                "namatama_35",
                "namatama_39",
                "namatama_41",
                "namatama_28",
                "namatama_26",
                "namatama_22",
                "namatama_18",
                "namatama_12",
                "namatama_11",
                "namatama_10",
                "namatama_9",
                "namatama_7",
                "namatama_3",
                "namatama_29",
                "namatama_19",
                "namatama_14",
                "namatama_24"
            )
            
            val prefs = context.getSharedPreferences("mascot_prefs", android.content.Context.MODE_PRIVATE)
            val historyString = prefs.getString("history", "") ?: ""
            val history = historyString.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }

            val existingDrawables = possibleNames.map { name ->
                context.resources.getIdentifier(name, "drawable", context.packageName)
            }.filter { it != 0 }

            if (existingDrawables.isNotEmpty()) {
                var available = existingDrawables.filter { !history.contains(it) }
                if (available.isEmpty()) {
                    available = existingDrawables
                }
                
                val chosen = available.random()
                
                val newHistory = (history + chosen).takeLast(5)
                prefs.edit().putString("history", newHistory.joinToString(",")).apply()
                
                chosen
            } else {
                R.drawable.namatama_splash
            }
        }

        val animFinalsAlpha by animateFloatAsState(targetValue = finalsAlpha, animationSpec = tween(400), label = "alpha")
        val animFinalsScale by animateFloatAsState(targetValue = finalsScale, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "scale")

        val animLogoScale by animateFloatAsState(
            targetValue = logoScale,
            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "lscale"
        )
        val animLogoOffsetY by animateDpAsState(
            targetValue = logoOffsetY,
            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "ly"
        )

        val progressAnim by animateFloatAsState(
            targetValue = progressTarget,
            animationSpec = tween(durationMillis = 4000, easing = LinearEasing),
            label = "Progress"
        )

        LaunchedEffect(Unit) {
            finalsAlpha = 1f
            finalsScale = 1f
            progressTarget = 1f

            delay(300)
            logoScale = 1.0f
            logoOffsetY = 0.dp
        }

        LaunchedEffect(Unit) {
            delay(2200)
            onDismiss()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "THE FINALS",
                color = Color.White,
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

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .scale(animLogoScale)
                    .offset(y = animLogoOffsetY)
            ) {
                Image(
                    painter = painterResource(id = selectedMascot),
                    contentDescription = "Splash Logo",
                    modifier = Modifier.size(380.dp)
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .background(palette.surfaceAlt, RoundedCornerShape(1.5.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressAnim.coerceIn(0f, 1f))
                        .background(palette.accent, RoundedCornerShape(1.5.dp))
                )
            }
        }
    }
}