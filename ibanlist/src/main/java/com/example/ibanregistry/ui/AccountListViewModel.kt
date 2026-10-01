package com.example.ibanregistry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ibanregistry.domain.BankAccount
import com.example.ibanregistry.domain.BankAccountRepository
import com.example.ibanregistry.domain.AccountPersistenceException
import com.example.ibanregistry.domain.BackupFormatException
import com.example.ibanregistry.domain.BackupImportResult
import com.example.ibanregistry.domain.IbanValidator
import com.example.ibanregistry.domain.BankAccountBackupCodec
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountListUiState(
    val accounts: List<BankAccount> = emptyList(),
    val allAccounts: List<BankAccount> = emptyList(),
    val folders: List<String> = emptyList(),
    val selectedFolder: String? = null,
    val query: String = "",
)

sealed interface AccountListEvent {
    data object Deleted : AccountListEvent
    data object DeleteFailed : AccountListEvent
    data class BackupReady(val content: String) : AccountListEvent
    data object BackupFailed : AccountListEvent
    data class ImportCompleted(val result: BackupImportResult) : AccountListEvent
    data object InvalidBackup : AccountListEvent
    data object ImportFailed : AccountListEvent
    data object ReorderFailed : AccountListEvent
}

class AccountListViewModel(
    private val repository: BankAccountRepository,
) : ViewModel() {
    private val query = kotlinx.coroutines.flow.MutableStateFlow("")
    private val selectedFolder = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val eventChannel = Channel<AccountListEvent>(Channel.BUFFERED)

    val events = eventChannel.receiveAsFlow()

    val uiState: StateFlow<AccountListUiState> = combine(
        repository.observeAccounts(),
        query,
        selectedFolder,
    ) { accounts, searchQuery, folder ->
        val normalizedQuery = IbanValidator.normalize(searchQuery)
        val filteredAccounts = accounts.filter { account ->
            val matchesFolder = when (folder) {
                null -> true
                "" -> account.folder == null
                else -> account.folder.equals(folder, ignoreCase = true)
            }
            val matchesQuery = searchQuery.isBlank() ||
                account.description.contains(searchQuery.trim(), ignoreCase = true) ||
                account.iban.contains(normalizedQuery, ignoreCase = true) ||
                account.folder?.contains(searchQuery.trim(), ignoreCase = true) == true ||
                account.tags.any { it.contains(searchQuery.trim(), ignoreCase = true) }
            matchesFolder && matchesQuery
        }
        AccountListUiState(
            accounts = filteredAccounts,
            allAccounts = accounts,
            folders = accounts
                .map { it.folder.orEmpty() }
                .distinctBy(String::lowercase)
                .sortedWith(compareBy<String> { it.isNotEmpty() }.thenBy(String::lowercase)),
            selectedFolder = folder,
            query = searchQuery,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountListUiState(),
    )

    fun updateQuery(value: String) {
        query.value = value
    }

    fun selectFolder(folder: String?) {
        selectedFolder.value = folder
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

    fun createBackup() {
        viewModelScope.launch {
            try {
                val content = BankAccountBackupCodec.encode(repository.getAccounts())
                eventChannel.send(AccountListEvent.BackupReady(content))
            } catch (_: AccountPersistenceException) {
                eventChannel.send(AccountListEvent.BackupFailed)
            }
        }
    }

    fun importBackup(content: String) {
        viewModelScope.launch {
            try {
                val accounts = BankAccountBackupCodec.decode(content)
                val result = repository.importAccounts(accounts)
                eventChannel.send(AccountListEvent.ImportCompleted(result))
            } catch (_: BackupFormatException) {
                eventChannel.send(AccountListEvent.InvalidBackup)
            } catch (_: AccountPersistenceException) {
                eventChannel.send(AccountListEvent.ImportFailed)
            }
        }
    }

    fun moveAccount(id: Long, offset: Int) {
        viewModelScope.launch {
            try {
                repository.moveAccount(id, offset)
            } catch (_: AccountPersistenceException) {
                eventChannel.send(AccountListEvent.ReorderFailed)
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
