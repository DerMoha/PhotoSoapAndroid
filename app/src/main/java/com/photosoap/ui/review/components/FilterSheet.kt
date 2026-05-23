package com.photosoap.ui.review.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.photosoap.R
import com.photosoap.domain.model.AlbumInfo
import com.photosoap.domain.model.PhotoFilter
import com.photosoap.domain.model.ReviewMediaKind
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    currentMediaKind: ReviewMediaKind,
    currentFilter: PhotoFilter,
    sortNewestFirst: Boolean,
    albums: List<AlbumInfo>,
    years: List<Int>,
    onMediaKindChange: (ReviewMediaKind) -> Unit,
    onFilterChange: (PhotoFilter) -> Unit,
    onSortChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.filter_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Media kind segmented control
            Text(
                text = "Media Type",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            var selectedKind by remember {
                mutableIntStateOf(
                    when (currentMediaKind) {
                        ReviewMediaKind.All -> 0
                        ReviewMediaKind.Photos -> 1
                        ReviewMediaKind.Videos -> 2
                    }
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedKind == 0,
                    onClick = {
                        selectedKind = 0
                        onMediaKindChange(ReviewMediaKind.All)
                    },
                ) {
                    Text(stringResource(R.string.filter_all_media))
                }
                SegmentedButton(
                    selected = selectedKind == 1,
                    onClick = {
                        selectedKind = 1
                        onMediaKindChange(ReviewMediaKind.Photos)
                    },
                ) {
                    Text(stringResource(R.string.filter_photos))
                }
                SegmentedButton(
                    selected = selectedKind == 2,
                    onClick = {
                        selectedKind = 2
                        onMediaKindChange(ReviewMediaKind.Videos)
                    },
                ) {
                    Text(stringResource(R.string.filter_videos))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort order
            Text(
                text = "Sort Order",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            var sortNewest by remember { mutableStateOf(sortNewestFirst) }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = sortNewest,
                    onClick = {
                        sortNewest = true
                        onSortChange(true)
                    },
                ) {
                    Text(stringResource(R.string.filter_sort_newest))
                }
                SegmentedButton(
                    selected = !sortNewest,
                    onClick = {
                        sortNewest = false
                        onSortChange(false)
                    },
                ) {
                    Text(stringResource(R.string.filter_sort_oldest))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // All Photos option
            val isAllPhotos = currentFilter is PhotoFilter.All
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isAllPhotos,
                        onClick = { onFilterChange(PhotoFilter.All) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isAllPhotos, onClick = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.filter_all_photos),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            HorizontalDivider()

            // Years
            if (years.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.filter_years),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(years) { year ->
                        val isSelected = currentFilter is PhotoFilter.Year && currentFilter.year == year
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    onClick = { onFilterChange(PhotoFilter.Year(year)) },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = year.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }

            // Albums
            if (albums.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.filter_albums),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(albums) { album ->
                        val isSelected = currentFilter is PhotoFilter.Album && currentFilter.id == album.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    onClick = { onFilterChange(PhotoFilter.Album(album.id, album.title)) },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = album.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "${album.count} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Done button
            Spacer(modifier = Modifier.height(16.dp))
            if (false) { // Not shown since selections are applied immediately
                // Selection is applied immediately via callbacks
            }
        }
    }
}
