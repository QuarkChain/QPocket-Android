package com.quarkonium.qpocket.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.quarkonium.qpocket.R;

public class QuarkChooseLanDialog extends Dialog {
    public interface OnChooseListener {
        void choose(int index);
    }

    private ViewGroup mEnView;
    private ViewGroup mCnView;
    private ViewGroup mTwView;

    private OnChooseListener mListener;

    private int mIndex;

    public QuarkChooseLanDialog(Context context) {
        super(context, R.style.CompositeSDKFullScreenDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_choose_language_layout);
        setCanceledOnTouchOutside(false);

        setOnKeyListener((DialogInterface dialog, int keyCode, KeyEvent event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                return true;
            }
            return true;
        });

        mEnView = findViewById(R.id.lan_en);
        mEnView.setOnClickListener(v -> {
            chooseButton(0);
            if (mListener != null) {
                mListener.choose(0);
            }
            dismiss();
        });
        mCnView = findViewById(R.id.lan_cn);
        mCnView.setOnClickListener(v -> {
            chooseButton(1);
            if (mListener != null) {
                mListener.choose(1);
            }
            dismiss();
        });
        mTwView = findViewById(R.id.lan_tw);
        mTwView.setOnClickListener(v -> {
            chooseButton(2);
            if (mListener != null) {
                mListener.choose(2);
            }
            dismiss();
        });

        chooseButton(mIndex);

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.8);
        getWindow().setAttributes(attributes);
    }

    public void setChooseIndex(int index) {
        mIndex = index;
    }

    public void setOnChooseListener(OnChooseListener listener) {
        mListener = listener;
    }

    private void chooseButton(int index) {
        switch (index) {
            case 0:
                mEnView.getChildAt(1).setVisibility(View.VISIBLE);
                mCnView.getChildAt(1).setVisibility(View.GONE);
                mTwView.getChildAt(1).setVisibility(View.GONE);
                break;
            case 1:
                mEnView.getChildAt(1).setVisibility(View.GONE);
                mCnView.getChildAt(1).setVisibility(View.VISIBLE);
                mTwView.getChildAt(1).setVisibility(View.GONE);
                break;
            case 2:
                mEnView.getChildAt(1).setVisibility(View.GONE);
                mCnView.getChildAt(1).setVisibility(View.GONE);
                mTwView.getChildAt(1).setVisibility(View.VISIBLE);
                break;
        }
    }
}
