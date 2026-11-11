package org.isoron.uhabits.activities.settings

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.flowPopupSurface
import org.isoron.uhabits.utils.sres

class FlowChoicePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ListPreference(context, attrs) {
    private var anchor: View? = null
    private var popup: PopupWindow? = null

    init {
        widgetLayoutResource = R.layout.flow_preference_dropdown
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        anchor = holder.itemView
    }

    override fun onDetached() {
        popup?.dismiss()
        anchor = null
        super.onDetached()
    }

    override fun onClick() {
        val anchor = anchor ?: return
        popup?.dismiss()
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val rows = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val scroll = ScrollView(context).apply {
            addView(rows)
        }
        val frame = Rect().also(anchor::getWindowVisibleDisplayFrame)
        val width = dp(248).coerceAtMost((frame.width() - dp(32)).coerceAtLeast(1))
        val window = PopupWindow(scroll, width, WRAP_CONTENT, true).apply {
            setBackgroundDrawable(anchor.flowPopupSurface(dp(24).toFloat()))
            isOutsideTouchable = true
            elevation = anchor.dim(R.dimen.flow_popup_elevation)
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            animationStyle = R.style.Animation_Flow_Menu
        }
        entries.forEachIndexed { index, label ->
            rows.addView(
                TextView(context).apply {
                    setTextAppearance(R.style.TextAppearance_Flow_Body)
                    setTextColor(anchor.sres.getColor(R.attr.flowTextPrimaryColor))
                    text = label
                    isSelected = value == entryValues[index].toString()
                    if (isSelected) {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.flow_ic_entry_check, 0)
                        compoundDrawableTintList = android.content.res.ColorStateList.valueOf(anchor.sres.getColor(R.attr.flowAccentColor))
                    }
                    minimumHeight = dp(52)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    setBackgroundResource(R.drawable.flow_touch_feedback)
                    setOnClickListener {
                        val chosen = entryValues[index].toString()
                        if (callChangeListener(chosen)) value = chosen
                        window.dismiss()
                    }
                },
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            )
        }
        popup = window
        window.showAsDropDown(anchor, 0, dp(4), Gravity.END)
    }
}
