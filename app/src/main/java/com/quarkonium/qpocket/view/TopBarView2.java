package com.quarkonium.qpocket.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quarkonium.qpocket.R;

public class TopBarView2 extends RelativeLayout {

    private ImageView mBackView;
    private ImageView mHomeView;
    private TextView mTitleView;
    private ImageView mRightImageView;
    private ImageView mRightImageView2;

    public TopBarView2(@NonNull Context context) {
        this(context, null);
    }

    public TopBarView2(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TopBarView2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.top_bar_layout2, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mBackView = findViewById(R.id.re_take);
        mHomeView = findViewById(R.id.btn_home);
        mTitleView = findViewById(R.id.top_bar_title);
        mRightImageView = findViewById(R.id.btn_refresh);
        mRightImageView2 = findViewById(R.id.btn_menu);
    }

    public void setTitle(int src) {
        mTitleView.setText(src);
    }

    public void setTitle(String text) {
        mTitleView.setText(text);
    }

    public void setBackClickListener(OnClickListener clickListener) {
        mBackView.setOnClickListener(clickListener);
    }

    public void setHomeClickListener(OnClickListener clickListener) {
        mHomeView.setOnClickListener(clickListener);
    }

    public void setRightImageClickListener(OnClickListener clickListener) {
        mRightImageView.setOnClickListener(clickListener);
    }

    public void setRight2ImageClickListener(OnClickListener clickListener) {
        mRightImageView2.setOnClickListener(clickListener);
    }

    public TextView getTitleView() {
        return mTitleView;
    }

    public ImageView getBackView() {
        return mBackView;
    }

    public ImageView getRightImageView() {
        return mRightImageView;
    }
}
