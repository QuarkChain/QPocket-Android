package com.quarkonium.qpocket.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.quarkonium.qpocket.R;

public class TintTextView extends AppCompatTextView {

    private boolean mShowTint = true;

    ColorStateList tint = null;

    public TintTextView(Context context) {
        this(context, null);
    }

    public TintTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TintTextView(Context context, AttributeSet attrs, int defStyleAttr) {
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

    private void updateTintColor() {
        int color = tint.getColorForState(getDrawableState(), 0);
        if (mShowTint) {
            final Drawable[] compoundDrawables = getCompoundDrawables();
            applyCompoundDrawableTint(compoundDrawables[0], color);
            applyCompoundDrawableTint(compoundDrawables[1], color);
            applyCompoundDrawableTint(compoundDrawables[2], color);
            applyCompoundDrawableTint(compoundDrawables[3], color);
        }
        setTextColor(color);
    }

    final void applyCompoundDrawableTint(Drawable drawable, int color) {
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));

            if (Build.VERSION.SDK_INT <= 10) {
                drawable.invalidateSelf();
            }
        }
    }

    public void setShowTint(boolean showTint) {
        mShowTint = showTint;
    }
}
