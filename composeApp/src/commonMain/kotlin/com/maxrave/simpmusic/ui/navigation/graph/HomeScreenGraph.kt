package com.maxrave.simpmusic.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.maxrave.simpmusic.ui.navigation.destination.home.AppLogDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.CreditDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherSettingsDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.MoodDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.SettingsDestination
import com.maxrave.simpmusic.ui.screen.home.ListenTogetherScreen
import com.maxrave.simpmusic.ui.screen.home.ListenTogetherSettingsScreen
import com.maxrave.simpmusic.ui.screen.home.MoodScreen
import com.maxrave.simpmusic.ui.screen.home.NotificationScreen
import com.maxrave.simpmusic.ui.screen.home.SettingScreen
import com.maxrave.simpmusic.ui.screen.other.AppLogScreen
import com.maxrave.simpmusic.expect.ui.RootBackdropSurface
import com.maxrave.simpmusic.ui.screen.other.CreditScreen

fun NavGraphBuilder.homeScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<CreditDestination> {
        RootBackdropSurface {
            CreditScreen(
                paddingValues = innerPadding,
                navController = navController,
            )
        }
    }
    composable<AppLogDestination> {
        RootBackdropSurface {
            AppLogScreen(
                paddingValues = innerPadding,
                navController = navController,
            )
        }
    }
    composable<MoodDestination> { entry ->
        val params = entry.toRoute<MoodDestination>().params
        RootBackdropSurface {
            MoodScreen(
                navController = navController,
                params = params,
            )
        }
    }
    composable<ListenTogetherDestination> {
        RootBackdropSurface {
            ListenTogetherScreen(
                navController = navController,
                innerPadding = innerPadding,
            )
        }
    }
    composable<ListenTogetherSettingsDestination> {
        RootBackdropSurface {
            ListenTogetherSettingsScreen(
                navController = navController,
                innerPadding = innerPadding,
            )
        }
    }
    composable<NotificationDestination> {
        RootBackdropSurface {
            NotificationScreen(
                navController = navController,
            )
        }
    }
    composable<SettingsDestination> {
        RootBackdropSurface {
            SettingScreen(
                navController = navController,
                innerPadding = innerPadding,
            )
        }
    }
}