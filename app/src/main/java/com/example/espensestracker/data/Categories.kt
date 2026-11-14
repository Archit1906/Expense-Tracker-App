package com.example.espensestracker.data

import androidx.compose.ui.graphics.Color

data class Cat(val name: String, val emoji: String, val color: Color)

const val TRANSFER_CATEGORY = "Transfers"

val CATEGORY_LIST = listOf(
    Cat("Food & Dining", "🍽️", Color(0xFFC4A055)),
    Cat("Groceries", "🛒", Color(0xFF6F8F72)),
    Cat("Rides & Travel", "🚕", Color(0xFF2B3445)),
    Cat("Shopping", "🛍️", Color(0xFF8A6A33)),
    Cat("Subscriptions", "🔁", Color(0xFF9AA5BB)),
    Cat("Bills & Utilities", "🧾", Color(0xFFD3D8E3)),
    Cat("Health", "❤️", Color(0xFFC86B6B)),
    Cat("Entertainment", "🎬", Color(0xFF8E7CB3)),
    Cat(TRANSFER_CATEGORY, "🔄", Color(0xFF8C8C8C)),
    Cat("Other", "🏷️", Color(0xFFB8B3A8))
)

fun catOf(name: String): Cat = CATEGORY_LIST.firstOrNull { it.name == name } ?: CATEGORY_LIST.last()
