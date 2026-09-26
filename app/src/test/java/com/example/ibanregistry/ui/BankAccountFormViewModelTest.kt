package com.example.ibanregistry.ui

import androidx.lifecycle.SavedStateHandle
import com.example.ibanregistry.domain.BankAccount
import com.example.ibanregistry.domain.BankAccountRepository
import com.example.ibanregistry.domain.DuplicateIbanException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BankAccountFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveNormalizesInputAndTrimsDescription() = runTest {
        val repository = FakeBankAccountRepository()
        val viewModel = BankAccountFormViewModel(SavedStateHandle(), repository)

        viewModel.updateIban("nl91 abna 0417 1643 00")
        viewModel.updateDescription("  Savings  ")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("NL91ABNA0417164300", repository.savedAccount?.iban)
        assertEquals("Savings", repository.savedAccount?.description)
        assertNull(viewModel.uiState.value.ibanError)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun invalidFieldsAreReportedWithoutSaving() = runTest {
        val repository = FakeBankAccountRepository()
        val viewModel = BankAccountFormViewModel(SavedStateHandle(), repository)

        viewModel.updateIban("not-an-iban")
        viewModel.updateDescription(" ")
        viewModel.save()

        assertEquals(IbanFieldError.Invalid, viewModel.uiState.value.ibanError)
        assertEquals(
            DescriptionFieldError.Required,
            viewModel.uiState.value.descriptionError,
        )
        assertNull(repository.savedAccount)
    }

    @Test
    fun duplicateIbanIsReportedOnTheField() = runTest {
        val repository = FakeBankAccountRepository(duplicateOnSave = true)
        val viewModel = BankAccountFormViewModel(SavedStateHandle(), repository)

        viewModel.updateIban("GB82 WEST 1234 5698 7654 32")
        viewModel.updateDescription("Current account")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(IbanFieldError.Duplicate, viewModel.uiState.value.ibanError)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun loadsExistingAccountForEditing() = runTest {
        val repository = FakeBankAccountRepository(
            existingAccount = BankAccount(
                id = 7,
                iban = "DE89370400440532013000",
                description = "Household",
            ),
        )
        val viewModel = BankAccountFormViewModel(
            SavedStateHandle(mapOf("accountId" to 7L)),
            repository,
        )
        advanceUntilIdle()

        assertEquals(7L, viewModel.uiState.value.accountId)
        assertEquals("DE89 3704 0044 0532 0130 00", viewModel.uiState.value.iban)
        assertEquals("Household", viewModel.uiState.value.description)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}

private class FakeBankAccountRepository(
    private val existingAccount: BankAccount? = null,
    private val duplicateOnSave: Boolean = false,
) : BankAccountRepository {
    private val accounts = MutableStateFlow<List<BankAccount>>(emptyList())
    var savedAccount: BankAccount? = null
        private set

    override fun observeAccounts(): Flow<List<BankAccount>> = accounts

    override suspend fun getAccount(id: Long): BankAccount? = existingAccount

    override suspend fun saveAccount(account: BankAccount): Long {
        if (duplicateOnSave) throw DuplicateIbanException()
        savedAccount = account.copy(iban = account.iban.filterNot(Char::isWhitespace).uppercase())
        return account.id.takeIf { it != 0L } ?: 1L
    }

    override suspend fun deleteAccount(id: Long) = Unit
}
