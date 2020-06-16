
package com.quarkonium.qpocket.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;

public abstract class AbsIndicator extends View {

    private Context mContext;
    private int mCurrentItem;

    private int mGapWidth;
    private int mWidth;
    private int mHeight;
    private int mCellWidth;
    private int mCellHeight;

    private OnIndicatorClickListener mListener;
    private int mDownIndex = -1;

    private LinkedList<Rect> mRectList = new LinkedList<Rect>();

    public AbsIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setGap(10);
    }

    public AbsIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsIndicator(Context context) {
        this(context, null);
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }
        mCellWidth = getIndicator().getIntrinsicWidth() > getHighlight().getIntrinsicWidth() ? getIndicator()
                .getIntrinsicWidth() : getHighlight().getIntrinsicWidth();
        mCellHeight = getIndicator().getIntrinsicHeight() > getHighlight().getIntrinsicHeight() ? getIndicator()
                .getIntrinsicHeight() : getHighlight().getIntrinsicHeight();
        mWidth = mCellWidth * getCount() + mGapWidth * (getCount() + 1) + getPaddingLeft()
                + getPaddingRight();
        mHeight = mCellHeight + getPaddingBottom() + getPaddingTop();
    }

    public abstract int getCount();

    public abstract Drawable getIndicator();

    public abstract Drawable getHighlight();

    public int getCurrentItem() {
        return mCurrentItem;
    }

    /**
     * 设置指示器之间的距离，单位dp
     *
     * @param gap
     */
    public void setGap(int gap) {
        mGapWidth = (int) (gap * mContext.getResources().getDisplayMetrics().density);
    }

    public void setOnIndicatorClickListener(OnIndicatorClickListener listener) {
        mListener = listener;
    }

    public void setCurrentItem(int currentItem) {
        if (currentItem == mCurrentItem) {
            return;
        }
        if (currentItem < 0) {
            currentItem = 0;
        } else if (currentItem >= getCount()) {
            currentItem = getCount() - 1;
        }

        mCurrentItem = currentItem;
        if (mCurrentItem >= 0) {
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownIndex = touchIndex(event);
                if (mDownIndex >= 0 && mListener != null) {
                    mListener.onIndicatorClick(mDownIndex);
                }

                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_UP:


                mDownIndex = -1;
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private int touchIndex(MotionEvent event) {
        int i = 0;

        for (Rect r : mRectList) {
            if (r.contains((int) event.getX(), (int) event.getY())) {
                return i;
            }
            i++;
        }

        return -1;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        init();
        setMeasuredDimension(mWidth, mHeight);
    }

    private int mDeltaX = 0;

    @Override
    protected void onDraw(Canvas canvas) {
//        canvas.drawColor(Color.RED);
        mRectList.clear();
        mDeltaX = 0;

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        for (int i = 0; i < getCount(); i++) {

            if (i == 0) {
                //the first cell
                canvas.translate(mGapWidth, 0);
            }

            drawCell(canvas, getIndicator());
            if (i == mCurrentItem && isEnabled()) {
                drawCell(canvas, getHighlight());
            }
            canvas.translate(mCellWidth + mGapWidth, 0);


            Drawable indicator = getIndicator();
            Rect r = new Rect(0, 0, indicator.getIntrinsicWidth(), indicator.getIntrinsicHeight());
            r.offset(mDeltaX, 0);//计算indicator的当前位置

            //放大点击区域
            int scale = mGapWidth / 2;
            r.left -= scale;
            r.right += scale;
            r.top = 0;
            r.bottom = this.getHeight();

            mRectList.add(r);

            mDeltaX += (mCellWidth + mGapWidth);
        }
        canvas.restore();
    }

    private void drawCell(Canvas canvas, Drawable cell) {
        canvas.save();
        int xRevise = (mCellWidth - cell.getIntrinsicWidth()) / 2;
        int yRevise = (mCellHeight - cell.getIntrinsicHeight()) / 2;
        canvas.translate(xRevise, yRevise);
        cell.draw(canvas);
        canvas.restore();
    }

    public interface OnIndicatorClickListener {
        void onIndicatorClick(int index);
    }
}
