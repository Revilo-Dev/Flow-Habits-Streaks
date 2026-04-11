package org.isoron.platform.gui

import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.PI
import kotlin.math.roundToInt

class JsCanvas(
    private val canvas: HTMLCanvasElement,
    private val pixelScale: Double = 2.0
) : Canvas {
    private val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    private var fontSize = 12.0
    private var font = Font.REGULAR
    private var textAlign = TextAlign.CENTER

    init {
        updateFont()
    }

    private fun toPixel(x: Double): Double = pixelScale * x
    private fun toDp(x: Double): Double = x / pixelScale

    override fun setColor(color: Color) {
        val r = (color.red * 255).roundToInt()
        val g = (color.green * 255).roundToInt()
        val b = (color.blue * 255).roundToInt()
        val a = color.alpha
        val css = "rgba($r,$g,$b,$a)"
        ctx.fillStyle = css
        ctx.strokeStyle = css
    }

    override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        ctx.beginPath()
        ctx.moveTo(toPixel(x1), toPixel(y1))
        ctx.lineTo(toPixel(x2), toPixel(y2))
        ctx.stroke()
    }

    override fun drawText(text: String, x: Double, y: Double) {
        updateFont()
        val px = toPixel(x)
        val py = toPixel(y)
        val metrics = ctx.measureText(text)
        val textWidth = metrics.width
        val ascent = metrics.asDynamic().actualBoundingBoxAscent as Double
        val descent = metrics.asDynamic().actualBoundingBoxDescent as Double
        val xPos = when (textAlign) {
            TextAlign.CENTER -> px - textWidth / 2
            TextAlign.RIGHT -> px - textWidth
            TextAlign.LEFT -> px
        }
        val yPos = py + (ascent - descent) / 2
        ctx.fillText(text, xPos, yPos)
    }

    override fun fillRect(x: Double, y: Double, width: Double, height: Double) {
        ctx.fillRect(toPixel(x), toPixel(y), toPixel(width), toPixel(height))
    }

    override fun fillRoundRect(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        cornerRadius: Double
    ) {
        val px = toPixel(x)
        val py = toPixel(y)
        val pw = toPixel(width)
        val ph = toPixel(height)
        val pr = toPixel(cornerRadius)
        ctx.beginPath()
        ctx.moveTo(px + pr, py)
        ctx.arcTo(px + pw, py, px + pw, py + ph, pr)
        ctx.arcTo(px + pw, py + ph, px, py + ph, pr)
        ctx.arcTo(px, py + ph, px, py, pr)
        ctx.arcTo(px, py, px + pw, py, pr)
        ctx.closePath()
        ctx.fill()
    }

    override fun drawRect(x: Double, y: Double, width: Double, height: Double) {
        ctx.strokeRect(toPixel(x), toPixel(y), toPixel(width), toPixel(height))
    }

    override fun getHeight(): Double = toDp(canvas.height.toDouble())
    override fun getWidth(): Double = toDp(canvas.width.toDouble())

    override fun setFont(font: Font) {
        this.font = font
        updateFont()
    }

    override fun setFontSize(size: Double) {
        this.fontSize = size
        updateFont()
    }

    override fun setStrokeWidth(size: Double) {
        ctx.lineWidth = size * pixelScale
    }

    override fun fillArc(
        centerX: Double,
        centerY: Double,
        radius: Double,
        startAngle: Double,
        swipeAngle: Double
    ) {
        val cx = toPixel(centerX)
        val cy = toPixel(centerY)
        val r = toPixel(radius)
        val start = -startAngle * PI / 180.0
        val end = -(startAngle + swipeAngle) * PI / 180.0
        ctx.beginPath()
        ctx.moveTo(cx, cy)
        ctx.arc(cx, cy, r, start, end, swipeAngle > 0)
        ctx.closePath()
        ctx.fill()
    }

    override fun fillCircle(centerX: Double, centerY: Double, radius: Double) {
        val cx = toPixel(centerX)
        val cy = toPixel(centerY)
        val r = toPixel(radius)
        ctx.beginPath()
        ctx.arc(cx, cy, r, 0.0, 2 * PI)
        ctx.fill()
    }

    override fun setTextAlign(align: TextAlign) {
        this.textAlign = align
    }

    override fun toImage(): Image {
        val copy = document.createElement("canvas") as HTMLCanvasElement
        copy.width = canvas.width
        copy.height = canvas.height
        val copyCtx = copy.getContext("2d") as CanvasRenderingContext2D
        copyCtx.drawImage(canvas, 0.0, 0.0)
        return JsImage(copy)
    }

    override fun measureText(text: String): Double {
        updateFont()
        return toDp(ctx.measureText(text).width)
    }

    private fun updateFont() {
        val sizePx = (fontSize * pixelScale).roundToInt()
        val family = when (font) {
            Font.REGULAR -> "NotoSans"
            Font.BOLD -> "NotoSansBold"
            Font.FONT_AWESOME -> "FontAwesome"
        }
        val weight = if (font == Font.BOLD) "bold" else "normal"
        ctx.font = "$weight ${sizePx}px $family"
    }

    companion object {
        fun create(width: Int, height: Int, pixelScale: Double = 2.0): JsCanvas {
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.width = (width * pixelScale).roundToInt()
            canvas.height = (height * pixelScale).roundToInt()
            return JsCanvas(canvas, pixelScale)
        }
    }
}
