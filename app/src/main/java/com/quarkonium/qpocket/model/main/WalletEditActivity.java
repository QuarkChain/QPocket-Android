package com.quarkonium.qpocket.model.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWWalletDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.entity.ErrorEnvelope;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.model.main.bean.WalletManagerBean;
import com.quarkonium.qpocket.model.main.viewmodel.EditWalletViewModel;
import com.quarkonium.qpocket.model.main.viewmodel.EditWalletViewModelFactory;
import com.quarkonium.qpocket.model.wallet.BackupPhraseHintActivity;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.CircleImageView;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkNameDialog;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.view.QuarkThreeButtonDialog;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

//钱包编辑界面
public class WalletEditActivity extends BaseActivity {

    public static void startActivity(Activity activity, WalletManagerBean bean) {
        Intent intent = new Intent(activity, WalletEditActivity.class);
        intent.putExtra(WalletEditActivity.KEY_WALLET, bean.getWallet());
        intent.putExtra(WalletEditActivity.KEY_ACCOUNT, bean.getAccount());
        activity.startActivityForResult(intent, Constant.REQUEST_CODE_EDIT_WALLET);
    }

    public boolean showGuardTipsForWatchAccountIfNeeded(QWWallet wallet) {
        if (wallet == null || wallet.getIsWatch() == 0) {
            return false;
        }
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setTitle(wallet.isLedger() ? R.string.wallet_ledger_error_title : R.string.wallet_watch_error_title);
        dialog.setMessage(wallet.isLedger() ? R.string.wallet_ledger_error_message : R.string.wallet_watch_error_message);
        dialog.setPositiveBtn(R.string.ok, v -> dialog.dismiss());
        dialog.show();
        return true;
    }

    public static final String KEY_WALLET = "key_wallet";
    public static final String KEY_ACCOUNT = "key_account";

    private QWWallet mQuarkWallet;
    private QWAccount mCurrentAccount;

    @Inject
    EditWalletViewModelFactory mExportFactory;
    EditWalletViewModel mEditWalletViewModel;

    private View mProgressLayout;
    private TextView mNameView;
    private CircleImageView mIconImageView;
    private boolean mIsChange;
    private boolean mWalletHasBalance;
    private boolean mAccountHasBalance;

    private TextView mAddressText;
    private TextView mBTCSegWit;


    @Override
    protected int getLayoutResource() {
        return R.layout.activity_edit_wallet;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_edit_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        if (mQuarkWallet == null || mCurrentAccount == null) {
            return;
        }

        mProgressLayout = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressLayout, UiUtils.dpToPixel(4));

        mTopBarView.setTitle(R.string.wallet_edit_title);

        findViewById(R.id.edit_icon_layout).setOnClickListener((v) -> onEditIcon());
        findViewById(R.id.edit_name_layout).setOnClickListener((v) -> onEditName());

        findViewById(R.id.export_phrase_btn).setOnClickListener((v) -> onExportPhrase());
        findViewById(R.id.export_ks_btn).setOnClickListener((v) -> onExportKeystore());
        findViewById(R.id.export_pk_btn).setOnClickListener((v) -> onExportPrivateKey());

        findViewById(R.id.delete_account_action).setOnClickListener((v) -> onDeleteWallet());

        mNameView = findViewById(R.id.wallet_name_text);
        mNameView.setText(mCurrentAccount.getName());

        mAddressText = findViewById(R.id.wallet_address_text);
        mAddressText.setText(QWWallet.getCurrentShowAddress(mCurrentAccount.getAddress()));

        //设置头像
        mIconImageView = findViewById(R.id.wallet_icon);
        Glide.with(this)
                .asBitmap()
                .load(mCurrentAccount.getIcon())
                .into(mIconImageView);


