package com.example.parisaracycle.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parisaracycle.data.AppContainer
import com.example.parisaracycle.ui.components.AppBottomBar
import com.example.parisaracycle.ui.components.AppDestination
import com.example.parisaracycle.ui.screens.LoginScreen
import com.example.parisaracycle.ui.screens.MapScreen
import com.example.parisaracycle.ui.screens.ProfileScreen
import com.example.parisaracycle.ui.screens.StatsScreen
import com.example.parisaracycle.ui.theme.ParisaraCycleTheme
import com.example.parisaracycle.viewmodel.AuthViewModel
import com.example.parisaracycle.viewmodel.MapViewModel
import com.example.parisaracycle.viewmodel.StatsViewModel

@Composable
fun ParisaraCycleApp(container: AppContainer) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(container.authRepository)
    )
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    ParisaraCycleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val user = authState.user
            if (user == null) {
                LoginScreen(
                    uiState = authState,
                    onSignIn = authViewModel::signIn,
                    onRegister = authViewModel::register,
                    onClearError = authViewModel::clearError
                )
                return@Surface
            }

            val mapViewModel: MapViewModel = viewModel(
                factory = MapViewModel.factory(
                    locationRepository = container.locationRepository,
                    directionsRepository = container.directionsRepository,
                    dangerZoneRepository = container.dangerZoneRepository,
                    pitStopRepository = container.pitStopRepository,
                    buddyLocationRepository = container.buddyLocationRepository,
                    ecoStatsRepository = container.ecoStatsRepository
                )
            )
            val statsViewModel: StatsViewModel = viewModel(
                factory = StatsViewModel.factory(container.ecoStatsRepository)
            )
            val mapState by mapViewModel.uiState.collectAsStateWithLifecycle()
            var selectedDestination by rememberSaveable { mutableStateOf(AppDestination.Map) }

            LaunchedEffect(user.uid) {
                mapViewModel.setActiveUser(user.uid)
            }

            Scaffold(
                bottomBar = {
                    AppBottomBar(
                        selected = selectedDestination,
                        onSelected = { selectedDestination = it }
                    )
                }
            ) { innerPadding: PaddingValues ->
                when (selectedDestination) {
                    AppDestination.Map -> MapScreen(
                        viewModel = mapViewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )

                    AppDestination.Stats -> StatsScreen(
                        viewModel = statsViewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )

                    AppDestination.Profile -> ProfileScreen(
                        user = user,
                        mapUiState = mapState,
                        onLocationPermissionChanged = mapViewModel::setLocationPermissionGranted,
                        onSharingChanged = mapViewModel::setSharingLocation,
                        onSignOut = {
                            mapViewModel.setSharingLocation(false)
                            authViewModel.signOut()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}
