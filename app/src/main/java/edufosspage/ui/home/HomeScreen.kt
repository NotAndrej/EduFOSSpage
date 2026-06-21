package org.edufosspage.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.edufosspage.data.DashGrade
import org.edufosspage.data.DashMessage
import org.edufosspage.data.EduSession
import org.edufosspage.data.Lesson
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel = viewModel(),
    onNavigateToTab: (Int) -> Unit = {}
) {
    val lessons      by vm.todayLessons.collectAsState()
    val messages     by vm.messages.collectAsState()
    val grades       by vm.grades.collectAsState()
    val loading      by vm.loading.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val displayDate  by vm.displayDate.collectAsState()

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = LocalTime.now()
        }
    }

    LaunchedEffect(Unit) { vm.loadDashboard() }

    if (loading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh    = { vm.refreshDashboard() },
        modifier     = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WelcomeCard(lessons, displayDate, now)

            if (displayDate != LocalDate.now()) {
                Text(
                    "No lessons today, showing ${
                        displayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    }",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            TimetableCard(lessons ?: emptyList(), displayDate, now)
            if (grades.isNotEmpty())   GradesCard(grades,   onClick = { onNavigateToTab(2) })
            MessagesCard(messages, onClick = { onNavigateToTab(3) })
        }
    }
}

@Composable
private fun WelcomeCard(lessons: List<Lesson>?, displayDate: LocalDate, now: LocalTime) {
    val isToday       = displayDate == LocalDate.now()
    val currentLesson = if (isToday) lessons?.firstOrNull {
        val s = it.startTime.toTime(); val e = it.endTime.toTime()
        s != null && e != null && now >= s && now <= e
    } else null
    val nextLesson = when {
        // Normal case: today, no ongoing lesson → find the next upcoming one
        isToday && currentLesson == null -> lessons?.firstOrNull {
            it.startTime.toTime()?.let { t -> t > now } == true
        }
        // Day has advanced past today → first lesson of the next day is always NEXT
        !isToday -> lessons?.firstOrNull()
        else     -> null
    }
    val displayLesson = currentLesson ?: nextLesson

    Surface(
        shape    = RoundedCornerShape(28.dp),
        color    = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Welcome ${EduSession.userFullName.ifBlank { "Student" }}!",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    buildString {
                        if (EduSession.subdomain.isNotBlank()) append(EduSession.subdomain)
                        if (EduSession.userClass.isNotBlank()) append(" - ${EduSession.userClass}")
                    }.ifBlank { "EduFOSSpage" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }
            displayLesson?.let { lesson ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (currentLesson != null) "NOW" else "NEXT",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        "${lesson.period}. period",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (currentLesson != null) lesson.endTime else lesson.startTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetableCard(lessons: List<Lesson>, displayDate: LocalDate, now: LocalTime) {
    val fmt = DateTimeFormatter.ofPattern("dd.MM.")
    Surface(
        shape    = RoundedCornerShape(28.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Timetable for ${displayDate.format(fmt)}",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (lessons.isEmpty()) {
                Text(
                    "No lessons",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val isToday = displayDate == LocalDate.now()
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lessons.forEach { lesson ->
                        val start = lesson.startTime.toTime()
                        val end   = lesson.endTime.toTime()
                        val isNow = isToday && start != null && end != null && now >= start && now <= end

                        val cellColor = when {
                            isNow                 -> MaterialTheme.colorScheme.primary
                            lesson.isCancelled    -> MaterialTheme.colorScheme.errorContainer
                            lesson.isSubstitution -> MaterialTheme.colorScheme.tertiaryContainer
                            else                  -> MaterialTheme.colorScheme.surface
                        }
                        val cellText = when {
                            isNow                 -> MaterialTheme.colorScheme.onPrimary
                            lesson.isCancelled    -> MaterialTheme.colorScheme.onErrorContainer
                            lesson.isSubstitution -> MaterialTheme.colorScheme.onTertiaryContainer
                            else                  -> MaterialTheme.colorScheme.onSurface
                        }
                        Surface(
                            shape         = RoundedCornerShape(16.dp),
                            color         = cellColor,
                            tonalElevation = if (isNow) 0.dp else 1.dp
                        ) {
                            Column(
                                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${lesson.period}.", style = MaterialTheme.typography.labelSmall,  color = cellText.copy(alpha = 0.6f))
                                Text(lesson.subjectShort, style = MaterialTheme.typography.labelLarge,  fontWeight = FontWeight.Bold, color = cellText)
                                Text(lesson.startTime,    style = MaterialTheme.typography.labelSmall,  color = cellText.copy(alpha = 0.6f))
                                Text(lesson.endTime,      style = MaterialTheme.typography.labelSmall,  color = cellText.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradesCard(grades: List<DashGrade>, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(28.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(48.dp), Alignment.Center) {
                    Text(
                        "A+",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Grades", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                // Group by relative date, explicit comparator so "Today"/"Yesterday"
                // are never both mapped to 0 by toIntOrNull().
                val grouped = grades
                    .groupBy { grade ->
                        runCatching {
                            val d    = LocalDate.parse(grade.date)
                            val days = java.time.temporal.ChronoUnit.DAYS.between(d, LocalDate.now())
                            when {
                                days == 0L -> "Today"
                                days == 1L -> "Yesterday"
                                else       -> "${days}d ago"
                            }
                        }.getOrElse { grade.date }
                    }
                    .entries
                    .sortedWith(compareBy {
                        when (it.key) {
                            "Today"     -> 0
                            "Yesterday" -> 1
                            else        -> it.key.removeSuffix("d ago").toIntOrNull() ?: Int.MAX_VALUE
                        }
                    })

                grouped.forEach { (label, gs) ->
                    Text(
                        "$label: ${gs.joinToString(", ") { it.grade }}",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MessagesCard(messages: List<DashMessage>, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(28.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(48.dp), Alignment.Center) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Messages", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                if (messages.isEmpty()) {
                    Text(
                        "No messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    messages.take(3).forEach { msg ->
                        Text(
                            "${msg.sender}: ${msg.preview}",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun String.toTime(): LocalTime? = runCatching {
    val p = split(":"); LocalTime.of(p[0].toInt(), p[1].toInt())
}.getOrNull()
