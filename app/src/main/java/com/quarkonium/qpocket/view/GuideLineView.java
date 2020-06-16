package com.quarkonium.qpocket.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.quarkonium.qpocket.util.UiUtils;

public class GuideLineView extends AppCompatImageView {

    public enum Line {
        Line,
        Left,
        Right,
        LeftAndRight
    }

    private Paint mPaint;
    private Paint mLinePaint;

    private Line mLineState = Line.Left;
    private float mRadius;
    private float mStokeWidth;

    public GuideLineView(Context context) {
        this(context, null);
    }

    public GuideLineView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public GuideLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        mStokeWidth = UiUtils.dpToPixel(2f);
        mRadius = UiUtils.dpToPixel(6);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.parseColor("#ffffff"));
        mPaint.setStrokeWidth(mStokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);

        mLinePaint = new Paint(mPaint);
        mLinePaint.setPathEffect(new DashPathEffect(new float[]{mStokeWidth * 2.5f, mStokeWidth * 2.5f}, 0));
    }

    public void setLineState(Line state) {
        mLineState = state;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (mLineState) {
            case Line:
                canvas.drawCircle(getWidth() / 2f, getWidth() / 2f, mRadius, mPaint);
                canvas.drawLine(getWidth() / 2f, getWidth() / 2f + mRadius + mStokeWidth, getWidth() / 2f, getHeight(), mLinePaint);
                break;
            case Left:
                canvas.drawCircle(mRadius + mStokeWidth, mRadius + mStokeWidth, mRadius, mPaint);
                canvas.drawLine(mRadius * 2 + mStokeWidth * 2, mRadius + mStokeWidth, getWidth(), mRadius + mStokeWidth, mLinePaint);
                canvas.drawLine(getWidth() - mStokeWidth / 2, mRadius + mStokeWidth, getWidth() - mStokeWidth / 2, getHeight(), mLinePaint);
                break;
            case Right:
                canvas.drawCircle(getWidth() - mRadius - mStokeWidth, mRadius + mStokeWidth, mRadius, mPaint);
                canvas.drawLine(0, mRadius + mStokeWidth, getWidth() - mRadius * 2 - mStokeWidth * 2, mRadius + mStokeWidth, mLinePaint);
                canvas.drawLine(mStokeWidth / 2, mRadius + mStokeWidth, mStokeWidth / 2, getHeight(), mLinePaint);
                break;
            case LeftAndRight:
                canvas.drawCircle(mRadius + mStokeWidth, mRadius + mStokeWidth, mRadius, mPaint);
                canvas.drawCircle(getWidth() - mRadius - mStokeWidth, mRadius + mStokeWidth, mRadius, mPaint);
                canvas.drawLine(mRadius * 2 + mStokeWidth * 2, mRadius + mStokeWidth,
                        getWidth() - mRadius * 2 - mStokeWidth * 2, mRadius + mStokeWidth, mLinePaint);
                canvas.drawLine(getWidth() / 2 - mStokeWidth / 2, mRadius + mStokeWidth, getWidth() / 2 - mStokeWidth / 2, getHeight(), mLinePaint);
                break;
        }
    }

    public float getCircleWidth() {
        return mRadius * 2 + mStokeWidth * 2;
    }
}
