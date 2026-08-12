package com.example.data.model

data class MarketCitation(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String = ""
)

data class GeminiMarketIntelligence(
    val symbol: String,
    val currentPrice: Double,
    val trendDirection: String, // "BUY", "SELL", "HOLD"
    val confidenceScore: Int, // 0..100
    val sentimentSummary: String,
    val keyCatalysts: List<String>,
    val supportLevel: Double,
    val resistanceLevel: Double,
    val suggestedEntry: Double,
    val suggestedStopLoss: Double,
    val suggestedTakeProfit: Double,
    val strategyName: String,
    val newsCitations: List<MarketCitation>,
    val searchQueries: List<String>,
    val rawAnalysis: String,
    val isGrounded: Boolean = true,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

sealed class MarketDataFlowState {
    object Idle : MarketDataFlowState()
    data class Loading(
        val symbol: String,
        val message: String = "Connecting to Gemini API & Google Search Grounding..."
    ) : MarketDataFlowState()
    data class Success(
        val intelligence: GeminiMarketIntelligence
    ) : MarketDataFlowState()
    data class Error(
        val symbol: String,
        val errorMessage: String,
        val fallbackIntelligence: GeminiMarketIntelligence? = null
    ) : MarketDataFlowState()
}
