package com.quarkonium.qpocket.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class TouchRelativeLayout extends RelativeLayout {

    public interface OnMenuChangeListener {

        int getTitle();

        boolean onBack();
    }

    public interface IListener {
        void onBackFinished();
    }

    private IListener mListener;

    private boolean mIsTouchable = true;

    public TouchRelativeLayout(Context context) {
        this(context, null);
    }

    public TouchRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setListener(IListener listener) {
        mListener = listener;
    }

    public boolean onBack() {
        boolean ret = true;
        int i = getChildCount();
        if (i > 0) {
            View view = getChildAt(0);
            if (view instanceof TouchRelativeLayout.OnMenuChangeListener) {
                TouchRelativeLayout.OnMenuChangeListener mOnMenuChangeListner = (TouchRelativeLayout.OnMenuChangeListener) view;
                ret = mOnMenuChangeListner.onBack();
            }
        }

        if (ret && (mListener != null)) {
            mListener.onBackFinished();
        }
        return ret;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !mIsTouchable || super.onInterceptTouchEvent(ev);
    }

    public void setTouchable(boolean touchable) {
        mIsTouchable = touchable;
    }

    public boolean isTouchable() {
        return mIsTouchable;
    }
}
