package com.quarkonium.qpocket.model.main;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.zxing.Result;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.crypto.utils.Convert;
import com.quarkonium.qpocket.model.permission.PermissionHelper;
import com.quarkonium.qpocket.model.transaction.TransactionCreateActivity;
import com.quarkonium.qpocket.rx.NetworkChangeEvent;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.EncodingUtils;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zbar.ZBarView;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 二维码扫描
 */
public class CaptureActivity extends BaseActivity implements QRCodeView.Delegate {

    public static void startForResultActivity(Activity activity, QWWallet wallet, String tokenAddress, int type) {
        Intent intent = new Intent(activity, CaptureActivity.class);
        intent.putExtra(Constant.KEY_ACCOUNT_TYPE, type);
        intent.putExtra(Constant.KEY_WALLET, wallet);
        intent.putExtra(Constant.KEY_TOKEN_ADDRESS, tokenAddress);
        intent.putExtra(Constant.KEY_CAPTURE_FOR_RESULT, true);
        activity.startActivityForResult(intent, Constant.REQUEST_CODE_CAPTURE);
    }

    public static void startForResultActivity(Activity activity, int type) {
        Intent intent = new Intent(activity, CaptureActivity.class);
        intent.putExtra(Constant.KEY_ACCOUNT_TYPE, type);
        intent.putExtra(Constant.KEY_CAPTURE_ONLY_SCAN, true);
        intent.putExtra(Constant.KEY_CAPTURE_FOR_RESULT, true);
        activity.startActivityForResult(intent, Constant.REQUEST_CODE_CAPTURE);
    }

    public static void startForResultActivity(Fragment fragment, Activity activity, int type) {
        Intent intent = new Intent(activity, CaptureActivity.class);
        intent.putExtra(Constant.KEY_ACCOUNT_TYPE, type);
        intent.putExtra(Constant.KEY_CAPTURE_WATCH, true);
        fragment.startActivityForResult(intent, Constant.REQUEST_CODE_CAPTURE);
    }


    private TextView mLightView;
    private boolean mIsOpen;

    private ZBarView mZBarView;

    private Disposable mDisposable;

    protected QWWallet mWallet;
    protected int mAccountType;
    private String mContractAddress;
    private boolean mIsCaptureResult;
    private boolean mIsOnlyScan;

