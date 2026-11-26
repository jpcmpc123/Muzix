package com.maxrave.simpmusic.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryHistoryDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistScreen
import com.maxrave.simpmusic.expect.ui.RootBackdropSurface
import com.maxrave.simpmusic.ui.screen.library.LibraryHistoryScreen

@ExperimentalMaterial3Api
fun NavGraphBuilder.libraryScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<LibraryDynamicPlaylistDestination> { entry ->
        val data = entry.toRoute<LibraryDynamicPlaylistDestination>()
        RootBackdropSurface {
            LibraryDynamicPlaylistScreen(
                innerPadding = innerPadding,
                navController = navController,
                type = data.type,
            )
        }
    }
    composable<LibraryHistoryDestination> {
        RootBackdropSurface {
            LibraryHistoryScreen(
                innerPadding = innerPadding,
                navController = navController,
            )
        }
    }
}