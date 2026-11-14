package com.dokanhisab.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconMapper {
    fun iconFor(key: String): ImageVector = when (key) {
        "storefront" -> Icons.Rounded.Storefront
        "inventory" -> Icons.Rounded.Inventory2
        "payments" -> Icons.Rounded.Payments
        "handyman" -> Icons.Rounded.Handyman
        "savings" -> Icons.Rounded.Savings
        "shopping_bag" -> Icons.Rounded.ShoppingBag
        "home_work" -> Icons.Rounded.HomeWork
        "bolt" -> Icons.Rounded.Bolt
        "groups" -> Icons.Rounded.Groups
        "local_shipping" -> Icons.Rounded.LocalShipping
        "receipt_long" -> Icons.Rounded.ReceiptLong
        "more_horiz" -> Icons.Rounded.MoreHoriz
        else -> Icons.Rounded.Category
    }
}
