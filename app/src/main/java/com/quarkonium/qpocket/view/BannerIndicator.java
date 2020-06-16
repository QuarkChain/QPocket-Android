
package com.quarkonium.qpocket.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;

import com.quarkonium.qpocket.R;

public class BannerIndicator extends AbsIndicator {

    private Drawable mIndDrawable;
    private Drawable mHighlightDrawable;
    private int mCount;

    public BannerIndicator(Context context) {
        this(context, null);
    }

    public BannerIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        setGap(4);
        if (isInEditMode()) {
            return;
        }
        mIndDrawable = context.getResources().getDrawable(R.drawable.page_indicator);
        mIndDrawable.setBounds(0, 0, mIndDrawable.getIntrinsicWidth(), mIndDrawable.getIntrinsicHeight());
        mIndDrawable = DrawableCompat.wrap(mIndDrawable);
        DrawableCompat.setTint(mIndDrawable, Color.parseColor("#d0d0d0"));

        mHighlightDrawable = context.getResources().getDrawable(R.drawable.page_indicator_focused);
        mHighlightDrawable.setBounds(0, 0, mHighlightDrawable.getIntrinsicWidth(), mHighlightDrawable.getIntrinsicHeight());
        mHighlightDrawable = DrawableCompat.wrap(mHighlightDrawable);
        DrawableCompat.setTint(mHighlightDrawable, Color.parseColor("#bcbcbc"));
    }

    public void setCount(int count) {
        boolean changed = (mCount != count);
        mCount = count;
        if (changed) {
            requestLayout();
        }
    }

    public void setDAppColor() {
        mIndDrawable = getResources().getDrawable(R.drawable.page_indicator);
        mIndDrawable.setBounds(0, 0, mIndDrawable.getIntrinsicWidth(), mIndDrawable.getIntrinsicHeight());
        mIndDrawable = DrawableCompat.wrap(mIndDrawable);
        DrawableCompat.setTint(mIndDrawable, Color.parseColor("#cfcfcf"));

        mHighlightDrawable = getResources().getDrawable(R.drawable.page_indicator_focused);
        mHighlightDrawable.setBounds(0, 0, mHighlightDrawable.getIntrinsicWidth(), mHighlightDrawable.getIntrinsicHeight());
        mHighlightDrawable = DrawableCompat.wrap(mHighlightDrawable);
        DrawableCompat.setTint(mHighlightDrawable, Color.parseColor("#7d7d7d"));
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public Drawable getIndicator() {
        return mIndDrawable;
    }

    @Override
    public Drawable getHighlight() {
        return mHighlightDrawable;
    }

    public void clear() {
        mIndDrawable = null;
        mHighlightDrawable = null;
    }
}
