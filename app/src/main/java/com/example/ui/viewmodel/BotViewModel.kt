package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.BotSettings
import com.example.data.database.SignalEntity
import com.example.data.database.TradePosition
import com.example.data.database.MarketNewsReport
import com.example.data.model.CandleStickData
import com.example.data.model.GeminiMarketIntelligence
import com.example.data.model.MarketCitation
import com.example.data.model.MarketDataFlowState
import com.example.data.model.MarketTrendSnapshot
import com.example.data.model.RsiClassification
import com.example.data.model.TechnicalAnalysis
import com.example.data.repository.BotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

enum class ActiveScreen {
    HOME,
    METATRADER,
    NEWS,
    SETTINGS
}

data class SymbolQuote(
    val symbol: String,
    val price: Double,
    val decimalPlaces: Int = 2,
    val pipsPerUnit: Double = 100.0,
    val name: String = "",
    val change24h: Double = 0.0,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val category: String = "Forex",
    val spreadPips: Double = 1.0,
    val volume24h: String = "$1.5B",
    val sparkline: List<Double> = emptyList(),
    val lastTickDirection: Int = 0 // +1 up, -1 down, 0 neutral
)

class BotViewModel(application: Application) : AndroidViewModel(application) {

    private val botDao = AppDatabase.getDatabase(application).botDao()
    private val repository = BotRepository(botDao)

    // Current app state flows
    var currentScreen by mutableStateOf(ActiveScreen.HOME)
    var selectedSymbol by mutableStateOf("XAUUSD")
    var selectedTimeframe by mutableStateOf("M5")

    // Dynamic Live Price Map
    val symbolQuotes = MutableStateFlow(
        listOf(
            SymbolQuote(
                symbol = "XAUUSD",
                name = "Gold Spot / US Dollar",
                price = 2345.50,
                decimalPlaces = 2,
                pipsPerUnit = 100.0,
                change24h = 1.42,
                high24h = 2358.80,
                low24h = 2338.20,
                category = "Commodities",
                spreadPips = 1.2,
                volume24h = "$45.2B",
                sparkline = listOf(2340.1, 2341.5, 2339.8, 2342.3, 2344.0, 2343.2, 2345.0, 2344.8, 2346.1, 2345.50)
            ),
            SymbolQuote(
                symbol = "BTCUSD",
                name = "Bitcoin / USD",
                price = 67840.0,
                decimalPlaces = 1,
                pipsPerUnit = 1.0,
                change24h = 2.85,
                high24h = 68450.0,
                low24h = 66200.0,
                category = "Crypto",
                spreadPips = 2.5,
                volume24h = "$28.4B",
                sparkline = listOf(66500.0, 66800.0, 66400.0, 67100.0, 67400.0, 67250.0, 67600.0, 67840.0)
            ),
            SymbolQuote(
                symbol = "EURUSD",
                name = "Euro / US Dollar",
                price = 1.0854,
                decimalPlaces = 4,
                pipsPerUnit = 10000.0,
                change24h = -0.34,
                high24h = 1.0892,
                low24h = 1.0831,
                category = "Forex",
                spreadPips = 0.6,
                volume24h = "$110.5B",
                sparkline = listOf(1.0880, 1.0875, 1.0868, 1.0872, 1.0860, 1.0858, 1.0850, 1.0854)
            ),
            SymbolQuote(
                symbol = "GBPUSD",
                name = "British Pound / USD",
                price = 1.2725,
                decimalPlaces = 4,
                pipsPerUnit = 10000.0,
                change24h = 0.18,
                high24h = 1.2760,
                low24h = 1.2690,
                category = "Forex",
                spreadPips = 0.9,
                volume24h = "$75.3B",
                sparkline = listOf(1.2700, 1.2710, 1.2705, 1.2718, 1.2720, 1.2715, 1.2722, 1.2725)
            ),
            SymbolQuote(
                symbol = "TSLA",
                name = "Tesla Inc",
                price = 184.20,
                decimalPlaces = 2,
                pipsPerUnit = 100.0,
                change24h = -1.15,
                high24h = 188.50,
                low24h = 182.10,
                category = "Stocks",
                spreadPips = 1.5,
                volume24h = "$14.8B",
                sparkline = listOf(187.0, 186.2, 185.5, 186.0, 184.8, 183.9, 184.5, 184.20)
            ),
            SymbolQuote(
                symbol = "NVDA",
                name = "Nvidia Corp",
                price = 126.80,
                decimalPlaces = 2,
                pipsPerUnit = 100.0,
                change24h = 3.42,
                high24h = 128.40,
                low24h = 123.10,
                category = "Stocks",
                spreadPips = 1.0,
                volume24h = "$38.9B",
                sparkline = listOf(122.5, 123.8, 124.2, 125.0, 125.8, 126.2, 126.80)
            ),
            SymbolQuote(
                symbol = "ETHUSD",
                name = "Ethereum / USD",
                price = 3450.0,
                decimalPlaces = 1,
                pipsPerUnit = 1.0,
                change24h = 1.95,
                high24h = 3510.0,
                low24h = 3380.0,
                category = "Crypto",
                spreadPips = 1.8,
                volume24h = "$16.2B",
                sparkline = listOf(3390.0, 3410.0, 3400.0, 3430.0, 3445.0, 3450.0)
            ),
            SymbolQuote(
                symbol = "SOLUSD",
                name = "Solana / USD",
                price = 148.50,
                decimalPlaces = 2,
                pipsPerUnit = 10.0,
                change24h = 4.12,
                high24h = 152.30,
                low24h = 142.80,
                category = "Crypto",
                spreadPips = 2.0,
                volume24h = "$8.4B",
                sparkline = listOf(142.0, 144.5, 143.8, 146.2, 147.9, 148.50)
            )
        )
    )

