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
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.dao.QWTransactionDao;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWTransaction;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.utils.Convert;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionModelFactory;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionViewModel;
import com.quarkonium.qpocket.rx.RxBus;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.view.JustifyTextView;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.R;

import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * 交易详情列表界面
 */
public class TransactionDetailCostActivity extends BaseActivity {

    private QWTransaction mQWTransaction;

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

        findViewById(R.id.re_take).setOnClickListener(v -> finish());

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
            //ETH BTC 不需要获取交易gas费
            if (mQWTransaction.getToken().getType() != Constant.ACCOUNT_TYPE_ETH) {
                TransactionViewModel transactionViewModel = new ViewModelProvider(this, mTransactionFactory).get(TransactionViewModel.class);
                transactionViewModel.findObserve().observe(this, this::findSuccess);
                //获取详情数据
                boolean isTrx = QWTokenDao.TRX_SYMBOL.equals(mQWTransaction.getToken().getSymbol()) || mQWTransaction.getToken().getType() == Constant.ACCOUNT_TYPE_TRX;
                transactionViewModel.getTransactionCostById(mQWTransaction.getTxId(), isTrx, mQWTransaction.getFrom(), mQWTransaction.getTo());
            }
        }
        updateUi();
    }

    private void findSuccess(String cost) {
        mQWTransaction.setCost(cost);

        //更新数据库
        QWTransactionDao dao = new QWTransactionDao(getApplicationContext());
        QWTransaction transaction = dao.queryByID(mQWTransaction.getId());
        if (transaction != null) {
            transaction.setCost(cost);
            dao.updateTransaction(transaction);

            String[] value = new String[]{transaction.getTxId(), cost};
            RxBus.get().send(Constant.RX_BUS_CODE_TRANS_COST, value);
        }

        //刷新UI
        updateUi();
    }


    private void updateUi() {
        //TRX
        if (mQWTransaction.getToken().getType() == Constant.ACCOUNT_TYPE_TRX) {
            updateTrxUi();
            return;
        }

        //ETH
        if (mQWTransaction.getToken().getType() == Constant.ACCOUNT_TYPE_ETH) {
            updateETHUi();
            return;
        }

        //chain
        String allChain = SharedPreferencesUtils.getTotalChainCount(getApplicationContext());
        BigInteger totalCount = Numeric.toBigInt(allChain);
        //shared
        List<String> allShared = SharedPreferencesUtils.getTotalSharedSizes(getApplicationContext());

        //pending状态
        if (Constant.QKC_TRANSACTION_STATE_PENDING.equals(mQWTransaction.getDirection())) {
            findViewById(R.id.tx_more_detail).setVisibility(View.GONE);
            mStatusImg.setImageResource(R.drawable.state_pending);
            mStatusText.setText(R.string.wallet_transaction_pending);
            mFromToText.setText(R.string.wallet_transaction_to2);

            String address = mQWTransaction.getTo();
            if (QWWalletUtils.isQKCValidAddress(address)) {
                address = paseQkcAddress(address, totalCount, allShared);
            }
            mAddressText.setCopyText(address);

            QWToken token = mQWTransaction.getToken();
            String amount = parseAmount(token, TronWalletClient.isTronAddressValid(address));
            if (!TextUtils.isEmpty(mQWTransaction.getTransferTokenStr())) {
                String text = amount + " " + mQWTransaction.getTransferTokenStr();
                mAmountText.setText(text);
            } else {
                mAmountText.setText(token != null ? amount + " " + token.getSymbol().toUpperCase() : amount);
            }

            findViewById(R.id.detail_other).setVisibility(View.GONE);
            findViewById(R.id.transaction_cost_token_title).setVisibility(View.GONE);
            mCostText.setVisibility(View.GONE);
            return;
        }


        //失败
        if (!Boolean.parseBoolean(mQWTransaction.getStatus())) {
            mStatusImg.setImageResource(R.drawable.tran_fail);
            mStatusText.setText(R.string.wallet_transaction_sent_fail);

            String address;
            if (Constant.QKC_TRANSACTION_STATE_SEND.equals(mQWTransaction.getDirection())) {
                if (Boolean.parseBoolean(mQWTransaction.getStatus())) {
                    mStatusText.setText(R.string.wallet_transaction_sent);
                }
                mFromToText.setText(R.string.wallet_transaction_to2);

                address = mQWTransaction.getTo();
            } else {
                if (Boolean.parseBoolean(mQWTransaction.getStatus())) {
                    mStatusText.setText(R.string.wallet_transaction_receive);
                }
                mFromToText.setText(R.string.wallet_transaction_from2);

                address = mQWTransaction.getFrom();
            }

            if (TextUtils.isEmpty(address)) {
                address = getString(R.string.none);
                mAddressText.setText(address);
            } else if (QWWalletUtils.isQKCValidAddress(address)) {
                address = paseQkcAddress(address, totalCount, allShared);
                mAddressText.setCopyText(address);
            }

            QWToken token = mQWTransaction.getToken();
            String amount = parseAmount(token, TronWalletClient.isTronAddressValid(address));
            if (!TextUtils.isEmpty(mQWTransaction.getTransferTokenStr())) {
                String text = amount + " " + mQWTransaction.getTransferTokenStr();
                mAmountText.setText(text);
            } else {
                mAmountText.setText(token != null ? amount + " " + token.getSymbol().toUpperCase() : amount);
            }

            findViewById(R.id.detail_other).setVisibility(View.GONE);
            findViewById(R.id.transaction_cost_token_title).setVisibility(View.GONE);
            mCostText.setVisibility(View.GONE);
            return;
        }


        //成功
        String direction = mQWTransaction.getDirection();
        if (Constant.QKC_TRANSACTION_STATE_SEND.equals(direction)) {
            mStatusImg.setImageResource(R.drawable.send_success);
        } else {
            mStatusImg.setImageResource(R.drawable.tran_success);
        }
        //block
        String block = Numeric.toBigInt(mQWTransaction.getBlock()).toString();
        mBlockText.setText(block);
        //txID
        mTxText.setCopyText(mQWTransaction.getTxId());
        //地址
        String normalAddress;
        if (Constant.QKC_TRANSACTION_STATE_SEND.equals(mQWTransaction.getDirection())) {
            if (Boolean.parseBoolean(mQWTransaction.getStatus())) {
                mStatusText.setText(R.string.wallet_transaction_sent);
            }
            mFromToText.setText(R.string.wallet_transaction_to2);
            normalAddress = mQWTransaction.getTo();
        } else {
            if (Boolean.parseBoolean(mQWTransaction.getStatus())) {
                mStatusText.setText(R.string.wallet_transaction_receive);
            }
            mFromToText.setText(R.string.wallet_transaction_from2);
            normalAddress = mQWTransaction.getFrom();
        }
        String address = normalAddress;
        if (QWWalletUtils.isQKCValidAddress(normalAddress)) {
            address = paseQkcAddress(normalAddress, totalCount, allShared);
        }
        mAddressText.setCopyText(address);

        //时间
        mTimeText.setText(QWWalletUtils.parseTimeFor16(mQWTransaction.getTimestamp()));

        //amount金额
        QWToken token = mQWTransaction.getToken();
        String amount = parseAmount(token, false);
        if (!TextUtils.isEmpty(mQWTransaction.getTransferTokenStr())) {
            String text = amount + " " + mQWTransaction.getTransferTokenStr();
            mAmountText.setText(text);
        } else {
            mAmountText.setText(token != null ? amount + " " + token.getSymbol().toUpperCase() : amount);
        }

        //gas手续费
        String cost = "0";
        if (!TextUtils.isEmpty(mQWTransaction.getCost())) {
            cost = QWWalletUtils.getIntTokenFromWei16(mQWTransaction.getCost(), Convert.Unit.ETHER, Constant.QKC_DECIMAL_NUMBER, true);
        }
        String text = cost + " " + QWTokenDao.QKC_NAME;
        String symbol = mQWTransaction.getGasTokenStr();
        if (!TextUtils.isEmpty(symbol)) {
            text = cost + " " + symbol.toUpperCase();
        }
        mCostText.setText(text);
    }

    private String paseQkcAddress(String address, BigInteger totalCount, List<String> allShared) {
        String chain = QWWalletUtils.parseChainForAddress(address, totalCount);
        int currentChain = Numeric.toBigInt(chain).intValue();
        BigInteger totalShard = BigInteger.ONE;
        if (allShared != null && currentChain < allShared.size() && currentChain >= 0) {
            totalShard = Numeric.toBigInt(allShared.get(currentChain));
        }
        String addressShard = QWWalletUtils.parseShardForAddress(address, totalShard);
        int currentShard = Numeric.toBigInt(addressShard).intValue();

        return String.format(getString(R.string.wallet_transaction_address_chain_shard), address, currentChain + "", currentShard + "");
    }

    private void updateETHUi() {
        //pending状态
        if (Constant.QKC_TRANSACTION_STATE_PENDING.equals(mQWTransaction.getDirection())) {
            findViewById(R.id.tx_more_detail).setVisibility(View.GONE);
            mStatusImg.setImageResource(R.drawable.state_pending);
            mStatusText.setText(R.string.wallet_transaction_pending);
            mFromToText.setText(R.string.wallet_transaction_to2);

            String address = mQWTransaction.getTo();
            mAddressText.setCopyText(address);

            QWToken token = mQWTransaction.getToken();
            String amount = parseAmount(token, TronWalletClient.isTronAddressValid(address));
            if (!TextUtils.isEmpty(mQWTransaction.getTransferTokenStr())) {
                String text = amount + " " + mQWTransaction.getTransferTokenStr();
                mAmountText.setText(text);
            } else {
                mAmountText.setText(token != null ? amount + " " + token.getSymbol().toUpperCase() : amount);
            }

            findViewById(R.id.detail_other).setVisibility(View.GONE);
            findViewById(R.id.transaction_cost_token_title).setVisibility(View.GONE);
            mCostText.setVisibility(View.GONE);
            return;
        }


        //失败
        if (!Boolean.parseBoolean(mQWTransaction.getStatus())) {
            mStatusImg.setImageResource(R.drawable.tran_fail);
            mStatusText.setText(R.string.wallet_transaction_sent_fail);

            String address;
            if (Constant.QKC_TRANSACTION_STATE_SEND.equals(mQWTransaction.getDirection())) {
                if (Boolean.parseBoolean(mQWTransaction.getStatus())) {
                    mStatusText.setText(R.string.wallet_transaction_sent);
                }
                mFromToText.setText(R.string.wallet_transaction_to2);

                address = mQWTransaction.getTo();
            } else {
                if (Boolean.parseBoolean(mQWTransaction.getStatus())) {
                    mStatusText.setText(R.string.wallet_transaction_receive);
                }
                mFromToText.setText(R.string.wallet_transaction_from2);

                address = mQWTransaction.getFrom();
            }

            if (TextUtils.isEmpty(address)) {
                address = getString(R.string.none);
                mAddressText.setText(address);
            } else {
                mAddressText.setCopyText(address);
            }

            QWToken token = mQWTransaction.getToken();
            String amount = parseAmount(token, TronWalletClient.isTronAddressValid(address));
            if (!TextUtils.isEmpty(mQWTransaction.getTransferTokenStr())) {
                String text = amount + " " + mQWTransaction.getTransferTokenStr();
                mAmountText.setText(text);
            } else {
                mAmountText.setText(token != null ? amount + " " + token.getSymbol().toUpperCase() : amount);
            }

            findViewById(R.id.detail_other).setVisibility(View.GONE);
            findViewById(R.id.transaction_cost_token_title).setVisibility(View.GONE);
            mCostText.setVisibility(View.GONE);
            return;
        }


        //成功
        String direction = mQWTransaction.getDirection();
        if (Constant.QKC_TRANSACTION_STATE_SEND.equals(direction)) {
            mStatusImg.setImageResource(R.drawable.send_success);
        } else {
            mStatusImg.setImageResource(R.drawable.tran_success);
        }
        //block
        String block = Numeric.toBigInt(mQWTransaction.getBlock()).toString();
        mBlockText.setText(block);
        //txID
        mTxText.setCopyText(mQWTransaction.getTxId());
        //地址
        if (Constant.QKC_TRANSACTION_STATE_SEND.equals(direction)) {
            if (Boolean.parseBoolean(mQWTransaction.getStatus())) {
                mStatusText.setText(R.string.wallet_transaction_sent);
            }
            mFromToText.setText(R.string.wallet_transaction_to2);
            mAddressText.setCopyText(mQWTransaction.getTo());
        } else {
            if (Boolean.parseBoolean(mQWTransaction.getStatus())) {
                mStatusText.setText(R.string.wallet_transaction_receive);
            }
            mFromToText.setText(R.string.wallet_transaction_from2);
            mAddressText.setCopyText(mQWTransaction.getFrom());
        }

        //时间
        mTimeText.setText(QWWalletUtils.parseTimeFor16(mQWTransaction.getTimestamp()));

        //amount金额
        QWToken token = mQWTransaction.getToken();
        String amount = parseAmount(token, false);
        if (!TextUtils.isEmpty(mQWTransaction.getTransferTokenStr())) {
            String text = amount + " " + mQWTransaction.getTransferTokenStr();
            mAmountText.setText(text);
        } else {
            mAmountText.setText(token != null ? amount + " " + token.getSymbol().toUpperCase() : amount);
        }

        //gas手续费
        String cost = QWWalletUtils.getIntTokenFromWeiUP16(mQWTransaction.getCost());
        if (!TextUtils.isEmpty(cost)) {
            mCostText.setText(cost + "ETH");
        } else {
            mCostText.setText("0 ETH");
        }
    }

    private void updateTrxUi() {
        String direction = mQWTransaction.getDirection();
        if (!Boolean.parseBoolean(mQWTransaction.getStatus())) {
            //Trx未确认状态
            mStatusImg.setImageResource(R.drawable.state_pending);
            mStatusText.setText(R.string.transaction_unconfirmed);
        } else {
            if (Constant.QKC_TRANSACTION_STATE_SEND.equals(direction)) {
                mStatusImg.setImageResource(R.drawable.send_success);
                mStatusText.setText(R.string.wallet_transaction_sent);

                mFromToText.setText(R.string.wallet_transaction_to2);
                mAddressText.setCopyText(mQWTransaction.getTo());
            } else if (Constant.QKC_TRANSACTION_STATE_RECEIVE.equals(direction)) {
                mStatusImg.setImageResource(R.drawable.tran_success);
                mStatusText.setText(R.string.wallet_transaction_receive);

                mFromToText.setText(R.string.wallet_transaction_from2);
                mAddressText.setCopyText(mQWTransaction.getFrom());
            } else if (Constant.QKC_TRANSACTION_STATE_FREEZE.equals(direction)) {
                //冻结
                mStatusImg.setImageResource(R.drawable.trans_freeze_success);
                mStatusText.setText(R.string.tran_freeze);

                mFromToText.setText(R.string.wallet_transaction_from2);
                mAddressText.setCopyText(mQWTransaction.getFrom());
            } else if (Constant.QKC_TRANSACTION_STATE_UNFREEZE.equals(direction)) {
                //解冻
                mStatusImg.setImageResource(R.drawable.trans_unfreeze_success);
                mStatusText.setText(R.string.tran_unfreeze);

                mFromToText.setText(R.string.wallet_transaction_from2);
                mAddressText.setCopyText(mQWTransaction.getFrom());
            } else if (Constant.QKC_TRANSACTION_STATE_VOTE.equals(direction)) {
                //投票
                mStatusImg.setImageResource(R.drawable.trans_vote_success);
                mStatusText.setText(R.string.tran_vote);

                mFromToText.setText(R.string.wallet_transaction_from2);
                mAddressText.setCopyText(mQWTransaction.getFrom());
            } else if (Constant.QKC_TRANSACTION_STATE_CONTRACT.equals(direction)) {
                //合约
                mStatusImg.setImageResource(R.drawable.trans_contact_success);
                mStatusText.setText(R.string.tran_smart);

                mFromToText.setText(R.string.wallet_transaction_from2);
                mAddressText.setCopyText(mQWTransaction.getFrom());
            }
        }

        mTimeText.setText(QWWalletUtils.parseFullTimeFor16(mQWTransaction.getTimestamp()));

        QWToken token = mQWTransaction.getToken();
        String amount = parseAmount(token, true);
        mAmountText.setText(token != null ? amount + " " + token.getSymbol().toUpperCase() : amount);

        String block = Numeric.toBigInt(mQWTransaction.getBlock()).toString();
        mBlockText.setText(block);

        mTxText.setCopyText(mQWTransaction.getTxId());

        //trx没有gas概念
        if (!TextUtils.isEmpty(mQWTransaction.getCost())) {
            String cost = QWWalletUtils.getIntTokenFromSun16(mQWTransaction.getCost());
            mCostText.setText(cost + " TRX");
        } else {
            mCostText.setText("0 TRX");
        }
    }

    private String parseAmount(QWToken token, boolean isTrx) {
        if (token == null) {
            if (isTrx) {
                return QWWalletUtils.getIntTokenFromSun16(mQWTransaction.getAmount());
            } else {
                return QWWalletUtils.getIntTokenNotScaleFromWei16(mQWTransaction.getAmount());
            }
        } else {
            return QWWalletUtils.getIntTokenNotScaleFromWei16(mQWTransaction.getAmount(), token.getTokenUnit());
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
        if (TextUtils.isEmpty(mQWTransaction.getTxId())) {
            return;
        }
        String address = !TextUtils.isEmpty(mQWTransaction.getFrom()) ? mQWTransaction.getFrom() : mQWTransaction.getTo();
        TxWebViewActivity.startActivity(this, address, mQWTransaction.getTxId());
    }
}
