package com.quarkonium.qpocket.view;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class UnScrollViewPager extends ViewPager {
    private boolean mIsCanScroll = false;

    public void setLimitScroll(boolean scroll) {
        mIsCanScroll = scroll;
    }

    public UnScrollViewPager(Context context) {
        super(context);
    }

    public UnScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mIsCanScroll && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mIsCanScroll && super.onTouchEvent(ev);
    }
}
