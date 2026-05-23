package com.photosoap.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable
    data object Main : AppDestination

    @Serializable
    data object Settings : AppDestination

    @Serializable
    data object DeveloperOptions : AppDestination
}
