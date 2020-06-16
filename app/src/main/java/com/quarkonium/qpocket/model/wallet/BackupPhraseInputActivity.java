package com.quarkonium.qpocket.model.wallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWWalletDao;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.model.main.MainActivity;
import com.quarkonium.qpocket.model.main.WalletEditActivity;
import com.quarkonium.qpocket.model.wallet.viewmodel.CreateWalletViewModel;
import com.quarkonium.qpocket.model.wallet.viewmodel.CreateWalletViewModelFactory;
import com.quarkonium.qpocket.rx.RxBus;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.FlowLayout;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

//输入确认钱包助记词界面
public class BackupPhraseInputActivity extends BaseActivity {

    @Inject
    CreateWalletViewModelFactory mWalletViewModelFactory;
    CreateWalletViewModel mViewModel;

    private String mWalletKey;

    private String mMnemonic;
    private String mPassword;
    private String mPasswordHint;

    private boolean mIsExport;
    private boolean mIsResultBackup;

    private FlowLayout mEditText;
    private FlowLayout mLabelText;
    private LinkedList<FlowLayout.TextBean> mPhraseList = new LinkedList<>();
    private LinkedList<FlowLayout.TextBean> mLabelList = new LinkedList<>();
    private ArrayList<FlowLayout.TextBean> mPhraseRandomList;

    private View mActionButton;
    private TextView mErrorView;

