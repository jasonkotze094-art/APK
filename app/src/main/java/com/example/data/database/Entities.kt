package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "bot_settings")
data class BotSettings(
    @PrimaryKey val id: Int = 1,
    val magicNumber: Int = 991122,
    val lotSize: Double = 0.01,
    val isScanningActive: Boolean = true,
    val selectedTimeframe: String = "M5",
    val selectedSymbol: String = "XAUUSD",
    val primaryAccent: String = "NeonGreen", // "NeonGreen", "Magenta", "Aqua", "Crimson", "Blue", "Sky", "Purple", "Pink", "Lime", "Green", "Yellow", "Gold", "Red", "Orange", "Gray"
    val useGoogleGrounding: Boolean = true,
    val activeLayout: String = "Cyborg", // "Classic", "Terminal", "Cyborg"
    val activeEffect: String = "None", // "None", "Pulsing Glow", "Matrix Rain", "Neon Moons", "Trailer Night"
    val selectedBackground: String = "Default" // "Default", "Grid", "Holo", "Mainframe"
)

@Entity(tableName = "trend_signals")
data class SignalEntity(
    @PrimaryKey val symbol: String,
    val direction: String, // "BUY", "SELL", "SCANNING"
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val strategy: String,
    val timeframe: String,
    val confidence: Int,
    val analysis: String,
    val groundingQueries: String, // Comma-separated search queries
    val groundingSources: String = "", // Delimited title|||url string for search grounding results
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "trade_positions")
data class TradePosition(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val direction: String, // "BUY", "SELL"
    val openPrice: Double,
    val closePrice: Double? = null,
    val lotSize: Double,
    val currentPrice: Double,
    val profit: Double,
    val status: String, // "OPEN", "CLOSED"
    val openTime: Long = System.currentTimeMillis(),
    val closeTime: Long? = null
)

@Entity(tableName = "market_news_reports")
data class MarketNewsReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val groundingQueries: String,
    val category: String, // "General", "Macro", "Symbol-Specific"
    val timestamp: Long = System.currentTimeMillis()
)

