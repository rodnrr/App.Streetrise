package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF011624), // Midnight Navy Top
            Color(0xFF032D3D), // Deep Ocean Blue
            Color(0xFF0B192A)  // Dark Blue Bottom
          )
        )
      )
      .testTag("streetrise_loading_screen")
  ) {
    // 1. Watermark Map Grid Lines in background
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val lineColor = Color(0xFF46CACF).copy(alpha = 0.05f)
      val strokeWidth = 1.5.dp.toPx()

      // Diagonal Street Grid Map effect
      for (i in -4..10) {
        drawLine(
          color = lineColor,
          start = Offset(0f, i * h / 8f),
          end = Offset(w, (i + 2) * h / 8f),
          strokeWidth = strokeWidth
        )
        drawLine(
          color = lineColor,
          start = Offset(w, i * h / 8f),
          end = Offset(0f, (i + 2) * h / 8f),
          strokeWidth = strokeWidth
        )
      }

      // Draw faint navigation concentric rings / location center coordinates
      drawCircle(
        color = lineColor,
        center = Offset(w * 0.25f, h * 0.25f),
        radius = h * 0.12f,
        style = Stroke(strokeWidth)
      )
      drawCircle(
        color = lineColor,
        center = Offset(w * 0.75f, h * 0.23f),
        radius = h * 0.15f,
        style = Stroke(strokeWidth)
      )
    }

    // 2. Central Logo & Branding Content
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.Center)
        .padding(horizontal = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      
      // Floating glowing orb behind Pin Logo
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
      ) {
        // Glowing aura match
        Canvas(modifier = Modifier.size(140.dp * glowScale)) {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                Color(0xFFFFDFB4).copy(alpha = 0.28f), // Soft Sunrise Golden glow
                Color(0xFF46CACF).copy(alpha = 0.09f),
                Color.Transparent
              )
            )
          )
        }

        // Custom drawn pin containing hills, sun, winding path and arrow
        Canvas(
          modifier = Modifier
            .size(110.dp)
            .testTag("splash_pin_logo")
        ) {
          val width = size.width
          val height = size.height

          // Draw Teardrop bounding container
          // High fidelity vector map pin path
          val teardropPath = Path().apply {
            moveTo(width / 2f, height)
            cubicTo(
              width / 8f, height * 0.65f,
              0f, height * 0.45f,
              0f, height * 0.32f
            )
            cubicTo(
              0f, height * 0.14f,
              width * 0.22f, 0f,
              width / 2f, 0f
            )
            cubicTo(
              width * 0.78f, 0f,
              width, height * 0.14f,
              width, height * 0.32f
            )
            cubicTo(
              width, height * 0.45f,
              width * 0.88f, height * 0.65f,
              width / 2f, height
            )
          }

          // Draw base white outer thick frame
          drawPath(
            path = teardropPath,
            color = Color.White,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
          )

          // Clip content inside the teardrop
          drawPath(
            path = teardropPath,
            brush = Brush.verticalGradient(
              colors = listOf(
                Color(0xFF0FBDD3), // Teal sky top
                Color(0xFF042B3A)  // Deep bottom
              )
            )
          )

          // Draw glowing sun circle
          drawCircle(
            color = Color(0xFFFFAE34),
            radius = width * 0.18f,
            center = Offset(width / 2f, height * 0.38f)
          )

          // Draw sun rays
          val rayLength = width * 0.08f
          val numRays = 8
          for (i in 0 until numRays) {
            val angleDeg = 180f + (i * 180f / (numRays - 1))
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val startX = (width / 2f) + (width * 0.22f * Math.cos(angleRad)).toFloat()
            val startY = (height * 0.38f) + (width * 0.22f * Math.sin(angleRad)).toFloat()
            val endX = startX + (rayLength * Math.cos(angleRad)).toFloat()
            val endY = startY + (rayLength * Math.sin(angleRad)).toFloat()

            drawLine(
              color = Color(0xFFFFAE34).copy(alpha = 0.85f),
              start = Offset(startX, startY),
              end = Offset(endX, endY),
              strokeWidth = 2.dp.toPx(),
              cap = StrokeCap.Round
            )
          }

          // Draw turquoise left hill waving
          val leftHill = Path().apply {
            moveTo(0f, height * 0.5f)
            quadraticTo(
              width * 0.35f, height * 0.42f,
              width * 0.75f, height * 0.58f
            )
            lineTo(width * 0.75f, height)
            lineTo(0f, height)
            close()
          }
          drawPath(
            path = leftHill,
            color = Color(0xFF1D8396)
          )

          // Draw darker right hill waving
          val rightHill = Path().apply {
            moveTo(width * 0.45f, height * 0.52f)
            quadraticTo(
              width * 0.75f, height * 0.45f,
              width, height * 0.5f
            )
            lineTo(width, height)
            lineTo(width * 0.45f, height)
            close()
          }
          drawPath(
            path = rightHill,
            color = Color(0xFF04586B)
          )

          // Draw curved rising winding road
          val roadPath = Path().apply {
            moveTo(width * 0.15f, height * 0.95f)
            cubicTo(
              width * 0.3f, height * 0.8f,
              width * 0.18f, height * 0.7f,
              width * 0.45f, height * 0.6f
            )
            cubicTo(
              width * 0.58f, height * 0.55f,
              width * 0.55f, height * 0.48f,
              width * 0.68f, height * 0.44f
            )
          }

          drawPath(
            path = roadPath,
            color = Color.White,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
          )

          // Draw precise arrow tip winding forward
          val arrowTip = Path().apply {
            moveTo(width * 0.58f, height * 0.44f)
            lineTo(width * 0.72f, height * 0.42f)
            lineTo(width * 0.69f, height * 0.54f)
          }
          drawPath(
            path = arrowTip,
            color = Color.White
          )
        }
      }

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
