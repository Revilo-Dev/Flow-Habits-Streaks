package org.isoron.uhabits.core.ui.screens.habits.show.views

import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.preferences.MemoryStorage
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.callbacks.CheckMarkDialogCallback
import org.isoron.uhabits.core.ui.callbacks.NumberPickerCallback
import org.isoron.uhabits.core.ui.views.OnDateClickedListener
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HistoryCardPresenterTest : BaseUnitTest() {
    private class Screen : HistoryCardPresenter.Screen {
        var note: String? = null
        var noteDate: LocalDate? = null
        var edit: (() -> Unit)? = null
        var checkmarkEditors = 0
        var numberEditors = 0

        override fun showNote(date: LocalDate, notes: String, onEdit: () -> Unit) {
            noteDate = date
            note = notes
            edit = onEdit
        }

        override fun showHistoryEditorDialog(listener: OnDateClickedListener) = Unit
        override fun showFeedback() = Unit

        override fun showNumberPopup(value: Double, notes: String, callback: NumberPickerCallback) {
            numberEditors++
        }

        override fun showCheckmarkPopup(
            selectedValue: Int,
            notes: String,
            color: PaletteColor,
            callback: CheckMarkDialogCallback
        ) {
            checkmarkEditors++
        }
    }

    @Test
    fun tappingANotedDayShowsItsNoteWithoutTogglingTheAnswer() {
        val habit = fixtures.createEmptyHabit()
        val date = getToday().minus(3)
        habit.originalEntries.add(Entry(date, Entry.YES_MANUAL, "Morning walk"))
        habit.recompute()
        val screen = Screen()
        val preferences = Preferences(MemoryStorage()).apply { isShortToggleEnabled = true }
        val presenter = HistoryCardPresenter(commandRunner, habit, habitList, preferences, screen)

        presenter.onDateShortPress(date)

        assertEquals("Morning walk", screen.note)
        assertEquals(date, screen.noteDate)
        assertEquals(0, screen.checkmarkEditors)
        assertEquals(Entry.YES_MANUAL, habit.originalEntries.get(date).value)
        assertNotNull(screen.edit).invoke()
        assertEquals(1, screen.checkmarkEditors)
        assertEquals(Entry.YES_MANUAL, habit.originalEntries.get(date).value)
    }

    @Test
    fun numericalNotesOfferTheNumericalEditor() {
        val habit = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_LEAST)
        val date = getToday()
        habit.originalEntries.add(Entry(date, 2500, "A longer session"))
        habit.recompute()
        val screen = Screen()
        val presenter = HistoryCardPresenter(
            commandRunner,
            habit,
            habitList,
            Preferences(MemoryStorage()),
            screen
        )

        presenter.onDateShortPress(date)

        assertEquals("A longer session", screen.note)
        assertEquals(0, screen.numberEditors)
        assertNotNull(screen.edit).invoke()
        assertEquals(1, screen.numberEditors)
        assertEquals(2500, habit.originalEntries.get(date).value)
    }

    @Test
    fun longPressOnANotedDayEditsWithoutToggling() {
        val habit = fixtures.createEmptyHabit()
        val date = getToday()
        habit.originalEntries.add(Entry(date, Entry.NO, "Rest"))
        habit.recompute()
        val screen = Screen()
        val presenter = HistoryCardPresenter(
            commandRunner,
            habit,
            habitList,
            Preferences(MemoryStorage()),
            screen
        )

        presenter.onDateLongPress(date)

        assertEquals(1, screen.checkmarkEditors)
        assertEquals(Entry.NO, habit.originalEntries.get(date).value)
    }
}
