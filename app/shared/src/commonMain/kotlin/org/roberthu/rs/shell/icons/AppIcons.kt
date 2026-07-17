package org.roberthu.rs.shell.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Small set of Material-style icons used by the desktop shell.
 * Kept in-repo so the app does not depend on the deprecated material-icons-extended artifact.
 */
object AppIcons {
    val Cable: ImageVector by lazy {
        materialIcon("Cable") {
            moveTo(20.0f, 5.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-4.0f)
            lineTo(16.0f, 5.0f)
            horizontalLineToRelative(4.0f)
            close()
            moveTo(20.0f, 17.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(4.0f)
            close()
            moveTo(10.0f, 5.0f)
            verticalLineToRelative(2.0f)
            lineTo(4.0f, 7.0f)
            lineTo(4.0f, 5.0f)
            horizontalLineToRelative(6.0f)
            close()
            moveTo(10.0f, 17.0f)
            verticalLineToRelative(2.0f)
            lineTo(4.0f, 19.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(6.0f)
            close()
            moveTo(16.0f, 11.0f)
            verticalLineToRelative(2.0f)
            lineTo(8.0f, 13.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(8.0f)
            close()
            moveTo(7.0f, 9.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(2.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineToRelative(-2.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(17.0f, 9.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(2.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineToRelative(-2.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
        }
    }

    val Speed: ImageVector by lazy {
        materialIcon("Speed") {
            moveTo(20.38f, 8.57f)
            lineToRelative(-1.23f, 1.85f)
            arcToRelative(8.0f, 8.0f, 0.0f, true, true, -12.3f, 0.0f)
            lineTo(5.62f, 8.57f)
            arcToRelative(10.0f, 10.0f, 0.0f, true, false, 14.76f, 0.0f)
            close()
            moveTo(12.0f, 10.0f)
            lineToRelative(3.0f, -5.0f)
            lineToRelative(1.0f, 6.0f)
            close()
        }
    }

    val Article: ImageVector by lazy {
        materialIcon("Article") {
            moveTo(19.0f, 3.0f)
            lineTo(5.0f, 3.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(21.0f, 5.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(14.0f, 17.0f)
            lineTo(7.0f, 17.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(7.0f)
            verticalLineToRelative(2.0f)
            close()
            moveTo(17.0f, 13.0f)
            lineTo(7.0f, 13.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(10.0f)
            verticalLineToRelative(2.0f)
            close()
            moveTo(17.0f, 9.0f)
            lineTo(7.0f, 9.0f)
            lineTo(7.0f, 7.0f)
            horizontalLineToRelative(10.0f)
            verticalLineToRelative(2.0f)
            close()
        }
    }

    val AccountTree: ImageVector by lazy {
        materialIcon("AccountTree") {
            moveTo(22.0f, 11.0f)
            lineTo(22.0f, 3.0f)
            horizontalLineToRelative(-7.0f)
            verticalLineToRelative(3.0f)
            lineTo(9.0f, 6.0f)
            lineTo(9.0f, 3.0f)
            lineTo(2.0f, 3.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(7.0f)
            lineTo(9.0f, 8.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(10.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(7.0f)
            verticalLineToRelative(-8.0f)
            horizontalLineToRelative(-7.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(-2.0f)
            lineTo(11.0f, 8.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(3.0f)
            close()
        }
    }

    val AddLink: ImageVector by lazy {
        materialIcon("AddLink") {
            moveTo(8.0f, 11.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(2.0f)
            lineTo(8.0f, 13.0f)
            close()
            moveTo(20.1f, 12.0f)
            curveToRelative(0.0f, 2.21f, -1.79f, 4.0f, -4.0f, 4.0f)
            horizontalLineToRelative(-3.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(3.0f)
            curveToRelative(3.31f, 0.0f, 6.0f, -2.69f, 6.0f, -6.0f)
            reflectiveCurveToRelative(-2.69f, -6.0f, -6.0f, -6.0f)
            horizontalLineToRelative(-3.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(3.0f)
            curveToRelative(2.21f, 0.0f, 4.0f, 1.79f, 4.0f, 4.0f)
            close()
            moveTo(3.9f, 12.0f)
            curveToRelative(0.0f, -2.21f, 1.79f, -4.0f, 4.0f, -4.0f)
            horizontalLineToRelative(3.0f)
            lineTo(10.9f, 6.0f)
            horizontalLineToRelative(-3.0f)
            curveToRelative(-3.31f, 0.0f, -6.0f, 2.69f, -6.0f, 6.0f)
            reflectiveCurveToRelative(2.69f, 6.0f, 6.0f, 6.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-3.0f)
            curveToRelative(-2.21f, 0.0f, -4.0f, -1.79f, -4.0f, -4.0f)
            close()
            moveTo(19.0f, 5.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(-3.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(2.0f)
            lineTo(21.0f, 10.0f)
            horizontalLineToRelative(3.0f)
            lineTo(24.0f, 8.0f)
            horizontalLineToRelative(-3.0f)
            lineTo(21.0f, 5.0f)
            close()
        }
    }

    val CreateNewFolder: ImageVector by lazy {
        materialIcon("CreateNewFolder") {
            moveTo(20.0f, 6.0f)
            horizontalLineToRelative(-8.0f)
            lineToRelative(-2.0f, -2.0f)
            lineTo(4.0f, 4.0f)
            curveToRelative(-1.11f, 0.0f, -1.99f, 0.89f, -1.99f, 2.0f)
            lineTo(2.0f, 18.0f)
            curveToRelative(0.0f, 1.11f, 0.89f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(16.0f)
            curveToRelative(1.11f, 0.0f, 2.0f, -0.89f, 2.0f, -2.0f)
            lineTo(22.0f, 8.0f)
            curveToRelative(0.0f, -1.11f, -0.89f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(19.0f, 14.0f)
            horizontalLineToRelative(-3.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(-3.0f)
            horizontalLineToRelative(-3.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(-3.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(2.0f)
            close()
        }
    }

    val Download: ImageVector by lazy {
        materialIcon("Download") {
            moveTo(5.0f, 20.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(-2.0f)
            lineTo(5.0f, 18.0f)
            verticalLineToRelative(2.0f)
            close()
            moveTo(19.0f, 9.0f)
            horizontalLineToRelative(-4.0f)
            lineTo(15.0f, 3.0f)
            lineTo(9.0f, 3.0f)
            verticalLineToRelative(6.0f)
            lineTo(5.0f, 9.0f)
            lineToRelative(7.0f, 7.0f)
            close()
        }
    }

    val Folder: ImageVector by lazy {
        materialIcon("Folder") {
            moveTo(10.0f, 4.0f)
            lineTo(4.0f, 4.0f)
            curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
            lineTo(2.0f, 18.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(16.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(22.0f, 8.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            horizontalLineToRelative(-8.0f)
            lineToRelative(-2.0f, -2.0f)
            close()
        }
    }

    val ExpandLess: ImageVector by lazy {
        materialIcon("ExpandLess") {
            moveTo(12.0f, 8.0f)
            lineToRelative(-6.0f, 6.0f)
            lineToRelative(1.41f, 1.41f)
            lineTo(12.0f, 10.83f)
            lineToRelative(4.59f, 4.58f)
            lineTo(18.0f, 14.0f)
            close()
        }
    }

    val ExpandMore: ImageVector by lazy {
        materialIcon("ExpandMore") {
            moveTo(16.59f, 8.59f)
            lineTo(12.0f, 13.17f)
            lineTo(7.41f, 8.59f)
            lineTo(6.0f, 10.0f)
            lineToRelative(6.0f, 6.0f)
            lineToRelative(6.0f, -6.0f)
            close()
        }
    }

    val Visibility: ImageVector by lazy {
        materialIcon("Visibility") {
            moveTo(12.0f, 4.5f)
            curveTo(7.0f, 4.5f, 2.73f, 7.61f, 1.0f, 12.0f)
            curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
            reflectiveCurveToRelative(9.27f, -3.11f, 11.0f, -7.5f)
            curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
            close()
            moveTo(12.0f, 17.0f)
            curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
            reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
            reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
            reflectiveCurveToRelative(-2.24f, 5.0f, -5.0f, 5.0f)
            close()
            moveTo(12.0f, 9.0f)
            curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
            reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
            reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
            reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
            close()
        }
    }

    val VisibilityOff: ImageVector by lazy {
        materialIcon("VisibilityOff") {
            moveTo(12.0f, 7.0f)
            curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
            curveToRelative(0.0f, 0.65f, -0.13f, 1.26f, -0.36f, 1.83f)
            lineToRelative(2.92f, 2.92f)
            curveToRelative(1.51f, -1.26f, 2.7f, -2.89f, 3.43f, -4.75f)
            curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
            curveToRelative(-1.4f, 0.0f, -2.74f, 0.25f, -3.98f, 0.7f)
            lineToRelative(2.16f, 2.16f)
            curveTo(10.74f, 7.13f, 11.35f, 7.0f, 12.0f, 7.0f)
            close()
            moveTo(2.0f, 4.27f)
            lineToRelative(2.28f, 2.28f)
            lineToRelative(0.46f, 0.46f)
            curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1.0f, 12.0f)
            curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
            curveToRelative(1.55f, 0.0f, 3.03f, -0.3f, 4.38f, -0.84f)
            lineToRelative(0.42f, 0.42f)
            lineTo(19.73f, 22.0f)
            lineTo(21.0f, 20.73f)
            lineTo(3.27f, 3.0f)
            lineTo(2.0f, 4.27f)
            close()
            moveTo(7.53f, 9.8f)
            lineToRelative(1.55f, 1.55f)
            curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
            curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
            curveToRelative(0.22f, 0.0f, 0.44f, -0.03f, 0.65f, -0.08f)
            lineToRelative(1.55f, 1.55f)
            curveToRelative(-0.67f, 0.33f, -1.41f, 0.53f, -2.2f, 0.53f)
            curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
            curveToRelative(0.0f, -0.79f, 0.2f, -1.53f, 0.53f, -2.2f)
            close()
            moveTo(11.84f, 9.02f)
            lineToRelative(3.15f, 3.15f)
            lineToRelative(0.02f, -0.16f)
            curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f)
            lineToRelative(-0.17f, 0.01f)
            close()
        }
    }

    val Pause: ImageVector by lazy {
        materialIcon("Pause") {
            moveTo(6.0f, 19.0f)
            horizontalLineToRelative(4.0f)
            lineTo(10.0f, 5.0f)
            lineTo(6.0f, 5.0f)
            verticalLineToRelative(14.0f)
            close()
            moveTo(14.0f, 5.0f)
            verticalLineToRelative(14.0f)
            horizontalLineToRelative(4.0f)
            lineTo(18.0f, 5.0f)
            horizontalLineToRelative(-4.0f)
            close()
        }
    }

    /**
     * Panel-left icon: expand the navigation sidebar.
     * Represents a layout with the left panel visible (three horizontal lines,
     * left column highlighted).
     */
    val SidebarOpen: ImageVector by lazy {
        materialIcon("SidebarOpen") {
            // outer rect
            moveTo(3.0f, 3.0f)
            horizontalLineToRelative(18.0f)
            verticalLineToRelative(18.0f)
            horizontalLineToRelative(-18.0f)
            close()
            // fill: draw background by subtracting — use two filled rects instead
            // Left panel column (filled accent)
            moveTo(3.0f, 3.0f)
            verticalLineToRelative(18.0f)
            horizontalLineToRelative(6.0f)
            verticalLineToRelative(-18.0f)
            close()
        }
    }

    /**
     * Panel-left-close icon: collapse the navigation sidebar.
     * Uses a simpler three-bar hamburger with a leftward chevron hint.
     */
    val SidebarClose: ImageVector by lazy {
        materialIcon("SidebarClose") {
            // top bar
            moveTo(3.0f, 18.0f)
            horizontalLineToRelative(18.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-18.0f)
            close()
            // middle bar
            moveTo(3.0f, 13.0f)
            horizontalLineToRelative(18.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-18.0f)
            close()
            // bottom bar
            moveTo(3.0f, 8.0f)
            horizontalLineToRelative(18.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-18.0f)
            close()
        }
    }

    val DragHandle: ImageVector by lazy {
        materialIcon("DragHandle") {
            moveTo(9.0f, 3.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            lineTo(9.0f, 5.0f)
            close()
            moveTo(9.0f, 7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            lineTo(9.0f, 9.0f)
            close()
            moveTo(9.0f, 11.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            lineTo(9.0f, 13.0f)
            close()
            moveTo(13.0f, 3.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
            moveTo(13.0f, 7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
            moveTo(13.0f, 11.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
        }
    }

    val MoreVert: ImageVector by lazy {
        materialIcon("MoreVert") {
            moveTo(12.0f, 8.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
            reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
            close()
            moveTo(12.0f, 10.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
            reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(12.0f, 16.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
            reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
            close()
        }
    }
}

private fun materialIcon(
    name: String,
    pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = pathBuilder)
    }.build()
