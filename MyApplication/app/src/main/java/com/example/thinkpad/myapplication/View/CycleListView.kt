package com.example.thinkpad.myapplication.View

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import java.util.*
import com.example.thinkpad.myapplication.R;

/**
 * Created by mei on 2017/6/15.
 */
class CycleListView : View{
    constructor(c:Context) : super(c)
    constructor(c:Context,arr:AttributeSet):super(c,arr){
        pullData(arr)
    }
    constructor(c:Context,arr:AttributeSet,defStyle:Int):super(c,arr,defStyle){
        pullData(arr)
    }
    var downY = 0f
    var firstY = 0f

    var childPair = 0
    var offset = 0f
    var itemHeight = 0f
    var halfRangeLength = 0f
    var speed = 0f
    var firstpoint = 0
    var mainTextSize = 0f
    var miniTextSize = 0f
    var totSize = 0f
    var textInX = 0f
    var textCenterY = 0f
    val slowerRate = 0.2f
    var isFixed = false
    var fixedColor = resources.getColor(R.color.colorPrimary)
    lateinit var roundRect : RectF
    val rand by lazy{Random(System.currentTimeMillis())}
    val foucusPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG)}
    val rectPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }
    val mSpeedTracker by lazy{VelocityTracker.obtain()}
    val mHandler = object : Handler(){
        override fun handleMessage(msg: Message?) {
            when(msg?.what){
                1 -> {invalidate()}
                4 -> {reLocate()}
            }
            super.handleMessage(msg)
        }
    }
    lateinit var sources : MutableList<String>

    init {
        initPaint();
    }

    fun pullData(arr:AttributeSet){
        val a : TypedArray = context.obtainStyledAttributes(arr,R.styleable.CycleListView)
        childPair = a.getInt(R.styleable.CycleListView_childpair,0)
    }
    fun bindData(arr : MutableList<String>){
        sources = arr;
    }

    inline fun initPaint(){
        with(foucusPaint){
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        with(rectPaint){
            color = fixedColor
            style = Paint.Style.STROKE
            strokeWidth = 12f
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)return
        if (sources == null||sources.size<1)return

        if(isFixed){
            canvas.drawRoundRect(roundRect,24f,24f,rectPaint)
        }
        foucusPaint.textSize = getzTextSize()
        foucusPaint.alpha = getzTextAlpha()
        canvas.drawText(sources[(firstpoint+childPair+1)%sources.size],textInX,getYLocation(0,true),foucusPaint)
        for(i in 1..(childPair+1)){
            foucusPaint.textSize = getcTextSize(i,true)
            foucusPaint.alpha = getTextAlpha(i,true)
            canvas.drawText(
                    sources[(firstpoint+childPair+1-i)%sources.size],textInX,
                    getYLocation(i,true),foucusPaint)
            foucusPaint.textSize = getcTextSize(i, false)
            foucusPaint.alpha = getTextAlpha(i,false)
            canvas.drawText(
                    sources[(firstpoint+childPair+1+i)%sources.size],textInX,
                    getYLocation(i,false),foucusPaint)

        }
    }

    inline fun getcTextSize(which : Int,above : Boolean):Float{
        return if(above){
            mainTextSize + totSize*(which*itemHeight-offset)
        }else{
            mainTextSize + totSize*(which*itemHeight+offset)
        }
    }

    inline fun getzTextSize():Float{
        return if(offset>0){
            mainTextSize + totSize*offset
        }else{
            mainTextSize - totSize*offset
        }
    }

    inline fun getTextAlpha(which:Int,above: Boolean):Int{
        return if(above){
            ((1-(itemHeight*which - offset)/halfRangeLength)*255).toInt()
        }else{
            ((1-(itemHeight*which + offset)/halfRangeLength)*255).toInt()
        }
    }

    inline fun getzTextAlpha():Int{
        return if(offset>0){
            ((1 - offset/halfRangeLength)*255).toInt()
        }else{
            ((1 + offset/halfRangeLength)*255).toInt()
        }
    }


    inline fun getYLocation(which:Int,above: Boolean):Float{
        return if(above){
            textCenterY - which*itemHeight + offset
        }else{
            textCenterY + which*itemHeight + offset
        }
    }

    fun reLocate(){
        if(offset != 0.0f){
            offset = 0f
            invalidate()
        }
    }

    fun randomScroll(){
        speed = rand.nextFloat()*14 + 16
        scroll()
    }

    fun getFocusText():String = sources[(firstpoint+childPair+1)%sources.size]

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mSpeedTracker.addMovement(event)

        when(event!!.action){
            MotionEvent.ACTION_MOVE ->{

                if (!isFixed){
                    offset += event.y - downY;
                    if(offset>itemHeight/2){
                        offset = offset - itemHeight
                        if(firstpoint == 0)firstpoint = sources.size-1
                        else firstpoint -= 1
                    }else if(offset<-itemHeight/2){
                        offset = itemHeight+offset
                        firstpoint = (firstpoint+1)%sources.size
                    }
                    downY = event.y;
                    invalidate();
                }

            }

            MotionEvent.ACTION_UP -> {
                mSpeedTracker.computeCurrentVelocity(1)
                speed = mSpeedTracker.yVelocity

                if((event.y == firstY)&&isInCenter(event)){
                    isFixed = !isFixed
                    reLocate()
                    invalidate()
                }else if(!isFixed){
                    scroll()
                }else{
                    reLocate()
                }
            }
            MotionEvent.ACTION_DOWN -> {
                speed = 0f
                downY = event.y
                firstY = event.y
            }
        }
        return true
    }

    fun scroll(){
        if (speed > 0){
            kotlin.concurrent.timer(period = 10,action = {
                if(speed>0){
                    offset += speed*10
                    if(offset>itemHeight)offset=itemHeight/2 + 1
                    if(offset>itemHeight/2){
                        offset = offset - itemHeight
                        if(firstpoint == 0)firstpoint = sources.size-1
                        else firstpoint -= 1
                    }
                    mHandler.sendEmptyMessage(1)
                    speed-=slowerRate
                }else{
                    this.cancel()
                    mHandler.sendEmptyMessage(4)
                }
            })
        }else if(speed < 0){
            kotlin.concurrent.timer(period = 10,action = {
                if(speed<0){
                    offset += speed*10
                    if(offset<-itemHeight)offset=-itemHeight/2 - 1
                    if(offset<-itemHeight/2){
                        offset = itemHeight+offset
                        firstpoint = (firstpoint+1)%sources.size
                    }
                    mHandler.sendEmptyMessage(1)
                    speed+=slowerRate
                }else{
                    this.cancel()
                    mHandler.sendEmptyMessage(4)
                }
            })
        }else{
            mHandler.sendEmptyMessage(4)
        }
    }

    inline fun isInCenter(event:MotionEvent):Boolean{
        return event.y in (itemHeight*childPair)..(itemHeight*(childPair+1))
    }

    fun clearRes(){
        mSpeedTracker?.recycle()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        foucusPaint.textSize = width.toFloat()/12
        val singleTextSize = foucusPaint.measureText("乙")
        itemHeight = height.toFloat()/(childPair*2 + 1)
        textInX = width.toFloat()/2
        textCenterY = height.toFloat()/2 + singleTextSize/2
        mainTextSize = width.toFloat()/8
        miniTextSize = mainTextSize/3

        totSize = (miniTextSize-mainTextSize)/itemHeight/(childPair+1)
        halfRangeLength = itemHeight*(childPair+1)

        roundRect = RectF(10f,itemHeight*childPair+10f,width-10f,itemHeight*(childPair+1)-10f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)

        foucusPaint.textSize = widthSize.toFloat() / 12
        var singleTextSize = foucusPaint.measureText("乙")
        if(heightMode == MeasureSpec.UNSPECIFIED){
            if(childPair <= 0)childPair = 2
            itemHeight = singleTextSize*2
            heightSize = (itemHeight*(childPair*2 + 1)).toInt()
        }else if(heightMode == MeasureSpec.EXACTLY){
            if(childPair == 0){
                itemHeight = singleTextSize*2
                if(heightSize>itemHeight*3){
                    childPair = ((heightSize-itemHeight)/(itemHeight*2)).toInt()
                }
            }
        }
        setMeasuredDimension(widthSize,heightSize)
    }
}