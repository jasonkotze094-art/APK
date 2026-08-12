package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BotDao {
    // --- Settings queries ---
    @Query("SELECT * FROM bot_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<BotSettings?>

    @Query("SELECT * FROM bot_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): BotSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: BotSettings)

    // --- Signal queries ---
    @Query("SELECT * FROM trend_signals ORDER BY timestamp DESC")
    fun getAllSignalsFlow(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM trend_signals WHERE symbol = :symbol LIMIT 1")
    fun getSignalForSymbolFlow(symbol: String): Flow<SignalEntity?>

    @Query("SELECT * FROM trend_signals WHERE symbol = :symbol LIMIT 1")
    suspend fun getSignalForSymbolDirect(symbol: String): SignalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSignal(signal: SignalEntity)

    // --- simulated Positions queries ---
    @Query("SELECT * FROM trade_positions ORDER BY openTime DESC")
    fun getAllPositionsFlow(): Flow<List<TradePosition>>

    @Query("SELECT * FROM trade_positions WHERE status = 'OPEN' ORDER BY openTime DESC")
    fun getOpenPositionsFlow(): Flow<List<TradePosition>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePosition(position: TradePosition)

    @Update
    suspend fun updatePosition(position: TradePosition)

    @Delete
    suspend fun deletePosition(position: TradePosition)

    @Query("DELETE FROM trade_positions")
    suspend fun clearAllPositions()

    // --- Market News & Economic Reports ---
    @Query("SELECT * FROM market_news_reports ORDER BY timestamp DESC")
    fun getAllNewsReportsFlow(): Flow<List<MarketNewsReport>>

    @Query("SELECT * FROM market_news_reports WHERE category = :category ORDER BY timestamp DESC")
    fun getNewsReportsByCategoryFlow(category: String): Flow<List<MarketNewsReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNewsReport(report: MarketNewsReport)

    @Delete
    suspend fun deleteNewsReport(report: MarketNewsReport)

    @Query("DELETE FROM market_news_reports")
    suspend fun clearAllNewsReports()
}
