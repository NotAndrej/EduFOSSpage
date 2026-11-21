package org.edufosspage.wear.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import org.edufosspage.data.Lesson
import org.edufosspage.wear.InfoBlock
import org.edufosspage.wear.LoadingRow
import org.edufosspage.wear.SectionHeader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val LABEL_FMT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

@Composable
fun TimetableScreen(onBack: () -> Unit, vm: TimetableViewModel = viewModel()) {
    val date by vm.date.collectAsState()
    val lessonsByDate by vm.lessons.collectAsState()
    val failed by vm.failed.collectAsState()

    val haptics = LocalHapticFeedback.current
    val key = date.format(DAY_FMT)
    val lessons = lessonsByDate[key]
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText          = { TimeText() },
        positionIndicator = { PositionIndicator(listState) },
    ) {
        ScalingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 8.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { SectionHeader("Timetable", onBack) }

            item {
                DayPicker(
                    date    = date,
                    onPrev  = { haptics.tap(); vm.select(date.minusDays(1)) },
                    onNext  = { haptics.tap(); vm.select(date.plusDays(1)) },
                    onToday = { haptics.tap(); vm.select(LocalDate.now()) },
                )
            }

            when {
                key in failed -> item {
                    InfoBlock("Couldn't load this day", "Tap to retry", isError = true)
                }

                lessons == null -> item { LoadingRow("Loading timetable…") }

                lessons.isEmpty() -> item {
                    InfoBlock("No lessons", "Enjoy the free time")
                }

                else -> items(lessons) { lesson -> LessonRow(lesson) }
            }
        }
    }
}

private fun androidx.compose.ui.hapticfeedback.HapticFeedback.tap() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
private fun DayPicker(
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CompactChip(
                onClick = onPrev,
                icon    = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day") },
            )
            Text(
                text      = date.format(LABEL_FMT),
                style     = MaterialTheme.typography.body1,
                maxLines  = 1,
                modifier  = Modifier.padding(horizontal = 2.dp),
            )
            CompactChip(
                onClick = onNext,
                icon    = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day") },
            )
        }
        if (date != LocalDate.now()) {
            CompactChip(
                onClick = onToday,
                label   = { Text("Today") },
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson) {
    val title = lesson.subjectShort.ifBlank { lesson.subjectName }
    val time = if (lesson.startTime.isNotBlank()) "${lesson.startTime}–${lesson.endTime}" else ""

    val details = buildList {
        if (time.isNotBlank()) add(time)
        if (lesson.classroom.isNotBlank()) add(lesson.classroom)
        if (lesson.teacherName.isNotBlank()) add(lesson.teacherName)
        if (lesson.isSubstitution) add("subst.")
    }.joinToString(" · ")

    BoxPill {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text            = title,
                    style           = MaterialTheme.typography.body2,
                    maxLines        = 1,
                    overflow        = TextOverflow.Ellipsis,
                    color           = if (lesson.isCancelled) MaterialTheme.colors.error
                                      else MaterialTheme.colors.onBackground,
                    textDecoration  = if (lesson.isCancelled) TextDecoration.LineThrough else null,
                )
                Text(
                    text     = details.ifBlank { "—" },
                    style    = MaterialTheme.typography.caption2,
                    color    = MaterialTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            lesson.period?.let {
                Text(
                    text  = it.toString(),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** Stadium-shaped, non-interactive row used for display-only content. */
@Composable
fun BoxPill(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        content()
    }
}
