package com.example.ibanregistry

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ibanregistry.domain.BankAccountRepository
import com.example.ibanregistry.security.AppLockManager
import com.example.ibanregistry.security.AppLockViewModel
import com.example.ibanregistry.ui.AccountListScreen
import com.example.ibanregistry.ui.AppLockScreen
import com.example.ibanregistry.ui.AccountListViewModel
import com.example.ibanregistry.ui.BankAccountFormScreen
import com.example.ibanregistry.ui.BankAccountFormViewModel
import com.example.ibanregistry.ui.SecuritySettingsScreen
import com.example.ibanregistry.ui.theme.IbanRegistryTheme

class MainActivity : FragmentActivity() {
    private var appLockViewModel: AppLockViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as IbanRegistryApplication).container
        setContent {
            IbanRegistryTheme {
                val lockViewModel: AppLockViewModel = viewModel(
                    factory = AppLockViewModel.factory(container.appLockManager),
                )
                appLockViewModel = lockViewModel
                SecureIbanRegistryApp(
                    repository = container.bankAccountRepository,
                    appLockManager = container.appLockManager,
                    appLockViewModel = lockViewModel,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) appLockViewModel?.lock()
    }

    internal fun showBiometricPrompt(onAuthenticated: () -> Unit) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    onAuthenticated()
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.use_pin))
                .build(),
        )
    }
}

@Composable
private fun MainActivity.SecureIbanRegistryApp(
    repository: BankAccountRepository,
    appLockManager: AppLockManager,
    appLockViewModel: AppLockViewModel,
) {
    val isUnlocked by appLockViewModel.isUnlocked.collectAsStateWithLifecycle()
    val biometricAvailable = remember {
        BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    if (!isUnlocked) {
        AppLockScreen(
            biometricEnabled = appLockManager.isBiometricEnabled,
            biometricAvailable = biometricAvailable,
            onPinSubmitted = appLockViewModel::unlockWithPin,
            onBiometricRequested = {
                showBiometricPrompt(appLockViewModel::unlockWithBiometric)
            },
        )
    } else {
        IbanRegistryApp(
            repository = repository,
            appLockManager = appLockManager,
            biometricAvailable = biometricAvailable,
            onSecurityChanged = appLockViewModel::refreshAfterSettingsChange,
        )
    }
}

@Composable
private fun IbanRegistryApp(
    repository: BankAccountRepository,
    appLockManager: AppLockManager,
    biometricAvailable: Boolean,
    onSecurityChanged: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "accounts") {
        composable("accounts") {
            val viewModel: AccountListViewModel = viewModel(
                factory = AccountListViewModel.factory(repository),
            )
            AccountListScreen(
                viewModel = viewModel,
                onAdd = { navController.navigate("account/new") },
                onEdit = { id -> navController.navigate("account/$id") },
                onSecurity = { navController.navigate("security") },
            )
        }
        composable("security") {
            SecuritySettingsScreen(
                appLockManager = appLockManager,
                biometricAvailable = biometricAvailable,
                onBack = { navController.popBackStack() },
                onChanged = onSecurityChanged,
            )
        }
        composable("account/new") {
            val viewModel: BankAccountFormViewModel = viewModel(
                factory = BankAccountFormViewModel.factory(repository),
            )
            BankAccountFormScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "account/{accountId}",
            arguments = listOf(navArgument("accountId") { type = NavType.LongType }),
        ) {
            val viewModel: BankAccountFormViewModel = viewModel(
                factory = BankAccountFormViewModel.factory(repository),
            )
            BankAccountFormScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
