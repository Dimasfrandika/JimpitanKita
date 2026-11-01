package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CalendarHelper
import com.example.util.MonthInfo

@Composable
fun MonthSelectorBar(
    currentMonthInfo: MonthInfo,
    onSelectMonth: (year: Int, monthIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedYearMenu by remember { mutableStateOf(false) }
    val years = remember { CalendarHelper.getAvailableYears() }
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("month_selector_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Periode Kalender:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val newMonth = if (currentMonthInfo.monthIndex > 1) currentMonthInfo.monthIndex - 1 else 12
                            val newYear = if (currentMonthInfo.monthIndex > 1) currentMonthInfo.year else currentMonthInfo.year - 1
                            onSelectMonth(newYear, newMonth)
                        },
                        modifier = Modifier.size(28.dp).testTag("prev_month_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Bulan Sebelumnya")
                    }

                    Text(
                        text = "${currentMonthInfo.monthName} ${currentMonthInfo.year}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            val newMonth = if (currentMonthInfo.monthIndex < 12) currentMonthInfo.monthIndex + 1 else 1
                            val newYear = if (currentMonthInfo.monthIndex < 12) currentMonthInfo.year else currentMonthInfo.year + 1
                            onSelectMonth(newYear, newMonth)
                        },
                        modifier = Modifier.size(28.dp).testTag("next_month_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Bulan Berikutnya")
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Month Chips (Januari s/d Desember)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalendarHelper.MONTH_NAMES.forEachIndexed { index, name ->
                    val monthIdx = index + 1
                    val isSelected = currentMonthInfo.monthIndex == monthIdx
                    val shortName = name.take(3)
                    val days = CalendarHelper.getDaysInMonth(currentMonthInfo.year, monthIdx)

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectMonth(currentMonthInfo.year, monthIdx) },
                        label = {
                            Text(
                                text = "$shortName ($days hr)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("month_chip_$monthIdx")
                    )
                }
            }
        }
    }
}
