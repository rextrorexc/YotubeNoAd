package app.yotubenoad

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.min

/**
 * Yari seffaf, yuvarlak tek buton. Acikken kalkan dolar ve tik cizilir,
 * kapaliyken tik silinir ve kalkan uzerine capraz cizgi cizilir.
 */
class ShieldButton(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.SURFACE }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = Palette.SURFACE_STROKE
    }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.8f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val shield = Path()
    private val check = Path()
    private val slash = Path()
    private val segment = Path()
    private val measure = PathMeasure()
    private val evaluator = ArgbEvaluator()

    /** 1 = acik, 0 = kapali (animasyonlu gecis) */
    private var state = 1f
    private var press = 0f
    private var stateAnim: ValueAnimator? = null
    private var pressAnim: ValueAnimator? = null

    var on: Boolean = true
        private set

    /** Animasyonsuz ilk durum */
    fun setInstant(value: Boolean) {
        on = value
        state = if (value) 1f else 0f
        invalidate()
    }

    fun setOn(value: Boolean) {
        if (on == value) return
        on = value
        stateAnim?.cancel()
        stateAnim = ValueAnimator.ofFloat(state, if (value) 1f else 0f).apply {
            duration = 380
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                state = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animatePress(to: Float) {
        pressAnim?.cancel()
        pressAnim = ValueAnimator.ofFloat(press, to).apply {
            duration = if (to > 0f) 140 else 320
            addUpdateListener {
                press = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animate().scaleX(0.88f).scaleY(0.88f).setDuration(120)
                    .setInterpolator(DecelerateInterpolator()).start()
                animatePress(1f)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animate().scaleX(1f).scaleY(1f).setDuration(280)
                    .setInterpolator(OvershootInterpolator(2.4f)).start()
                animatePress(0f)
                val inside = e.x in 0f..width.toFloat() && e.y in 0f..height.toFloat()
                if (e.actionMasked == MotionEvent.ACTION_UP && inside) performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f - dp(1f)

        canvas.drawCircle(cx, cy, radius, bgPaint)
        canvas.drawCircle(cx, cy, radius, ringPaint)
        if (press > 0f) {
            ripplePaint.alpha = (30 * press).toInt()
            canvas.drawCircle(cx, cy, radius * (0.55f + 0.45f * press), ripplePaint)
        }

        val r = dp(11f)
        val sy = cy - dp(0.5f)

        shield.reset()
        shield.moveTo(cx, sy - r)
        shield.lineTo(cx + 0.8f * r, sy - 0.65f * r)
        shield.lineTo(cx + 0.8f * r, sy + 0.05f * r)
        shield.cubicTo(cx + 0.8f * r, sy + 0.6f * r, cx + 0.4f * r, sy + 0.88f * r, cx, sy + r)
        shield.cubicTo(cx - 0.4f * r, sy + 0.88f * r, cx - 0.8f * r, sy + 0.6f * r, cx - 0.8f * r, sy + 0.05f * r)
        shield.lineTo(cx - 0.8f * r, sy - 0.65f * r)
        shield.close()

        val color = evaluator.evaluate(state, Palette.OFF, Palette.ON) as Int

        fillPaint.color = color
        fillPaint.alpha = (70 * state).toInt()
        canvas.drawPath(shield, fillPaint)

        linePaint.color = color
        canvas.drawPath(shield, linePaint)

        // tik isareti: state ile cizilir
        if (state > 0.01f) {
            check.reset()
            check.moveTo(cx - 0.32f * r, sy + 0.02f * r)
            check.lineTo(cx - 0.06f * r, sy + 0.3f * r)
            check.lineTo(cx + 0.38f * r, sy - 0.25f * r)
            measure.setPath(check, false)
            segment.reset()
            measure.getSegment(0f, measure.length * state, segment, true)
            canvas.drawPath(segment, linePaint)
        }

        // capraz cizgi: kapaliyken cizilir
        if (state < 0.99f) {
            val p = 1f - state
            slash.reset()
            slash.moveTo(cx - 0.95f * r, sy - 0.95f * r)
            slash.lineTo(cx + 0.95f * r, sy + 0.95f * r)
            measure.setPath(slash, false)
            segment.reset()
            measure.getSegment(0f, measure.length * p, segment, true)
            canvas.drawPath(segment, linePaint)
        }
    }
}
