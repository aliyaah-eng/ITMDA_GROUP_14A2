package com.securehomes.app.models

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String = "user", // "user" (participant) or "admin" (researcher)
    /** Assigned study version: "guided" (Version A) or "unguided" (Version B); null for researchers. */
    val studyGroup: String? = null,
    val createdAt: String
)
