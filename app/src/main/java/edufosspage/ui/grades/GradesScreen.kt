package org.edufosspage.ui.grades

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.edufosspage.R
import org.edufosspage.data.EduGrade
import org.edufosspage.data.SubjectGrades
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(vm: GradesViewModel = viewModel()) {
    val grades       by vm.grades.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val error        by vm.error.collectAsState()
    val refreshError by vm.refreshError.collectAsState()
    val selectedYear by vm.selectedYear.collectAsState()
    val selectedTerm by vm.selectedTerm.collectAsState()
    val availYears   by vm.availableYears.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }
    val refreshErrorMsg = stringResource(R.string.msg_grades_refresh_error)

    LaunchedEffect(refreshError) {
        if (refreshError) {
            snackbarHost.showSnackbar(refreshErrorMsg, duration = SnackbarDuration.Short)
            vm.clearRefreshError()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            GradesToolbar(
                selectedYear = selectedYear,
                selectedTerm = selectedTerm,
                availYears   = availYears,
                onYearSelect = { vm.selectYear(it) },
                onTermSelect = { vm.selectTerm(it) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when {
                grades == null && !error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.msg_grades_load_error), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = { vm.refresh() }) {
                            Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.msg_retry))
                        }
                    }
                }
                else -> PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
                    val currentGrades = grades ?: emptyList()
                    if (currentGrades.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(stringResource(R.string.msg_grades_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier            = Modifier.fillMaxSize()
                        ) {
                            items(currentGrades, key = { it.subjectId }) { subject -> SubjectCard(subject) }
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun GradesToolbar(
    selectedYear: Int,
    selectedTerm: String,
    availYears:   List<Pair<Int, String>>,
    onYearSelect: (Int) -> Unit,
    onTermSelect: (String) -> Unit,
) {
    var showYearMenu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box {
            OutlinedButton(onClick = { showYearMenu = true }, shape = MaterialTheme.shapes.large, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                Text("$selectedYear/${selectedYear + 1}", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
            }
            DropdownMenu(expanded = showYearMenu, onDismissRequest = { showYearMenu = false }) {
                availYears.forEach { (year, label) ->
                    DropdownMenuItem(
                        text        = { Text(label, fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal) },
                        onClick     = { onYearSelect(year); showYearMenu = false },
                        leadingIcon = if (year == selectedYear) {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null
                    )
                }
            }
        }
        SingleChoiceSegmentedButtonRow {
            listOf("P1" to stringResource(R.string.msg_term_first), "P2" to stringResource(R.string.msg_term_second)).forEachIndexed { idx, (term, label) ->
                SegmentedButton(
                    selected = selectedTerm == term,
                    onClick  = { onTermSelect(term) },
                    shape    = SegmentedButtonDefaults.itemShape(idx, 2),
                    label    = { Text(label, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
}

@Composable
private fun SubjectCard(subject: SubjectGrades) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.weight(1f)
                ) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            subject.subjectShort,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(subject.subjectName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Average chip, neutral color, no grading scale assumptions
                    subject.average?.let { avg ->
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            val label = if (subject.isPercentSubject) "⌀ ${"%.0f".format(avg)}%" else "⌀ ${"%.1f".format(avg)}"
                            Text(
                                label,
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 4.dp))
                    subject.grades.forEach { grade -> GradeRow(grade) }
                }
            }
        }
    }
}

@Composable
private fun GradeRow(grade: EduGrade) {
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.")
    val dateStr = runCatching { LocalDate.parse(grade.date).format(dateFmt) }.getOrElse { grade.date.take(5) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Neutral grade bubble, no color coding
        Surface(
            shape    = CircleShape,
            color    = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(38.dp)
        ) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    if (grade.gradeType == 3) "${grade.gradeValue}%" else grade.gradeValue,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines   = 1
                )
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(grade.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaChip(dateStr)
                grade.importance?.let { MetaChip("⚖ ${"%.0f".format(it)}×") }
                grade.maxPoints?.let  { MetaChip("/ ${"%.0f".format(it)}b") }
                grade.classAvg?.let   { MetaChip("⌀ ${"%.2f".format(it)}") }
                grade.percent?.let    { MetaChip("${"%.0f".format(it)}%") }
            }
            if (grade.teacherName.isNotBlank()) {
                Text(grade.teacherName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
}
