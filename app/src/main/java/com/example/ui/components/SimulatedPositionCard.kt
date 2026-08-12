package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.TradePosition
import com.example.ui.theme.BorderGray
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.PanelDark
import com.example.ui.theme.PrimaryWhite
import com.example.ui.theme.SubtitleWhite
import java.util.Locale

@Composable
fun SimulatedPositionCard(
    position: TradePosition,
    onCloseOrder: (TradePosition) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBuy = position.direction.uppercase() == "BUY"
    val accColor = if (isBuy) CyberNeonGreen else CyberCrimson
    val profitColor = if (position.profit >= 0.0) CyberNeonGreen else CyberCrimson

    Card(
        colors = CardDefaults.cardColors(containerColor = PanelDark),
        border = BorderStroke(1.dp, BorderGray),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("simulated_pos_${position.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accColor.copy(alpha = 0.12f))
                            .border(1.dp, accColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(position.direction, color = accColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(position.symbol, color = PrimaryWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${position.lotSize} Lots", color = SubtitleWhite, fontSize = 10.5.sp)
                }

                Row(
                    modifier = Modifier.padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Open: ${String.format(Locale.US, "%.2f", position.openPrice)}", color = SubtitleWhite, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Live: ${String.format(Locale.US, "%.2f", position.currentPrice)}", color = PrimaryWhite, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (position.profit >= 0) "+$${String.format(Locale.US, "%.2f", position.profit)}" else "-$${String.format(Locale.US, "%.2f", -position.profit)}",
                        color = profitColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("Unrealized P&L", color = SubtitleWhite, fontSize = 8.5.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = { onCloseOrder(position) },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E1E1E))
                        .testTag("close_pos_btn_${position.id}")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close Order",
                        tint = SubtitleWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
