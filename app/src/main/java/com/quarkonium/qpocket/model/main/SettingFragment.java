package com.quarkonium.qpocket.model.main;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWBalanceDao;
import com.quarkonium.qpocket.api.db.dao.QWChainDao;
import com.quarkonium.qpocket.api.db.dao.QWShardDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.repository.ETHTransactionRepository;
import com.quarkonium.qpocket.base.SupportBaseFragment;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.finger.FingerprintDialogFragment;
import com.quarkonium.qpocket.finger.FingerprintIdentify;
import com.quarkonium.qpocket.jsonrpc.protocol.Web3j;
import com.quarkonium.qpocket.jsonrpc.protocol.Web3jFactory;
import com.quarkonium.qpocket.jsonrpc.protocol.http.HttpService;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetAccountData;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCNetworkInfo;
import com.quarkonium.qpocket.model.book.AddressBookActivity;
import com.quarkonium.qpocket.model.main.viewmodel.MainWallerViewModel;
import com.quarkonium.qpocket.model.unlock.UnlockManagerActivity;
import com.quarkonium.qpocket.statistic.UmengStatistics;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.view.SwitchButton;
import com.quarkonium.qpocket.view.SwitchDialog;
import com.quarkonium.qpocket.view.TopBarView;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 主界面-设置
 */
public class SettingFragment extends SupportBaseFragment {

    private enum Settings {
        Touch(1),
        Network(2),
        Languages(3),
        Coin(4),
        AddressBook(5),
        Unlock(13);

        private int mIndex;

        Settings(int index) {
            mIndex = index;
        }

        public int getIndex() {
            return mIndex;
        }
    }

    private class SettingsAdapter extends BaseQuickAdapter<HashMap<String, Integer>, BaseViewHolder> {

        private HashMap<String, Integer> netWork;

        SettingsAdapter(int layoutResId, @Nullable List<HashMap<String, Integer>> data) {
            super(layoutResId, data);

            netWork = new HashMap<>();
            netWork.put("name", R.string.settings_network);
            netWork.put("icon", R.drawable.settings_network);
            netWork.put("key", Settings.Network.getIndex());
        }

        @Override
        protected void convert(BaseViewHolder holder, HashMap item) {
            holder.setText(R.id.settings_name, (int) item.get("name"));
            holder.setTextColor(R.id.settings_name, getResources().getColor(R.color.color_666666));

            ImageView imageView = holder.getView(R.id.settings_img);
            imageView.setImageResource((int) item.get("icon"));
            imageView.setColorFilter(Color.TRANSPARENT);
            //Touch
            if (R.drawable.finger_print == (int) item.get("icon")) {
                holder.getView(R.id.settings_right_img).setVisibility(View.GONE);
                holder.getView(R.id.settings_state).setVisibility(View.GONE);

                FingerprintIdentify mFingerprintIdentify = new FingerprintIdentify(requireContext().getApplicationContext());
                //如果支持指纹，则激活指纹按钮
                if (mFingerprintIdentify.isHardwareEnable()) {
                    SwitchButton toggleButton = holder.getView(R.id.settings_right_toogle);
                    toggleButton.setVisibility(View.VISIBLE);
                    if (SharedPreferencesUtils.isSupportFingerprint(requireContext())) {
                        toggleButton.setChecked(true);
                    } else {
                        toggleButton.setChecked(false);
                    }
                    toggleButton.setOnCheckedChangeListener((SwitchButton buttonView, boolean isChecked) -> toggleButton(isChecked));
                } else {
                    holder.setTextColor(R.id.settings_name, getResources().getColor(R.color.color_cccccc));
                    imageView.setColorFilter(getResources().getColor(R.color.color_cccccc));
                    SwitchButton toggleButton = holder.getView(R.id.settings_right_toogle);
                    toggleButton.setVisibility(View.VISIBLE);
                    toggleButton.setEnabled(false);
                }
                holder.itemView.setEnabled(false);
            } else {
                holder.getView(R.id.settings_right_toogle).setVisibility(View.GONE);
                holder.getView(R.id.settings_right_img).setVisibility(View.VISIBLE);
                holder.itemView.setEnabled(true);

                holder.getView(R.id.settings_state).setVisibility(View.GONE);
            }

            if (holder.getAdapterPosition() == getItemCount() - 1) {
                holder.getView(R.id.line).setVisibility(View.GONE);
            } else {
                holder.getView(R.id.line).setVisibility(View.VISIBLE);
            }
        }

