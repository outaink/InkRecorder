package com.outaink.inkrecorder.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.outaink.inkrecorder.ui.WaveformProcessor.smoothWaveform
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * 波形图数据模型
 */
data class WaveformData(
    val amplitudes: List<Float>, // 振幅数据，范围 -1.0 到 1.0
    val progress: Float = 0f // 播放进度，范围 0.0 到 1.0
)

/**
 * 波形图样式配置
 */
data class WaveformStyle(
    val waveColor: Color = Color(0xFF2196F3),
    val progressColor: Color = Color(0xFF1976D2),
    val backgroundColor: Color = Color.Transparent,
    val strokeWidth: Dp = 2.dp,
    val barWidth: Dp = 3.dp,
    val barSpacing: Dp = 1.dp,
    val cornerRadius: Dp = 1.dp,
    val drawMode: DrawMode = DrawMode.Bars
)

/**
 * 绘制模式
 */
enum class DrawMode {
    Bars,      // 柱状图
    Line,      // 线条
    Mirror,    // 镜像柱状图
    Gradient   // 渐变柱状图
}

/**
 * 波形图 Composable
 */
@Composable
fun Waveform(
    data: WaveformData,
    modifier: Modifier = Modifier,
    style: WaveformStyle = WaveformStyle(),
    animated: Boolean = true
) {
    val density = LocalDensity.current

    // 动画值
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) data.progress else data.progress,
        animationSpec = tween(300)
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(style.backgroundColor)
    ) {
        when (style.drawMode) {
            DrawMode.Bars -> drawBars(data, animatedProgress, style, density)
            DrawMode.Line -> drawLine(data, animatedProgress, style, density)
            DrawMode.Mirror -> drawMirrorBars(data, animatedProgress, style, density)
            DrawMode.Gradient -> drawGradientBars(data, animatedProgress, style, density)
        }
    }
}

/**
 * 绘制柱状波形
 */
private fun DrawScope.drawBars(
    data: WaveformData,
    progress: Float,
    style: WaveformStyle,
    density: androidx.compose.ui.unit.Density
) {
    val barWidthPx = with(density) { style.barWidth.toPx() }
    val barSpacingPx = with(density) { style.barSpacing.toPx() }
    val strokeWidthPx = with(density) { style.strokeWidth.toPx() }
    val cornerRadiusPx = with(density) { style.cornerRadius.toPx() }

    val totalBarWidth = barWidthPx + barSpacingPx
    val barCount = (size.width / totalBarWidth).toInt()

    if (data.amplitudes.isEmpty()) return

    val samplesPerBar = data.amplitudes.size / barCount.coerceAtLeast(1)
    val centerY = size.height / 2

    for (i in 0 until barCount) {
        val startIdx = i * samplesPerBar
        val endIdx = ((i + 1) * samplesPerBar).coerceAtMost(data.amplitudes.size)

        if (startIdx >= data.amplitudes.size) break

        // 计算该柱的平均振幅
        val avgAmplitude = data.amplitudes
            .subList(startIdx, endIdx)
            .map { it.absoluteValue }
            .average()
            .toFloat()

        val barHeight = avgAmplitude * size.height * 0.8f
        val x = i * totalBarWidth

        // 判断是否已播放
        val isPlayed = (i.toFloat() / barCount) <= progress
        val color = if (isPlayed) style.progressColor else style.waveColor

        // 绘制柱状
        drawRoundRect(
            color = color,
            topLeft = Offset(x, centerY - barHeight / 2),
            size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
        )
    }
}

/**
 * 绘制线条波形
 */
