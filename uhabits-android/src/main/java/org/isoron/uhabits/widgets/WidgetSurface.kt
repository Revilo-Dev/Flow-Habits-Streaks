package org.isoron.uhabits.widgets

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews

// Tint the launcher background without applying wallpaper blur
internal object WidgetSurface {
    fun apply(views: RemoteViews, opacity: Int = 102, color: Int? = null) {
        // Older Android versions keep the background defined in the widget layout
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
