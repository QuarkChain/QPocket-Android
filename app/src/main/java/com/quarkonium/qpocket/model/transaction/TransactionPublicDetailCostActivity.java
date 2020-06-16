package com.quarkonium.qpocket.model.transaction;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWPublicTokenTransactionDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWPublicTokenTransaction;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionModelFactory;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionViewModel;
import com.quarkonium.qpocket.rx.RxBus;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.view.JustifyTextView;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.R;

import java.math.BigInteger;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * 交易详情列表界面
 */
public class TransactionPublicDetailCostActivity extends BaseActivity {

    private QWPublicTokenTransaction mQWTransaction;

    private ImageView mStatusImg;
    private TextView mStatusText;
    private TextView mTimeText;

    private TextView mAmountText;
    private TextView mFromToText;
    private JustifyTextView mAddressText;
    private TextView mCostText;

    private TextView mBlockText;
    private JustifyTextView mTxText;

    @Inject
    TransactionModelFactory mTransactionFactory;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_transaction_detail_item;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_transaction_detail_item_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mTopBarView.setTitle(R.string.wallet_transaction_detail_item_title);

        mStatusImg = findViewById(R.id.transaction_state_img);
        mStatusText = findViewById(R.id.transaction_state_text);
        mTimeText = findViewById(R.id.transaction_time);

        mAmountText = findViewById(R.id.transaction_amount_token);
        mFromToText = findViewById(R.id.transaction_state_ft);
        mAddressText = findViewById(R.id.transaction_address);
        mAddressText.setOnClickListener(v -> onCopyAddress());
        mCostText = findViewById(R.id.transaction_cost_token);

        mBlockText = findViewById(R.id.transaction_block_value);
        mTxText = findViewById(R.id.transaction_tx_value);
        mTxText.setOnClickListener(v -> onCopyHash());

        findViewById(R.id.tx_more_detail).setOnClickListener(v -> gotoWebView());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        mQWTransaction = getIntent().getParcelableExtra(Constant.KEY_TRANSACTION);