private fun DrawScope.drawLine(
    data: WaveformData,
    progress: Float,
    style: WaveformStyle,
    density: androidx.compose.ui.unit.Density
) {
    if (data.amplitudes.isEmpty()) return

    val strokeWidthPx = with(density) { style.strokeWidth.toPx() }
    val path = Path()
    val centerY = size.height / 2
    val progressX = size.width * progress

    // 创建波形路径
    data.amplitudes.forEachIndexed { index, amplitude ->
        val x = (index.toFloat() / data.amplitudes.size) * size.width
        val y = centerY - (amplitude * size.height * 0.4f)

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    // 绘制未播放部分
    drawPath(
        path = path,
        color = style.waveColor,
        style = Stroke(width = strokeWidthPx)
    )

    // 绘制已播放部分
    if (progress > 0) {
        clipRect(right = progressX) {
            drawPath(
                path = path,
                color = style.progressColor,
                style = Stroke(width = strokeWidthPx)
            )
        }
    }
}

/**
 * 绘制镜像柱状波形
 */
private fun DrawScope.drawMirrorBars(
    data: WaveformData,
    progress: Float,
    style: WaveformStyle,
    density: androidx.compose.ui.unit.Density
) {
    val barWidthPx = with(density) { style.barWidth.toPx() }
    val barSpacingPx = with(density) { style.barSpacing.toPx() }
    val cornerRadiusPx = with(density) { style.cornerRadius.toPx() }

    val totalBarWidth = barWidthPx + barSpacingPx
    val barCount = (size.width / totalBarWidth).toInt()

    if (data.amplitudes.isEmpty()) return

    val samplesPerBar = data.amplitudes.size / barCount.coerceAtLeast(1)
    val centerY = size.height / 2

    for (i in 0 until barCount) {
        val startIdx = i * samplesPerBar
        val endIdx = ((i + 1) * samplesPerBar).coerceAtMost(data.amplitudes.size)

        if (startIdx >= data.amplitudes.size) break

        val avgAmplitude = data.amplitudes
            .subList(startIdx, endIdx)
            .map { it.absoluteValue }
            .average()
            .toFloat()

        val barHeight = avgAmplitude * size.height * 0.4f
        val x = i * totalBarWidth

        val isPlayed = (i.toFloat() / barCount) <= progress
        val color = if (isPlayed) style.progressColor else style.waveColor

        // 绘制上半部分
        drawRoundRect(
            color = color,
            topLeft = Offset(x, centerY - barHeight),
            size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
        )

        // 绘制下半部分
        drawRoundRect(
            color = color,
            topLeft = Offset(x, centerY),
            size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
        )
    }
}

/**
 * 绘制渐变柱状波形
 */
private fun DrawScope.drawGradientBars(
    data: WaveformData,
    progress: Float,
    style: WaveformStyle,
    density: androidx.compose.ui.unit.Density
) {
    val barWidthPx = with(density) { style.barWidth.toPx() }
    val barSpacingPx = with(density) { style.barSpacing.toPx() }
    val cornerRadiusPx = with(density) { style.cornerRadius.toPx() }

    val totalBarWidth = barWidthPx + barSpacingPx
    val barCount = (size.width / totalBarWidth).toInt()

    if (data.amplitudes.isEmpty()) return

    val samplesPerBar = data.amplitudes.size / barCount.coerceAtLeast(1)
    val centerY = size.height / 2

    for (i in 0 until barCount) {
        val startIdx = i * samplesPerBar
        val endIdx = ((i + 1) * samplesPerBar).coerceAtMost(data.amplitudes.size)

        if (startIdx >= data.amplitudes.size) break

        val avgAmplitude = data.amplitudes
            .subList(startIdx, endIdx)
            .map { it.absoluteValue }
            .average()
            .toFloat()

        val barHeight = avgAmplitude * size.height * 0.8f
        val x = i * totalBarWidth

        val isPlayed = (i.toFloat() / barCount) <= progress
        val baseColor = if (isPlayed) style.progressColor else style.waveColor

        // 创建渐变
        val gradient = Brush.verticalGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.3f),
                baseColor,
                baseColor.copy(alpha = 0.3f)
            ),
            startY = centerY - barHeight / 2,
            endY = centerY + barHeight / 2
        )

        drawRoundRect(
            brush = gradient,
            topLeft = Offset(x, centerY - barHeight / 2),
            size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
        )
    }
}

/**
 * 示例：如何使用 Waveform
 */
@Preview(showBackground = true)
@Composable
fun WaveformExample() {
    // 生成示例数据
    val waveformData = remember {
        WaveformData(
            amplitudes = smoothWaveform(List(200) { Random.nextFloat() * 2 - 1 }),
            progress = 0.3f
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 柱状图样式
        Waveform(
            data = waveformData,
            style = WaveformStyle(
                drawMode = DrawMode.Bars,
                waveColor = Color(0xFF4CAF50),
                progressColor = Color(0xFF2E7D32)
            )
        )

        // 线条样式
        Waveform(
            data = waveformData,
            style = WaveformStyle(
                drawMode = DrawMode.Line,
                strokeWidth = 3.dp
            )
        )

        // 镜像样式
        Waveform(
            data = waveformData,
            style = WaveformStyle(
                drawMode = DrawMode.Mirror,
                waveColor = Color(0xFFFF5722),
                progressColor = Color(0xFFD84315)
            )
        )

        // 渐变样式
        Waveform(
            data = waveformData,
            style = WaveformStyle(
                drawMode = DrawMode.Gradient,
                waveColor = Color(0xFF9C27B0),
                progressColor = Color(0xFF6A1B9A)
            ),
            animated = true
        )
    }
}

/**
 * 音频波形处理工具类
 */
object WaveformProcessor {
    /**
     * 从音频采样数据生成波形数据
     */
    fun processAudioSamples(
        samples: ShortArray,
        targetSize: Int = 100
    ): List<Float> {
        if (samples.isEmpty()) return emptyList()

        val result = mutableListOf<Float>()
        val samplesPerPoint = samples.size / targetSize

        for (i in 0 until targetSize) {
            val start = i * samplesPerPoint
            val end = ((i + 1) * samplesPerPoint).coerceAtMost(samples.size)

            if (start >= samples.size) break

            // 计算该段的平均振幅
            var sum = 0.0
            for (j in start until end) {
                sum += samples[j].toFloat() / Short.MAX_VALUE
            }

            result.add((sum / (end - start)).toFloat())
        }

        return result
    }

    /**
     * 平滑波形数据
     */
    fun smoothWaveform(
        amplitudes: List<Float>,
        windowSize: Int = 3
    ): List<Float> {
        if (amplitudes.size <= windowSize) return amplitudes

        return amplitudes.mapIndexed { index, _ ->
            val start = (index - windowSize / 2).coerceAtLeast(0)
            val end = (index + windowSize / 2 + 1).coerceAtMost(amplitudes.size)

            amplitudes.subList(start, end).average().toFloat()
        }
    }
}
