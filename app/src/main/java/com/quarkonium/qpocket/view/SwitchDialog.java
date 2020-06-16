package com.quarkonium.qpocket.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SwitchDialog extends Dialog implements OnItemClickListener {
    public interface OnSwitchItemClickListener {
        void onItemClick(String name, int position);
    }

    private class SwitchAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

        private SwitchAdapter(int layoutResId, @Nullable List<String> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, String item) {
            helper.setText(R.id.name, item);

            View selected = helper.getView(R.id.selected);
            if (TextUtils.equals(item, mSelectedName)) {
                selected.setVisibility(View.VISIBLE);
            } else {
                selected.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onItemClick(@NotNull BaseQuickAdapter adapter, @NotNull View view, int position) {
        String name = mList.get(position);
        if (mOnListener != null) {
            mOnListener.onItemClick(name, position);
        }
    }

    private OnSwitchItemClickListener mOnListener;
    private List<String> mList = new ArrayList<>();
    private String mSelectedName;

    private View mProgressView;

    public SwitchDialog(Context context) {
        super(context, R.style.CompositeSDKFullScreenDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.switch_pop_layout);
        setCanceledOnTouchOutside(true);

        setOnKeyListener((DialogInterface dialog, int keyCode, KeyEvent event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                return true;
            }
            return true;
        });

        RecyclerView mRecyclerView = findViewById(R.id.pop_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        SwitchAdapter mPopAdapter = new SwitchAdapter(R.layout.switch_pop_menu_item, mList);
        mPopAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(mPopAdapter);

        mProgressView = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressView, UiUtils.dpToPixel(4));
    }

    public void setData(String[] data) {
        mList = Arrays.asList(data);
    }

    public void setSelected(String name) {
        mSelectedName = name;
    }

    public void setOnListener(OnSwitchItemClickListener listener) {
        mOnListener = listener;
    }

    public void showProgress() {
        setCanceledOnTouchOutside(false);
        mProgressView.setVisibility(View.VISIBLE);
    }
}
