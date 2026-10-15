package org.isoron.uhabits.activities.common.views

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import org.isoron.platform.gui.AndroidDataView
import org.isoron.uhabits.R
import org.isoron.uhabits.core.ui.screens.habits.show.views.HistoryCardPresenter
import org.isoron.uhabits.core.ui.views.HistoryChart
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

class AccessibleHistoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AndroidDataView(context, attrs) {
    private val chart get() = view as? HistoryChart
    private var geometryKey = ""
    private val helper = object : ExploreByTouchHelper(this) {
        override fun getVirtualViewAt(x: Float, y: Float): Int {
            val density = resources.displayMetrics.density
            return chart?.visibleDays()?.indexOfFirst { day ->
                x / density >= day.left && x / density <= day.left + day.size &&
                    y / density >= day.top && y / density <= day.top + day.size
            }?.takeIf { it >= 0 } ?: INVALID_ID
        }

        override fun getVisibleVirtualViews(ids: MutableList<Int>) {
            chart?.visibleDays()?.indices?.forEach { ids.add(it) }
        }

        override fun onPopulateNodeForVirtualView(id: Int, node: AccessibilityNodeInfoCompat) {
            val day = chart?.visibleDays()?.getOrNull(id)
            if (day == null) {
                node.contentDescription = context.getString(R.string.calendar)
                node.setBoundsInParent(Rect(0, 0, 1, 1))
                return
            }
            val density = resources.displayMetrics.density
            node.setBoundsInParent(
                Rect(
                    (day.left * density).toInt(),
                    (day.top * density).toInt(),
                    ((day.left + day.size) * density).toInt(),
                    ((day.top + day.size) * density).toInt()
                )
            )
            val dateFormat = DateFormat.getDateInstance(DateFormat.FULL).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val state = context.getString(
                when (day.square) {
                    HistoryChart.Square.ON -> R.string.flow_calendar_completed
                    HistoryChart.Square.DIMMED -> R.string.flow_calendar_scheduled
                    HistoryChart.Square.HATCHED -> R.string.flow_calendar_skipped
                    HistoryChart.Square.GREY -> R.string.flow_calendar_partial
                    HistoryChart.Square.OFF -> R.string.flow_calendar_unrecorded
                }
            )
            val note = if (day.hasNotes) " · " + context.getString(R.string.flow_calendar_has_note) else ""
            node.contentDescription = context.getString(
                R.string.flow_calendar_day_description,
                dateFormat.format(Date(day.date.unixTime)),
                state + note
            )
            node.className = android.widget.Button::class.java.name
            node.isClickable = true
            node.isLongClickable = true
            node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
            node.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_LONG_CLICK,
                    context.getString(R.string.flow_edit_entry)
                )
            )
        }

        override fun onPerformActionForVirtualView(id: Int, action: Int, arguments: Bundle?): Boolean {
            val day = chart?.visibleDays()?.getOrNull(id) ?: return false
            val density = resources.displayMetrics.density
            setInteractionPoint(
                ((day.left + day.size / 2) * density).toFloat(),
                ((day.top + day.size / 2) * density).toFloat()
            )
            when (action) {
                AccessibilityNodeInfoCompat.ACTION_CLICK -> chart?.onDateClickedListener?.onDateShortPress(day.date)
                AccessibilityNodeInfoCompat.ACTION_LONG_CLICK -> {
                    val listener = chart?.onDateClickedListener
                    if (listener is HistoryCardPresenter) {
                        listener.onEditEntry(day.date)
                    } else {
                        listener?.onDateLongPress(day.date)
                    }
                }
                else -> return false
            }
            sendEventForVirtualView(id, AccessibilityEvent.TYPE_VIEW_CLICKED)
            return true
        }
    }

    init {
        isFocusable = true
        ViewCompat.setAccessibilityDelegate(this, helper)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val key = "${view.hashCode()}:${view?.dataOffset}:$width:$height"
        if (key != geometryKey) {
            geometryKey = key
            helper.invalidateRoot()
        }
    }

    override fun dispatchHoverEvent(event: MotionEvent) =
        helper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    override fun dispatchKeyEvent(event: KeyEvent) =
        helper.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        helper.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }
}
