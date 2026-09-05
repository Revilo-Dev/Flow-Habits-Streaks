/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 */

package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.sres

class FlowSelectionActionBar(context: Context) : LinearLayout(context) {
    private val edit = addAction(R.drawable.flow_ic_edit, R.string.edit)
    private val color = addAction(R.drawable.flow_ic_color, R.string.color)
    private val archive = addAction(R.drawable.flow_ic_archive, R.string.archive)
    private val unarchive = addAction(R.drawable.flow_ic_unarchive, R.string.unarchive)
    private val delete = addAction(R.drawable.flow_ic_delete, R.string.delete)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val inset = dim(R.dimen.flow_small_spacing).toInt()
        setPadding(inset, inset, inset, inset)
        background = resources.getDrawable(R.drawable.flow_selection_actions_background, context.theme)
        clipToOutline = true
        ViewCompat.setElevation(
            this,
            if (ColorUtils.calculateLuminance(sres.getColor(R.attr.flowBackgroundColor)) > 0.5) {
                dim(R.dimen.flow_card_elevation)
            } else {
                0f
            }
        )
        visibility = GONE
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
        color.visibility = VISIBLE
        archive.visibility = if (canArchive) VISIBLE else GONE
        unarchive.visibility = if (canUnarchive) VISIBLE else GONE
        delete.visibility = VISIBLE
        edit.setOnClickListener { onEdit() }
        color.setOnClickListener { onColor() }
        archive.setOnClickListener { onArchive() }
        unarchive.setOnClickListener { onUnarchive() }
        delete.setOnClickListener { onDelete() }
    }

    fun show() {
        if (visibility == VISIBLE) return
        animate().cancel()
        visibility = VISIBLE
        alpha = 0f
        translationY = dim(R.dimen.flow_large_spacing)
        animate().alpha(1f).translationY(0f).setDuration(180).start()
    }

    fun hide() {
        if (visibility != VISIBLE) return
        animate().cancel()
        animate().alpha(0f).translationY(dim(R.dimen.flow_large_spacing)).setDuration(140)
            .withEndAction {
                visibility = GONE
                alpha = 1f
                translationY = 0f
            }.start()
    }

    private fun addAction(icon: Int, description: Int): ImageButton {
        return ImageButton(context).apply {
            layoutParams = LayoutParams(
                resources.getDimensionPixelSize(R.dimen.flow_min_touch_target),
                resources.getDimensionPixelSize(R.dimen.flow_min_touch_target)
            )
            background = resources.getDrawable(R.drawable.flow_selection_action_background, context.theme)
            contentDescription = resources.getString(description)
            setImageResource(icon)
            setPadding(
                dim(R.dimen.flow_medium_spacing).toInt(),
                dim(R.dimen.flow_medium_spacing).toInt(),
                dim(R.dimen.flow_medium_spacing).toInt(),
                dim(R.dimen.flow_medium_spacing).toInt()
            )
            addView(this)
        }
    }
}
