package org.isoron.uhabits.activities.common.views

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import com.google.android.material.button.MaterialButton
import org.isoron.platform.gui.AndroidDataView
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.sres
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

class EntryNotePopup(
    private val anchor: AndroidDataView,
    private val date: LocalDate,
    private val notes: String,
    private val onEdit: () -> Unit
) {
    private var popup: PopupWindow? = null

    fun dismiss() {
        popup?.dismiss()
        popup = null
    }

    fun show() {
        if (!anchor.isAttachedToWindow) return
        val context = anchor.context
        val resources = context.resources
        val gap = resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
        val margin = resources.getDimensionPixelSize(R.dimen.flow_screen_padding)
        val target = resources.getDimensionPixelSize(R.dimen.flow_min_touch_target)
        val frame = Rect().also { anchor.getWindowVisibleDisplayFrame(it) }
        val width = minOf((360 * resources.displayMetrics.density).roundToInt(), frame.width() - 2 * margin)
            .coerceAtLeast(1)
        val maxHeight = minOf((360 * resources.displayMetrics.density).roundToInt(), frame.height() - 2 * margin)
            .coerceAtLeast(1)
        val formatter = DateFormat.getDateInstance(DateFormat.LONG).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val title = context.getString(R.string.flow_note_for_date, formatter.format(Date(date.unixTime)))
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.flow_large_spacing)
            setPadding(padding, padding, padding, padding)
        }
        val heading = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(
            TextView(context).apply {
                text = title
                setTextAppearance(R.style.TextAppearance_Flow_Body)
                ViewCompat.setAccessibilityHeading(this, true)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        heading.addView(
            ImageButton(context).apply {
                setImageResource(R.drawable.flow_ic_close)
                setColorFilter(anchor.sres.getColor(R.attr.flowTextPrimaryColor))
                background = null
                contentDescription = context.getString(R.string.flow_close_note)
                TooltipCompat.setTooltipText(this, contentDescription)
                setOnClickListener { dismiss() }
            },
            LinearLayout.LayoutParams(target, target)
        )
        content.addView(heading)
        content.addView(
            TextView(context).apply {
                text = notes
                setTextAppearance(R.style.TextAppearance_Flow_Body)
                setTextIsSelectable(true)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        content.addView(
            MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                text = context.getString(R.string.flow_edit_entry)
                minHeight = target
                isAllCaps = false
                setTextColor(anchor.sres.getColor(R.attr.flowAccentColor))
                setOnClickListener {
                    dismiss()
                    onEdit()
                }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = gap
            }
        )
        val scroll = ScrollView(context).apply {
            setBackgroundResource(R.drawable.flow_popup_background)
            clipToOutline = true
            addView(content)
        }
        ViewCompat.setAccessibilityPaneTitle(scroll, title)
        scroll.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(maxHeight, android.view.View.MeasureSpec.AT_MOST)
        )
        val location = IntArray(2).also { anchor.getLocationOnScreen(it) }
        val tapX = location[0] + anchor.lastTouchX.toInt()
        val tapY = location[1] + anchor.lastTouchY.toInt()
        val x = (tapX - width / 2).coerceIn(frame.left + margin, maxOf(frame.left + margin, frame.right - margin - width))
        val below = tapY + gap
        val y = if (below + scroll.measuredHeight <= frame.bottom - margin) {
            below
        } else {
            tapY - gap - scroll.measuredHeight
        }.coerceIn(frame.top + margin, maxOf(frame.top + margin, frame.bottom - margin - scroll.measuredHeight))

        // Keep the note inside the usable window and leave entry changes to the edit action
        popup = PopupWindow(scroll, width, scroll.measuredHeight, true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = resources.getDimension(R.dimen.flow_card_elevation)
            setOnDismissListener { anchor.requestFocus() }
            showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        }
    }
}
