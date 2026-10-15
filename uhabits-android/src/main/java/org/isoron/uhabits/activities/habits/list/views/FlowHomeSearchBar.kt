package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.requestFocusWithKeyboard
import org.isoron.uhabits.utils.sres

class FlowHomeSearchBar(context: Context) : LinearLayout(context) {
    private var onQueryChanged: ((String) -> Unit)? = null
    private var transition = 0
    var onOpen: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null
    var isOpen = false
        private set
    val query get() = queryInput.text.toString()

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
        addView(
            ImageView(context).apply {
                setImageResource(R.drawable.search)
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LayoutParams(
                resources.getDimensionPixelSize(R.dimen.flow_icon_size),
                resources.getDimensionPixelSize(R.dimen.flow_icon_size)
            )
        )
        addView(queryInput)
        addView(clearButton)
        addView(action(R.drawable.flow_ic_back, R.string.flow_close_search) { close() })
        visibility = GONE

        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            val params = view.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            params?.bottomMargin = dim(R.dimen.flow_large_spacing).toInt() + maxOf(bars.bottom, keyboard.bottom)
            if (params != null) view.layoutParams = params
            insets
        }
    }

    fun open(initialQuery: String, onQueryChanged: (String) -> Unit, requestKeyboard: Boolean = true) {
        transition++
        animate().cancel()
        this.onQueryChanged = null
        queryInput.setText(initialQuery)
        queryInput.setSelection(queryInput.text.length)
        this.onQueryChanged = onQueryChanged
        isOpen = true
        visibility = VISIBLE
        alpha = 1f
        translationY = 0f
        onOpen?.invoke()
        if (requestKeyboard) queryInput.requestFocusWithKeyboard()
    }

    fun hideKeyboard() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(queryInput.windowToken, 0)
        queryInput.clearFocus()
    }

    fun close() {
        transition++
        animate().cancel()
        isOpen = false
        onQueryChanged?.invoke("")
        onQueryChanged = null
        hideKeyboard()
        visibility = GONE
        alpha = 1f
        onDismiss?.invoke()
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