        if (Constant.WALLET_TYPE_HD != mQuarkWallet.getType()) {
            //非HD钱包
            findViewById(R.id.export_phrase_btn).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        mQuarkWallet = getIntent().getParcelableExtra(KEY_WALLET);
        mCurrentAccount = getIntent().getParcelableExtra(KEY_ACCOUNT);
        if (savedInstanceState != null) {
            mQuarkWallet = savedInstanceState.getParcelable(KEY_WALLET);
            mCurrentAccount = savedInstanceState.getParcelable(KEY_ACCOUNT);
        }
        super.onCreate(savedInstanceState);

        mEditWalletViewModel = new ViewModelProvider(this, mExportFactory)
                .get(EditWalletViewModel.class);
        mEditWalletViewModel.getKeystoreData().observe(this, this::exportKeyStoreSuccess);
        mEditWalletViewModel.getPhraseData().observe(this, this::exportPhraseSuccess);
        mEditWalletViewModel.getPrivateKeyData().observe(this, this::exportPrivateKeySuccess);
        mEditWalletViewModel.getDeleteData().observe(this, this::deleteSuccess);
        mEditWalletViewModel.progress().observe(this, this::showProgress);
        mEditWalletViewModel.error().observe(this, this::onError);

        mEditWalletViewModel.walletHasBalance().observe(this, this::onWalletBalance);
        mEditWalletViewModel.accountHasBalance().observe(this, this::onAccountBalance);
        //查询account是否有钱
        mEditWalletViewModel.checkBalance(getApplicationContext(), mCurrentAccount);
        if (mQuarkWallet.getType() == Constant.WALLET_TYPE_HD) {
            //查询整个wallet是否有钱
            mEditWalletViewModel.checkBalance(getApplicationContext(), mQuarkWallet);
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        //重启时更新钱包数据
        outState.putParcelable(KEY_WALLET, mQuarkWallet);
        outState.putParcelable(KEY_ACCOUNT, mCurrentAccount);
    }

    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mQuarkWallet = savedInstanceState.getParcelable(KEY_WALLET);
        mCurrentAccount = savedInstanceState.getParcelable(KEY_ACCOUNT);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mQuarkWallet != null) {
            mQuarkWallet.setIsBackup(1);
            mIsChange = true;
        }
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressLayout.setVisibility(View.VISIBLE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
        }
    }

    private void onWalletBalance(boolean has) {
        mWalletHasBalance = has;
    }

    private void onAccountBalance(boolean has) {
        mAccountHasBalance = has;
    }

    private void onError(ErrorEnvelope envelope) {

        MyToast.showSingleToastShort(this, R.string.password_error);
        showProgress(false);

        Constant.sPasswordHintMap.put(mQuarkWallet.getKey(), mQuarkWallet.getHint());
    }

    private void onEditIcon() {
        Intent intent = new Intent(this, WalletEditSettingIconActivity.class);
        intent.putExtra(Constant.KEY_WALLET_ICON_PATH, mCurrentAccount.getIcon());
        startActivityForResult(intent, Constant.REQUEST_CODE_EDIT_ICON);

        UmengStatistics.editWalletIconClickCount(getApplicationContext(), mCurrentAccount.getShardAddress());
    }

    private void onEditName() {
        QuarkNameDialog dialog = new QuarkNameDialog(this);
        dialog.setText(mCurrentAccount.getName());
        dialog.setOnClickListener((v) -> {
            String name = dialog.getText();
            if (TextUtils.equals(name, mCurrentAccount.getName())) {
                dialog.dismiss();
                return;
            }

            QWAccountDao qwAccountDao = new QWAccountDao(getApplicationContext());
            qwAccountDao.updateAccountName(name, mCurrentAccount.getAddress());

            mNameView.setText(name);
            mCurrentAccount.setName(name);
            mIsChange = true;

            MyToast.showSingleToastShort(WalletEditActivity.this, R.string.wallet_edit_change_name_success);

            UmengStatistics.editWalletNameClickCount(getApplicationContext(), mCurrentAccount.getAddress());
            dialog.dismiss();
        });
        dialog.show();
    }

    //备份助记词
    private void onExportPhrase() {
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mQuarkWallet, new SystemUtils.OnCheckPassWordListenerImp() {
            @Override
            public void onPasswordSuccess(String password) {
                mEditWalletViewModel.exportPhrase(mQuarkWallet, password, password);
            }
        });
    }

    private void onExportKeystore() {
        if (showGuardTipsForWatchAccountIfNeeded(mQuarkWallet)) {
            return;
        }
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mQuarkWallet, new SystemUtils.OnCheckPassWordListenerImp() {
            @Override
            public void onPasswordSuccess(String password) {
                mEditWalletViewModel.exportKeystore(mCurrentAccount, password);
            }
        });
    }

    private void onExportPrivateKey() {
        if (showGuardTipsForWatchAccountIfNeeded(mQuarkWallet)) {
            return;
        }
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mQuarkWallet, new SystemUtils.OnCheckPassWordListenerImp() {
            @Override
            public void onPasswordSuccess(String password) {
                mEditWalletViewModel.exportPrivateKey(mCurrentAccount, password, password);
            }
        });
    }

    //删除钱包
    public void onDeleteWallet() {
        if (ToolUtils.isFastDoubleClick(400)) {
            return;
        }

        //获取钱包数量
        QWWalletDao dao = new QWWalletDao(getApplicationContext());
        int walletSize = dao.querySize();
        if (Constant.WALLET_TYPE_HD == mQuarkWallet.getType()) {
            //HD钱包
            //查询该钱包account数量
            QWAccountDao accountDao = new QWAccountDao(getApplicationContext());
            int accountSize = accountDao.querySizeByKey(mQuarkWallet.getKey());
            if (accountSize == 2) {
                //当只剩2个account时，确定这两个account是否为同一个btc的隔离见证和非隔离见证
                List<QWAccount> accountList = accountDao.queryByKey(mQuarkWallet.getKey());
                if (accountList != null && accountList.size() == 2) {
                    QWAccount account1 = accountList.get(0);
                    QWAccount account2 = accountList.get(1);
                    if (account1.isAllBTC() && account2.isAllBTC()
                            && account1.getBitCoinIndex() == account2.getBitCoinIndex()) {
                        //该钱包是btc的隔离见证和非隔离见证
                        accountSize = accountSize - 1;
                    }
                }
            }
            if (walletSize <= 1) {
                //只剩最后一个钱包时
                if (accountSize <= 1) {
                    //最后一个HD钱包只剩最后一个account时，不让删除
                    MyToast.showSingleToastShort(this, R.string.delete_wallet_only_one);
                } else {
                    checkDeleteAccount();
                }
                return;
            }

            if (accountSize <= 1) {
                //该HD钱包只剩一个account时，不再弹删除HD的提示
                checkDeleteAccount();
            } else {
                onDeleteHDWalletDialog();
            }
        } else {
            //非HD钱包
            //最后一个钱包不能删除
            if (walletSize <= 1) {
                MyToast.showSingleToastShort(this, R.string.delete_wallet_only_one);
                return;
            }
            checkDeleteAccount();
        }
    }

    //校验hd钱包余额
    private void onDeleteHDWalletDialog() {
        final QuarkThreeButtonDialog dialog = new QuarkThreeButtonDialog(this);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage(R.string.delete_wallet_message);
        dialog.setPositiveBtn(R.string.delete_wallet_sub, (v) -> {
            dialog.dismiss();
            checkDeleteAccount();
        });
        dialog.setPositiveBtn2(R.string.delete_wallet_hd, (v) -> {
            dialog.dismiss();
            checkDeleteHDWallet();
        });
        dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
        dialog.show();
    }

    //校验account余额
    private void checkDeleteAccount() {
        //该account有余额
        if (mAccountHasBalance) {
            final QuarkSDKDialog dialog = new QuarkSDKDialog(this);
            dialog.setTitle(R.string.delete_wallet_amount_title);
            dialog.setMessage(R.string.delete_wallet_amount_message);
            dialog.setPositiveBtn(R.string.delete, v -> {
                deleteAccount();
                dialog.dismiss();
            });
            dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
            dialog.show();
            return;
        }
        deleteAccount();
    }

    private void checkDeleteHDWallet() {
        //该HD钱包下任何一个account有余额
        if (mWalletHasBalance) {
            final QuarkSDKDialog dialog = new QuarkSDKDialog(this);
            dialog.setTitle(R.string.delete_wallet_amount_title);
            dialog.setMessage(R.string.delete_wallet_amount_message);
            dialog.setPositiveBtn(R.string.delete, v -> {
                deleteHD();
                dialog.dismiss();
            });
            dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
            dialog.show();
            return;
        }
        deleteHD();
    }

    //真正删除HD Wallet
    private void deleteHD() {
        //正常钱包，校验密码
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mQuarkWallet, new SystemUtils.OnCheckPassWordListenerImp() {
            @Override
            public void onPasswordSuccess(String password) {
                mEditWalletViewModel.deleteData(getApplicationContext(), mQuarkWallet, password);
            }
        });
        UmengStatistics.editWalletDeleteClickCount(getApplicationContext(), mCurrentAccount.getAddress());
    }

    //真正删除account
    private void deleteAccount() {
        //观察钱包，不需要校验密码
        if (mQuarkWallet.isWatch()) {
            mEditWalletViewModel.deleteWatchWalletData(getApplicationContext(), mQuarkWallet);
            UmengStatistics.editWalletDeleteClickCount(getApplicationContext(), mCurrentAccount.getAddress());
            return;
        }

        //正常钱包，校验密码
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mQuarkWallet, new SystemUtils.OnCheckPassWordListenerImp() {
            @Override
            public void onPasswordSuccess(String password) {
                mEditWalletViewModel.deleteAccountData(getApplicationContext(), mCurrentAccount, mQuarkWallet.getCurrentAddress(), password);
            }
        });
        UmengStatistics.editWalletDeleteClickCount(getApplicationContext(), mCurrentAccount.getAddress());
    }

    private void exportKeyStoreSuccess(String keystore) {
        Constant.sPasswordHintMap.put(mQuarkWallet.getKey(), "");
        Intent intent = new Intent(this, WalletExportSuccessActivity.class);
        intent.putExtra(WalletExportSuccessActivity.KEY_PASSWORD, keystore);
        boolean isHD = mQuarkWallet.getType() == Constant.WALLET_TYPE_HD;
        intent.putExtra(WalletExportSuccessActivity.KEY_IS_HD, isHD);
        intent.putExtra(WalletExportSuccessActivity.KEY_TYPE, WalletExportSuccessActivity.KEY_TYPE_KEYSTORE);
        startActivity(intent);

        UmengStatistics.editWalletExportKeystoreClickCount(getApplicationContext(), mCurrentAccount.getAddress());
    }

    private void exportPrivateKeySuccess(String privateKey) {
        Constant.sPasswordHintMap.put(mQuarkWallet.getKey(), "");
        Intent intent = new Intent(this, WalletExportSuccessActivity.class);
        intent.putExtra(WalletExportSuccessActivity.KEY_PASSWORD, privateKey);
        boolean isHD = mQuarkWallet.getType() == Constant.WALLET_TYPE_HD;
        intent.putExtra(WalletExportSuccessActivity.KEY_IS_HD, isHD);
        intent.putExtra(WalletExportSuccessActivity.KEY_TYPE, WalletExportSuccessActivity.KEY_TYPE_PRIVATE_KEY);
        startActivity(intent);

        UmengStatistics.editWalletExportPrivateClickCount(getApplicationContext(), mCurrentAccount.getAddress());
    }

    private void exportPhraseSuccess(String phrase) {
        Constant.sPasswordHintMap.put(mQuarkWallet.getKey(), "");
        Intent intent = new Intent(this, BackupPhraseHintActivity.class);
        intent.putExtra(Constant.WALLET_KEY, mQuarkWallet.getKey());
        intent.putExtra(Constant.IS_EXPORT_PHRASE, true);
        startActivity(intent);

        UmengStatistics.editWalletExportPhraseClickCount(getApplicationContext(), mCurrentAccount.getAddress());
    }

    //删除成功
    private void deleteSuccess(QWWallet wallet) {
        if (wallet != null) {
            Intent intent = getIntent();
            intent.putExtra(Constant.KEY_WALLET, mQuarkWallet);
            intent.putExtra(Constant.KEY_DELETE_WALLET, true);
            intent.putExtra(Constant.KEY_ACCOUNT, mCurrentAccount);
            if (!TextUtils.isEmpty(wallet.getKey())) {
                //切换钱包
                intent.putExtra(Constant.KEY_CHOOSE_NEW_WALLET, true);
                intent.putExtra(Constant.KEY_CHOOSE_WALLET, wallet);
            }
            setResult(RESULT_OK, intent);
            super.finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_CODE_EDIT_ICON && resultCode == RESULT_OK) {
            String icon = data.getStringExtra(Constant.KEY_WALLET_ICON_PATH);
            mCurrentAccount.setIcon(icon);
            mQuarkWallet.setIcon(icon);
            QWAccountDao accountDao = new QWAccountDao(getApplicationContext());
            accountDao.updateWalletIcon(icon, mQuarkWallet.getKey());
            QWWalletDao walletDao = new QWWalletDao(getApplicationContext());
            walletDao.updateWalletIcon(icon, mQuarkWallet.getKey());

            Glide.with(this)
                    .asBitmap()
                    .load(mCurrentAccount.getIcon())
                    .into(mIconImageView);

            mIsChange = true;
        }
    }

    @Override
    public void finish() {
        if (mIsChange) {
            Intent intent = getIntent();
            intent.putExtra(Constant.KEY_WALLET, mQuarkWallet);
            setResult(RESULT_OK, intent);
        }
        super.finish();
    }
}
