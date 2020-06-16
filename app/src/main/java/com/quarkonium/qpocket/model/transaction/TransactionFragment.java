package com.quarkonium.qpocket.model.transaction;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWTransaction;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseFragment;
import com.quarkonium.qpocket.model.main.bean.TransactionLoadBean;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionViewModel;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


//交易详情fragment
public class TransactionFragment extends BaseFragment implements OnItemClickListener {


    private static class TransactionAdapter extends BaseQuickAdapter<QWTransaction, BaseViewHolder> {

        private TransactionAdapter(int layoutId, List<QWTransaction> datas) {
            super(layoutId, datas);
        }

        @Override
        protected void convert(BaseViewHolder holder, QWTransaction transaction) {
            holder.setText(R.id.transaction_address, transaction.getTxId());

            String totalToken = QWWalletUtils.getIntTokenFromWei16(transaction.getAmount());
            if (QWTokenDao.TRX_SYMBOL.equals(transaction.getToken().getSymbol())) {
                totalToken = QWWalletUtils.getIntTokenFromSun16(transaction.getAmount());
            } else if (QWTokenDao.BTC_SYMBOL.equals(transaction.getToken().getSymbol())) {
                totalToken = QWWalletUtils.getIntTokenFromCong16(transaction.getAmount());
            } else if (!QWTokenDao.ETH_SYMBOL.equals(transaction.getToken().getSymbol())
                    && !QWTokenDao.QKC_SYMBOL.equals(transaction.getToken().getSymbol())) {
                totalToken = QWWalletUtils.getIntTokenFromWei16(transaction.getAmount(), transaction.getToken().getTokenUnit());
            }

            if (QWTokenDao.TRX_SYMBOL.equals(transaction.getToken().getSymbol())
                    || TronWalletClient.isTronErc10TokenAddressValid(transaction.getToken().getAddress())) {
                //绑定trx
                bindTrxHolder(holder, transaction, totalToken);
            } else if (QWTokenDao.BTC_SYMBOL.equals(transaction.getToken().getSymbol())) {
                bindBTCHolder(holder, transaction, totalToken);
            } else {
                TextView textView = holder.getView(R.id.transaction_token);
                ImageView icon = holder.getView(R.id.transaction_state_icon);

                String direction = transaction.getDirection();
                if (Constant.QKC_TRANSACTION_STATE_PENDING.equals(direction)) {
                    //未确认状态
                    textView.setTextColor(Color.parseColor("#a0a0a0"));
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        String text = "-" + totalToken;
                        textView.setText(text);
                    }

                    icon.setImageResource(R.drawable.trans_pending);
                    holder.setText(R.id.transaction_time, Constant.DEFAULT_PENDING_TIME);
                } else if (Constant.QKC_TRANSACTION_STATE_SEND.equals(direction)) {
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        String text = "-" + totalToken;
                        textView.setText(text);
                    }

                    if (!Boolean.parseBoolean(transaction.getStatus())) {
                        //eth qkc失败
                        icon.setImageResource(R.drawable.trans_send_fail);
                        textView.setTextColor(Color.parseColor("#ff3233"));
                    } else {
                        //eth qkc成功
                        icon.setImageResource(R.drawable.trans_send);
                        textView.setTextColor(Color.parseColor("#3ea5ff"));
                    }

                    holder.setText(R.id.transaction_time, QWWalletUtils.parseTimeFor16(transaction.getTimestamp()));
                } else if (Constant.QKC_TRANSACTION_STATE_RECEIVE.equals(direction)) {
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        String text = "+" + totalToken;
                        textView.setText(text);
                    }

                    //接受状态
                    if (!Boolean.parseBoolean(transaction.getStatus())) {
                        icon.setImageResource(R.drawable.trans_receive_fail);
                        textView.setTextColor(Color.parseColor("#ff3233"));
                    } else {
                        icon.setImageResource(R.drawable.trans_receive);
                        textView.setTextColor(Color.parseColor("#03c873"));
                    }

