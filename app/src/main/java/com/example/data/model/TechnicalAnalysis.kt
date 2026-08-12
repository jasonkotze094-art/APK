package com.example.data.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Encapsulates the current technical and momentum trend state of a financial asset.
 */
data class MarketTrendSnapshot(
    val symbol: String,
    val timeframe: String,
    val currentPrice: Double,
    val change24h: Double,
    val high24h: Double,
    val low24h: Double,
    val currentRsi: Double?,
    val rsiClassification: RsiClassification,
    val ema14: Double,
    val ema50: Double,
    val isAboveEma14: Boolean,
    val isAboveEma50: Boolean,
    val trendDirection: String, // "UPTREND", "DOWNTREND", "SIDEWAYS"
    val trendStrength: String,  // "STRONG", "MODERATE", "CONSOLIDATION"
    val supportLevel: Double,
    val resistanceLevel: Double,
    val momentumScore: Int,      // 0..100
    val volumeTrend: String      // "ACCUMULATION", "DISTRIBUTION", "AVERAGE"
)

/**
 * Technical analysis utility providing standard market indicators including Relative Strength Index (RSI),
 * Exponential Moving Averages (EMA), Support/Resistance pivot identification, and composite trend snapshots.
 */
object TechnicalAnalysis {

    const val DEFAULT_RSI_PERIOD = 14
    const val DEFAULT_OVERBOUGHT_LEVEL = 70.0
    const val DEFAULT_OVERSOLD_LEVEL = 30.0
    const val DEFAULT_NEUTRAL_LEVEL = 50.0

    /**
     * Calculates Relative Strength Index (RSI) for a list of Candlestick data using Wilder's Smoothed Moving Average method.
     */
    fun calculateRSI(candles: List<CandleStickData>, period: Int = DEFAULT_RSI_PERIOD): List<Double?> {
        if (candles.size < period + 1) {
            return List(candles.size) { null }
        }
        val closes = candles.map { it.close }
        return calculateRsiFromPrices(closes, period)
    }

    /**
     * Core price-based RSI calculation with Wilder's exponential smoothing.
     */
    fun calculateRsiFromPrices(prices: List<Double>, period: Int = DEFAULT_RSI_PERIOD): List<Double?> {
        if (prices.size < period + 1 || period <= 0) {
            return List(prices.size) { null }
        }

        val result = MutableList<Double?>(prices.size) { null }

        val changes = mutableListOf<Double>()
        for (i in 1 until prices.size) {
            changes.add(prices[i] - prices[i - 1])
        }

        var sumGain = 0.0
        var sumLoss = 0.0
        for (i in 0 until period) {
            val change = changes[i]
            if (change > 0) {
                sumGain += change
            } else {
                sumLoss += abs(change)
            }
        }

        var avgGain = sumGain / period
        var avgLoss = sumLoss / period

        val initialRsi = computeRsiFromAverages(avgGain, avgLoss)
        result[period] = roundTwoDecimals(initialRsi)

        for (i in period until changes.size) {
            val change = changes[i]
            val currentGain = if (change > 0) change else 0.0
            val currentLoss = if (change < 0) abs(change) else 0.0

            avgGain = ((avgGain * (period - 1)) + currentGain) / period
            avgLoss = ((avgLoss * (period - 1)) + currentLoss) / period

            val rsi = computeRsiFromAverages(avgGain, avgLoss)
            result[i + 1] = roundTwoDecimals(rsi)
        }

        return result
    }

    /**
     * Calculates Exponential Moving Average (EMA) for a price series.
     */
    fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        if (prices.size <= period) return prices.average()

        val multiplier = 2.0 / (period + 1.0)
        var ema = prices.take(period).average()

