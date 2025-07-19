package com.outaink.inkrecorder.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValueAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
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
import java.lang.Math.pow
import kotlin.math.absoluteValue
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.pow
import kotlin.random.Random

/**
 * 波形图数据模型
 */
data class WaveformData(
    val amplitudes: List<Float>, // 振幅数据，范围 -1.0 到 1.0
    val frequencies: List<Float> = emptyList(), // 频谱数据，范围 0.0 到 1.0，从高频到低频
    val progress: Float = 0f, // 播放进度，范围 0.0 到 1.0
    val isFrequencyMode: Boolean = false // 是否为频谱模式
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
    val drawMode: DrawMode = DrawMode.Bars,
    val animationIntensity: Float = 1.2f // 动画强度倍数，用于放大新数据的变化
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

    // 动画值 - 进度动画
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) data.progress else data.progress,
        animationSpec = tween(300),
        label = "progress"
    )

    // 维护前一个状态以实现平滑过渡
    val previousAmplitudes = remember { mutableStateOf(data.amplitudes) }
    val animationProgress = remember { mutableStateOf(1f) }
    
    // 当数据更新时触发动画
    LaunchedEffect(data.amplitudes) {
        if (animated && data.amplitudes != previousAmplitudes.value && data.amplitudes.isNotEmpty()) {
            animationProgress.value = 0f
        }
    }
    
    // 动画进度
    val interpolationProgress by animateFloatAsState(
        targetValue = animationProgress.value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = { animationProgress.value = 1f },
        label = "waveform_update"
    )
    
    // 插值后的振幅数据
    val interpolatedAmplitudes = remember(data.amplitudes, interpolationProgress) {
        if (!animated || previousAmplitudes.value.isEmpty() || interpolationProgress >= 1f) {
            data.amplitudes
        } else {
            // 在前一个状态和当前状态之间进行插值
            val prev = previousAmplitudes.value
            val current = data.amplitudes
            val maxSize = maxOf(prev.size, current.size)
            
            List(maxSize) { i ->
                val prevValue = prev.getOrElse(i) { 0f }
                val currentValue = current.getOrElse(i) { 0f }
                prevValue + (currentValue - prevValue) * interpolationProgress
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(style.backgroundColor)
    ) {
        // 创建动画化的数据对象
        val animatedData = WaveformData(
            amplitudes = interpolatedAmplitudes,
            progress = data.progress
        )
        
        when (style.drawMode) {
            DrawMode.Bars -> drawBars(animatedData, animatedProgress, style, density, interpolationProgress)
            DrawMode.Line -> drawLine(animatedData, animatedProgress, style, density, interpolationProgress)
            DrawMode.Mirror -> drawMirrorBars(animatedData, animatedProgress, style, density, interpolationProgress)
            DrawMode.Gradient -> drawGradientBars(animatedData, animatedProgress, style, density, interpolationProgress)
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
    density: androidx.compose.ui.unit.Density,
    animationProgress: Float = 1f
) {
    val barWidthPx = with(density) { style.barWidth.toPx() }
    val barSpacingPx = with(density) { style.barSpacing.toPx() }
    val cornerRadiusPx = with(density) { style.cornerRadius.toPx() }

    val totalBarWidth = barWidthPx + barSpacingPx
    val barCount = (size.width / totalBarWidth).toInt()

    // 选择要显示的数据源
    val displayData = if (data.isFrequencyMode && data.frequencies.isNotEmpty()) {
        data.frequencies
    } else {
        data.amplitudes
    }

    if (displayData.isEmpty()) return

    val samplesPerBar = displayData.size / barCount.coerceAtLeast(1)

    for (i in 0 until barCount) {
        val startIdx = i * samplesPerBar
        val endIdx = ((i + 1) * samplesPerBar).coerceAtMost(displayData.size)

        if (startIdx >= displayData.size) break

        // 计算该柱的平均振幅
        val avgAmplitude = if (data.isFrequencyMode) {
            // 频谱模式：数据已经是正值，直接使用
            displayData.subList(startIdx, endIdx).average().toFloat()
        } else {
            // 波形模式：取绝对值
            displayData.subList(startIdx, endIdx).map { it.absoluteValue }.average().toFloat()
        }

        // 应用动画强度和进度
        val animationScale = 1f + (style.animationIntensity - 1f) * (1f - animationProgress)
        val barHeight = if (data.isFrequencyMode) {
            // 频谱模式：从底部向上绘制，高度基于频率强度
            avgAmplitude * size.height * 0.9f * animationScale
        } else {
            // 波形模式：居中绘制
            avgAmplitude * size.height * 0.8f * animationScale
        }
        
        val x = i * totalBarWidth

        // 判断是否已播放
        val isPlayed = (i.toFloat() / barCount) <= progress
        val baseColor = if (isPlayed) style.progressColor else style.waveColor
        
        // 添加动画期间的颜色强度变化
        val colorIntensity = 0.7f + 0.3f * animationProgress
        
        // 频谱模式：根据频率高低调整颜色
        val color = if (data.isFrequencyMode) {
            val freqRatio = i.toFloat() / barCount
            // 高频(左侧)偏蓝，低频(右侧)偏红
            val hue = 240f - freqRatio * 60f // 240°(蓝) 到 180°(青绿)
            val saturation = 0.8f
            val lightness = 0.4f + avgAmplitude * 0.4f // 根据强度调整亮度
            android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, lightness))
                .let { Color(it) }
                .copy(alpha = colorIntensity)
        } else {
            baseColor.copy(alpha = baseColor.alpha * colorIntensity)
        }

        // 绘制柱状
        if (data.isFrequencyMode) {
            // 频谱模式：从底部开始绘制
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
            )
        } else {
            // 波形模式：居中绘制
            val centerY = size.height / 2
            drawRoundRect(
                color = color,
                topLeft = Offset(x, centerY - barHeight / 2),
                size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
            )
        }
    }
}