        if (ConnectionUtil.isInternetConnection(this) && Boolean.parseBoolean(mQWTransaction.getStatus())) {
            if (mQWTransaction.getToken() == null
                    || WalletUtils.isQKCValidAddress(mQWTransaction.getToken().getAddress())) {
                TransactionViewModel transactionViewModel = new ViewModelProvider(this, mTransactionFactory)
                        .get(TransactionViewModel.class);
                transactionViewModel.findObserve().observe(this, this::findSuccess);
                //获取详情数据
                transactionViewModel.getTransactionCostById(mQWTransaction.getTxId(), false, mQWTransaction.getFrom(), mQWTransaction.getTo());
            }
        }
        updateUi();
    }

    private void findSuccess(String cost) {
        mQWTransaction.setCost(cost);

        //更新数据库
        QWPublicTokenTransactionDao dao = new QWPublicTokenTransactionDao(getApplicationContext());
        QWPublicTokenTransaction transaction = dao.queryByID(mQWTransaction.getId());
        if (transaction != null) {
            transaction.setCost(cost);
            dao.update(transaction);

            String[] value = new String[]{transaction.getTxId(), cost};
            RxBus.get().send(Constant.RX_BUS_CODE_TRANS_COST, value);
        }

        //刷新UI
        updateUi();
    }


    private void updateUi() {
        if (!Boolean.parseBoolean(mQWTransaction.getStatus())) {
            findViewById(R.id.tx_more_detail).setVisibility(View.GONE);

            mStatusImg.setImageResource(R.drawable.tran_fail);
            mStatusText.setText(R.string.wallet_transaction_sent_fail);

            mFromToText.setText(R.string.wallet_transaction_from2);
            String address = mQWTransaction.getFrom();
            if (TextUtils.isEmpty(address)) {
                address = getString(R.string.none);
            } else if (QWWalletUtils.isQKCValidAddress(address)) {
                BigInteger toChain = QWWalletUtils.getChainByAddress(getApplicationContext(), address);
                BigInteger toShard = QWWalletUtils.getShardByAddress(getApplicationContext(), address, toChain);
                address = String.format(getString(R.string.wallet_transaction_address_chain_shard), address, toChain.toString(), toShard.toString());
            }
            mAddressText.setCopyText(address);

            boolean isTrx = TronWalletClient.isTronAddressValid(address);
            String amount = isTrx ? QWWalletUtils.getIntTokenFromSun16(mQWTransaction.getAmount()) : QWWalletUtils.getIntTokenNotScaleFromWei16(mQWTransaction.getAmount());
            String tokenText = amount + " " + (QWWalletUtils.isQKCValidAddress(mQWTransaction.getFrom()) ?
                    QWTokenDao.QKC_NAME :
                    (isTrx ? QWTokenDao.TRX_NAME : QWTokenDao.ETH_NAME));
            mAmountText.setText(tokenText);

            findViewById(R.id.detail_other).setVisibility(View.GONE);
            findViewById(R.id.transaction_cost_token_title).setVisibility(View.GONE);
            mCostText.setVisibility(View.GONE);
            return;
        }


        String direction = mQWTransaction.getDirection();
        if (Constant.TOKEN_TRANSACTION_STATE_BUY.equals(direction)) {
            mStatusImg.setImageResource(R.drawable.tran_success);
        } else {
            mStatusImg.setImageResource(R.drawable.send_success);
        }

        mFromToText.setText(R.string.wallet_transaction_from2);
        String normalAddress = mQWTransaction.getFrom();
        String address = normalAddress;
        if (Constant.TOKEN_TRANSACTION_STATE_SEND.equals(mQWTransaction.getDirection())) {
            mStatusText.setText(R.string.public_send_success);
        } else {
            mStatusText.setText(R.string.public_buy_success);
        }

        if (QWWalletUtils.isQKCValidAddress(address)) {
            BigInteger toChain = QWWalletUtils.getChainByAddress(getApplicationContext(), address);
            BigInteger toShard = QWWalletUtils.getShardByAddress(getApplicationContext(), address, toChain);
            address = String.format(getString(R.string.wallet_transaction_address_chain_shard), address, toChain.toString(), toShard.toString());
        }
        mAddressText.setCopyText(address);

        boolean isTrx = TronWalletClient.isTronAddressValid(address);
        mTimeText.setText(isTrx ? QWWalletUtils.parseFullTimeFor16(mQWTransaction.getTimestamp()) : QWWalletUtils.parseTimeFor16(mQWTransaction.getTimestamp()));

        String amount = isTrx ? QWWalletUtils.getIntTokenFromSun16(mQWTransaction.getAmount()) : QWWalletUtils.getIntTokenNotScaleFromWei16(mQWTransaction.getAmount());
        String tokenText = amount + " " + (QWWalletUtils.isQKCValidAddress(normalAddress) ?
                QWTokenDao.QKC_NAME :
                (isTrx ? QWTokenDao.TRX_NAME : QWTokenDao.ETH_NAME));
        mAmountText.setText(tokenText);

        String block = Numeric.toBigInt(mQWTransaction.getBlock()).toString();
        mBlockText.setText(block);

        mTxText.setCopyText(mQWTransaction.getTxId());

        if (QWWalletUtils.isQKCValidAddress(normalAddress)) {
            String cost = "0";
            if (!TextUtils.isEmpty(mQWTransaction.getCost())) {
                cost = QWWalletUtils.getIntTokenFromWeiUP16(mQWTransaction.getCost());
            }
            String text = cost + " " + QWTokenDao.QKC_NAME;
            String symbol = mQWTransaction.getGasTokenName();
            if (!TextUtils.isEmpty(symbol)) {
                text = cost + " " + symbol.toUpperCase();
            }
            mCostText.setText(text);
        } else if (!TextUtils.isEmpty(mQWTransaction.getCost())) {
            String cost;
            if (isTrx) {
                cost = QWWalletUtils.getIntTokenFromSun16(mQWTransaction.getCost());
            } else {
                cost = QWWalletUtils.getIntTokenFromWeiUP16(mQWTransaction.getCost());
            }
            String text = cost + " " + QWTokenDao.ETH_NAME;
            mCostText.setText(text);
        }
    }

    private void onCopyAddress() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && !TextUtils.isEmpty(mAddressText.getText())) {
            String text = mAddressText.getText().toString();
            String[] message = text.split(" ");
            String label = getString(R.string.wallet_copy_address_label);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText(label, message[0]);
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);

            MyToast.showSingleToastShort(this, R.string.copy_success);
        }
    }

    private void onCopyHash() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && !TextUtils.isEmpty(mTxText.getText())) {
            String label = getString(R.string.wallet_copy_address_label);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText(label, mTxText.getText().toString());
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);

            MyToast.showSingleToastShort(this, R.string.copy_success);
        }
    }

    private void gotoWebView() {
        String address = !TextUtils.isEmpty(mQWTransaction.getFrom()) ? mQWTransaction.getFrom() : mQWTransaction.getTo();
        TxWebViewActivity.startActivity(this, address, mTxText.getText().toString());
    }
}