    private View mProgressLayout;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_home_backup_input;
    }

    @Override
    public int getActivityTitle() {
        return R.string.backup_wallet_input_hint_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mWalletKey = getIntent().getStringExtra(Constant.WALLET_KEY);

        mIsExport = getIntent().getBooleanExtra(Constant.IS_EXPORT_PHRASE, false);
        mIsResultBackup = getIntent().getBooleanExtra(Constant.IS_RESULT_BACKUP_PHRASE, false);

        mTopBarView.setTitle(R.string.backup_wallet_input_hint_title);
        mTopBarView.setRightText(R.string.backup_wallet_skip);
        View skip = mTopBarView.getRightTextView();
        skip.setOnClickListener(v -> {
            createWallet();
            UmengStatistics.topBarCreateWalletSkipClickCount(getApplicationContext(), QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
        });
        if (mIsExport || mIsResultBackup) {
            skip.setVisibility(View.GONE);
        }

        mProgressLayout = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressLayout, UiUtils.dpToPixel(3));

        mActionButton = findViewById(R.id.account_action_next);
        mActionButton.setOnClickListener((v) -> checkPhrase());
        mActionButton.setEnabled(false);

        mErrorView = findViewById(R.id.error_layout);
        mErrorView.setText(R.string.backup_wallet_input_fail);

        mEditText = findViewById(R.id.backup_phrase_edit_text);
        mEditText.setTagListener(new FlowLayout.FlowTagClickListenerIml() {

            @Override
            public void onSelected(FlowLayout.TextBean bean) {
                //刷新编辑框
                mPhraseList.remove(bean);
                mEditText.removeAllViews();
                mEditText.setData(mPhraseList);

                //刷新label状态
                mLabelList.add(bean);
                checkOrder();
                mLabelText.removeAllViews();
                mLabelText.setData(mLabelList);

                if (!mLabelList.isEmpty()) {
                    mActionButton.setEnabled(false);
                }
                checkInput();
            }
        });

        mLabelText = findViewById(R.id.backup_phrase_edit_label);
        mLabelText.setTextBackgroundId(R.drawable.flow_layout_gray_bg);
        mLabelText.setTextColor(getResources().getColor(R.color.text_title));
        mLabelText.setTagListener(new FlowLayout.FlowTagClickListenerIml() {

            @Override
            public void onSelected(FlowLayout.TextBean bean) {

                //刷新label状态
                mLabelList.remove(bean);
                mLabelText.removeAllViews();
                mLabelText.setData(mLabelList);

                //刷新编辑框
                mPhraseList.add(bean);
                mEditText.removeAllViews();
                mEditText.setData(mPhraseList);

                if (mLabelList.isEmpty() && !checkInputFail()) {
                    mActionButton.setEnabled(true);
                }
                checkInput();
            }
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        mMnemonic = getIntent().getStringExtra(Constant.KEY_MNEMONIC);
        mPassword = getIntent().getStringExtra(Constant.KEY_PASSWORD);
        mPasswordHint = getIntent().getStringExtra(Constant.KEY_PASSWORD_HINT);

        mViewModel = new ViewModelProvider(this, mWalletViewModelFactory)
                .get(CreateWalletViewModel.class);
        mViewModel.createdWallet().observe(this, v -> goMain());
        mViewModel.error().observe(this, v -> onError());
        mViewModel.progress().observe(this, this::showProgress);

        findWalterSuccess();
    }

    private void findWalterSuccess() {
        if (!TextUtils.isEmpty(mMnemonic)) {
            String[] phases = mMnemonic.split(" ");
            String[] list = randomString(phases);
            mPhraseRandomList = new ArrayList<>();
            for (int i = 0, size = list.length; i < size; i++) {
                FlowLayout.TextBean bean = new FlowLayout.TextBean(i, list[i]);
                mPhraseRandomList.add(bean);
            }

            //刷新UI
            mLabelList.clear();
            mLabelList.addAll(mPhraseRandomList);
            mLabelText.setData(mLabelList);
        }
    }

    //对数组随机排序
    public String[] randomString(String[] arr) {
        String[] arr2 = new String[arr.length];
        int count = arr.length;
        int cbRandCount = 0;// 索引
        int cbPosition;// 位置
        int k = 0;
        Random rand = new Random();
        do {
            int r = count - cbRandCount;
            cbPosition = rand.nextInt(r);
            arr2[k++] = arr[cbPosition];
            cbRandCount++;
            arr[cbPosition] = arr[r - 1];// 将最后一位数值赋值给已经被使用的cbPosition
        } while (cbRandCount < count);
        return arr2;
    }

    private void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void goExport() {
        Intent intent = new Intent(this, WalletEditActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void checkPhrase() {
        if (TextUtils.isEmpty(mMnemonic)) {
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (FlowLayout.TextBean text : mPhraseList) {
            stringBuilder.append(text.getText());
        }
        String randomStr = stringBuilder.toString();
        String phase = mMnemonic.replaceAll(" ", "");
        if (randomStr.equals(phase)) {
            if (mIsExport) {
                //更新备份状态
                QWWalletDao walletDao = new QWWalletDao(getApplicationContext());
                walletDao.updateWalletBackup(true, mWalletKey);
                RxBus.get().send(Constant.RX_BUS_CODE_BACKUP_PHRASE, "");
                goExport();
            } else if (mIsResultBackup) {
                //更新备份状态
                QWWalletDao walletDao = new QWWalletDao(getApplicationContext());
                walletDao.updateWalletBackup(true, mWalletKey);
                RxBus.get().send(Constant.RX_BUS_CODE_BACKUP_PHRASE, "");
                setResult(Activity.RESULT_OK);
                finish();
            } else {
                mViewModel.newWallet(mMnemonic, mPassword, mPasswordHint, 1);
            }
        } else {
            mErrorView.setVisibility(View.VISIBLE);
        }
    }

    private void checkOrder() {
        ArrayList<FlowLayout.TextBean> list = new ArrayList<>();
        for (FlowLayout.TextBean text : mPhraseRandomList) {
            if (mLabelList.contains(text)) {
                list.add(text);
                mLabelList.remove(text);
            }
        }
        mLabelList.clear();
        mLabelList.addAll(list);
    }

    private void checkInput() {
        if (checkInputFail()) {
            mErrorView.setVisibility(View.VISIBLE);
        } else {
            mErrorView.setVisibility(View.GONE);
        }
    }

    private boolean checkInputFail() {
        StringBuilder stringBuilder = new StringBuilder();
        for (FlowLayout.TextBean text : mPhraseList) {
            stringBuilder.append(text.getText());
        }
        String orderStr = stringBuilder.toString();

        String phase = mMnemonic.replaceAll(" ", "");
        return !phase.startsWith(orderStr);
    }


    private void createWallet() {
        mViewModel.newWallet(mMnemonic, mPassword, mPasswordHint);
    }

    private void onError() {
        showProgress(false);
        MyToast.showSingleToastShort(this, R.string.create_password_fail);
    }

    private void showProgress(boolean isShow) {
        if (isShow) {
            mProgressLayout.setVisibility(View.VISIBLE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
        }
    }
}
