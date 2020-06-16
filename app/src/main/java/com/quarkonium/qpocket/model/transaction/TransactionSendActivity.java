package com.quarkonium.qpocket.model.transaction;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.entity.ErrorEnvelope;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.Keys;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionModelFactory;
import com.quarkonium.qpocket.model.transaction.viewmodel.TransactionViewModel;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.JustifyTextView;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import java.math.BigInteger;
import java.util.Set;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * 交易详情列表界面
 */
public class TransactionSendActivity extends BaseActivity {

    private static Intent getSendIntent(Activity activity,
                                        String fromAddress, String toAddress,
                                        String amount, String cost,
                                        String hashId, String encode, String token,
                                        int accountType,
                                        boolean notShowToast,
                                        int formActivity,
                                        String tokenSymbol,
                                        String tokenAddress) {
        Intent intent = new Intent(activity, TransactionSendActivity.class);
        intent.putExtra(KEY_FROM_ADDRESS, fromAddress);
        intent.putExtra(KEY_TO_ADDRESS, toAddress);
        intent.putExtra(KEY_AMOUNT, amount);
        intent.putExtra(KEY_COST, cost);
        intent.putExtra(KEY_HASH_ID, hashId);
        intent.putExtra(KEY_ENCODE, encode);
        intent.putExtra(KEY_TOKEN, token);
        intent.putExtra(KEY_NOT_TOAST, notShowToast);
        intent.putExtra(KEY_ACCOUNT_TYPE, accountType);

        intent.putExtra(KEY_FROM_ACTIVITY, formActivity);
        intent.putExtra(KEY_TOKEN_SYMBOL, tokenSymbol);
        intent.putExtra(KEY_TOKEN_SYMBOL_ADDRESS, tokenAddress);
        return intent;
    }

    public static void startTransactionSendActivity(Activity activity,
                                                    String fromAddress, String toAddress,
                                                    String amount, String cost,
                                                    String hashId, String encode, String token,
                                                    int accountType,
                                                    boolean notShowToast,
                                                    int formActivity,
                                                    String tokenSymbol,
                                                    String tokenAddress) {

        Intent intent = getSendIntent(activity, fromAddress, toAddress, amount, cost, hashId, encode, token, accountType, notShowToast, formActivity, tokenSymbol, tokenAddress);
        activity.startActivityForResult(intent, Constant.REQUEST_CODE_SEND_TRANSACTIONS);
    }

    public static void startQKCTransactionSendActivity(Activity activity,
                                                       String fromAddress, String toAddress,
                                                       String amount, String cost,
                                                       String hashId, String encode, String token,
                                                       int accountType,
                                                       boolean notShowToast,
                                                       int formActivity,
                                                       String tokenSymbol,
                                                       String tokenAddress,
                                                       String gasToken) {
        Intent intent = getSendIntent(activity, fromAddress, toAddress, amount, cost, hashId, encode, token, accountType, notShowToast, formActivity, tokenSymbol, tokenAddress);
        intent.putExtra(KEY_GAS_TOKEN_SYMBOL, gasToken);
        activity.startActivityForResult(intent, Constant.REQUEST_CODE_SEND_TRANSACTIONS);
    }

    public static void startTransactionDAppSendActivity(Activity activity,
                                                        String fromAddress, String toAddress,
                                                        String amount, String cost,
                                                        String hashId, String encode, String token,
                                                        int accountType,
                                                        boolean notShowToast,
                                                        int formActivity,
                                                        String tokenSymbol,
                                                        String tokenAddress,
                                                        String host) {

        Intent intent = getSendIntent(activity, fromAddress, toAddress, amount, cost, hashId, encode, token, accountType, notShowToast, formActivity, tokenSymbol, tokenAddress);
        intent.putExtra(KEY_URL_HOST, host);
        activity.startActivityForResult(intent, Constant.REQUEST_CODE_SEND_TRANSACTIONS);
    }

    private static final String KEY_FROM_ADDRESS = "key_from_address";
    private static final String KEY_TO_ADDRESS = "key_to_address";
    private static final String KEY_AMOUNT = "key_amount";
    private static final String KEY_COST = "key_cost";
    private static final String KEY_HASH_ID = "key_hash_id";
    private static final String KEY_ENCODE = "key_encode";
    private static final String KEY_TOKEN = "key_token";
    private static final String KEY_NOT_TOAST = "key_not_toast";
    private static final String KEY_ACCOUNT_TYPE = "key_account_type";

