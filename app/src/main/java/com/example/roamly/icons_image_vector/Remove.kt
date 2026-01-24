package com.example.roamly.icons_image_vector

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Filled.Remove: ImageVector
    get() {
        if (_remove != null) {
            return _remove!!
        }
        _remove = materialIcon(name = "Filled.Remove") {
            materialPath {
                moveTo(5.0f, 13.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(5.0f)
                close()
            }
        }
        return _remove!!
    }

private var _remove: ImageVector? = null