package com.learncodingrsa.model

data class PasswordRequest(
    val username: String,
    val old_password: String,
    val new_password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)
