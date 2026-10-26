package com.seldon.fleet.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.seldon.fleet.presentation.detail.DeviceDetailScreen
import com.seldon.fleet.presentation.fleet.FleetListScreen

@Composable
fun SeldonNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = FleetRoute,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 260)) +
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 300),
                    initialOffsetX = { width -> width / 10 },
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(durationMillis = 160))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 220)) +
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 280),
                    initialOffsetX = { width -> -width / 12 },
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(durationMillis = 180)) +
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = 260),
                    targetOffsetX = { width -> width / 10 },
                )
        },
    ) {
        composable<FleetRoute> {
            FleetListScreen(
                onOpenDevice = { deviceId ->
                    navController.navigate(DeviceDetailRoute(deviceId)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<DeviceDetailRoute> {
            DeviceDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
