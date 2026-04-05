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
package org.isoron.uhabits.activities.common.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.countWeekdayOccurrencesInMonth
import org.isoron.platform.time.getToday
import org.isoron.platform.time.getWeekdaySequence
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.ColorUtils.mixColors
import org.isoron.uhabits.utils.StyledResources
import java.util.Locale
import java.util.Random
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FrequencyChart : ScrollableChart {
    private var pGrid: Paint? = null
    private var em = 0f
    private var dateFormatter: JavaLocalDateFormatter? = null
    private var pText: Paint? = null
    private var pGraph: Paint? = null
    private var rect: RectF? = null
    private var prevRect: RectF? = null
    private var baseSize = 0
    private var internalPaddingTop = 0
    private var columnWidth = 0f
    private var columnHeight = 0
    private var nColumns = 0
    private var textColor = 0
    private var gridColor = 0
    private lateinit var colors: IntArray
    private var primaryColor = 0
    private var isBackgroundTransparent = false
    private lateinit var frequency: HashMap<LocalDate, Array<Int>>
    private var maxFreq = 0
    private var firstWeekday: DayOfWeek = DayOfWeek.SUNDAY
    private var isNumerical: Boolean = false

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        frequency = HashMap()
        init()
    }

    fun setColor(color: Int) {
        primaryColor = color
        initColors()
        postInvalidate()
    }

    fun setIsNumerical(type: Boolean) {
        isNumerical = type
        postInvalidate()
    }

    fun setFrequency(frequency: java.util.HashMap<LocalDate, Array<Int>>) {
        this.frequency = frequency
        maxFreq = getMaxFreq(frequency)
        postInvalidate()
    }

    fun setFirstWeekday(firstWeekday: DayOfWeek) {
        this.firstWeekday = firstWeekday
        postInvalidate()
    }

    private fun getMaxFreq(frequency: HashMap<LocalDate, Array<Int>>): Int {
        var maxValue = 1
        for (values in frequency.values) for (value in values) maxValue = max(
            value,
            maxValue
        )
        return maxValue
    }

    fun setIsBackgroundTransparent(isBackgroundTransparent: Boolean) {
        this.isBackgroundTransparent = isBackgroundTransparent
        initColors()
    }

    private fun initPaints() {
        pText = Paint()
        pText!!.isAntiAlias = true
        pGraph = Paint()
        pGraph!!.textAlign = Paint.Align.CENTER
        pGraph!!.isAntiAlias = true
        pGrid = Paint()
        pGrid!!.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rect!![0f, 0f, nColumns * columnWidth] = columnHeight.toFloat()
        rect!!.offset(0f, internalPaddingTop.toFloat())
        drawGrid(canvas, rect)
        pText!!.textAlign = Paint.Align.CENTER
        pText!!.color = textColor
        pGraph!!.color = primaryColor
        prevRect!!.setEmpty()
        val today = getToday()
        var currentDate = LocalDate(today.year, today.month, 1)
        currentDate = stepMonth(currentDate, -nColumns + 2 - dataOffset)
        for (i in 0 until nColumns - 1) {
            rect!![0f, 0f, columnWidth] = columnHeight.toFloat()
            rect!!.offset(i * columnWidth, 0f)
            drawColumn(canvas, rect, currentDate)
            currentDate = stepMonth(currentDate, 1)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int
    ) {
        var height = height
        if (height < 9) height = 200
        baseSize = height / 8
        setScrollerBucketSize(baseSize)
        pText!!.textSize = baseSize * 0.4f
        pGraph!!.textSize = baseSize * 0.4f
        pGraph!!.strokeWidth = baseSize * 0.1f
        pGrid!!.strokeWidth = baseSize * 0.05f
        em = pText!!.fontSpacing
        columnWidth = baseSize.toFloat()
        columnWidth = max(columnWidth, maxMonthWidth * 1.2f)
        columnHeight = 8 * baseSize
        nColumns = (width / columnWidth).toInt()
        internalPaddingTop = 0
    }

    private fun drawColumn(canvas: Canvas, rect: RectF?, date: LocalDate) {
        val values = frequency[date]
        val weekDaysInMonth = countWeekdayOccurrencesInMonth(date)
        val rowHeight = rect!!.height() / 8.0f
        prevRect!!.set(rect)
        val localeWeekdayList = getWeekdaySequence(firstWeekday)
        for (j in localeWeekdayList.indices) {
            rect[0f, 0f, baseSize.toFloat()] = baseSize.toFloat()
            rect.offset(prevRect!!.left, prevRect!!.top + baseSize * j)
            val i = (localeWeekdayList[j].daysSinceSunday + 1) % 7
            if (values != null) {
                drawMarker(canvas, rect, values[i], weekDaysInMonth[i])
            }
            rect.offset(0f, rowHeight)
        }
        drawFooter(canvas, rect, date)
    }

    private fun drawFooter(canvas: Canvas, rect: RectF?, date: LocalDate) {
        val df = dateFormatter ?: return
        canvas.drawText(
            df.shortMonthName(date),
            rect!!.centerX(),
            rect.centerY() - 0.1f * em,
            pText!!
        )
        if (date.month == 2) {
            canvas.drawText(
                date.year.toString(),
                rect.centerX(),
                rect.centerY() + 0.9f * em,
                pText!!
            )
        }
    }

    private fun drawGrid(canvas: Canvas, rGrid: RectF?) {
        val nRows = 7
        val rowHeight = rGrid!!.height() / (nRows + 1)
        pText!!.textAlign = Paint.Align.LEFT
        pText!!.color = textColor
        pGrid!!.color = gridColor
        val df = dateFormatter ?: return
        for (day in df.shortWeekdayNames(firstWeekday)) {
            canvas.drawText(
                day,
                rGrid.right - columnWidth,
                rGrid.top + rowHeight / 2 + 0.25f * em,
                pText!!
            )
            pGrid!!.strokeWidth = 1f
            canvas.drawLine(
                rGrid.left,
                rGrid.top,
                rGrid.right,
                rGrid.top,
                pGrid!!
            )
            rGrid.offset(0f, rowHeight)
        }
        canvas.drawLine(rGrid.left, rGrid.top, rGrid.right, rGrid.top, pGrid!!)
    }

    private fun drawMarker(canvas: Canvas, rect: RectF?, value: Int?, weekdayFrequency: Int) {
        // value can be negative when the entry is skipped
        val valueCopy = value?.let { max(0, it) }

        val padding = rect!!.height() * 0.2f
        // maximal allowed mark radius
        val maxRadius = (rect.height() - 2 * padding) / 2.0f
        // the real mark radius is scaled down by a factor depending on the maximal frequency
        val scalingFactor = if (isNumerical) maxFreq else weekdayFrequency
        val scale = 1.0f / scalingFactor * valueCopy!!
        val radius = maxRadius * scale
        val colorIndex = min((colors.size - 1), ((colors.size - 1) * scale).roundToInt())
        pGraph!!.color = colors[colorIndex]
        canvas.drawCircle(rect.centerX(), rect.centerY(), radius, pGraph!!)
    }

    private val maxMonthWidth: Float
        get() {
            val df = dateFormatter ?: return 0f
            var maxMonthWidth = 0f
            for (i in 1..12) {
                val date = LocalDate(2020, i, 1)
                val monthWidth = pText!!.measureText(df.shortMonthName(date))
                maxMonthWidth = max(maxMonthWidth, monthWidth)
            }
            return maxMonthWidth
        }

    private fun init() {
        initPaints()
        initColors()
        initDateFormats()
        initRects()
    }

    private fun initColors() {
        val res = StyledResources(context)
        textColor = res.getColor(R.attr.contrast60)
        gridColor = res.getColor(R.attr.contrast20)
        colors = IntArray(4)
        colors[0] = gridColor
        colors[3] = primaryColor
        colors[1] = mixColors(colors[0], colors[3], 0.66f)
        colors[2] = mixColors(colors[0], colors[3], 0.33f)
    }

    private fun initDateFormats() {
        dateFormatter = JavaLocalDateFormatter(Locale.getDefault())
    }

    private fun initRects() {
        rect = RectF()
        prevRect = RectF()
    }

    private fun stepMonth(date: LocalDate, months: Int): LocalDate {
        var y = date.year
        var m = date.month + months
        while (m < 1) { m += 12; y -= 1 }
        while (m > 12) { m -= 12; y += 1 }
        return LocalDate(y, m, 1)
    }

    fun populateWithRandomData() {
        val today = getToday()
        var date = LocalDate(today.year, today.month, 1)
        val rand = Random()
        frequency.clear()
        for (i in 0..39) {
            val values = IntArray(7) { rand.nextInt(5) }.toTypedArray()
            frequency[date] = values
            date = stepMonth(date, -1)
        }
        maxFreq = getMaxFreq(frequency)
    }
}
