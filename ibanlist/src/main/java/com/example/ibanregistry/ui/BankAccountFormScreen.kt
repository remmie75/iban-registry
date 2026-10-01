package com.example.ibanregistry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ibanregistry.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountFormScreen(
    viewModel: BankAccountFormViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val saveFailedMessage = stringResource(R.string.save_failed)
    val notFoundMessage = stringResource(R.string.account_not_found)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                BankAccountFormEvent.Saved -> onBack()
                BankAccountFormEvent.SaveFailed ->
                    snackbarHostState.showSnackbar(saveFailedMessage)
                BankAccountFormEvent.AccountNotFound -> {
                    snackbarHostState.showSnackbar(notFoundMessage)
                    onBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.accountId == 0L) {
                                R.string.add_account
                            } else {
                                R.string.edit_account
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = state.iban,
                    onValueChange = viewModel::updateIban,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.iban_label)) },
                    supportingText = {
                        val error = when (state.ibanError) {
                            IbanFieldError.Invalid -> R.string.invalid_iban
                            IbanFieldError.Duplicate -> R.string.duplicate_iban
                            null -> null
                        }
                        error?.let { Text(stringResource(it)) }
                    },
                    isError = state.ibanError != null,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::updateDescription,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.description_label)) },
                    supportingText = {
                        when (state.descriptionError) {
                            DescriptionFieldError.Required ->
                                Text(stringResource(R.string.description_required))
                            DescriptionFieldError.TooLong ->
                                Text(
                                    stringResource(
                                        R.string.description_too_long,
                                        MAX_DESCRIPTION_LENGTH,
                                    ),
                                )
                            null ->
                                Text(
                                    stringResource(
                                        R.string.description_supporting_text,
                                        state.description.length,
                                        MAX_DESCRIPTION_LENGTH,
                                    ),
                                )
                        }
                    },
                    isError = state.descriptionError != null,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    minLines = 3,
                    maxLines = 5,
                )

                OutlinedTextField(
                    value = state.folder,
                    onValueChange = viewModel::updateFolder,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.folder_label)) },
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.tags,
                    onValueChange = viewModel::updateTags,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.tags_label)) },
                    supportingText = {
                        Text(
                            stringResource(
                                if (state.tagsError) {
                                    R.string.tags_invalid
                                } else {
                                    R.string.tags_supporting_text
                                },
                            ),
                        )
                    },
                    isError = state.tagsError,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.save()
                        },
                    ),
                    singleLine = true,
                )

                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator()
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}
