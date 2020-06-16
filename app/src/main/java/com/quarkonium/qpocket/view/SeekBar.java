package com.quarkonium.qpocket.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.quarkonium.qpocket.R;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.TreeMap;

public class SeekBar extends View {

    /**
     * 当原始值需要变换的时候需要实现此接口
     */
    public interface ITransformer {
        float transform(float value, boolean fromModel);
    }


    private Scroller mScroller;
    private GestureDetector mGestureDetector;
    private Paint mThumbPaint;
    private Paint mThumbLinePaint;
    private Paint mLinePaint1;
    private Paint mLinePaint2;
    private Paint mLinePaintOther;
    private Paint mHighLightLinePaint;
    private Paint mNailPaint;
    private Paint mNailSolidPaint;
    private Map<String, Integer> mSavedColors;

    private float mThumbRadius = 10.0f;
    //private float mNailRadius = 4.0f;
    private float mNailRadius = 0;
    private float mNailStrokeWidth = 1.5f;
    private float mLineWidth = 1.5f;
    private float mDefaultAreaRadius;

    private float mSeekLength;
    private float mSeekLineStart;
    private float mSeekLineEnd;
    private float mNailOffset;
    private float mThumbOffset;
    private int mMaxValue = 100;
    private int mCurrentValue = 50;
    private int mDefaultValue = 50;
    private int mStartValue;
    private OnSeekChangeListener mListener;
    private OnSeekDefaultListener mDefaultListener;
    private float mStep;
    private SeekBarGestureListener mGestureListener;
    private String mLineColor;
    private boolean mEnableTouch = true;

    private Rect mCircleRect = new Rect();
    private boolean mIsGlobalDrag = true;
    private boolean mIsTouchCircle = false;
    private boolean mSupportSingleTap = true;
    private WeakReference<Bitmap> mBitmapReference;
    private int mDrableId = -1;

    private int mLeftColor;
    private int mRightColor;
    private int[] mColors;
    private LinearGradient mLinearGradient = null;


    private DrawMode mDrawMode = DrawMode.NORMAL;

    private float top;
    private float bottom;
    private RectF mRect = new RectF();
    private Rect mBitmapRect = new Rect();
    private int mHighLightLineColor;

    private ITransformer mTransformer;
    static private ITransformer mDefTransformer = new ITransformer() {
        @Override
        public float transform(float value, boolean fromModel) {
            return value;
        }
    };

    public SeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHighLightLineColor = context.getResources().getColor(R.color.colorAccent);

        mThumbRadius = dpToPixel(mThumbRadius);
        mNailRadius = dpToPixel(mNailRadius);
        mNailStrokeWidth = context.getResources().getDimension(R.dimen.seekbar_nail_stroke_width);
        mLineWidth = context.getResources().getDimension(R.dimen.seekbar_line_width);
        mDefaultAreaRadius = ((mThumbRadius - mNailRadius - mNailStrokeWidth) + mThumbRadius) / 2;
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mScroller = new Scroller(getContext());
        mGestureListener = new SeekBarGestureListener();
        mGestureDetector = new GestureDetector(getContext(), mGestureListener);

        mNailSolidPaint = new Paint();
        mNailSolidPaint.setAntiAlias(true);
        mNailSolidPaint.setColor(Color.parseColor("#000000"));
        mNailSolidPaint.setStrokeWidth(mNailStrokeWidth);
        mNailSolidPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mNailPaint = new Paint();
        mNailPaint.setAntiAlias(true);
        mNailPaint.setColor(Color.parseColor("#ffd600"));
        mNailPaint.setStrokeWidth(mNailStrokeWidth);
        mNailPaint.setStyle(Paint.Style.STROKE);

        mThumbPaint = new Paint();
        mThumbPaint.setAntiAlias(true);
        mThumbPaint.setColor(Color.parseColor("#ffffff"));
        mThumbPaint.setStyle(Paint.Style.FILL);
        mThumbPaint.setShadowLayer(dpToPixel(1), 0, 0, Color.parseColor("#a0000000"));

