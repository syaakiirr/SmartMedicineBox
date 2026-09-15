package com.example.smartmedicinebox.data.model

data class User(
    val userId: String,
    val name: String,
    val role: UserRole
)

enum class UserRole {
    USER,       // Patient - views schedule, receives reminders
    CAREGIVER   // Caregiver - monitors status, receives alerts
}
