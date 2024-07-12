package com.drawing.han

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class DrawingBoardView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var drawPath: Path = Path()
    private var drawPaint: Paint = Paint()
    private var canvasPaint: Paint = Paint(Paint.DITHER_FLAG)
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null

    //三角形
    private val trianglePath = Path()
    private var triangleStartX = 0f
    private var triangleStartY = 0f

    //四边形
    private val quadrilateralPath = Path()
    private var quadrilateralStartX = 0f
    private var quadrilateralStartY = 0f

    //圆
    private var circleStartX = 0f
    private var circleStartY = 0f
    private var circleRadius = 0f

    //椭圆
    private var ellipseStartX = 0f
    private var ellipseStartY = 0f
    private var ellipseEndX = 0f
    private var ellipseEndY = 0f

    //箭头
    private var arrowStartX = 0f
    private var arrowStartY = 0f
    private var arrowEndX = 0f
    private var arrowEndY = 0f

    //直线
    private var lineStartX = 0f
    private var lineStartY = 0f
    private var lineEndX = 0f
    private var lineEndY = 0f
    //储存绘制
    private val drawingStack = mutableListOf<DrawAction>()

    private val undoStack = mutableListOf<DrawAction>()

    //图案类型
    var type: ActionType = ActionType.PATH
    private var isDraw = true

    enum class ActionType {
        PATH, TRIANGLE, QUADRILATERAL, CIRCLE, ELLIPSE, ARROW, LINE
    }

    private data class DrawAction(
        val type: ActionType,
        val path: Path? = null,
        val startX: Float = 0f,
        val startY: Float = 0f,
        val endX: Float = 0f,
        val endY: Float = 0f,
        val radius: Float = 0f,
        val paint: Paint
    )

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        drawPaint.color = Color.BLACK
        drawPaint.isAntiAlias = true
        drawPaint.strokeWidth = 10f
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND

        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                drawCanvas = Canvas(canvasBitmap!!)
                backgroundBitmap?.let {
                    backgroundBitmap = Bitmap.createScaledBitmap(it, width, height, true)
                }
            }
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
        backgroundBitmap?.let {
            backgroundBitmap = Bitmap.createScaledBitmap(it, w, h, true)
        }
    }

    private fun dip2px(dip: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dip,
            resources.displayMetrics
        )
    }

    private fun sp2px(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            resources.displayMetrics
        )
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        backgroundBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, canvasPaint)
        }
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)
        for (action in drawingStack) {
            when (action.type) {
                ActionType.PATH -> action.path?.let {
                    canvas.drawPath(it, action.paint)
                }
                //三角形
                ActionType.TRIANGLE -> action.path?.let { canvas.drawPath(it, action.paint) }
                //四边形
                ActionType.QUADRILATERAL -> action.path?.let {
                    canvas.drawPath(
                        it,
                        action.paint
                    )
                }
                //圆
                ActionType.CIRCLE -> canvas.drawCircle(
                    action.startX,
                    action.startY,
                    action.radius,
                    action.paint
                )
                //椭圆
                ActionType.ELLIPSE -> canvas.drawOval(
                    action.startX,
                    action.startY,
                    action.endX,
                    action.endY,
                    action.paint
                )
                //箭头
                ActionType.ARROW -> {
                    drawArrow(
                        canvas,
                        action.startX,
                        action.startY,
                        action.endX,
                        action.endY,
                        action.paint
                    )
                }
                //直线
                ActionType.LINE -> canvas.drawLine(
                    action.startX,
                    action.startY,
                    action.endX,
                    action.endY,
                    action.paint
                )
            }
        }
        canvas.drawPath(drawPath, drawPaint)
        when (type) {
            ActionType.TRIANGLE -> {
                //三角形
                canvas.drawPath(trianglePath, drawPaint)
            }

            ActionType.QUADRILATERAL -> {
                //四边形
                canvas.drawPath(quadrilateralPath, drawPaint)
            }

            ActionType.CIRCLE -> {
                //圆
                canvas.drawCircle(circleStartX, circleStartY, circleRadius, drawPaint)
            }

            ActionType.ELLIPSE -> {
                //椭圆
                val left = ellipseStartX.coerceAtMost(ellipseEndX)
                val right = ellipseStartX.coerceAtLeast(ellipseEndX)
                val top = ellipseStartY.coerceAtMost(ellipseEndY)
                val bottom = ellipseStartY.coerceAtLeast(ellipseEndY)
                canvas.drawOval(left, top, right, bottom, drawPaint)
            }

            ActionType.ARROW -> {
                //箭头
                if (arrowStartX.toInt() == 0 && arrowStartY.toInt() == 0 && arrowEndX.toInt() == 0 && arrowEndY.toInt() == 0) {
                    return
                }
                drawArrow(canvas, arrowStartX, arrowStartY, arrowEndX, arrowEndY)
            }

            ActionType.LINE -> {
                //直线
                if (lineStartX.toInt() == 0 && lineStartY.toInt() == 0 && lineEndX.toInt() == 0 && lineEndY.toInt() == 0) {
                    return
                }
                canvas.drawLine(lineStartX, lineStartY, lineEndX, lineEndY, drawPaint)
            }

            else -> {}
        }
        if (isDraw || drawingStack.isEmpty()) {
            val textPaint = TextPaint()
            textPaint.color = Color.parseColor("#666666")
            textPaint.textSize = sp2px(14f)
            val str = ""
            val layout = StaticLayout(
                str,
                textPaint,
                width - dip2px(16f).toInt(),
                Layout.Alignment.ALIGN_NORMAL,
                1.0F,
                0.0F,
                true
            )
            canvas.translate(dip2px(8f), dip2px(8f))
            layout.draw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                when (type) {
                    ActionType.PATH -> {
                        drawPath.moveTo(touchX, touchY)
                    }

                    ActionType.TRIANGLE -> {
                        //三角形
                        triangleStartX = touchX
                        triangleStartY = touchY
                        trianglePath.moveTo(touchX, touchY)
                    }

                    ActionType.QUADRILATERAL -> {
                        //四边形
                        quadrilateralStartX = touchX
                        quadrilateralStartY = touchY
                        quadrilateralPath.moveTo(touchX, touchY)
                    }

                    ActionType.CIRCLE -> {
                        //圆
                        circleStartX = touchX
                        circleStartY = touchY
                        circleRadius = 0f
                    }

                    ActionType.ELLIPSE -> {
                        //椭圆
                        ellipseStartX = touchX
                        ellipseStartY = touchY
                        ellipseEndX = touchX
                        ellipseEndY = touchY
                    }

                    ActionType.ARROW -> {
                        //箭头
                        arrowStartX = touchX
                        arrowStartY = touchY
                        arrowEndX = touchX
                        arrowEndY = touchY
                    }

                    ActionType.LINE -> {
                        //直线
                        lineStartX = touchX
                        lineStartY = touchY
                        lineEndX = touchX
                        lineEndY = touchY
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (type) {
                    ActionType.TRIANGLE -> {
                        //三角形
                        updateTrianglePath(touchX, touchY)
                    }

                    ActionType.QUADRILATERAL -> {
                        //四边形
                        updateQuadrilateralPath(touchX, touchY)
                    }

                    ActionType.CIRCLE -> {
                        //圆
                        updateCirclePath(touchX, touchY)
                    }

                    ActionType.ELLIPSE -> {
                        //椭圆
                        updateEllipsePath(touchX, touchY)
                    }

                    ActionType.ARROW -> {
                        //箭头
                        updateArrowPath(touchX, touchY)
                    }

                    ActionType.LINE -> {
                        //直线
                        lineEndX = touchX
                        lineEndY = touchY
                    }

                    ActionType.PATH -> {
                        drawPath.lineTo(touchX, touchY)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                isDraw = false
                val currentPaint = Paint(drawPaint)
                when (type) {
                    ActionType.TRIANGLE -> {
                        //三角形
                        updateTrianglePath(touchX, touchY)
//                    drawCanvas?.drawPath(trianglePath, drawPaint)
                        drawingStack.add(
                            DrawAction(
                                ActionType.TRIANGLE,
                                Path(trianglePath),
                                paint = currentPaint
                            )
                        )
                        trianglePath.reset()
                    }

                    ActionType.QUADRILATERAL -> {
                        //四边形
                        updateQuadrilateralPath(touchX, touchY)
//                    drawCanvas?.drawPath(quadrilateralPath, drawPaint)
                        drawingStack.add(
                            DrawAction(
                                ActionType.QUADRILATERAL,
                                Path(quadrilateralPath),
                                paint = currentPaint
                            )
                        )
                        quadrilateralPath.reset()
                    }

                    ActionType.CIRCLE -> {
                        //圆
                        updateCirclePath(touchX, touchY)
//                    drawCanvas?.drawCircle(circleStartX, circleStartY, circleRadius, drawPaint)
                        drawingStack.add(
                            DrawAction(
                                ActionType.CIRCLE,
                                null,
                                circleStartX,
                                circleStartY,
                                radius = circleRadius, paint = currentPaint
                            )
                        )
                        circleRadius = 0f
                    }

                    ActionType.ELLIPSE -> {
                        //椭圆
                        updateEllipsePath(touchX, touchY)
//                    drawCanvas?.drawOval(
//                        ellipseStartX.coerceAtMost(ellipseEndX),
//                        ellipseStartY.coerceAtMost(ellipseEndY),
//                        ellipseStartX.coerceAtLeast(ellipseEndX),
//                        ellipseStartY.coerceAtLeast(ellipseEndY),
//                        drawPaint
//                    )
                        drawingStack.add(
                            DrawAction(
                                ActionType.ELLIPSE,
                                null,
                                ellipseStartX,
                                ellipseStartY,
                                ellipseEndX,
                                ellipseEndY, paint = currentPaint
                            )
                        )
                        ellipseStartX = 0f
                        ellipseStartY = 0f
                        ellipseEndX = 0f
                        ellipseEndY = 0f
                    }

                    ActionType.ARROW -> {
                        //箭头
                        updateArrowPath(touchX, touchY)
//                    drawCanvas?.let {
//                        drawArrow(
//                            it,
//                            arrowStartX,
//                            arrowStartY,
//                            arrowEndX,
//                            arrowEndY
//                        )
//                    }
                        drawingStack.add(
                            DrawAction(
                                ActionType.ARROW,
                                null,
                                arrowStartX,
                                arrowStartY,
                                arrowEndX,
                                arrowEndY, paint = currentPaint
                            )
                        )
                        arrowStartX = 0f
                        arrowStartY = 0f
                        arrowEndX = 0f
                        arrowEndY = 0f
                    }

                    ActionType.LINE -> {
                        //直线
//                    drawCanvas?.drawLine(lineStartX, lineStartY, lineEndX, lineEndY, drawPaint)
                        drawingStack.add(
                            DrawAction(
                                ActionType.LINE,
                                null,
                                lineStartX,
                                lineStartY,
                                lineEndX,
                                lineEndY, paint = currentPaint
                            )
                        )
                        lineStartX = 0f
                        lineStartY = 0f
                        lineEndX = 0f
                        lineEndY = 0f
                    }

                    ActionType.PATH -> {
//                    drawCanvas?.drawPath(drawPath, drawPaint)
                        drawingStack.add(
                            DrawAction(
                                ActionType.PATH,
                                Path(drawPath),
                                paint = currentPaint
                            )
                        )
                        drawPath.reset()
                    }
                }
            }

            else -> return false
        }
        invalidate()
        return true
    }

    //箭头
    private fun updateArrowPath(touchX: Float, touchY: Float) {
        arrowEndX = touchX
        arrowEndY = touchY
    }

    //箭头
    private fun drawArrow(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        paint: Paint? = drawPaint
    ) {
        val arrowHeadAngle = Math.toRadians(45.0)
        val arrowHeadLength = 50f
        val dx = endX - startX
        val dy = endY - startY
        val angle = atan2(dy.toDouble(), dx.toDouble())

        val arrowX1 = (endX - arrowHeadLength * cos(angle - arrowHeadAngle)).toFloat()
        val arrowY1 = (endY - arrowHeadLength * sin(angle - arrowHeadAngle)).toFloat()
        val arrowX2 = (endX - arrowHeadLength * cos(angle + arrowHeadAngle)).toFloat()
        val arrowY2 = (endY - arrowHeadLength * sin(angle + arrowHeadAngle)).toFloat()

        if (paint != null) {
            canvas.drawLine(startX, startY, endX, endY, paint)
            canvas.drawLine(endX, endY, arrowX1, arrowY1, paint)
            canvas.drawLine(endX, endY, arrowX2, arrowY2, paint)
        }
    }

    //椭圆
    private fun updateEllipsePath(touchX: Float, touchY: Float) {
        ellipseEndX = touchX
        ellipseEndY = touchY
    }

    //正圆
    private fun updateCirclePath(touchX: Float, touchY: Float) {
        circleRadius =
            hypot((touchX - circleStartX).toDouble(), (touchY - circleStartY).toDouble())
                .toFloat()
    }

    //四边形
    private fun updateQuadrilateralPath(touchX: Float, touchY: Float) {
        quadrilateralPath.reset()
        quadrilateralPath.moveTo(quadrilateralStartX, quadrilateralStartY)
        quadrilateralPath.lineTo(touchX, quadrilateralStartY)
        quadrilateralPath.lineTo(touchX, touchY)
        quadrilateralPath.lineTo(quadrilateralStartX, touchY)
        quadrilateralPath.close()
    }

    //三角形
    private fun updateTrianglePath(touchX: Float, touchY: Float) {
        val sideLength = hypot(
            (touchX - triangleStartX).toDouble(),
            (touchY - triangleStartY).toDouble()
        ).toFloat()
        val height = (Math.sqrt(3.0) / 2 * sideLength).toFloat()
        val midX = (triangleStartX + touchX) / 2

        trianglePath.reset()
        trianglePath.moveTo(triangleStartX, triangleStartY)
        trianglePath.lineTo(touchX, triangleStartY)
        trianglePath.lineTo(midX, triangleStartY - height)
        trianglePath.close()
    }

    //设置图片
    fun setBackgroundBitmap(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        backgroundBitmap?.let {
            if (width > 0 && height > 0) {
                backgroundBitmap = Bitmap.createScaledBitmap(it, width, height, true)
            }
        }
        invalidate()
    }

    //添加
    fun addDraw() {
        if (undoStack.isNotEmpty()) {
            val drawAction = undoStack.lastOrNull()
            drawAction?.let {
                drawingStack.add(it)
                undoStack.remove(it)
            }
            invalidate()
        }
    }

    //撤销
    fun undo() {
//        isDraw = false
        if (drawingStack.isNotEmpty()) {
            val drawAction = drawingStack.lastOrNull()
            drawAction?.let {
                undoStack.add(it)
                drawingStack.remove(it)
            }
            redrawCanvas()
        }
    }

    private fun redrawCanvas() {
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
        backgroundBitmap?.let {
            drawCanvas?.drawBitmap(it, 0f, 0f, canvasPaint)
        }
        invalidate()
    }

    fun setActionType(type: ActionType) {
        this.type = type
    }

    fun setPaint(colorStr: String? = "#000000", width: Float? = 10f, alphaInt: Int? = 1) {
        drawPaint.apply {
            strokeWidth = width!!
            color = Color.parseColor(colorStr)
            alpha = (alphaInt!!.coerceIn(0, 100) * 2.55).toInt()
        }
        invalidate()
    }

    fun setPaintColor(colorStr: String) {
        drawPaint.color = Color.parseColor(colorStr)
        invalidate()
    }

    fun setStrokeWidth(width: Float) {
        drawPaint.strokeWidth = width
        invalidate()
    }

    fun setPaintAlpha(alpha: Int) {
        val alphaValue = (alpha.coerceIn(0, 100) * 2.55).toInt()
        drawPaint.alpha = alphaValue
        invalidate()
    }

    fun clear() {
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
        circleRadius = 0f
        drawPath.reset()
        lineStartX = 0f
        lineStartY = 0f
        lineEndX = 0f
        lineEndY = 0f
        drawingStack.clear()
        undoStack.clear()
        invalidate()
    }
}