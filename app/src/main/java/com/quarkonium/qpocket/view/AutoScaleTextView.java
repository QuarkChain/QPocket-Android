package com.quarkonium.qpocket.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.quarkonium.qpocket.R;

public class AutoScaleTextView extends AppCompatTextView {

    private float preferredTextSize;
    private float minTextSize;
    private Paint textPaint;

    public AutoScaleTextView(Context context) {
        this(context, null);
    }

    public AutoScaleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.autoScaleTextViewStyle);
    }

    public AutoScaleTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.textPaint = new Paint();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoScaleTextView, defStyleAttr, 0);
        this.minTextSize = a.getDimension(R.styleable.AutoScaleTextView_minTextSize, 6f);
        a.recycle();
        this.preferredTextSize = this.getTextSize();
    }

    /**
     * 设置最小的size
     *
     * @param minTextSize
     */
    public void setMinTextSize(float minTextSize) {
        this.minTextSize = minTextSize;
    }

    /**
     * 根据填充内容调整textview
     *
     * @param text
     * @param textWidth
     */
    private void refitText(String text, int textWidth) {
        //如果内容是空，那就不去管他了
        if (textWidth <= 0 || text == null || text.length() == 0) {
            return;
        }

        int targetWidth = textWidth - this.getPaddingLeft() - this.getPaddingRight();//文字所占真实的宽度

        final float threshold = 0.5f;

        this.textPaint.set(this.getPaint());

        this.textPaint.setTextSize(this.preferredTextSize);
        //如果测量的文本宽度小于等于文本真实宽度，那就按原始像素显示
        if (this.textPaint.measureText(text) <= targetWidth) {
            this.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.preferredTextSize);
            return;
        }

        float tempMinTextSize = this.minTextSize;
        float tempPreferredTextSize = this.preferredTextSize;
        //这个循环就是在一次次的缩小文字大小，直到满足条件
        while ((tempPreferredTextSize - tempMinTextSize) > threshold) {
            float size = (tempPreferredTextSize + tempMinTextSize) / 2;
            this.textPaint.setTextSize(size);
            if (this.textPaint.measureText(text) >= targetWidth) {
                tempPreferredTextSize = size;
            } else {
                tempMinTextSize = size;
            }
        }
        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, tempMinTextSize);

    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        this.refitText(text.toString(), this.getWidth());
    }

    @Override
    protected void onSizeChanged(int width, int h, int oldw, int oldh) {
        if (width != oldw) {
            this.refitText(this.getText().toString(), width);
        }
    }
}
