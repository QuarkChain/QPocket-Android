package com.quarkonium.qpocket.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * 圆形进度条
 */
public class CircleProgressBar extends View {
    private Paint mBackPaint;
    private Paint mFrontPaint;
    private float mStrokeWidth = 10;
    private float mRadius = 150;
    private RectF mRect;
    private float mProgress = 0;
    private int mMax = 100;
    private int mCount = 10;
    private int mWidth;
    private int mHeight;

    private ValueAnimator mAnim;

    public CircleProgressBar(Context context) {
        super(context);
        init();
    }

    public CircleProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    //完成相关参数初始化
    private void init() {
        mBackPaint = new Paint();
        mBackPaint.setColor(Color.WHITE);
        mBackPaint.setAntiAlias(true);
        mBackPaint.setStyle(Paint.Style.STROKE);
        mBackPaint.setStrokeWidth(mStrokeWidth);

        mFrontPaint = new Paint();
        mFrontPaint.setColor(Color.WHITE);
        mFrontPaint.setAntiAlias(true);
        mFrontPaint.setStyle(Paint.Style.FILL);
        mFrontPaint.setStrokeWidth(mStrokeWidth);

        mAnim = new ValueAnimator();
    }


    //重写测量大小的onMeasure方法和绘制View的核心方法onDraw()
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getRealSize(widthMeasureSpec);
        mHeight = getRealSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        initRect();
        float angle = mProgress / (float) mMax * 360;
        canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius, mBackPaint);
        canvas.drawArc(mRect, -90, angle, true, mFrontPaint);
    }

    public int getRealSize(int measureSpec) {
        int result;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);

        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.UNSPECIFIED) {
            //自己计算
            result = (int) (mRadius * 2 + mStrokeWidth);
        } else {
            result = size;

            int strokeWidth = (int) (result / 15f);
            int radius = (int) ((result - strokeWidth) / 2f) - 5;
            if (mRadius > radius) {
                mStrokeWidth = strokeWidth;
                mRadius = radius;
            }
        }
        return result;
    }

    private void initRect() {
        if (mRect == null) {
            mRect = new RectF();
            int viewSize = (int) (mRadius * 2);
            int left = (mWidth - viewSize) / 2;
            int top = (mHeight - viewSize) / 2;
            int right = left + viewSize;
            int bottom = top + viewSize;
            mRect.set(left, top, right, bottom);
        }
    }

    public void updateProgress(int progress, long time) {
        float end = mProgress + progress;

        if (end >= mMax - mCount) {
            end = mMax - mCount;
            mCount--;
        }

        if (end >= mMax - 1) {
            end = mMax - 1;
        }

        if (mProgress == end) {
            return;
        }

        mAnim.cancel();
        mAnim.setObjectValues(mProgress, end);
        mAnim.setDuration(time);
        mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgress = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mAnim.start();
    }

    public void setProgress(int progress) {
        if (progress < mProgress) {
            return;
        }

        if (mAnim.isRunning()) {
            mAnim.cancel();
        }

        mProgress = progress;
        if (mProgress >= mMax) {
            mProgress = mMax;
        }
        postInvalidate();
    }

    public void endProgress(Animator.AnimatorListener listener) {
        mAnim.cancel();

        mAnim.setObjectValues(mProgress, mMax);
        mAnim.setDuration(500);
        mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgress = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mAnim.addListener(listener);
        mAnim.start();
    }

    public void reset() {
        mProgress = 0;
        updateProgress(3, 200);
    }
}
