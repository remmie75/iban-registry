package com.example.ibanregistry.domain

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object BankAccountBackupCodec {
    const val MAX_BACKUP_BYTES = 1_048_576
    private const val SCHEMA_VERSION = 2
    private const val MAX_ACCOUNTS = 10_000
    private const val MAX_DESCRIPTION_LENGTH = 200

    fun encode(accounts: List<BankAccount>): String {
        val entries = JSONArray()
        accounts.forEachIndexed { index, account ->
            entries.put(
                JSONObject()
                    .put("iban", IbanValidator.normalize(account.iban))
                    .put("description", account.description.trim())
                    .put("folder", account.folder ?: JSONObject.NULL)
                    .put("tags", JSONArray(account.tags))
                    .put("position", index),
            )
        }
        return JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("accounts", entries)
            .toString(2)
    }

    fun decode(content: String): List<BankAccount> {
        if (content.toByteArray(Charsets.UTF_8).size > MAX_BACKUP_BYTES) {
            throw BackupFormatException("Backup file is too large")
        }

        try {
            val root = JSONObject(content)
            val schemaVersion = root.getInt("schemaVersion")
            if (schemaVersion !in 1..SCHEMA_VERSION) {
                throw BackupFormatException("Unsupported backup version")
            }
            val entries = root.getJSONArray("accounts")
            if (entries.length() > MAX_ACCOUNTS) {
                throw BackupFormatException("Backup contains too many accounts")
            }

            val ibans = mutableSetOf<String>()
            return buildList(entries.length()) {
                repeat(entries.length()) { index ->
                    val entry = entries.getJSONObject(index)
                    val iban = IbanValidator.normalize(entry.getString("iban"))
                    val description = entry.getString("description").trim()
                    val folder = if (entry.isNull("folder")) {
                        null
                    } else {
                        entry.optString("folder").trim().takeIf(String::isNotEmpty)
                    }
                    val tags = entry.optJSONArray("tags")?.let { values ->
                        buildList(values.length()) {
                            repeat(values.length()) { tagIndex ->
                                add(values.getString(tagIndex).trim())
                            }
                        }
                    }.orEmpty()
                    if (!IbanValidator.isValid(iban)) {
                        throw BackupFormatException("Account ${index + 1} has an invalid IBAN")
                    }
                    if (description.isEmpty() || description.length > MAX_DESCRIPTION_LENGTH) {
                        throw BackupFormatException("Account ${index + 1} has an invalid description")
                    }
                    if (folder != null && folder.length > 50) {
                        throw BackupFormatException("Account ${index + 1} has an invalid folder")
                    }
                    if (
                        tags.size > 10 ||
                        tags.any { it.isEmpty() || it.length > 30 } ||
                        tags.distinctBy(String::lowercase).size != tags.size
                    ) {
                        throw BackupFormatException("Account ${index + 1} has invalid tags")
                    }
                    if (!ibans.add(iban)) {
                        throw BackupFormatException("Backup contains a duplicate IBAN")
                    }
                    add(
                        BankAccount(
                            iban = iban,
                            description = description,
                            folder = folder,
                            tags = tags,
                            sortOrder = entry.optLong("position", index.toLong()),
                        ),
                    )
                }
            }
        } catch (exception: BackupFormatException) {
            throw exception
        } catch (exception: JSONException) {
            throw BackupFormatException("Backup is not valid JSON", exception)
        }
    }
}

class BackupFormatException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