    private static final String KEY_FROM_ACTIVITY = "key_from_activity";
    private static final String KEY_TOKEN_SYMBOL = "key_token_symbol";
    private static final String KEY_TOKEN_SYMBOL_ADDRESS = "key_token_symbol_address";

    private static final String KEY_URL_HOST = "key_url_host";

    private static final String KEY_GAS_TOKEN_SYMBOL = "key_gas_token_symbol";

    public static final int KEY_FROM_DAPP = 11;
    public static final int KEY_FROM_NORMAL = 12;
    public static final int KEY_FROM_PUBLICSALE = 13;

    @Inject
    TransactionModelFactory mTransactionFactory;
    private TransactionViewModel mTransactionViewModel;

    private View mProgressLayout;

    private String mTransaction;
    private boolean mNotShowToast;
    private int mAccountType;

    private int mFromActivity;
    private String mTokenName;
    private String mTokenAddress;
    private String mAmount;

    private View mCheckWhiteList;
    private String mHost;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_transaction_send;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_transaction_send_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mTopBarView.setTitle(R.string.wallet_transaction_send_title);

        mAccountType = getIntent().getIntExtra(KEY_ACCOUNT_TYPE, Constant.ACCOUNT_TYPE_QKC);
        mFromActivity = getIntent().getIntExtra(KEY_FROM_ACTIVITY, KEY_FROM_NORMAL);
        mTokenName = getIntent().getStringExtra(KEY_TOKEN_SYMBOL);
        mTokenAddress = getIntent().getStringExtra(KEY_TOKEN_SYMBOL_ADDRESS);
        mHost = getIntent().getStringExtra(KEY_URL_HOST);

        View mSendView = findViewById(R.id.account_action_next);
        mSendView.setOnClickListener(v -> onSend());

