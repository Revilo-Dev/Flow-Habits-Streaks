package org.isoron.uhabits.activities.common.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.databinding.CheckmarkPopupBinding
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

fun CheckmarkPopupBinding.prepareEntryLayout() {
    val configuration = root.resources.configuration
    val vertical = configuration.fontScale > 1.2f || configuration.screenWidthDp < 360
    numberFooter.orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
    for (index in 0 until numberFooter.childCount) {
        val child = numberFooter.getChildAt(index)
        child.layoutParams = LinearLayoutCompat.LayoutParams(
            if (vertical) ViewGroup.LayoutParams.MATCH_PARENT else 0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (vertical) 0f else 1f
        )
    }
}

fun CheckmarkPopupBinding.prepareEntryActions(activity: Activity, arguments: Bundle) {
    prepareEntryLayout()
    val app = activity.application as HabitsApplication
    val habit = app.component.habitList.getById(arguments.getLong("habit", -1)) ?: return
    val formatter = DateFormat.getDateInstance(DateFormat.LONG).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    entryTitle.text = activity.getString(
        R.string.flow_entry_title,
        habit.name,
        formatter.format(Date(arguments.getLong("date")))
    )
    entryTitle.visibility = View.VISIBLE
    value.contentDescription = if (habit.unit.isNotBlank()) {
        activity.getString(R.string.flow_entry_value_unit, habit.unit)
    } else {
        activity.getString(R.string.flow_entry_value)
    }
    valueLabel.text = value.contentDescription
}

fun Dialog.prepareEntryWindow(activity: Activity) {
    val density = activity.resources.displayMetrics.density
    val margin = activity.resources.getDimensionPixelSize(R.dimen.flow_screen_padding)
    val available = activity.window.decorView.width.takeIf { it > 0 }
        ?: (activity.resources.configuration.screenWidthDp * density).toInt()
    window?.apply {
        setBackgroundDrawableResource(android.R.color.transparent)
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        setLayout(minOf(available - margin * 2, (420 * density).toInt()).coerceAtLeast(1), ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}

// Restored dialogs use the saved habit and date when their callback is unavailable
fun Bundle.saveRestoredEntry(context: Context, value: Int, notes: String) {
    val component = (context.applicationContext as HabitsApplication).component
    val habit = component.habitList.getById(getLong("habit", -1)) ?: return
    if (!containsKey("date")) return
    component.commandRunner.run(
        CreateRepetitionCommand(component.habitList, habit, LocalDate.fromUnixTime(getLong("date")), value, notes)
    )
}
