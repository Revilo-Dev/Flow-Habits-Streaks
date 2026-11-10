package org.isoron.uhabits.activities.common.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.ViewCompat
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.databinding.CheckmarkPopupBinding
import org.isoron.uhabits.utils.FlowBlurSettings
import org.isoron.uhabits.utils.FlowHighlightDrawable
import org.isoron.uhabits.utils.StyledResources
import org.isoron.uhabits.utils.currentDialogFragment
import java.lang.ref.WeakReference
import java.util.function.Consumer

internal fun AppCompatDialogFragment.createEntryDialog(binding: CheckmarkPopupBinding): Dialog {
    binding.habitName.text = requireArguments().getString("habitName") ?: getString(R.string.app_name)
    ViewCompat.setAccessibilityHeading(binding.habitName, true)
    return Dialog(requireContext(), R.style.FlowEntryDialog).apply {
        setContentView(binding.root)
        setCanceledOnTouchOutside(true)
        configureFlowDialogBlur(requireActivity().findViewById(android.R.id.content))
    }
}

internal fun AppCompatDialogFragment.configureEntryWindow() {
    currentDialogFragment = WeakReference(this)
    val preferences = (requireActivity().application as HabitsApplication).component.preferences
    val centered = preferences.entryDialogPosition == "center"
    configureHabitDialogSize(centered)
}

internal fun AppCompatDialogFragment.configureHabitDialogSize(centered: Boolean) {
    val window = requireDialog().window ?: return
    val density = resources.displayMetrics.density
    val margin = (16 * density).toInt()
    val hostWidth = requireActivity().window.decorView.width.takeIf { it > 0 }
        ?: resources.displayMetrics.widthPixels
    val width = (hostWidth - margin * 2).coerceAtMost((360 * density).toInt()).coerceAtLeast(1)
    window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
    val gravity = if (centered) {
        Gravity.CENTER
    } else {
        Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
    }
    window.setGravity(gravity)
    window.attributes = window.attributes.apply {
        y = if (centered) 0 else margin
    }
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
}

internal fun Dialog.configureFlowDialogBlur(backdrop: View?) {
    val window = window ?: return
    val density = context.resources.displayMetrics.density
    val surface = StyledResources(context).getColor(R.attr.flowSurfaceSecondaryColor)
    val glass = FlowHighlightDrawable(context, context.resources.getDimension(R.dimen.flow_dialog_radius))
    glass.setColor(surface)
    window.setBackgroundDrawable(glass)
    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    window.setDimAmount(0.4f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var localBlur = false
        val listener = Consumer<Boolean> { crossWindowBlur ->
            val radius = (FlowBlurSettings.intensity(context) * density).toInt()
                .takeIf { FlowBlurSettings.isEnabled(context) } ?: 0
            val windowBlur = crossWindowBlur && radius > 0
            val useLocalBlur = !windowBlur && radius > 0 && backdrop?.isHardwareAccelerated == true
            if (windowBlur) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
            window.setBackgroundBlurRadius(if (windowBlur) radius else 0)
            window.attributes = window.attributes.apply {
                blurBehindRadius = if (windowBlur) radius else 0
            }
            // Use the toolkit local backdrop when the window compositor cannot blur
            if (useLocalBlur) {
                backdrop?.setRenderEffect(RenderEffect.createBlurEffect(radius.toFloat(), radius.toFloat(), Shader.TileMode.CLAMP))
            } else if (localBlur) {
                backdrop?.setRenderEffect(null)
            }
            localBlur = useLocalBlur
            val blurred = windowBlur || useLocalBlur
            glass.setColor(if (blurred) FlowBlurSettings.tint(context, surface) else surface)
            window.setDimAmount(if (blurred) 0.18f else 0.4f)
        }
        window.decorView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                manager.addCrossWindowBlurEnabledListener(listener)
            }
            override fun onViewDetachedFromWindow(view: View) {
                manager.removeCrossWindowBlurEnabledListener(listener)
                if (localBlur) backdrop?.setRenderEffect(null)
                localBlur = false
            }
        })
        FlowBlurSettings.observe(window.decorView) { listener.accept(manager.isCrossWindowBlurEnabled) }
    }
}

internal fun AppCompatDialogFragment.saveRestoredEntry(value: Int, notes: String): Boolean {
    val args = requireArguments()
    if (!args.containsKey("habitId") || !args.containsKey("timestamp")) return false
    val component = (requireActivity().application as HabitsApplication).component
    val habit = component.habitList.getById(args.getLong("habitId")) ?: return false
    component.commandRunner.run(
        CreateRepetitionCommand(
            component.habitList,
            habit,
            LocalDate.fromUnixTime(args.getLong("timestamp")),
            value,
            notes
        )
    )
    return true
}
