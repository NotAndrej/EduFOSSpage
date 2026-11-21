package org.edufosspage.wear.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
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
import org.edufosspage.R
import org.edufosspage.data.EduGrade
import org.edufosspage.data.SubjectGrades
import org.edufosspage.ui.grades.GradesViewModel
import org.edufosspage.wear.InfoBlock
import org.edufosspage.wear.LoadingRow
import org.edufosspage.wear.RefreshChip
import org.edufosspage.wear.SectionHeader
import java.util.Locale

private fun fmt(value: Float): String = String.format(Locale.US, "%.2f", value)

@Composable
fun GradesScreen(onBack: () -> Unit, vm: GradesViewModel = viewModel()) {
    val subjects by vm.grades.collectAsState()
    val refreshing by vm.isRefreshing.collectAsState()
    val error by vm.error.collectAsState()
    val term by vm.selectedTerm.collectAsState()

    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = subjects?.firstOrNull { it.subjectId == selectedId }
    if (selected != null) {
        GradeDetailScreen(subject = selected, onBack = { selectedId = null })
        return
    }

    val subjectList = subjects
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText          = { TimeText() },
        positionIndicator = { PositionIndicator(listState) },
    ) {
        ScalingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { SectionHeader(stringResource(R.string.wear_title_grades), onBack, trailing = { RefreshChip(refreshing) { vm.refresh() } }) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TermChip("P1", term == "P1") { vm.selectTerm("P1") }
                    TermChip("P2", term == "P2") { vm.selectTerm("P2") }
                }
            }

            when {
                subjectList == null -> item {
                    if (error) InfoBlock(
                        stringResource(R.string.wear_grades_error_title),
                        stringResource(R.string.wear_error_check_connection),
                        isError = true,
                    )
                    else LoadingRow(stringResource(R.string.wear_grades_loading))
                }

                subjectList.isEmpty() -> item {
                    InfoBlock(
                        stringResource(R.string.wear_grades_empty_title),
                        stringResource(R.string.wear_grades_empty_detail),
                    )
                }

                else -> items(subjectList) { subject ->
                    SubjectChip(subject) { selectedId = subject.subjectId }
                }
            }
        }
    }
}

@Composable
private fun TermChip(label: String, selected: Boolean, onClick: () -> Unit) {
    CompactChip(
        onClick = onClick,
        label   = { Text(label) },
        colors  = if (selected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
    )
}

@Composable
private fun SubjectChip(subject: SubjectGrades, onClick: () -> Unit) {
    val average = subject.average
    Chip(
        label = {
            Text(
                subject.subjectShort.ifBlank { subject.subjectName },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        secondaryLabel = {
            val count = subject.grades.size
            val countLabel = pluralStringResource(R.plurals.wear_grade_count, count, count)
            Text(
                if (average != null) {
                    "$countLabel · ${stringResource(R.string.wear_grades_avg, fmt(average))}"
                } else {
                    countLabel
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GradeDetailScreen(subject: SubjectGrades, onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText          = { TimeText() },
        positionIndicator = { PositionIndicator(listState) },
    ) {
        ScalingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { SectionHeader(subject.subjectShort.ifBlank { subject.subjectName }, onBack) }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        subject.subjectName,
                        style     = MaterialTheme.typography.caption1,
                        color     = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    subject.average?.let {
                        Text(
                            stringResource(R.string.wear_grades_average, fmt(it)),
                            style = MaterialTheme.typography.title3,
                            color = MaterialTheme.colors.primary,
                        )
                    }
                }
            }

            items(subject.grades) { grade -> GradeRow(grade) }
        }
    }
}

@Composable
private fun GradeRow(grade: EduGrade) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    grade.title,
                    style    = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    grade.date,
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
            Text(
                grade.gradeValue,
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.onSurface,
            )
        }
    }
}