    // Settings State
    val settingsState: StateFlow<BotSettings> = repository.settings
        .combine(MutableStateFlow(BotSettings())) { dbSettings, fallback ->
            dbSettings ?: fallback
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BotSettings())

    // Signal state for active selected symbol
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    // Retrieve active signals reactively
    val allSignals: StateFlow<List<SignalEntity>> = repository.allSignals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time market data & Gemini web-grounded search results state flows
    private val _marketIntelligenceState = MutableStateFlow<MarketDataFlowState>(MarketDataFlowState.Idle)
    val marketIntelligenceState: StateFlow<MarketDataFlowState> = _marketIntelligenceState.asStateFlow()

    private val _marketDataBySymbol = MutableStateFlow<Map<String, GeminiMarketIntelligence>>(emptyMap())
    val marketDataBySymbol: StateFlow<Map<String, GeminiMarketIntelligence>> = _marketDataBySymbol.asStateFlow()

    private val _isLiveStreamingIntelligence = MutableStateFlow(false)
    val isLiveStreamingIntelligence: StateFlow<Boolean> = _isLiveStreamingIntelligence.asStateFlow()

    // All historic positions
    val allPositions: StateFlow<List<TradePosition>> = repository.allPositions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Candlestick Series for lightweight charting
    private val _candlestickSeries = MutableStateFlow<List<CandleStickData>>(emptyList())
    val candlestickSeries: StateFlow<List<CandleStickData>> = _candlestickSeries.asStateFlow()

    // RSI Series and Current RSI Value StateFlows
    private val _rsiSeries = MutableStateFlow<List<Double?>>(emptyList())
    val rsiSeries: StateFlow<List<Double?>> = _rsiSeries.asStateFlow()

    private val _currentRsi = MutableStateFlow<Double?>(null)
    val currentRsi: StateFlow<Double?> = _currentRsi.asStateFlow()

    // Computed Real-Time Market Trend Snapshot StateFlow
    private val _activeTrendSnapshot = MutableStateFlow<MarketTrendSnapshot?>(null)
    val activeTrendSnapshot: StateFlow<MarketTrendSnapshot?> = _activeTrendSnapshot.asStateFlow()

    // News & Economic reports flows
    val allNewsReports: StateFlow<List<MarketNewsReport>> = repository.allNewsReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isFetchingNews = MutableStateFlow(false)
    val isFetchingNews = _isFetchingNews.asStateFlow()

    // Helper UI indicators
    private val _systemLogs = MutableStateFlow<List<String>>(
        listOf("[+] THE CLOWN BEAST scanner engine initialized. Ready to trade.")
    )
    val systemLogs = _systemLogs.asStateFlow()

    init {
        // Prepare default settings and initial signals if empty
        viewModelScope.launch {
            val currentSettings = repository.getSettingsDirect()
            selectedSymbol = currentSettings.selectedSymbol
            selectedTimeframe = currentSettings.selectedTimeframe
            
            // Seed initial rich signals if none exist
            seedInitialSignalsIfEmpty()

            // Start the live market ticker loop to alter prices slightly
            startLiveTickerLoop()

            // Fetch real-time market data and web-grounded search results for default selected pair
            fetchRealtimeMarketDataForSymbol(selectedSymbol)

            // Initialize historical candles for selected symbol and timeframe
            rebuildCandlestickSeries(selectedSymbol, selectedTimeframe)
        }

        // Listen for new signal records to attach to candlestick series
        viewModelScope.launch {
            allSignals.collect { signals ->
                updateCandlesWithSignals(signals)
            }
        }
    }

