package com.quarkonium.qpocket.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.util.QWWalletUtils;

import java.util.ArrayList;
import java.util.Collections;

public class WheelBalancePopWindow implements DialogInterface.OnKeyListener {

    private WheelView.OnItemSelectListener mListener;
    private Dialog mDialog;

    private ArrayList<QWBalance> mList;
    private ArrayList<String> mItems;
    private WheelView mWheelView;
    private TextView mBalanceView;
    private TextView mTitleTip;
    private QWToken mSendToken;

    public WheelBalancePopWindow(Activity activity) {
        View rootView = LayoutInflater.from(activity).inflate(R.layout.wheel_balance_pop_layout, null);
        rootView.setFocusable(true);
        rootView.setFocusableInTouchMode(true);

        mBalanceView = rootView.findViewById(R.id.balance_value);
        mTitleTip = rootView.findViewById(R.id.balance_pop_tip);

        mWheelView = rootView.findViewById(R.id.pop_wheel_view);
        mWheelView.setLineSpaceMultiplier(2.5f);
        mWheelView.setTextSize(R.dimen.wheel_text_size);
        mWheelView.setTextColor(Color.parseColor("#55212121"), Color.parseColor("#212121"));
        mWheelView.setCycleDisable(true);
        mWheelView.setUseWeight(true);
        mWheelView.setTextSizeAutoFit(true);
        mWheelView.setTextPadding(-1);
        mWheelView.setOffset(2);
        mWheelView.setDividerColor(Color.parseColor("#ebebeb"));
        mWheelView.setOnItemSelectListener(this::onItemSelected);

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

        rootView.findViewById(R.id.negative_btn).setOnClickListener(v -> dismiss());
        rootView.findViewById(R.id.positive_btn).setOnClickListener(v -> onSwitch());
    }

    public void setOnNumberPickListener(WheelView.OnItemSelectListener listener) {
        mListener = listener;
    }

    public void setItem(ArrayList<QWBalance> item) {
        mList = item;
        if (item.size() > 1) {
            //排序
            Collections.sort(mList, (QWBalance p1, QWBalance p2) -> {
                /*
                 * int compare(Person p1, Person p2) 返回一个基本类型的整型，
                 * 返回负数表示：p1 小于p2，
                 * 返回0 表示：p1和p2相等，
                 * 返回正数表示：p1大于p2
                 */
                int value = Numeric.toBigInt(p2.getBalance()).compareTo(Numeric.toBigInt(p1.getBalance()));
                if (value == 0) {
                    value = Numeric.toBigInt(p1.getChain().getChain()).compareTo(Numeric.toBigInt(p2.getChain().getChain()));
                    if (value == 0) {
                        value = Numeric.toBigInt(p1.getQWShard().getShard()).compareTo(Numeric.toBigInt(p2.getQWShard().getShard()));
                    }
                }
                return value;
            });
        }
        mItems = new ArrayList<>();
        for (QWBalance balance : item) {
            String chain = balance.getChain().getChain();
            String shard = balance.getQWShard().getShard();
            String csStr = mBalanceView.getContext().getString(R.string.merge_chain_shard);
            mItems.add(String.format(csStr, Numeric.toBigInt(chain).toString(), Numeric.toBigInt(shard).toString()));
        }

        if (!item.isEmpty()) {
            String token = mSendToken == null
                    ? QWWalletUtils.getIntTokenFromWei16(mList.get(0).getBalance())
                    : QWWalletUtils.getIntTokenFromWei16(mList.get(0).getBalance(), mSendToken.getTokenUnit());
            String tokens = mSendToken == null ? (token + " " + QWTokenDao.QKC_NAME) : (token + " " + mSendToken.getSymbol().toUpperCase());
            mBalanceView.setText(tokens);
        }
    }

    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            mTitleTip.setText(title);
        }
    }

    public void setToken(QWToken token) {
        mSendToken = token;
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            dialog.dismiss();
        }
        return false;
    }

    private void onItemSelected(int index) {
        if (index >= 0 && index < mList.size()) {
            String token = mSendToken == null
                    ? QWWalletUtils.getIntTokenFromWei16(mList.get(index).getBalance())
                    : QWWalletUtils.getIntTokenFromWei16(mList.get(index).getBalance(), mSendToken.getTokenUnit());
            String tokens = mSendToken == null ? (token + " " + QWTokenDao.QKC_NAME) : (token + " " + mSendToken.getSymbol().toUpperCase());
            mBalanceView.setText(tokens);
        }
    }

    private void dismiss() {
        mDialog.dismiss();
    }

    public final void show() {
        mWheelView.setItems(mItems);
        mDialog.show();
    }

    private void onSwitch() {
        if (mListener != null) {
            int index = mWheelView.getSelectedIndex();
            mListener.onSelected(index);
        }
        mDialog.dismiss();
    }
}
