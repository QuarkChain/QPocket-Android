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
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.util.GalleryLayoutManager;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.R;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.annotations.Nullable;

public class WheelPopWindow implements DialogInterface.OnKeyListener {

    private class WheelAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
        private int mTag;

        WheelAdapter(int tag, int layoutResId, @Nullable List<String> data) {
            super(layoutResId, data);
            mTag = tag;
        }

        @Override
        protected void convert(BaseViewHolder holder, String item) {
            TextView textView = holder.getView(R.id.wheel_value);
            textView.setText(item);
            textView.setOnClickListener(v -> {
                int position = mChainRecyclerView.getChildAdapterPosition(holder.itemView);
                if (mTag == 0) {
                    mChainRecyclerView.smoothScrollToPosition(position);
                } else {
                    mShardRecyclerView.smoothScrollToPosition(position);
                }
            });
        }
    }

    //滑动过程中的缩放
    public static class Transformer implements GalleryLayoutManager.ItemTransformer {
        @Override
        public void transformItem(GalleryLayoutManager layoutManager, View item, float fraction) {
            //以圆心进行缩放
            item.setPivotX(item.getWidth() / 2.0f);
            item.setPivotY(item.getHeight() / 2.0f);
            float scale = 1 - 0.3f * Math.abs(fraction);
            item.setScaleX(scale);
            item.setScaleY(scale);
        }
    }

    private static class ChainItemSelectedListener implements GalleryLayoutManager.OnItemSelectedListener {

        private SoftReference<WheelPopWindow> mPopWindow;

        ChainItemSelectedListener(WheelPopWindow popWindow) {
            mPopWindow = new SoftReference<>(popWindow);
        }

        @Override
        public void onItemSelected(RecyclerView recyclerView, View item, int position, boolean isScrolling) {
            if (mPopWindow != null && mPopWindow.get() != null) {
                WheelPopWindow popWindow = mPopWindow.get();
                popWindow.switchShard(position);
            }
        }
    }

    private static class ShardItemSelectedListener implements GalleryLayoutManager.OnItemSelectedListener {

        private SoftReference<WheelPopWindow> mPopWindow;

        ShardItemSelectedListener(WheelPopWindow popWindow) {
            mPopWindow = new SoftReference<>(popWindow);
        }

        @Override
        public void onItemSelected(RecyclerView recyclerView, View item, int position, boolean isScrolling) {
            if (mPopWindow != null && mPopWindow.get() != null) {
                WheelPopWindow popWindow = mPopWindow.get();
                popWindow.mCurrShard = position;
            }
        }
    }

    public interface OnItemSelectListener {
        void onSelected(int chain, int shard);
    }

    private OnItemSelectListener mListener;
    private Dialog mDialog;


    private int mChain;
    private int mShard;
    private int mCurrChain;
    private int mCurrShard;
    private RecyclerView mChainRecyclerView;
    private GalleryLayoutManager mChainManager;

    private List<String> mShardTotalSizeList;
    private RecyclerView mShardRecyclerView;
    private WheelAdapter mShardAdapter;
    private List<String> mShardList;
    private GalleryLayoutManager mShardManager;

    public WheelPopWindow(Activity activity) {
        View rootView = LayoutInflater.from(activity).inflate(R.layout.wheel_pop_layout, null);
        rootView.setFocusable(true);
        rootView.setFocusableInTouchMode(true);
        rootView.findViewById(R.id.done_action).setOnClickListener(v -> onSwitch());

        String totalChain = SharedPreferencesUtils.getTotalChainCount(activity.getApplicationContext());
        mChainRecyclerView = rootView.findViewById(R.id.main_chain_rv);
        mChainManager = new GalleryLayoutManager(GalleryLayoutManager.HORIZONTAL);
        mChainManager.attach(mChainRecyclerView);
        //设置滑动缩放效果
        mChainManager.setItemTransformer(new Transformer());
        //设置数据
        ArrayList<String> chainList = new ArrayList<>();
        int chainSize = Numeric.toBigInt(totalChain).intValue();
        for (int i = 0; i < chainSize; i++) {
            //滑动到某一项的position
            chainList.add(i + "");
        }
        WheelAdapter mChainAdapter = new WheelAdapter(0, R.layout.holder_recycler_chain_wheel_item, chainList);
        mChainRecyclerView.setAdapter(mChainAdapter);


        mShardTotalSizeList = SharedPreferencesUtils.getTotalSharedSizes(activity.getApplicationContext());
        mShardRecyclerView = rootView.findViewById(R.id.main_shard_rv);
        mShardManager = new GalleryLayoutManager(GalleryLayoutManager.HORIZONTAL);
        mShardManager.attach(mShardRecyclerView);
        mShardManager.setItemTransformer(new Transformer());
        mShardManager.setOnItemSelectedListener(new ShardItemSelectedListener(this));
        //设置默认数据
        mShardList = new ArrayList<>();
        mShardList.add("0");
        mShardAdapter = new WheelAdapter(1, R.layout.holder_recycler_chain_wheel_item, mShardList);
        mShardRecyclerView.setAdapter(mShardAdapter);

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
            p.width = activity.getResources().getDisplayMetrics().widthPixels; // 宽度设置为屏幕
            window.setAttributes(p);
        }
    }

    private void switchShard(int p) {
        //滑动到某一项的position
        mCurrChain = p;
        if (p >= mShardTotalSizeList.size() || p < 0) {
            return;
        }

        mShardList.clear();
        String size = mShardTotalSizeList.get(p);
        int chardSize = Numeric.toBigInt(size).intValue();
        for (int i = 0; i < chardSize; i++) {
            mShardList.add(i + "");
        }
        mShardAdapter.setNewInstance(mShardList);
        mShardManager.scrollToPosition(0);
    }

    public void setOnNumberPickListener(OnItemSelectListener listener) {
        mListener = listener;
    }

    public void setSelected(int chain, int shard) {
        mCurrChain = mChain = chain;
        mCurrShard = mShard = shard;
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            dialog.dismiss();
        }
        return false;
    }

    public final void show() {
        mDialog.show();
        mChainRecyclerView.scrollToPosition(mChain);

        if (mChain < mShardTotalSizeList.size() && mChain >= 0) {
            mShardList.clear();
            String size = mShardTotalSizeList.get(mChain);
            int chardSize = Numeric.toBigInt(size).intValue();
            for (int i = 0; i < chardSize; i++) {
                mShardList.add(i + "");
            }
        }
        mShardAdapter.setNewInstance(mShardList);
        mShardManager.scrollToPosition(mShard);

        mShardRecyclerView.postDelayed(() -> mChainManager.setOnItemSelectedListener(new ChainItemSelectedListener(this)), 100);
    }

    private void onSwitch() {
        if (mListener != null) {
            mListener.onSelected(mCurrChain, mCurrShard);
        }
        mDialog.dismiss();
    }
}
