package com.quarkonium.qpocket.view;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.quarkonium.qpocket.R;

public class JustifyTextView extends AppCompatTextView {

    private String mText;
    private float[] mWidths = new float[1];
    private SpannableStringBuilder mStringBuilder = new SpannableStringBuilder();

    private int mResId = -1;

    private View.OnClickListener mOnClickListener;

    public JustifyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!TextUtils.isEmpty(mText) && getMeasuredWidth() > 0) {
            int width = getMeasuredWidth();
            width = width - getPaddingLeft() - getPaddingRight();
            Paint mPaint = getPaint();
            mStringBuilder.clear();

            String text = (mResId != -1) ? (mText + "  c") : mText;
            int start = 0;//行起始Index
            int curLineWidth = 0;//当前行宽
            int length = text.length();
            for (int i = 0; i < length; i++) {
                char ch = text.charAt(i);//获取当前字符
                String srt = String.valueOf(ch);
                mPaint.getTextWidths(srt, mWidths);//获取这个字符的宽度
                if (ch == '\n') {//如果是换行符，则当独一行
                    start = i + 1;
                    curLineWidth = 0;
                    mStringBuilder.append(text.substring(start, i));
                } else {
                    curLineWidth += (int) (Math.ceil(mWidths[0]));//计算当前宽度
                    if (curLineWidth > width) {//直到当前行宽度大于控件宽度，截取为一行
                        mStringBuilder.append(text.substring(start, i));
                        if (i != (text.length() - 1)) {
                            mStringBuilder.append('\n');
                        }
                        start = i;
                        i--;
                        curLineWidth = 0;
                    } else {
                        if (i == (text.length() - 1)) {//剩余的单独一行
                            String s = text.substring(start, length);
                            if (!TextUtils.isEmpty(s)) {
                                mStringBuilder.append(s);
                            }
                        }
                    }
                }
            }

            if (mResId != -1) {
                Drawable drawable = getResources().getDrawable(mResId);
                drawable.setBounds(0, 0, 48, 50);
                ImageSpan span = new ImageSpan(drawable);
                int textLength = mStringBuilder.length();
                mStringBuilder.setSpan(span, textLength - 1, textLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            setText(mStringBuilder);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mOnClickListener != null) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= getTotalPaddingLeft();
                y -= getTotalPaddingTop();

                x += getScrollX();
                y += getScrollY();
                if (isClickSpan(x, y)) {
                    if (action == MotionEvent.ACTION_UP) {
                        mOnClickListener.onClick(this);
                    }
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private boolean isClickSpan(int x, int y) {
        Layout layout = getLayout();
        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);

        int start = x;
        while (start > 0) {
            int temp = layout.getOffsetForHorizontal(line, start = start - 5);
            if (temp != off) {
                break;
            }
        }
        if (x - start > 40) {
            return false;
        }

        ImageSpan[] imageSpans = mStringBuilder.getSpans(off - 2, off + 2, ImageSpan.class);
        return imageSpans.length != 0;
    }

    public void setText(String text) {
        mText = text;
        super.setText(text);
        requestLayout();
    }

    public void setCopyText(String text) {
        mText = text;
        mResId = R.drawable.tc_copy;
        super.setText(text);
    }

    @Override
    public CharSequence getText() {
        return mText;
    }

    public void setCopyButtonClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;
    }
}