        for (i in period until prices.size) {
            ema = (prices[i] - ema) * multiplier + ema
        }
        return roundTwoDecimals(ema)
    }

    /**
     * Identifies dynamic structural support and resistance levels from candlestick pivots.
     */
    fun calculatePivotLevels(candles: List<CandleStickData>, fallbackPrice: Double): Pair<Double, Double> {
        if (candles.isEmpty()) {
            return Pair(roundTwoDecimals(fallbackPrice * 0.995), roundTwoDecimals(fallbackPrice * 1.005))
        }
        val lows = candles.map { it.low }
        val highs = candles.map { it.high }

        val support = lows.minOrNull() ?: (fallbackPrice * 0.995)
        val resistance = highs.maxOrNull() ?: (fallbackPrice * 1.005)

        return Pair(roundTwoDecimals(support), roundTwoDecimals(resistance))
    }

    /**
     * Builds a comprehensive market trend snapshot by combining live price metrics,
     * RSI, EMA crossovers, and structural price pivots.
     */
    fun buildMarketTrendSnapshot(
        symbol: String,
        timeframe: String,
        currentPrice: Double,
        change24h: Double,
        high24h: Double,
        low24h: Double,
        candles: List<CandleStickData>
    ): MarketTrendSnapshot {
        val closes = if (candles.isNotEmpty()) candles.map { it.close } else listOf(currentPrice)
        val rsiSeries = calculateRsiFromPrices(closes, DEFAULT_RSI_PERIOD)
        val currentRsi = rsiSeries.lastOrNull() ?: (if (change24h >= 0) 58.0 else 44.0)
        val rsiClassification = classifyRsiState(currentRsi)

        val ema14 = calculateEMA(closes, 14).let { if (it == 0.0) currentPrice * 0.998 else it }
        val ema50 = calculateEMA(closes, 50).let { if (it == 0.0) currentPrice * 0.995 else it }

        val isAboveEma14 = currentPrice >= ema14
        val isAboveEma50 = currentPrice >= ema50

        val (support, resistance) = calculatePivotLevels(candles, currentPrice)

        val trendDirection = when {
            isAboveEma14 && isAboveEma50 && change24h > 0.1 -> "UPTREND"
            !isAboveEma14 && !isAboveEma50 && change24h < -0.1 -> "DOWNTREND"
            else -> "SIDEWAYS"
        }

        val trendStrength = when {
            abs(change24h) > 2.0 || (currentRsi >= 65 || currentRsi <= 35) -> "STRONG"
            abs(change24h) > 0.5 -> "MODERATE"
            else -> "CONSOLIDATION"
        }

        val momentumScore = when (trendDirection) {
            "UPTREND" -> ((currentRsi ?: 60.0) * 0.6 + 40).toInt().coerceIn(60, 98)
            "DOWNTREND" -> ((100 - (currentRsi ?: 40.0)) * 0.6 + 40).toInt().coerceIn(60, 98)
            else -> 50
        }

        val volumeTrend = if (change24h >= 0.5) "ACCUMULATION" else if (change24h <= -0.5) "DISTRIBUTION" else "AVERAGE"

        return MarketTrendSnapshot(
            symbol = symbol,
            timeframe = timeframe,
            currentPrice = currentPrice,
            change24h = change24h,
            high24h = if (high24h > 0) high24h else currentPrice * 1.01,
            low24h = if (low24h > 0) low24h else currentPrice * 0.99,
            currentRsi = currentRsi,
            rsiClassification = rsiClassification,
            ema14 = ema14,
            ema50 = ema50,
            isAboveEma14 = isAboveEma14,
            isAboveEma50 = isAboveEma50,
            trendDirection = trendDirection,
            trendStrength = trendStrength,
            supportLevel = support,
            resistanceLevel = resistance,
            momentumScore = momentumScore,
            volumeTrend = volumeTrend
        )
    }

    /**
     * Computes RSI from average gain and average loss.
     */
    private fun computeRsiFromAverages(avgGain: Double, avgLoss: Double): Double {
        if (avgLoss == 0.0) {
            return if (avgGain == 0.0) 50.0 else 100.0
        }
        if (avgGain == 0.0) {
            return 0.0
        }
        val rs = avgGain / avgLoss
        val rsi = 100.0 - (100.0 / (1.0 + rs))
        return rsi.coerceIn(0.0, 100.0)
    }

    /**
     * Attaches computed RSI values directly to the Candlestick data objects.
     */
    fun enrichCandlesWithRsi(candles: List<CandleStickData>, period: Int = DEFAULT_RSI_PERIOD): List<CandleStickData> {
        val rsiSeries = calculateRSI(candles, period)
        return candles.mapIndexed { index, candle ->
            candle.copy(rsi = rsiSeries.getOrNull(index))
        }
    }

    /**
     * Categorizes the RSI into market state descriptions.
     */
    fun classifyRsiState(
        rsi: Double?,
        overboughtLevel: Double = DEFAULT_OVERBOUGHT_LEVEL,
        oversoldLevel: Double = DEFAULT_OVERSOLD_LEVEL
    ): RsiClassification {
        if (rsi == null) return RsiClassification.INSUFFICIENT_DATA
        return when {
            rsi >= overboughtLevel -> RsiClassification.OVERBOUGHT
            rsi <= oversoldLevel -> RsiClassification.OVERSOLD
            rsi >= 55.0 -> RsiClassification.BULLISH_MOMENTUM
            rsi <= 45.0 -> RsiClassification.BEARISH_MOMENTUM
            else -> RsiClassification.NEUTRAL
        }
    }

    private fun roundTwoDecimals(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }
}

enum class RsiClassification(val label: String, val description: String) {
    OVERBOUGHT("OVERBOUGHT", "RSI >= 70 | Potential Bearish Mean Reversion"),
    OVERSOLD("OVERSOLD", "RSI <= 30 | Potential Bullish Rebound Zone"),
    BULLISH_MOMENTUM("BULLISH", "55 <= RSI < 70 | Strong Upward Momentum"),
    BEARISH_MOMENTUM("BEARISH", "30 < RSI <= 45 | Downward Pressure Dominates"),
    NEUTRAL("NEUTRAL", "45 < RSI < 55 | Equilibrium Zone"),
    INSUFFICIENT_DATA("CALCULATING", "Awaiting historical periods")
}
