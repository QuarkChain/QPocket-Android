package com.quarkonium.qpocket.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.R;

public class TopBarView extends RelativeLayout {

    private ImageView mBackView;
    private TextView mTitleView;
    private ImageView mRightImageView;
    private TextView mRightTextView;
    private TextView mRightTextView2;

    public TopBarView(@NonNull Context context) {
        this(context, null);
    }

    public TopBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TopBarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.top_bar_layout, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mBackView = findViewById(R.id.re_take);
        mTitleView = findViewById(R.id.top_bar_title);
        mRightImageView = findViewById(R.id.right_img);
        mRightTextView = findViewById(R.id.right_text);
        mRightTextView2 = findViewById(R.id.right_text2);
    }

    public void setTitle(int src) {
        mTitleView.setText(src);
    }

    public void setTitle(String text) {
        mTitleView.setText(text);
    }

    public void setOnlyTitle(int src) {
        mBackView.setVisibility(GONE);
        RelativeLayout.LayoutParams lp = (LayoutParams) mTitleView.getLayoutParams();
        lp.leftMargin = (int) UiUtils.dpToPixel(15);
        mTitleView.setLayoutParams(lp);
        mTitleView.setText(src);
    }

    public void setRightText(int src) {
        mRightTextView.setText(src);
        mRightTextView.setVisibility(VISIBLE);
    }

    public void setRightText(String text) {
        mRightTextView.setText(text);
        mRightTextView.setVisibility(VISIBLE);
    }

    public void setRightTextDrawableRight(int src) {
        Drawable dra = getResources().getDrawable(src);
        dra.setBounds(0, 0, dra.getIntrinsicWidth(), dra.getIntrinsicHeight());
        mRightTextView.setCompoundDrawables(null, null, dra, null);
    }

    public void setRightImage(int res) {
        mRightImageView.setImageResource(res);
        mRightImageView.setVisibility(VISIBLE);
    }

    public void setRightTextClickListener(OnClickListener clickListener) {
        mRightTextView.setOnClickListener(clickListener);
    }

    public void setRightImageClickListener(OnClickListener clickListener) {
        mRightImageView.setOnClickListener(clickListener);
    }

    public void setBackClickListener(OnClickListener clickListener) {
        mBackView.setOnClickListener(clickListener);
    }

    public void serAllColor(int color) {
        mTitleView.setTextColor(color);
        mRightTextView.setTextColor(color);

        Drawable drawable = getResources().getDrawable(R.drawable.appbar_back_icon).mutate();
        //2:先调用DrawableCompat的wrap方法
        drawable = DrawableCompat.wrap(drawable);
        //3:再调用DrawableCompat的setTint方法，为Drawable实例进行着色
        DrawableCompat.setTint(drawable, color);
        mBackView.setImageDrawable(drawable);
    }

    public TextView getTitleView() {
        return mTitleView;
    }

    public TextView getRightTextView() {
        return mRightTextView;
    }

    public TextView getRightTextView2() {
        return mRightTextView2;
    }

    public ImageView getBackView() {
        return mBackView;
    }

    public ImageView getRightImageView() {
        return mRightImageView;
    }

    public void setBackgroundColor(int color) {
        View view = getChildAt(0);
        if (view != null) {
            view.setBackgroundColor(color);
        }
    }
}
