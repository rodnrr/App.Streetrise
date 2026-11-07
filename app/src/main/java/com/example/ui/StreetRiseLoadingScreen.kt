package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun StreetRiseLoadingScreen(
  modifier: Modifier = Modifier,
  stateText: String = "Loading nearby resources...",
  onDismiss: (() -> Unit)? = null
) {
  // Infinite rotation for indicator
  val infiniteTransition = rememberInfiniteTransition(label = "indicator")
  val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteSpec(1500),
    label = "rotation"
  )

  // Gentle pulse for the glow around the logo pin
  val glowScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )

  // Auto-dismiss safety timeout helper
  var showDismissBtn by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay(7000)
    showDismissBtn = true
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF011624))
      .testTag("streetrise_loading_screen")
  ) {
    Image(
      painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_splash_screen_1784581397533),
      contentDescription = "StreetRise Splash",
      modifier = Modifier.fillMaxSize(),
      contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )

    // 2. Central Logo & Branding Content
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.Center)
        .padding(horizontal = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      
      Image(
        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.streetrise_sr_icon_1794083325402),
        contentDescription = "StreetRise Icon",
        modifier = Modifier
          .size(110.dp)
          .clip(RoundedCornerShape(24.dp))
          .testTag("splash_pin_logo")
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Logo brand typography
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = "Street",
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Bold,
          fontSize = 38.sp,
          color = Color.White
        )
        Text(
          text = "Rise",
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Light,
          fontSize = 38.sp,
          color = Color(0xFF46CACF)
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Connecting people to local resources",
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = Color(0xFFBACEDD),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(64.dp))

      // Rotating Circular Indicator matching user image
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(52.dp)
      ) {
        CircularProgressIndicator(
          modifier = Modifier
            .size(36.dp)
            .testTag("splash_loading_indicator"),
          color = Color(0xFF46CACF),
          strokeWidth = 2.dp,
          trackColor = Color(0xFF46CACF).copy(alpha = 0.15f)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = stateText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFFBACEDD).copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("splash_loading_status_text")
      )
    }

    // 3. Optional Safe Auto-Dismiss Skip Action if load hangs too long
    if (showDismissBtn && onDismiss != null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
      ) {
        TextButton(
          onClick = onDismiss,
          colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF46CACF)),
          modifier = Modifier.testTag("splash_skip_button")
        ) {
          Text(
            "Skip to Offline Directory >",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

private fun infiniteSpec(duration: Int): InfiniteRepeatableSpec<Float> {
  return infiniteRepeatable(
    animation = tween(duration, easing = LinearEasing),
    repeatMode = RepeatMode.Restart
  )
}