        private void toggleButton(boolean off) {
            if (off) {
                FingerprintDialogFragment fragment = new FingerprintDialogFragment();
                fragment.setStage(FingerprintDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
                fragment.setDismissListener(() -> mAdapter.notifyItemChanged(0));
                fragment.show(requireFragmentManager(), "DIALOG_FRAGMENT_TAG");
            } else {
                SharedPreferencesUtils.setSupportFingerprint(requireContext(), false);
            }

            UmengStatistics.settingTouchFingerprintClickCount(requireContext(), QWWalletUtils.getCurrentWalletAddress(requireContext()));
        }

        private void addNetWorkItem() {
            int index = getData().indexOf(netWork);
            if (index == -1) {
                List<HashMap<String, Integer>> list = getData();
                Integer icon = list.get(0).get("icon");
                if (icon != null && icon == R.drawable.finger_print) {
                    list.add(2, netWork);
                    notifyItemInserted(2);
                } else {
                    list.add(0, netWork);
                    notifyItemInserted(0);
                }
            }
        }

        private void removeNetWorkItem() {
            int index = getData().indexOf(netWork);
            if (index != -1) {
                getData().remove(netWork);
                notifyItemRemoved(index);
            }
        }
    }

    public static SettingFragment newInstance() {
        Bundle args = new Bundle();
        SettingFragment fragment = new SettingFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private SettingsAdapter mAdapter;

    private Disposable mDisposable;
    private SwitchDialog mSwitchDialog;

    private int mAccountType;
    private int mDefaultEthIndex;
    private int mDefaultQkcIndex;
    private String mDefaultCoins;

    private Object[] lock = new Object[1];

    private QWWallet mCurrentWallet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new SettingsAdapter(R.layout.holder_recycler_settings_item, new ArrayList<>());
        mAdapter.setOnItemClickListener((@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) -> onItemClick(position));

        //卡片1
        ArrayList<HashMap<String, Integer>> models = new ArrayList<>();

        HashMap<String, Integer> touch = new HashMap<>();
        touch.put("name", R.string.finger_touch);
        touch.put("icon", R.drawable.finger_print);
        touch.put("key", Settings.Touch.getIndex());
        models.add(touch);

        //锁屏界面
        FingerprintIdentify mFingerprintIdentify = new FingerprintIdentify(requireContext().getApplicationContext());
        //如果支持指纹，则显示指纹按钮
        if (mFingerprintIdentify.isHardwareEnable()) {
            HashMap<String, Integer> unlock = new HashMap<>();
            unlock.put("name", R.string.setting_unlock);
            unlock.put("icon", R.drawable.settings_unlock);
            unlock.put("key", Settings.Unlock.getIndex());
            models.add(unlock);
        }

        //网络
        models.add(mAdapter.netWork);

        //语言
        HashMap<String, Integer> model = new HashMap<>();
        model.put("name", R.string.settings_language);
        model.put("icon", R.drawable.settings_languages);
        model.put("key", Settings.Languages.getIndex());
        models.add(model);

        //币种
        HashMap<String, Integer> coin = new HashMap<>();
        coin.put("name", R.string.settings_coin);
        coin.put("icon", R.drawable.settings_coin);
        coin.put("key", Settings.Coin.getIndex());
        models.add(coin);

        HashMap<String, Integer> addressBook = new HashMap<>();
        addressBook.put("name", R.string.address_book_title);
        addressBook.put("icon", R.drawable.settings_address_book);
        addressBook.put("key", Settings.AddressBook.getIndex());
        models.add(addressBook);

        mAdapter.setNewInstance(models);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_settings;
    }

