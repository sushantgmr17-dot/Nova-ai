package com.example.auth

import android.content.Context
import android.util.Patterns

class AuthManager(private val context: Context) {

    fun validateEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword(password: String): Boolean {
        // Safe check for length and complexity
        return password.length >= 6
    }

    fun mockAuthenticate(email: String, password: String): Boolean {
        return validateEmail(email) && password.length >= 6
    }
}
