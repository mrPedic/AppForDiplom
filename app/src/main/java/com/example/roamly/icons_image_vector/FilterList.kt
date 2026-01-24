package com.example.roamly.icons_image_vector

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Filled.FilterList: ImageVector
    get() {
        if (_filterList != null) {
            return _filterList!!
        }
        _filterList = materialIcon(name = "Filled.FilterList") {
            materialPath {
                moveTo(10.0f, 18.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(3.0f, 6.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(18.0f)
                verticalLineTo(6.0f)
                horizontalLineTo(3.0f)
                close()
                moveTo(6.0f, 13.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(6.0f)
                verticalLineToRelative(2.0f)
                close()
            }
        }
        return _filterList!!
    }

private var _filterList: ImageVector? = null