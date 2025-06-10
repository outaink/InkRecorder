package com.outaink.inkrecorder.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

private sealed interface TopLevelRoute {
    val icon: ImageVector
}

private data object Recorder
private data object AllAudio: TopLevelRoute {
    override val icon = Icons.Rounded.FolderOpen
}
private data object Favorite: TopLevelRoute { override val icon = Icons.Rounded.Favorite }

private val TOP_LEVEL_ROUTES : List<TopLevelRoute> = listOf(AllAudio, Favorite)

@Composable
fun NavigationRoot() {
    val backStack = reme
}