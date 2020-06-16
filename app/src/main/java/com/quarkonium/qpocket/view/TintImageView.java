package com.quarkonium.qpocket.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.quarkonium.qpocket.R;


/**
 * This view is used to support tint with ColorStateList on devices lower than API 21.
 */

public class TintImageView extends AppCompatImageView {
    ColorStateList tint = null;

    public TintImageView(Context context) {
        this(context, null);
    }

    public TintImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TintImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.TintImageView, defStyleAttr, 0);
        try {
            if (a.hasValue(R.styleable.TintImageView_tintList)) {
                tint = a.getColorStateList(R.styleable.TintImageView_tintList);
            }
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (tint != null && tint.isStateful())
            updateTintColor();
    }

    public void setColorFilter(ColorStateList tint) {
        this.tint = tint;
        super.setColorFilter(tint.getColorForState(getDrawableState(), 0));
    }

    private void updateTintColor() {
        int color = tint.getColorForState(getDrawableState(), 0);
        setColorFilter(color);
    }

    public void clearTint() {
        clearColorFilter();
        this.tint = null;
    }
}
