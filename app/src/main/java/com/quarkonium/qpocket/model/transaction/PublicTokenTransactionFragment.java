package com.quarkonium.qpocket.model.transaction;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.table.QWPublicTokenTransaction;
import com.quarkonium.qpocket.base.BaseFragment;
import com.quarkonium.qpocket.model.transaction.bean.PublicTokenLoadBean;
import com.quarkonium.qpocket.model.transaction.viewmodel.PublicSaleDetailViewModel;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


//交易详情fragment
public class PublicTokenTransactionFragment extends BaseFragment implements OnItemClickListener {


    private static class TransactionAdapter extends BaseQuickAdapter<QWPublicTokenTransaction, BaseViewHolder> {

        private TransactionAdapter(int layoutId, List<QWPublicTokenTransaction> datas) {
            super(layoutId, datas);
        }

        @Override
        protected void convert(BaseViewHolder holder, QWPublicTokenTransaction transaction) {
            ImageView icon = holder.getView(R.id.transaction_state_icon);

            String totalToken = QWWalletUtils.getIntTokenFromWei16(transaction.getAmount());
            String direction = transaction.getDirection();
            if (Constant.TOKEN_TRANSACTION_STATE_BUY.equals(direction)) {
                TextView textView = holder.getView(R.id.transaction_token);

                if ("0".equals(totalToken)) {
                    holder.setText(R.id.transaction_token, totalToken);
                } else {
                    holder.setText(R.id.transaction_token, totalToken);
                }

                if (!Boolean.parseBoolean(transaction.getStatus())) {
                    icon.setImageResource(R.drawable.token_trans_buy_fail);
                    textView.setTextColor(Color.parseColor("#ff3233"));
                } else {
                    icon.setImageResource(R.drawable.token_trans_buy);
                    textView.setTextColor(Color.parseColor("#03c873"));
                }
                holder.setText(R.id.transaction_time, QWWalletUtils.parseTimeFor16(transaction.getTimestamp()));
            } else if (Constant.TOKEN_TRANSACTION_STATE_SEND.equals(direction)) {
                TextView textView = holder.getView(R.id.transaction_token);
                if (!Boolean.parseBoolean(transaction.getStatus())) {
                    icon.setImageResource(R.drawable.token_trans_receive_fail);
                    textView.setTextColor(Color.parseColor("#ff3233"));
                } else {
                    icon.setImageResource(R.drawable.token_trans_receive);
                    textView.setTextColor(Color.parseColor("#3ea5ff"));
                }

                holder.setText(R.id.transaction_token, totalToken);

                holder.setText(R.id.transaction_time, QWWalletUtils.parseTimeFor16(transaction.getTimestamp()));
            }

            holder.setText(R.id.transaction_address, transaction.getTxId());
        }
    }

    private final static String TAG = "tag";
    private final static int TAG_ALL = 0;
    private final static int TAG_BUY = 1;
    private final static int TAG_RECEIVE = 2;
    private final static int TAG_FAIL = 3;

    private PublicSaleDetailViewModel mPublicSaleFragmentViewModel;

    private TransactionAdapter mBalanceAdapter;
    private String mAddress;
    private int mTag;

    public static PublicTokenTransactionFragment getInstance(int tag, String tokenAddress) {
        PublicTokenTransactionFragment fragment = new PublicTokenTransactionFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(TAG, tag);
        bundle.putString(Constant.KEY_TOKEN_ADDRESS, tokenAddress);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_wallet_transaction;
    }

    @Override
    public int getFragmentTitle() {
        return 0;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mTag = bundle.getInt(TAG);
            mAddress = bundle.getString(Constant.KEY_TOKEN_ADDRESS);
        }

        PublicSaleDetailActivity activity = (PublicSaleDetailActivity) requireActivity();
        mPublicSaleFragmentViewModel = new ViewModelProvider(activity, activity.mPublicSaleFragmentFactory).get(PublicSaleDetailViewModel.class);
    }

    @Override
    protected void onInitView(Bundle savedInstanceState, View rootView) {
        if (mBalanceAdapter == null) {
            mBalanceAdapter = new TransactionAdapter(R.layout.holder_recycler_transaction_item, new ArrayList<>());
            mBalanceAdapter.setOnItemClickListener(this);
        }
        RecyclerView mRecyclerView = rootView.findViewById(R.id.fragment_transaction_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.setAdapter(mBalanceAdapter);

        mPublicSaleFragmentViewModel.transactionObserve().observe(requireActivity(), this::onTransactionSuccess);
    }

    private void onTransactionSuccess(PublicTokenLoadBean bean) {
        if (mBalanceAdapter == null) {
            return;
        }
        mBalanceAdapter.setNewInstance(checkList(bean.getList()));
    }

    private ArrayList<QWPublicTokenTransaction> checkList(List<QWPublicTokenTransaction> collection) {
        if (collection == null || getContext() == null) {
            return new ArrayList<>();
        }
        ArrayList<QWPublicTokenTransaction> list = new ArrayList<>();
        for (QWPublicTokenTransaction transaction : collection) {
            switch (mTag) {
                case TAG_ALL:
                    list.add(transaction);
                    break;
                case TAG_BUY:
                    if (Constant.TOKEN_TRANSACTION_STATE_BUY.equals(transaction.getDirection()) && Boolean.parseBoolean(transaction.getStatus())) {
                        list.add(transaction);
                    }
                    break;
                case TAG_RECEIVE:
                    if (Constant.TOKEN_TRANSACTION_STATE_SEND.equals(transaction.getDirection()) && Boolean.parseBoolean(transaction.getStatus())) {
                        list.add(transaction);
                    }
                    break;
                case TAG_FAIL:
                    if (!Boolean.parseBoolean(transaction.getStatus())) {
                        list.add(transaction);
                    }
                    break;
            }
        }
        return list;
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, @NotNull View view, int position) {
        QWPublicTokenTransaction transaction = (QWPublicTokenTransaction) adapter.getData().get(position);
        Intent intent = new Intent(requireActivity(), TransactionPublicDetailCostActivity.class);
        intent.putExtra(Constant.KEY_TRANSACTION, transaction);
        startActivity(intent);
    }
}
