package com.example.traductorlsa.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.background

import androidx.compose.material3.*
import androidx.compose.runtime.*


import androidx.compose.ui.unit.dp

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traductorlsa.ui.screens.AboutScreen
import com.example.traductorlsa.ui.screens.AuthEntryScreen
import com.example.traductorlsa.ui.screens.DatasetScreen
import com.example.traductorlsa.ui.screens.DictionaryScreen
import com.example.traductorlsa.ui.screens.nombreParaMostrar
import com.example.traductorlsa.ui.screens.HomeScreen
import com.example.traductorlsa.ui.screens.SettingsScreen
import com.example.traductorlsa.ui.screens.TrainingHomeScreen
import com.example.traductorlsa.ui.screens.TranslateVoiceScreen
import com.example.traductorlsa.ui.screens.OnboardingScreen
import com.example.traductorlsa.ui.screens.SplashScreen
import com.example.traductorlsa.ui.theme.SenarTheme

import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthView
// CORRECCIóN 1: Nombre correcto del componente de Clerk


import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri




@Composable
fun LsaTranslatorApp() {
    val navController = rememberNavController()

    SenarTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavHost(navController = navController)
        }
    }
}

/* ----------------- Destinos de navegación ----------------- */

sealed class AppDestination(val route: String) {
    object Splash : AppDestination("splash")
    object Onboarding : AppDestination("onboarding")
    object AuthEntry : AppDestination("auth_entry")
    object AuthClerk : AppDestination("auth_clerk")
    object Home : AppDestination("home")
    object TranslateSign : AppDestination("translate_sign")
    object TranslateVoice : AppDestination("translate_voice")
    object TrainingHome : AppDestination("training_home")
    object TrainingCapture : AppDestination("training_capture") {
        const val ARG_SENA = "sena"

        /**
         * Ruta registrada en el NavHost. El argumento es opcional: entrar desde
         * «Grabar nuevas muestras» sigue navegando a `route` pelada y el
         * selector se abre solo, como antes.
         */
        val rutaConSena = "training_capture?$ARG_SENA={$ARG_SENA}"

        /** Abre la camara ya entrenando esta sena. Lo usa el dataset. */
        fun paraSena(etiqueta: String) =
            "training_capture?$ARG_SENA=${Uri.encode(etiqueta)}"
    }

    object Dataset : AppDestination("dataset")
    object Dictionary : AppDestination("dictionary")
    object Settings : AppDestination("settings")
    object About : AppDestination("about")
}

/* ----------------- NavHost principal ----------------- */
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Splash.route
    ) {
        composable(AppDestination.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(AppDestination.Onboarding.route) {
                        popUpTo(AppDestination.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestination.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(AppDestination.AuthEntry.route) {
                        popUpTo(AppDestination.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Y aca usamos directamente AuthView en la ruta AuthEntry
        composable(AppDestination.AuthEntry.route) {
            AuthEntryScreen(navController)
        }

        composable(AppDestination.AuthClerk.route) {
            AuthClerkScreen(navController)
        }


        composable(AppDestination.Home.route) {
            HomeScreen(
                onTranslateSign = { navController.navigate(AppDestination.TranslateSign.route) },
                onTranslateVoice = { navController.navigate(AppDestination.TranslateVoice.route) },
                onTraining = { navController.navigate(AppDestination.TrainingHome.route) },
                onDictionary = { navController.navigate(AppDestination.Dictionary.route) },
                onSettings = { navController.navigate(AppDestination.Settings.route) }
            )
        }

        composable(AppDestination.TranslateSign.route) {
            TranslateSignScreen(navController)
        }

        composable(AppDestination.TranslateVoice.route) {
            TranslateVoiceScreen(navController)
        }

        composable(AppDestination.TrainingHome.route) {
            TrainingAccessGate(navController) { TrainingHomeScreen(navController) }
        }
        composable(
            route = AppDestination.TrainingCapture.rutaConSena,
            arguments = listOf(
                navArgument(AppDestination.TrainingCapture.ARG_SENA) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) { entrada ->
            val sena = entrada.arguments?.getString(AppDestination.TrainingCapture.ARG_SENA)
            TrainingAccessGate(navController) { TrainingCaptureScreen(navController, sena) }
        }
        composable(AppDestination.Dataset.route) {
            TrainingAccessGate(navController) { DatasetScreen(navController) }
        }
        composable(AppDestination.Dictionary.route) { DictionaryScreen(navController) }
        composable(AppDestination.Settings.route) { SettingsScreen(navController) }
        composable(AppDestination.About.route) { AboutScreen(navController) }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingAccessGate(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    if (user != null) {
        content()
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Modo entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "El entrenamiento solo está disponible con tu cuenta sincronizada.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Podrás seguir usando el diccionario en modo offline. Para entrenar nuevas señas, iniciá sesión desde la app.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = { navController.navigate(AppDestination.AuthClerk.route) }) {
                Text("Iniciar sesión")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthClerkScreen(navController: NavHostController) {
    val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle(false)
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    // Cuando Clerk está listo y hay usuaria ir al Home
    LaunchedEffect(isInitialized, user) {
        if (isInitialized && user != null) {
            navController.navigate(AppDestination.Home.route) {
                // limpiamos el flujo de auth del back stack
                popUpTo(AppDestination.AuthEntry.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sincronizar con mi cuenta") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->

        when {
            // 1) Clerk todavía inicializando spinner
            !isInitialized -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // 2) Sin usuaria mostramos AuthView normalmente
            user == null -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    AuthView(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3) Con usuaria no mostramos AuthView (ya se está¡ navegando al Home)
            else -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Opcional: un mini texto tipo "Entrando a SeÑAR..."
                    // Text("Entrando a SeÑAR")
                }
            }
        }
    }
}
