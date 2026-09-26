package com.example.ibanregistry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ibanregistry.domain.BankAccount
import com.example.ibanregistry.domain.BankAccountRepository
import com.example.ibanregistry.domain.AccountPersistenceException
import com.example.ibanregistry.domain.IbanValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountListUiState(
    val accounts: List<BankAccount> = emptyList(),
    val query: String = "",
)

sealed interface AccountListEvent {
    data object Deleted : AccountListEvent
    data object DeleteFailed : AccountListEvent
}

class AccountListViewModel(
    private val repository: BankAccountRepository,
) : ViewModel() {
    private val query = kotlinx.coroutines.flow.MutableStateFlow("")
    private val eventChannel = Channel<AccountListEvent>(Channel.BUFFERED)

    val events = eventChannel.receiveAsFlow()

    val uiState: StateFlow<AccountListUiState> = combine(
        repository.observeAccounts(),
        query,
    ) { accounts, searchQuery ->
        val normalizedQuery = IbanValidator.normalize(searchQuery)
        val filteredAccounts = if (searchQuery.isBlank()) {
            accounts
        } else {
            accounts.filter { account ->
                account.description.contains(searchQuery.trim(), ignoreCase = true) ||
                    account.iban.contains(normalizedQuery, ignoreCase = true)
            }
        }
        AccountListUiState(accounts = filteredAccounts, query = searchQuery)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountListUiState(),
    )

    fun updateQuery(value: String) {
        query.value = value
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(id)
                eventChannel.send(AccountListEvent.Deleted)
            } catch (_: AccountPersistenceException) {
                eventChannel.send(AccountListEvent.DeleteFailed)
            }
        }
    }

    companion object {
        fun factory(repository: BankAccountRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AccountListViewModel(repository) }
            }
    }
}
