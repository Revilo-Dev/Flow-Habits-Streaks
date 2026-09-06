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
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.requestFocusWithKeyboard
import org.isoron.uhabits.utils.sres

/** A OneUI-style search field that stays above the navigation bar and IME. */
class FlowHomeSearchBar(context: Context) : LinearLayout(context) {
    private var onQueryChanged: ((String) -> Unit)? = null
    var onOpen: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    private val queryInput = EditText(context).apply {
        setTextAppearance(R.style.TextAppearance_Flow_Body)
        hint = resources.getString(R.string.search)
        setHintTextColor(sres.getColor(R.attr.flowTextTertiaryColor))
        setTextColor(sres.getColor(R.attr.flowTextPrimaryColor))
        isSingleLine = true
        background = null
        layoutParams = LayoutParams(0, WRAP_CONTENT, 1f)
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                onQueryChanged?.invoke(s?.toString().orEmpty())
            }
        })
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.flow_toolbar_height)
        val horizontalPadding = dim(R.dimen.flow_medium_spacing).toInt()
        setPadding(horizontalPadding, 0, horizontalPadding, 0)
        background = GradientDrawable().apply {
            cornerRadius = dim(R.dimen.flow_toolbar_pill_radius)
            setColor(sres.getColor(R.attr.flowSurfaceSecondaryColor))
        }
        ViewCompat.setElevation(
            this,
            if (ColorUtils.calculateLuminance(sres.getColor(R.attr.flowBackgroundColor)) > 0.5) {
                dim(R.dimen.flow_card_elevation)
            } else {
                0f
            }
        )

        addView(
            ImageView(context).apply {
                setImageResource(R.drawable.search)
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.flow_medium_spacing),
                    resources.getDimensionPixelSize(R.dimen.flow_medium_spacing),
                    resources.getDimensionPixelSize(R.dimen.flow_medium_spacing),
                    resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
                )
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LayoutParams(
                resources.getDimensionPixelSize(R.dimen.flow_icon_container_size),
                resources.getDimensionPixelSize(R.dimen.flow_icon_container_size)
            )
        )
        addView(queryInput)
        addView(
            ImageButton(context).apply {
                background = null
                contentDescription = resources.getString(R.string.flow_close_search)
                setImageResource(R.drawable.flow_ic_close)
                setOnClickListener { close() }
            },
            LayoutParams(
                resources.getDimensionPixelSize(R.dimen.flow_min_touch_target),
                resources.getDimensionPixelSize(R.dimen.flow_min_touch_target)
            )
        )
        visibility = GONE

        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val params = view.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            params?.bottomMargin = dim(R.dimen.flow_body_padding).toInt() +
                maxOf(systemBars.bottom, ime.bottom)
            if (params != null) view.layoutParams = params
            insets
        }
    }

    fun open(initialQuery: String, onQueryChanged: (String) -> Unit) {
        this.onQueryChanged = null
        queryInput.setText(initialQuery)
        queryInput.setSelection(queryInput.text.length)
        this.onQueryChanged = onQueryChanged
        onOpen?.invoke()
        visibility = VISIBLE
        alpha = 0f
        translationY = dim(R.dimen.flow_large_spacing)
        animate().alpha(1f).translationY(0f).setDuration(180).start()
        queryInput.requestFocusWithKeyboard()
    }

    fun close() {
        onQueryChanged?.invoke("")
        onQueryChanged = null
        queryInput.clearFocus()
        animate().alpha(0f).translationY(dim(R.dimen.flow_large_spacing)).setDuration(130)
            .withEndAction {
                visibility = GONE
                alpha = 1f
                translationY = 0f
                onDismiss?.invoke()
            }
            .start()
    }
}
