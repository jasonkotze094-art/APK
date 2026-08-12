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
    val decimalPlaces: Int,
    val pipsPerUnit: Double
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
            SymbolQuote("XAUUSD", 2345.50, 2, 100.0),
            SymbolQuote("BTCUSD", 67840.0, 1, 1.0),
            SymbolQuote("EURUSD", 1.0854, 4, 10000.0),
            SymbolQuote("GBPUSD", 1.2725, 4, 10000.0),
            SymbolQuote("TSLA", 184.20, 2, 100.0)
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
        // Prepare default settings if empty
        viewModelScope.launch {
            val currentSettings = repository.getSettingsDirect()
            selectedSymbol = currentSettings.selectedSymbol
            selectedTimeframe = currentSettings.selectedTimeframe
            
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
                        val changePct = Random.nextDouble(-0.0012, 0.0012)
                        val newPrice = quote.price * (1.0 + changePct)
                        quote.copy(price = Math.round(newPrice * Math.pow(10.0, quote.decimalPlaces.toDouble())) / Math.pow(10.0, quote.decimalPlaces.toDouble()))
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

    // Execution handlers
    fun triggerAIGroundedScan() {
        val symbol = selectedSymbol
        val timeframe = selectedTimeframe
        val currentQuote = symbolQuotes.value.find { it.symbol == symbol } ?: return
        val currentPrice = currentQuote.price
        val useGrounding = settingsState.value.useGoogleGrounding

        viewModelScope.launch {
            _isAnalyzing.value = true
            addLog("[🔄 AI SCAN] Uploading quote sequence... Calling THE CLOWN BEAST neural processor.")
            
            // Invoke the repository's grounded search analysis
            val result = repository.analyzeAssetWithGemini(symbol, timeframe, currentPrice, useGrounding)
            
            _isAnalyzing.value = false
            addLog("[🟢 SCAN SUCCESS] Symbol: $symbol | Action: ${result.direction} (Conf: ${result.confidence}%) | Strategy: ${result.strategy}")
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
