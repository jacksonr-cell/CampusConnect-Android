package com.campusconnect.app.ui.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

/**
 * Maps WindowWidthSizeClass to a grid column count, per the CampusConnect
 * adaptive design spec:
 *   Compact (phones)   → 1 column (or 2 for card grids)
 *   Medium  (tablets)  → 2-3 columns
 *   Expanded (desktop) → 3-4 columns
 */
fun columnsForWidth(widthSizeClass: WindowWidthSizeClass, dense: Boolean = false): Int {
    return when (widthSizeClass) {
        WindowWidthSizeClass.Compact  -> if (dense) 2 else 1
        WindowWidthSizeClass.Medium   -> if (dense) 3 else 2
        else                          -> if (dense) 4 else 3
    }
}

fun adaptiveGridCells(widthSizeClass: WindowWidthSizeClass, dense: Boolean = false): GridCells =
    GridCells.Fixed(columnsForWidth(widthSizeClass, dense))
