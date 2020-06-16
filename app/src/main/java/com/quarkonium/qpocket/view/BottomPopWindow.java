package com.quarkonium.qpocket.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.quarkonium.qpocket.R;

public class BottomPopWindow implements DialogInterface.OnKeyListener {


    private Dialog mDialog;

    private TextView mButton1Text;
    private TextView mButton2Text;

    private int mTitleId;
    private int mButton1Id;
    private int mButton2Id;
    private View.OnClickListener mButton1Click;
    private View.OnClickListener mButton2Click;

    public BottomPopWindow(Activity activity) {
        View rootView = LayoutInflater.from(activity).inflate(R.layout.bottom_pop_layout, null);
        rootView.setFocusable(true);
        rootView.setFocusableInTouchMode(true);

        rootView.findViewById(R.id.cancel_account_action).setOnClickListener(v -> dismiss());


        mButton1Text = rootView.findViewById(R.id.pop_button_1);
        mButton1Text.setOnClickListener(v -> onClickButton1());
        mButton2Text = rootView.findViewById(R.id.pop_button_2);
        mButton2Text.setOnClickListener(v -> onClickButton2());

        mDialog = new Dialog(activity);
        mDialog.setCanceledOnTouchOutside(true);//触摸屏幕取消窗体
        mDialog.setCancelable(true);//按返回键取消窗体
        mDialog.setOnKeyListener(this);
        Window window = mDialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            //AndroidRuntimeException: requestFeature() must be called before adding content
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setContentView(rootView);

            WindowManager.LayoutParams p = window.getAttributes(); // 获取对话框当前的参数值
            p.height = ViewGroup.LayoutParams.WRAP_CONTENT; // 高度设置
            p.width = activity.getResources().getDisplayMetrics().widthPixels; // 宽度设置为屏幕
            window.setAttributes(p);
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            dialog.dismiss();
        }
        return false;
    }

    private void dismiss() {
        mDialog.dismiss();
    }

    public final void show() {
        mDialog.show();
    }

    private void setTitle(int res) {
        mTitleId = res;
    }

    private void setButton1Text(int res) {

    }

    private void setButton2Text(int res) {

    }

    public void setButton1TextClick(View.OnClickListener onClickListener) {
        mButton1Click = onClickListener;
    }

    public void setButton2TextClick(View.OnClickListener onClickListener) {
        mButton2Click = onClickListener;
    }

    private void onClickButton1() {
        if (mButton1Click != null) {
            mButton1Click.onClick(null);
        }
        dismiss();
    }

    private void onClickButton2() {
        if (mButton2Click != null) {
            mButton2Click.onClick(null);
        }
        dismiss();
    }
}
