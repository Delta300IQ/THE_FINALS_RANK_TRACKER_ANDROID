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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .background(palette.bg)
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
        var counterAlpha by remember { mutableStateOf(0f) }
        var counterOffsetY by remember { mutableStateOf(40.dp) }
        var logoScale by remember { mutableStateOf(0f) }
        var nameAlpha by remember { mutableStateOf(0f) }
        var logoOffsetY by remember { mutableStateOf((-100).dp) }
        var progressTarget by remember { mutableStateOf(0f) }

        val animFinalsAlpha by animateFloatAsState(targetValue = finalsAlpha, animationSpec = tween(400), label = "alpha")
        val animFinalsScale by animateFloatAsState(targetValue = finalsScale, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "scale")
        val animCounterAlpha by animateFloatAsState(targetValue = counterAlpha, animationSpec = tween(400), label = "calpha")
        val animCounterOffsetY by animateDpAsState(targetValue = counterOffsetY, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "cy")

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
        val animNameAlpha by animateFloatAsState(targetValue = nameAlpha, animationSpec = tween(400), label = "nalpha")

        val progressAnim by animateFloatAsState(
            targetValue = progressTarget,
            animationSpec = tween(durationMillis = 4000, easing = LinearEasing),
            label = "Progress"
        )

        var isRolling by remember { mutableStateOf(true) }
        var rollValue by remember { mutableStateOf(0) }
        var targetRank by remember { mutableStateOf(0) }
        val animatedSplashRank by animateIntAsState(
            targetValue = targetRank,
            animationSpec = tween(1200, easing = FastOutSlowInEasing),
            label = "SplashRank"
        )

        LaunchedEffect(Unit) {
            finalsAlpha = 1f
            finalsScale = 1f
            progressTarget = 1f

            delay(200)
            counterAlpha = 1f
            counterOffsetY = 0.dp

            delay(200)
            logoScale = 1.0f
            logoOffsetY = 0.dp

            delay(200)
            nameAlpha = 1f
        }

        LaunchedEffect(Unit) {
            val finalValue = currentRank ?: 0
            if (finalValue > 0) {
                val rollStartTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - rollStartTime < 600) {
                    rollValue = (1000..55000).random()
                    delay(40)
                }
                isRolling = false
                targetRank = finalValue
            } else {
                isRolling = false
            }
        }

        LaunchedEffect(Unit) {
            delay(2200)
            onDismiss()
        }

        val displayValue = if (isRolling) rollValue else animatedSplashRank

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "THE FINALS",
                color = palette.accent,
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

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(animCounterAlpha)
                    .offset(y = animCounterOffsetY)
            ) {
                Text(
                    text = formatNum(displayValue),
                    style = TextStyle(
                        brush = animatedRankBrush,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            if (currentRank != null) {
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .scale(animLogoScale)
                        .offset(y = animLogoOffsetY)
                ) {
                    Image(
                        painter = painterResource(id = rankLogoResFor(currentRank)),
                        contentDescription = rankNameFor(currentRank),
                        modifier = Modifier.size(250.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = getLocalizedRankName(rankNameFor(currentRank), isEnglish),
                    color = palette.cyan,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alpha(animNameAlpha)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

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