        //to地址
        String toDefaultAddress = getIntent().getStringExtra(KEY_TO_ADDRESS);
        String toAddress = toDefaultAddress;
        if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
            BigInteger toChain = QWWalletUtils.getChainByAddress(getApplicationContext(), toDefaultAddress);
            BigInteger toShard = QWWalletUtils.getShardByAddress(getApplicationContext(), toDefaultAddress, toChain);
            toAddress = String.format(getString(R.string.wallet_transaction_address_chain_shard), toAddress, toChain.toString(), toShard.toString());
        }
        JustifyTextView toAddressText = findViewById(R.id.transaction_to_address);
        toAddressText.setText(toAddress);

        //from地址
        String fromDefaultAddress = getIntent().getStringExtra(KEY_FROM_ADDRESS);
        JustifyTextView fromAddressText = findViewById(R.id.transaction_from_address);
        String fromAddress = fromDefaultAddress;
        if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
            fromAddress = Keys.toChecksumHDAddress(fromAddress);
            BigInteger fromChain = QWWalletUtils.getChainByAddress(getApplicationContext(), fromDefaultAddress);
            BigInteger fromShard = QWWalletUtils.getShardByAddress(getApplicationContext(), fromDefaultAddress, fromChain);
            fromAddress = String.format(getString(R.string.wallet_transaction_address_chain_shard), fromAddress, fromChain.toString(), fromShard.toString());
        } else if (mAccountType == Constant.ACCOUNT_TYPE_ETH) {
            fromAddress = Keys.toChecksumHDAddress(fromAddress);
        }
        fromAddressText.setText(fromAddress);

        TextView feeTextView = findViewById(R.id.transaction_send_gas_title);
        //转账数量
        String tokenName = getIntent().getStringExtra(KEY_TOKEN);
        mAmount = getIntent().getStringExtra(KEY_AMOUNT);
        TextView amountText = findViewById(R.id.transaction_send_amount);
        TextView amountPriceView = findViewById(R.id.transaction_send_amount_price);
        if (TextUtils.isEmpty(tokenName)) {
            if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
                amountText.setText(String.format(getString(R.string.wallet_transaction_total_token), mAmount));

                String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.QKC_SYMBOL, mAmount);
                amountPriceView.setText(priceStr);
            } else if (mAccountType == Constant.ACCOUNT_TYPE_ETH) {
                String text = mAmount + " " + getString(R.string.eth);
                amountText.setText(text);

                String priceStr = ToolUtils.getETHTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.ETH_SYMBOL, mAmount);
                amountPriceView.setText(priceStr);
            } else if (mAccountType == Constant.ACCOUNT_TYPE_TRX) {
                String text = mAmount + " " + getString(R.string.trx);
                amountText.setText(text);
                feeTextView.setText(R.string.transaction_send_title);

                String priceStr = ToolUtils.getTRXTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.TRX_SYMBOL, mAmount);
                amountPriceView.setText(priceStr);
            } else {
                amountText.setText(mAmount);

                String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.ETH_SYMBOL, "0");
                amountPriceView.setText(priceStr);
            }
        } else {
            //转token
            feeTextView.setText(R.string.wallet_transaction_send_token_max_title);
            if (mAccountType == Constant.ACCOUNT_TYPE_ETH) {
                String text = "0 " + getString(R.string.eth);
                amountText.setText(text);

                //当前版本除去QKC native外，其他ERC20 Token不显示具体花费
                String priceStr = ToolUtils.getETHTokenCurrentCoinPriceText(getApplicationContext(), mTokenName, "0");
                amountPriceView.setText(priceStr);
            } else if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
                if (QWWalletUtils.isQKCValidAddress(mTokenAddress)) {
                    amountText.setText(String.format(getString(R.string.wallet_transaction_total_token), "0"));

                    //当前版本除去QKC native外，其他ERC20 Token不显示具体花费
                    String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), mTokenName, "0");
                    amountPriceView.setText(priceStr);
                } else {
                    String text = mAmount + " " + mTokenName.toUpperCase();
                    amountText.setText(text);

                    //当前版本除去QKC native外，其他ERC20 Token不显示具体花费
                    String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), mTokenName, mAmount);
                    amountPriceView.setText(priceStr);
                }
            } else if (mAccountType == Constant.ACCOUNT_TYPE_TRX) {
                String text = "0 " + getString(R.string.trx);
                amountText.setText(text);
                feeTextView.setText(R.string.transaction_send_title);

                //当前版本除去QKC native外，其他ERC20 Token不显示具体花费
                String priceStr = ToolUtils.getTRXTokenCurrentCoinPriceText(getApplicationContext(), mTokenName, "0");
                amountPriceView.setText(priceStr);
            }
        }

        //手续费
        String mGasTokenText = getIntent().getStringExtra(KEY_GAS_TOKEN_SYMBOL);
        String cost = getIntent().getStringExtra(KEY_COST);
        TextView gasText = findViewById(R.id.transaction_send_gas);
        TextView gasPriceView = findViewById(R.id.transaction_send_gas_price);
        if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
            if (TextUtils.isEmpty(mGasTokenText)) {
                gasText.setText(String.format(getString(R.string.wallet_transaction_total_token), cost));

                String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.QKC_SYMBOL, cost);
                gasPriceView.setText(priceStr);
            } else {
                String text = cost + " " + mGasTokenText;
                gasText.setText(text);

                String priceStr = ToolUtils.getQKCTokenCurrentCoinPriceText(getApplicationContext(), mGasTokenText, cost);
                gasPriceView.setText(priceStr);
            }
        } else if (mAccountType == Constant.ACCOUNT_TYPE_ETH) {
            String text = cost + " " + getString(R.string.eth);
            gasText.setText(text);

            String priceStr = ToolUtils.getETHTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.ETH_SYMBOL, cost);
            gasPriceView.setText(priceStr);
        } else if (mAccountType == Constant.ACCOUNT_TYPE_TRX) {
            String text = cost + " " + getString(R.string.trx);
            gasText.setText(text);

            String priceStr = ToolUtils.getETHTokenCurrentCoinPriceText(getApplicationContext(), QWTokenDao.TRX_SYMBOL, cost);
            gasPriceView.setText(priceStr);
        }

        String hash = getIntent().getStringExtra(KEY_HASH_ID);
        TextView hashText = findViewById(R.id.transaction_send_hash);
        hashText.setText(hash);

        mTransaction = getIntent().getStringExtra(KEY_ENCODE);


        //分片ID
        TextView shardTextView = findViewById(R.id.transaction_send_shard_limit);
        if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
            BigInteger fromChain = QWWalletUtils.getChainByAddress(getApplicationContext(), fromDefaultAddress);
            BigInteger fromShard = QWWalletUtils.getShardByAddress(getApplicationContext(), fromDefaultAddress, fromChain);

            BigInteger toChain = QWWalletUtils.getChainByAddress(getApplicationContext(), toDefaultAddress);
            BigInteger toShard = QWWalletUtils.getShardByAddress(getApplicationContext(), toDefaultAddress, toChain);
            if (fromChain.equals(toChain) && fromShard.equals(toShard)) {
                findViewById(R.id.transaction_send_shard_second).setVisibility(View.GONE);
                shardTextView.setVisibility(View.GONE);
            } else {
                String shardText = String.format(getString(R.string.transaction_send_chain_shard_limit), fromChain.toString(), fromShard.toString(), toChain.toString(), toShard.toString());
                shardTextView.setText(shardText);
            }
        } else {
            findViewById(R.id.transaction_send_shard_second).setVisibility(View.GONE);
            shardTextView.setVisibility(View.GONE);
        }

        mNotShowToast = getIntent().getBooleanExtra(KEY_NOT_TOAST, false);

        if (!TextUtils.isEmpty(mHost)) {
            findViewById(R.id.white_list_title).setVisibility(View.VISIBLE);
            findViewById(R.id.white_list_msg).setVisibility(View.VISIBLE);
            mCheckWhiteList = findViewById(R.id.white_list_check);
            mCheckWhiteList.setVisibility(View.VISIBLE);
            mCheckWhiteList.setOnClickListener(v -> openWhiteList());
            mCheckWhiteList.setSelected(isWhiteList());
        }

        mProgressLayout = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressLayout, UiUtils.dpToPixel(3));
    }

    private boolean isWhiteList() {
        Set<String> set = SharedPreferencesUtils.getDAppWhiteList(getApplicationContext());
        return set.contains(mHost);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        mTransactionViewModel = new ViewModelProvider(this, mTransactionFactory)
                .get(TransactionViewModel.class);
        mTransactionViewModel.sendObserve().observe(this, this::onSendSuccess);
        mTransactionViewModel.error().observe(this, this::onError);
        mTransactionViewModel.progress().observe(this, this::showProgress);
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressLayout.setVisibility(View.VISIBLE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
        }
    }

    private void onSend() {
        if (!ConnectionUtil.isInternetConnection(this)) {
            MyToast.showSingleToastShort(this, R.string.network_error);
            return;
        }

        if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
            mTransactionViewModel.senTransaction(mTransaction);
        } else if (mAccountType == Constant.ACCOUNT_TYPE_ETH) {
            mTransactionViewModel.senEthTransaction(mTransaction);
        } else if (mAccountType == Constant.ACCOUNT_TYPE_TRX) {
            if (QWWalletUtils.isJson(mTransaction)) {
                onSendSuccess(mTransaction);
            } else {
                mTransactionViewModel.senTrxTransaction(mTransaction);
            }
        }

        if (mFromActivity == KEY_FROM_DAPP) {
            UmengStatistics.walletDAppSendTransactionClickCount(getApplicationContext(), mTokenName, QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
        } else if (mFromActivity == KEY_FROM_PUBLICSALE) {
            UmengStatistics.walletPublicSaleSendTransactionClickCount(getApplicationContext(), mTokenName, QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
        } else {
            UmengStatistics.walletSendTokenTransactionClickCount(getApplicationContext(), mTokenName, QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
        }
    }


    private void onSendSuccess(String hashId) {
        Intent intent = getIntent();
        intent.putExtra(Constant.KEY_RESULT, true);
        intent.putExtra(Constant.KEY_TRANSACTION_HASH, hashId);
        setResult(Activity.RESULT_OK, intent);
        finish();

        if (!mNotShowToast) {
            MyToast.showSingleToastShort(this, R.string.transaction_send_success);
        }

        int amount = (int) (Double.parseDouble(mAmount) * 1000000);
        //上报成功数据
        if (mFromActivity == KEY_FROM_DAPP) {
            //DApp转账次数
            UmengStatistics.dAppSendTranSuccessCount(getApplicationContext(), mAccountType, mTokenName, mTokenAddress, amount);
        } else {
            //普通转账次数
            UmengStatistics.sendTranSuccessCount(getApplicationContext(), mAccountType, mTokenName, mTokenAddress, amount);
        }
    }

    private void onError(ErrorEnvelope throwable) {
        showProgress(false);
        MyToast.showSingleToastShort(this, R.string.transaction_send_fail);
    }

    private void openWhiteList() {
        if (mCheckWhiteList.isSelected()) {
            mCheckWhiteList.setSelected(false);
            Set<String> set = SharedPreferencesUtils.getDAppWhiteList(getApplicationContext());
            set.remove(mHost);
            SharedPreferencesUtils.setDAppWhiteList(getApplicationContext(), set);
        } else {
            mCheckWhiteList.setSelected(true);
            Set<String> set = SharedPreferencesUtils.getDAppWhiteList(getApplicationContext());
            set.add(mHost);
            SharedPreferencesUtils.setDAppWhiteList(getApplicationContext(), set);
        }
    }
}
