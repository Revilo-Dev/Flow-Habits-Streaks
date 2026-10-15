package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import com.google.android.material.button.MaterialButton
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.sres

class FlowSelectionActionBar(context: Context) : LinearLayout(context) {
    var onExit: (() -> Unit)? = null
    private val count = TextView(context).apply {
        setTextAppearance(R.style.TextAppearance_Flow_Supporting)
        accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE
    }
    private val actions = LinearLayout(context)
    private val edit = action(R.drawable.flow_ic_edit, R.string.edit)
    private val color = action(R.drawable.palette, R.string.color)
    private val archive = action(R.drawable.archive, R.string.archive)
    private val unarchive = action(R.drawable.flow_ic_unarchive, R.string.unarchive)
    private val delete = action(R.drawable.delete, R.string.delete)

    init {
        orientation = VERTICAL
        val padding = resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
        setPadding(padding, 0, padding, padding)
        setBackgroundResource(R.drawable.flow_selection_actions_background)
        val target = resources.getDimensionPixelSize(R.dimen.flow_min_touch_target)
        val heading = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        heading.addView(count, LayoutParams(0, WRAP_CONTENT, 1f))
        heading.addView(
            ImageButton(context).apply {
                setImageResource(R.drawable.flow_ic_close)
                background = null
                setColorFilter(sres.getColor(R.attr.flowTextPrimaryColor))
                contentDescription = resources.getString(R.string.flow_exit_selection)
                TooltipCompat.setTooltipText(this, contentDescription)
                setOnClickListener { onExit?.invoke() }
            },
            LayoutParams(target, target)
        )
        addView(heading, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(
            HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = true
                addView(actions)
            },
            LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        )
        visibility = GONE
    }

    fun setSelectionCount(selected: Int) {
        count.text = resources.getQuantityString(R.plurals.flow_selection_count, selected, selected)
    }

    fun setActions(
        canEdit: Boolean,
        canArchive: Boolean,
        canUnarchive: Boolean,
        onEdit: () -> Unit,
        onColor: () -> Unit,
        onArchive: () -> Unit,
        onUnarchive: () -> Unit,
        onDelete: () -> Unit
    ) {
        edit.visibility = if (canEdit) VISIBLE else GONE
        archive.visibility = if (canArchive) VISIBLE else GONE
        unarchive.visibility = if (canUnarchive) VISIBLE else GONE
        edit.setOnClickListener { onEdit() }
        color.setOnClickListener { onColor() }
        archive.setOnClickListener { onArchive() }
        unarchive.setOnClickListener { onUnarchive() }
        delete.setOnClickListener { onDelete() }
    }

    fun show() {
        visibility = VISIBLE
        ViewCompat.requestApplyInsets(this)
    }

    fun hide() {
        visibility = GONE
    }

    private fun action(iconResource: Int, label: Int) = MaterialButton(
        context,
        null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply {
        text = resources.getString(label)
        isAllCaps = false
        icon = AppCompatResources.getDrawable(context, iconResource)
        iconTint = android.content.res.ColorStateList.valueOf(sres.getColor(R.attr.flowTextPrimaryColor))
        setTextColor(sres.getColor(R.attr.flowTextPrimaryColor))
        minHeight = resources.getDimensionPixelSize(R.dimen.flow_min_touch_target)
        actions.addView(this, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
    }
}
