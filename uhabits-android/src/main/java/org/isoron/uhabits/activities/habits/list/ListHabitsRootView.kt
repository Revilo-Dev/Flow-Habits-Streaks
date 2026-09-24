/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.habits.list

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View.GONE
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.tatarka.inject.annotations.Inject
import nl.dionsegijn.konfetti.xml.KonfettiView
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.ScrollableChart
import org.isoron.uhabits.activities.common.views.TaskProgressBar
import org.isoron.uhabits.activities.habits.list.views.EmptyListView
import org.isoron.uhabits.activities.habits.list.views.FlowLargeHeaderView
import org.isoron.uhabits.activities.habits.list.views.FlowHomeSearchBar
import org.isoron.uhabits.activities.habits.list.views.FlowSelectionActionBar
import org.isoron.uhabits.activities.habits.list.views.HabitCardListAdapter
import org.isoron.uhabits.activities.habits.list.views.HabitCardListView
import org.isoron.uhabits.activities.habits.list.views.HabitCardListViewFactory
import org.isoron.uhabits.activities.habits.list.views.HeaderView
import org.isoron.uhabits.activities.habits.list.views.HintView
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.core.ui.screens.habits.list.HintListFactory
import org.isoron.uhabits.core.utils.MidnightTimer
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.inject.ActivityScope
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.addFlowScrollFades
import org.isoron.uhabits.utils.buildFlowToolbar
import org.isoron.uhabits.utils.currentTheme
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.setupToolbar
import org.isoron.uhabits.utils.sres
import org.isoron.uhabits.utils.bindFlowHeader
import org.isoron.uhabits.utils.FlowHeaderBehavior
import kotlin.math.abs

const val MAX_CHECKMARK_COUNT = 60
const val HOME_CHECKMARK_COUNT = 4

