package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.SignalEntity
import com.example.data.model.CandleStickData
import com.example.data.model.ChartCrosshairState
import com.example.data.model.ChartIndicatorConfig
import com.example.data.model.ChartViewMode
import com.example.data.model.RsiClassification
import com.example.data.model.TechnicalAnalysis
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun LightweightPriceChart(
    symbol: String,
    candles: List<CandleStickData>,
    currentPrice: Double,
    activeSignal: SignalEntity?,
    allSignals: List<SignalEntity>,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onSignalClick: ((SignalEntity) -> Unit)? = null
) {
    var viewMode by remember { mutableStateOf(ChartViewMode.CANDLESTICK) }
    var indicatorConfig by remember { mutableStateOf(ChartIndicatorConfig()) }
    var crosshairState by remember { mutableStateOf(ChartCrosshairState()) }
    var showIndicatorMenu by remember { mutableStateOf(false) }
    var selectedSignalDetail by remember { mutableStateOf<SignalEntity?>(null) }

    val textMeasurer = rememberTextMeasurer()

    Card(
        colors = CardDefaults.cardColors(containerColor = PanelDark),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("lightweight_price_chart_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // --- 1. Top Header: Symbol, Live Price, Current Time, View Selector ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = symbol,
                            color = PrimaryWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberNeonGreen.copy(alpha = 0.15f))
                                .border(1.dp, CyberNeonGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = CyberNeonGreen,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    val lastClose = candles.lastOrNull()?.close ?: currentPrice
                    val firstOpen = candles.firstOrNull()?.open ?: currentPrice
                    val priceChange = lastClose - firstOpen
                    val changePercent = if (firstOpen != 0.0) (priceChange / firstOpen) * 100.0 else 0.0
                    val isPositive = priceChange >= 0

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2f", currentPrice),
                            color = if (isPositive) CyberNeonGreen else CyberCrimson,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "%s%.2f (%.2f%%)", if (isPositive) "+" else "", priceChange, changePercent),
                            color = if (isPositive) CyberNeonGreen else CyberCrimson,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Controls: View Mode Tabs and Indicators Filter Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { showIndicatorMenu = !showIndicatorMenu },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .border(1.dp, if (showIndicatorMenu) accentColor else BorderGray, CircleShape)
                            .testTag("toggle_indicators_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Chart Indicators",
                            tint = if (showIndicatorMenu) accentColor else SubtitleWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // View Mode Segmented Controls
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(ChartViewMode.values()) { mode ->
                    val isSelected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF161616))
                            .border(1.dp, if (isSelected) accentColor else BorderGray.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { viewMode = mode }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("chart_mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) accentColor else SubtitleWhite,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Expandable Indicator Options Panel
            AnimatedVisibility(
                visible = showIndicatorMenu,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "INDICATOR OVERLAYS & SIGNALS",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IndicatorToggleChip(
                            label = "EMA 9/21",
                            checked = indicatorConfig.showEma9,
                            onToggle = { indicatorConfig = indicatorConfig.copy(showEma9 = it, showEma21 = it) }
                        )
                        IndicatorToggleChip(
                            label = "Bollinger",
                            checked = indicatorConfig.showBollingerBands,
                            onToggle = { indicatorConfig = indicatorConfig.copy(showBollingerBands = it) }
                        )
                        IndicatorToggleChip(
                            label = "Volume",
                            checked = indicatorConfig.showVolume,
                            onToggle = { indicatorConfig = indicatorConfig.copy(showVolume = it) }
                        )
                        IndicatorToggleChip(
                            label = "RSI (14)",
                            checked = indicatorConfig.showRsi,
                            onToggle = { indicatorConfig = indicatorConfig.copy(showRsi = it) }
                        )
                        IndicatorToggleChip(
                            label = "AI Badges",
                            checked = indicatorConfig.showSignalMarkers,
                            onToggle = { indicatorConfig = indicatorConfig.copy(showSignalMarkers = it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 2. Interactive Crosshair HUD Banner ---
            if (crosshairState.isVisible && crosshairState.candle != null) {
                val c = crosshairState.candle!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .border(1.dp, CyberAqua.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = c.timeLabel,
                            color = CyberAqua,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            HudValue("O", c.open)
                            HudValue("H", c.high)
                            HudValue("L", c.low)
                            HudValue("C", c.close)
                            c.rsi?.let { rsiVal ->
                                val rsiColor = when {
                                    rsiVal >= 70.0 -> CyberCrimson
                                    rsiVal <= 30.0 -> CyberNeonGreen
                                    else -> Color(0xFF8B5CF6)
                                }
                                Row {
                                    Text(text = "RSI: ", color = SubtitleWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = String.format(Locale.US, "%.1f", rsiVal),
                                        color = rsiColor,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (c.signal != null) {
                            val sigColor = if (c.signal.direction == "BUY") CyberNeonGreen else CyberCrimson
                            Box(
                                modifier = Modifier
                                    .background(sigColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .border(1.dp, sigColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${c.signal.direction} ${c.signal.confidence}%",
                                    color = sigColor,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // --- 3. Main Chart Canvas ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(Color(0xFF070B11), RoundedCornerShape(16.dp))
                    .border(1.dp, BorderGray.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(candles, viewMode) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val (candle, signal) = resolveCandleAtOffset(offset.x, size.width.toFloat(), candles)
                                    if (candle != null) {
                                        crosshairState = ChartCrosshairState(
                                            isVisible = true,
                                            xPx = offset.x,
                                            yPx = offset.y,
                                            candle = candle,
                                            price = candle.close,
                                            signal = signal
                                        )
                                        if (signal != null) {
                                            selectedSignalDetail = signal
                                            onSignalClick?.invoke(signal)
                                        }
                                    } else {
                                        crosshairState = crosshairState.copy(isVisible = false)
                                    }
                                }
                            )
                        }
                        .pointerInput(candles) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val (candle, signal) = resolveCandleAtOffset(offset.x, size.width.toFloat(), candles)
                                    if (candle != null) {
                                        crosshairState = ChartCrosshairState(
                                            isVisible = true,
                                            xPx = offset.x,
                                            yPx = offset.y,
                                            candle = candle,
                                            price = candle.close,
                                            signal = signal
                                        )
                                    }
                                },
                                onDragEnd = {
                                    // Keep visible for reading or fade out
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val (candle, signal) = resolveCandleAtOffset(change.position.x, size.width.toFloat(), candles)
                                    if (candle != null) {
                                        crosshairState = ChartCrosshairState(
                                            isVisible = true,
                                            xPx = change.position.x,
                                            yPx = change.position.y,
                                            candle = candle,
                                            price = candle.close,
                                            signal = signal
                                        )
                                    }
                                }
                            )
                        }
                ) {
                    if (candles.isEmpty()) return@Canvas

                    val chartWidth = size.width
                    val chartHeight = size.height
                    val volumeHeight = if (indicatorConfig.showVolume) chartHeight * 0.22f else 0f
                    val pricePlotHeight = chartHeight - volumeHeight - 20f

                    // Calculate price min/max
                    val minPrice = candles.minOfOrNull { it.low } ?: currentPrice
                    val maxPrice = candles.maxOfOrNull { it.high } ?: currentPrice
                    val pricePadding = max((maxPrice - minPrice) * 0.08, 0.5)
                    val plotMinPrice = minPrice - pricePadding
                    val plotMaxPrice = maxPrice + pricePadding
                    val priceRange = max(plotMaxPrice - plotMinPrice, 0.0001)

                    val maxVolume = candles.maxOfOrNull { it.volume } ?: 1.0

                    // 1. Draw Grid Lines and Y-Axis Price Scales
                    drawChartGridAndLabels(
                        width = chartWidth,
                        height = pricePlotHeight,
                        minPrice = plotMinPrice,
                        maxPrice = plotMaxPrice,
                        textMeasurer = textMeasurer
                    )

                    val candleCount = candles.size
                    val colWidth = chartWidth / max(candleCount, 1)

                    // 2. Draw Bollinger Bands if enabled
                    if (indicatorConfig.showBollingerBands && (viewMode == ChartViewMode.INDICATORS || viewMode == ChartViewMode.CANDLESTICK)) {
                        drawBollingerBands(
                            candles = candles,
                            width = chartWidth,
                            height = pricePlotHeight,
                            minPrice = plotMinPrice,
                            priceRange = priceRange
                        )
                    }

                    // 3. Draw Volume Histogram at the bottom
                    if (indicatorConfig.showVolume) {
                        drawVolumeHistogram(
                            candles = candles,
                            width = chartWidth,
                            chartHeight = chartHeight,
                            volumeHeight = volumeHeight,
                            maxVolume = maxVolume
                        )
                    }

                    // 4. Draw Main Series depending on ViewMode
                    when (viewMode) {
                        ChartViewMode.CANDLESTICK, ChartViewMode.SIGNALS_OVERLAY, ChartViewMode.INDICATORS, ChartViewMode.RSI_OSCILLATOR -> {
                            drawCandlestickSeries(
                                candles = candles,
                                colWidth = colWidth,
                                height = pricePlotHeight,
                                minPrice = plotMinPrice,
                                priceRange = priceRange
                            )
                        }
                        ChartViewMode.NEON_LINE -> {
                            drawNeonAreaLine(
                                candles = candles,
                                colWidth = colWidth,
                                height = pricePlotHeight,
                                minPrice = plotMinPrice,
                                priceRange = priceRange,
                                accentColor = accentColor
                            )
                        }
                    }

                    // 5. Draw Exponential Moving Averages (EMA 9 & EMA 21)
                    if (indicatorConfig.showEma9 || viewMode == ChartViewMode.INDICATORS) {
                        drawEMA(
                            candles = candles,
                            period = 9,
                            color = CyberAqua,
                            colWidth = colWidth,
                            height = pricePlotHeight,
                            minPrice = plotMinPrice,
                            priceRange = priceRange
                        )
                        drawEMA(
                            candles = candles,
                            period = 21,
                            color = Color(0xFFF59E0B),
                            colWidth = colWidth,
                            height = pricePlotHeight,
                            minPrice = plotMinPrice,
                            priceRange = priceRange
                        )
                    }

                    // 6. Draw AI-Generated Signal Timestamps & Markers
                    if (indicatorConfig.showSignalMarkers || viewMode == ChartViewMode.SIGNALS_OVERLAY) {
                        drawAiSignalMarkers(
                            candles = candles,
                            colWidth = colWidth,
                            height = pricePlotHeight,
                            minPrice = plotMinPrice,
                            priceRange = priceRange,
                            textMeasurer = textMeasurer,
                            activeSignal = activeSignal
                        )
                    }

                    // 7. Draw Live Current Price Horizontal Bid Line
                    val currentPriceY = (pricePlotHeight - ((currentPrice - plotMinPrice) / priceRange * pricePlotHeight)).toFloat()
                    drawLine(
                        color = CyberAqua,
                        start = Offset(0f, currentPriceY),
                        end = Offset(chartWidth, currentPriceY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    // 8. Draw Crosshair if active
                    if (crosshairState.isVisible) {
                        drawCrosshair(
                            state = crosshairState,
                            width = chartWidth,
                            height = chartHeight
                        )
                    }
                }
            }

            // --- 3.5. Relative Strength Index (RSI 14) Sub-Chart Overlay ---
            if (indicatorConfig.showRsi || viewMode == ChartViewMode.RSI_OSCILLATOR) {
                Spacer(modifier = Modifier.height(10.dp))
                RsiOscillatorSubChart(
                    candles = candles,
                    crosshairState = crosshairState,
                    textMeasurer = textMeasurer,
                    onCrosshairChange = { newCrosshair ->
                        crosshairState = newCrosshair
                    }
                )
            }

            // --- 4. Signal Detail Modal Card when a Signal Timestamp is selected ---
            AnimatedVisibility(
                visible = selectedSignalDetail != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedSignalDetail?.let { sig ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, if (sig.direction == "BUY") CyberNeonGreen else CyberCrimson),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val sigColor = if (sig.direction == "BUY") CyberNeonGreen else CyberCrimson
                                    Box(
                                        modifier = Modifier
                                            .background(sigColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .border(1.dp, sigColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SIGNAL: ${sig.direction}",
                                            color = sigColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sig.strategy,
                                        color = PrimaryWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = { selectedSignalDetail = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close Detail",
                                        tint = SubtitleWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("ENTRY", fontSize = 8.5.sp, color = SubtitleWhite, fontWeight = FontWeight.Bold)
                                    Text(
                                        String.format(Locale.US, "%.2f", sig.entryPrice),
                                        fontSize = 11.sp,
                                        color = PrimaryWhite,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text("STOP LOSS", fontSize = 8.5.sp, color = SubtitleWhite, fontWeight = FontWeight.Bold)
                                    Text(
                                        String.format(Locale.US, "%.2f", sig.stopLoss),
                                        fontSize = 11.sp,
                                        color = CyberCrimson,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text("TAKE PROFIT", fontSize = 8.5.sp, color = SubtitleWhite, fontWeight = FontWeight.Bold)
                                    Text(
                                        String.format(Locale.US, "%.2f", sig.takeProfit),
                                        fontSize = 11.sp,
                                        color = CyberNeonGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text("CONFIDENCE", fontSize = 8.5.sp, color = SubtitleWhite, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${sig.confidence}%",
                                        fontSize = 11.sp,
                                        color = accentColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (sig.analysis.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = sig.analysis,
                                    fontSize = 10.sp,
                                    color = SubtitleWhite,
                                    lineHeight = 13.sp,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Canvas Drawing Helpers ---

private fun DrawScope.drawChartGridAndLabels(
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double,
    textMeasurer: TextMeasurer
) {
    val steps = 4
    for (i in 0..steps) {
        val y = (height / steps) * i
        val price = maxPrice - ((maxPrice - minPrice) / steps) * i

        // Horizontal dashed line
        drawLine(
            color = BorderGray.copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        // Price label text
        val labelText = String.format(Locale.US, "%.2f", price)
        val textLayout = textMeasurer.measure(
            text = labelText,
            style = TextStyle(
                color = SubtitleWhite.copy(alpha = 0.65f),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            )
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(width - textLayout.size.width - 6f, y - textLayout.size.height - 2f)
        )
    }
}

private fun DrawScope.drawCandlestickSeries(
    candles: List<CandleStickData>,
    colWidth: Float,
    height: Float,
    minPrice: Double,
    priceRange: Double
) {
    candles.forEachIndexed { index, candle ->
        val x = (index + 0.5f) * colWidth

        val openY = (height - ((candle.open - minPrice) / priceRange * height)).toFloat()
        val closeY = (height - ((candle.close - minPrice) / priceRange * height)).toFloat()
        val highY = (height - ((candle.high - minPrice) / priceRange * height)).toFloat()
        val lowY = (height - ((candle.low - minPrice) / priceRange * height)).toFloat()

        val isBullish = candle.close >= candle.open
        val candleColor = if (isBullish) CyberNeonGreen else CyberCrimson

        // 1. Draw High-Low Wick
        drawLine(
            color = candleColor,
            start = Offset(x, highY),
            end = Offset(x, lowY),
            strokeWidth = 1.8f
        )

        // 2. Draw Real Body Rect
        val bodyTop = min(openY, closeY)
        val bodyBottom = max(openY, closeY)
        val bodyHeight = max(bodyBottom - bodyTop, 2.5f)
        val bodyWidth = colWidth * 0.65f

        drawRect(
            color = candleColor,
            topLeft = Offset(x - (bodyWidth / 2f), bodyTop),
            size = Size(bodyWidth, bodyHeight)
        )
    }
}

private fun DrawScope.drawNeonAreaLine(
    candles: List<CandleStickData>,
    colWidth: Float,
    height: Float,
    minPrice: Double,
    priceRange: Double,
    accentColor: Color
) {
    if (candles.size < 2) return

    val path = Path()
    val fillPath = Path()

    val points = candles.mapIndexed { index, candle ->
        val x = (index + 0.5f) * colWidth
        val y = (height - ((candle.close - minPrice) / priceRange * height)).toFloat()
        Offset(x, y)
    }

    path.moveTo(points.first().x, points.first().y)
    fillPath.moveTo(points.first().x, height)
    fillPath.lineTo(points.first().x, points.first().y)

    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val controlX = (p0.x + p1.x) / 2f
        path.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
        fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
    }

    fillPath.lineTo(points.last().x, height)
    fillPath.close()

    // 1. Draw neon gradient fill
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.35f),
                accentColor.copy(alpha = 0.05f),
                Color.Transparent
            ),
            startY = 0f,
            endY = height
        )
    )

    // 2. Draw glowing spline line
    drawPath(
        path = path,
        color = accentColor,
        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawVolumeHistogram(
    candles: List<CandleStickData>,
    width: Float,
    chartHeight: Float,
    volumeHeight: Float,
    maxVolume: Double
) {
    val colWidth = width / max(candles.size, 1)
    val baseY = chartHeight

    candles.forEachIndexed { index, candle ->
        val x = (index + 0.5f) * colWidth
        val barHeight = ((candle.volume / max(maxVolume, 1.0)) * volumeHeight).toFloat()
        val isBullish = candle.close >= candle.open
        val barColor = if (isBullish) CyberNeonGreen.copy(alpha = 0.35f) else CyberCrimson.copy(alpha = 0.35f)

        drawRect(
            color = barColor,
            topLeft = Offset(x - (colWidth * 0.35f), baseY - barHeight),
            size = Size(colWidth * 0.7f, barHeight)
        )
    }
}

private fun DrawScope.drawEMA(
    candles: List<CandleStickData>,
    period: Int,
    color: Color,
    colWidth: Float,
    height: Float,
    minPrice: Double,
    priceRange: Double
) {
    if (candles.size < period) return

    val k = 2.0 / (period + 1.0)
    var currentEma = candles.take(period).map { it.close }.average()
    val emaPoints = mutableListOf<Offset>()

    candles.forEachIndexed { index, candle ->
        if (index >= period - 1) {
            currentEma = (candle.close * k) + (currentEma * (1.0 - k))
            val x = (index + 0.5f) * colWidth
            val y = (height - ((currentEma - minPrice) / priceRange * height)).toFloat()
            emaPoints.add(Offset(x, y))
        }
    }

    for (i in 0 until emaPoints.size - 1) {
        drawLine(
            color = color.copy(alpha = 0.85f),
            start = emaPoints[i],
            end = emaPoints[i + 1],
            strokeWidth = 1.8f
        )
    }
}

private fun DrawScope.drawBollingerBands(
    candles: List<CandleStickData>,
    width: Float,
    height: Float,
    minPrice: Double,
    priceRange: Double
) {
    val period = 14
    if (candles.size < period) return

    val upperPath = Path()
    val lowerPath = Path()
    val colWidth = width / candles.size

    val upperPoints = mutableListOf<Offset>()
    val lowerPoints = mutableListOf<Offset>()

    for (i in (period - 1) until candles.size) {
        val window = candles.subList(i - period + 1, i + 1).map { it.close }
        val sma = window.average()
        val stdDev = kotlin.math.sqrt(window.map { (it - sma) * (it - sma) }.average())
        val upper = sma + (2.0 * stdDev)
        val lower = sma - (2.0 * stdDev)

        val x = (i + 0.5f) * colWidth
        val upperY = (height - ((upper - minPrice) / priceRange * height)).toFloat()
        val lowerY = (height - ((lower - minPrice) / priceRange * height)).toFloat()

        upperPoints.add(Offset(x, upperY))
        lowerPoints.add(Offset(x, lowerY))
    }

    if (upperPoints.isNotEmpty()) {
        upperPath.moveTo(upperPoints.first().x, upperPoints.first().y)
        lowerPath.moveTo(lowerPoints.first().x, lowerPoints.first().y)

        for (i in 1 until upperPoints.size) {
            upperPath.lineTo(upperPoints[i].x, upperPoints[i].y)
            lowerPath.lineTo(lowerPoints[i].x, lowerPoints[i].y)
        }

        // Draw upper and lower lines
        drawPath(upperPath, color = Color(0xFF64748B).copy(alpha = 0.5f), style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)))
        drawPath(lowerPath, color = Color(0xFF64748B).copy(alpha = 0.5f), style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)))
    }
}

private fun DrawScope.drawAiSignalMarkers(
    candles: List<CandleStickData>,
    colWidth: Float,
    height: Float,
    minPrice: Double,
    priceRange: Double,
    textMeasurer: TextMeasurer,
    activeSignal: SignalEntity?
) {
    candles.forEachIndexed { index, candle ->
        val sig = candle.signal ?: (if (index == candles.size - 1) activeSignal else null)
        if (sig != null) {
            val x = (index + 0.5f) * colWidth
            val isBuy = sig.direction == "BUY"
            val sigColor = if (isBuy) CyberNeonGreen else CyberCrimson

            val anchorY = if (isBuy) {
                (height - ((candle.low - minPrice) / priceRange * height)).toFloat() + 14f
            } else {
                (height - ((candle.high - minPrice) / priceRange * height)).toFloat() - 14f
            }

            // Draw glowing halo pin
            drawCircle(
                color = sigColor.copy(alpha = 0.25f),
                radius = 12f,
                center = Offset(x, anchorY)
            )
            drawCircle(
                color = sigColor,
                radius = 5f,
                center = Offset(x, anchorY)
            )

            // Draw Flag / Arrow pointer
            val arrowPath = Path().apply {
                if (isBuy) {
                    moveTo(x, anchorY - 4f)
                    lineTo(x - 5f, anchorY + 5f)
                    lineTo(x + 5f, anchorY + 5f)
                } else {
                    moveTo(x, anchorY + 4f)
                    lineTo(x - 5f, anchorY - 5f)
                    lineTo(x + 5f, anchorY - 5f)
                }
                close()
            }
            drawPath(arrowPath, color = sigColor)

            // Draw Target Entry & Stop Loss Envelope lines across chart if latest signal
            if (index >= candles.size - 3) {
                val entryY = (height - ((sig.entryPrice - minPrice) / priceRange * height)).toFloat()
                val slY = (height - ((sig.stopLoss - minPrice) / priceRange * height)).toFloat()
                val tpY = (height - ((sig.takeProfit - minPrice) / priceRange * height)).toFloat()

                // Entry Line
                drawLine(
                    color = CyberAqua.copy(alpha = 0.6f),
                    start = Offset(x, entryY),
                    end = Offset(size.width, entryY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )

                // SL Line
                drawLine(
                    color = CyberCrimson.copy(alpha = 0.6f),
                    start = Offset(x, slY),
                    end = Offset(size.width, slY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )

                // TP Line
                drawLine(
                    color = CyberNeonGreen.copy(alpha = 0.6f),
                    start = Offset(x, tpY),
                    end = Offset(size.width, tpY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )
            }
        }
    }
}

private fun DrawScope.drawCrosshair(
    state: ChartCrosshairState,
    width: Float,
    height: Float
) {
    // Vertical line
    drawLine(
        color = CyberAqua.copy(alpha = 0.65f),
        start = Offset(state.xPx, 0f),
        end = Offset(state.xPx, height),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    )

    // Horizontal line
    drawLine(
        color = CyberAqua.copy(alpha = 0.65f),
        start = Offset(0f, state.yPx),
        end = Offset(width, state.yPx),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    )

    // Crosshair target circle
    drawCircle(
        color = CyberAqua,
        radius = 4f,
        center = Offset(state.xPx, state.yPx)
    )
}

private fun resolveCandleAtOffset(
    xPx: Float,
    totalWidth: Float,
    candles: List<CandleStickData>
): Pair<CandleStickData?, SignalEntity?> {
    if (candles.isEmpty() || totalWidth <= 0f) return Pair(null, null)
    val colWidth = totalWidth / candles.size
    val index = (xPx / colWidth).toInt().coerceIn(0, candles.size - 1)
    val candle = candles.getOrNull(index)
    return Pair(candle, candle?.signal)
}

@Composable
private fun IndicatorToggleChip(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) CyberAqua.copy(alpha = 0.15f) else Color(0xFF161616))
            .border(1.dp, if (checked) CyberAqua.copy(alpha = 0.6f) else BorderGray, RoundedCornerShape(6.dp))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (checked) CyberAqua else Color.Gray)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (checked) CyberAqua else SubtitleWhite,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HudValue(label: String, value: Double) {
    Row {
        Text(text = "$label: ", color = SubtitleWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(
            text = String.format(Locale.US, "%.2f", value),
            color = PrimaryWhite,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RsiOscillatorSubChart(
    candles: List<CandleStickData>,
    crosshairState: ChartCrosshairState,
    textMeasurer: TextMeasurer,
    onCrosshairChange: ((ChartCrosshairState) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val latestRsi = candles.lastOrNull()?.rsi
    val rsiClassification = TechnicalAnalysis.classifyRsiState(latestRsi)

    val badgeColor = when (rsiClassification) {
        RsiClassification.OVERBOUGHT -> CyberCrimson
        RsiClassification.OVERSOLD -> CyberNeonGreen
        RsiClassification.BULLISH_MOMENTUM -> CyberAqua
        RsiClassification.BEARISH_MOMENTUM -> Color(0xFFF59E0B)
        RsiClassification.NEUTRAL -> Color(0xFF8B5CF6)
        RsiClassification.INSUFFICIENT_DATA -> SubtitleWhite
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF070B11))
            .border(1.dp, BorderGray.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(10.dp)
            .testTag("rsi_oscillator_subchart")
    ) {
        // RSI Header: Title, Level badges, Current classification
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "RELATIVE STRENGTH INDEX (RSI 14)",
                    color = PrimaryWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.18f))
                        .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = if (latestRsi != null) "${rsiClassification.label} (${String.format(Locale.US, "%.1f", latestRsi)})" else "CALCULATING",
                        color = badgeColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = "OB 70 / OS 30",
                color = SubtitleWhite.copy(alpha = 0.7f),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // RSI Canvas Sub-Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF1E293B).copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(candles) {
                        detectTapGestures(
                            onTap = { offset ->
                                val (candle, signal) = resolveCandleAtOffset(offset.x, size.width.toFloat(), candles)
                                if (candle != null) {
                                    onCrosshairChange?.invoke(
                                        ChartCrosshairState(
                                            isVisible = true,
                                            xPx = offset.x,
                                            yPx = offset.y,
                                            candle = candle,
                                            price = candle.close,
                                            signal = signal,
                                            rsi = candle.rsi
                                        )
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(candles) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                val (candle, signal) = resolveCandleAtOffset(change.position.x, size.width.toFloat(), candles)
                                if (candle != null) {
                                    onCrosshairChange?.invoke(
                                        ChartCrosshairState(
                                            isVisible = true,
                                            xPx = change.position.x,
                                            yPx = change.position.y,
                                            candle = candle,
                                            price = candle.close,
                                            signal = signal,
                                            rsi = candle.rsi
                                        )
                                    )
                                }
                            }
                        )
                    }
            ) {
                if (candles.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height

                // Draw RSI Scale Levels: 70 (Overbought), 50 (Neutral), 30 (Oversold)
                val y70 = (height * (1.0f - 0.70f))
                val y50 = (height * (1.0f - 0.50f))
                val y30 = (height * (1.0f - 0.30f))

                // 1. Shaded channels
                // Overbought zone (70 to 100)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(CyberCrimson.copy(alpha = 0.18f), CyberCrimson.copy(alpha = 0.04f)),
                        startY = 0f,
                        endY = y70
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(width, y70)
                )

                // Neutral zone (30 to 70)
                drawRect(
                    color = Color(0xFF1E293B).copy(alpha = 0.20f),
                    topLeft = Offset(0f, y70),
                    size = Size(width, y30 - y70)
                )

                // Oversold zone (0 to 30)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(CyberNeonGreen.copy(alpha = 0.04f), CyberNeonGreen.copy(alpha = 0.18f)),
                        startY = y30,
                        endY = height
                    ),
                    topLeft = Offset(0f, y30),
                    size = Size(width, height - y30)
                )

                // 2. Guideline dashed lines
                // 70 Line
                drawLine(
                    color = CyberCrimson.copy(alpha = 0.6f),
                    start = Offset(0f, y70),
                    end = Offset(width, y70),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )
                // 50 Line
                drawLine(
                    color = BorderGray.copy(alpha = 0.4f),
                    start = Offset(0f, y50),
                    end = Offset(width, y50),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 5f), 0f)
                )
                // 30 Line
                drawLine(
                    color = CyberNeonGreen.copy(alpha = 0.6f),
                    start = Offset(0f, y30),
                    end = Offset(width, y30),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )

                // Level labels
                drawText(
                    textMeasurer = textMeasurer,
                    text = "70",
                    style = TextStyle(color = CyberCrimson.copy(alpha = 0.8f), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace),
                    topLeft = Offset(width - 24f, y70 - 12f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "50",
                    style = TextStyle(color = SubtitleWhite.copy(alpha = 0.5f), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace),
                    topLeft = Offset(width - 24f, y50 - 10f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "30",
                    style = TextStyle(color = CyberNeonGreen.copy(alpha = 0.8f), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace),
                    topLeft = Offset(width - 24f, y30 + 2f)
                )

                // 3. Draw RSI Smoothed Spline Curve
                val colWidth = width / max(candles.size, 1)
                val rsiPoints = mutableListOf<Offset>()
                val rsiCandles = mutableListOf<Pair<Int, CandleStickData>>()

                candles.forEachIndexed { index, candle ->
                    val rsiVal = candle.rsi
                    if (rsiVal != null) {
                        val x = (index + 0.5f) * colWidth
                        val y = (height * (1.0f - (rsiVal.toFloat() / 100.0f))).coerceIn(0f, height)
                        rsiPoints.add(Offset(x, y))
                        rsiCandles.add(Pair(index, candle))
                    }
                }

                if (rsiPoints.size >= 2) {
                    val rsiPath = Path()
                    val fillPath = Path()

                    rsiPath.moveTo(rsiPoints.first().x, rsiPoints.first().y)
                    fillPath.moveTo(rsiPoints.first().x, height)
                    fillPath.lineTo(rsiPoints.first().x, rsiPoints.first().y)

                    for (i in 0 until rsiPoints.size - 1) {
                        val p0 = rsiPoints[i]
                        val p1 = rsiPoints[i + 1]
                        val controlX = (p0.x + p1.x) / 2f
                        rsiPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(rsiPoints.last().x, height)
                    fillPath.close()

                    // Gradient fill beneath RSI curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                Color(0xFF8B5CF6).copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Glowing RSI line
                    drawPath(
                        path = rsiPath,
                        color = Color(0xFF8B5CF6),
                        style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // AI Signals Markers correlated on the RSI Curve
                    rsiCandles.forEachIndexed { i, pair ->
                        val (_, candle) = pair
                        val pt = rsiPoints[i]
                        if (candle.signal != null) {
                            val isBuy = candle.signal.direction == "BUY"
                            val sigColor = if (isBuy) CyberNeonGreen else CyberCrimson
                            drawCircle(
                                color = sigColor.copy(alpha = 0.35f),
                                radius = 7f,
                                center = pt
                            )
                            drawCircle(
                                color = sigColor,
                                radius = 3.5f,
                                center = pt
                            )
                        }
                    }

                    // Pulsing Dot on Latest RSI value
                    val lastPt = rsiPoints.last()
                    drawCircle(
                        color = Color(0xFF8B5CF6).copy(alpha = 0.4f),
                        radius = 8f,
                        center = lastPt
                    )
                    drawCircle(
                        color = PrimaryWhite,
                        radius = 3.5f,
                        center = lastPt
                    )
                }

                // 4. Crosshair Tracking on RSI
                if (crosshairState.isVisible) {
                    val candleIdx = (crosshairState.xPx / colWidth).toInt().coerceIn(0, candles.size - 1)
                    val candle = candles.getOrNull(candleIdx)
                    val rsiVal = candle?.rsi
                    if (rsiVal != null) {
                        val crosshairY = (height * (1.0f - (rsiVal.toFloat() / 100.0f))).coerceIn(0f, height)
                        // Vertical guide
                        drawLine(
                            color = CyberAqua.copy(alpha = 0.65f),
                            start = Offset(crosshairState.xPx, 0f),
                            end = Offset(crosshairState.xPx, height),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                        // Marker point
                        drawCircle(
                            color = CyberAqua,
                            radius = 4.5f,
                            center = Offset(crosshairState.xPx, crosshairY)
                        )
                    }
                }
            }
        }
    }
}

