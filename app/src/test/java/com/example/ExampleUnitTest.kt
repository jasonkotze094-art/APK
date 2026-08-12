package com.example

import com.example.data.database.SignalEntity
import com.example.data.model.CandleStickData
import com.example.data.model.ChartCrosshairState
import com.example.data.model.ChartIndicatorConfig
import com.example.data.model.ChartViewMode
import com.example.data.model.RsiClassification
import com.example.data.model.TechnicalAnalysis
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCandleStickDataCalculations() {
    val signal = SignalEntity(
      symbol = "XAUUSD",
      direction = "BUY",
      confidence = 94,
      entryPrice = 2345.50,
      stopLoss = 2335.00,
      takeProfit = 2365.00,
      strategy = "Scalp EMA Pullback",
      timeframe = "M5",
      analysis = "Bullish momentum with RSI support and EMA golden cross",
      groundingQueries = "XAUUSD gold price action, Fed rate cut expectations"
    )

    val candle = CandleStickData(
      timestamp = 1700000000000L,
      open = 2340.0,
      high = 2350.0,
      low = 2338.0,
      close = 2348.0,
      volume = 120.5,
      timeLabel = "14:30",
      signal = signal,
      rsi = 62.4
    )

    assertTrue("Candle should be bullish/green", candle.isBullish)
    assertEquals(8.0, candle.bodyHeight, 0.001)
    assertEquals(12.0, candle.totalRange, 0.001)
    assertEquals(2.0, candle.upperWickHeight, 0.001)
    assertEquals(2.0, candle.lowerWickHeight, 0.001)
    assertNotNull(candle.signal)
    assertEquals("BUY", candle.signal?.direction)
    assertEquals(94, candle.signal?.confidence)
    assertEquals(62.4, candle.rsi ?: 0.0, 0.001)
  }

  @Test
  fun testChartIndicatorConfigDefaults() {
    val config = ChartIndicatorConfig()
    assertTrue(config.showEma9)
    assertTrue(config.showEma21)
    assertTrue(config.showBollingerBands)
    assertTrue(config.showVolume)
    assertTrue(config.showSignalMarkers)
    assertTrue(config.showRsi)
    assertEquals(14, config.rsiPeriod)
    assertEquals(70.0, config.rsiOverboughtLevel, 0.001)
    assertEquals(30.0, config.rsiOversoldLevel, 0.001)
  }

  @Test
  fun testChartViewModes() {
    val modes = ChartViewMode.values()
    assertTrue(modes.size >= 4)
    assertTrue(modes.contains(ChartViewMode.CANDLESTICK))
    assertTrue(modes.contains(ChartViewMode.NEON_LINE))
    assertTrue(modes.contains(ChartViewMode.SIGNALS_OVERLAY))
    assertTrue(modes.contains(ChartViewMode.INDICATORS))
    assertTrue(modes.contains(ChartViewMode.RSI_OSCILLATOR))
  }

  @Test
  fun testChartCrosshairState() {
    val crosshair = ChartCrosshairState(
      isVisible = true,
      xPx = 150f,
      yPx = 200f,
      price = 2345.80,
      rsi = 58.2
    )
    assertTrue(crosshair.isVisible)
    assertTrue(crosshair.isActive)
    assertEquals(150f, crosshair.xPx, 0.01f)
    assertEquals(200f, crosshair.yPx, 0.01f)
    assertEquals(2345.80, crosshair.price, 0.001)
    assertEquals(58.2, crosshair.rsi ?: 0.0, 0.001)
  }

  @Test
  fun testRsiCalculation_UpwardTrend() {
    // 15 strictly rising prices: RSI should be 100 or close to 100
    val risingPrices = listOf(
      10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0,
      18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0, 25.0
    )
    val rsiSeries = TechnicalAnalysis.calculateRsiFromPrices(risingPrices, period = 14)
    assertEquals(risingPrices.size, rsiSeries.size)
    assertNull(rsiSeries[0])
    assertNull(rsiSeries[13])
    assertNotNull(rsiSeries[14])
    assertEquals(100.0, rsiSeries[14]!!, 0.01)
    assertEquals(100.0, rsiSeries[15]!!, 0.01)
  }

  @Test
  fun testRsiCalculation_DownwardTrend() {
    // 15 strictly falling prices: RSI should be 0.0
    val fallingPrices = listOf(
      50.0, 48.0, 46.0, 44.0, 42.0, 40.0, 38.0, 36.0,
      34.0, 32.0, 30.0, 28.0, 26.0, 24.0, 22.0, 20.0
    )
    val rsiSeries = TechnicalAnalysis.calculateRsiFromPrices(fallingPrices, period = 14)
    assertEquals(fallingPrices.size, rsiSeries.size)
    assertNotNull(rsiSeries[14])
    assertEquals(0.0, rsiSeries[14]!!, 0.01)
  }

  @Test
  fun testRsiClassification() {
    assertEquals(RsiClassification.OVERBOUGHT, TechnicalAnalysis.classifyRsiState(75.0))
    assertEquals(RsiClassification.OVERBOUGHT, TechnicalAnalysis.classifyRsiState(70.0))
    assertEquals(RsiClassification.OVERSOLD, TechnicalAnalysis.classifyRsiState(25.0))
    assertEquals(RsiClassification.OVERSOLD, TechnicalAnalysis.classifyRsiState(30.0))
    assertEquals(RsiClassification.BULLISH_MOMENTUM, TechnicalAnalysis.classifyRsiState(60.0))
    assertEquals(RsiClassification.BEARISH_MOMENTUM, TechnicalAnalysis.classifyRsiState(40.0))
    assertEquals(RsiClassification.NEUTRAL, TechnicalAnalysis.classifyRsiState(50.0))
    assertEquals(RsiClassification.INSUFFICIENT_DATA, TechnicalAnalysis.classifyRsiState(null))
  }

  @Test
  fun testTechnicalAnalysisEmaAndPivots() {
    val prices = listOf(10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0)
    val ema = TechnicalAnalysis.calculateEMA(prices, 5)
    assertTrue("EMA should be positive", ema > 10.0)

    val candles = (1..10).map { i ->
      CandleStickData(
        timestamp = 1700000000000L + (i * 60000L),
        open = 100.0 + i,
        high = 110.0 + i,
        low = 90.0 + i,
        close = 105.0 + i,
        volume = 50.0,
        timeLabel = "10:$i"
      )
    }
    val (support, resistance) = TechnicalAnalysis.calculatePivotLevels(candles, 100.0)
    assertEquals(91.0, support, 0.01)
    assertEquals(120.0, resistance, 0.01)
  }

  @Test
  fun testMarketTrendSnapshotBuilder() {
    val candles = (1..20).map { i ->
      CandleStickData(
        timestamp = 1700000000000L + (i * 60000L),
        open = 100.0 + i,
        high = 105.0 + i,
        low = 98.0 + i,
        close = 102.0 + i,
        volume = 50.0,
        timeLabel = "10:$i"
      )
    }

    val snapshot = TechnicalAnalysis.buildMarketTrendSnapshot(
      symbol = "XAUUSD",
      timeframe = "M5",
      currentPrice = 2345.50,
      change24h = 1.45,
      high24h = 2355.0,
      low24h = 2330.0,
      candles = candles
    )

    assertEquals("XAUUSD", snapshot.symbol)
    assertEquals("M5", snapshot.timeframe)
    assertEquals(2345.50, snapshot.currentPrice, 0.001)
    assertNotNull(snapshot.currentRsi)
    assertTrue(snapshot.momentumScore in 0..100)
    assertTrue(snapshot.trendDirection.isNotEmpty())
  }
}


