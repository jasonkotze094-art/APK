package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.data.database.SignalEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class CandleStickData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val timeLabel: String,
    val signal: SignalEntity? = null,
    val rsi: Double? = null
) {
    val isBullish: Boolean get() = close >= open
    val bodyHeight: Double get() = abs(close - open)
    val totalRange: Double get() = max(0.0, high - low)
    val upperWickHeight: Double get() = high - max(open, close)
    val lowerWickHeight: Double get() = min(open, close) - low
}

enum class ChartViewMode(val displayName: String) {
    CANDLESTICK("Candles"),
    NEON_LINE("Neon Area"),
    SIGNALS_OVERLAY("AI Signals"),
    INDICATORS("EMA & Bands"),
    RSI_OSCILLATOR("RSI 14")
}

data class ChartIndicatorConfig(
    val showEma9: Boolean = true,
    val showEma21: Boolean = true,
    val showSma50: Boolean = false,
    val showBollingerBands: Boolean = true,
    val showVolume: Boolean = true,
    val showSignalMarkers: Boolean = true,
    val showTargetZones: Boolean = true,
    val showRsi: Boolean = true,
    val rsiPeriod: Int = 14,
    val rsiOverboughtLevel: Double = 70.0,
    val rsiOversoldLevel: Double = 30.0,
    val showEMA14: Boolean = true,
    val showEMA50: Boolean = true,
    val showVolumeProfile: Boolean = true,
    val showBidAskSpread: Boolean = true,
    val showGridLines: Boolean = true,
    val showCrosshair: Boolean = true,
    val emaPeriodFast: Int = 14,
    val emaPeriodSlow: Int = 50
)

data class ChartCrosshairState(
    val isVisible: Boolean = false,
    val xPx: Float = 0f,
    val yPx: Float = 0f,
    val candle: CandleStickData? = null,
    val price: Double = 0.0,
    val signal: SignalEntity? = null,
    val rsi: Double? = candle?.rsi,
    val isActive: Boolean = isVisible,
    val x: Float = xPx,
    val y: Float = yPx,
    val candleIndex: Int = 0,
    val priceValue: Double = price,
    val timeLabel: String = candle?.timeLabel ?: ""
)

