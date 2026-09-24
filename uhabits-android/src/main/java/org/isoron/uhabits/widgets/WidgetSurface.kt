package org.isoron.uhabits.widgets

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews

/** Tints the launcher-hosted widget background. This does not enable wallpaper blur. */
internal object WidgetSurface {
    fun apply(views: RemoteViews, opacity: Int = 102, color: Int? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val tintColor = color?.let {
                Color.argb(opacity.coerceIn(1, 254), Color.red(it), Color.green(it), Color.blue(it))
            } ?: Color.HSVToColor(opacity.coerceIn(1, 254), floatArrayOf(0f, 0f, 1f))
            views.setColorStateList(
                android.R.id.background,
                "setBackgroundTintList",
                ColorStateList.valueOf(tintColor)
            )
        }
    }
}
