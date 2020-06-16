package com.quarkonium.qpocket.model.transaction;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.model.transaction.bean.MergeBean;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionModelFactory;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionViewModel;
import com.quarkonium.qpocket.rx.SendFinishEvent;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.MergeEditPopWindow;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class MergeActivity extends BaseActivity {

    private class MergeAdapter extends BaseQuickAdapter<MergeBean, BaseViewHolder> {
        private String mMainChain;
        private String mMainShard;

        MergeAdapter(int layoutResId, @Nullable List<MergeBean> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder holder, MergeBean item) {
            QWBalance balance = item.balance;
            holder.setText(R.id.merge_to, String.format(getString(R.string.merge_chain_shard), Numeric.toBigInt(mMainChain), Numeric.toBigInt(mMainShard)));
            holder.setText(R.id.merge_from, String.format(getString(R.string.merge_chain_shard),
                    Numeric.toBigInt(balance.getChain().getChain()),
                    Numeric.toBigInt(balance.getQWShard().getShard())));

            //总余额
            String symbol = mToken == null ? QWTokenDao.QKC_NAME : mToken.getSymbol().toUpperCase();

            String gasPriceCount = mToken == null ?
                    QWWalletUtils.getIntTokenNotScaleFromWei16(balance.getBalance()) :
                    QWWalletUtils.getIntTokenNotScaleFromWei16(balance.getBalance(), mToken.getTokenUnit());
            String gasPrice = String.format(getString(R.string.merge_gasPrice), gasPriceCount) + symbol;
            holder.setText(R.id.merge_gas_price, gasPrice);

            TextView sendTokenPrice = holder.getView(R.id.merge_send_token_price);
            String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), symbol, gasPriceCount);
            sendTokenPrice.setText(priceStr);

            //矿工费用
            BigInteger gasCost = item.gasPrice.multiply(item.gasLimit);
            String gasTokenSymbol = getTokenSymbol(item.gasTokenId, item.gasTokenList);
            String gasLimitCount = QWWalletUtils.getIntTokenFromWei10(gasCost.toString(), true, Constant.QKC_DECIMAL_NUMBER);
            String gasLimit = String.format(getString(R.string.merge_gasLimit), gasLimitCount) + gasTokenSymbol;
            holder.setText(R.id.merge_gas_limit, gasLimit);

            TextView txFreePrice = holder.getView(R.id.merge_send_free_price);
            priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), symbol, gasLimitCount);
            txFreePrice.setText(priceStr);

            //可转账金额
            String mainToken = mToken == null ?
                    QWWalletUtils.getIntTokenNotScaleFromWei16(item.amount.toString(16)) :
                    QWWalletUtils.getIntTokenNotScaleFromWei16(item.amount.toString(16), mToken.getTokenUnit());
            String totalToken = mainToken + " " + symbol;
            holder.setText(R.id.merge_amount, totalToken);

            TextView amountPrice = holder.getView(R.id.merge_amount_price);
            priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), symbol, mainToken);
            amountPrice.setText(priceStr);

            ImageView editView = holder.getView(R.id.merge_edit);
            ImageView imageView = holder.getView(R.id.merge_selected);
            if (item.isSelected) {
                imageView.setImageResource(R.drawable.check_box_selected);
                editView.setImageResource(R.drawable.merge_edit_enable);
                editView.setEnabled(true);
            } else {
                imageView.setImageResource(R.drawable.check_box_unselected);
                editView.setImageResource(R.drawable.merge_edit);
                editView.setEnabled(false);
            }
            imageView.setOnClickListener(v -> onCheck(holder.getAdapterPosition()));
            editView.setOnClickListener(v -> onEdit(holder.getAdapterPosition()));

            //用当前可转账的金额小于等于0时，不作转账
            if (BigInteger.ZERO.compareTo(item.amount) >= 0) {
                imageView.setVisibility(View.GONE);
                editView.setVisibility(View.GONE);
                holder.setTextColor(R.id.merge_to_title, Color.parseColor("#a0a0a0"));
                holder.setTextColor(R.id.merge_from_title, Color.parseColor("#a0a0a0"));
                holder.setTextColor(R.id.merge_amount, Color.parseColor("#EC5A4B"));
                item.isSelected = false;
            } else {
                imageView.setVisibility(View.VISIBLE);
                editView.setVisibility(View.VISIBLE);
                holder.setTextColor(R.id.merge_to_title, Color.parseColor("#03c873"));
                holder.setTextColor(R.id.merge_from_title, Color.parseColor("#3ea5ff"));
                holder.setTextColor(R.id.merge_amount, Color.parseColor("#212121"));
            }
        }

        private String getTokenSymbol(String tokenId, ArrayList<QWBalance> list) {
            if (list != null && !list.isEmpty()) {
                for (QWBalance balance : list) {
                    if (TextUtils.equals(tokenId, balance.getQWToken().getAddress())) {
                        return balance.getQWToken().getSymbol().toUpperCase();
                    }
                }
            }
            return QWTokenDao.QKC_NAME;
        }

        private void setMainShard(String chain, String shard) {
            mMainChain = chain;
            mMainShard = shard;
        }

        private void onCheck(int position) {
            MergeBean bean = getData().get(position);
            bean.isSelected = !bean.isSelected;
            notifyItemChanged(position);

            changeTotalCount();
        }

        private void onEdit(int position) {
            MergeBean bean = getData().get(position);
            String title = String.format(getString(R.string.merge_edit_chain_title),
                    Numeric.toBigInt(bean.balance.getChain().getChain()),
                    Numeric.toBigInt(bean.balance.getQWShard().getShard()),
                    Numeric.toBigInt(mMainChain), Numeric.toBigInt(mMainShard));
            MergeEditPopWindow picker = new MergeEditPopWindow();
            picker.setData(bean, title, mToken != null ? mToken.getSymbol().toUpperCase() : QWTokenDao.QKC_NAME);
            picker.setOnClickListener((newBean) -> {
                getData().remove(position);
                getData().add(position, newBean);
                notifyItemChanged(position);

                changeTotalCount();
            });
            picker.show(getSupportFragmentManager(), "MergeEditPopWindow");
        }

        private BigInteger getTotalAmount() {
            BigInteger totalCost = BigInteger.ZERO;
            for (MergeBean bean : getData()) {
                if (bean.isSelected) {
                    totalCost = totalCost.add(bean.amount);
                }
            }
            return totalCost;
        }

        private ArrayList<MergeBean> getSelectedBalance() {
            ArrayList<MergeBean> list = new ArrayList<>();
            for (MergeBean bean : getData()) {
                if (bean.isSelected) {
                    list.add(bean);
                }
            }
            return list;
        }
    }

    public static void startMergeActivity(Activity activity, QWToken token) {
        Intent intent = new Intent(activity, MergeActivity.class);
        if (token != null) {
            intent.putExtra(Constant.KEY_TOKEN, token);
        }
        activity.startActivityForResult(intent, Constant.REQUEST_CODE_QCK_MERGE);
    }

    @Inject
    TransactionModelFactory mTransactionFactory;
    private TransactionViewModel mTransactionViewModel;

    private QWWallet mDefaultWallet;
    private MergeAdapter mMergeAdapter;

    private View mMergeView;
    private TextView mTotalBalanceView;
    private TextView mTotalPriceView;

    private View mProgressView;

    private BigDecimal mAmountGWei;

    private String mToAddress;

    private QWToken mToken;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_merge_layoutl;
    }

    @Override
    public int getActivityTitle() {
        return R.string.merge_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mToken = getIntent().getParcelableExtra(Constant.KEY_TOKEN);
        mToAddress = getIntent().getStringExtra(Constant.WALLET_ADDRESS);

        mTopBarView.setTitle(R.string.merge_title);

        mProgressView = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressView, UiUtils.dpToPixel(3));

        mMergeView = findViewById(R.id.public_sale_merge);
        mMergeView.setOnClickListener(v -> onMergeCheck());
        mMergeView.setEnabled(false);

        mTotalBalanceView = findViewById(R.id.merge_total_balance);
        mTotalPriceView = findViewById(R.id.merge_total_balance_price);

        RecyclerView recyclerView = findViewById(R.id.merge_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mMergeAdapter = new MergeAdapter(R.layout.holder_recycler_merge_item, new ArrayList<>());
        recyclerView.setAdapter(mMergeAdapter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);

        String balance = getIntent().getStringExtra(Constant.KEY_BALANCE);
        if (!TextUtils.isEmpty(balance)) {
            mAmountGWei = new BigDecimal(balance);
        }

        //获取当前主钱包
        mTransactionViewModel = new ViewModelProvider(this, mTransactionFactory)
                .get(TransactionViewModel.class);
        mTransactionViewModel.findDefaultWalletObserve().observe(this, this::findWalterSuccess);

        mTransactionViewModel.mergeStateObserve().observe(this, this::onMegeState);

        mTransactionViewModel.feachMergeData().observe(this, this::feachMergeData);
        mTransactionViewModel.feachMergeFinsh().observe(this, this::feachMergeFinish);

        mTransactionViewModel.findWallet();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void findWalterSuccess(QWWallet wallet) {
        mDefaultWallet = wallet;

        String mainChain = TextUtils.isEmpty(mToAddress)
                ? SharedPreferencesUtils.getCurrentChain(getApplicationContext(), mDefaultWallet.getCurrentAddress())
                : Numeric.toHexStringWithPrefix(QWWalletUtils.getChainByAddress(this, mToAddress));
        String mainShard = TextUtils.isEmpty(mToAddress)
                ? SharedPreferencesUtils.getCurrentShard(getApplicationContext(), mDefaultWallet.getCurrentAddress())
                : Numeric.toHexStringWithPrefix(QWWalletUtils.getShardByAddress(this, mToAddress, Numeric.toBigInt(mainChain)));
        mMergeAdapter.setMainShard(mainChain, mainShard);
        String symbol = mToken == null ? QWTokenDao.QKC_SYMBOL : mToken.getSymbol();

        showProgress(true);
        mTransactionViewModel.feachMergeData(wallet, symbol, mainChain, mainShard, mAmountGWei);
    }

    private void feachMergeData(List<MergeBean> list) {
        mMergeAdapter.setNewInstance(list);
        mMergeAdapter.notifyDataSetChanged();
        changeTotalCount();
    }

    private void feachMergeFinish(boolean value) {
        showProgress(false);
    }

    private void changeTotalCount() {
        BigInteger integer = mMergeAdapter.getTotalAmount();
        if (integer.compareTo(BigInteger.ZERO) <= 0) {
            mMergeView.setEnabled(false);
        } else {
            mMergeView.setEnabled(true);
        }

        String symbol = mToken == null ? QWTokenDao.QKC_NAME : mToken.getSymbol().toUpperCase();
        String count = mToken == null ?
                QWWalletUtils.getIntTokenFromWei10(integer.toString()) :
                QWWalletUtils.getIntTokenFromWei10(integer.toString(), mToken.getTokenUnit());
        String countStr = count + " " + symbol;
        mTotalBalanceView.setText(countStr);

        String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), symbol, count);
        mTotalPriceView.setText(priceStr);
    }

    private void onMergeCheck() {
        if (!ConnectionUtil.isInternetConnection(this)) {
            MyToast.showSingleToastShort(this, R.string.network_error);
            return;
        }

        ArrayList<MergeBean> list = mMergeAdapter.getSelectedBalance();
        if (mDefaultWallet.isWatch() && list.size() > 20) {
            //14条限制
            MyToast.showSingleToastShort(this, R.string.qkc_merge_size_error);
            return;
        }

        //校验手续费是否足够
        //校验native token充值金额是否足够
        ArrayList<MergeBean> errorList = new ArrayList<>();
        ArrayList<MergeBean> gasBalanceList = new ArrayList<>();
        for (MergeBean bean : list) {
            for (QWBalance balance : bean.gasTokenList) {
                if (TextUtils.equals(balance.getQWToken().getAddress(), bean.gasTokenId)) {
                    BigInteger qwBalance = Numeric.toBigInt(balance.getBalance());
                    BigInteger mGasCost = bean.gasPrice.multiply(bean.gasLimit);
                    if (qwBalance.compareTo(mGasCost) < 0) {
                        //不够手续费
                        errorList.add(bean);
                    } else if (!TextUtils.equals(QWTokenDao.TQKC_ADDRESS, bean.gasTokenId)) {
                        //非QKC作为手续费，需要判定充值金额是否还够
                        //充值金额
                        BigDecimal reserveBalance = bean.reserveTokenBalance;
                        //兑换比例
                        BigDecimal percentage = bean.refundPercentage;
                        //转换成qkc手续费需要消耗多少
                        BigDecimal qkcCost = new BigDecimal(mGasCost).divide(percentage, Constant.QKC_DECIMAL_NUMBER, RoundingMode.CEILING);
                        if (reserveBalance.compareTo(qkcCost) < 0) {
                            gasBalanceList.add(bean);
                        }
                    }
                    break;
                }
            }
        }
        //手续费不够
        if (!errorList.isEmpty()) {
            //有分片的gasToken余额不足
            StringBuilder fail = new StringBuilder();
            for (int i = 0, size = errorList.size(); i < size; i++) {
                MergeBean bean = errorList.get(i);
                if (bean.balance != null) {
                    String chain = bean.balance.getChain().getChain();
                    String shard = bean.balance.getQWShard().getShard();
                    String value = String.format(getString(R.string.merge_chain_shard), Numeric.toBigInt(chain).toString(), Numeric.toBigInt(shard).toString());
                    if (i == size - 1) {
                        fail.append(value);
                    } else {
                        fail.append(value).append("; ");
                    }
                }
            }
            QuarkSDKDialog dialog = new QuarkSDKDialog(this);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setMessage(String.format(getString(R.string.merge_gas_token_not_support), fail.toString()));
            dialog.setPositiveBtn(R.string.ok, (v) -> dialog.dismiss());
            dialog.show();
            return;
        }
        //充值金额不够
        if (!gasBalanceList.isEmpty()) {
            //不是QKC,需要判定该gas token是否支持手续费
            StringBuilder fail = new StringBuilder();
            for (int i = 0, size = gasBalanceList.size(); i < size; i++) {
                MergeBean bean = gasBalanceList.get(i);
                if (bean.balance != null) {
                    String chain = bean.balance.getChain().getChain();
                    String shard = bean.balance.getQWShard().getShard();
                    String value = String.format(getString(R.string.merge_chain_shard), Numeric.toBigInt(chain).toString(), Numeric.toBigInt(shard).toString());
                    if (i == size - 1) {
                        fail.append(value);
                    } else {
                        fail.append(value).append("; ");
                    }
                }
            }
            QuarkSDKDialog dialog = new QuarkSDKDialog(this);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setMessage(String.format(getString(R.string.merge_gas_token_balance_not_enough), fail.toString()));
            dialog.setPositiveBtn(R.string.ok, (v) -> dialog.dismiss());
            dialog.show();
            return;
        }
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mDefaultWallet,
                new SystemUtils.OnCheckPassWordListenerImp() {
                    @Override
                    public void onPasswordSuccess(String password) {
                        onMerge(password);
                    }
                });
    }

    private void onMerge(String password) {
        showProgress(true);
        ArrayList<MergeBean> list = mMergeAdapter.getSelectedBalance();
        String toAddress = TextUtils.isEmpty(mToAddress) ? mDefaultWallet.getCurrentShareAddress() : mToAddress;
        mTransactionViewModel.mergeTransaction(toAddress, list, password, mToken == null ? QWTokenDao.TQKC_ADDRESS : mToken.getAddress());
        UmengStatistics.mergeSubmitToken(getApplicationContext(), mToken == null ? "qkc" : mToken.getSymbol());
    }

    private void onMegeState(ArrayList<MergeBean> result) {
        if (result == null || result.isEmpty()) {
            Intent intent = getIntent();
            intent.putExtra(Constant.KEY_RESULT, true);
            setResult(Activity.RESULT_OK, intent);
            finish();
            return;
        }

        StringBuilder fail = new StringBuilder();
        int size = result.size();
        for (int i = 0; i < size; i++) {
            MergeBean bean = result.get(i);
            if (bean.balance != null) {
                String chain = bean.balance.getChain().getChain();
                String shard = bean.balance.getQWShard().getShard();
                String value = String.format(getString(R.string.merge_chain_shard), Numeric.toBigInt(chain).toString(), Numeric.toBigInt(shard).toString());
                if (i == size - 1) {
                    fail.append(value);
                } else {
                    fail.append(value).append("; ");
                }
            }
        }
        MyToast.showSingleToastShort(this, String.format(getString(R.string.merge_send_fail), fail.toString()));

        showProgress(false);
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressView.setVisibility(View.VISIBLE);
        } else {
            mProgressView.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void rxBusEventChange(SendFinishEvent event) {
        if (event.isState()) {
            Intent intent = getIntent();
            intent.putExtra(Constant.KEY_RESULT, true);
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }
}
