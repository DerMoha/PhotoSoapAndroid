package com.photosoap.android.ui.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.SubcomposeAsyncImage
import com.photosoap.android.R
import com.photosoap.android.domain.model.Photo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPreviewSheet(
    photo: Photo,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var scale by remember(photo.uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(photo.uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(photo.uri) { mutableFloatStateOf(0f) }
    val gestureModifier = if (photo.isVideo) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (scale * zoom).coerceIn(1f, 5f)
                scale = newScale
                if (newScale > 1f) {
                    val maxX = (size.width * (newScale - 1f)) / 2f
                    val maxY = (size.height * (newScale - 1f)) / 2f
                    offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                    offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                } else {
                    offsetX = 0f
                    offsetY = 0f
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Black,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .then(gestureModifier),
            contentAlignment = Alignment.Center,
        ) {
            if (photo.isVideo) {
                VideoPreview(photo = photo)
            } else {
                SubcomposeAsyncImage(
                    model = photo.uri,
                    contentDescription = photo.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    contentScale = ContentScale.Fit,
                    loading = {
                        PreviewPlaceholder(isError = false)
                    },
                    error = {
                        PreviewPlaceholder(isError = true)
                    },
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp),
            ) {
                Text(
                    text = photo.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${photo.width} × ${photo.height}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(24.dp),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun VideoPreview(photo: Photo) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var hasError by remember(photo.uri) { mutableStateOf(false) }
    val player = remember(photo.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(photo.uri))
            prepare()
            playWhenReady = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
    }

    DisposableEffect(player, lifecycle) {
        var resumePlayback = true
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { hasError = true }
        }
        player.addListener(listener)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    resumePlayback = player.playWhenReady
                    player.pause()
                }
                Lifecycle.Event.ON_RESUME -> if (resumePlayback && !hasError) player.play()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (player.playerError != null) hasError = true
        onDispose {
            lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }

    if (hasError) {
        PreviewPlaceholder(isError = true)
        return
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                contentDescription = viewContext.getString(R.string.video)
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun PreviewPlaceholder(isError: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (isError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = Color.White.copy(alpha = 0.75f),
                )
                Text(
                    text = stringResource(R.string.media_preview_unavailable),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
