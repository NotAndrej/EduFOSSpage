package org.edufosspage.ui.timetable

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.edufosspage.R
import org.edufosspage.data.Lesson
import org.edufosspage.ui.home.HomeViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun TimetableScreen(vm: HomeViewModel = viewModel()) {
    val timetables    by vm.timetables.collectAsState()
    val failedDates   by vm.failedDates.collectAsState()

    val selectedDate  by vm.timetableSelectedDate.collectAsState()
    val weekStart     by vm.timetableWeekStart.collectAsState()

    val fmt         = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val selectedKey = selectedDate.format(fmt)
    val lessons     = timetables[selectedKey]
    val hasFailed   = selectedKey in failedDates

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = LocalTime.now()
        }
    }

    LaunchedEffect(selectedDate) { vm.loadDate(selectedDate) }

    Column(
        Modifier.fillMaxSize()
    ) {
        WeekStrip(
            weekStart      = weekStart,
            selectedDate   = selectedDate,
            onDateSelected = { date -> vm.setTimetableSelectedDate(date) },
            onPrevWeek     = {
                val prev = weekStart.minusWeeks(1)
                vm.setTimetableWeekStart(prev)
            },
            onNextWeek     = {
                val next = weekStart.plusWeeks(1)
                vm.setTimetableWeekStart(next)
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when {
            hasFailed -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.core_timetable_load_error),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(onClick = { vm.retryDate(selectedDate) }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.core_retry))
                    }
                }
            }

            lessons == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }

            lessons.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎉", style = MaterialTheme.typography.displayMedium)
                    Text(
                        stringResource(R.string.core_no_lessons),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> LazyColumn(
                // nav bar inset comes from Scaffold, no extra padding needed
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                // Pass selectedDate so LessonCard knows whether to show "NOW"
                items(lessons) { lesson -> LessonCard(lesson, selectedDate, now) }
            }
        }
    }
}

@Composable
private fun WeekStrip(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val today  = LocalDate.now()
    val dayFmt = DateTimeFormatter.ofPattern("dd")

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevWeek) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.core_prev_week))
        }

        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            (0..6).forEach { offset ->
                val date       = weekStart.plusDays(offset.toLong())
                val isSelected = date == selectedDate
                val isToday    = date == today
                val dayLabel   = date.dayOfWeek
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)

                val containerColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday    -> MaterialTheme.colorScheme.secondaryContainer
                    else       -> MaterialTheme.colorScheme.surfaceVariant
                }
                val contentColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday    -> MaterialTheme.colorScheme.onSecondaryContainer
                    else       -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    onClick  = { onDateSelected(date) },
                    shape    = RoundedCornerShape(12.dp),
                    color    = containerColor,
                    modifier = Modifier.width(44.dp)
                ) {
                    Column(
                        Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            date.format(dayFmt),
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color      = contentColor
                        )
                    }
                }
            }
        }

        IconButton(onClick = onNextWeek) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.core_next_week))
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, date: LocalDate, now: LocalTime) {
    val start = lesson.startTime.toLocalTimeOrNull()
    val end   = lesson.endTime.toLocalTimeOrNull()
    val isNow = date == LocalDate.now() && start != null && end != null && now >= start && now <= end

    val cardColor = when {
        isNow                 -> MaterialTheme.colorScheme.primaryContainer
        lesson.isCancelled    -> MaterialTheme.colorScheme.errorContainer
        lesson.isSubstitution -> MaterialTheme.colorScheme.tertiaryContainer
        else                  -> MaterialTheme.colorScheme.surfaceVariant
    }
    val onCardColor = when {
        isNow                 -> MaterialTheme.colorScheme.onPrimaryContainer
        lesson.isCancelled    -> MaterialTheme.colorScheme.onErrorContainer
        lesson.isSubstitution -> MaterialTheme.colorScheme.onTertiaryContainer
        else                  -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = cardColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape    = CircleShape,
                color    = onCardColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        "${lesson.period}",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = onCardColor
                    )
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        lesson.subjectName.ifBlank { lesson.subjectShort },
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = onCardColor,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )
                    when {
                        isNow                 -> StatusChip(stringResource(R.string.core_now), MaterialTheme.colorScheme.primary,  MaterialTheme.colorScheme.onPrimary)
                        lesson.isCancelled    -> StatusChip(stringResource(R.string.core_cancelled), MaterialTheme.colorScheme.error,    MaterialTheme.colorScheme.onError)
                        lesson.isSubstitution -> StatusChip(stringResource(R.string.core_substitution), MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
                    }
                }

                val details = buildList {
                    if (lesson.teacherName.isNotBlank()) add(lesson.teacherName)
                    if (lesson.classroom.isNotBlank())   add(lesson.classroom)
                    if (lesson.groups.isNotEmpty())      add(lesson.groups.joinToString(", "))
                }.joinToString(" · ")

                if (details.isNotBlank()) {
                    Text(
                        details,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = onCardColor.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                lesson.curriculum?.let {
                    Text(
                        it,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = onCardColor.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    lesson.startTime,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = onCardColor
                )
                Text(
                    lesson.endTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = onCardColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color
) {
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            color      = fg,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

private fun String.toLocalTimeOrNull(): LocalTime? = runCatching {
    val p = split(":"); LocalTime.of(p[0].toInt(), p[1].toInt())
}.getOrNull()