                    holder.setText(R.id.transaction_time, QWWalletUtils.parseTimeFor16(transaction.getTimestamp()));
                }
            }
        }

        private void bindTrxHolder(BaseViewHolder holder, QWTransaction transaction, String totalToken) {
            TextView textView = holder.getView(R.id.transaction_token);
            ImageView icon = holder.getView(R.id.transaction_state_icon);

            //发送状态
            //trx 未确认状态
            if (!Boolean.parseBoolean(transaction.getStatus())) {
                if ("0".equals(totalToken)) {
                    textView.setText(totalToken);
                } else {
                    String text = "-" + totalToken;
                    textView.setText(text);
                }

                icon.setImageResource(R.drawable.trans_pending);
                textView.setTextColor(Color.parseColor("#a0a0a0"));

                holder.setText(R.id.transaction_time, Constant.DEFAULT_PENDING_TIME);
            } else {
                String direction = transaction.getDirection();
                if (Constant.QKC_TRANSACTION_STATE_SEND.equals(direction)) {
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        String text = "-" + totalToken;
                        textView.setText(text);
                    }

                    icon.setImageResource(R.drawable.trans_send);
                    textView.setTextColor(Color.parseColor("#3ea5ff"));
                } else if (Constant.QKC_TRANSACTION_STATE_RECEIVE.equals(direction)) {
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        String text = "+" + totalToken;
                        textView.setText(text);
                    }

                    icon.setImageResource(R.drawable.trans_receive);
                    textView.setTextColor(Color.parseColor("#03c873"));
                } else if (Constant.QKC_TRANSACTION_STATE_FREEZE.equals(direction)) {
                    //冻结
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        String text = "-" + totalToken;
                        textView.setText(text);
                    }

                    icon.setImageResource(R.drawable.trans_freeze);
                    textView.setTextColor(Color.parseColor("#fd9727"));
                } else if (Constant.QKC_TRANSACTION_STATE_UNFREEZE.equals(direction)) {
                    //解冻
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        String text = "+" + totalToken;
                        textView.setText(text);
                    }

                    icon.setImageResource(R.drawable.trans_unfreeze);
                    textView.setTextColor(Color.parseColor("#03c873"));
                } else if (Constant.QKC_TRANSACTION_STATE_VOTE.equals(direction)) {
                    //投票
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        String text = "-" + totalToken;
                        textView.setText(text);
                    }

                    icon.setImageResource(R.drawable.trans_vote);
                    textView.setTextColor(Color.parseColor("#03c873"));
                } else if (Constant.QKC_TRANSACTION_STATE_CONTRACT.equals(direction)) {
                    //合约
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        textView.setText(totalToken);
                    }

                    icon.setImageResource(R.drawable.trans_contact);
                    textView.setTextColor(Color.parseColor("#448aff"));
                }

                holder.setText(R.id.transaction_time, QWWalletUtils.parseFullTimeFor16(transaction.getTimestamp()));
            }
        }

        private void bindBTCHolder(BaseViewHolder holder, QWTransaction transaction, String totalToken) {
            TextView textView = holder.getView(R.id.transaction_token);
            ImageView icon = holder.getView(R.id.transaction_state_icon);

            //发送状态
            //trx 未确认状态
            if (!Boolean.parseBoolean(transaction.getStatus())) {
                if ("0".equals(totalToken)) {
                    textView.setText(totalToken);
                } else {
                    String direction = transaction.getDirection();
                    if (Constant.QKC_TRANSACTION_STATE_SEND.equals(direction)) {
                        String text = "-" + totalToken;
                        textView.setText(text);
                    } else {
                        String text = "+" + totalToken;
                        textView.setText(text);
                    }
                }

                icon.setImageResource(R.drawable.trans_pending);
                textView.setTextColor(Color.parseColor("#a0a0a0"));

                holder.setText(R.id.transaction_time, Constant.DEFAULT_PENDING_TIME);
            } else {
                String direction = transaction.getDirection();
                if (Constant.QKC_TRANSACTION_STATE_SEND.equals(direction)) {
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        String text = "-" + totalToken;
                        textView.setText(text);
                    }

                    icon.setImageResource(R.drawable.trans_send);
                    textView.setTextColor(Color.parseColor("#3ea5ff"));
                } else {
                    if ("0".equals(totalToken)) {
                        textView.setText(totalToken);
                    } else {
                        String text = "+" + totalToken;
                        textView.setText(text);
                    }

                    icon.setImageResource(R.drawable.trans_receive);
                    textView.setTextColor(Color.parseColor("#03c873"));
                }

                holder.setText(R.id.transaction_time, QWWalletUtils.parseFullTimeFor16(transaction.getTimestamp()));
            }
        }
    }

    private final static String TAG = "tag";
    private final static int TAG_ALL = 0;
    private final static int TAG_SEND = 1;
    private final static int TAG_RECEIVE = 2;
    private final static int TAG_FAIL = 3;

    private TransactionViewModel mTransactionViewModel;

    private QWWallet mQuarkWallet;
    private TransactionAdapter mBalanceAdapter;
    private int mTag;

    private QWToken mToken;

    public static TransactionFragment getInstance(int tag) {
        TransactionFragment fragment = new TransactionFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(TAG, tag);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static TransactionFragment getInstance(QWToken token, int tag) {
        TransactionFragment fragment = new TransactionFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(TAG, tag);
        bundle.putParcelable(Constant.KEY_TOKEN, token);
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
            mToken = bundle.getParcelable(Constant.KEY_TOKEN);
        }

        if (requireActivity() instanceof TransactionDetailActivity) {
            TransactionDetailActivity activity = (TransactionDetailActivity) requireActivity();
            mTransactionViewModel = new ViewModelProvider(activity, activity.mTransactionFactory).get(TransactionViewModel.class);
        } else {
            OtherTokenDetailActivity activity = (OtherTokenDetailActivity) requireActivity();
            mTransactionViewModel = new ViewModelProvider(activity, activity.mTransactionFactory).get(TransactionViewModel.class);
        }
    }

    @Override
    protected void onInitView(Bundle savedInstanceState, View rootView) {
        RecyclerView mRecyclerView = rootView.findViewById(R.id.fragment_transaction_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (mBalanceAdapter == null) {
            mBalanceAdapter = new TransactionAdapter(R.layout.holder_recycler_transaction_item, new ArrayList<>());
            mBalanceAdapter.setOnItemClickListener(this);
        }
        mRecyclerView.setAdapter(mBalanceAdapter);

        mTransactionViewModel.findDefaultWalletObserve().observe(requireActivity(), this::findWalterSuccess);
        mTransactionViewModel.transactionObserve().observe(requireActivity(), this::transactionUpdate);
    }

    private void findWalterSuccess(QWWallet wallet) {
        mQuarkWallet = wallet;
    }

    private void transactionUpdate(TransactionLoadBean bean) {
        if (getContext() == null) {
            return;
        }
        if (mBalanceAdapter == null) {
            return;
        }
        mBalanceAdapter.setNewInstance(checkList(bean.getList()));
    }

    private ArrayList<QWTransaction> checkList(Collection<QWTransaction> collection) {
        if (collection == null || getContext() == null) {
            return new ArrayList<>();
        }

        boolean isEth = mQuarkWallet.getCurrentAccount().isEth();
        boolean isTrx = mQuarkWallet.getCurrentAccount().isTRX();
        boolean isBTC = mQuarkWallet.getCurrentAccount().isAllBTC();
        ArrayList<QWTransaction> list = new ArrayList<>();
        for (QWTransaction transaction : collection) {
            QWToken token = transaction.getToken();
            if (isEth) {
                if (token.getType() != Constant.ACCOUNT_TYPE_ETH) {
                    continue;
                }
            } else if (isTrx) {
                if (!(mToken != null
                        && TronWalletClient.isTronErc10TokenAddressValid(mToken.getAddress())
                        && TextUtils.equals(mToken.getSymbol(), token.getSymbol())) && !QWTokenDao.TRX_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
            } else {
                if (mToken != null) {
                    if (!mToken.getSymbol().equals(token.getSymbol())) {
                        continue;
                    }
                } else if (!QWTokenDao.QKC_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
                //只获取当前分片下QKC的记录
                String mainChain = SharedPreferencesUtils.getCurrentChain(getContext().getApplicationContext(), mQuarkWallet.getCurrentAddress());
                String mainShard = SharedPreferencesUtils.getCurrentShard(getContext().getApplicationContext(), mQuarkWallet.getCurrentAddress());
                QWShard shard = transaction.getShard();
                QWChain chain = transaction.getChain();
                if (!mainChain.equals(chain.getChain()) || !mainShard.equals(shard.getShard())) {
                    continue;
                }
            }

            switch (mTag) {
                case TAG_ALL:
                    list.add(transaction);
                    break;
                case TAG_SEND:
                    if (Constant.QKC_TRANSACTION_STATE_SEND.equals(transaction.getDirection())) {
                        list.add(transaction);
                    }
                    break;
                case TAG_RECEIVE:
                    if (Constant.QKC_TRANSACTION_STATE_RECEIVE.equals(transaction.getDirection())) {
                        list.add(transaction);
                    }
                    break;
                case TAG_FAIL:
                    if (!Boolean.parseBoolean(transaction.getStatus()) && !Constant.QKC_TRANSACTION_STATE_PENDING.equals(transaction.getDirection())) {
                        list.add(transaction);
                    }
                    break;
            }
        }
        return list;
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, @NotNull View view, int position) {
        QWTransaction transaction = (QWTransaction) adapter.getData().get(position);
        Intent intent = new Intent(requireActivity(), TransactionDetailCostActivity.class);
        intent.putExtra(Constant.KEY_TRANSACTION, transaction);
        startActivity(intent);
    }
}