        mThumbLinePaint = new Paint(mThumbPaint);

        mLinePaintOther = new Paint();
        mLinePaintOther.setAntiAlias(true);

        mLinePaint1 = new Paint();
        mLinePaint1.setAntiAlias(true);
        mLinePaint1.setColor(Color.parseColor("#e9f9f1"));
        mLinePaint1.setAlpha(200);

        mLinePaint2 = new Paint();
        mLinePaint2.setAntiAlias(true);
        mLinePaint2.setColor(Color.parseColor("#e9f9f1"));
        mLinePaint2.setAlpha(200);

        mHighLightLinePaint = new Paint();
        mHighLightLinePaint.setAntiAlias(true);

        //mHighLightLinePaint.setColor(Color.parseColor("#1ec896"));
        mHighLightLinePaint.setColor(mHighLightLineColor);
        mHighLightLinePaint.setAlpha(200);

        mSupportSingleTap = true;

        mTransformer = mDefTransformer;
    }

    public float dpToPixel(float dp) {
        return getResources().getDisplayMetrics().density * dp;
    }

    /**
     * set if will handle the singleTap event.
     *
     * @param support - by default is true.
     */
    public void setSingleTapSupport(boolean support) {
        mSupportSingleTap = support;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int hmode = MeasureSpec.getMode(heightMeasureSpec);
        if (hmode == MeasureSpec.AT_MOST) {
            int hsize = Math.round(mThumbRadius * 2);
            hsize += getPaddingTop() + getPaddingBottom();

            int wsize = MeasureSpec.getSize(widthMeasureSpec);
            setMeasuredDimension(wsize, hsize);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mSeekLength == 0) {
            final int width = getWidth();
            mSeekLength = width - getPaddingLeft() - getPaddingRight() - mThumbRadius * 2;
            mSeekLineStart = getPaddingLeft() + mThumbRadius;
            mSeekLineEnd = width - getPaddingRight() - mThumbRadius;

            int currValue = Math.max(0, mCurrentValue);
            mNailOffset = mSeekLength * mDefaultValue / mMaxValue;
            if (mDefaultValue == 0
                    || mDefaultValue == mMaxValue) {
                mThumbOffset = mSeekLength * currValue / mMaxValue;
            } else {
                float defaultAreaLength = mDefaultAreaRadius * 2;
                if (currValue < mDefaultValue) {
                    mThumbOffset = (mSeekLength - defaultAreaLength) * currValue / mMaxValue;
                } else if (currValue > mDefaultValue) {
                    mThumbOffset = (mSeekLength - defaultAreaLength) * currValue / mMaxValue + mDefaultAreaRadius * 2;
                } else {
                    mThumbOffset = mNailOffset;
                }
            }

            top = getMeasuredHeight() / 2 - mLineWidth / 2;
            bottom = top + mLineWidth;

            mRect.set(mSeekLineStart, top, mSeekLineEnd, bottom);

            if (mDrawMode == DrawMode.SHADER) {
                int disX = Math.round(mSeekLineEnd - mSeekLineStart);
                mLinearGradient = new LinearGradient(0, 0, disX, 0, mColors, null, Shader.TileMode.CLAMP);
                mLinePaintOther.setShader(mLinearGradient);
            }
        }

        final float top = getMeasuredHeight() / 2 - mLineWidth / 2;
        final float bottom = top + mLineWidth;


        //draw thumb
        final float nailX = mSeekLineStart + mNailOffset;
        final float nailY = getMeasuredHeight() / 2;

        float thumbX = mSeekLineStart + mThumbOffset;
        final float thumbY = getMeasuredHeight() / 2;


        switch (mDrawMode) {
            case BITMAP:
                Bitmap mBitmap = getDrawableBitmap();
                mBitmapRect.set(0, 0, (int) (mSeekLineStart + mBitmap.getWidth()), (int) (top + mBitmap.getHeight()));
                canvas.drawBitmap(mBitmap, mBitmapRect, mRect, mLinePaintOther);
                break;

            case SHADER:
                canvas.drawRect(mRect, mLinePaintOther);

                break;
            case NORMAL:
            default:
                final float right1 = mSeekLineStart + mNailOffset + mNailStrokeWidth / 2 - mNailRadius;

                if (right1 > mSeekLineStart) {
                    canvas.drawRect(mSeekLineStart, top, right1, bottom, mLinePaint1);
                }
                final float left2 = right1 + mNailRadius * 2;
                if (mSeekLineEnd > left2) {
                    canvas.drawRect(left2, top, mSeekLineEnd + dpToPixel(1), bottom, mLinePaint2);
                }

                float highLightLeft = thumbX + mThumbRadius;
                float highLightRight = nailX - mNailRadius;
                if (thumbX > nailX) {
                    highLightLeft = nailX + mNailRadius;
                    highLightRight = thumbX - mThumbRadius;
                }
                canvas.drawRect(highLightLeft, top, highLightRight + dpToPixel(1), bottom, mHighLightLinePaint);
                break;
        }


        //添加这个圆主要是为了填充外圆的实心黑色
//        canvas.drawCircle(nailX, nailY, mNailRadius, mNailSolidPaint);
        if (mStartValue < 0) {
            canvas.drawRect(nailX - 1, nailY - mThumbRadius, nailX + 1, nailY - mLineWidth - 1, mThumbLinePaint);
            canvas.drawRect(nailX - 1, nailY + mLineWidth + 1, nailX + 1, nailY + mThumbRadius, mThumbLinePaint);
        }
        //canvas.drawCircle(nailX, nailY, mNailRadius, mNailPaint);
        canvas.drawCircle(thumbX, thumbY, mThumbRadius - dpToPixel(1), mThumbPaint);
        mCircleRect.top = (int) (thumbY - mThumbRadius);
        mCircleRect.left = (int) (thumbX - mThumbRadius);
        mCircleRect.right = (int) (thumbX + mThumbRadius);
        mCircleRect.bottom = (int) (thumbY + mThumbRadius);

        if (mScroller.computeScrollOffset()) {
            mThumbOffset = mScroller.getCurrY();
            invalidate();
        }

        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (MotionEvent.ACTION_DOWN == event.getAction()) {
            if (!mIsGlobalDrag) {
                mIsTouchCircle = mCircleRect.contains((int) event.getX(), (int) event.getY());
            }
        }

        if (!mIsGlobalDrag && !mIsTouchCircle) {
            return true;
        }

        if (!mEnableTouch) {
            return true;
        }

        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        if (MotionEvent.ACTION_UP == event.getAction() || MotionEvent.ACTION_CANCEL == event.getAction()) {
            mIsTouchCircle = false;
            mGestureListener.onUp(event);
            if (null != mListener) {
                mListener.onSeekStopped(mTransformer.transform((mCurrentValue + mStartValue) * mStep, false), (mCurrentValue + mStartValue));
            }
            return true;
        }

        return false;
    }

    private float getThumbOffset(float pos) {
        if (pos < 0) {
            pos = 0;
        } else if (pos > mSeekLength) {
            pos = mSeekLength;
        }
        return pos;
    }

    public void setLineColor(String color) {
        mHighLightLinePaint.setColor(Color.parseColor(color));
        mNailPaint.setColor(Color.parseColor(color));
        invalidate();
    }

    public void setBaseLineColor(String color) {
        mLinePaint1.setColor(Color.parseColor(color));
        mLinePaint2.setColor(Color.parseColor(color));
    }

    public void setEditSeekBarColor(int colorId) {
        mLinePaint1.setColor(getResources().getColor(colorId));
        mLinePaint2.setColor(getResources().getColor(colorId));
        mThumbLinePaint.setColor(getResources().getColor(colorId));
    }

    public void setThumbColor(String color) {
        mThumbPaint.setColor(Color.parseColor(color));
    }


    @Override
    public void setEnabled(boolean enabled) {
        if (enabled == this.isEnabled()) {
            return;
        }

        super.setEnabled(enabled);
        mEnableTouch = enabled;

        if (mSavedColors == null) {
            mSavedColors = new TreeMap<String, Integer>();
        }

        if (enabled) {
            int color;
            color = mSavedColors.get("mNailPaint");
            mNailPaint.setColor(color);

            color = mSavedColors.get("mThumbPaint");
            mThumbPaint.setColor(color);

            color = mSavedColors.get("mLinePaint1");
            mLinePaint1.setColor(color);

            color = mSavedColors.get("mLinePaint2");
            mLinePaint2.setColor(color);

            color = mSavedColors.get("mHighLightLinePaint");
            mHighLightLinePaint.setColor(color);
        } else {
            mSavedColors.put("mNailPaint", mNailPaint.getColor());
            mSavedColors.put("mThumbPaint", mThumbPaint.getColor());
            mSavedColors.put("mLinePaint1", mLinePaint1.getColor());
            mSavedColors.put("mLinePaint2", mLinePaint2.getColor());
            mSavedColors.put("mHighLightLinePaint", mHighLightLinePaint.getColor());

            mNailPaint.setColor(Color.parseColor("#505050"));
            mThumbPaint.setColor(Color.parseColor("#505050"));
            mLinePaint1.setColor(Color.parseColor("#505050"));
            mLinePaint2.setColor(Color.parseColor("#505050"));
            mHighLightLinePaint.setColor(Color.parseColor("#505050"));
        }

    }

    public void setThumbSize(float size) {
        mThumbRadius = size;
    }

    /**
     * @param startRawValue  - slider变换前的startValue
     * @param endRawValue    - 变换前的endValue
     * @param circleRawValue - 变换前的原点
     * @param step           - 变换前的step
     * @param transform      - 变换函数接口
     */
    public void setSeekLength(float startRawValue, float endRawValue, float circleRawValue, float step, ITransformer transform) {
        setSeekLength(startRawValue, endRawValue, circleRawValue, step);
        mTransformer = transform;
    }

    /**
     * Get display value by real value.
     *
     * @param value - the real value (after transformed)
     * @return - the display value on UI.
     */
    public float getDisplayValue(float value) {
        float tValue = mTransformer.transform(value, true);
        return tValue / mStep;
    }

    public void setSeekLength(int startValue, int endValue, int circleValue, float step) {

        mDefaultValue = Math.round((float) (circleValue - startValue) / step);
        mMaxValue = Math.round((float) (endValue - startValue) / step);
        mStartValue = Math.round((float) startValue / step);
        mStep = step;
        mTransformer = mDefTransformer;
    }

    public void setSeekLength(float startValue, float endValue, float circleValue, float step) {

        mDefaultValue = Math.round((circleValue - startValue) / step);
        mMaxValue = Math.round((endValue - startValue) / step);
        mStartValue = Math.round(startValue / step);
        mStep = step;
        mTransformer = mDefTransformer;
    }

    public void setDefaultValue(float value) {
        mCurrentValue = Math.round(value / mStep) - mStartValue;
        if (null != mDefaultListener) {
            mDefaultListener.onSeekDefaulted(value);
        }

        updateThumbOffset();
        invalidate();
    }

    public void setUndoValue(float value) {
        if (null != mListener) {
            mListener.onSeekStarted(mTransformer.transform((mCurrentValue + mStartValue) * mStep, false), mCurrentValue + mStartValue);
        }

        mCurrentValue = Math.round(value / mStep) - mStartValue;
        updateThumbOffset();
        invalidate();

        if (null != mListener) {
            mListener.onSeekChanged(mTransformer.transform((mCurrentValue + mStartValue) * mStep, false), (mCurrentValue + mStartValue));
            mListener.onSeekStopped(mTransformer.transform((mCurrentValue + mStartValue) * mStep, false), (mCurrentValue + mStartValue));
        }
    }

    public float getValue() {
        float val = (mCurrentValue + mStartValue) * mStep;
        val = mTransformer.transform(val, false);

        return val;
    }

    public void setValue(float realValue) {
        setValue(realValue, false);
    }

    public void setValueFromModel(float realValue) {
        setValue(realValue, true);
    }

    public void setValue(float realValue, boolean fromModel) {
        float value = mTransformer.transform(realValue, true);

        int newValue = Math.round(value / mStep) - mStartValue;
        if (newValue == mCurrentValue) {
            return;
        }

        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
            invalidate();
        }

        mCurrentValue = newValue;
        if (!fromModel && null != mListener) {
            mListener.onSeekChanged(realValue, value / mStep);
        }

        updateThumbOffset();
        invalidate();
    }

    public void setOnSeekChangeListener(OnSeekChangeListener listener) {
        mListener = listener;
    }

    public OnSeekChangeListener getOnSeekChangeListener() {
        return mListener;
    }

    public void setOnDefaultListener(OnSeekDefaultListener listener) {
        mDefaultListener = listener;
    }

    public OnSeekDefaultListener getOnDefaultListener() {
        return mDefaultListener;
    }

    private void setValueInternal(int value) {
        if (mCurrentValue == value) {
            return;
        }
        mCurrentValue = value;
        if (null != mListener) {
            mListener.onSeekChanged(mTransformer.transform((value + mStartValue) * mStep, false), value + mStartValue);
        }

    }

    private void updateThumbOffset() {

        if (mDefaultValue == 0
                || mDefaultValue == mMaxValue) {
            if (mCurrentValue <= 0) {
                mThumbOffset = 0;
            } else if (mCurrentValue == mMaxValue) {
                mThumbOffset = mSeekLineEnd - mSeekLineStart;
            } else if (mCurrentValue == mDefaultValue) {
                mThumbOffset = mNailOffset;
            } else {
                mThumbOffset = mCurrentValue * mSeekLength / mMaxValue;
            }
        } else {
            float defaultAreaLength = mDefaultAreaRadius * 2;
            if (mCurrentValue <= 0) {
                mThumbOffset = 0;
            } else if (mCurrentValue == mMaxValue) {
                mThumbOffset = mSeekLineEnd - mSeekLineStart;
            } else if (mCurrentValue < mDefaultValue) {
                mThumbOffset = (mSeekLength - defaultAreaLength) * mCurrentValue / mMaxValue;
            } else if (mCurrentValue > mDefaultValue) {
                mThumbOffset = (mSeekLength - defaultAreaLength) * mCurrentValue / mMaxValue + defaultAreaLength;
            } else {
                mThumbOffset = mNailOffset;
            }
        }

    }

    public void reset() {
        mSeekLength = 0;
        mSeekLineStart = 0;
        mSeekLineEnd = 0;
        mNailOffset = 0;
        mThumbOffset = 0;
        mMaxValue = 0;
        mCurrentValue = Integer.MAX_VALUE;
        mDefaultValue = 0;
        mStartValue = 0;
        mStep = 0;
        mScroller.abortAnimation();
    }

    public interface OnSeekChangeListener {

        void onSeekStarted(float currentValue, float currDisplayValue);

        void onSeekChanged(float currentValue, float currDisplayValue);

        void onSeekStopped(float currentValue, float currDisplayValue);

    }


    public interface OnSeekDefaultListener {

        void onSeekDefaulted(float currentValue);
    }

    private class SeekBarGestureListener extends GestureDetector.SimpleOnGestureListener {

        public boolean onUp(MotionEvent e) {
            float initThumbOffset = mThumbOffset;
            updateThumbOffset();
            mScroller.startScroll(0, Math.round(initThumbOffset), 0, Math.round(mThumbOffset - initThumbOffset), 0);
            mThumbOffset = initThumbOffset;
            invalidate();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (mListener != null) {
                mListener.onSeekStarted(mTransformer.transform((mCurrentValue + mStartValue) * mStep, false),
                        mCurrentValue + mStartValue);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            mThumbOffset -= distanceX;
            if (mThumbOffset < mSeekLineStart - mThumbRadius) {
                mThumbOffset = mSeekLineStart - mThumbRadius;
            }

            if (mThumbOffset > mSeekLineEnd - mThumbRadius) {
                mThumbOffset = mSeekLineEnd - mThumbRadius;
            }
            float newValue;
            if (mDefaultValue == 0
                    || mDefaultValue == mMaxValue) {
                newValue = mThumbOffset * mMaxValue / mSeekLength;
            } else {
                float defaultAreaLength = mDefaultAreaRadius * 2;
                if (mThumbOffset < mNailOffset - mDefaultAreaRadius) {
                    newValue = mThumbOffset * (mMaxValue - 2)
                            / (mSeekLength - defaultAreaLength);

                } else if (mThumbOffset > mNailOffset + mDefaultAreaRadius) {
                    newValue = mDefaultValue + (mThumbOffset - mNailOffset - mDefaultAreaRadius)
                            * (mMaxValue - 2) / (mSeekLength - defaultAreaLength) + 1;
                } else {
                    newValue = mDefaultValue;
                }
            }

            if (newValue < 0) {
                newValue = 0;
            }

            if (newValue > mMaxValue) {
                newValue = mMaxValue;
            }

            setValueInternal(Math.round(newValue));
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (!mSupportSingleTap) {
                return false;
            }

            int newValue = mCurrentValue - 1;
            if (e.getX() > mThumbOffset) {
                newValue = mCurrentValue + 1;
            }

            if (newValue < 0) {
                newValue = 0;
            }

            if (newValue > mMaxValue) {
                newValue = mMaxValue;
            }

            setValueInternal(newValue);

            float initThumbOffset = mThumbOffset;
            updateThumbOffset();
            mScroller.startScroll(0, Math.round(initThumbOffset), 0, Math.round(mThumbOffset - initThumbOffset), 400);
            mThumbOffset = initThumbOffset;
            invalidate();

            if (null != mListener) {
                mListener.onSeekStopped(mTransformer.transform((mCurrentValue + mStartValue) * mStep, false), (mCurrentValue + mStartValue));
            }

            return true;
        }

    }

    public void setIsGlobalDrag(boolean mIsGlobalDrag) {
        this.mIsGlobalDrag = mIsGlobalDrag;
    }

    public void setLineDrable(int drableId) {
        mDrableId = drableId;
        if (-1 == drableId) {
            return;
        }
        if (null != mBitmapReference) {
            mBitmapReference.clear();
            mBitmapReference = null;
        }
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), mDrableId);
        mBitmapReference = new WeakReference<Bitmap>(bitmap);
        invalidate();
    }


    private Bitmap getDrawableBitmap() {
        if (mDrableId == -1 || null == mBitmapReference) {
            return null;
        }
        Bitmap bitmap = mBitmapReference.get();
        if (null == bitmap) {
            Bitmap tmpBitmap = BitmapFactory.decodeResource(this.getResources(), mDrableId);
            mBitmapReference = new WeakReference<Bitmap>(tmpBitmap);
            bitmap = mBitmapReference.get();
        }
        return bitmap;
    }


    public void setDrawMode(DrawMode drawMode) {
        mDrawMode = drawMode;
    }

    public void setLineClors(int colors[]) {
        mColors = colors;
        int disX = Math.round(mSeekLineEnd - mSeekLineStart);
        mLinearGradient = new LinearGradient(0, 0, disX, 0, mColors, null, Shader.TileMode.CLAMP);
        mLinePaintOther.setShader(mLinearGradient);
        invalidate();
    }


    public enum DrawMode {
        NORMAL,
        SHADER,
        BITMAP,
    }


}
