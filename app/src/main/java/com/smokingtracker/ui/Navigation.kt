package com.smokingtracker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smokingtracker.MainViewModel
import com.smokingtracker.R
import kotlinx.coroutines.delay

sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector) {
    object Registration : Screen("registration", R.string.registration_title, Icons.Filled.Home)
    object Home : Screen("home", R.string.nav_home, Icons.Filled.Home)
    object Graph : Screen("graph", R.string.nav_graph, Icons.Filled.BarChart)
    object Personal : Screen("personal", R.string.nav_personal, Icons.Filled.Settings)
    object About : Screen("about", R.string.about_app, Icons.Filled.Info)
    object Achievements : Screen("achievements", R.string.settings_achievements, Icons.Filled.EmojiEvents)
    object Statistics : Screen("statistics", R.string.settings_statistics, Icons.Filled.BarChart)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingWrapper(content: @Composable () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        val randomDelay = (100..300).random().toLong()
        delay(randomDelay)
        isLoading = false
    }
    
    Crossfade(
        targetState = isLoading,
        animationSpec = tween(500),
        label = "loading_crossfade"
    ) { loading ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(), 
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(64.dp)
                )
            }
        } else {
            content()
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val isRegistered by viewModel.isRegistered.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != Screen.Registration.route && 
                       currentRoute != Screen.About.route &&
                       currentRoute != Screen.Achievements.route &&
                       currentRoute != Screen.Statistics.route

    if (isRegistered == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val startDest = if (isRegistered == true) Screen.Home.route else Screen.Registration.route

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDest,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300))
                }
            ) {
                composable(Screen.Registration.route) { 
                    LoadingWrapper { RegistrationScreen(viewModel, navController) } 
                }
                composable(Screen.Home.route) { 
                    LoadingWrapper { HomeScreen(viewModel) } 
                }
                composable(Screen.Graph.route) { 
                    LoadingWrapper { GraphScreen(viewModel) } 
                }
                composable(Screen.Personal.route) { 
                    LoadingWrapper {
                        PersonalScreen(
                            viewModel = viewModel, 
                            onNavigateToAbout = { navController.navigate(Screen.About.route) },
                            onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                            onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) }
                        ) 
                    }
                }
                composable(Screen.About.route) { 
                    LoadingWrapper { AboutScreen(navController) } 
                }
                composable(Screen.Achievements.route) {
                    LoadingWrapper { AchievementsScreen(viewModel, onBack = { navController.popBackStack() }) }
                }
                composable(Screen.Statistics.route) {
                    LoadingWrapper { StatisticsScreen(viewModel, onBack = { navController.popBackStack() }) }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(bottom = 24.dp, start = 48.dp, end = 48.dp)
        ) {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(
                    initialOffsetY = { it }, 
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                ) + fadeIn(tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it }, 
                    animationSpec = tween(300)
                ) + fadeOut(tween(300))
            ) {
                BottomNavigationBar(navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Home, Screen.Graph, Screen.Personal)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp)

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val itemWidth = maxWidth / items.size
            val selectedIndex = items.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
            val targetOffset = itemWidth * selectedIndex

            val indicatorOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessLow
                ),
                label = "indicatorOffset"
            )

            val distanceToTarget = kotlin.math.abs(indicatorOffset.value - targetOffset.value)
            val isMoving = distanceToTarget > 20f

            val pillHeight by animateDpAsState(
                targetValue = if (isMoving) 32.dp else 56.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                label = "pillHeight"
            )

            val pillWidth by animateDpAsState(
                targetValue = if (isMoving) 64.dp else 80.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                label = "pillWidth"
            )

            val pillCorner by animateDpAsState(
                targetValue = if (isMoving) 16.dp else 28.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                label = "pillCorner"
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                    .width(itemWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(pillHeight)
                        .clip(RoundedCornerShape(pillCorner))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, screen ->
                    val isSelected = index == selectedIndex
                    
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(300),
                        label = "iconColor"
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.height(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = stringResource(screen.titleResId),
                                tint = iconColor
                            )
                        }

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(250, delayMillis = 100)) + expandVertically(tween(250, delayMillis = 100)),
                            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                        ) {
                            Text(
                                text = stringResource(screen.titleResId),
                                style = MaterialTheme.typography.labelSmall,
                                color = iconColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(navController = rememberNavController())
}
