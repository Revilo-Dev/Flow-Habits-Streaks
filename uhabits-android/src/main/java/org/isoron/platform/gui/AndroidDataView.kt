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

package org.isoron.platform.gui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Scroller
import kotlin.math.abs
import kotlin.math.max

/**
 * An AndroidView that implements scrolling.
 */
open class AndroidDataView(
    context: Context,
    attrs: AttributeSet? = null
) : AndroidView<DataView>(context, attrs),
    GestureDetector.OnGestureListener,
    ValueAnimator.AnimatorUpdateListener {

    private var restoredOffset: Int? = null
    private val detector = GestureDetector(context, this)
    private val scroller = Scroller(context, null, true)
    private val scrollAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        addUpdateListener(this@AndroidDataView)
    }

    override fun onTouchEvent(event: MotionEvent) = detector.onTouchEvent(event)
    var lastTouchX: Float = 0f
        private set
    var lastTouchY: Float = 0f
        private set

    protected fun setInteractionPoint(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        restoredOffset?.let { offset -> view?.dataOffset = offset }
        super.onDraw(canvas)
        if (view != null && restoredOffset != null) {
            alignScrollerWithView()
            restoredOffset = null
        }
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable("parent", super.onSaveInstanceState())
        putInt("dataOffset", view?.dataOffset ?: restoredOffset ?: 0)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            restoredOffset = state.getInt("dataOffset")
            super.onRestoreInstanceState(state.getParcelable("parent"))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private fun alignScrollerWithView() {
        val chart = view ?: return
        val x = (chart.dataOffset * chart.dataColumnWidth * resources.displayMetrics.density).toInt()
        scroller.startScroll(x, 0, 0, 0, 0)
        scroller.forceFinished(true)
    }

    override fun onDown(e: MotionEvent): Boolean {
        scrollAnimator.cancel()
        alignScrollerWithView()
        return true
    }
    override fun onShowPress(e: MotionEvent) = Unit

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return handleClick(e, true)
    }

    override fun onLongPress(e: MotionEvent) {
        handleClick(e)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        dx: Float,
        dy: Float
    ): Boolean {
        if (abs(dx) > abs(dy)) {
            val parent = parent
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        scroller.startScroll(
            scroller.currX,
            scroller.currY,
            -dx.toInt(),
            dy.toInt(),
            0
        )
        scroller.computeScrollOffset()
        updateDataOffset()
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        scroller.fling(
            scroller.currX,
            scroller.currY,
            velocityX.toInt() / 2,
            0,
            0,
            Integer.MAX_VALUE,
            0,
            0
        )
        invalidate()
        scrollAnimator.duration = scroller.duration.toLong()
        scrollAnimator.start()
        return false
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        if (!scroller.isFinished) {
            scroller.computeScrollOffset()
            updateDataOffset()
        } else {
            scrollAnimator.cancel()
        }
    }

    fun resetDataOffset() {
        scroller.finalX = 0
        scroller.computeScrollOffset()
        updateDataOffset()
    }

    private fun updateDataOffset() {
        view?.let { v ->
            var newDataOffset: Int =
                scroller.currX / (v.dataColumnWidth * canvas.innerDensity).toInt()
            newDataOffset = max(0, newDataOffset)
            if (newDataOffset != v.dataOffset) {
                v.dataOffset = newDataOffset
                postInvalidate()
            }
        }
    }

    private fun handleClick(e: MotionEvent, isSingleTap: Boolean = false): Boolean {
        val x: Float
        val y: Float
        try {
            x = e.getX(0)
            y = e.getY(0)
            lastTouchX = x
            lastTouchY = y
        } catch (ex: RuntimeException) {
            // Ignore taps whose pointer is no longer available
            return false
        }
        if (isSingleTap) {
            view?.onClick(x / canvas.innerDensity, y / canvas.innerDensity)
        } else {
            view?.onLongClick(x / canvas.innerDensity, y / canvas.innerDensity)
        }
        return true
    }
}
