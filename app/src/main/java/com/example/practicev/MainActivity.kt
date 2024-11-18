package com.example.practicev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import com.example.practicev.ui.theme.PracticeVTheme
import kotlinx.coroutines.launch
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PracticeVTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String) {
    object LocalEvents : Screen("local_events")
    object MyEvents : Screen("my_events")
    object Settings : Screen("settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    var showDialog by remember { mutableStateOf(false) }
    var places by remember { mutableStateOf(getSamplePlaces()) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f), // Drawer width (75% of screen)
                content = {
                    DrawerContent(
                        onItemSelected = { screen ->
                            coroutineScope.launch {
                                drawerState.close() // Close drawer before navigating
                            }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            )
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.app_name)) },
                        navigationIcon = {
                            IconButton(onClick = {
                                coroutineScope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Filled.Menu, contentDescription = stringResource(id = R.string.menu_title))
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            showDialog = true
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Place")
                    }
                },
                content = { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.LocalEvents.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.LocalEvents.route) {
                            LocalEventsScreen(places, navController)
                        }
                        composable(Screen.MyEvents.route) {
                            MyEventsScreen()
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen()
                        }
                        composable(
                            route = "place_details/{placeJson}",
                            arguments = listOf(navArgument("placeJson") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val placeJson = backStackEntry.arguments?.getString("placeJson")
                            val place = Gson().fromJson(placeJson, Place::class.java)
                            if (place != null) {
                                PlaceDetailsScreen(place)
                            } else {
                                Text("Place not found")
                            }
                        }
                    }

                    if (showDialog) {
                        AddPlaceDialog(
                            onDismiss = { showDialog = false },
                            onAddPlace = { place ->
                                places = places + place
                                showDialog = false
                            }
                        )
                    }
                }
            )
        }
    )
}


@Composable
fun DrawerContent(onItemSelected: (Screen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "Menu",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Navigation items
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.local_events)) },
            selected = false,
            onClick = { onItemSelected(Screen.LocalEvents) },
            icon = { Icon(Icons.Filled.Event, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.my_events)) },
            selected = false,
            onClick = { onItemSelected(Screen.MyEvents) },
            icon = { Icon(Icons.Filled.Star, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.settings)) },
            selected = false,
            onClick = { onItemSelected(Screen.Settings) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun LocalEventsScreen(places: List<Place>, navController: NavController) {
    PlacesList(
        places = places,
        navController = navController
    )
}

@Composable
fun MyEventsScreen() {
    // Implement your My Events Screen content
    Text(
        text = "My Events Screen",
        modifier = Modifier.fillMaxSize(),
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
fun SettingsScreen() {
    // Implement your Settings Screen content
    Text(
        text = "Settings Screen",
        modifier = Modifier.fillMaxSize(),
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
fun PlacesList(places: List<Place>, navController: NavController, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(places.size) { index ->
            val place = places[index]
            PlaceCard(
                place = place,
                onClick = {
                    val placeJson = Gson().toJson(place)
                    navController.navigate("place_details/$placeJson")
                }
            )
        }
    }
}


@Composable
fun PlaceCard(place: Place, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Image(
                painter = painterResource(id = place.imageResId),
                contentDescription = place.name,
                modifier = Modifier
                    .size(64.dp)
                    .padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = place.name, style = MaterialTheme.typography.titleMedium)
                Text(text = place.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}


data class Place(
    val name: String,
    val description: String,
    val imageResId: Int,
    val latitude: Double,
    val longitude: Double
)

fun getSamplePlaces(): List<Place> {
    return listOf(
        Place(
            name = "Central Park",
            description = "A large public park in the city center.",
            imageResId = R.drawable.museum_loc,
            latitude = 40.785091,
            longitude = -73.968285
        ),
        Place(
            name = "City Museum",
            description = "Explore the history of the city.",
            imageResId = R.drawable.museum_loc,
            latitude = 40.779437,
            longitude = -73.963244
        ),
        Place(
            name = "Downtown Cafe",
            description = "A cozy place to enjoy coffee.",
            imageResId = R.drawable.museum_loc,
            latitude = 40.730610,
            longitude = -73.935242
        ),
    )
}

@Composable
fun AddPlaceDialog(onDismiss: () -> Unit, onAddPlace: (Place) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.add_new_place)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.place_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.place_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text(stringResource(id = R.string.latitude)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text(stringResource(id = R.string.longitude)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && description.isNotBlank() && latitude.isNotBlank() && longitude.isNotBlank()) {
                    val newPlace = Place(
                        name = name,
                        description = description,
                        imageResId = R.drawable.museum_loc,
                        latitude = latitude.toDoubleOrNull() ?: 0.0,
                        longitude = longitude.toDoubleOrNull() ?: 0.0
                    )
                    onAddPlace(newPlace)
                    onDismiss()
                }
            }) {
                Text(stringResource(id = R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )

}


