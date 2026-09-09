package org.isoron.uhabits.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.R
import org.isoron.uhabits.databinding.AboutBinding
import org.isoron.uhabits.databinding.CheckmarkPopupBinding
import org.isoron.uhabits.databinding.SelectHabitTypeBinding
import org.isoron.uhabits.utils.StyledResources
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FlowSurfaceTest {
    @Test
    fun inputAndCreationSurfacesRemainReadableInEveryTheme() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        instrumentation.runOnMainSync {
            for ((name, theme) in listOf(
                "light" to R.style.FlowTheme_Light,
                "dark" to R.style.FlowTheme_Dark,
                "black" to R.style.FlowTheme_Dark_PureBlack
            )) {
                val context = ContextThemeWrapper(target, theme)
                val inflater = LayoutInflater.from(context)
                val colors = StyledResources(context)
                val input = CheckmarkPopupBinding.inflate(inflater)
                input.numberButtons.visibility = View.VISIBLE
                input.numberFooter.visibility = View.VISIBLE
                input.value.setText("12")
                input.skipBtnNumber.visibility = View.GONE
                assertTrue(ColorUtils.calculateContrast(input.value.currentTextColor,
                    colors.getColor(R.attr.flowSurfaceSecondaryColor)) >= 4.5)
                org.junit.Assert.assertEquals(android.graphics.Color.WHITE, input.saveBtn.currentTextColor)
                val directory = target.getExternalFilesDir("oneui-review")!!
                directory.mkdirs()
                fun render(view: View, file: String, heightDp: Int? = null) {
                    val density = context.resources.displayMetrics.density
                    val width = (360 * density).toInt()
                    val height = ((heightDp ?: 800) * density).toInt()
                    view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, if (heightDp == null) View.MeasureSpec.AT_MOST else View.MeasureSpec.EXACTLY))
                    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                    val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    File(directory, "$name-$file.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
                render(input.root, "input")
                val creation = SelectHabitTypeBinding.inflate(inflater)
                render(creation.root, "create", 700)
                assertTrue("Cancel label must be laid out", creation.buttonCancel.layout.lineCount > 0)
                assertTrue("Cancel label must fit vertically",
                    creation.buttonCancel.height - creation.buttonCancel.compoundPaddingTop -
                        creation.buttonCancel.compoundPaddingBottom >= creation.buttonCancel.layout.height)
                assertTrue("Cancel pill must stay within its card",
                    creation.buttonCancel.right <= (creation.buttonCancel.parent as View).width -
                        (creation.buttonCancel.parent as View).paddingRight)
                val about = AboutBinding.inflate(inflater)
                about.collapsingToolbar.title = context.getString(R.string.about)
                render(about.root, "about", 800)
            }
        }
    }
}
