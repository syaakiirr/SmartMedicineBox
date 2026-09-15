package com.example.smartmedicinebox

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartmedicinebox.data.model.Medicine
import com.example.smartmedicinebox.data.model.UserRole
import com.example.smartmedicinebox.notification.NotificationHelper
import com.example.smartmedicinebox.ui.screens.*
import com.example.smartmedicinebox.ui.theme.SmartMedicineBoxTheme
import com.example.smartmedicinebox.ui.viewmodel.MedicineViewModel

// Nav routes
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Schedule  : Screen("schedule", "Schedule", Icons.Default.DateRange)
    object History   : Screen("history", "History", Icons.Default.History)
    object Caregiver : Screen("caregiver", "Caregiver", Icons.Default.Person)
    object Login     : Screen("login", "Login", Icons.Default.Home)
    object AddMedicine  : Screen("add_medicine", "Add Medicine", Icons.Default.Home)
    object EditMedicine : Screen("edit_medicine", "Edit Medicine", Icons.Default.Home)
}

val patientNavItems = listOf(
    Screen.Dashboard,
    Screen.Schedule,
    Screen.History
)

val caregiverNavItems = listOf(
    Screen.Caregiver,
    Screen.Schedule,
    Screen.History
)

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            SmartMedicineBoxTheme {
                SmartMedicineBoxApp(context = this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMedicineBoxApp(context: Context) {
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val isLoggedIn = prefs.getBoolean("is_logged_in", false)
    val userName = prefs.getString("user_name", "") ?: ""
    val savedRole = runCatching {
        UserRole.valueOf(prefs.getString("user_role", UserRole.USER.name) ?: UserRole.USER.name)
    }.getOrDefault(UserRole.USER)

    val navController = rememberNavController()
    val viewModel: MedicineViewModel = viewModel()
    var userRole by remember { mutableStateOf(savedRole) }

    // Track selected medicine for edit
    var medicineToEdit by remember { mutableStateOf<Medicine?>(null) }

    val roleNavItems = if (userRole == UserRole.CAREGIVER) caregiverNavItems else patientNavItems
    val roleHomeRoute = roleNavItems.first().route
    val startDestination = if (isLoggedIn) roleHomeRoute else Screen.Login.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in roleNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    roleNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    context = context,
                    onLoginSuccess = { selectedRole ->
                        userRole = selectedRole
                        val destination = if (selectedRole == UserRole.CAREGIVER) {
                            Screen.Caregiver.route
                        } else {
                            Screen.Dashboard.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                val savedUserName = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .getString("user_name", "") ?: ""
                DashboardScreen(
                    viewModel = viewModel,
                    userName = savedUserName
                )
            }

            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    viewModel = viewModel,
                    onAddMedicine = {
                        medicineToEdit = null
                        navController.navigate(Screen.AddMedicine.route)
                    },
                    onEditMedicine = { medicine ->
                        medicineToEdit = medicine
                        navController.navigate(Screen.EditMedicine.route)
                    }
                )
            }

            composable(Screen.AddMedicine.route) {
                AddEditMedicineScreen(
                    viewModel = viewModel,
                    existingMedicine = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.EditMedicine.route) {
                AddEditMedicineScreen(
                    viewModel = viewModel,
                    existingMedicine = medicineToEdit,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(viewModel = viewModel)
            }

            composable(Screen.Caregiver.route) {
                CaregiverScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
