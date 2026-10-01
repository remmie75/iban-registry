package com.example.ibanregistry.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BankAccountBackupCodecTest {
    @Test
    fun roundTripsAccountsWithoutDatabaseIds() {
        val accounts = listOf(
            BankAccount(
                id = 7,
                iban = "NL91 ABNA 0417 1643 00",
                description = " Savings ",
                folder = "Personal",
                tags = listOf("savings", "primary"),
            ),
            BankAccount(
                id = 9,
                iban = "GB82WEST12345698765432",
                description = "Household",
            ),
        )

        val decoded = BankAccountBackupCodec.decode(BankAccountBackupCodec.encode(accounts))

        assertEquals(
            listOf(
                BankAccount(
                    iban = "NL91ABNA0417164300",
                    description = "Savings",
                    folder = "Personal",
                    tags = listOf("savings", "primary"),
                ),
                BankAccount(
                    iban = "GB82WEST12345698765432",
                    description = "Household",
                    sortOrder = 1,
                ),
            ),
            decoded,
        )
    }

    @Test
    fun rejectsInvalidIbanAndDuplicateEntries() {
        assertThrows(BackupFormatException::class.java) {
            BankAccountBackupCodec.decode(
                """{"schemaVersion":1,"accounts":[{"iban":"invalid","description":"Bad"}]}""",
            )
        }
        assertThrows(BackupFormatException::class.java) {
            BankAccountBackupCodec.decode(
                """
                {
                  "schemaVersion": 1,
                  "accounts": [
                    {"iban":"NL91ABNA0417164300","description":"One"},
                    {"iban":"nl91 abna 0417 1643 00","description":"Two"}
                  ]
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun rejectsUnsupportedOrOversizedBackups() {
        assertThrows(BackupFormatException::class.java) {
            BankAccountBackupCodec.decode("""{"schemaVersion":99,"accounts":[]}""")
        }
        assertThrows(BackupFormatException::class.java) {
            BankAccountBackupCodec.decode(" ".repeat(BankAccountBackupCodec.MAX_BACKUP_BYTES + 1))
        }
    }

    @Test
    fun importsVersionOneBackupWithoutOrganizationFields() {
        val decoded = BankAccountBackupCodec.decode(
            """
            {
              "schemaVersion": 1,
              "accounts": [
                {"iban":"NL91ABNA0417164300","description":"Savings"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(
            listOf(BankAccount(iban = "NL91ABNA0417164300", description = "Savings")),
            decoded,
        )
    }
}
