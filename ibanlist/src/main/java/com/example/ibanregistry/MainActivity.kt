package com.example.ibanregistry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ibanregistry.domain.BankAccountRepository
import com.example.ibanregistry.ui.AccountListScreen
import com.example.ibanregistry.ui.AccountListViewModel
import com.example.ibanregistry.ui.BankAccountFormScreen
import com.example.ibanregistry.ui.BankAccountFormViewModel
import com.example.ibanregistry.ui.theme.IbanRegistryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as IbanRegistryApplication).container.bankAccountRepository
        setContent {
            IbanRegistryTheme {
                IbanRegistryApp(repository)
            }
        }
    }
}

@Composable
private fun IbanRegistryApp(repository: BankAccountRepository) {
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