    @Override
    public int getFragmentTitle() {
        return R.string.main_menu_tag_setting;
    }

    @Override
    protected void onInitView(Bundle savedInstanceState, View rootView) {

        TopBarView topBarView = rootView.findViewById(R.id.top_layout);
        topBarView.setOnlyTitle(R.string.main_menu_tag_setting);

        //设置卡片1
        RecyclerView recyclerView = rootView.findViewById(R.id.main_settings_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(mAdapter);
        if (recyclerView.getItemAnimator() != null) {
            RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
            itemAnimator.setChangeDuration(0);
            itemAnimator.setMoveDuration(0);
            itemAnimator.setRemoveDuration(0);
            if (itemAnimator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
            }
        }
    }

    @Override
    public void onLazyInitView(@Nullable Bundle savedInstanceState) {
        super.onLazyInitView(savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        MainWallerViewModel mMainWalletFragmentViewModel = new ViewModelProvider(requireActivity(), activity.mMainWallerFragmentFactory).get(MainWallerViewModel.class);
        mMainWalletFragmentViewModel.findDefaultWalletObserve().observe(requireActivity(), this::findWalletSuccess);
        mCurrentWallet = mMainWalletFragmentViewModel.findDefaultWalletObserve().getValue();
        if (mCurrentWallet == null) {
            mMainWalletFragmentViewModel.findWallet();
        }

        mDefaultCoins = SharedPreferencesUtils.getCurrentMarketCoin(requireContext());
    }

    private void findWalletSuccess(QWWallet wallet) {
        if (getContext() == null) {
            return;
        }

        mCurrentWallet = wallet;
        mAccountType = wallet.getCurrentAccount().getType();
        if (mAccountType == Constant.ACCOUNT_TYPE_ETH) {
            mDefaultEthIndex = (int) SharedPreferencesUtils.getEthNetworkIndex(requireContext().getApplicationContext());
            mAdapter.addNetWorkItem();
        } else if (mAccountType == Constant.ACCOUNT_TYPE_QKC) {
            mDefaultQkcIndex = SharedPreferencesUtils.getQKCNetworkIndex(requireContext().getApplicationContext());
            mAdapter.addNetWorkItem();
        } else {
            mAdapter.removeNetWorkItem();
        }
    }

    //设置卡片1 点击事件
    public void onItemClick(int position) {
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }
        HashMap<String, Integer> hashMap = mAdapter.getItem(position);
        if (hashMap == null) {
            return;
        }

        Integer index = hashMap.get("key");
        if (index == null) {
            return;
        }
        String currentAddress = QWWalletUtils.getCurrentWalletAddress(requireContext());
        if (index == Settings.Languages.getIndex()) {
            SettingsLanguagesActivity.startActivity(requireActivity());
            UmengStatistics.settingLanguageClickCount(requireContext(), currentAddress);
        } else if (index == Settings.Coin.getIndex()) {
            CoinsActivity.startActivity(requireActivity());
            UmengStatistics.settingCoinClickCount(requireContext(), currentAddress);
        } else if (index == Settings.Network.getIndex()) {
            switchNetwork();
            UmengStatistics.settingSwitchNetworkClickCount(requireContext(), currentAddress);
        } else if (index == Settings.AddressBook.getIndex()) {
            AddressBookActivity.startSettingActivity(requireActivity(), mAccountType);
            UmengStatistics.settingAddressBookClickCount(requireContext(), currentAddress);
        } else if (index == Settings.Unlock.getIndex()) {
            UnlockManagerActivity.startSettingActivity(requireActivity());
            UmengStatistics.settingUnlockClickCount(requireContext(), currentAddress);
        }
    }

    //***********设置卡片点击事件*******************
    //***********切换网络 start*******************
    private void switchNetwork() {
        String[] data = getResources().getStringArray(Constant.ACCOUNT_TYPE_ETH == mAccountType ? R.array.setting_network_eth : R.array.setting_network_qkc);
        int selectedIndex = Constant.ACCOUNT_TYPE_ETH == mAccountType ?
                getEthIndex() :
                getQKCIndex();
        selectedIndex = Math.max(0, Math.min(selectedIndex, data.length));
        mSwitchDialog = new SwitchDialog(requireContext());
        mSwitchDialog.setData(data);
        mSwitchDialog.setSelected(data[selectedIndex]);
        mSwitchDialog.setOnListener((String name, int position) -> {
            if (Constant.ACCOUNT_TYPE_ETH == mAccountType) {
                long lastID = SharedPreferencesUtils.getEthNetworkIndex(requireContext().getApplicationContext());
                switch (position) {
                    case 0:
                        SharedPreferencesUtils.setEthNetworkIndex(requireContext().getApplicationContext(), Constant.ETH_PUBLIC_PATH_MAIN_INDEX);
                        Constant.sETHNetworkId = Constant.ETH_PUBLIC_PATH_MAIN_INDEX;
                        Constant.sEthNetworkPath = Constant.ETH_PUBLIC_PATH_MAIN;
                        break;
                    case 1:
                        SharedPreferencesUtils.setEthNetworkIndex(requireContext().getApplicationContext(), Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX);
                        Constant.sETHNetworkId = Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX;
                        Constant.sEthNetworkPath = Constant.ETH_PUBLIC_PATH_ROPSTEN;
                        break;
                    case 2:
                        SharedPreferencesUtils.setEthNetworkIndex(requireContext().getApplicationContext(), Constant.ETH_PUBLIC_PATH_KOVAN_INDEX);
                        Constant.sETHNetworkId = Constant.ETH_PUBLIC_PATH_KOVAN_INDEX;
                        Constant.sEthNetworkPath = Constant.ETH_PUBLIC_PATH_KOVAN;
                        break;
                    case 3:
                        SharedPreferencesUtils.setEthNetworkIndex(requireContext().getApplicationContext(), Constant.ETH_PUBLIC_PATH_RINKBY_INDEX);
                        Constant.sETHNetworkId = Constant.ETH_PUBLIC_PATH_RINKBY_INDEX;
                        Constant.sEthNetworkPath = Constant.ETH_PUBLIC_PATH_RINKBY;
                        break;
                }
                if (lastID == Constant.sETHNetworkId) {
                    mSwitchDialog.dismiss();
                    mSwitchDialog = null;
                    return;
                }
            } else if (Constant.ACCOUNT_TYPE_QKC == mAccountType) {
                int lastId = SharedPreferencesUtils.getQKCNetworkIndex(requireContext().getApplicationContext());
                switch (position) {
                    case 0:
                        SharedPreferencesUtils.setQKCNetworkIndex(requireContext().getApplicationContext(), Constant.QKC_PUBLIC_MAIN_INDEX);
                        Constant.sNetworkId = new BigInteger(String.valueOf(Constant.QKC_PUBLIC_MAIN_INDEX));
                        Constant.sQKCNetworkPath = Constant.QKC_PUBLIC_PATH_MAIN;
                        break;
                    case 1:
                        SharedPreferencesUtils.setQKCNetworkIndex(requireContext().getApplicationContext(), Constant.QKC_PUBLIC_DEVNET_INDEX);
                        Constant.sNetworkId = new BigInteger(String.valueOf(Constant.QKC_PUBLIC_DEVNET_INDEX));
                        Constant.sQKCNetworkPath = Constant.QKC_PUBLIC_PATH_DEVNET;
                        break;
                }
                if (lastId == Constant.sNetworkId.intValue()) {
                    mSwitchDialog.dismiss();
                    mSwitchDialog = null;
                    return;
                }
            }

            getWalletBalance();
        });
        mSwitchDialog.show();
    }

    //获取钱包balance
    private void getWalletBalance() {
        if (mCurrentWallet != null) {
            mSwitchDialog.showProgress();

            QWAccount account = mCurrentWallet.getCurrentAccount();
            String address = account.getShardAddress();
            Context context = mContext.getApplicationContext();
            if (account.isEth()) {
                //eth 余额
                ETHTransactionRepository transactionRepository = new ETHTransactionRepository();
                Disposable disposable = transactionRepository
                        .balanceInWei(account)
                        .flatMap(balance -> Single.fromCallable(() -> {
                            synchronized (lock) {
                                QWBalanceDao dao = new QWBalanceDao(context);
                                dao.insertEthBalance(context, account, balance);
                            }
                            return true;
                        })).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(v -> onGetBalanceSuccess(), v -> onGetBalanceSuccess());
            } else if (account.isTRX()) {
                //trx 余额
                Disposable disposable = Single.fromCallable(() -> {
                            byte[] tronAddress = TronWalletClient.decodeFromBase58Check(account.getAddress());
                            return new TronWalletClient().queryAccount(tronAddress, false);
                        }
                ).flatMap(balance -> Single.fromCallable(() -> {
                    synchronized (lock) {
                        QWBalanceDao dao = new QWBalanceDao(context);
                        dao.insertTrxBalance(context, account, new BigInteger(balance.getBalance() + ""));
                    }
                    return true;
                })).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(v -> onGetBalanceSuccess(), v -> onGetBalanceSuccess());
            } else if (account.isQKC()) {
                //qkc networkInfo 余额
                Web3j web3j = Web3jFactory.build(new HttpService(Constant.sQKCNetworkPath, false));
                Disposable disposable = getQKCNetworkInfo(web3j)
                        .flatMap(networkInfo -> {
                            //获取网络信息
                            //networkInfo
                            String nowTotalCount = SharedPreferencesUtils.getTotalChainCount(context);
                            List<String> nowTotalShard = SharedPreferencesUtils.getTotalSharedSizes(context);
                            //分片或者链有变化
                            if (!TextUtils.equals(nowTotalCount, networkInfo.getChainSize()) || isShardChange(nowTotalShard, networkInfo.getShardSizes())) {
                                //更新数据
                                SharedPreferencesUtils.setTotalChainCount(context, networkInfo.getChainSize());
                                SharedPreferencesUtils.setTotalSharedSizes(context, networkInfo.getShardSizes());

                                //增加chain
                                QWChainDao chainDao = new QWChainDao(context);
                                chainDao.addTotal(Numeric.toBigInt(networkInfo.getChainSize()).intValue());

                                //增加分片
                                int max = getMaxShard(networkInfo.getShardSizes());
                                QWShardDao shardDao = new QWShardDao(context);
                                shardDao.addTotal(max);

                                //判断当前chain是否大于总size
                                String chainId = SharedPreferencesUtils.getCurrentChain(context, address);
                                int chain = Numeric.toBigInt(chainId).intValue();
                                if (chain >= Numeric.toBigInt(networkInfo.getChainSize()).intValue()) {
                                    chainId = "0x0";
                                    chain = 0;
                                    SharedPreferencesUtils.setCurrentChain(context, address, chainId);
                                }
                                String shardId = SharedPreferencesUtils.getCurrentShard(context, address);
                                int shard = Numeric.toBigInt(shardId).intValue();
                                ArrayList<String> allShared = networkInfo.getShardSizes();
                                if (allShared != null && chain < allShared.size()) {
                                    if (shard >= Numeric.toBigInt(allShared.get(chain)).intValue()) {
                                        shardId = "0x0";
                                        shard = 0;
                                        SharedPreferencesUtils.setCurrentShard(context, address, shardId);
                                    }
                                }
                                String newAddress = Numeric.selectChainAndShardAddress(address, chain, shard);
                                return getQKCBalance(web3j, newAddress);
                            }
                            return getQKCBalance(web3j, address);
                        }).map((balance) -> {
                            //balance
                            QWBalanceDao dao = new QWBalanceDao(context);
                            dao.insertQKCBalance(context, account, balance);
                            return true;
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(v -> onGetBalanceSuccess(), v -> onGetBalanceSuccess());
            }
        }
    }

    private Single<QKCNetworkInfo.NetworkInfo> getQKCNetworkInfo(Web3j web3j) {
        return Single.fromCallable(() -> web3j
                .networkInfoSuccess()
                .send()
                .getQKCNetworkInfo());
    }

    private Single<QKCGetAccountData.AccountData> getQKCBalance(Web3j web3j, String address) {
        return Single.fromCallable(() -> web3j
                .getAccountData(address)
                .send()
                .getQKCGetAccountData()
        );
    }

    //分片有变化
    private boolean isShardChange(List<String> nowShards, List<String> networkShards) {
        if (networkShards == null) {
            return false;
        }

        if (nowShards == null) {
            return true;
        }

        if (networkShards.size() != nowShards.size()) {
            return true;
        }

        int size = nowShards.size();
        for (int i = 0; i < size; i++) {
            if (!TextUtils.equals(nowShards.get(i), networkShards.get(i))) {
                return true;
            }
        }

        return false;
    }

    private int getMaxShard(List<String> shared) {
        int max = 1;
        if (shared != null && !shared.isEmpty()) {
            for (String s : shared) {
                int sharedId = Numeric.toBigInt(s).intValue();
                if (sharedId > max) {
                    max = sharedId;
                }
            }
        }
        return max;
    }

    private void onGetBalanceSuccess() {
        if (mSwitchDialog != null) {
            mSwitchDialog.dismiss();
        }
        mSwitchDialog = null;
    }

    private int getEthIndex() {
        int index = (int) SharedPreferencesUtils.getEthNetworkIndex(requireContext().getApplicationContext());
        switch (index) {
            case Constant.ETH_PUBLIC_PATH_MAIN_INDEX:
                return 0;
            case Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX:
                return 1;
            case Constant.ETH_PUBLIC_PATH_KOVAN_INDEX:
                return 2;
            case Constant.ETH_PUBLIC_PATH_RINKBY_INDEX:
                return 3;
        }
        return 0;
    }

    private int getQKCIndex() {
        int index = SharedPreferencesUtils.getQKCNetworkIndex(requireContext().getApplicationContext());
        switch (index) {
            case Constant.QKC_PUBLIC_MAIN_INDEX:
                return 0;
            case Constant.QKC_PUBLIC_DEVNET_INDEX:
                return 1;
        }
        return 0;
    }
    //***********切换网络 end*******************

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            //切换了币种，Market界面进行刷新，钱包界面进行价格刷新
            String newCoins = SharedPreferencesUtils.getCurrentMarketCoin(requireContext());
            if (!TextUtils.equals(mDefaultCoins, newCoins)) {
                //更新钱包页
                MainActivity activity = (MainActivity) requireActivity();
                activity.updateFragmentNoDApp();
                mDefaultCoins = newCoins;
            }

            if (Constant.ACCOUNT_TYPE_ETH == mAccountType) {
                int index = (int) SharedPreferencesUtils.getEthNetworkIndex(requireContext().getApplicationContext());
                if (mDefaultEthIndex != index) {
                    MainActivity activity = (MainActivity) requireActivity();
                    activity.updateFragment();
                    mDefaultEthIndex = index;
                }
            } else if (Constant.ACCOUNT_TYPE_QKC == mAccountType) {
                int index = SharedPreferencesUtils.getQKCNetworkIndex(requireContext().getApplicationContext());
                if (mDefaultQkcIndex != index) {
                    MainActivity activity = (MainActivity) requireActivity();
                    activity.updateFragment();
                    mDefaultQkcIndex = index;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
            mDisposable = null;
        }
    }
}
