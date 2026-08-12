package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderGray
import com.example.ui.theme.CyberAqua
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.PanelDark
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SubtitleWhite
import com.example.ui.viewmodel.SymbolQuote
import java.util.Locale

/**
 * Compact Live Market Symbol Card for Carousels and Grid views.
 */
@Composable
fun CompactLiveSymbolCard(
    quote: SymbolQuote,
    isSelected: Boolean,
    accentColor: Color,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = quote.change24h >= 0
    val trendColor = if (isPositive) CyberNeonGreen else CyberCrimson

    val targetPriceBgColor = when (quote.lastTickDirection) {
        1 -> CyberNeonGreen.copy(alpha = 0.18f)
        -1 -> CyberCrimson.copy(alpha = 0.18f)
        else -> Color.Transparent
    }

    val animatedPriceBg by animateColorAsState(
        targetValue = targetPriceBgColor,
        animationSpec = tween(durationMillis = 350),
        label = "TickBgAnimation"
    )

    Card(
        modifier = modifier
            .width(170.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .testTag("compact_symbol_card_${quote.symbol}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PanelDark else Color(0xFF0F0F0F)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else BorderGray
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isSelected) {
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF141414), Color(0xFF0C0C0C))
                        )
                    }
                )
                .padding(12.dp)
        ) {
            // Top Row: Symbol & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = quote.symbol,
                        color = if (isSelected) accentColor else PrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    if (quote.name.isNotEmpty()) {
                        Text(
                            text = quote.name,
                            color = SubtitleWhite,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Category pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1F1F1F))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = quote.category,
                        color = SubtitleWhite,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Middle: Live Price with tick flash
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(animatedPriceBg)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.${quote.decimalPlaces}f", quote.price),
                    color = PrimaryWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sparkline Graph
            if (quote.sparkline.isNotEmpty()) {
                MiniSparkline(
                    data = quote.sparkline,
                    lineColor = trendColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Row: 24h Change & Spread
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%+.2f%%", quote.change24h),
                        color = trendColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "${quote.spreadPips} pips",
                    color = SubtitleWhite,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Full-width Expanded Live Market Symbol Card for Detailed Dashboard Listings.
 */
@Composable
fun FullLiveSymbolPriceCard(
    quote: SymbolQuote,
    isSelected: Boolean,
    accentColor: Color,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = quote.change24h >= 0
    val trendColor = if (isPositive) CyberNeonGreen else CyberCrimson

    val targetPriceBgColor = when (quote.lastTickDirection) {
        1 -> CyberNeonGreen.copy(alpha = 0.18f)
        -1 -> CyberCrimson.copy(alpha = 0.18f)
        else -> Color.Transparent
    }

    val animatedPriceBg by animateColorAsState(
        targetValue = targetPriceBgColor,
        animationSpec = tween(durationMillis = 350),
        label = "FullTickBgAnimation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect)
            .testTag("full_symbol_card_${quote.symbol}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PanelDark else Color(0xFF111111)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else BorderGray
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isSelected) {
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF161616), Color(0xFF0D0D0D))
                        )
                    }
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Asset details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Category Icon / Avatar Indicator
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.18f) else Color(0xFF1E1E1E)
                            )
                            .border(
                                1.dp,
                                if (isSelected) accentColor else BorderGray,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = quote.symbol.take(2),
                            color = if (isSelected) accentColor else PrimaryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = quote.symbol,
                                color = PrimaryWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF222222))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = quote.category,
                                    color = SubtitleWhite,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = quote.name.ifEmpty { "${quote.symbol} Live Quote" },
                            color = SubtitleWhite,
                            fontSize = 11.sp
                        )
                    }
                }

                // Right: Live Price & 24h Change Badge
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(animatedPriceBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.${quote.decimalPlaces}f", quote.price),
                            color = PrimaryWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(trendColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isPositive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = trendColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%+.2f%%", quote.change24h),
                                    color = trendColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle: Sparkline & Quick stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sparkline
                if (quote.sparkline.isNotEmpty()) {
                    Box(modifier = Modifier.weight(1f).height(36.dp)) {
                        MiniSparkline(
                            data = quote.sparkline,
                            lineColor = trendColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Range / Spread stats
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "24h H: ${String.format(Locale.US, "%.${quote.decimalPlaces}f", quote.high24h.takeIf { it > 0 } ?: (quote.price * 1.012))}",
                        color = SubtitleWhite,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "24h L: ${String.format(Locale.US, "%.${quote.decimalPlaces}f", quote.low24h.takeIf { it > 0 } ?: (quote.price * 0.988))}",
                        color = SubtitleWhite,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Vol: ${quote.volume24h} | Sp: ${quote.spreadPips}p",
                        color = CyberAqua,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
