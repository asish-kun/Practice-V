package com.example.practicev

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.MarkerOptions
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView

// Sealed class for Bottom Navigation Tabs
sealed class DetailsTab(val route: String, val icon: ImageVector, val label: String) {
    object LocationDetails : DetailsTab("location_details", Icons.Filled.Info, "Details")
    object MapView : DetailsTab("map_view", Icons.Filled.Map, "Map")
}

// PlaceDetailsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailsScreen(place: Place) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val tabs = listOf(DetailsTab.LocationDetails, DetailsTab.MapView)

                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        content = { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = DetailsTab.LocationDetails.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(DetailsTab.LocationDetails.route) {
                    LocationDetailsTab(place)
                }
                composable(DetailsTab.MapView.route) {
                    MapViewTab(place)
                }
            }
        }
    )
}

// Location Details Tab
@Composable
fun LocationDetailsTab(place: Place) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = place.name, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = place.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Additional details can go here (e.g., address, hours of operation)
        Text(text = stringResource(id = R.string.coordinates))
        Text(text = stringResource(id = R.string.latitude_label, place.latitude))
        Text(text = stringResource(id = R.string.longitude_label, place.longitude))
    }
}

// Map View Tab
@Composable
fun MapViewTab(place: Place) {
    val mapView = rememberMapViewWithLifecycle()
    val northAmericaBounds = LatLngBounds(
        LatLng(5.0, -168.0), // Southwest corner of North America (approx. Panama region)
        LatLng(83.0, -52.0)  // Northeast corner of North America (approx. Greenland region)
    )
    var isCameraAnimated by remember { mutableStateOf(false) } // Track if animation has already occurred

    AndroidView({ mapView }) { mapView ->
        mapView.getMapAsync { googleMap ->
            // Map Settings
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.uiSettings.isScrollGesturesEnabled = true

            // Set initial view to show the whole North America
            if (!isCameraAnimated) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(northAmericaBounds, 0))

                // Add a marker for the designated location
                val location = LatLng(place.latitude, place.longitude)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title(place.name)
                        .snippet(place.description)
                )

                // Animate the camera to the specific location
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f), 2000, null)
                isCameraAnimated = true // Mark animation as completed
            }
        }
    }
}

// Helper to manage MapView lifecycle
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    val lifecycleObserver = rememberMapLifecycleObserver(mapView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

@Composable
fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver {
    return remember {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
    }
}