    private suspend fun seedInitialSignalsIfEmpty() {
        val existing = botDao.getSignalForSymbolDirect("XAUUSD")
        if (existing == null) {
            val defaultSeedSignals = listOf(
                SignalEntity(
                    symbol = "XAUUSD",
                    direction = "BUY",
                    entryPrice = 2345.50,
                    stopLoss = 2335.00,
                    takeProfit = 2365.00,
                    strategy = "Liquidity Sweep Rebound",
                    timeframe = "M5",
                    confidence = 88,
                    analysis = "Price swept previous session lows at $2,338 and printed a strong bullish pinbar on M5. 14-period EMA crossover indicates ascending momentum targeting the next liquidity pool at $2,365.",
                    groundingQueries = "gold prices today, spot XAU USD technical analysis",
                    groundingSources = "Spot Gold Prices and Technical Forecasts - DailyFX|||https://www.dailyfx.com/gold-price\nFederal Reserve Interest Rate Path & Yield Curve - Bloomberg|||https://www.bloomberg.com/markets",
                    timestamp = System.currentTimeMillis() - 120_000
                ),
                SignalEntity(
                    symbol = "BTCUSD",
                    direction = "BUY",
                    entryPrice = 67840.0,
                    stopLoss = 66500.0,
                    takeProfit = 70200.0,
                    strategy = "Halving Inflow Momentum",
                    timeframe = "M5",
                    confidence = 92,
                    analysis = "Institutional ETF net inflows spiked over the last 4-hour window. BTC reclaimed the 50-EMA support level with elevated volume confirmation, projecting expansion toward $70,200.",
                    groundingQueries = "bitcoin ETF inflows, BTC USD news today",
                    groundingSources = "Bitcoin ETF Net Inflows & Crypto Regulations - Bloomberg|||https://www.bloomberg.com/crypto\nCrypto Fear & Greed Index Market Sentiment - Alternative.me|||https://alternative.me/crypto/fear-and-greed-index",
                    timestamp = System.currentTimeMillis() - 300_000
                ),
                SignalEntity(
                    symbol = "EURUSD",
                    direction = "SELL",
                    entryPrice = 1.0854,
                    stopLoss = 1.0890,
                    takeProfit = 1.0780,
                    strategy = "ECB Dovish Rejection",
                    timeframe = "M5",
                    confidence = 78,
                    analysis = "Eurozone core inflation prints reinforced expectations of ECB rate cuts, while the US Dollar Index (DXY) staged a rebound above 104.20. EUR/USD rejected the overhead 1.0890 resistance.",
                    groundingQueries = "EUR USD exchange rate news, ECB rate outlook",
                    groundingSources = "ECB Interest Rate Decisions & Eurozone Inflation - Reuters|||https://www.reuters.com/markets/currencies\nEUR/USD Price Analysis: Cable Technical Resistance - FXStreet|||https://www.fxstreet.com/currencies/eurusd",
                    timestamp = System.currentTimeMillis() - 480_000
                ),
                SignalEntity(
                    symbol = "GBPUSD",
                    direction = "BUY",
                    entryPrice = 1.2725,
                    stopLoss = 1.2680,
                    takeProfit = 1.2810,
                    strategy = "Cable Support Rebound",
                    timeframe = "M5",
                    confidence = 82,
                    analysis = "UK GDP growth showed resilience against macro forecasts. Cable formed a double-bottom structure around 1.2690 with RSI printing hidden bullish divergence on M5.",
                    groundingQueries = "GBP USD cable news, Bank of England rate policy",
                    groundingSources = "GBP/USD News: Cable Structural MA & Resistance Levels - FXStreet|||https://www.fxstreet.com/currencies/gbpusd\nUK Economic GDP Growth Data - Office for National Statistics|||https://www.ons.gov.uk",
                    timestamp = System.currentTimeMillis() - 600_000
                ),
                SignalEntity(
                    symbol = "TSLA",
                    direction = "SELL",
                    entryPrice = 184.20,
                    stopLoss = 189.00,
                    takeProfit = 175.00,
                    strategy = "Descending Channel Test",
                    timeframe = "M5",
                    confidence = 74,
                    analysis = "Tesla met persistent supply at the descending channel upper trendline. Profit taking ahead of delivery disclosures keeps near-term price action constrained below $185.00.",
                    groundingQueries = "tesla stock news today, TSLA technical breakdown",
                    groundingSources = "Tesla Delivery Counts & Quarterly Earnings Outlook - CNBC|||https://www.cnbc.com/tesla\nNasdaq-100 index (NDX) Technical Strength & Growth Capital|||https://www.nasdaq.com",
                    timestamp = System.currentTimeMillis() - 900_000
                ),
                SignalEntity(
                    symbol = "NVDA",
                    direction = "BUY",
                    entryPrice = 126.80,
                    stopLoss = 122.50,
                    takeProfit = 134.00,
                    strategy = "AI Demand Surge Expansion",
                    timeframe = "M5",
                    confidence = 94,
                    analysis = "Datacenter revenue guidance and sustained cloud accelerator orders continue to fuel upward momentum. Bullish consolidation flag formed at $126 with volume rising.",
                    groundingQueries = "Nvidia stock news, semiconductor market forecast",
                    groundingSources = "Nasdaq-100 index (NDX) Technical Strength & Growth Capital|||https://www.nasdaq.com\nWorld Stock Indices Technical Pivot Levels Today|||https://www.bloomberg.com",
                    timestamp = System.currentTimeMillis() - 150_000
                ),
                SignalEntity(
                    symbol = "ETHUSD",
                    direction = "BUY",
                    entryPrice = 3450.0,
                    stopLoss = 3380.0,
                    takeProfit = 3580.0,
                    strategy = "L2 Staking Inflow Breakout",
                    timeframe = "M5",
                    confidence = 85,
                    analysis = "Layer 2 total value locked crossed key milestones. ETH defended the $3,400 psychological support with increasing buy-side liquidity on decentralized order books.",
                    groundingQueries = "ethereum ETF inflows, ETH USD technical trend",
                    groundingSources = "Bitcoin ETF Net Inflows & Crypto Regulations - Bloomberg|||https://www.bloomberg.com/crypto",
                    timestamp = System.currentTimeMillis() - 400_000
                ),
                SignalEntity(
                    symbol = "SOLUSD",
                    direction = "BUY",
                    entryPrice = 148.50,
                    stopLoss = 142.00,
                    takeProfit = 160.00,
                    strategy = "DEX Volume Expansion",
                    timeframe = "M5",
                    confidence = 89,
                    analysis = "Solana network transaction velocity reached multi-week highs. Price broke out of a multi-day ascending triangle pattern with strong target projections toward $160.",
                    groundingQueries = "solana ecosystem growth, SOL price breakout",
                    groundingSources = "Crypto Fear & Greed Index Market Sentiment - Alternative.me|||https://alternative.me/crypto/fear-and-greed-index",
                    timestamp = System.currentTimeMillis() - 250_000
                )
            )

            for (sig in defaultSeedSignals) {
                repository.saveSignal(sig)
            }
        }
    }

