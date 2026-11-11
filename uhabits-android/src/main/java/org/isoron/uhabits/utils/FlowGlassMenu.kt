package org.isoron.uhabits.utils

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.Gravity
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuView
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import org.isoron.uhabits.R
import java.util.WeakHashMap

@SuppressLint("RestrictedApi", "ClickableViewAccessibility")
internal class FlowGlassMenu(private val toolbar: Toolbar, private val pillInset: Int) {
    private var popup: PopupWindow? = null
    private val boundItems = WeakHashMap<View, MenuItem?>()

    init {
        toolbar.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) {
                popup?.dismiss()
            }
        })
    }

    fun bind(actions: ActionMenuView) {
        for (child in actions.children) {
            val item = (child as? MenuView.ItemView)?.itemData
            val overflow = (child.layoutParams as? ActionMenuView.LayoutParams)?.isOverflowButton == true
            if (!overflow && item?.hasSubMenu() != true) continue
            if (boundItems.containsKey(child) && boundItems[child] === item) continue
            boundItems[child] = item
            val open = {
                val items = if (overflow) {
                    (toolbar.menu as? MenuBuilder)?.nonActionItems.orEmpty()
                } else {
                    val subMenu = item?.subMenu
                    if (subMenu == null) emptyList() else (0 until subMenu.size()).map { subMenu.getItem(it) }
                }
                show(child, items)
            }
            child.setOnClickListener { open() }
            // Keep the framework forwarding listener from opening a second popup
            child.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> view.isPressed = true
                    MotionEvent.ACTION_CANCEL -> view.isPressed = false
                    MotionEvent.ACTION_UP -> {
                        view.isPressed = false
                        if (event.x in 0f..view.width.toFloat() && event.y in 0f..view.height.toFloat()) view.performClick()
                    }
                }
                true
            }
        }
    }

    private fun show(anchor: View, items: List<MenuItem>) {
        popup?.dismiss()
        val visibleItems = items.filter { it.isVisible }
        if (visibleItems.isEmpty()) return
        val context = toolbar.context
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val surface = toolbar.sres.getColor(R.attr.flowSurfaceSecondaryColor)
        val textColor = toolbar.sres.getColor(
            if (ColorUtils.calculateLuminance(surface) > 0.5) {
                R.attr.flowTextSecondaryColor
            } else {
                R.attr.flowTextPrimaryColor
            }
        )
        val rows = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = false
            addView(rows)
        }
        val frame = Rect().also(anchor::getWindowVisibleDisplayFrame)
        val location = IntArray(2).also(anchor::getLocationOnScreen)
        val pill = IntArray(2).also((anchor.parent as View)::getLocationOnScreen)
        val pillTop = (pill[1] + pillInset).coerceAtLeast(frame.top)
        val width = dp(196).coerceAtMost((frame.width() - dp(16)).coerceAtLeast(1))
        val window = PopupWindow(scroll, width, WRAP_CONTENT, true).apply {
            setBackgroundDrawable(toolbar.flowPopupSurface(dp(24).toFloat()))
            isOutsideTouchable = true
            elevation = toolbar.dim(R.dimen.flow_popup_elevation)
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            animationStyle = R.style.Animation_Flow_Menu
        }
        visibleItems.forEach { item ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(52)
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setBackgroundResource(R.drawable.flow_touch_feedback)
                isEnabled = item.isEnabled
                alpha = if (item.isEnabled) 1f else 0.4f
            }
            item.icon?.constantState?.newDrawable()?.mutate()?.let { icon ->
                icon.setTint(toolbar.sres.getColor(R.attr.flowTextSecondaryColor))
                row.addView(
                    ImageView(context).apply { setImageDrawable(icon) },
                    LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                        marginEnd = dp(12)
                    }
                )
            }
            row.addView(
                TextView(context).apply {
                    setTextAppearance(R.style.TextAppearance_Flow_Body)
                    setTextColor(textColor)
                    text = item.title
                },
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            )
            if (item.isCheckable || item.hasSubMenu()) {
                row.addView(
                    ImageView(context).apply {
                        if (item.hasSubMenu()) {
                            setImageResource(R.drawable.flow_ic_expand_more)
                            rotation = -90f
                        } else if (item.isChecked) {
                            setImageResource(R.drawable.flow_ic_entry_check)
                        }
                        imageTintList = android.content.res.ColorStateList.valueOf(
                            toolbar.sres.getColor(R.attr.flowTextSecondaryColor)
                        )
                        scaleType = ImageView.ScaleType.CENTER
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    },
                    LinearLayout.LayoutParams(dp(32), dp(32))
                )
            }
            row.contentDescription = item.title
            row.isSelected = item.isCheckable && item.isChecked
            row.setOnClickListener {
                if (item.hasSubMenu()) {
                    val subMenu = item.subMenu!!
                    show(anchor, (0 until subMenu.size()).map { subMenu.getItem(it) })
                } else {
                    window.dismiss()
                    toolbar.menu.performIdentifierAction(item.itemId, 0)
                }
            }
            rows.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        scroll.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((frame.bottom - pillTop).coerceAtLeast(1), MeasureSpec.AT_MOST)
        )
        window.height = scroll.measuredHeight
        popup = window
        window.showAsDropDown(anchor, 0, pillTop - location[1] - anchor.height, Gravity.END)
    }
}
