package com.welen.it.pullanimation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;


/**
 * TODO: document your custom view class.
 */
public class PullAnimationView extends View {

    public static final boolean DEBUG = true;
    private static final int TAG_CIRCLE_ARROW = R.id.animator_circle_arrow_tag;
    private static final int TAG_CIRCLE_SCALE = R.id.animator_circle_scale_tag;

    private float mAngle = 0;
    private float mLineLen = 200;
    /**
     * 这个属性用来当line mode 为relative当时候，设置相对的比例
     */
    private float mLinePercent = 0.5f;
    private LineMode mLineMode = LineMode.RELATIVE;
    private AnimationState mState = AnimationState.LINE_ARROW;
    float mProcess = 0;
    int mGravity = Gravity.CENTER;

    int mEfficacyNum = 8;
    float mEfficacyDist = 50;
    float mEfficacyLen = 50;

    private Paint mPaint;
    private TextPaint mTextPaint;
    private float mArrowLen = 30;
    private int mArrowAngle = 45;
    private float mRadius = 100;
    private int mColor;
    private float mStokeWidth = 4;
    private boolean mCenterByCircle;
    private PointF mPoint = new PointF();
    private PointF mCenter = new PointF();
    private FPSTracker mTracker;
    /**
     * 此属性获取系统的
     */
    private static final int[] ATTRS = new int[] {
            android.R.attr.gravity,
    };

    enum LineMode{
        //消失当长度是圆圈增加当边长
        ABSOLUTE,
        //相对未知需要根据{@link #mLinePercent}来确定
        RELATIVE,
        //长度不会跟着变化
        FIXED
    }
    enum AnimationState {
        LINE_ARROW, ARROW_CIRCLE, SCALE_CIRCLE, CIRCLE_EFFICACY
    }

    public PullAnimationView(Context context) {
        super(context);
        init(null, 0);
    }

    public PullAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PullAnimationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private LineMode parseLineMode(int value){
        if(value == 0){
            return LineMode.ABSOLUTE;
        }else if(value == 1){
            return LineMode.RELATIVE;
        }else{
            return LineMode.FIXED;
        }
    }

    private void init(AttributeSet attrs, int defStyle) {
        //获取系统的属性
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, ATTRS);
        mGravity = typedArray.getInt(0, mGravity);
        typedArray.recycle();