    fun rebuildCandlestickSeries(symbol: String, timeframe: String) {
        val currentQuote = symbolQuotes.value.find { it.symbol == symbol }
        val basePrice = currentQuote?.price ?: 2345.50
        val signals = allSignals.value.filter { it.symbol == symbol }
        val generatedCandles = generateHistoricalCandles(symbol, timeframe, basePrice, signals)
        _candlestickSeries.value = generatedCandles
    }

    private fun generateHistoricalCandles(
        symbol: String,
        timeframe: String,
        basePrice: Double,
        signals: List<SignalEntity>
    ): List<CandleStickData> {
        val count = 24
        val result = mutableListOf<CandleStickData>()
        val intervalMinutes = when (timeframe) {
            "M1" -> 1
            "M5" -> 5
            "M15" -> 15
            "M30" -> 30
            "H1" -> 60
            else -> 5
        }
        val intervalMs = intervalMinutes * 60 * 1000L
        val now = System.currentTimeMillis()

        var currentClose = basePrice * (1.0 - (count * 0.0015))
        val volatility = when (symbol) {
            "BTCUSD" -> 180.0
            "XAUUSD" -> 4.5
            "TSLA" -> 1.2
            else -> 0.0015
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (i in 0 until count) {
            val candleTime = now - ((count - 1 - i) * intervalMs)
            val open = currentClose
            val trend = sin(i.toDouble() * 0.4) * (volatility * 0.4)
            val noise = (Random.nextDouble() - 0.48) * volatility
            val close = max(0.01, open + trend + noise)
            val high = max(open, close) + (Random.nextDouble() * volatility * 0.5)
            val low = max(0.001, min(open, close) - (Random.nextDouble() * volatility * 0.5))
            val volume = 100.0 + (Random.nextDouble() * 500.0)

            // Check if any AI Signal matches this timestamp interval
            val matchingSignal = signals.find {
                abs(it.timestamp - candleTime) < (intervalMs / 2) || (i == count - 1 && signals.isNotEmpty())
            }

            result.add(
                CandleStickData(
                    timestamp = candleTime,
                    open = Math.round(open * 100.0) / 100.0,
                    high = Math.round(high * 100.0) / 100.0,
                    low = Math.round(low * 100.0) / 100.0,
                    close = Math.round(close * 100.0) / 100.0,
                    volume = Math.round(volume * 10.0) / 10.0,
                    timeLabel = timeFormat.format(Date(candleTime)),
                    signal = matchingSignal
                )
            )
            currentClose = close
        }
        // Enrich candles with technical analysis indicators (RSI 14)
        val enrichedCandles = TechnicalAnalysis.enrichCandlesWithRsi(result, 14)
        _rsiSeries.value = enrichedCandles.map { it.rsi }
        _currentRsi.value = enrichedCandles.lastOrNull()?.rsi
        return enrichedCandles
    }

    /**
     * Public calculation utilities for Relative Strength Index (RSI)
     */
    fun calculateRSI(candles: List<CandleStickData>, period: Int = 14): List<Double?> {
        return TechnicalAnalysis.calculateRSI(candles, period)
    }

    fun calculateRsiFromPrices(prices: List<Double>, period: Int = 14): List<Double?> {
        return TechnicalAnalysis.calculateRsiFromPrices(prices, period)
    }

    fun classifyRsi(rsi: Double?): RsiClassification {
        return TechnicalAnalysis.classifyRsiState(rsi)
    }

    private fun updateCandlesWithSignals(signals: List<SignalEntity>) {
        val currentList = _candlestickSeries.value
        if (currentList.isEmpty()) return

        val matchingSignals = signals.filter { it.symbol == selectedSymbol }
        if (matchingSignals.isEmpty()) return

        val updated = currentList.toMutableList()
        val latestSignal = matchingSignals.lastOrNull()
        if (latestSignal != null && updated.isNotEmpty()) {
            val lastIdx = updated.size - 1
            updated[lastIdx] = updated[lastIdx].copy(signal = latestSignal)
            _candlestickSeries.value = updated
        }
    }

    fun addLog(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _systemLogs.value = (listOf("[$timestamp] $msg") + _systemLogs.value).take(40)
    }

    private fun startLiveTickerLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val config = settingsState.value
                if (config.isScanningActive) {
                    // 1. Tick price for each asset
                    val updatedQuotes = symbolQuotes.value.map { quote ->
                        val changePct = Random.nextDouble(-0.0015, 0.0015)
                        val rawNewPrice = quote.price * (1.0 + changePct)
                        val newPrice = Math.round(rawNewPrice * Math.pow(10.0, quote.decimalPlaces.toDouble())) / Math.pow(10.0, quote.decimalPlaces.toDouble())
                        val direction = if (newPrice > quote.price) 1 else if (newPrice < quote.price) -1 else 0
                        val updatedSparkline = if (quote.sparkline.isEmpty()) {
                            listOf(quote.price, newPrice)
                        } else {
                            (quote.sparkline + newPrice).takeLast(16)
                        }
                        val newHigh = if (quote.high24h > 0) kotlin.math.max(quote.high24h, newPrice) else newPrice * 1.01
                        val newLow = if (quote.low24h > 0) kotlin.math.min(quote.low24h, newPrice) else newPrice * 0.99
                        
                        quote.copy(
                            price = newPrice,
                            lastTickDirection = direction,
                            sparkline = updatedSparkline,
                            high24h = newHigh,
                            low24h = newLow
                        )
                    }
                    symbolQuotes.value = updatedQuotes

                    // 2. Fetch active positions and update profits inside Room database safely
                    val openPosList = botDao.getOpenPositionsFlow().firstOrNull()?.filter { it.status == "OPEN" }
                    if (openPosList != null && openPosList.isNotEmpty()) {
                        for (pos in openPosList) {
                            val activeQuote = updatedQuotes.find { it.symbol == pos.symbol } ?: continue
                            val currentPrice = activeQuote.price
                            
                            // Profit equation: (Current - Open) * Volume * multiplier for BUY; (Open - Current) * Volume * multiplier for SELL
                            val diff = if (pos.direction == "BUY") currentPrice - pos.openPrice else pos.openPrice - currentPrice
                            val multiplier = activeQuote.pipsPerUnit
                            
                            // Simple profit model matching Metatrader lot sizes (standard lot is typically 100,000 units, but pips/unit scales it)
                            val lotScaledProfit = diff * pos.lotSize * multiplier * 100.0
                            val roundedProfit = Math.round(lotScaledProfit * 100.0) / 100.0

                            botDao.savePosition(
                                pos.copy(
                                    currentPrice = currentPrice,
                                    profit = roundedProfit
                                )
                            )
                        }
                    }
                }
                delay(1500)
            }
        }
    }

    // Update settings wrappers
    fun updateBotScanningState(isActive: Boolean) {
        viewModelScope.launch {
            val s = settingsState.value
            repository.saveSettings(s.copy(isScanningActive = isActive))
            addLog("Core Engine Scanner scanning = $isActive")
        }
    }

    fun updateSettingsValues(magic: Int, lot: Double, accent: String) {
        viewModelScope.launch {
            val s = settingsState.value
            repository.saveSettings(s.copy(
                magicNumber = magic,
                lotSize = lot,
                primaryAccent = accent
            ))
            addLog("Settings updated. Magic: $magic, Vol: $lot, Theme: $accent")
        }
    }

    fun updateGoogleGroundingState(useGrounding: Boolean) {
        viewModelScope.launch {
            val s = settingsState.value
            repository.saveSettings(s.copy(useGoogleGrounding = useGrounding))
            addLog("Google Grounding active: $useGrounding")
        }
    }

    fun selectSymbolAndTimeframe(symbol: String, timeframe: String) {
        selectedSymbol = symbol
        selectedTimeframe = timeframe
        rebuildCandlestickSeries(symbol, timeframe)
        viewModelScope.launch {
            val s = settingsState.value
            repository.saveSettings(s.copy(
                selectedSymbol = symbol,
                selectedTimeframe = timeframe
            ))
            // Fetch real-time market data with web grounding for the newly selected symbol
            fetchRealtimeMarketDataForSymbol(symbol)
        }
    }

    // --- Real-time Market Data & Web-Grounded Search Coroutine Flows ---
    fun fetchRealtimeMarketDataForSymbol(symbol: String = selectedSymbol, forceRefresh: Boolean = false) {
        val timeframe = selectedTimeframe
        val currentQuote = symbolQuotes.value.find { it.symbol == symbol }
        val currentPrice = currentQuote?.price ?: 2345.50
        val useGrounding = settingsState.value.useGoogleGrounding

        viewModelScope.launch {
            _marketIntelligenceState.value = MarketDataFlowState.Loading(
                symbol = symbol,
                message = "Connecting to Gemini 3.5 & Google Search Grounding for $symbol..."
            )
            _isLiveStreamingIntelligence.value = true
            addLog("[🌐 GEMINI LIVE] Fetching real-time market data & web grounding for $symbol...")

            try {
                val intelligence = repository.fetchRealtimeMarketIntelligence(
                    symbol = symbol,
                    currentPrice = currentPrice,
                    timeframe = timeframe,
                    useGoogleGrounding = useGrounding
                )
                
                // Update specific symbol cache map and active flow state
                _marketDataBySymbol.value = _marketDataBySymbol.value + (symbol to intelligence)
                _marketIntelligenceState.value = MarketDataFlowState.Success(intelligence)
                
                addLog("[✅ GEMINI LIVE SUCCESS] $symbol Trend: ${intelligence.trendDirection} (${intelligence.confidenceScore}%) | Citations: ${intelligence.newsCitations.size}")
            } catch (e: Exception) {
                addLog("[⚠️ GEMINI ERROR] Failed to fetch real-time data for $symbol: ${e.localizedMessage}")
                val fallback = _marketDataBySymbol.value[symbol]
                _marketIntelligenceState.value = MarketDataFlowState.Error(
                    symbol = symbol,
                    errorMessage = e.localizedMessage ?: "Unknown error while processing market intelligence",
                    fallbackIntelligence = fallback
                )
            } finally {
                _isLiveStreamingIntelligence.value = false
            }
        }
    }

    fun refreshCurrentPairMarketData() {
        fetchRealtimeMarketDataForSymbol(selectedSymbol, forceRefresh = true)
    }

    fun fetchAllPairsMarketIntelligence() {
        viewModelScope.launch {
            val pairs = listOf("XAUUSD", "BTCUSD", "EURUSD", "GBPUSD", "TSLA")
            addLog("[🚀 BATCH SCAN] Initiating Gemini real-time web grounding for all ${pairs.size} pairs...")
            for (pair in pairs) {
                val quote = symbolQuotes.value.find { it.symbol == pair }
                val price = quote?.price ?: 100.0
                try {
                    val intel = repository.fetchRealtimeMarketIntelligence(
                        symbol = pair,
                        currentPrice = price,
                        timeframe = selectedTimeframe,
                        useGoogleGrounding = settingsState.value.useGoogleGrounding
                    )
                    _marketDataBySymbol.value = _marketDataBySymbol.value + (pair to intel)
                } catch (e: Exception) {
                    // Safe continue
                }
                delay(400) // Stagger requests gracefully
            }
            // Update active state
            _marketDataBySymbol.value[selectedSymbol]?.let {
                _marketIntelligenceState.value = MarketDataFlowState.Success(it)
            }
            addLog("[✨ BATCH COMPLETE] All pair market intelligences updated via Gemini grounding.")
        }
    }

    // --- Gemini API Market Trend Analysis & Signal Generation ---

    /**
     * Calculates the real-time technical trend snapshot for a specific symbol and timeframe.
     */
    fun getTrendSnapshotForSymbol(
        symbol: String = selectedSymbol,
        timeframe: String = selectedTimeframe
    ): MarketTrendSnapshot {
        val quote = symbolQuotes.value.find { it.symbol == symbol }
            ?: SymbolQuote(symbol, 100.0, 2, 100.0)
        val candles = if (symbol == selectedSymbol) _candlestickSeries.value else emptyList()

        return TechnicalAnalysis.buildMarketTrendSnapshot(
            symbol = symbol,
            timeframe = timeframe,
            currentPrice = quote.price,
            change24h = quote.change24h,
            high24h = quote.high24h,
            low24h = quote.low24h,
            candles = candles
        )
    }

    /**
     * Analyzes live market data (price, 24h change, high/low, RSI, EMAs, support/resistance)
     * using the Gemini API to generate an algorithmic Buy/Sell/Hold signal.
     */
    fun analyzeMarketDataAndGenerateSignal(
        symbol: String = selectedSymbol,
        timeframe: String = selectedTimeframe
    ) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            val quote = symbolQuotes.value.find { it.symbol == symbol }
                ?: SymbolQuote(symbol, 100.0, 2, 100.0)
            val useGrounding = settingsState.value.useGoogleGrounding

            // 1. Calculate the comprehensive trend snapshot
            val trendSnapshot = getTrendSnapshotForSymbol(symbol, timeframe)
            _activeTrendSnapshot.value = trendSnapshot

            addLog("[🔄 GEMINI AI] Analyzing market data for $symbol on $timeframe (Trend: ${trendSnapshot.trendDirection}, RSI: ${String.format(Locale.US, "%.1f", trendSnapshot.currentRsi ?: 50.0)})...")

            try {
                // 2. Transmit trend matrix to Gemini API / Search Grounding
                val signal = repository.analyzeMarketTrendsAndGenerateSignal(
                    symbol = symbol,
                    timeframe = timeframe,
                    trendSnapshot = trendSnapshot,
                    useGoogleGrounding = useGrounding
                )

                // 3. Update candles with the newly generated signal indicator
                updateCandlesWithSignals(allSignals.value)

                addLog("[🟢 SIGNAL GENERATED] $symbol: ${signal.direction} @ ${signal.entryPrice} (SL: ${signal.stopLoss}, TP: ${signal.takeProfit}, Conf: ${signal.confidence}%) | Strategy: ${signal.strategy}")
            } catch (e: Exception) {
                addLog("[⚠️ ANALYSIS ERROR] $symbol analysis failed: ${e.localizedMessage}")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Convenience method to trigger a full Gemini trend analysis on the currently active symbol.
     */
    fun scanLiveMarketTrends() {
        analyzeMarketDataAndGenerateSignal(selectedSymbol, selectedTimeframe)
    }

    /**
     * Runs Gemini trend analysis across the entire monitored market symbol universe.
     */
    fun batchAnalyzeAllMarketTrends() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            val symbols = symbolQuotes.value.map { it.symbol }
            addLog("[🚀 BATCH SCAN] Initiating Gemini trend analysis for ${symbols.size} market symbols...")

            for (sym in symbols) {
                val trend = getTrendSnapshotForSymbol(sym, selectedTimeframe)
                try {
                    repository.analyzeMarketTrendsAndGenerateSignal(
                        symbol = sym,
                        timeframe = selectedTimeframe,
                        trendSnapshot = trend,
                        useGoogleGrounding = settingsState.value.useGoogleGrounding
                    )
                } catch (e: Exception) {
                    // Gracefully continue to next symbol
                }
                delay(350) // Prevent rapid burst rate limiting
            }

            _isAnalyzing.value = false
            addLog("[✨ BATCH COMPLETE] All symbol market trend signals generated.")
        }
    }

    // Execution handlers for backward-compatibility & UI events
    fun triggerAIGroundedScan() {
        analyzeMarketDataAndGenerateSignal(selectedSymbol, selectedTimeframe)
    }

    fun triggerScanForSymbol(symbol: String) {
        analyzeMarketDataAndGenerateSignal(symbol, selectedTimeframe)
    }

    fun batchScanAllSymbols() {
        batchAnalyzeAllMarketTrends()
    }

    fun executeSimulatedOrderForSignal(symbol: String, direction: String) {
        val currentQuote = symbolQuotes.value.find { it.symbol == symbol } ?: return
        val config = settingsState.value

        viewModelScope.launch {
            val pos = TradePosition(
                symbol = symbol,
                direction = direction,
                openPrice = currentQuote.price,
                lotSize = config.lotSize,
                currentPrice = currentQuote.price,
                profit = 0.0,
                status = "OPEN",
                openTime = System.currentTimeMillis()
            )
            repository.savePosition(pos)
            addLog("[🔥 TRADE OPEN] Executed Simulated $direction order for ${pos.lotSize} lots on $symbol.")
        }
    }

    fun executeSimulatedOrder(direction: String) {
        val symbol = selectedSymbol
        val currentQuote = symbolQuotes.value.find { it.symbol == symbol } ?: return
        val config = settingsState.value

        viewModelScope.launch {
            val pos = TradePosition(
                symbol = symbol,
                direction = direction,
                openPrice = currentQuote.price,
                lotSize = config.lotSize,
                currentPrice = currentQuote.price,
                profit = 0.0,
                status = "OPEN",
                openTime = System.currentTimeMillis()
            )
            repository.savePosition(pos)
            addLog("[🔥 TRADE OPEN] Executed Simulated $direction scale order for ${pos.lotSize} lots on $symbol.")
        }
    }

    fun closeSimulatedOrder(position: TradePosition) {
        viewModelScope.launch {
            val currentQuote = symbolQuotes.value.find { it.symbol == position.symbol } ?: return@launch
            val finalizedPos = position.copy(
                status = "CLOSED",
                closePrice = currentQuote.price,
                closeTime = System.currentTimeMillis()
            )
            repository.updatePosition(finalizedPos)
            addLog("[⛔ TRADE CLOSED] Closed order #${position.id} (${position.symbol}) with net profit element: $${finalizedPos.profit}")
        }
    }

    fun clearTradeHistory() {
        viewModelScope.launch {
            repository.clearAllPositions()
            addLog("[🧹 WIPE SUCCESS] Cleared all trade history tracks.")
        }
    }

    // --- Market News & Economic Reports Service trigger ---
    fun fetchMarketNewsAndEconomicReports(category: String, symbol: String? = null) {
        viewModelScope.launch {
            _isFetchingNews.value = true
            addLog("[📰 NEWS FETCH] Connecting to Google Search grounding engine for $category reports...")
            try {
                val report = repository.fetchMarketNewsReport(category, symbol ?: selectedSymbol)
                addLog("[📰 NEWS SUCCESS] Report fetched: ${report.title}")
            } catch (e: Exception) {
                addLog("[🚨 NEWS ERROR] Failed to fetch reports: ${e.localizedMessage}")
            } finally {
                _isFetchingNews.value = false
            }
        }
    }

    fun clearAllNewsReports() {
        viewModelScope.launch {
            repository.clearAllNewsReports()
            addLog("[🧹 NEWS WIPE] Cleared all cached market reports.")
        }
    }

    // --- Dynamic Interface Style Customization ---
    fun updateActiveLayout(layout: String) {
        viewModelScope.launch {
            val s = settingsState.value
            repository.saveSettings(s.copy(activeLayout = layout))
            addLog("[🎭 LAYOUT UPDATED] Switched interface rendering structure to: $layout")
        }
    }

    fun updateActiveEffect(effect: String) {
        viewModelScope.launch {
            val s = settingsState.value
            repository.saveSettings(s.copy(activeEffect = effect))
            addLog("[✨ EFFECT UPDATED] Active design filter set to: $effect")
        }
    }

    fun updateSelectedBackground(bg: String) {
        viewModelScope.launch {
            val s = settingsState.value
            repository.saveSettings(s.copy(selectedBackground = bg))
            addLog("[🌄 BG UPDATED] Loaded custom background asset theme: $bg")
        }
    }
}
