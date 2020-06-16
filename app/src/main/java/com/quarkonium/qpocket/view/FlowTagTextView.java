package com.quarkonium.qpocket.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quarkonium.qpocket.R;

public class FlowTagTextView extends LinearLayout {

    private TextView mTextView;

    public FlowTagTextView(Context context) {
        this(context, null);
    }

    public FlowTagTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowTagTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(HORIZONTAL);

        LayoutInflater.from(context).inflate(R.layout.community_flow_tag_view, this, true);
        mTextView = findViewById(R.id.text);

        setGravity(Gravity.CENTER_VERTICAL);
    }

    public void setText(int resId) {
        mTextView.setText(resId);
    }

    public void setText(CharSequence text) {
        mTextView.setText(text);
    }

    public String getText() {
        return mTextView.getText().toString();
    }

    public void setTextColor(int color) {
        mTextView.setTextColor(color);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        int width = sizeWidth - lp.leftMargin - lp.rightMargin;
        int widthNewMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);//减去view的margin 获得最大宽度
        // 通过索引拿到每一个子view
        View child = getChildAt(0);
        // 测量子View的宽和高,系统提供的measureChild
        measureChild(child, widthNewMeasureSpec, heightMeasureSpec);

        // 子View占据的宽度
        int childWidth = child.getMeasuredWidth();
        // 子View占据的高度
        int childHeight = child.getMeasuredHeight();
        setMeasuredDimension(childWidth, childHeight);
    }
}
