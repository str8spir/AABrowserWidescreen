/*
 * Copyright (C) 2025 AABrowser Contributors (https://github.com/kododake/AABrowser)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package com.kododake.aabrowser.ui.compose.screens.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kododake.aabrowser.R
import com.kododake.aabrowser.ui.compose.theme.AABrowserTheme

/** Actions for the Widescreen Edition video buttons on the floating dock. */
data class DockActions(
    val onCropToFill: () -> Unit = {},
    val onResetZoom: () -> Unit = {},
    val onZoomIn: () -> Unit = {},
    val onZoomOut: () -> Unit = {}
)

/**
 * Floating dock: the regular menu / address-bar button plus crop-to-fill, reset and zoom buttons.
 * It is driven by the same visibility state as the stock FAB, so auto-hide, "always visible" and
 * the corner-position preference keep working.
 */
@Composable
fun BrowserNavigationDock(
    stateHolder: MenuStateHolder,
    onMenuClick: () -> Unit,
    actions: DockActions
) {
    AABrowserTheme {
        AnimatedVisibility(
            visible = stateHolder.isFabVisible,
            enter = scaleIn(tween(180)) + fadeIn(tween(180)),
            exit = scaleOut(tween(120)) + fadeOut(tween(120)),
            modifier = Modifier.padding(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val menuIcon = if (stateHolder.fabIsAddressBarMode) R.drawable.search_24px else R.drawable.ic_menu_24px
                    val menuDesc = if (stateHolder.fabIsAddressBarMode) R.string.menu_open_address_bar else R.string.menu_open_description
                    DockButton(menuIcon, menuDesc, onMenuClick)
                    DockButton(R.drawable.devices_other_24px, R.string.menu_zoom_fill, actions.onCropToFill)
                    DockButton(R.drawable.lock_reset_24px, R.string.menu_zoom_reset, actions.onResetZoom)
                    DockButton(R.drawable.ic_add, R.string.menu_zoom_in, actions.onZoomIn)
                    DockButton(R.drawable.ic_remove, R.string.menu_zoom_out, actions.onZoomOut)
                }
            }
        }
    }
}

@Composable
private fun DockButton(
    @DrawableRes icon: Int,
    @StringRes description: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            modifier = Modifier.size(24.dp)
        )
    }
}