@Inject
@ActivityScope
class ListHabitsRootView(
    @ActivityContext context: Context,
    hintListFactory: HintListFactory,
    preferences: Preferences,
    midnightTimer: MidnightTimer,
    runner: TaskRunner,
    private val listAdapter: HabitCardListAdapter,
    habitCardListViewFactory: HabitCardListViewFactory
) : FrameLayout(context), ModelObservable.Listener {

    val listView: HabitCardListView = habitCardListViewFactory.create()
    val llEmpty = EmptyListView(context)
    val tbar = buildFlowToolbar()
    val konfettiView = KonfettiView(context).apply {
        translationZ = 10f
    }
    val progressBar = TaskProgressBar(context, runner)
    val hintView: HintView
    val header = HeaderView(context, preferences, midnightTimer)
    val largeHeader = FlowLargeHeaderView(context, midnightTimer, preferences)
    val selectionActions = FlowSelectionActionBar(context).apply {
        id = R.id.flowSelectionActions
    }
    val reorderPrompt = TextView(context).apply {
        id = R.id.flowReorderPrompt
        text = resources.getString(R.string.flow_drag_to_reorder)
        setTextAppearance(R.style.TextAppearance_Flow_Metadata)
        setTextColor(sres.getColor(R.attr.flowTextTertiaryColor))
        gravity = Gravity.CENTER
        visibility = GONE
    }
    private val homeSearchBar = FlowHomeSearchBar(context).apply {
        id = R.id.flowHomeSearchBar
    }
    private val collapsedBrand = TextView(context).apply {
        text = resources.getString(R.string.flow_app_title)
        setTextAppearance(R.style.TextAppearance_Flow_Body)
        setTextColor(sres.getColor(R.attr.flowTextPrimaryColor))
        val icon = AppCompatResources.getDrawable(context, R.drawable.onboarding_appicon)
        val size = (28 * resources.displayMetrics.density).toInt()
        icon?.setBounds(0, 0, size, size)
        setCompoundDrawablesRelative(icon, null, null, null)
        compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
        gravity = Gravity.CENTER_VERTICAL
        visibility = GONE
    }
    var onCreateHabit: (() -> Unit)? = null

    private val addButton = FloatingActionButton(context).apply {
        id = R.id.actionCreateHabit
        customSize = resources.getDimensionPixelSize(R.dimen.flow_fab_size)
        setImageResource(R.drawable.flow_ic_add)
        backgroundTintList = ColorStateList.valueOf(sres.getColor(R.attr.flowSurfaceSecondaryColor))
        imageTintList = ColorStateList.valueOf(sres.getColor(R.attr.flowAccentColor))
        contentDescription = resources.getString(R.string.add_habit)
        compatElevation = if (
            ColorUtils.calculateLuminance(sres.getColor(R.attr.flowBackgroundColor)) > 0.5
        ) {
            dim(R.dimen.flow_fab_elevation)
        } else {
            0f
        }
        setOnClickListener { onCreateHabit?.invoke() }
    }

    init {
        val hints = resources.getStringArray(R.array.hints)
        val hintList = hintListFactory.create(hints)
        hintView = HintView(context, hintList)
        largeHeader.setHintView(hintView)

        val flowBackground = sres.getColor(R.attr.flowBackgroundColor)
        val toolbarHeight = resources.getDimensionPixelSize(R.dimen.flow_toolbar_height)
        val appBar = AppBarLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            elevation = 0f
            applyToolbarInsets()
        }
        val collapsingToolbar = CollapsingToolbarLayout(context).apply {
            isTitleEnabled = false
            title = ""
            setExpandedTitleColor(Color.TRANSPARENT)
            setCollapsedTitleTextColor(Color.TRANSPARENT)
            setContentScrimColor(Color.TRANSPARENT)
            setStatusBarScrimColor(Color.TRANSPARENT)
            addView(
                largeHeader,
                CollapsingToolbarLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = toolbarHeight
                    collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX
                    parallaxMultiplier = 0.7f
                }
            )
            addView(
                tbar,
                CollapsingToolbarLayout.LayoutParams(MATCH_PARENT, toolbarHeight).apply {
                    collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PIN
                }
            )
        }
        appBar.addView(
            collapsingToolbar,
            AppBarLayout.LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT
            ).apply {
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
            }
        )
        appBar.addView(
            header,
            AppBarLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            }
        )
        tbar.addView(collapsedBrand, androidx.appcompat.widget.Toolbar.LayoutParams(
            WRAP_CONTENT, WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL
        ).apply {
            marginStart = resources.getDimensionPixelSize(R.dimen.flow_screen_padding)
        })
        var previousCollapsed: Boolean? = null
        appBar.bindFlowHeader(tbar) { collapsed, _ ->
            if (previousCollapsed == collapsed) return@bindFlowHeader
            val animate = previousCollapsed != null
            previousCollapsed = collapsed
            if (!animate) {
                largeHeader.visibility = if (collapsed) INVISIBLE else VISIBLE
                largeHeader.alpha = 1f
                collapsedBrand.visibility = if (collapsed) VISIBLE else GONE
                collapsedBrand.alpha = 1f
                collapsedBrand.translationY = 0f
            } else if (collapsed) {
                largeHeader.animate().cancel()
                largeHeader.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction { largeHeader.visibility = INVISIBLE }
                    .start()
                collapsedBrand.apply {
                    visibility = VISIBLE
                    alpha = 0f
                    translationY = -dim(R.dimen.flow_small_spacing)
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setStartDelay(55)
                        .setDuration(170)
                        .start()
                }
            } else {
                collapsedBrand.animate().cancel()
                collapsedBrand.animate()
                    .alpha(0f)
                    .translationY(-dim(R.dimen.flow_small_spacing))
                    .setDuration(110)
                    .withEndAction { collapsedBrand.visibility = GONE }
                    .start()
                largeHeader.apply {
                    visibility = VISIBLE
                    alpha = 0f
                    animate().alpha(1f).setDuration(170).start()
                }
            }
            tbar.title = ""
        }
        val content = FrameLayout(context).apply {
            setBackgroundColor(flowBackground)
            addView(listView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(llEmpty, LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(
                progressBar,
                LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = -resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
                }
            )
        }
        content.addFlowScrollFades(listView)
        val rootView = CoordinatorLayout(context).apply {
            setBackgroundColor(flowBackground)
            addView(appBar, CoordinatorLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                behavior = FlowHeaderBehavior(context)
            })
            addView(
                content,
                CoordinatorLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    behavior = AppBarLayout.ScrollingViewBehavior()
                }
            )
            addView(
                homeSearchBar,
                CoordinatorLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    gravity = Gravity.BOTTOM
                    marginStart = resources.getDimensionPixelSize(R.dimen.flow_body_padding)
                    marginEnd = resources.getDimensionPixelSize(R.dimen.flow_body_padding)
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.flow_body_padding)
                }
            )
            addView(
                selectionActions,
                CoordinatorLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.flow_selection_actions_bottom_margin)
                }
            )
            addView(
                reorderPrompt,
                CoordinatorLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.flow_reorder_prompt_bottom_margin)
                }
            )
            addView(
                addButton,
                CoordinatorLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.flow_fab_size),
                    resources.getDimensionPixelSize(R.dimen.flow_fab_size)
                ).apply {
                    gravity = Gravity.END or Gravity.BOTTOM
                    marginEnd = resources.getDimensionPixelSize(R.dimen.flow_fab_inline_margin)
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.flow_fab_inline_margin)
                }
            )
            addView(
                konfettiView,
                CoordinatorLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
        }
        homeSearchBar.onOpen = { addButton.visibility = INVISIBLE }
        homeSearchBar.onDismiss = { addButton.visibility = VISIBLE }
        val baseFabBottomMargin = resources.getDimensionPixelSize(R.dimen.flow_fab_inline_margin)
        ViewCompat.setOnApplyWindowInsetsListener(addButton) { button, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (button.layoutParams as CoordinatorLayout.LayoutParams).apply {
                bottomMargin = baseFabBottomMargin + systemBars.bottom
                button.layoutParams = this
            }
            insets
        }
        val baseSelectionBottomMargin =
            resources.getDimensionPixelSize(R.dimen.flow_selection_actions_bottom_margin)
        ViewCompat.setOnApplyWindowInsetsListener(selectionActions) { actions, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (actions.layoutParams as CoordinatorLayout.LayoutParams).apply {
                bottomMargin = baseSelectionBottomMargin + systemBars.bottom
                actions.layoutParams = this
            }
            insets
        }
        val baseReorderPromptBottomMargin =
            resources.getDimensionPixelSize(R.dimen.flow_reorder_prompt_bottom_margin)
        ViewCompat.setOnApplyWindowInsetsListener(reorderPrompt) { prompt, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (prompt.layoutParams as CoordinatorLayout.LayoutParams).apply {
                bottomMargin = baseReorderPromptBottomMargin + systemBars.bottom
                prompt.layoutParams = this
            }
            insets
        }
        rootView.setupToolbar(
            toolbar = tbar,
            title = "",
            color = PaletteColor(17),
            displayHomeAsUpEnabled = false,
            theme = currentTheme(),
            applyTopInset = false
        )
        tbar.setTitleMarginStart(resources.getDimensionPixelSize(R.dimen.flow_screen_padding))
        tbar.overflowIcon = AppCompatResources.getDrawable(context, R.drawable.more)
        tbar.background = ColorDrawable(Color.TRANSPARENT)
        tbar.elevation = 0f
        val window = (context as Activity).window
        window.statusBarColor = flowBackground
        WindowInsetsControllerCompat(window, rootView).isAppearanceLightStatusBars =
            ColorUtils.calculateLuminance(flowBackground) > 0.5
        listView.setBackgroundColor(flowBackground)
        addView(rootView, MATCH_PARENT, MATCH_PARENT)
        listAdapter.setListView(listView)
        updateHeader()
    }

    override fun onModelChange() {
        updateEmptyView()
        updateHeader()
    }

    private fun setupControllers() {
        header.setScrollController(
            object : ScrollableChart.ScrollController {
                override fun onDataOffsetChanged(newDataOffset: Int) {
                    listView.dataOffset = newDataOffset
                }
            }
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupControllers()
        listView.dataOffset = 0
        listAdapter.observable.addListener(this)
    }

    override fun onDetachedFromWindow() {
        listAdapter.observable.removeListener(this)
        super.onDetachedFromWindow()
    }

    /** Restores the home timeline to today whenever the screen becomes active. */
    fun showMostRecentDays() {
        listView.dataOffset = 0
    }

    fun alignSelectionControls(selected: Boolean) {
        val actionsParams = selectionActions.layoutParams as CoordinatorLayout.LayoutParams
        actionsParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        selectionActions.translationX = 0f
        selectionActions.layoutParams = actionsParams
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val count = if (resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE) 7 else HOME_CHECKMARK_COUNT
        header.buttonCount = count
        header.setMaxDataOffset(MAX_CHECKMARK_COUNT - count)
        listView.checkmarkCount = count
        super.onSizeChanged(w, h, oldw, oldh)
    }

    fun dismissTransientUi(): Boolean {
        if (!listAdapter.isSelectionEmpty) {
            listAdapter.clearSelection()
            selectionActions.hide()
            reorderPrompt.visibility = GONE
            return true
        }
        if (homeSearchBar.visibility == VISIBLE) {
            homeSearchBar.close()
            return true
        }
        return false
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP && !listAdapter.isSelectionEmpty) {
            val bounds = android.graphics.Rect()
            fun contains(view: android.view.View): Boolean =
                view.visibility == VISIBLE && view.getGlobalVisibleRect(bounds) &&
                    bounds.contains(event.rawX.toInt(), event.rawY.toInt())
            val onCard = (0 until listView.childCount).any { contains(listView.getChildAt(it)) }
            if (!onCard && !contains(selectionActions)) dismissTransientUi()
        }
        return super.dispatchTouchEvent(event)
    }

    fun openSearch(initialQuery: String, onQueryChanged: (String) -> Unit) {
        homeSearchBar.open(initialQuery, onQueryChanged)
    }

    private fun updateHeader() {
        largeHeader.updateSummary(
            listAdapter.getCompletionSummary(),
            listAdapter.getPerfectStreakSummary()
        )
    }

    private fun updateEmptyView() {
        if (listAdapter.itemCount == 0) {
            if (listAdapter.hasNoHabit()) {
                llEmpty.showEmpty()
            } else {
                llEmpty.showDone()
            }
        } else {
            llEmpty.hide()
        }
    }
}
