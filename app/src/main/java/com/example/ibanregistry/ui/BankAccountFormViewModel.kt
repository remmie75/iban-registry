package com.example.ibanregistry.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ibanregistry.domain.BankAccount
import com.example.ibanregistry.domain.BankAccountRepository
import com.example.ibanregistry.domain.AccountPersistenceException
import com.example.ibanregistry.domain.DuplicateIbanException
import com.example.ibanregistry.domain.IbanValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MAX_DESCRIPTION_LENGTH = 200

enum class IbanFieldError {
    Invalid,
    Duplicate,
}

enum class DescriptionFieldError {
    Required,
    TooLong,
}

data class BankAccountFormUiState(
    val accountId: Long = 0,
    val iban: String = "",
    val description: String = "",
    val ibanError: IbanFieldError? = null,
    val descriptionError: DescriptionFieldError? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
)

sealed interface BankAccountFormEvent {
    data object Saved : BankAccountFormEvent
    data object SaveFailed : BankAccountFormEvent
    data object AccountNotFound : BankAccountFormEvent
}

class BankAccountFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: BankAccountRepository,
) : ViewModel() {
    private val accountId = savedStateHandle.get<Long>("accountId") ?: 0L
    private val mutableUiState = MutableStateFlow(
        BankAccountFormUiState(
            accountId = accountId,
            isLoading = accountId != 0L,
        ),
    )
    private val eventChannel = Channel<BankAccountFormEvent>(Channel.BUFFERED)

    val uiState: StateFlow<BankAccountFormUiState> = mutableUiState.asStateFlow()
    val events = eventChannel.receiveAsFlow()

    init {
        if (accountId != 0L) {
            loadAccount()
        }
    }

    fun updateIban(value: String) {
        if (value.length <= 42 && value.all { it.isLetterOrDigit() || it.isWhitespace() }) {
            mutableUiState.update { it.copy(iban = value, ibanError = null) }
        }
    }

    fun updateDescription(value: String) {
        if (value.length <= MAX_DESCRIPTION_LENGTH + 1) {
            mutableUiState.update { it.copy(description = value, descriptionError = null) }
        }
    }

    fun save() {
        val current = mutableUiState.value
        val ibanError = if (IbanValidator.isValid(current.iban)) null else IbanFieldError.Invalid
        val trimmedDescription = current.description.trim()
        val descriptionError = when {
            trimmedDescription.isEmpty() -> DescriptionFieldError.Required
            trimmedDescription.length > MAX_DESCRIPTION_LENGTH -> DescriptionFieldError.TooLong
            else -> null
        }
        if (ibanError != null || descriptionError != null) {
            mutableUiState.update {
                it.copy(ibanError = ibanError, descriptionError = descriptionError)
            }
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true) }
            try {
                repository.saveAccount(
                    BankAccount(
                        id = current.accountId,
                        iban = current.iban,
                        description = trimmedDescription,
                    ),
                )
                mutableUiState.update { it.copy(isSaving = false) }
                eventChannel.send(BankAccountFormEvent.Saved)
            } catch (_: DuplicateIbanException) {
                mutableUiState.update {
                    it.copy(isSaving = false, ibanError = IbanFieldError.Duplicate)
                }
            } catch (_: AccountPersistenceException) {
                mutableUiState.update { it.copy(isSaving = false) }
                eventChannel.send(BankAccountFormEvent.SaveFailed)
            }
        }
    }

    private fun loadAccount() {
        viewModelScope.launch {
            val account = repository.getAccount(accountId)
            if (account == null) {
                mutableUiState.update { it.copy(isLoading = false) }
                eventChannel.send(BankAccountFormEvent.AccountNotFound)
            } else {
                mutableUiState.value = BankAccountFormUiState(
                    accountId = account.id,
                    iban = IbanValidator.format(account.iban),
                    description = account.description,
                )
            }
        }
    }

    companion object {
        fun factory(repository: BankAccountRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    BankAccountFormViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        repository = repository,
                    )
                }
            }
    }
}
