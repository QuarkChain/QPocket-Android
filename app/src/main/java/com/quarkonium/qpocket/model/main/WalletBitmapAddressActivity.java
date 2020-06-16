package com.quarkonium.qpocket.model.main;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.model.main.view.SpinnerPopWindow;
import com.quarkonium.qpocket.model.wallet.BackupPhraseHintActivity;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.EncodingUtils;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.SystemUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.R;

import java.util.ArrayList;
import java.util.List;

public class WalletBitmapAddressActivity extends BaseActivity {

    private static class ViewHolder {
        private ImageView iv;
        private TextView tv;
    }

    public static class SpinnerAdapter extends BaseAdapter {

        private List<QWToken> mList;
        private String mGasToken;

        SpinnerAdapter(List<QWToken> list, String token) {
            this.mList = list;
            this.mGasToken = token;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.spinner_gas_symbol_item, null);

                holder = new ViewHolder();
                holder.iv = convertView.findViewById(R.id.gas_symbol_selected);
                holder.tv = convertView.findViewById(R.id.gas_symbol_text);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            QWToken token = mList.get(position);
            holder.tv.setText(token.getSymbol().toUpperCase());
            if (TextUtils.equals(mGasToken, token.getSymbol())) {
                holder.iv.setVisibility(View.VISIBLE);
            } else {
                holder.iv.setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    public static void startActivity(Activity activity, QWWallet wallet) {
        Intent intent = new Intent(activity, WalletBitmapAddressActivity.class);
        intent.putExtra(Constant.KEY_WALLET, wallet);
        activity.startActivity(intent);
    }

    public static void startActivity(Activity activity, QWWallet wallet, int networkId) {
        Intent intent = new Intent(activity, WalletBitmapAddressActivity.class);
        intent.putExtra(Constant.KEY_WALLET, wallet);
        intent.putExtra(Constant.KEY_TOKEN_NETWORK_ID, networkId);
        activity.startActivity(intent);
    }

    public static void startTokenActivity(Activity activity, QWWallet wallet, String contractAddress, int networkId) {
        Intent intent = new Intent(activity, WalletBitmapAddressActivity.class);
        intent.putExtra(Constant.KEY_WALLET, wallet);
        intent.putExtra(Constant.KEY_TOKEN_ADDRESS, contractAddress);
        intent.putExtra(Constant.KEY_TOKEN_NETWORK_ID, networkId);
        activity.startActivity(intent);
    }

    private static final float QR_IMAGE_WIDTH_RATIO = 0.7f;

    private QWWallet mWallet;

    private ImageView mQRAddressImg;
    private TextView mAddressTextView;
    private TextView mWalletNameTextView;
    private ImageView mWalletIcon;

    private TextView mDonateSymbolView;
    private SpinnerPopWindow mMenuPopWindow;
    private QWToken mDonateToken;
    private List<QWToken> mDonateTokenList;

    private View mProgressView;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main_bitmap_address;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_qr_address_title2;
    }

    @Override
    protected void onInitialization(Bundle bundle) {

        mTopBarView.setTitle(R.string.wallet_qr_address_title2);
        mTopBarView.setRightImage(R.drawable.filter_pop_share);
        mTopBarView.setRightImageClickListener(this::onShare);

        findViewById(R.id.wallet_address).setOnClickListener(this::onClick);
        mDonateSymbolView = findViewById(R.id.qr_donate_symbol);
        mDonateSymbolView.setOnClickListener(this::switchSymbol);


        mProgressView = findViewById(R.id.progress_layout);
        ViewCompat.setElevation(mProgressView, UiUtils.dpToPixel(3));

        mWalletIcon = findViewById(R.id.wallet_icon);
        mQRAddressImg = findViewById(R.id.wallet_address_img);
        mAddressTextView = findViewById(R.id.wallet_address);
        mWalletNameTextView = findViewById(R.id.wallet_name);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWallet = getIntent().getParcelableExtra(Constant.KEY_WALLET);
        String address = mWallet.getCurrentShowAddress();
        QWAccountDao dao = new QWAccountDao(getApplicationContext());
        QWAccount account = dao.queryAllParamsByAddress(mWallet.getCurrentAddress());
        mWallet.setCurrentAccount(account);

        if (WalletUtils.isValidAddress(address)) {
            TextView addressTip = findViewById(R.id.address_tips);
            addressTip.setText(R.string.wallet_qr_eth_address);
        } else if (TronWalletClient.isTronAddressValid(mWallet.getCurrentAddress())) {
            TextView addressTip = findViewById(R.id.address_tips);
            addressTip.setText(R.string.wallet_qr_trx_address);
        }

        mAddressTextView.setText(address);
        mWalletNameTextView.setText(account.getName());
        Glide.with(this)
                .asBitmap()
                .load(account.getIcon())
                .into(mWalletIcon);

        //生成二维码字符串
        String qrInfo = parseQRInfoString(address);
        final Bitmap qrCode = createQRImage(qrInfo);
        mQRAddressImg.setImageBitmap(qrCode);

        //没有备份并且是第一次点击弹备份提示
        if (0 == mWallet.getIsBackup() && !SharedPreferencesUtils.isBackupByKey(getApplicationContext(), mWallet.getKey())) {
            checkWalletBackup();
            SharedPreferencesUtils.setBackupByKey(getApplicationContext(), mWallet.getKey());
        }
    }

