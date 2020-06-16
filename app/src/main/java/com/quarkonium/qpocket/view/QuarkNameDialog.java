package com.quarkonium.qpocket.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.quarkonium.qpocket.R;

public class QuarkNameDialog extends Dialog {
    private Activity mActivity;
    private EditText mPassword;
    private View.OnClickListener mListener;

    private String mText;

    private View mOkView;

    public QuarkNameDialog(Activity context) {
        super(context, R.style.CompositeSDKFullScreenResizeDialog);
        mActivity = context;
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_name_layout);
        setCanceledOnTouchOutside(false);

        mOkView = findViewById(R.id.positive_btn);
        mOkView.setOnClickListener((v) -> {
            mListener.onClick(v);
        });

        View cancel = findViewById(R.id.negative_btn);
        cancel.setOnClickListener((v) -> dismiss());

        mPassword = findViewById(R.id.personal_nick_name);
        mPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    mOkView.setEnabled(false);
                } else {
                    mOkView.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mPassword.setText(mText);
        mPassword.requestFocus();
    }

    private void hideSoftwareKeyboard(EditText input) {
        try {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dismiss() {
        hideSoftwareKeyboard(mPassword);
        super.dismiss();
    }

    public String getText() {
        return mPassword.getText().toString().trim();
    }

    public void setText(String text) {
        mText = text;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null) {
                dismiss();
            }
        }
        return super.onTouchEvent(event);
    }
}
