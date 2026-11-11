package org.isoron.uhabits.activities.habits.list.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.TooltipCompat
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.requestFocusWithKeyboard
import org.isoron.uhabits.utils.sres

class FlowHomeSearchBar(context: Context) : LinearLayout(context) {
    private var onQueryChanged: ((String) -> Unit)? = null
    private var transition = 0
    private var animator: ValueAnimator? = null
    private var expansion = 0f
    private var triggerWidth = 0
    private val shape = Path()
    private val visibleArea = RectF()
    var onOpen: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null
    var isOpen = false
        private set
    val query get() = queryInput.text.toString()

    private val searchIcon = ImageView(context).apply {
        setImageResource(R.drawable.search)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val queryInput: EditText = EditText(context).apply {
        id = R.id.flowSearchInput
        setTextAppearance(R.style.TextAppearance_Flow_Body)
        hint = resources.getString(R.string.search)
        contentDescription = resources.getString(R.string.search)
        setHintTextColor(sres.getColor(R.attr.flowTextTertiaryColor))
        isSingleLine = true
        background = null
        minimumHeight = resources.getDimensionPixelSize(R.dimen.flow_min_touch_target)
        layoutParams = LayoutParams(0, WRAP_CONTENT, 1f)
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                clearButton.visibility = if (s.isNullOrEmpty()) INVISIBLE else VISIBLE
                onQueryChanged?.invoke(s?.toString().orEmpty())
            }
        })
    }

    private val clearButton: ImageButton = action(R.drawable.flow_ic_close, R.string.flow_clear_search) {
        queryInput.text.clear()
        queryInput.requestFocus()
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.flow_toolbar_height)
        val padding = dim(R.dimen.flow_medium_spacing).toInt()
        setPadding(padding, 0, padding, 0)
        background = GradientDrawable().apply {
            cornerRadius = dim(R.dimen.flow_toolbar_pill_radius)
            setColor(sres.getColor(R.attr.flowSurfaceSecondaryColor))
        }
        val iconSize = resources.getDimensionPixelSize(R.dimen.flow_icon_size)
        addView(searchIcon, LayoutParams(iconSize, iconSize))
        addView(queryInput)
        addView(clearButton)
        addView(action(R.drawable.flow_ic_back, R.string.flow_close_search) { close() })
        visibility = GONE

        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            val params = view.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            params?.bottomMargin = dim(R.dimen.flow_fab_inline_margin).toInt() + maxOf(bars.bottom, keyboard.bottom)
            if (params != null) view.layoutParams = params
            insets
        }
    }

    fun open(trigger: View, initialQuery: String, onQueryChanged: (String) -> Unit, requestKeyboard: Boolean) {
        if (isOpen) return
        transition++
        val opening = transition
        animator?.cancel()
        triggerWidth = trigger.width.takeIf { it > 0 } ?: resources.getDimensionPixelSize(R.dimen.flow_fab_size)
        this.onQueryChanged = null
        queryInput.setText(initialQuery)
        queryInput.setSelection(queryInput.text.length)
        this.onQueryChanged = onQueryChanged
        isOpen = true
        updateExpansion(if (requestKeyboard) 0f else 1f)
        visibility = VISIBLE
        onOpen?.invoke()
        if (requestKeyboard) {
            doOnLayout {
                if (transition == opening) animateExpansion(1f, 240) { queryInput.requestFocusWithKeyboard() }
            }
        }
    }

    fun hideKeyboard() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(queryInput.windowToken, 0)
        queryInput.clearFocus()
    }

    fun close() {
        if (!isOpen) return
        transition++
        animator?.cancel()
        isOpen = false
        onQueryChanged?.invoke("")
        onQueryChanged = null
        hideKeyboard()
        animateExpansion(0f, 180) {
            visibility = GONE
            onDismiss?.invoke()
        }
    }

    override fun draw(canvas: Canvas) {
        if (expansion >= 1f) {
            super.draw(canvas)
            return
        }
        val startWidth = triggerWidth.coerceAtMost(width)
        val visibleWidth = startWidth + (width - startWidth) * expansion
        val left = if (layoutDirection == LAYOUT_DIRECTION_RTL) 0f else width - visibleWidth
        visibleArea.set(left, 0f, left + visibleWidth, height.toFloat())
        shape.rewind()
        val radius = minOf(height / 2f, visibleWidth / 2f)
        shape.addRoundRect(visibleArea, radius, radius, Path.Direction.CW)
        val save = canvas.save()
        canvas.clipPath(shape)
        super.draw(canvas)
        canvas.restoreToCount(save)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateExpansion(expansion)
    }

    private fun animateExpansion(target: Float, durationMs: Long, onFinished: () -> Unit) {
        val currentTransition = transition
        animator = ValueAnimator.ofFloat(expansion, target).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { updateExpansion(it.animatedValue as Float) }
            doOnEnd { if (transition == currentTransition) onFinished() }
            start()
        }
    }

    private fun updateExpansion(value: Float) {
        expansion = value
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val triggerCenter = if (rtl) triggerWidth / 2f else width - triggerWidth / 2f
        val iconCenter = (searchIcon.left + searchIcon.right) / 2f
        searchIcon.translationX = (triggerCenter - iconCenter) * (1f - expansion)
        val contentAlpha = ((expansion - 0.35f) / 0.65f).coerceIn(0f, 1f)
        for (index in 1 until childCount) getChildAt(index).alpha = contentAlpha
        invalidate()
    }

    private fun action(icon: Int, label: Int, callback: () -> Unit) = ImageButton(context).apply {
        val size = resources.getDimensionPixelSize(R.dimen.flow_min_touch_target)
        layoutParams = LayoutParams(size, size)
        setImageResource(icon)
        imageTintList = android.content.res.ColorStateList.valueOf(sres.getColor(R.attr.flowTextPrimaryColor))
        background = null
        contentDescription = resources.getString(label)
        TooltipCompat.setTooltipText(this, contentDescription)
        setOnClickListener { callback() }
    }
}
