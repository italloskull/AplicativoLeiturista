package com.example.oaplicativo.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

@Composable
fun WaterBucketLoader(
    progress: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier.size(150.dp, 200.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "water_drops")
    
    // Animação das gotas caindo
    val drop1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drop1"
    )

    val drop2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drop2"
    )

    // Animação de vibração da água no balde
    val waterVibration by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vibration"
    )

    // SÊNIOR MOTION: Animação de transbordamento lateral quando chega em 1.0
    val overflowTransition = rememberInfiniteTransition(label = "overflow")
    val spillWidth by overflowTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spill"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val pipeColor = Color(0xFF475569) // Slate 600
        val waterColor = Color(0xFF38BDF8) // Sky 400
        
        // 1. DESENHAR A TORNEIRA (Pipe)
        drawRoundRect(
            color = pipeColor,
            topLeft = Offset(width * 0.8f, 0f),
            size = Size(8.dp.toPx(), height * 0.4f),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = pipeColor,
            topLeft = Offset(width * 0.4f, height * 0.1f),
            size = Size(width * 0.45f, 12.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = pipeColor,
            topLeft = Offset(width * 0.4f, height * 0.1f),
            size = Size(16.dp.toPx(), 24.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx())
        )

        // 2. DESENHAR AS GOTAS (Apenas se o progresso não estiver cheio)
        if (progress < 1f) {
            val dropStartX = width * 0.4f + 8.dp.toPx()
            val dropStartY = height * 0.25f
            val dropDistance = height * 0.5f

            drawCircle(
                color = waterColor.copy(alpha = 0.6f),
                radius = 4.dp.toPx(),
                center = Offset(dropStartX, dropStartY + (dropDistance * drop1Offset))
            )
            drawCircle(
                color = waterColor.copy(alpha = 0.4f),
                radius = 4.dp.toPx(),
                center = Offset(dropStartX, dropStartY + (dropDistance * drop2Offset))
            )
        }

        // 3. DESENHAR O BALDE (Bucket)
        val bucketWidth = width * 0.7f
        val bucketHeight = height * 0.4f
        val bucketLeft = (width - bucketWidth) / 2
        val bucketTop = height - bucketHeight - 10.dp.toPx()

        val bucketPath = Path().apply {
            moveTo(bucketLeft, bucketTop)
            lineTo(bucketLeft + bucketWidth * 0.1f, bucketTop + bucketHeight)
            lineTo(bucketLeft + bucketWidth * 0.9f, bucketTop + bucketHeight)
            lineTo(bucketLeft + bucketWidth, bucketTop)
            close()
        }

        drawPath(
            path = bucketPath,
            color = pipeColor.copy(alpha = 0.2f)
        )

        // 4. DESENHAR A ÁGUA E O TRANSBORDAMENTO
        clipPath(bucketPath) {
            val fillHeight = bucketHeight * progress.coerceIn(0f, 1f)
            val topVibration = if (progress > 0 && progress < 1f) waterVibration * 2.dp.toPx() else 0f
            
            drawRect(
                color = waterColor.copy(alpha = 0.7f),
                topLeft = Offset(bucketLeft, bucketTop + bucketHeight - fillHeight + topVibration),
                size = Size(bucketWidth, fillHeight)
            )
        }

        // Efeito de Transbordamento (Quando progress >= 1.0)
        if (progress >= 1f) {
            // SÊNIOR MOTION: Gotas vazando fisicamente das bordas do balde
            val spillDistance = height * 0.3f
            
            // Gota escorrendo pela esquerda
            drawCircle(
                color = waterColor.copy(alpha = 0.5f * (1f - drop1Offset)),
                radius = 3.dp.toPx(),
                center = Offset(bucketLeft + 4.dp.toPx(), bucketTop + bucketHeight + (spillDistance * drop1Offset))
            )
            
            // Gota escorrendo pela direita
            drawCircle(
                color = waterColor.copy(alpha = 0.5f * (1f - drop2Offset)),
                radius = 3.dp.toPx(),
                center = Offset(bucketLeft + bucketWidth - 4.dp.toPx(), bucketTop + bucketHeight + (spillDistance * drop2Offset))
            )

            // "Lágrimas" de água estáticas nas bordas
            drawRoundRect(
                color = waterColor.copy(alpha = 0.4f),
                topLeft = Offset(bucketLeft - 1.dp.toPx(), bucketTop),
                size = Size(3.dp.toPx(), bucketHeight * 0.9f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            drawRoundRect(
                color = waterColor.copy(alpha = 0.4f),
                topLeft = Offset(bucketLeft + bucketWidth - 2.dp.toPx(), bucketTop),
                size = Size(3.dp.toPx(), bucketHeight * 0.9f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}
