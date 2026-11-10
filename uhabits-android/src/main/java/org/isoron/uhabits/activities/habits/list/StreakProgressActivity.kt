package org.isoron.uhabits.activities.habits.list

import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.activities.FlowActivity
import org.isoron.uhabits.activities.habits.show.views.OverviewCardView
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.ui.screens.habits.list.StreakProgress
import org.isoron.uhabits.core.ui.screens.habits.show.views.BarCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.CurrentStreakCardState
import org.isoron.uhabits.core.ui.screens.habits.show.views.HistoryCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.StreakCardState
import org.isoron.uhabits.databinding.StreakProgressBinding
import org.isoron.uhabits.intents.IntentFactory
import org.isoron.uhabits.utils.FlowHighlightDrawable
import org.isoron.uhabits.utils.addFlowScrollFades
import org.isoron.uhabits.utils.applyFlowContentInsets
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.bindFlowHeader
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.sres

class StreakProgressActivity : FlowActivity(), CommandRunner.Listener {
    private val component get() = (application as HabitsApplication).component
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null
    private lateinit var binding: StreakProgressBinding
    private lateinit var themeSwitcher: AndroidThemeSwitcher
    private var state: StreakProgress? = null
    private var cardHabit: Habit? = null
    private var restoredScroll = 0
    private var historyOffset = 0
    private var bucket = 1
    private var previousLimit = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeSwitcher = AndroidThemeSwitcher(this, component.preferences).apply { apply() }
        restoredScroll = savedInstanceState?.getInt("scroll") ?: 0
        historyOffset = savedInstanceState?.getInt("historyOffset") ?: 0
        bucket = (savedInstanceState?.getInt("bucket") ?: 1).coerceIn(0, 3)
        previousLimit = savedInstanceState?.getInt("previousLimit") ?: 20
        binding = StreakProgressBinding.inflate(layoutInflater)
        binding.collapsingToolbar.title = getString(R.string.flow_streak_progress)
        binding.toolbar.setNavigationIcon(R.drawable.flow_ic_back)
        binding.toolbar.navigationContentDescription = getString(androidx.appcompat.R.string.abc_action_bar_up_description)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.root.applyRootViewInsets()
        binding.appBar.applyToolbarInsets()
        binding.streakScroll.applyFlowContentInsets(includeBottom = true)
        binding.appBar.bindFlowHeader(binding.toolbar)
        binding.root.addFlowScrollFades(binding.streakScroll)
        val background = binding.root.sres.getColor(R.attr.flowBackgroundColor)
        val light = ColorUtils.calculateLuminance(background) > 0.5
        binding.root.background = LayerDrawable(
            arrayOf(
                ColorDrawable(background),
                backdropGlow(getColor(R.color.flow_streak_end), if (light) 84 else 128, 0.78f, 0.26f),
                backdropGlow(getColor(R.color.flow_streak_aqua), if (light) 66 else 92, 0.16f, 0.64f)
            )
        )
        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = light
        val elevation = if (light) resources.getDimension(R.dimen.flow_card_elevation) else 0f
        listOf(
            binding.overviewCard,
            binding.currentStreakCard,
            binding.historyCard,
            binding.bestStreakCard,
            binding.barCard,
            binding.previousStreakCard
        ).forEach {
            styleStreakCard(it, elevation)
        }
        binding.historyCard.setReadOnly(R.string.flow_streak_calendar)
        binding.previousStreakCard.setTitle(R.string.flow_streak_previous)
        binding.barCard.setOnBucketSelected { position ->
            if (cardHabit != null && bucket != position) {
                bucket = position
                renderBar()
            }
        }
        binding.moreStreaks.setOnClickListener {
            previousLimit += 20
            renderPrevious()
        }
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        component.commandRunner.addListener(this)
        refresh()
    }

    override fun onPause() {
        component.commandRunner.removeListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("historyOffset", binding.historyCard.dataOffset)
        outState.putInt("scroll", binding.streakScroll.scrollY)
        outState.putInt("bucket", bucket)
        outState.putInt("previousLimit", previousLimit)
        super.onSaveInstanceState(outState)
    }

    override fun onCommandFinished(command: Command) = refresh()

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val (progress, habit) = withContext(Dispatchers.Default) {
                val progress = StreakProgress.build(
                    component.habitList,
                    component.preferences.firstWeekday,
                    component.preferences.skipsCountTowardsPerfectStreak
                )
                progress to progress.toCardHabit()
            }
            state = progress
            cardHabit = habit
            render(progress, habit)
        }
    }

    private fun render(progress: StreakProgress, habit: Habit) {
        val scroll = maxOf(restoredScroll, binding.streakScroll.scrollY)
        val offset = maxOf(historyOffset, binding.historyCard.dataOffset)
        val theme = themeSwitcher.currentTheme
        val color = theme.color(habit.color).toInt()
        binding.overviewCard.setStreakSummary(
            progress.current?.length ?: 0,
            progress.longest,
            progress.perfectDays.size,
            color,
            getString(R.string.flow_streak_totals)
        )
        binding.currentStreakCard.setPerfectState(
            CurrentStreakCardState(
                progress.current?.length ?: 0,
                (6 downTo 0).map { progress.today.minus(it) in progress.perfectDays },
                component.preferences.firstWeekday
            )
        )
        binding.historyCard.setState(HistoryCardPresenter.buildState(habit, component.preferences.firstWeekday, theme))
        binding.historyCard.restoreDataOffset(offset)
        val best = progress.streaks.sortedByDescending { it.length }.take(10).sortedByDescending { it.end }
        binding.bestStreakCard.setState(StreakCardState(habit.color, best, theme))
        renderBar()
        renderPrevious()
        binding.habitCards.removeAllViews()
        progress.habits.forEach { item ->
            val card = layoutInflater.inflate(R.layout.streak_habit_summary, binding.habitCards, false) as OverviewCardView
            card.setStreakSummary(
                item.current,
                item.longest,
                item.streaks.sumOf { it.length },
                theme.color(item.habit.color).toInt(),
                item.habit.name
            )
            styleStreakCard(card, binding.overviewCard.elevation)
            card.setOnClickListener { startActivity(IntentFactory().startShowHabitActivity(this, item.habit)) }
            binding.habitCards.addView(card)
        }
        binding.streakScroll.post { binding.streakScroll.scrollTo(0, scroll) }
        restoredScroll = 0
        historyOffset = 0
    }

    private fun styleStreakCard(card: View, elevation: Float) {
        card.foreground = FlowHighlightDrawable(this, card.dim(R.dimen.flow_card_radius))
        card.clipToOutline = true
        ViewCompat.setElevation(card, elevation)
    }

    private fun backdropGlow(color: Int, opacity: Int, x: Float, y: Float) = object : GradientDrawable() {
        init {
            gradientType = RADIAL_GRADIENT
            setColors(intArrayOf(ColorUtils.setAlphaComponent(color, opacity), ColorUtils.setAlphaComponent(color, 0)))
            setGradientCenter(x, y)
        }

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            setGradientRadius(maxOf(bounds.width(), bounds.height()) * 0.64f)
        }
    }

    private fun renderBar() {
        val habit = cardHabit ?: return
        binding.barCard.setState(
            BarCardPresenter.buildState(
                habit,
                component.preferences.firstWeekdayInt,
                numericalSpinnerPosition = 0,
                boolSpinnerPosition = bucket,
                theme = themeSwitcher.currentTheme
            )
        )
    }

    private fun renderPrevious() {
        val progress = state ?: return
        val habit = cardHabit ?: return
        binding.previousStreakCard.setState(
            StreakCardState(habit.color, progress.previous.take(previousLimit), themeSwitcher.currentTheme)
        )
        binding.moreStreaks.visibility = if (previousLimit < progress.previous.size) View.VISIBLE else View.GONE
    }
}
