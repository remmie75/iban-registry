package com.example.ibanregistry.domain

data class BankAccount(
    val id: Long = 0,
    val iban: String,
    val description: String,
    val folder: String? = null,
    val tags: List<String> = emptyList(),
    val sortOrder: Long = 0,
)
