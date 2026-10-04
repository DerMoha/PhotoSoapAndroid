package com.photosoap.android.ui.review.components

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
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

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
                    text = stringResource(R.string.filter_all_photos),
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
                    Column {
                        months.forEach { month ->
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
                                Text(
                                    text = month.month.getDisplayName(TextStyle.FULL, locale),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
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
                Column {
                    years.forEach { year ->
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
                            Text(text = year.toString(), style = MaterialTheme.typography.bodyLarge)
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
