package com.quarkonium.qpocket.view;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.View;

import com.quarkonium.qpocket.R;

public class CollapsibleTextView extends AppCompatTextView {

    private int mSuffixColor = 0xff0000ff;

    private int mCollapsedLines = 1;

    private boolean mSuffixTrigger = false;

    private String mText;

    private OnClickListener mCustomClickListener;

    private boolean mShouldInitLayout = true;

    private boolean mExpanded = false;

    private String
            mCollapsedText = " Show All",
            mExpandedText = " Hide";

    public CollapsibleTextView(Context context) {
        this(context, null);
    }

    public CollapsibleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CollapsibleTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attributes = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.CollapsibleTextView, defStyleAttr, 0);

        mSuffixColor = attributes.getColor(R.styleable.CollapsibleTextView_suffixColor, 0xff0000ff);
        mCollapsedLines = attributes.getInt(R.styleable.CollapsibleTextView_collapsedLines, 1);
        mCollapsedText = " Show All";
        int collapsedId = attributes.getResourceId(R.styleable.CollapsibleTextView_collapsedText, -1);
        if (collapsedId != -1) {
            mCollapsedText = getResources().getString(collapsedId);
        }

        int expandedId = attributes.getResourceId(R.styleable.CollapsibleTextView_expandedText, -1);
        mExpandedText = " Hide";
        if (expandedId != -1) {
            mExpandedText = getResources().getString(expandedId);
        }
        mSuffixTrigger = attributes.getBoolean(R.styleable.CollapsibleTextView_suffixTrigger, false);

        this.mText = getText() == null ? null : getText().toString();
        setMovementMethod(LinkMovementMethod.getInstance());
        super.setOnClickListener(mClickListener);
    }

    private OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mSuffixTrigger) {
                mExpanded = !mExpanded;
                applyState(mExpanded);
            }

            if (mCustomClickListener != null) {
                mCustomClickListener.onClick(v);
            }
        }
    };

    private ClickableSpan mClickSpanListener
            = new ClickableSpan() {
        @Override
        public void onClick(View widget) {
            if (mSuffixTrigger) {
                mExpanded = !mExpanded;
                applyState(mExpanded);
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    };

    private void applyState(boolean expanded) {
        if (TextUtils.isEmpty(mText)) return;

        String note = mText, suffix;
        if (expanded) {
            suffix = mExpandedText;
            note = note + "\n";
        } else {
            if (mCollapsedLines - 1 < 0) {
                throw new RuntimeException("CollapsedLines must equal or greater than 1");
            }
            int lineEnd = getLayout().getLineEnd(mCollapsedLines - 1);
            suffix = mCollapsedText;
            int newEnd = lineEnd - suffix.length() - 1;
            int end = newEnd > 0 ? newEnd : lineEnd;

            TextPaint paint = getPaint();
            int maxWidth = mCollapsedLines * (getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
            while (paint.measureText(note.substring(0, end) + "...  " + suffix) > maxWidth)
                end--;
            note = note.substring(0, end) + "...  ";
        }

        final SpannableString str = new SpannableString(note + suffix);
        if (mSuffixTrigger) {
            str.setSpan(mClickSpanListener,
                    note.length(),
                    note.length() + suffix.length(),
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        str.setSpan(new ForegroundColorSpan(mSuffixColor),
                note.length(),
                note.length() + suffix.length(),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        post(new Runnable() {
            @Override
            public void run() {
                setText(str);
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mShouldInitLayout && getLineCount() > mCollapsedLines) {
            mShouldInitLayout = false;
            applyState(mExpanded);
        }
    }

    public void setFullString(CharSequence str) {
        this.mText = TextUtils.isEmpty(str) ? "" : str.toString();
        mShouldInitLayout = true;
        setText(str);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mCustomClickListener = l;
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public void setExpanded(boolean mExpanded) {
        if (this.mExpanded != mExpanded) {
            this.mExpanded = mExpanded;
            applyState(mExpanded);
        }
    }

    public int getSuffixColor() {
        return mSuffixColor;
    }

    public void setSuffixColor(int mSuffixColor) {
        this.mSuffixColor = mSuffixColor;
        applyState(mExpanded);
    }

    public int getCollapsedLines() {
        return mCollapsedLines;
    }

    public void setCollapsedLines(int mCollapsedLines) {
        this.mCollapsedLines = mCollapsedLines;
        mShouldInitLayout = true;
        setText(mText);
    }

    public boolean isSuffixTrigger() {
        return mSuffixTrigger;
    }

    public void setSuffixTrigger(boolean mSuffixTrigger) {
        this.mSuffixTrigger = mSuffixTrigger;
        applyState(mExpanded);
    }

    public String getCollapsedText() {
        return mCollapsedText;
    }

    public void setCollapsedText(String mCollapsedText) {
        this.mCollapsedText = mCollapsedText;
        applyState(mExpanded);
    }

    public String getExpandedText() {
        return mExpandedText;
    }

    public void setExpandedText(String mExpandedText) {
        this.mExpandedText = mExpandedText;
        applyState(mExpanded);
    }
}
