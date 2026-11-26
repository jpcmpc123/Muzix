package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val SimpIcons.GridView: ImageVector
  get() {
    if (_GridView != null) {
      return _GridView!!
    }
    _GridView =
      ImageVector.Builder(
          name = "GridView",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.EvenOdd,
          ) {
            moveTo(3f, 3f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(8f)
            lineTo(11f, 3f)
            lineTo(3f, 3f)
            close()
            moveTo(9f, 9f)
            lineTo(5f, 9f)
            lineTo(5f, 5f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(4f)
            close()
            moveTo(3f, 13f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(-8f)
            lineTo(3f, 13f)
            close()
            moveTo(9f, 19f)
            lineTo(5f, 19f)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(4f)
            close()
            moveTo(13f, 3f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(8f)
            lineTo(21f, 3f)
            horizontalLineToRelative(-8f)
            close()
            moveTo(19f, 9f)
            horizontalLineToRelative(-4f)
            lineTo(15f, 5f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(4f)
            close()
            moveTo(13f, 13f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(-8f)
            horizontalLineToRelative(-8f)
            close()
            moveTo(19f, 19f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(4f)
            close()
          }
        }
        .buildSimpIcon()
    return _GridView!!
  }

private var _GridView: ImageVector? = null
