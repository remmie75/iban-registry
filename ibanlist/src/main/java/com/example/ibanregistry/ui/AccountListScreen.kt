package com.example.ibanregistry.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ibanregistry.R
import com.example.ibanregistry.domain.BankAccount
import com.example.ibanregistry.domain.BankAccountBackupCodec
import com.example.ibanregistry.domain.IbanValidator
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    viewModel: AccountListViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val deletedMessage = stringResource(R.string.account_deleted)
    val deleteFailedMessage = stringResource(R.string.delete_failed)
    val copiedMessage = stringResource(R.string.iban_copied)
    val backupExportedMessage = stringResource(R.string.backup_exported)
    val backupExportFailedMessage = stringResource(R.string.backup_export_failed)
    val invalidBackupMessage = stringResource(R.string.invalid_backup)
    val backupImportFailedMessage = stringResource(R.string.backup_import_failed)
    val reorderFailedMessage = stringResource(R.string.reorder_failed)
    var pendingDelete by remember { mutableStateOf<BankAccount?>(null) }
    var pendingBackup by remember { mutableStateOf<String?>(null) }
    var showBackupWarning by remember { mutableStateOf(false) }

    val createBackupDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val content = pendingBackup
        pendingBackup = null
        if ((uri != null) && (content != null)) {
            coroutineScope.launch {
                val exported = withContext(Dispatchers.IO) {
                    writeBackup(context, uri, content)
                }
                snackbarHostState.showSnackbar(
                    if (exported) backupExportedMessage else backupExportFailedMessage,
                )
            }
        }
    }
    val openBackupDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val content = withContext(Dispatchers.IO) {
                    readBackup(context, uri)
                }
                if (content == null) {
                    snackbarHostState.showSnackbar(backupImportFailedMessage)
                } else {
                    viewModel.importBackup(content)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AccountListEvent.Deleted ->
                    snackbarHostState.showSnackbar(deletedMessage)
                AccountListEvent.DeleteFailed ->
                    snackbarHostState.showSnackbar(deleteFailedMessage)
                is AccountListEvent.BackupReady -> {
                    pendingBackup = event.content
                    createBackupDocument.launch("iban-registry-backup.json")
                }
                AccountListEvent.BackupFailed ->
                    snackbarHostState.showSnackbar(backupExportFailedMessage)
                is AccountListEvent.ImportCompleted ->
                    snackbarHostState.showSnackbar(
                        context.getString(
                            R.string.backup_imported,
                            event.result.added,
                            event.result.updated,
                            event.result.unchanged,
                        ),
                    )
                AccountListEvent.InvalidBackup ->
                    snackbarHostState.showSnackbar(invalidBackupMessage)
                AccountListEvent.ImportFailed ->
                    snackbarHostState.showSnackbar(backupImportFailedMessage)
                AccountListEvent.ReorderFailed ->
                    snackbarHostState.showSnackbar(reorderFailedMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.accounts_title)) },
                actions = {
                    IconButton(onClick = {
                        openBackupDocument.launch(
                            arrayOf(
                                "application/json",
                                "text/plain",
                                "application/octet-stream",
                            ),
                        )
                    }) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = stringResource(R.string.import_backup),
                        )
                    }
                    IconButton(
                        onClick = { showBackupWarning = true },
                        enabled = state.allAccounts.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.export_backup),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_account))
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_accounts)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))

            if (state.folders.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedFolder == null,
                            onClick = { viewModel.selectFolder(null) },
                            label = { Text(stringResource(R.string.all_folders)) },
                        )
                    }
                    items(state.folders, key = { "folder:$it" }) { folder ->
                        FilterChip(
                            selected = state.selectedFolder == folder,
                            onClick = { viewModel.selectFolder(folder) },
                            label = {
                                Text(
                                    folder.ifEmpty {
                                        stringResource(R.string.no_folder)
                                    },
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (state.accounts.isEmpty()) {
                EmptyAccounts(searching = state.query.isNotBlank())
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        items = state.accounts,
                        key = { _, account -> account.id },
                    ) { index, account ->
                        val canReorder = state.query.isBlank() && state.selectedFolder == null
                        AccountCard(
                            account = account,
                            onCopy = {
                                val clipboard = context.getSystemService(
                                    Context.CLIPBOARD_SERVICE,
                                ) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("IBAN", account.iban))
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(copiedMessage)
                                }
                            },
                            onEdit = { onEdit(account.id) },
                            onDelete = { pendingDelete = account },
                            canMoveUp = canReorder && index > 0,
                            canMoveDown = canReorder && index < state.accounts.lastIndex,
                            onMoveUp = { viewModel.moveAccount(account.id, -1) },
                            onMoveDown = { viewModel.moveAccount(account.id, 1) },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = {
                Text(stringResource(R.string.delete_account_message, account.description))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(account.id)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showBackupWarning) {
        AlertDialog(
            onDismissRequest = { showBackupWarning = false },
            title = { Text(stringResource(R.string.backup_warning_title)) },
            text = { Text(stringResource(R.string.backup_warning_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackupWarning = false
                        viewModel.createBackup()
                    },
                ) {
                    Text(stringResource(R.string.export))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupWarning = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun writeBackup(context: Context, uri: Uri, content: String): Boolean =
    try {
        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        } != null
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }

private fun readBackup(context: Context, uri: Uri): String? =
    try {
        context.contentResolver.openInputStream(uri)?.use(::readLimitedUtf8)
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

private fun readLimitedUtf8(input: InputStream): String? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read == -1) break
        total += read
        if (total > BankAccountBackupCodec.MAX_BACKUP_BYTES) return null
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}

@Composable
private fun AccountCard(
    account: BankAccount,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = account.description,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = IbanValidator.format(account.iban),
                style = MaterialTheme.typography.bodyLarge,
            )
            account.folder?.let { folder ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Text(
                        text = folder,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            if (account.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = account.tags.joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.move_up),
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.move_down),
                    )
                }
                IconButton(
                    onClick = onCopy,
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_iban),
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun EmptyAccounts(searching: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(
                    if (searching) R.string.no_search_results_title else R.string.no_accounts_title,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    if (searching) {
                        R.string.no_search_results_message
                    } else {
                        R.string.no_accounts_message
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
