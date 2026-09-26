package com.example.ibanregistry.domain

data class BankAccount(
    val id: Long = 0,
    val iban: String,
    val description: String,
)