        //获取我们自己的属性
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.PullAnimationView, defStyle, 0);

        mColor = a.getColor(R.styleable.PullAnimationView_color, Color.WHITE);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mStokeWidth = a.getDimension(R.styleable.PullAnimationView_stokeWidth, mStokeWidth);
        mArrowAngle = a.getInt(R.styleable.PullAnimationView_arrowAngle, mArrowAngle);
        mArrowLen = a.getDimension(R.styleable.PullAnimationView_arrowLen, mArrowLen);
        mEfficacyDist = a.getDimension(R.styleable.PullAnimationView_efficacyDist, mEfficacyDist);
        mEfficacyLen = a.getDimension(R.styleable.PullAnimationView_efficacyLen, mEfficacyLen);
        mEfficacyNum = a.getInt(R.styleable.PullAnimationView_efficacyNum, mEfficacyNum);
        mCenterByCircle = a.getBoolean(R.styleable.PullAnimationView_centerByCircle, false);
        mLineLen = a.getDimension(R.styleable.PullAnimationView_lineLen, mLineLen);
        mLineMode = parseLineMode(a.getInt(R.styleable.PullAnimationView_lineMode, 0));
        mRadius = a.getDimension(R.styleable.PullAnimationView_radius, mRadius);
        mLinePercent = a.getFloat(R.styleable.PullAnimationView_linePercent, mLinePercent);
        a.recycle();

        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);

        mTracker = new FPSTracker();
        mTracker.setTime(SystemClock.elapsedRealtimeNanos());

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();

        if(DEBUG){
            mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            mTextPaint.setColor(Color.GREEN);
            mTextPaint.setTextSize(50);
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mState == AnimationState.LINE_ARROW){
                        setCurrentState(AnimationState.ARROW_CIRCLE);
                    }else{
                        reset();
                    }
                }
            });
        }
    }

    private void invalidateTextPaintAndMeasurements() {
        mPaint.setColor(mColor);
        mPaint.setStrokeWidth(mStokeWidth);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int extra = (int) (mEfficacyDist + mEfficacyLen);
        int width, height;
        if(widthMode == MeasureSpec.EXACTLY){
            // 上层已经告诉我们要多大了，照做就可以
            width = widthSize;
        }else{
            width = (int) ((mRadius + extra) * 2);
            width = width + getPaddingLeft() + getPaddingRight();
            // 确认一下最小宽度
            width = Math.max(width, getSuggestedMinimumWidth());
            if(widthMode == MeasureSpec.AT_MOST){
                width = Math.min(widthSize, width);
            }
        }

        if(heightMode == MeasureSpec.EXACTLY){
            height = heightSize;
        }else{
            height = (int)(mCenterByCircle
                    ? (mRadius + Math.max(mLineLen,extra)) * 2
                    : (mRadius * 2 + Math.max(mLineLen,extra) + extra));
            height = height + getPaddingTop() + getPaddingBottom();
            height = Math.max(height, getSuggestedMinimumHeight());
            if(heightMode == MeasureSpec.AT_MOST){
                height = Math.min(heightSize, height);
            }
        }
        setMeasuredDimension(width, height);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int width = right - left;
        int height = bottom - top;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = width - paddingLeft - paddingRight;
        //int contentHeight = height - paddingTop - paddingBottom;
        int extra = (int) (mEfficacyLen + mEfficacyDist);
        int realWidth = (int) (mRadius + extra) * 2;
        //int realHeight = (int) (mRadius * 2 + Math.max(mLineLen, extra) + extra);
        //非黑即白
        if(Gravity.LEFT == mGravity){
            mCenter.x = realWidth / 2;
        }else if (Gravity.RIGHT  == mGravity){
            mCenter.x = contentWidth - realWidth / 2;
        }else{
            mCenter.x = contentWidth / 2;
        }
        mCenter.y = Math.max(mLineLen, extra) + mRadius;//暂时不处理垂直方向的gravity
        mCenter.offset(getPaddingLeft(), getPaddingTop());
        mRectF.set(mCenter.x - mRadius, mCenter.y - mRadius, mCenter.x + mRadius, mCenter.y + mRadius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (mState) {
            case LINE_ARROW:
                mAngle = 0;
                drawLine(canvas, mAngle);
                mPoint.set(mCenter.x, mCenter.y - mRadius);
                drawArrow(canvas, mPoint, 90);
                break;
            case ARROW_CIRCLE:
                drawLine(canvas, mAngle);
                drawArc(canvas, mAngle);
                calculateArrowPosition(mAngle);
                drawArrow(canvas, mPoint, mAngle);
                break;
            case SCALE_CIRCLE:
                drawCircle(canvas, mAngle / 360);
                break;
            case CIRCLE_EFFICACY:
                drawCircle(canvas, 1);
                drawEfficacy(canvas, mProcess / 100, mAngle);
                mProcess += 2;
                if (mProcess > 100) {
                    mProcess = 100;
                }
                mAngle += 3;
                if (mAngle > 360) {
                    mAngle = mAngle % 360;
                }
                postInvalidate();
                break;
        }

        if(DEBUG){
            mTracker.makeFPS();
            canvas.drawText(mTracker.getFPS(),0, 80, mTextPaint);
        }

    }

    public void startArrowCircleAnimator(){
        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 360);
        valueAnimator.setDuration(1000);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mState = AnimationState.ARROW_CIRCLE;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                setTag(TAG_CIRCLE_ARROW, null);
                if(mState == AnimationState.ARROW_CIRCLE){//check state
                    startScaleCircleAnimator();
                }
            }
        });
        valueAnimator.addUpdateListener(updateListener);
        setTag(TAG_CIRCLE_ARROW, valueAnimator);
        valueAnimator.start();
    }


    public void startScaleCircleAnimator(){
        ValueAnimator valueAnimator1 = ValueAnimator.ofInt(360, 0);
        valueAnimator1.setDuration(200);
        valueAnimator1.setInterpolator(new AccelerateInterpolator());
        ValueAnimator valueAnimator2 = ValueAnimator.ofInt(0, 360);
        valueAnimator2.setDuration(200);
        valueAnimator2.setInterpolator(new DecelerateInterpolator());
        valueAnimator1.addUpdateListener(updateListener);
        valueAnimator2.addUpdateListener(updateListener);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(valueAnimator1, valueAnimator2);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mState = AnimationState.SCALE_CIRCLE;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                setTag(TAG_CIRCLE_SCALE, null);
                mState = AnimationState.CIRCLE_EFFICACY;
                mProcess = 0;
                postInvalidate();
            }
        });
        setTag(TAG_CIRCLE_SCALE, set);
        set.start();
    }

    ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int value = (Integer) animation.getAnimatedValue();
            mAngle = value;
            postInvalidate();
        }
    };

    public void setCurrentState(AnimationState state){
        if(state == mState)return;//nothing
        cancelAnimatorIfNeed(TAG_CIRCLE_ARROW);
        cancelAnimatorIfNeed(TAG_CIRCLE_SCALE);
        mState = state;
        switch (state){
            case LINE_ARROW:
                mAngle = 0;
                break;
            case ARROW_CIRCLE:
                startArrowCircleAnimator();
                break;
            case SCALE_CIRCLE:
                startScaleCircleAnimator();
                break;
            case CIRCLE_EFFICACY:
                mProcess = 0;
                break;
        }
        postInvalidate();
    }

    public AnimationState getCurrentState(){
        return mState;
    }

    private void cancelAnimatorIfNeed(int tag) {
        Animator animator = (Animator) getTag();
        if(animator != null){
            if(animator.isRunning()){
                animator.cancel();
            }
            setTag(tag, null);
        }
    }

    public void reset(){
        setCurrentState(AnimationState.LINE_ARROW);
    }

    private void drawEfficacy(Canvas canvas, float process, float rotation){
        int myProcess = (int) (process *  mEfficacyNum);
        float radianOffset = (float) (Math.PI * 2 / mEfficacyNum);
        double radian = angle2Radian(rotation);
        for (int i = 0; i < myProcess; i++) {
            float startX = (float) (mCenter.x + (mRadius + mEfficacyDist) * Math.cos(radian));
            float startY = (float) (mCenter.y + (mRadius + mEfficacyDist) * Math.sin(radian));
            float endX = (float) (mCenter.x + (mRadius + mEfficacyDist + mEfficacyLen) * Math.cos(radian));
            float endY = (float) (mCenter.y + (mRadius + mEfficacyDist+ mEfficacyLen) * Math.sin(radian));
            canvas.drawLine(startX, startY, endX, endY, mPaint);
            radian += radianOffset;
        }
    }

    /**
     * draw circle with scale animation
     * @param canvas
     * @param process
     */
    private void drawCircle(Canvas canvas, float process){
        mPaint.setAlpha((int) (255 * process));
        canvas.drawCircle(mCenter.x, mCenter.y, mRadius * process, mPaint);
    }


    /**
     * draw line
     * @param canvas
     * @param rotation
     */
    private void drawLine(Canvas canvas, float rotation){
        float startX = mCenter.x;
        float startY = mCenter.y - mRadius;
        float endX = startX;
        float offset;
        if(mLineMode == LineMode.RELATIVE){
            if(mLinePercent <= 0){
                offset = 0;
            }else{
                offset = Math.max(0, Math.min(mLineLen, mLineLen * (1 - rotation/ 360/ mLinePercent)));
            }
        } else if(mLineMode == LineMode.ABSOLUTE){
            offset = Math.max(0, (float) (mLineLen - 2 * Math.PI * mRadius * (rotation / 360)));
        } else {
            offset = mLineLen;
        }
        float endY = startY - offset;
        canvas.drawLine(startX, startY, endX, endY, mPaint);
    }

    private void calculateArrowPosition(float rotation){
        double radian = angle2Radian(rotation - 90);
        float x = mCenter.x + (float) (mRadius * Math.cos(radian));
        float y = mCenter.y + (float) (mRadius * Math.sin(radian));
        mPoint.set(x, y);
    }


    private void drawArrow(Canvas canvas, PointF pointF, float rotation){
        double radian = angle2Radian(mArrowAngle);
        double rotationRadian = angle2Radian(rotation-180);
        double leftRadian = radian+ rotationRadian;
        double rightRadian = (2 * Math.PI - radian) + rotationRadian;
        double x1 = mArrowLen * Math.cos(leftRadian) + pointF.x;
        double y1 = mArrowLen * Math.sin(leftRadian) + pointF.y;
        double x2 = mArrowLen * Math.cos(rightRadian) + pointF.x;
        double y2 = mArrowLen * Math.sin(rightRadian) + pointF.y;
        canvas.drawLine(pointF.x,pointF.y, (float)x1, (float)y1,mPaint);
        canvas.drawLine(pointF.x, pointF.y,(float)x2,(float)y2,mPaint);
    }

    RectF mRectF = new RectF();
    private void drawArc(Canvas canvas, float rotation){
        canvas.drawArc(mRectF, -90, rotation, false, mPaint);
    }

    private double angle2Radian(float angle){
        return angle / 180 * Math.PI;
    }

    private double radian2Angle(float radian){
        return radian * Math.PI / 180;
    }

}
