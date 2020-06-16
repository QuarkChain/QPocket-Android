package com.quarkonium.qpocket.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.quarkonium.qpocket.R;

public class MaxHeightLinearLayout extends LinearLayout {

    private int mMaxHeight;

    public MaxHeightLinearLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public MaxHeightLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public MaxHeightLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightLinearLayout, defStyle, 0);
            mMaxHeight = a.getDimensionPixelSize(R.styleable.MaxHeightLinearLayout_max_height, -1);
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            //此处是关键，设置控件高度不能超过自己需要的高度
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //重新计算控件高、宽
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