/**
 * 绘制线条波形
 */
private fun DrawScope.drawLine(
    data: WaveformData,
    progress: Float,
    style: WaveformStyle,
    density: androidx.compose.ui.unit.Density,
    animationProgress: Float = 1f
) {
    if (data.amplitudes.isEmpty()) return

    val strokeWidthPx = with(density) { style.strokeWidth.toPx() }
    val path = Path()
    val centerY = size.height / 2
    val progressX = size.width * progress
    
    // 动画缩放和颜色强度
    val animationScale = 1f + (style.animationIntensity - 1f) * (1f - animationProgress)
    val colorIntensity = 0.7f + 0.3f * animationProgress

    // 创建波形路径
    data.amplitudes.forEachIndexed { index, amplitude ->
        val x = (index.toFloat() / data.amplitudes.size) * size.width
        val y = centerY - (amplitude * size.height * 0.4f * animationScale)

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    // 绘制未播放部分
    drawPath(
        path = path,
        color = style.waveColor.copy(alpha = style.waveColor.alpha * colorIntensity),
        style = Stroke(width = strokeWidthPx)
    )

    // 绘制已播放部分
    if (progress > 0) {
        clipRect(right = progressX) {
            drawPath(
                path = path,
                color = style.progressColor.copy(alpha = style.progressColor.alpha * colorIntensity),
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
    density: androidx.compose.ui.unit.Density,
    animationProgress: Float = 1f
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

        // 应用动画强度和进度
        val animationScale = 1f + (style.animationIntensity - 1f) * (1f - animationProgress)
        val barHeight = avgAmplitude * size.height * 0.4f * animationScale
        val x = i * totalBarWidth

        val isPlayed = (i.toFloat() / barCount) <= progress
        val baseColor = if (isPlayed) style.progressColor else style.waveColor
        
        // 添加动画期间的颜色强度变化
        val colorIntensity = 0.7f + 0.3f * animationProgress
        val color = baseColor.copy(alpha = baseColor.alpha * colorIntensity)

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
    density: androidx.compose.ui.unit.Density,
    animationProgress: Float = 1f
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

        // 应用动画强度和进度
        val animationScale = 1f + (style.animationIntensity - 1f) * (1f - animationProgress)
        val barHeight = avgAmplitude * size.height * 0.8f * animationScale
        val x = i * totalBarWidth

        val isPlayed = (i.toFloat() / barCount) <= progress
        val baseColor = if (isPlayed) style.progressColor else style.waveColor
        
        // 添加动画期间的颜色强度变化
        val colorIntensity = 0.7f + 0.3f * animationProgress

        // 创建渐变
        val gradient = Brush.verticalGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.3f * colorIntensity),
                baseColor.copy(alpha = baseColor.alpha * colorIntensity),
                baseColor.copy(alpha = 0.3f * colorIntensity)
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
     * 从音频采样数据生成频谱数据
     */
    fun processAudioToFrequency(
        samples: ShortArray,
        sampleRate: Int = 48000,
        targetSize: Int = 100
    ): List<Float> {
        if (samples.isEmpty()) return List(targetSize) { 0f }

        // Ensure we have a power of 2 for FFT
        val fftSize = nearestPowerOfTwo(samples.size.coerceAtMost(1024))
        val normalizedSamples = DoubleArray(fftSize) { i ->
            if (i < samples.size) {
                samples[i].toDouble() / Short.MAX_VALUE
            } else {
                0.0 // Zero padding
            }
        }

        // Apply windowing function (Hamming window)
        applyHammingWindow(normalizedSamples)

        // Perform FFT
        val fftResult = fft(normalizedSamples)

        // Convert to magnitude spectrum (only use first half due to symmetry)
        val magnitudes = DoubleArray(fftSize / 2) { i ->
            val real = fftResult[i * 2]
            val imag = fftResult[i * 2 + 1]
            sqrt(real * real + imag * imag)
        }

        // Convert to frequency bands (high to low frequency)
        return convertToFrequencyBands(magnitudes, sampleRate, targetSize)
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

    /**
     * 找到最接近的2的幂次
     */
    private fun nearestPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) {
            power *= 2
        }
        return power
    }

    /**
     * 应用汉明窗函数
     */
    private fun applyHammingWindow(samples: DoubleArray) {
        val n = samples.size
        for (i in samples.indices) {
            val window = 0.54 - 0.46 * cos(2 * PI * i / (n - 1))
            samples[i] *= window
        }
    }

    /**
     * 快速傅里叶变换 (FFT)
     * 返回交错的实部和虚部 [real0, imag0, real1, imag1, ...]
     */
    private fun fft(samples: DoubleArray): DoubleArray {
        val n = samples.size
        if (n <= 1) return samples

        // 创建复数数组 (实部, 虚部交错)
        val complex = DoubleArray(n * 2)
        for (i in samples.indices) {
            complex[i * 2] = samples[i]     // 实部
            complex[i * 2 + 1] = 0.0        // 虚部
        }

        fftRecursive(complex, n)
        return complex
    }

    /**
     * 递归FFT实现
     */
    private fun fftRecursive(complex: DoubleArray, n: Int) {
        if (n <= 1) return

        // 位逆序排列
        bitReverseReorder(complex, n)

        // 蝶形运算
        var len = 2
        while (len <= n) {
            val angle = -2 * PI / len
            val wlen = DoubleArray(2) // [cos(angle), sin(angle)]
            wlen[0] = cos(angle)
            wlen[1] = sin(angle)

            var i = 0
            while (i < n) {
                val w = doubleArrayOf(1.0, 0.0) // [1, 0]
                for (j in 0 until len / 2) {
                    val u_idx = (i + j) * 2
                    val v_idx = (i + j + len / 2) * 2

                    val u_real = complex[u_idx]
                    val u_imag = complex[u_idx + 1]
                    val v_real = complex[v_idx]
                    val v_imag = complex[v_idx + 1]

                    // v = v * w
                    val temp_real = v_real * w[0] - v_imag * w[1]
                    val temp_imag = v_real * w[1] + v_imag * w[0]

                    complex[v_idx] = u_real - temp_real
                    complex[v_idx + 1] = u_imag - temp_imag
                    complex[u_idx] = u_real + temp_real
                    complex[u_idx + 1] = u_imag + temp_imag

                    // w = w * wlen
                    val w_real = w[0] * wlen[0] - w[1] * wlen[1]
                    val w_imag = w[0] * wlen[1] + w[1] * wlen[0]
                    w[0] = w_real
                    w[1] = w_imag
                }
                i += len
            }
            len *= 2
        }
    }

    /**
     * 位逆序重排
     */
    private fun bitReverseReorder(complex: DoubleArray, n: Int) {
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit

            if (i < j) {
                // 交换complex[i]和complex[j]
                val temp_real = complex[i * 2]
                val temp_imag = complex[i * 2 + 1]
                complex[i * 2] = complex[j * 2]
                complex[i * 2 + 1] = complex[j * 2 + 1]
                complex[j * 2] = temp_real
                complex[j * 2 + 1] = temp_imag
            }
        }
    }

    /**
     * 将FFT magnitude转换为频率带 (从高频到低频)
     */
    private fun convertToFrequencyBands(
        magnitudes: DoubleArray,
        sampleRate: Int,
        targetSize: Int
    ): List<Float> {
        val nyquist = sampleRate / 2
        val freqPerBin = nyquist.toDouble() / magnitudes.size

        // 定义频率范围 (人耳可听范围: 20Hz - 20kHz)
        val minFreq = 20.0
        val maxFreq = 20000.0.coerceAtMost(nyquist.toDouble())

        // 使用对数刻度分配频率带 (更符合人耳感知)
        val result = mutableListOf<Float>()
        for (i in 0 until targetSize) {
            // 从高频到低频 (索引0是最高频)
            val freqRatio = i.toDouble() / (targetSize - 1)
            val logMinFreq = log10(minFreq)
            val logMaxFreq = log10(maxFreq)
            val logFreq = logMaxFreq - freqRatio * (logMaxFreq - logMinFreq)
            val targetFreq = 10.0.pow(logFreq)

            // 找到对应的频率bin范围
            val binStart = (targetFreq / freqPerBin).toInt().coerceIn(0, magnitudes.size - 1)
            val binEnd = ((targetFreq + freqPerBin) / freqPerBin).toInt().coerceIn(binStart, magnitudes.size - 1)

            // 计算该频率带的平均幅度
            var sum = 0.0
            var count = 0
            for (bin in binStart..binEnd) {
                sum += magnitudes[bin]
                count++
            }

            val avgMagnitude = if (count > 0) sum / count else 0.0
            // 应用对数压缩并归一化到[0,1]
            val normalizedMagnitude = (log10(1 + avgMagnitude * 1000) / log10(1001.0)).coerceIn(0.0, 1.0)
            result.add(normalizedMagnitude.toFloat())
        }

        return result
    }

}