    private String parseQRInfoString(String address) {
        if (!TextUtils.isEmpty(address)) {
            if (QWWalletUtils.isQKCValidAddress(address)) {
                //QKC
                int networkId = getIntent().getIntExtra(Constant.KEY_TOKEN_NETWORK_ID, -1);
                if (networkId == -1) {
                    //纯地址
                    return address;
                }
                String contractAddress = getIntent().getStringExtra(Constant.KEY_TOKEN_ADDRESS);
                if (TextUtils.isEmpty(contractAddress)) {
                    //转账qkc token
                    //quarkchain:0x0AE56e1aB705Eb3BEAA7E94dd59FBDE9b6fAdB38?networkid=1
                    return Constant.QR_QKC_TITLE + address + "?" + Constant.QR_NETWORK_ID + networkId;
                } else {
                    //quarkchain:0x0AE56e1aB705Eb3BEAA7E94dd59FBDE9b6fAdB38?contractAddress=0xea26c4ac16d4a5a106820bc8aee85fd0b7b2b664&networkid=1
                    return Constant.QR_QKC_TITLE + address + "?" + Constant.QR_CONTRACT_ADDRESS + contractAddress.toLowerCase() + "&" + Constant.QR_NETWORK_ID + networkId;
                }
            } else if (WalletUtils.isValidAddress(address)) {
                //ETH
                int networkId = getIntent().getIntExtra(Constant.KEY_TOKEN_NETWORK_ID, -1);
                if (networkId == -1) {
                    //纯地址
                    return address;
                }
                String contractAddress = getIntent().getStringExtra(Constant.KEY_TOKEN_ADDRESS);
                if (TextUtils.isEmpty(contractAddress)) {
                    //转账ETH
                    //ethereum:0x0AE56e1aB705Eb3BEAA7E94dd59FBDE9b6fAdB38?networkid='1'
                    return Constant.QR_ETH_TITLE + address + "?" + Constant.QR_NETWORK_ID + networkId;
                } else {
                    //ethereum:0x0AE56e1aB705Eb3BEAA7E94dd59FBDE9b6fAdB38?contractAddress=0xea26c4ac16d4a5a106820bc8aee85fd0b7b2b664&networkid='1'
                    return Constant.QR_ETH_TITLE + address + "?" + Constant.QR_CONTRACT_ADDRESS + contractAddress.toLowerCase() + "&" + Constant.QR_NETWORK_ID + networkId;
                }
            } else if (TronWalletClient.isTronAddressValid(address)) {
                //TRX
                //当前版本trx只显示纯地址
                return address;
            }
        }
        return address;
    }

    private Bitmap createQRImage(String address) {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int imageSize = (int) (size.x * QR_IMAGE_WIDTH_RATIO);
        try {
            return EncodingUtils.createQRCode(address, imageSize);
        } catch (Exception e) {
            MyToast.showSingleToastShort(this, getString(R.string.error_fail_generate_qr));
        }
        return null;
    }

    public void onClick(View v) {
        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            String label = getString(R.string.wallet_copy_address_label);
            // 创建普通字符型ClipData
            String address = mAddressTextView.getText().toString();
            ClipData mClipData = ClipData.newPlainText(label, address);
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);

            MyToast.showSingleToastShort(this, R.string.copy_success);
        }
    }

    public void onShare(View view) {
        String address = mAddressTextView.getText().toString();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, address);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, getTitle()));
    }

    private void goBackup() {
        showProgress(false);
        Intent intent = new Intent(this, BackupPhraseHintActivity.class);
        intent.putExtra(Constant.WALLET_KEY, mWallet.getKey());
        intent.putExtra(Constant.IS_RESULT_BACKUP_PHRASE, true);
        startActivity(intent);
    }

    private void switchSymbol(View view) {
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }

        if (mDonateTokenList == null) {
            mDonateTokenList = new ArrayList<>();
            mDonateTokenList.add(QWTokenDao.getTQKCToken());
        }
        if (mMenuPopWindow == null) {
            mMenuPopWindow = new SpinnerPopWindow(this);
            mMenuPopWindow.setOnItemClickListener((int position, Object o) -> {
                mDonateToken = (QWToken) o;
                mDonateSymbolView.setText(mDonateToken.getSymbol().toUpperCase());
            });
        }
        mMenuPopWindow.setAdapter(new SpinnerAdapter(mDonateTokenList, mDonateToken.getSymbol()));
        mMenuPopWindow.showWidth(view, (int) UiUtils.dpToPixel(175));
    }

    //做备份提示
    private void checkWalletBackup() {
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setTitle(R.string.wallet_backup_title);
        dialog.setMessage(R.string.wallet_backup_error_message);
        dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
        dialog.setPositiveBtn(R.string.ok, v -> {
            dialog.dismiss();
            checkPassWord();
        });
        dialog.show();
    }

    private void checkPassWord() {
        SystemUtils.checkPassword(this, getSupportFragmentManager(), mWallet, new SystemUtils.OnCheckPassWordListenerImp() {
            @Override
            public void onPasswordSuccess(String password) {
                goBackup();
            }
        });
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressView.setVisibility(View.VISIBLE);
        } else {
            mProgressView.setVisibility(View.GONE);
        }
    }
}
