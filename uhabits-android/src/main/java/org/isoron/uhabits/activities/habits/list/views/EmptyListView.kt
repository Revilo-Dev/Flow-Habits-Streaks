package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.button.MaterialButton
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.sres

class EmptyListView(context: Context) : LinearLayout(context) {
    var onCreateHabit: (() -> Unit)? = null
    var onClearFilters: (() -> Unit)? = null
    val iconView = AppCompatImageView(context).apply {
        imageTintList = ColorStateList.valueOf(sres.getColor(R.attr.flowTextSecondaryColor))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    val textTextView = TextView(context).apply {
        setTextAppearance(R.style.TextAppearance_Flow_Body)
        gravity = Gravity.CENTER
        accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE
    }
    private val action = MaterialButton(
        context,
        null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply { isAllCaps = false }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val padding = resources.getDimensionPixelSize(R.dimen.flow_screen_padding)
        setPadding(padding, padding, padding, padding)
        addView(iconView, LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.flow_min_touch_target)))
        addView(textTextView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = padding })
        addView(action, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        visibility = GONE
    }

    fun showDone() {
        show(R.drawable.checkbox_checked, resources.getString(R.string.no_habits_left_to_do), R.string.flow_clear_filters) {
            onClearFilters?.invoke()
        }
    }

    fun showEmpty() {
        show(R.drawable.flow_ic_calendar_task, resources.getString(R.string.no_habits_found), R.string.add_habit) {
            onCreateHabit?.invoke()
        }
    }

    fun showNoResults(query: String) {
        show(R.drawable.search, resources.getString(R.string.flow_no_search_results, query), R.string.flow_clear_filters) {
            onClearFilters?.invoke()
        }
    }

    fun showFiltered() {
        show(R.drawable.flow_ic_filter, resources.getString(R.string.flow_no_filter_results), R.string.flow_clear_filters) {
            onClearFilters?.invoke()
        }
    }

    private fun show(icon: Int, message: String, label: Int, callback: () -> Unit) {
        visibility = VISIBLE
        iconView.setImageResource(icon)
        textTextView.text = message
        action.setText(label)
        action.setOnClickListener { callback() }
    }

    fun hide() {
        visibility = GONE
    }
}
