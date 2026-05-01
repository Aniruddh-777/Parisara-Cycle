package com.example.parisaracycle.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(val label: String) {
    Map("Map"),
    Stats("Stats"),
    Profile("Profile")
}

@Composable
fun AppBottomBar(
    selected: AppDestination,
    onSelected: (AppDestination) -> Unit
) {
    NavigationBar {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

private val AppDestination.icon: ImageVector
    get() = when (this) {
        AppDestination.Map -> Icons.Default.Place
        AppDestination.Stats -> Icons.Default.Info
        AppDestination.Profile -> Icons.Default.AccountCircle
    }
