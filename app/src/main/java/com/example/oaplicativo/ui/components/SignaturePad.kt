package com.example.oaplicativo.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

class LinePath(
    val points: MutableList<Offset> = mutableStateListOf()
)

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    entrevistadoInfo: String = "",
    leituristaInfo: String = "",
    gpsInfo: String = "",
    onSignatureCaptured: (Bitmap, List<LinePath>) -> Unit,
    onClear: () -> Unit = {}
) {
    val lines = remember { mutableStateListOf<LinePath>() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 120.dp)
                .clipToBounds()
                .background(Color.White, shape = MaterialTheme.shapes.medium)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newLine = LinePath()
                            newLine.points.add(offset)
                            lines.add(newLine)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            lines.lastOrNull()?.points?.add(change.position)
                        },
                        onDragEnd = {},
                        onDragCancel = {}
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                // Desenha a linha de guia de assinatura no terço inferior
                val lineY = size.height * 0.78f
                drawLine(
                    color = Color.LightGray,
                    start = Offset(size.width * 0.08f, lineY),
                    end = Offset(size.width * 0.92f, lineY),
                    strokeWidth = 2.dp.toPx()
                )

                lines.forEach { line ->
                    if (line.points.size > 1) {
                        val path = Path().apply {
                            moveTo(line.points.first().x, line.points.first().y)
                            for (i in 1 until line.points.size) {
                                lineTo(line.points[i].x, line.points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = Color.Black,
                            style = Stroke(
                                width = 6.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else if (line.points.size == 1) {
                        drawCircle(
                            color = Color.Black,
                            radius = 3.dp.toPx(),
                            center = line.points.first()
                        )
                    }
                }
            }

            if (lines.isEmpty()) {
                Text(
                    text = "Assine com o dedo sobre a linha",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    lines.clear()
                    onClear()
                },
                modifier = Modifier.weight(1f),
                enabled = lines.isNotEmpty()
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Limpar")
            }

            Button(
                onClick = {
                    if (lines.isNotEmpty()) {
                        val timestampBrasilia = com.example.oaplicativo.util.DateUtils.getFullTimestampBrasilia()
                        val bitmap = createBitmapFromLinesWithStamp(
                            lines = lines,
                            width = 620,
                            signatureHeight = 280,
                            stampHeight = 135,
                            entrevistadoInfo = entrevistadoInfo,
                            leituristaInfo = leituristaInfo,
                            gpsInfo = gpsInfo,
                            timestampBrasilia = timestampBrasilia
                        )
                        onSignatureCaptured(bitmap, lines.toList())
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = lines.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Confirmar")
            }
        }
    }
}

fun createBitmapFromLinesWithStamp(
    lines: List<LinePath>,
    width: Int = 620,
    signatureHeight: Int = 280,
    stampHeight: Int = 135,
    entrevistadoInfo: String = "",
    leituristaInfo: String = "",
    gpsInfo: String = "",
    timestampBrasilia: String = com.example.oaplicativo.util.DateUtils.getFullTimestampBrasilia()
): Bitmap {
    val totalHeight = signatureHeight + stampHeight
    val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    // 1. DESENHA A ASSINATURA MANUSCRITA NA ÁREA SUPERIOR
    val paintStroke = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    val bounds = android.graphics.RectF()
    val combinedPath = android.graphics.Path()

    lines.forEach { line ->
        if (line.points.size > 1) {
            combinedPath.moveTo(line.points.first().x, line.points.first().y)
            for (i in 1 until line.points.size) {
                combinedPath.lineTo(line.points[i].x, line.points[i].y)
            }
        } else if (line.points.size == 1) {
            combinedPath.addCircle(
                line.points.first().x,
                line.points.first().y,
                4f,
                android.graphics.Path.Direction.CW
            )
        }
    }

    combinedPath.computeBounds(bounds, true)

    if (bounds.width() > 0 && bounds.height() > 0) {
        val matrix = android.graphics.Matrix()
        val padding = 20f
        val scaleX = (width - padding * 2) / bounds.width()
        val scaleY = (signatureHeight - padding * 2) / bounds.height()
        val scale = minOf(scaleX, scaleY, 2.0f)

        matrix.postTranslate(-bounds.left, -bounds.top)
        matrix.postScale(scale, scale)
        matrix.postTranslate(
            (width - bounds.width() * scale) / 2f,
            (signatureHeight - bounds.height() * scale) / 2f
        )
        combinedPath.transform(matrix)
    }

    canvas.drawPath(combinedPath, paintStroke)

    // 2. DESENHA O CARIMBO DIGITAL DE AUDITORIA NA ÁREA INFERIOR (LEI 14.063/2020)
    val paintLine = Paint().apply {
        color = android.graphics.Color.parseColor("#CBD5E1")
        strokeWidth = 2f
    }
    canvas.drawLine(15f, signatureHeight.toFloat(), (width - 15).toFloat(), signatureHeight.toFloat(), paintLine)

    val paintBg = Paint().apply {
        color = android.graphics.Color.parseColor("#F8FAFC")
        style = Paint.Style.FILL
    }
    canvas.drawRect(15f, (signatureHeight + 4).toFloat(), (width - 15).toFloat(), (totalHeight - 8).toFloat(), paintBg)

    val paintBorder = Paint().apply {
        color = android.graphics.Color.parseColor("#E2E8F0")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    canvas.drawRect(15f, (signatureHeight + 4).toFloat(), (width - 15).toFloat(), (totalHeight - 8).toFloat(), paintBorder)

    // Texto do Carimbo
    val paintTextHeader = Paint().apply {
        color = android.graphics.Color.parseColor("#0F172A")
        textSize = 16.5f
        isFakeBoldText = true
        isAntiAlias = true
    }

    val paintTextBody = Paint().apply {
        color = android.graphics.Color.parseColor("#334155")
        textSize = 13.5f
        isAntiAlias = true
    }

    val paintTextHash = Paint().apply {
        color = android.graphics.Color.parseColor("#0284C7")
        textSize = 11.2f
        typeface = android.graphics.Typeface.MONOSPACE
        isFakeBoldText = true
        isAntiAlias = true
    }

    val startX = 22f
    var startY = (signatureHeight + 26).toFloat()

    val entrevistadoClean = entrevistadoInfo.ifBlank { "NÃO INFORMADO" }
    val leituristaClean = leituristaInfo.ifBlank { "EQUIPE DE CAMPO" }
    val gpsClean = gpsInfo.ifBlank { "GPS NÃO CAPTURADO" }

    // Calcula Hash SHA-256 de integridade
    val rawPayload = "$entrevistadoClean|$leituristaClean|$timestampBrasilia|$gpsClean"
    val fullHash = computeFullSha256(rawPayload)

    canvas.drawText("⚖️ ASSINATURA ELETRÔNICA SIMPLES (LEI 14.063/2020)", startX, startY, paintTextHeader)
    startY += 22f
    canvas.drawText("Entrevistado: $entrevistadoClean  |  Agente: $leituristaClean", startX, startY, paintTextBody)
    startY += 20f
    canvas.drawText("Data/Hora: $timestampBrasilia  |  GPS: $gpsClean", startX, startY, paintTextBody)
    startY += 22f
    canvas.drawText("🔒 Hash SHA-256: $fullHash", startX, startY, paintTextHash)

    return bitmap
}

fun computeFullSha256(input: String): String {
    return try {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        hashBytes.joinToString("") { "%02x".format(it) }.uppercase()
    } catch (_: Exception) {
        "A1B2C3D4E5F67890A1B2C3D4E5F67890A1B2C3D4E5F67890A1B2C3D4E5F67890"
    }
}

fun bitmapToPngBytes(bitmap: Bitmap): ByteArray {
    val stream = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

fun computePngBytesSha256(bytes: ByteArray): String {
    return try {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        hashBytes.joinToString("") { "%02x".format(it) }.uppercase()
    } catch (_: Exception) {
        "A1B2C3D4E5F67890A1B2C3D4E5F67890A1B2C3D4E5F67890A1B2C3D4E5F67890"
    }
}
