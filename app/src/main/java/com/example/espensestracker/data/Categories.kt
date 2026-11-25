package com.example.espensestracker.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Cat(val name: String, val icon: ImageVector, val color: Color)

const val TRANSFER_CATEGORY = "Transfers"

val CATEGORY_LIST = listOf(
    Cat("Food & Dining", Icons.Filled.Restaurant, Color(0xFFC4A055)),
    Cat("Groceries", Icons.Filled.LocalGroceryStore, Color(0xFF6F8F72)),
    Cat("Rides & Travel", Icons.Filled.DirectionsCar, Color(0xFF2B3445)),
    Cat("Shopping", Icons.Filled.ShoppingBag, Color(0xFF8A6A33)),
    Cat("Subscriptions", Icons.Filled.Autorenew, Color(0xFF9AA5BB)),
    Cat("Bills & Utilities", Icons.Filled.Receipt, Color(0xFFA8B1C4)),
    Cat("Health", Icons.Filled.LocalHospital, Color(0xFFC86B6B)),
    Cat("Entertainment", Icons.Filled.Movie, Color(0xFF8E7CB3)),
    Cat(TRANSFER_CATEGORY, Icons.Filled.SwapHoriz, Color(0xFF8C8C8C)),
    Cat("Other", Icons.Filled.MoreHoriz, Color(0xFFB8B3A8))
)

fun catOf(name: String): Cat = CATEGORY_LIST.firstOrNull { it.name == name } ?: CATEGORY_LIST.last()