    private boolean mIsWatchScan;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_scanner;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_qr_scan_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {

        mTopBarView.setTitle(R.string.wallet_qr_scan_title);
        mTopBarView.setRightText(R.string.wallet_qr_scan_album);
        mTopBarView.setRightTextClickListener(v -> prePhoto());
        mTopBarView.serAllColor(Color.WHITE);
        mTopBarView.setBackgroundColor(Color.TRANSPARENT);

        mLightView = findViewById(R.id.scanner_light);
        mLightView.setOnClickListener((v) -> openLight());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWallet = getIntent().getParcelableExtra(Constant.KEY_WALLET);
        mAccountType = getIntent().getIntExtra(Constant.KEY_ACCOUNT_TYPE, 0);
        mContractAddress = getIntent().getStringExtra(Constant.KEY_TOKEN_ADDRESS);
        mIsCaptureResult = getIntent().getBooleanExtra(Constant.KEY_CAPTURE_FOR_RESULT, false);
        mIsOnlyScan = getIntent().getBooleanExtra(Constant.KEY_CAPTURE_ONLY_SCAN, false);
        mIsWatchScan = getIntent().getBooleanExtra(Constant.KEY_CAPTURE_WATCH, false);

        mZBarView = findViewById(R.id.zbarview);
        mZBarView.setDelegate(this);
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        try {
            handleDecode(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {

    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
        mZBarView.startSpotAndShowRect(); // 显示扫描框，并开始识别
    }

    @Override
    protected void onStart() {
        super.onStart();
        mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
        mZBarView.startSpotAndShowRect(); // 显示扫描框，并开始识别
    }

    @Override
    protected void onStop() {
        mZBarView.stopCamera(); // 关闭摄像头预览，并且隐藏扫描框
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
            mDisposable = null;
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mZBarView.onDestroy(); // 销毁二维码扫描控件
        super.onDestroy();
    }

    /**
     * 扫描完成
     */
    public void handleDecode(String resultString) {
        if (TextUtils.isEmpty(resultString)) {
            MyToast.showSingleToastShort(CaptureActivity.this, R.string.wallet_qr_scan_fail);
        } else {
            if (mIsWatchScan) {
                //观察钱包
                if (mAccountType != -1) {
                    if ((mAccountType == Constant.ACCOUNT_TYPE_QKC && QWWalletUtils.isQKCValidAddress(resultString))
                            || (mAccountType == Constant.ACCOUNT_TYPE_TRX && TronWalletClient.isTronAddressValid(resultString))
                            || (mAccountType == Constant.ACCOUNT_TYPE_ETH && WalletUtils.isValidAddress(resultString))) {
                        Intent intent = getIntent();
                        intent.putExtra(Constant.WALLET_ADDRESS, resultString);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                        return;
                    } else {
                        MyToast.showSingleToastShort(CaptureActivity.this, getErrorText());
                    }
                } else {
                    if (QWWalletUtils.isQKCValidAddress(resultString)
                            || TronWalletClient.isTronAddressValid(resultString)
                            || WalletUtils.isValidAddress(resultString)) {
                        Intent intent = getIntent();
                        intent.putExtra(Constant.WALLET_ADDRESS, resultString);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                        return;
                    } else {
                        MyToast.showSingleToastShort(CaptureActivity.this, R.string.wallet_transaction_to_address_error);
                    }
                }
            } else {
                if (resultString.startsWith(Constant.QR_ETH_TITLE)) {
                    //ETH
                    if (mAccountType == Constant.ACCOUNT_TYPE_ETH) {
                        //钱包是ETH币种
                        paresETHAddress(resultString);
                        return;
                    } else {
                        MyToast.showSingleToastShort(CaptureActivity.this, getErrorText());
                    }
                } else if (resultString.startsWith(Constant.QR_QKC_TITLE)) {
                    //QKC
                    if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
                        //钱包是QKC币种
                        paresQKCAddress(resultString);
                        return;
                    } else {
                        MyToast.showSingleToastShort(CaptureActivity.this, getErrorText());
                    }
                } else if (resultString.startsWith(Constant.QR_TRX_TITLE)) {
                    //TRX
                    if (mAccountType == Constant.ACCOUNT_TYPE_TRX) {
                        //钱包是TRX币种
                        paresTRXAddress(resultString);
                        return;
                    } else {
                        MyToast.showSingleToastShort(CaptureActivity.this, getErrorText());
                    }
                } else if (resultString.startsWith("{")) {
                    //兼容旧版本数据
                    try {
                        JSONObject jsonObject = new JSONObject(resultString);
                        String address = jsonObject.getString("address");
                        //token
                        switch (mAccountType) {
                            case Constant.ACCOUNT_TYPE_QKC:
                                //qkc
                                if (!QWWalletUtils.isQKCValidAddress(address)) {
                                    errorScan();
                                    return;
                                }
                                break;
                            case Constant.ACCOUNT_TYPE_ETH:
                                //eth
                                if (!WalletUtils.isValidAddress(address)) {
                                    errorScan();
                                    return;
                                }
                                break;
                            case Constant.ACCOUNT_TYPE_TRX:
                                //trx
                                if (!TronWalletClient.isTronAddressValid(address)) {
                                    errorScan();
                                    return;
                                }
                                break;
                            default:
                                errorScan();
                                return;
                        }
                        sendTransaction(address, null);
                        return;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if ((mAccountType == Constant.ACCOUNT_TYPE_QKC && QWWalletUtils.isQKCValidAddress(resultString))
                        || (mAccountType == Constant.ACCOUNT_TYPE_TRX && TronWalletClient.isTronAddressValid(resultString))
                        || (mAccountType == Constant.ACCOUNT_TYPE_ETH && WalletUtils.isValidAddress(resultString))) {
                    //纯地址
                    //转账
                    sendTransaction(resultString, null);
                    return;
                } else {
                    MyToast.showSingleToastShort(CaptureActivity.this, getErrorText());
                }
            }
        }
        openScan();
    }

    private void paresETHAddress(String resultString) {
        //ETH
        String message = resultString.substring(Constant.QR_ETH_TITLE.length());
        if (WalletUtils.isValidAddress(message)) {
            //纯地址
            //转账
            sendTransaction(message, null);
            return;
        } else {
            int index = message.indexOf("?");
            index = (index == -1) ? message.indexOf("&") : index;
            String address = message.substring(0, Math.max(0, index));
            if (WalletUtils.isValidAddress(address)) {
                //只扫描地址
                if (mIsOnlyScan) {
                    sendTransaction(address, null);
                    return;
                }

                message = message.substring(index + 1);
                String contractAddress = "";
                String networkId = "";
                String value = "";
                String decimal = "";
                String amount = null;
                String[] list = message.split("&");
                for (String msg : list) {
                    if (msg.startsWith(Constant.QR_CONTRACT_ADDRESS)) {
                        contractAddress = msg.substring(Constant.QR_CONTRACT_ADDRESS.length());
                    } else if (msg.startsWith(Constant.QR_NETWORK_ID)) {
                        networkId = msg.substring(Constant.QR_NETWORK_ID.length());
                    } else if (msg.startsWith(Constant.QR_AMOUNT)) {
                        amount = msg.substring(Constant.QR_AMOUNT.length());
                    } else if (msg.startsWith(Constant.QR_VALUE)) {
                        value = msg.substring(Constant.QR_VALUE.length());
                    } else if (msg.startsWith(Constant.QR_DECIMAL)) {
                        decimal = msg.substring(Constant.QR_DECIMAL.length());
                    }
                }
                if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(decimal)) {
                    amount = QWWalletUtils.getIntTokenFromWei10(value, Convert.Unit.ADJUST.setWeiFactor(Integer.parseInt(decimal)));
                }

                //转TRC20Token
                if (!TextUtils.isEmpty(contractAddress)) {
                    //币种不匹配
                    if (!isMatchCoin(contractAddress)) {
                        errorMatchCoinScan();
                        return;
                    }

                    int network = parseNetworkId(networkId);
                    //带网络信息时，判断是否在支持网络
                    if (network != -1 &&
                            network != Constant.ETH_PUBLIC_PATH_MAIN_INDEX
                            && network != Constant.ETH_PUBLIC_PATH_KOVAN_INDEX
                            && network != Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX
                            && network != Constant.ETH_PUBLIC_PATH_RINKBY_INDEX) {
                        errorScan();
                        return;
                    }
                    //带网络信息时，判断是否在转账网络
                    if (network != -1 && Constant.sETHNetworkId != network) {
                        showCheckoutNetWorkDialog(network, address, contractAddress, amount);
                        return;
                    }

                    //转账
                    if (hasNotExist(contractAddress, (int) Constant.sETHNetworkId)) {
                        if (network == -1 && Constant.sETHNetworkId != Constant.ETH_PUBLIC_PATH_MAIN_INDEX) {
                            //不带网络ID，并且当前网络不是主网
                            showNetworkAddTokenDialog(address, contractAddress, getEthNetworkIdName((int) Constant.sETHNetworkId));
                            return;
                        }
                        showAddTokenDialog(address, contractAddress);
                    } else {
                        sendTokenTransaction(address, contractAddress, amount);
                    }
                    return;
                } else {
                    int network = parseNetworkId(networkId);
                    //带网络信息时，判断是否在支持网络
                    if (network != -1 &&
                            network != Constant.ETH_PUBLIC_PATH_MAIN_INDEX
                            && network != Constant.ETH_PUBLIC_PATH_KOVAN_INDEX
                            && network != Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX
                            && network != Constant.ETH_PUBLIC_PATH_RINKBY_INDEX) {
                        errorScan();
                        return;
                    }
                    //带网络信息时，判断是否在转账网络
                    if (network != -1 && Constant.sETHNetworkId != network) {
                        showCheckoutNetWorkDialog(network, address, "", amount);
                        return;
                    }
                    //转账
                    sendTransaction(address, amount);
                    return;
                }
            } else {
                MyToast.showSingleToastShort(CaptureActivity.this, getErrorText());
            }
        }

        openScan();
    }

    private void paresQKCAddress(String resultString) {
        //ETH
        String message = resultString.substring(Constant.QR_QKC_TITLE.length());
        if (QWWalletUtils.isQKCValidAddress(message)) {
            //纯地址
            //转账
            sendTransaction(message, null);
            return;
        } else {
            int index = message.indexOf("?");
            index = (index == -1) ? message.indexOf("&") : index;
            String address = message.substring(0, Math.max(0, index));
            if (QWWalletUtils.isQKCValidAddress(address)) {
                //只扫描地址
                if (mIsOnlyScan) {
                    sendTransaction(address, null);
                    return;
                }

                message = message.substring(index + 1);
                String contractAddress = "";
                String networkId = "";
                String value = "";
                String decimal = "";
                String amount = null;
                String[] list = message.split("&");
                for (String msg : list) {
                    if (msg.startsWith(Constant.QR_CONTRACT_ADDRESS)) {
                        contractAddress = msg.substring(Constant.QR_CONTRACT_ADDRESS.length());
                    } else if (msg.startsWith(Constant.QR_NETWORK_ID)) {
                        networkId = msg.substring(Constant.QR_NETWORK_ID.length());
                    } else if (msg.startsWith(Constant.QR_AMOUNT)) {
                        amount = msg.substring(Constant.QR_AMOUNT.length());
                    } else if (msg.startsWith(Constant.QR_VALUE)) {
                        value = msg.substring(Constant.QR_VALUE.length());
                    } else if (msg.startsWith(Constant.QR_DECIMAL)) {
                        decimal = msg.substring(Constant.QR_DECIMAL.length());
                    }
                }
                if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(decimal)) {
                    amount = QWWalletUtils.getIntTokenFromWei10(value, Convert.Unit.ADJUST.setWeiFactor(Integer.parseInt(decimal)));
                }

                //转QRC20Token
                if (!TextUtils.isEmpty(contractAddress)) {
                    //币种不匹配
                    if (!isMatchCoin(contractAddress)) {
                        errorMatchCoinScan();
                        return;
                    }

                    int network = parseNetworkId(networkId);
                    //带网络信息时，判断是否在支持网络
                    if (network != -1
                            && network != Constant.QKC_PUBLIC_MAIN_INDEX
                            && network != Constant.QKC_PUBLIC_DEVNET_INDEX) {
                        errorScan();
                        return;
                    }
                    //带网络信息时，判断是否在转账网络
                    if (network != -1 && Constant.sNetworkId.intValue() != network) {
                        showCheckoutNetWorkDialog(network, address, contractAddress, amount);
                        return;
                    }

                    //转账
                    if (hasNotExist(contractAddress, Constant.sNetworkId.intValue())) {
                        String tokenAddress = contractAddress;
                        if (QWWalletUtils.isQKCNativeTokenID(tokenAddress)) {
                            //native token 不支持直接添加id，需要转成name
                            tokenAddress = QWWalletUtils.convertTokenNum2Name(tokenAddress);
                        }
                        if (network == -1) {
                            //不带网络ID，并且当前网络不是主网
                            if (Constant.sNetworkId.intValue() != Constant.QKC_PUBLIC_MAIN_INDEX) {
                                showNetworkAddTokenDialog(address, tokenAddress, getQkcNetworkIdName(Constant.sNetworkId.intValue()));
                                return;
                            }
                        }
                        showAddTokenDialog(address, tokenAddress);
                    } else {
                        sendTokenTransaction(address, contractAddress, amount);
                    }
                    return;
                } else {
                    int network = parseNetworkId(networkId);
                    //带网络信息时，判断是否在支持网络
                    if (network != -1
                            && network != Constant.QKC_PUBLIC_MAIN_INDEX
                            && network != Constant.QKC_PUBLIC_DEVNET_INDEX) {
                        errorScan();
                        return;
                    }
                    //带网络信息时，判断是否在转账网络
                    if (network != -1 && Constant.sNetworkId.intValue() != network) {
                        showCheckoutNetWorkDialog(network, address, "", amount);
                        return;
                    }
                    //转账
                    sendTransaction(address, amount);
                    return;
                }
            } else {
                MyToast.showSingleToastShort(CaptureActivity.this, getErrorText());
            }
        }

        openScan();
    }

    private void paresTRXAddress(String resultString) {
        //ETH
        String message = resultString.substring(Constant.QR_TRX_TITLE.length());
        if (TronWalletClient.isTronAddressValid(message)) {
            //转账
            sendTransaction(message, null);
            return;
        } else {
            int index = message.indexOf("?");
            index = (index == -1) ? message.indexOf("&") : index;
            String address = message.substring(0, Math.max(0, index));
            if (TronWalletClient.isTronAddressValid(address)) {
                //只扫描地址
                if (mIsOnlyScan) {
                    sendTransaction(address, null);
                    return;
                }

                message = message.substring(index + 1);
                String contractAddress = "";
                String value = "";
                String decimal = "";
                String amount = null;
                String[] list = message.split("&");
                for (String msg : list) {
                    if (msg.startsWith(Constant.QR_CONTRACT_ADDRESS)) {
                        contractAddress = msg.substring(Constant.QR_CONTRACT_ADDRESS.length());
                    } else if (msg.startsWith(Constant.QR_AMOUNT)) {
                        amount = msg.substring(Constant.QR_AMOUNT.length());
                    } else if (msg.startsWith(Constant.QR_VALUE)) {
                        value = msg.substring(Constant.QR_VALUE.length());
                    } else if (msg.startsWith(Constant.QR_DECIMAL)) {
                        decimal = msg.substring(Constant.QR_DECIMAL.length());
                    }
                }

                if (!TextUtils.isEmpty(value) && !TextUtils.isEmpty(decimal)) {
                    amount = QWWalletUtils.getIntTokenFromWei10(value, Convert.Unit.ADJUST.setWeiFactor(Integer.parseInt(decimal)));
                }

                //转QRC20Token
                if (!TextUtils.isEmpty(contractAddress)) {
                    //币种不匹配
                    if (!isMatchCoin(contractAddress)) {
                        errorMatchCoinScan();
                        return;
                    }

                    //转账
                    if (hasNotExist(contractAddress, Constant.TRX_MAIN_NETWORK)) {
                        showAddTokenDialog(address, contractAddress);
                    } else {
                        sendTokenTransaction(address, contractAddress, amount);
                    }
                    return;
                } else {
                    //转账
                    sendTransaction(address, amount);
                    return;
                }
            } else {
                MyToast.showSingleToastShort(CaptureActivity.this, getErrorText());
            }
        }

        openScan();
    }

    private int parseNetworkId(String networkId) {
        if (TextUtils.isEmpty(networkId)) {
            return -1;
        }

        try {
            return Integer.parseInt(networkId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void sendTransaction(String address, String amount) {
        if (mIsCaptureResult) {
            Intent intent = getIntent();
            intent.putExtra(Constant.WALLET_ADDRESS, address);
            if (!TextUtils.isEmpty(amount)) {
                intent.putExtra(Constant.KEY_BALANCE, amount);
            }
            setResult(Activity.RESULT_OK, intent);
            finish();
            return;
        }
        Intent intent = new Intent(this, TransactionCreateActivity.class);
        intent.putExtra(Constant.WALLET_ADDRESS, address);
        if (!TextUtils.isEmpty(amount)) {
            intent.putExtra(Constant.KEY_BALANCE, amount);
        }
        startActivity(intent);
        finish();
    }

    private void sendTokenTransaction(String address, String tokenAddress, String amount) {
        if (mIsCaptureResult) {
            Intent intent = getIntent();
            intent.putExtra(Constant.WALLET_ADDRESS, address);
            if (!TextUtils.isEmpty(amount)) {
                intent.putExtra(Constant.KEY_BALANCE, amount);
            }
            setResult(Activity.RESULT_OK, intent);
            finish();
            return;
        }
        Intent intent = new Intent(this, TransactionCreateActivity.class);
        intent.putExtra(Constant.WALLET_ADDRESS, address);
        intent.putExtra(Constant.KEY_TOKEN_ADDRESS, tokenAddress);
        if (!TextUtils.isEmpty(amount)) {
            intent.putExtra(Constant.KEY_BALANCE, amount);
        }
        startActivity(intent);
        finish();
    }

    private boolean isMatchCoin(String tokenAddress) {
        //不是从Token转账界面进入，从首页进入，不进行判定
        if (!mIsCaptureResult) {
            return true;
        }

        tokenAddress = tokenAddress != null ? tokenAddress.toLowerCase() : "";
        mContractAddress = mContractAddress != null ? mContractAddress.toLowerCase() : "";
        //币种不匹配
        return TextUtils.equals(tokenAddress, mContractAddress);
    }

    //币种不匹配
    private void errorMatchCoinScan() {
        final QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage(R.string.qr_coin_not_match);
        dialog.setPositiveBtn(R.string.ok, (v) -> dialog.dismiss());
        dialog.setOnDismissListener((v) -> openScan());
        dialog.show();
    }

    private void errorScan() {
        MyToast.showSingleToastShort(CaptureActivity.this, getErrorText());
        openScan();
    }

    private boolean hasNotExist(String address, int networkId) {
        QWTokenDao dao = new QWTokenDao(getApplicationContext());
        QWToken token = dao.queryTokenByAddress(address);
        return token == null || token.getChainId() != networkId;
    }

    private String getErrorText() {
        int name = R.string.import_wallet_qkc;
        switch (mAccountType) {
            case Constant.ACCOUNT_TYPE_ETH:
                name = R.string.import_wallet_eth;
                break;
            case Constant.ACCOUNT_TYPE_TRX:
                name = R.string.import_wallet_trx;
                break;
        }
        return String.format(getString(R.string.import_wallet_fail_watch), getString(name));
    }

    private void showNetworkAddTokenDialog(String address, String tokenAddress, String testName) {
        String msg = String.format(getString(R.string.add_token_dialog_network_msg), testName, testName);
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setTitle(R.string.add_token_dialog_network_title);
        dialog.setMessage(msg);
        dialog.setPositiveBtn(R.string.ok, v -> {
            Intent intent = new Intent(this, AddTokenActivity.class);
            intent.putExtra(Constant.WALLET_ADDRESS, address);
            intent.putExtra(Constant.KEY_TOKEN_ADDRESS, tokenAddress);
            intent.putExtra(Constant.KEY_WALLET, mWallet);
            startActivity(intent);
            CaptureActivity.this.finish();
            dialog.dismiss();
        });
        dialog.setNegativeBtn(R.string.cancel, v -> {
            dialog.dismiss();
            openScan();
        });
        dialog.show();
    }

    private void showAddTokenDialog(String address, String tokenAddress) {
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setTitle(R.string.add_token_dialog_title);
        dialog.setMessage(R.string.add_token_dialog_msg);
        dialog.setPositiveBtn(R.string.ok, v -> {
            Intent intent = new Intent(this, AddTokenActivity.class);
            intent.putExtra(Constant.WALLET_ADDRESS, address);
            intent.putExtra(Constant.KEY_TOKEN_ADDRESS, tokenAddress);
            intent.putExtra(Constant.KEY_WALLET, mWallet);
            startActivity(intent);
            CaptureActivity.this.finish();
            dialog.dismiss();
        });
        dialog.setNegativeBtn(R.string.cancel, v -> {
            dialog.dismiss();
            openScan();
        });
        dialog.show();
    }

    private void showCheckoutNetWorkDialog(int networkId, String address, String tokenAddress, String amount) {
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setTitle(R.string.switch_network_title);
        dialog.setMessage(getNetworkString(networkId));
        dialog.setPositiveBtn(R.string.ok, v -> {
            switch (mAccountType) {
                case Constant.ACCOUNT_TYPE_QKC:
                    //qkc
                    SharedPreferencesUtils.setQKCNetworkIndex(getApplicationContext(), networkId);
                    MainApplication.initQKCPath();
                    break;
                case Constant.ACCOUNT_TYPE_ETH:
                    SharedPreferencesUtils.setEthNetworkIndex(getApplicationContext(), networkId);
                    MainApplication.initEthPath();
                    break;
            }

            if (!TextUtils.isEmpty(tokenAddress)) {
                if (hasNotExist(tokenAddress, networkId)) {
                    showAddTokenDialog(address, tokenAddress);
                } else {
                    sendTokenTransaction(address, tokenAddress, amount);
                }
            } else {
                sendTransaction(address, amount);
            }

            NetworkChangeEvent messageEvent = new NetworkChangeEvent("");
            EventBus.getDefault().postSticky(messageEvent);
            dialog.dismiss();
        });
        dialog.setNegativeBtn(R.string.cancel, v -> {
            dialog.dismiss();
            openScan();
        });
        dialog.show();
    }

    private String getNetworkString(int chainId) {
        if (mAccountType == Constant.ACCOUNT_TYPE_ETH) {
            return String.format(getString(R.string.switch_network_message), getEthNetworkIdName(chainId), getEthNetworkIdName((int) Constant.sETHNetworkId));
        } else {
            return String.format(getString(R.string.switch_network_message), getQkcNetworkIdName(chainId), getQkcNetworkIdName((int) Constant.sETHNetworkId));
        }
    }

    private String getEthNetworkIdName(int chainId) {
        switch (chainId) {
            case Constant.ETH_PUBLIC_PATH_MAIN_INDEX:
                return Constant.ETH_PUBLIC_PATH_MAIN_NAME;
            case Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX:
                return Constant.ETH_PUBLIC_PATH_ROPSTEN_NAME;
            case Constant.ETH_PUBLIC_PATH_KOVAN_INDEX:
                return Constant.ETH_PUBLIC_PATH_KOVAN_NAME;
            case Constant.ETH_PUBLIC_PATH_RINKBY_INDEX:
                return Constant.ETH_PUBLIC_PATH_RINKBY_NAME;
        }
        return "";
    }


    private String getQkcNetworkIdName(int chainId) {
        switch (chainId) {
            case Constant.QKC_PUBLIC_MAIN_INDEX:
                return Constant.ETH_PUBLIC_PATH_MAIN_NAME;
            case Constant.QKC_PUBLIC_DEVNET_INDEX:
                return Constant.QKC_PUBLIC_PATH_DEVNET_NAME;
        }
        return "";
    }

    /**
     * 重新启动扫码和解码器
     */
    public void openScan() {
        mLightView.postDelayed(() -> mZBarView.startSpot(), 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //获取图片路径
        if (requestCode == Constant.REQUEST_CODE_SWITCH_QR_BITMAP && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                String[] filePathColumns = {MediaStore.Images.Media.DATA};
                Cursor c = null;
                String imagePath = "";
                try {
                    c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
                    if (c != null) {
                        c.moveToFirst();
                        int columnIndex = c.getColumnIndex(filePathColumns[0]);
                        imagePath = c.getString(columnIndex);
                    }
                } catch (Exception e) {
                    //nothing
                } finally {
                    if (c != null && !c.isClosed()) {
                        c.close();
                    }
                }

                if (!TextUtils.isEmpty(imagePath)) {
                    final String path = imagePath;
                    mDisposable = Single.fromCallable(() -> EncodingUtils.scanningImage(path))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onDecodeSuccess, v -> onDecodeFail());
                }
            }
        }
    }


    private void openLight() {
        //获取到ZXing相机管理器创建的camera
        if (mZBarView == null) {
            return;
        }

        // 关灯
        if (mIsOpen) {
            mLightView.setText(R.string.wallet_qr_light_up);
            Drawable dra = getResources().getDrawable(R.drawable.sacn_light_up);
            dra.setBounds(0, 0, dra.getIntrinsicWidth(), dra.getIntrinsicHeight());
            mLightView.setCompoundDrawables(null, dra, null, null);

            mZBarView.closeFlashlight(); // 关闭闪光灯
            mIsOpen = false;
        } else {  // 开灯
            mLightView.setText(R.string.wallet_qr_light_down);
            Drawable dra = getResources().getDrawable(R.drawable.sacn_light_down);
            dra.setBounds(0, 0, dra.getIntrinsicWidth(), dra.getIntrinsicHeight());
            mLightView.setCompoundDrawables(null, dra, null, null);

            mZBarView.openFlashlight(); // 打开闪光灯
            mIsOpen = true;
        }
    }

    private void onDecodeSuccess(Result result) {
        if (result == null) {
            MyToast.showSingleToastShort(CaptureActivity.this, R.string.wallet_qr_scan_fail);
            return;
        }
        handleDecode(result.toString());
    }

    private void onDecodeFail() {
        MyToast.showSingleToastShort(CaptureActivity.this, R.string.wallet_qr_scan_fail);
    }

    private void choosePhoto() {
        Intent intentToPickPic = new Intent(Intent.ACTION_PICK, null);
        // 如果限制上传到服务器的图片类型时可以直接写如："image/jpeg 、 image/png等的类型" 所有类型则写 "image/*"
        intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intentToPickPic, Constant.REQUEST_CODE_SWITCH_QR_BITMAP);
    }

    private void prePhoto() {
        final RxPermissions rxPermissions = new RxPermissions(this);
        if (!rxPermissions.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE) || !rxPermissions.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Disposable disposable = rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe(aBoolean -> {
                        if (aBoolean) {
                            choosePhoto();
                        } else {
                            String[] name = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                            MyToast.showSingleToastLong(this, PermissionHelper.getPermissionToast(getApplicationContext(), name));
                        }
                    });
            return;
        }

        choosePhoto();
    }
}