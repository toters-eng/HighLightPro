package com.hyy.highlightpro.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Region
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import com.hyy.highlightpro.parameter.Constraints
import com.hyy.highlightpro.parameter.HighlightParameter
import com.hyy.highlightpro.HighlightProImpl

/**
 *Create by hyy on 2021/2/6
 * [MaskContainer] will be add on the [HighlightProImpl]'s rootView
 * [HighlightParameter]'s tipView will be add on [MaskContainer]
 */
internal class MaskContainer constructor(context: Context, attributeSet: AttributeSet? = null) :
    FrameLayout(context, attributeSet) {

    private var rootWidth: Int = 0
    private var rootHeight: Int = 0
    private var bgColor: Int = -1
    private val highLightViewParameters = mutableListOf<HighlightParameter>()
    private val defaultHighlightBgColor: Int
        get() = "#80000000".toColorInt()

    private val defaultBgColor: Int
        get() = "#00000000".toColorInt()

    private var backPressedCallback: (() -> Unit)? = null
    internal var enableHighlight = true
    internal var interceptBackPressed = false
    internal var needAnchorTipView = true

    init {
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (enableHighlight) {
            canvas.save()

            //clip rect
            highLightViewParameters.forEach { parameter ->
                parameter.highlightShape?.run {
                    canvas.clipPath(path, Region.Op.DIFFERENCE)
                }
            }

            if (bgColor == -1) {
                bgColor = defaultHighlightBgColor
            }
            canvas.drawColor(bgColor)

            highLightViewParameters.forEach { parameter ->
                parameter.highlightShape?.run {
                    drawPath(canvas)
                }
            }
            canvas.restore()
        } else {
            if (bgColor == -1) {
                bgColor = defaultBgColor
            }
            canvas.drawColor(bgColor)
        }
    }

    override fun setBackgroundColor(color: Int) {
//        super.setBackgroundColor(color)
        bgColor = color
    }

    fun setOnBackPressedCallback(block: () -> Unit) {
        backPressedCallback = block
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backPressedCallback?.invoke()
            return interceptBackPressed
        }
        return super.onKeyDown(keyCode, event)
    }

    fun setHighLightParameters(list: List<HighlightParameter>) {
        children.forEach {
            it.clearAnimation()
        }
        //clear all childView
        removeAllViews()

        highLightViewParameters.clear()
        highLightViewParameters.addAll(list)

        addTipsView()

    }

    private fun addTipsView() {
        if (needAnchorTipView) {
            highLightViewParameters.forEach {highLightViewParameters->
                highLightViewParameters.tipsView?.run {
                    val layoutParams = calculateTipsViewLayoutParams(this, highLightViewParameters)
                    if (highLightViewParameters.tipViewDisplayAnimation != null){
                        startAnimation(highLightViewParameters.tipViewDisplayAnimation)
                    }
                    addView(this, layoutParams)
                }
            }
        } else {
            highLightViewParameters.forEach {highLightViewParameters->
                highLightViewParameters.tipsView?.run {
                    var layoutParams = (this.layoutParams ?: LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                    )) as LayoutParams
                    layoutParams.topMargin = highLightViewParameters.offsetY
                    layoutParams.leftMargin = highLightViewParameters.offsetX
                    if (highLightViewParameters.tipViewDisplayAnimation != null){
                        startAnimation(highLightViewParameters.tipViewDisplayAnimation)
                    }
                    addView(this, layoutParams)
                }
            }
        }

    }

    private fun calculateTipsViewLayoutParams(
        view: View,
        parameter: HighlightParameter
    ): LayoutParams {
        var layoutParams = (view.layoutParams ?: LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )) as LayoutParams

        val margin = parameter.marginOffset
        val highLightRect = parameter.rect
        val gravities = mutableListOf<Int>()
        val locale = ConfigurationCompat.getLocales(resources.configuration).get(0).toLanguageTag()
        parameter.constraints.forEach {
            when (it) {
                Constraints.StartToStartOfHighlight -> {
                    if (resources.configuration.layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR && locale != "ku") {
                        layoutParams.leftMargin = (highLightRect.left + margin.start).toInt()
                        gravities.add(Gravity.START)
                    } else {
                        layoutParams.rightMargin =
                            (rootWidth - highLightRect.right + margin.end).toInt()
                        gravities.add(Gravity.START)
                    }
                }

                Constraints.EndToStartOfHighlight -> {
                    //if constraint start to end of Highlight we need set right rightMargin
                    //and if margin.end is not 0 we should add the margin end
                    layoutParams.rightMargin =
                        (rootWidth - highLightRect.right + highLightRect.width() + margin.end).toInt()
                    gravities.add(Gravity.END)
                }

                Constraints.StartToEndOfHighlight -> {
                    layoutParams.leftMargin = (highLightRect.right + margin.start).toInt()
                    gravities.add(Gravity.START)
                }

                Constraints.EndToEndOfHighlight -> {
                    if (resources.configuration.layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR && locale != "ku") {
                        layoutParams.rightMargin =
                            (rootWidth - highLightRect.right + margin.end).toInt()
                        gravities.add(Gravity.END)
                    } else {
                        layoutParams.leftMargin = (highLightRect.left + margin.start).toInt()
                        gravities.add(Gravity.END)
                    }
                }

                Constraints.TopToTopOfHighlight -> {
                    layoutParams.topMargin = (highLightRect.top + margin.top).toInt()
                    gravities.add(Gravity.TOP)
                }

                Constraints.BottomToBottomOfHighlight -> {
                    layoutParams.bottomMargin =
                        (rootHeight - highLightRect.bottom + margin.bottom).toInt()
                    gravities.add(Gravity.BOTTOM)

                }

                Constraints.BottomToTopOfHighlight -> {
                    layoutParams.bottomMargin =
                        (rootHeight - highLightRect.bottom + highLightRect.height() + margin.bottom).toInt()
                    gravities.add(Gravity.BOTTOM)
                }

                Constraints.TopToBottomOfHighlight -> {
                    layoutParams.topMargin = (highLightRect.bottom + margin.top).toInt()
                    gravities.add(Gravity.TOP)
                }
            }
        }
        gravities.forEachIndexed { index, gravity ->
            if (index == 0) layoutParams.gravity = gravity
            else layoutParams.gravity = layoutParams.gravity or gravity
        }
        return layoutParams
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        highLightViewParameters.clear()
    }

    fun setRootWidth(width: Int) {
        this.rootWidth = width
    }

    fun setRootHeight(height: Int) {
        this.rootHeight = height
    }

}