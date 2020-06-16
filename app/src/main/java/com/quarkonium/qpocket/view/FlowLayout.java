package com.quarkonium.qpocket.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.quarkonium.qpocket.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 流式布局
 */
public class FlowLayout extends ViewGroup {
    private interface FlowTagClickListener {
        void onSelected(TextBean bean);

        void onLabel();
    }

    public static class FlowTagClickListenerIml implements FlowTagClickListener {

        @Override
        public void onSelected(TextBean bean) {

        }

        @Override
        public void onLabel() {

        }
    }

    public static class TextBean {
        private int mIndex;
        private String mText;

        public TextBean(int index, String text) {
            mIndex = index;
            mText = text;
        }

        public int getIndex() {
            return mIndex;
        }

        public void setIndex(int mIndex) {
            this.mIndex = mIndex;
        }

        public String getText() {
            return mText;
        }

        public void setText(String mText) {
            this.mText = mText;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TextBean)) return false;

            TextBean textBean = (TextBean) o;

            if (mIndex != textBean.mIndex) return false;
            return mText != null ? mText.equals(textBean.mText) : textBean.mText == null;
        }
    }

    public static class ObjectBean<T> extends TextBean {

        private T t;

        public ObjectBean(int index, String text) {
            super(index, text);
        }

        public void setT(T t) {
            this.t = t;
        }

        public T getT() {
            return t;
        }
    }

    private FlowTagClickListenerIml mListener;
    private boolean mHasLabel;
    private int mMaxLine, mMaxLineHeight;
    private int mTextBackgroundId = -1;
    private int mTextColor = -1;

    /**
     * 存储所有的View
     */
    private List<List<View>> mAllViews = new ArrayList<>();
    /**
     * 每一行的高度
     */
    private List<Integer> mLineHeight = new ArrayList<>();

    public FlowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context) {
        this(context, null);
    }

    //获取选中view组字符串
    public int getSelectedIndex() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != null && child.isSelected()) {
                return i;
            }
        }
        return -1;
    }

    //是否显示头部标签
    public void setShowLabel(boolean show) {
        mHasLabel = show;
        if (mHasLabel) {
            addLabel();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

        // 如果是warp_content情况下，记录宽和高
        int width = 0;
        int height = 0;

        // 记录每一行的宽度与高度
        int lineWidth = 0;
        int lineHeight = 0;

        // 得到内部元素的个数
        int cCount = getChildCount();
        int lineCount = 0;

        for (int i = 0; i < cCount; i++) {
            // 通过索引拿到每一个子view
            View child = getChildAt(i);
            // 测量子View的宽和高,系统提供的measureChild
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            // 得到LayoutParams
            MarginLayoutParams lp = (MarginLayoutParams) child
                    .getLayoutParams();

            // 子View占据的宽度
            int childWidth = child.getMeasuredWidth() + lp.leftMargin
                    + lp.rightMargin;
            // 子View占据的高度
            int childHeight = child.getMeasuredHeight() + lp.topMargin
                    + lp.bottomMargin;

            // 换行 判断 当前的宽度大于 开辟新行
            if (lineWidth + childWidth > sizeWidth - getPaddingLeft() - getPaddingRight()) {
                // 对比得到最大的宽度
                width = Math.max(width, lineWidth);
                // 重置lineWidth
                lineWidth = childWidth;
                // 记录行高
                height += lineHeight;
                lineHeight = childHeight;
                if (lineCount < mMaxLine) {
                    mMaxLineHeight = height + getPaddingTop() + getPaddingBottom();
                    lineCount++;
                }
            } else
            // 未换行
            {
                // 叠加行宽
                lineWidth += childWidth;
                // 得到当前行最大的高度
                lineHeight = Math.max(lineHeight, childHeight);
            }
            // 特殊情况,最后一个控件
            if (i == cCount - 1) {
                width = Math.max(lineWidth, width);
                height += lineHeight;
                if (lineCount < mMaxLine) {
                    mMaxLineHeight = height + getPaddingTop() + getPaddingBottom();
                    lineCount++;
                }
            }
        }

        int minHeight = getSuggestedMinimumHeight();
        int totalHeight = modeHeight == MeasureSpec.EXACTLY ? sizeHeight : height + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(
                modeWidth == MeasureSpec.EXACTLY ? sizeWidth : width + getPaddingLeft() + getPaddingRight(),
                Math.max(minHeight, totalHeight)
        );

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mAllViews.clear();
        mLineHeight.clear();

        // 当前ViewGroup的宽度
        int width = getWidth();

        int lineWidth = 0;
        int lineHeight = 0;

        // 存放每一行的子view
        List<View> lineViews = new ArrayList<>();

        int cCount = getChildCount();

        for (int i = 0; i < cCount; i++) {
            View child = getChildAt(i);
            MarginLayoutParams lp = (MarginLayoutParams) child
                    .getLayoutParams();

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            // 如果需要换行
            if (childWidth + lineWidth + lp.leftMargin + lp.rightMargin > width - getPaddingLeft() - getPaddingRight()) {
                // 记录LineHeight
                mLineHeight.add(lineHeight);
                // 记录当前行的Views
                mAllViews.add(lineViews);

                // 重置我们的行宽和行高
                lineWidth = 0;
                lineHeight = childHeight + lp.topMargin + lp.bottomMargin;
                // 重置我们的View集合
                lineViews = new ArrayList<>();
            }
            lineWidth += childWidth + lp.leftMargin + lp.rightMargin;
            lineHeight = Math.max(lineHeight, childHeight + lp.topMargin
                    + lp.bottomMargin);
            lineViews.add(child);

        }// for end
        // 处理最后一行
        mLineHeight.add(lineHeight);
        mAllViews.add(lineViews);

        // 设置子View的位置

        int left = getPaddingLeft();
        int top = getPaddingTop();

        // 行数
        int lineNum = mAllViews.size();

        for (int i = 0; i < lineNum; i++) {
            // 当前行的所有的View
            lineViews = mAllViews.get(i);
            lineHeight = mLineHeight.get(i);

            for (int j = 0; j < lineViews.size(); j++) {
                View child = lineViews.get(j);
                // 判断child的状态
                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                MarginLayoutParams lp = (MarginLayoutParams) child
                        .getLayoutParams();

                int lc = left + lp.leftMargin;
                int tc = top + lp.topMargin;
                int rc = lc + child.getMeasuredWidth();
                int bc = tc + child.getMeasuredHeight();

                // 为子View进行布局
                child.layout(lc, tc, rc, bc);

                left += child.getMeasuredWidth() + lp.leftMargin
                        + lp.rightMargin;
            }
            left = getPaddingLeft();
            top += lineHeight;
        }

    }

    /**
     * 与当前ViewGroup对应的LayoutParams
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    public void setData(List<TextBean> data) {
        if (data != null && !data.isEmpty()) {
            for (TextBean text : data) {
                final FlowTagTextView textView = (FlowTagTextView) LayoutInflater.from(getContext()).inflate(R.layout.community_flow_view, this, false);
                textView.setText(text.mText);
                textView.setTag(text);
                textView.setOnClickListener((view) -> {
                    if (mListener != null) {
                        TextBean bean = (TextBean) view.getTag();
                        mListener.onSelected(bean);
                    }
                });

                if (mTextBackgroundId != -1) {
                    textView.setBackgroundResource(mTextBackgroundId);
                }

                if (mTextColor != -1) {
                    textView.setTextColor(mTextColor);
                }

                addView(textView);
            }
        }
    }

    public <T> void setObjectData(List<ObjectBean<T>> data) {
        if (data != null && !data.isEmpty()) {
            for (ObjectBean objectBean : data) {
                final FlowTagTextView textView = (FlowTagTextView) LayoutInflater.from(getContext()).inflate(R.layout.community_flow_view, this, false);
                textView.setText(objectBean.getText());
                textView.setTag(objectBean);
                textView.setOnClickListener((view) -> {
                    if (mListener != null) {
                        TextBean bean = (TextBean) view.getTag();
                        mListener.onSelected(bean);
                    }
                });

                if (mTextBackgroundId != -1) {
                    textView.setBackgroundResource(mTextBackgroundId);
                }

                if (mTextColor != -1) {
                    textView.setTextColor(mTextColor);
                }

                addView(textView);
            }
        }
    }


    public void setTagListener(FlowTagClickListenerIml listener) {
        mListener = listener;
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();
        if (mHasLabel) {
            addLabel();
        }
    }

    //头部添加标签
    public void addLabel() {
        View child = getChildAt(0);
        if (child == null || child instanceof FlowTagTextView) {
            final View view = LayoutInflater.from(getContext()).inflate(R.layout.community_flow_label_view, this, false);
            view.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onLabel();
                }
            });
            addView(view, 0);
        }
    }

    //获取所有item字符串
    public ArrayList<String> getAllItemTexts() {
        ArrayList<String> list = new ArrayList<>();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != null && child instanceof FlowTagTextView) {
                FlowTagTextView textView = (FlowTagTextView) child;
                list.add(textView.getText());
            }
        }
        return list;
    }

    public int getLines() {
        return mAllViews.size();
    }

    public void setMaxLine(int maxLine) {
        mMaxLine = maxLine;
        mMaxLineHeight = 0;
    }

    public int getMaxLineHeight() {
        return mMaxLineHeight;
    }

    public void setTextColor(int color) {
        mTextColor = color;
    }

    public void setTextBackgroundId(int id) {
        mTextBackgroundId = id;
    }
}
