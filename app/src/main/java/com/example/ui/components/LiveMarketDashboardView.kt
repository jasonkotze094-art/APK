package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.SignalEntity
import com.example.ui.theme.BorderGray
import com.example.ui.theme.CyberAqua
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.PanelDark
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SubtitleWhite
import com.example.ui.viewmodel.BotViewModel
import com.example.ui.viewmodel.SymbolQuote

/**
 * Clean, high-fidelity Jetpack Compose Market Dashboard UI.
 * Integrates live market symbol price cards, active chart focus, and generated Buy/Sell signal cards.
 */
@Composable
fun LiveMarketDashboardView(
    viewModel: BotViewModel,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val quotes by viewModel.symbolQuotes.collectAsState()
    val allSignals by viewModel.allSignals.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val candles by viewModel.candlestickSeries.collectAsState()
    val openPositions by viewModel.allPositions.collectAsState()

    val selectedSymbol = viewModel.selectedSymbol
    val selectedTimeframe = viewModel.selectedTimeframe
    val activeQuote = quotes.find { it.symbol == selectedSymbol } ?: SymbolQuote(selectedSymbol, 2345.50, 2, 100.0)

    // Local UI states for filtering
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedSignalFilter by remember { mutableStateOf("ALL") } // "ALL", "BUY", "SELL", "HIGH_CONVICTION"
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(false) }

    val categories = listOf("ALL", "COMMODITIES", "CRYPTO", "FOREX", "STOCKS")

    // Filter quotes by category and search
    val filteredQuotes = quotes.filter { q ->
        val matchesCategory = selectedCategory == "ALL" || q.category.uppercase() == selectedCategory
        val matchesSearch = searchQuery.isEmpty() || q.symbol.contains(searchQuery, ignoreCase = true) || q.name.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    // Filter signals by direction, conviction, and search
    val filteredSignals = allSignals.filter { s ->
        val matchesDirection = when (selectedSignalFilter) {
            "BUY" -> s.direction.uppercase() == "BUY"
            "SELL" -> s.direction.uppercase() == "SELL"
            "HIGH_CONVICTION" -> s.confidence >= 80
            else -> true
        }
        val matchesSearch = searchQuery.isEmpty() || s.symbol.contains(searchQuery, ignoreCase = true) || s.strategy.contains(searchQuery, ignoreCase = true)
        matchesDirection && matchesSearch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Dashboard Header: Title, Status, & Quick Stats Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LIVE MARKET DASHBOARD",
                            color = CyberMagenta,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "The Clown Beast",
                            color = PrimaryWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Online / Scanner Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFF161616))
                            .border(1.dp, BorderGray, RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (settings.isScanningActive) CyberNeonGreen else Color.Gray)
                            )
                            Text(
                                text = if (settings.isScanningActive) "SCANNER LIVE" else "IDLE",
                                color = if (settings.isScanningActive) CyberNeonGreen else SubtitleWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Overview Bar
                DashboardStatsBar(
                    signals = allSignals,
                    isScanningActive = settings.isScanningActive,
                    isAnalyzing = isAnalyzing,
                    accentColor = accentColor,
                    onToggleScanning = {
                        viewModel.updateBotScanningState(!settings.isScanningActive)
                    },
                    onBatchScanAll = {
                        viewModel.batchScanAllSymbols()
                    }
                )
            }
        }

        // 2. Search & Category Filters Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search market symbols or strategies (e.g. XAUUSD, Rebound)", color = SubtitleWhite, fontSize = 12.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = SubtitleWhite, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SubtitleWhite, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("dashboard_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryWhite,
                        unfocusedTextColor = PrimaryWhite,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = BorderGray,
                        focusedContainerColor = PanelDark,
                        unfocusedContainerColor = PanelDark
                    ),
                    singleLine = true
                )

                // Asset Category Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) accentColor else Color(0xFF161616))
                                .border(1.dp, if (isSelected) accentColor else BorderGray, RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("category_chip_$cat"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.Black else PrimaryWhite,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Live Market Symbol Price Cards Section
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "LIVE MARKET SYMBOL PRICES",
                            color = SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberAqua.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${filteredQuotes.size} Active",
                                color = CyberAqua,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // View Toggle (Carousel vs Expanded Grid)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1A1A))
                            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                            .clickable { isGridView = !isGridView }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("toggle_symbol_view_button")
                    ) {
                        Text(
                            text = if (isGridView) "CAROUSEL VIEW" else "EXPANDED VIEW",
                            color = CyberAqua,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price Cards Render: Horizontal Carousel or Stacked List
                if (!isGridView) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(filteredQuotes, key = { it.symbol }) { quote ->
                            CompactLiveSymbolCard(
                                quote = quote,
                                isSelected = quote.symbol == selectedSymbol,
                                accentColor = accentColor,
                                onSelect = {
                                    viewModel.selectSymbolAndTimeframe(quote.symbol, selectedTimeframe)
                                }
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredQuotes.forEach { quote ->
                            FullLiveSymbolPriceCard(
                                quote = quote,
                                isSelected = quote.symbol == selectedSymbol,
                                accentColor = accentColor,
                                onSelect = {
                                    viewModel.selectSymbolAndTimeframe(quote.symbol, selectedTimeframe)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Timeframe Selector & Active Symbol Chart Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(BorderStroke(1.dp, BorderGray), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PanelDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header of active chart focus
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$selectedSymbol ($selectedTimeframe)",
                                    color = PrimaryWhite,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "FOCUS PAIR",
                                        color = accentColor,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Current Price: ${activeQuote.price}",
                                color = SubtitleWhite,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Timeframes row
                        val timeframes = listOf("M1", "M5", "M15", "M30", "H1")
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            timeframes.forEach { tf ->
                                val isSelected = tf == selectedTimeframe
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) PrimaryWhite else Color(0xFF1E1E1E))
                                        .border(1.dp, if (isSelected) PrimaryWhite else BorderGray, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.selectSymbolAndTimeframe(selectedSymbol, tf) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tf,
                                        color = if (isSelected) Color.Black else PrimaryWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Embedded lightweight interactive chart
                    val activeSignal = allSignals.find { it.symbol == selectedSymbol }
                    LightweightPriceChart(
                        symbol = selectedSymbol,
                        candles = candles,
                        currentPrice = activeQuote.price,
                        activeSignal = activeSignal,
                        allSignals = allSignals,
                        accentColor = accentColor,
                        onSignalClick = { sig ->
                            viewModel.selectSymbolAndTimeframe(sig.symbol, sig.timeframe)
                        }
                    )
                }
            }
        }

        // Progress bar for Gemini analytical sequence
        if (isAnalyzing) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        color = accentColor,
                        trackColor = BorderGray,
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                    )
                    Text(
                        text = "GENERATING AI SIGNALS WITH GOOGLE SEARCH GROUNDING...",
                        color = CyberAqua,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // 5. Generated Buy / Sell Signals Stream Section
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "GENERATED BUY / SELL SIGNALS",
                            color = SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberNeonGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${filteredSignals.size} Signals",
                                color = CyberNeonGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Single-click trigger for selected symbol
                    Button(
                        onClick = { viewModel.triggerAIGroundedScan() },
                        enabled = !isAnalyzing,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                        modifier = Modifier.height(30.dp).testTag("scan_active_symbol_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SCAN $selectedSymbol", color = PrimaryWhite, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Signal Filter Chips: All, Buy Only, Sell Only, High Conviction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        Pair("ALL", "ALL SIGNALS"),
                        Pair("BUY", "🟢 BUY ONLY"),
                        Pair("SELL", "🔴 SELL ONLY"),
                        Pair("HIGH_CONVICTION", "⚡ 80%+ CONVICTION")
                    )

                    filterOptions.forEach { (key, label) ->
                        val isSelected = selectedSignalFilter == key
                        val chipColor = when (key) {
                            "BUY" -> CyberNeonGreen
                            "SELL" -> CyberCrimson
                            "HIGH_CONVICTION" -> CyberAqua
                            else -> accentColor
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) chipColor.copy(alpha = 0.2f) else Color(0xFF161616))
                                .border(1.dp, if (isSelected) chipColor else BorderGray, RoundedCornerShape(16.dp))
                                .clickable { selectedSignalFilter = key }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("signal_filter_$key")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) chipColor else PrimaryWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 6. Signal Cards List
        if (filteredSignals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = PanelDark),
                    border = BorderStroke(1.dp, BorderGray)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SubtitleWhite, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No signals match the current filter",
                            color = PrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try clearing the search query or changing the direction filter.",
                            color = SubtitleWhite,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                selectedSignalFilter = "ALL"
                                searchQuery = ""
                                selectedCategory = "ALL"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset Filters", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredSignals, key = { "${it.symbol}_${it.timestamp}" }) { signal ->
                val quote = quotes.find { it.symbol == signal.symbol }
                val currentPrice = quote?.price ?: signal.entryPrice

                GeneratedBuySellSignalCard(
                    signal = signal,
                    currentPrice = currentPrice,
                    accentColor = accentColor,
                    onExecuteOrder = { direction ->
                        viewModel.executeSimulatedOrderForSignal(signal.symbol, direction)
                    },
                    onFocusChart = { sym, tf ->
                        viewModel.selectSymbolAndTimeframe(sym, tf)
                    },
                    onReScan = { sym ->
                        viewModel.triggerScanForSymbol(sym)
                    }
                )
            }
        }

        // 7. Active Open Simulated Orders Section
        val openOrders = openPositions.filter { it.status == "OPEN" }
        if (openOrders.isNotEmpty()) {
            item {
                Text(
                    text = "ACTIVE SIMULATED POSITIONS (${openOrders.size})",
                    color = SubtitleWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(openOrders, key = { it.id }) { pos ->
                SimulatedPositionCard(
                    position = pos,
                    onCloseOrder = { viewModel.closeSimulatedOrder(pos) }
                )
            }
        }

        // Bottom padding spacer
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
