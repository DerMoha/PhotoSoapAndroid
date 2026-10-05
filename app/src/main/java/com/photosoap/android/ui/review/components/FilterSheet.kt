package com.photosoap.android.ui.review.components

import com.photosoap.android.domain.model.SmartAlbum

import com.photosoap.android.domain.model.ReviewProgress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.photosoap.android.ui.review.titleResource
import com.photosoap.android.R
import com.photosoap.android.domain.model.MediaKind
import com.photosoap.android.domain.model.ReviewFilter
import com.photosoap.android.domain.model.SortOrder
import com.photosoap.android.domain.model.AlbumInfo
import java.time.YearMonth
import java.time.format.TextStyle
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    smartCounts: Map<SmartAlbum, Int>,
    progress: Map<YearMonth, ReviewProgress>,
    isLoading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    hideFavorites: Boolean,
    onHideFavoritesChange: (Boolean) -> Unit,
    selectedKind: MediaKind,
    selectedSort: SortOrder,
    selectedFilter: ReviewFilter,
    selectedYear: Int?,
    albums: List<AlbumInfo>,
    years: List<Int>,
    months: List<YearMonth>,
    onKindSelected: (MediaKind) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onFilterSelected: (ReviewFilter) -> Unit,
    onYearSelected: (Int) -> Unit,
    onDeselectYear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.filter_media_type),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (hasError) {
                Text(stringResource(R.string.filter_load_error))
                androidx.compose.material3.TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.filter_hide_favorites), Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = hideFavorites, onCheckedChange = onHideFavoritesChange)
                }
                Text(stringResource(R.string.filter_hide_favorites_help), style = MaterialTheme.typography.bodySmall)
            } else {
                Text(stringResource(R.string.filter_favorites_unavailable), style = MaterialTheme.typography.bodySmall)
            }
            MediaKind.entries.forEach { kind ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = selectedKind == kind, role = Role.RadioButton, onClick = { onKindSelected(kind) })
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedKind == kind,
                        onClick = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            when (kind) {
                                MediaKind.PHOTOS -> R.string.filter_photos
                                MediaKind.VIDEOS -> R.string.filter_videos
                                MediaKind.ALL -> R.string.filter_all_media
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.filter_sort_order),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SortOrder.entries.forEach { order ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = selectedSort == order, role = Role.RadioButton, onClick = { onSortSelected(order) })
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedSort == order,
                        onClick = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            when (order) {
                                SortOrder.NEWEST_FIRST -> R.string.filter_sort_newest
                                SortOrder.OLDEST_FIRST -> R.string.filter_sort_oldest
                                SortOrder.SHUFFLED -> R.string.filter_sort_shuffle
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.filter_browse),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!isLoading && !hasError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFilterSelected(ReviewFilter.All) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedFilter is ReviewFilter.All,
                    onClick = { onFilterSelected(ReviewFilter.All) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(when (selectedKind) { MediaKind.PHOTOS -> R.string.filter_all_images; MediaKind.VIDEOS -> R.string.filter_all_videos; MediaKind.ALL -> R.string.filter_all_photos }),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = selectedYear != null) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = onDeselectYear) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                        Text(
                            text = selectedYear.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (months.isEmpty()) Text(stringResource(R.string.filter_no_months), style = MaterialTheme.typography.bodySmall)
                    Column {
                        (if (selectedSort == SortOrder.OLDEST_FIRST) months.sorted() else months.sortedDescending()).forEach { month ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFilterSelected(ReviewFilter.Month(month.year, month.monthValue)) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = selectedFilter is ReviewFilter.Month &&
                                        selectedFilter.year == month.year &&
                                        selectedFilter.month == month.monthValue,
                                    onClick = {
                                        onFilterSelected(
                                            ReviewFilter.Month(month.year, month.monthValue)
                                        )
                                    },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(month.month.getDisplayName(TextStyle.FULL, locale), style = MaterialTheme.typography.bodyLarge)
                                    progress[month]?.let { FilterProgress(it) }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (selectedYear == null) {
                Text(
                    text = stringResource(R.string.filter_years),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (years.isEmpty()) Text(stringResource(R.string.filter_no_years), style = MaterialTheme.typography.bodySmall)
                Column {
                    (if (selectedSort == SortOrder.OLDEST_FIRST) years.sorted() else years.sortedDescending()).forEach { year ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onYearSelected(year) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedFilter is ReviewFilter.Year && selectedFilter.year == year,
                                onClick = { onFilterSelected(ReviewFilter.Year(year)) },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(text = year.toString(), style = MaterialTheme.typography.bodyLarge)
                                val values = progress.filterKeys { it.year == year }.values
                                FilterProgress(ReviewProgress(values.sumOf { it.reviewed }, values.sumOf { it.total }))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.filter_albums),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (albums.isEmpty()) Text(stringResource(R.string.filter_no_albums), style = MaterialTheme.typography.bodySmall)
                Column {
                    albums.forEach { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onFilterSelected(
                                        ReviewFilter.Album(album.id, album.name)
                                    )
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedFilter is ReviewFilter.Album && selectedFilter.albumId == album.id,
                                onClick = {
                                    onFilterSelected(
                                        ReviewFilter.Album(album.id, album.name)
                                    )
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = album.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.album_item_count,
                                        album.count,
                                        album.count,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Text(stringResource(R.string.filter_smart_albums), style = MaterialTheme.typography.titleSmall)
            smartCounts.filterValues { it > 0 }.forEach { (kind, count) ->
                Row(Modifier.fillMaxWidth().selectable(selected = selectedFilter == ReviewFilter.Smart(kind), role = Role.RadioButton,
                    onClick = { onFilterSelected(ReviewFilter.Smart(kind)) }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedFilter == ReviewFilter.Smart(kind), onClick = null)
                    Text(stringResource(kind.titleResource), Modifier.weight(1f))
                    Text(count.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (smartCounts.values.none { it > 0 }) Text(stringResource(R.string.filter_no_albums), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.filter_smart_help), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FilterProgress(progress: ReviewProgress) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
        Text(stringResource(R.string.filter_reviewed_count, progress.reviewed, progress.total), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(java.text.NumberFormat.getPercentInstance().format(progress.displayFraction.toDouble()) + if (progress.isComplete) " ✓" else "", style = MaterialTheme.typography.labelLarge)
    }
    androidx.compose.material3.LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
}
