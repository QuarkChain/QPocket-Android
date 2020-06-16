package com.quarkonium.qpocket.model.main.viewmodel;

import android.app.Application;

import androidx.lifecycle.MutableLiveData;

import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.bumptech.glide.Glide;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.db.wealth.dao.QWBanner2Dao;
import com.quarkonium.qpocket.api.db.wealth.table.QWBanner2;
import com.quarkonium.qpocket.api.interact.AppInstallInteract;
import com.quarkonium.qpocket.api.interact.FindDefaultWalletInteract;
import com.quarkonium.qpocket.api.interact.WalletTransactionInteract;
import com.quarkonium.qpocket.base.SingleLiveEvent;
import com.quarkonium.qpocket.model.main.bean.TokenBean;
import com.quarkonium.qpocket.model.market.bean.Coin;
import com.quarkonium.qpocket.model.market.bean.Price;
import com.quarkonium.qpocket.model.viewmodel.BaseAndroidViewModel;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.http.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class MainWallerViewModel extends BaseAndroidViewModel {

    private final FindDefaultWalletInteract mFindDefaultWalletInteract;
    private final WalletTransactionInteract mWalletTransactionInteract;
    private final AppInstallInteract mAppInstallInteract;

    private final MutableLiveData<QWWallet> mFindWalletObserve = new MutableLiveData<>();

    MainWallerViewModel(Application application,
                        FindDefaultWalletInteract findDefaultWalletInteract,
                        WalletTransactionInteract transactionInteract,
                        AppInstallInteract appInstallInteract) {
        super(application);
        this.mFindDefaultWalletInteract = findDefaultWalletInteract;
        this.mWalletTransactionInteract = transactionInteract;
        mAppInstallInteract = appInstallInteract;
    }


    //***********************************************************
    //************获取默认钱包***********
    //***********************************************************
    public MutableLiveData<QWWallet> findDefaultWalletObserve() {
        return mFindWalletObserve;
    }

    public void findWallet() {
        findWallet(true);
    }

    public void findWallet(boolean loadDB) {
        cancelDisposable("findWallet");
        cancelDisposable("findDAppWallet");
        progress.setValue(true);
        Disposable disposable = mFindDefaultWalletInteract
                .find(getApplication())
                .map(wallet -> queryDB(wallet, loadDB))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDefaultWalletChanged, this::onError);
        addDisposable("findWallet", disposable);
    }

    //获取数据库信息
    private QWWallet queryDB(QWWallet wallet, boolean loadDB) {
        if (loadDB) {
            QWAccountDao dao = new QWAccountDao(getApplication());
            QWAccount account = dao.queryByAddress(wallet.getCurrentAddress());
            wallet.setCurrentAccount(account);
        }
        return wallet;
    }

    private void onDefaultWalletChanged(QWWallet wallet) {
        progress.postValue(false);
        mFindWalletObserve.postValue(wallet);
    }
    //************获取默认钱包***********


    //***********************************************************
    //***************获取所有token******************
    //***********************************************************
    private MutableLiveData<ArrayList<TokenBean>> mTokensData = new MutableLiveData<>();

    public void fetchDBToken(QWWallet wallet) {
        fetchToken(wallet, true, false);
    }

    public void fetchRefreshToken(QWWallet wallet) {
        fetchToken(wallet, false, true);
    }

    //编列token
    public void fetchToken(QWWallet wallet) {
        fetchToken(wallet, false, false);
    }

    private void fetchToken(QWWallet wallet, boolean onlyLoadDB, boolean needRefresh) {
        cancelDisposable("fetchToken");
        Disposable disposable = mWalletTransactionInteract
                .fetchAllToken(getApplication(), wallet.getCurrentAccount(), onlyLoadDB, needRefresh, wallet.isLedger())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onQueryTokensSuccess, v -> onQueryError(), () -> onQueryFinish(onlyLoadDB));
        addDisposable("fetchToken", disposable);
    }

    private void onQueryTokensSuccess(ArrayList<TokenBean> list) {
        mTokensData.postValue(list);
    }

    private void onQueryError() {
        mTokensData.postValue(new ArrayList<>());
    }

    //刷新价格
    private void onQueryFinish(boolean loadDB) {
        if (!loadDB) {
            ArrayList<TokenBean> list = mTokensData.getValue();
            coinAllPrice(list);
        }
    }

    public MutableLiveData<ArrayList<TokenBean>> tokensObserver() {
        return mTokensData;
    }
    //***************读取token******************

    //***********************************************************
    //***************添加token******************
    //***********************************************************
    private MutableLiveData<QWToken> mAddToken = new MutableLiveData<>();

    public void fetchAddToken(String address, int accountType) {
        cancelDisposable("fetchAddToken");
        Disposable disposable = mWalletTransactionInteract
                .fetchAddToken(getApplication(), address, accountType)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onFetchTokensSuccess, this::onFetchTokensFail);
        addDisposable("fetchAddToken", disposable);
    }

    private void onFetchTokensSuccess(QWToken token) {
        if (token == null) {
            onFetchTokensFail(null);
            return;
        }

        mAddToken.postValue(token);
    }

    private void onFetchTokensFail(Throwable throwable) {
        onError(throwable);
    }

    public MutableLiveData<QWToken> fetchAddTokensObserver() {
        return mAddToken;
    }

    private MutableLiveData<String> mAddTokenStatus = new MutableLiveData<>();

    public void addToken(String currentAddress, QWToken token) {
        cancelDisposable("addToken");
        Disposable disposable = mWalletTransactionInteract
                .addToken(getApplication(), currentAddress, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onAddTokenSuccess, v -> onAddTokenFail());
        addDisposable("addToken", disposable);
    }

    private void onAddTokenSuccess(String address) {
        mAddTokenStatus.postValue(address);
    }

    private void onAddTokenFail() {
        mAddTokenStatus.postValue("");
    }

    public MutableLiveData<String> addTokenStatus() {
        return mAddTokenStatus;
    }
    //***************添加token******************

    //***************删除token******************
    public void deleteToken(QWToken token) {
        cancelDisposable("deleteToken");
        Disposable disposable = mWalletTransactionInteract
                .deleteToken(getApplication(), token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> onDeleteState(), v -> onDeleteState());
        addDisposable("deleteToken", disposable);
    }

    private void onDeleteState() {

    }
    //***************删除token******************


    //****************token列表********************
    private SingleLiveEvent<List<QWToken>> mTokenList = new SingleLiveEvent<>();

    //获取token列表
    public void fetchTokenList(QWWallet wallet, int loadType) {
        cancelDisposable("fetchTokenList");
        Disposable disposable = mWalletTransactionInteract
                .fetchTokenList(getApplication(), wallet.getCurrentAccount(), loadType)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTokenListSuccess, v -> onTokenListFail());
        addDisposable("fetchTokenList", disposable);
    }

    private void onTokenListSuccess(List<QWToken> list) {
        mTokenList.postValue(list);
    }

    private void onTokenListFail() {
        mTokenList.postValue(new ArrayList<>());
    }

    public SingleLiveEvent<List<QWToken>> tokenListObserver() {
        return mTokenList;
    }

    private SingleLiveEvent<List<QWToken>> mSearchTokenList = new SingleLiveEvent<>();

    public void searchTokenList(QWAccount account, String key) {
        cancelDisposable("searchTokenList");
        Disposable disposable = mWalletTransactionInteract
                .searchTokenList(account, key)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSearchTokenListSuccess, v -> onSearchTokenListFail());
        addDisposable("searchTokenList", disposable);
    }

    private void onSearchTokenListSuccess(List<QWToken> list) {
        mSearchTokenList.postValue(list);
    }

    private void onSearchTokenListFail() {
        mSearchTokenList.postValue(new ArrayList<>());
    }

    public SingleLiveEvent<List<QWToken>> searchObserve() {
        return mSearchTokenList;
    }
    //****************token列表********************

    //**********************************************
    //******************商店页获取钱包**********************
    //**********************************************
    private MutableLiveData<QWWallet> mFindWalletFail = new MutableLiveData<>();

    public void findDAppWallet() {
        cancelDisposable("findDAppWallet");
        cancelDisposable("findWallet");
        Disposable disposable = mFindDefaultWalletInteract
                .find(getApplication())
                .map(wallet -> queryDB(wallet, true))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDefaultWalletChanged, v -> onFindFail());
        addDisposable("findDAppWallet", disposable);
    }

    private void onFindFail() {
        mFindWalletFail.postValue(null);
    }

    public MutableLiveData<QWWallet> walletFail() {
        return mFindWalletFail;
    }
    //******************商店页获取钱包**********************

    //**********************升级**************************
    private SingleLiveEvent<Integer> mInstallProgress = new SingleLiveEvent<>();

    public void install() {
        cancelDisposable("install");
        Disposable disposable = mAppInstallInteract
                .install(getApplication())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onInstallProgress, v -> onInstallFinish(), this::onInstallFinish);
        addDisposable("install", disposable);
    }

    private void onInstallProgress(Integer progress) {
        mInstallProgress.postValue(progress);
    }

    private void onInstallFinish() {
        mInstallProgress.postValue(100);
    }

    public SingleLiveEvent<Integer> installProgress() {
        return mInstallProgress;
    }
    //**********************升级**************************

    //***********************************************************
    //***************获取Coin价格******************
    //***********************************************************
    private ArrayList<Coin> getCoinPrice(ArrayList<Coin> coinList) throws IOException, JSONException {
        StringBuilder idStr = new StringBuilder();
        int size = coinList.size();
        for (int i = 0; i < size; i++) {
            Coin coin = coinList.get(i);
            idStr.append(coin.getId());
            if (i != size - 1) {
                idStr.append(",");
            }
        }

        ArrayList<Coin> list = new ArrayList<>();
        String currencies = SharedPreferencesUtils.getCurrentMarketCoin(getApplication());
        String path = Constant.MARKET_API_PATH_COIN_PRICE;
        path = String.format(path, idStr.toString(), currencies);

        OkHttpClient okHttpClient = HttpUtils.getOkHttp();
        final okhttp3.Request request = new okhttp3.Request.Builder()
                .url(path)
                .build();
        final Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        if (response.body() != null) {
            String value = response.body().string();
            JSONObject jsonObject = new JSONObject(value);
            for (Coin coin : coinList) {
                if (jsonObject.has(coin.getId())) {
                    Price price = new Price();
                    price.setCoinID(coin.getId());

                    JSONObject priceObject = jsonObject.getJSONObject(coin.getId());
                    Iterator<String> it = priceObject.keys();
                    while (it.hasNext()) {
                        // 获得key
                        String key = it.next();
                        price.setPriceType(key);

                        float p = (float) priceObject.getDouble(key);
                        price.setPrice(p);
                    }
                    coin.setPrice(price);

                    list.add(coin);
                }
            }
        }

        return list;
    }

    private MutableLiveData<ArrayList<Coin>> mPriceData = new MutableLiveData<>();

    private void coinAllPrice(List<TokenBean> tokenBeanList) {
        cancelDisposable("coinAllPrice");
        Disposable disposable = Single.fromCallable(() -> {
            ArrayList<Coin> list = new ArrayList<>();

            ArrayList<String> symbolList = new ArrayList<>();
            for (TokenBean bean : tokenBeanList) {
                QWToken token = bean.getToken();
                symbolList.add(token.getSymbol().toLowerCase());
            }

//            List<AVObject> queryList = null;
//            AVQuery<AVObject> avQuery = new AVQuery<>("MarketSearch");
//            avQuery.whereContainedIn("symbol", symbolList);
//            try {
//                queryList = avQuery.find();
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//            if (queryList != null && !queryList.isEmpty()) {
            ArrayList<Coin> coinList = new ArrayList<>();
            Coin qkc = new Coin();
            qkc.setId("quark-chain");
            qkc.setName("QuarkChain");
            qkc.setSymbol("qkc");
            coinList.add(qkc);

            Coin eth = new Coin();
            eth.setId("ethereum");
            eth.setName("Ethereum");
            eth.setSymbol("eth");
            coinList.add(eth);

            Coin trx = new Coin();
            trx.setId("tron");
            trx.setName("TRON");
            trx.setSymbol("trx");
            coinList.add(trx);
//                HashSet<Coin> removeSet = new HashSet<>();
//                for (AVObject object : queryList) {
//                    String symbol = object.getString("symbol");
//                    Coin coin = isContains(coinList, symbol);
//                    if (coin != null) {
//                        removeSet.add(coin);
//                        continue;
//                    }
//                    coin = new Coin();
//                    coin.setId(object.getString("id"));
//                    coin.setName(object.getString("name"));
//                    coin.setSymbol(object.getString("symbol"));
//
//                    //有重复的，去除重复
//                    coinList.add(coin);
//                }
//                coinList.removeAll(removeSet);

            if (!coinList.isEmpty()) {
                list = getCoinPrice(coinList);
                //价格存缓存
                if (list != null) {
                    for (Coin coin : list) {
                        Price price = coin.getPrice();
                        SharedPreferencesUtils.putCoinPrice(getApplication(), coin.getSymbol(), price.getPriceType(), price.getPrice());
                    }
                }
            }
//            }
            return list;
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::coinPriceSuccess, v -> coinPriceFail());

        addDisposable("coinAllPrice", disposable);
    }

    private void coinPriceSuccess(ArrayList<Coin> list) {
        mPriceData.postValue(list);
    }

    private void coinPriceFail() {
        mPriceData.postValue(new ArrayList<>());
    }

    public MutableLiveData<ArrayList<Coin>> coinPrice() {
        return mPriceData;
    }
    //***************获取Coin价格*********************************

    //***********************************************************
    //************获取Wealth Banner***********
    //***********************************************************
    private MutableLiveData<List<QWBanner2>> mWealthBanner = new MutableLiveData<>();

    public void getWealthBannerList() {
        cancelDisposable("getWealthBannerList");
        Disposable disposable = loadWealthBannerList()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onWealthBannerSuccess, v -> onWealthBannerFail());

        addDisposable("getWealthBannerList", disposable);
    }

    private Observable<List<QWBanner2>> loadWealthBannerList() {
        return Observable.create(e -> {

            boolean hasData = true;
            String language = ToolUtils.isZh(getApplication()) ? "zh-Hans" : "en";
            //加载默认内容或者缓存
            QWBanner2Dao dao = new QWBanner2Dao(getApplication());
            List<QWBanner2> list = dao.queryAll(Constant.BANNER_TYPE_WEALTH, language);
            if (list == null || list.isEmpty()) {
                list = new ArrayList<>();
                QWBanner2 app = new QWBanner2();
                app.setName("default");
                list.add(app);
                hasData = false;
            }
            e.onNext(list);

            //拉取最新数据
            String key = "wealth_banner_list" + language;
            boolean refresh = ToolUtils.isLongDayTime(SharedPreferencesUtils.getBannerListTime(getApplication(), key));
            if (refresh || !hasData) {
                //刷新缓存
                if (ConnectionUtil.isInternetConnection(getApplication())) {
                    AVQuery<AVObject> avQuery = new AVQuery<>("BannerS3");
                    avQuery.whereEqualTo("type", Constant.BANNER_TYPE_WEALTH);
                    avQuery.whereEqualTo("localization", language);
                    if (ToolUtils.isNotOfficialChannel(getApplication())) {
                        avQuery.whereEqualTo("isGP", true);
                    }
                    avQuery.orderByDescending("order");
                    List<AVObject> queryList = null;
                    try {
                        queryList = avQuery.find();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    if (queryList != null && !queryList.isEmpty()) {
                        dao.clear(Constant.BANNER_TYPE_WEALTH, language);

                        ArrayList<QWBanner2> temp = new ArrayList<>();
                        for (AVObject object : queryList) {
                            //提前下载图片
                            String background = object.getString("backgroundURL");
                            Glide.with(getApplication())
                                    .downloadOnly()
                                    .load(background)
                                    .submit()
                                    .get();

                            QWBanner2 dApp = new QWBanner2();
                            dApp.setUrl(object.getString("URL"));
                            dApp.setName(object.getString("name"));
                            dApp.setLocalization(object.getString("localization"));
                            dApp.setBackgroundURL(background);
                            if (object.has("coinType")) {
                                dApp.setCoinType(object.getInt("coinType"));
                            }
                            dApp.setType(object.getInt("type"));
                            dApp.setOrder(object.getInt("order"));
                            temp.add(dApp);
                        }

                        dao.installAll(temp);
                        SharedPreferencesUtils.setBannerListTime(getApplication(), key);

                        if (temp.isEmpty()) {
                            list = new ArrayList<>();
                            QWBanner2 app = new QWBanner2();
                            app.setName("default");
                            list.add(app);
                        } else {
                            list = new ArrayList<>(temp);
                        }
                        e.onNext(list);
                    }
                }
            }
            e.onComplete();
        });
    }

    private void onWealthBannerSuccess(List<QWBanner2> list) {
        mWealthBanner.postValue(list);
    }

    private void onWealthBannerFail() {
        ArrayList<QWBanner2> list = new ArrayList<>();
        QWBanner2 app = new QWBanner2();
        app.setName("default");
        list.add(app);
        mWealthBanner.postValue(list);
    }

    public MutableLiveData<List<QWBanner2>> wealthBanner() {
        return mWealthBanner;
    }
    //***********************************************************
}
