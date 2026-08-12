package com.example.data.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Technical analysis utility providing standard market indicators including Relative Strength Index (RSI).
 */
object TechnicalAnalysis {

    const val DEFAULT_RSI_PERIOD = 14
    const val DEFAULT_OVERBOUGHT_LEVEL = 70.0
    const val DEFAULT_OVERSOLD_LEVEL = 30.0
    const val DEFAULT_NEUTRAL_LEVEL = 50.0

    /**
     * Calculates Relative Strength Index (RSI) for a list of Candlestick data using Wilder's Smoothed Moving Average method.
     *
     * @param candles The historical candlestick data points in chronological order.
     * @param period The lookback period (default 14).
     * @return List of nullable Double corresponding 1-to-1 with input candles (null for indices < period).
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
     *
     * @param prices Sequential closing prices.
     * @param period The lookback period (default 14).
     * @return List of RSI values bounded [0.0, 100.0], matching input indices.
     */
    fun calculateRsiFromPrices(prices: List<Double>, period: Int = DEFAULT_RSI_PERIOD): List<Double?> {
        if (prices.size < period + 1 || period <= 0) {
            return List(prices.size) { null }
        }

        val result = MutableList<Double?>(prices.size) { null }

        // 1. Calculate price deltas
        val changes = mutableListOf<Double>()
        for (i in 1 until prices.size) {
            changes.add(prices[i] - prices[i - 1])
        }

        // 2. Initial average gain/loss over first `period` samples
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

        // Initial RSI at index `period`
        val initialRsi = computeRsiFromAverages(avgGain, avgLoss)
        result[period] = roundTwoDecimals(initialRsi)

        // 3. Smoothed Moving Average (Wilder's Smoothing) for all subsequent candles
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
     * Categorizes the RSI into market state descriptions (Overbought, Oversold, Bullish/Bearish Momentum, Neutral).
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
