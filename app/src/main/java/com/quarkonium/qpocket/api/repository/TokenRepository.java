package com.quarkonium.qpocket.api.repository;

import android.content.Context;
import android.text.TextUtils;

import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.google.protobuf.ByteString;
import com.j256.ormlite.dao.ForeignCollection;
import com.quarkonium.qpocket.abi.FunctionEncoder;
import com.quarkonium.qpocket.abi.FunctionReturnDecoder;
import com.quarkonium.qpocket.abi.TypeReference;
import com.quarkonium.qpocket.abi.datatypes.Address;
import com.quarkonium.qpocket.abi.datatypes.Bool;
import com.quarkonium.qpocket.abi.datatypes.Function;
import com.quarkonium.qpocket.abi.datatypes.Type;
import com.quarkonium.qpocket.abi.datatypes.Utf8String;
import com.quarkonium.qpocket.abi.datatypes.generated.Uint128;
import com.quarkonium.qpocket.abi.datatypes.generated.Uint168;
import com.quarkonium.qpocket.abi.datatypes.generated.Uint256;
import com.quarkonium.qpocket.abi.datatypes.generated.Uint64;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWAccountDao;
import com.quarkonium.qpocket.api.db.dao.QWBalanceDao;
import com.quarkonium.qpocket.api.db.dao.QWChainDao;
import com.quarkonium.qpocket.api.db.dao.QWPublicScaleDao;
import com.quarkonium.qpocket.api.db.dao.QWShardDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.dao.QWTokenListOrderDao;
import com.quarkonium.qpocket.api.db.dao.QWTransactionDao;
import com.quarkonium.qpocket.api.db.dao.QWWalletTokenDao;
import com.quarkonium.qpocket.api.db.table.QWAccount;
import com.quarkonium.qpocket.api.db.table.QWBalance;
import com.quarkonium.qpocket.api.db.table.QWChain;
import com.quarkonium.qpocket.api.db.table.QWPublicScale;
import com.quarkonium.qpocket.api.db.table.QWShard;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWTokenListOrder;
import com.quarkonium.qpocket.api.db.table.QWWalletToken;
import com.quarkonium.qpocket.crypto.Keys;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.jsonrpc.protocol.Web3j;
import com.quarkonium.qpocket.jsonrpc.protocol.Web3jFactory;
import com.quarkonium.qpocket.jsonrpc.protocol.core.DefaultBlockParameterName;
import com.quarkonium.qpocket.jsonrpc.protocol.http.HttpService;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.request.TokenBalance;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.request.Transaction;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.Account;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.Balance;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.EthCall;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QKCGetAccountData;
import com.quarkonium.qpocket.jsonrpc.protocol.methods.response.QuarkCall;
import com.quarkonium.qpocket.model.main.bean.TokenBean;
import com.quarkonium.qpocket.model.transaction.bean.NativeGasBean;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.tron.TrxRequest;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.Hex;

import org.json.JSONArray;
import org.json.JSONException;
import org.tron.api.GrpcAPI;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class TokenRepository implements TokenRepositoryType {

    public TokenRepository() {
    }

    private int getChainId(QWAccount account) {
        if (account.isEth()) {
            return (int) Constant.sETHNetworkId;
        }
        if (account.isQKC()) {
            return Constant.sNetworkId.intValue();
        }
        if (account.isTRX()) {
            return Constant.TRX_MAIN_NETWORK;
        }
        return 0;
    }

    public static TokenBean getMainTokenBean(Context context, QWAccount account) {
        return getMainTokenBean(context, account, false);
    }

    //获取主网代币
    public static TokenBean getMainTokenBean(Context context, QWAccount account, boolean reload) {
        TokenBean tokenBean = new TokenBean();

        //重新查询数据库 刷新数据
        if (reload) {
            QWAccountDao dao = new QWAccountDao(context);
            account = dao.queryByAddress(account.getAddress());
        }
        ForeignCollection<QWBalance> collection = account.getBalances();
        if (collection != null && !collection.isEmpty()) {
            //更新数据
            if (account.isEth()) {
                //ETH
                for (QWBalance balance : collection) {
                    if (balance.getQWToken() != null && QWTokenDao.ETH_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                        tokenBean.setBalance(balance);
                        tokenBean.setToken(balance.getQWToken());
                        break;
                    }
                }
            } else if (account.isTRX()) {
                //trx
                for (QWBalance balance : collection) {
                    if (balance.getQWToken() != null && QWTokenDao.TRX_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                        tokenBean.setBalance(balance);
                        tokenBean.setToken(balance.getQWToken());
                        break;
                    }
                }
            } else {
                //QKC
                BigInteger total = BigInteger.ZERO;
                for (QWBalance balance : collection) {
                    if (balance.getQWToken() != null && QWTokenDao.QKC_SYMBOL.equals(balance.getQWToken().getSymbol())) {
                        total = total.add(Numeric.toBigInt(balance.getBalance()));
                        tokenBean.setBalance(balance);
                        tokenBean.setToken(balance.getQWToken());
                    }
                }
                if (tokenBean.getBalance() != null) {
                    tokenBean.getBalance().setBalance(total.toString(16));
                }
            }
        }

        if (tokenBean.getToken() == null) {
            QWTokenDao tokenDao = new QWTokenDao(context);
            if (account.isEth()) {
                //ETH
                QWToken token = tokenDao.queryTokenByName(QWTokenDao.ETH_NAME);
                tokenBean.setToken(token);
            } else if (account.isTRX()) {
                //trx
                QWToken token = tokenDao.queryTokenByName(QWTokenDao.TRX_NAME);
                tokenBean.setToken(token);
            } else if (account.isBTC() || account.isBTCSegWit()) {
                //btc
                QWToken token = tokenDao.queryTokenByName(QWTokenDao.BTC_NAME);
                tokenBean.setToken(token);
            } else {
                //QKC
                QWToken token = tokenDao.queryTokenByAddress(QWTokenDao.TQKC_ADDRESS);
                tokenBean.setToken(token);
            }
        }
        return tokenBean;
    }

    @Override
    public ArrayList<TokenBean> fetchTokenDB(Context context, QWAccount account) {
        ArrayList<TokenBean> result = new ArrayList<>();

        //先显示主币，在显示ERC20 QRC20等类型代币
        TokenBean mainBean = getMainTokenBean(context, account, true);
        result.add(mainBean);

        int networkId = getChainId(account);
        int accountType = getAccountType(account);
        //显示代币
        //************************获取本地缓存************************************
        //获取自定义TOKEN
        QWWalletTokenDao walletTokenDao = new QWWalletTokenDao(context);
        List<QWWalletToken> adjustTokenList = walletTokenDao.queryByWallet(account.getAddress());
        //服务器数据显示顺序
        QWTokenListOrderDao listOrderDao = new QWTokenListOrderDao(context);
        QWTokenListOrder oderList = listOrderDao.queryTokenList(accountType, networkId);
        //获取数据库中token
        QWTokenDao dao = new QWTokenDao(context);
        List<QWToken> list = dao.queryAllTokenByType(accountType);
        QWBalanceDao balanceDao = new QWBalanceDao(context);
        if (list != null) {
            ArrayList<TokenBean> addList = new ArrayList<>();
            ArrayList<TokenBean> cloudList = new ArrayList<>();
            //服务器获取的数据
            for (QWToken token : list) {
                if (QWTokenDao.BTC_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
                if (QWTokenDao.TRX_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
                if (QWTokenDao.ETH_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
                if (QWTokenDao.QKC_SYMBOL.equals(token.getSymbol()) && !account.isEth()) {
                    continue;
                }
                if (accountType == Constant.ACCOUNT_TYPE_ETH && token.getChainId() != Constant.sETHNetworkId) {
                    continue;
                }
                if (accountType == Constant.ACCOUNT_TYPE_QKC && token.getChainId() != networkId) {
                    continue;
                }


                if (adjustTokenList != null) {
                    //自定义添加的Token数据
                    for (QWWalletToken qwWalletToken : adjustTokenList) {
                        if (TextUtils.equals(qwWalletToken.getTokenAddress(), token.getAddress())) {
                            token.setIsShow(1);
                            break;
                        }
                    }
                }
                TokenBean bean = new TokenBean();
                if (token.isShow()) {
                    bean.setToken(token);
                    //获取balance
                    if (token.isNative() && accountType == Constant.ACCOUNT_TYPE_QKC) {
                        QWBalance balance = getQKCNativeBalance(balanceDao, account, token);
                        bean.setBalance(balance);
                    } else {
                        QWBalance balance = balanceDao.queryByWT(account, token);
                        bean.setBalance(balance);
                    }

                    if (token.getIsAdd() == 1) {
                        addList.add(bean);
                    } else {
                        cloudList.add(bean);
                    }
                }
            }

            //排序
            Collections.reverse(addList);
            //服务区数据排序过滤
            cloudList = orderTokenBeanList(cloudList, oderList);
            result.addAll(addList);
            result.addAll(cloudList);
        }
        return result;
    }

    private String getTypeName(QWAccount account) {
        switch (account.getType()) {
            case Constant.ACCOUNT_TYPE_ETH:
                return Constant.HD_PATH_CODE_ETH + "" + (int) Constant.sETHNetworkId;
            case Constant.ACCOUNT_TYPE_TRX:
                return Constant.HD_PATH_CODE_TRX + "" + Constant.TRX_MAIN_NETWORK;
        }
        return Constant.HD_PATH_CODE_QKC + "" + Constant.sNetworkId.intValue();
    }

    private int getAccountType(QWAccount account) {
        return account.getType();
    }

    @Override
    public ArrayList<TokenBean> fetchTokenCloud(ObservableEmitter emitter, Context context, QWAccount account, boolean needRefresh) {
        //************************刷新数据************************************
        //获取上次拉取时间
        String typeName = getTypeName(account);
        int accountType = getAccountType(account);
        boolean refresh = needRefresh || ToolUtils.isLongDayTime(SharedPreferencesUtils.getTokenListTime(context, typeName));
        if (!refresh && accountType == Constant.ACCOUNT_TYPE_ETH
                && (Constant.sETHNetworkId == Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX || Constant.sETHNetworkId == Constant.ETH_PUBLIC_PATH_KOVAN_INDEX)) {
            //ETH  ROPSTEN_INDEX   KOVAN_INDEX两network下token address相同，所以每次都需要刷新重置数据
            refresh = true;
        }

        //刷新缓存
        if (refresh && ConnectionUtil.isInternetConnection(context)) {
            QWTokenListOrder oderList = updateTokenInfoCache(context, account);
            //获取数据库中token
            QWTokenDao dao = new QWTokenDao(context);
            List<QWToken> list = dao.queryAllTokenByType(accountType);
            if (list == null) {
                return null;
            }
            SharedPreferencesUtils.setTokenListTime(context, typeName);

            //获取自定义TOKEN
            QWWalletTokenDao walletTokenDao = new QWWalletTokenDao(context);
            List<QWWalletToken> adjustTokenList = walletTokenDao.queryByWallet(account.getAddress());
            QWBalanceDao balanceDao = new QWBalanceDao(context);
            if (account.isTRX()) {
                //过滤并获取 TRX Token
                return filterTrxTokenList(emitter, context, list, adjustTokenList, account, oderList, balanceDao, walletTokenDao);
            } else if (account.isEth()) {
                //过滤并获取 ETH Token
                return filterEthTokenList(emitter, context, list, adjustTokenList, account, oderList, balanceDao, walletTokenDao);
            } else {
                //过滤并获取 QKC Token
                return filterQkcTokenList(emitter, context, list, adjustTokenList, account, oderList, balanceDao, walletTokenDao);
            }
        }
        return null;
    }

    //获取QKC native Token总余额
    private QWBalance getQKCNativeBalance(QWBalanceDao balanceDao, QWAccount account, QWToken token) {
        List<QWBalance> list = balanceDao.queryTokenAllByWT(account, token);
        if (list != null && !list.isEmpty()) {
            BigInteger totalBalance = BigInteger.ZERO;
            QWBalance currentBalance = list.get(0);
            for (QWBalance balance : list) {
                String value = balance.getBalance();
                if (!TextUtils.isEmpty(value)) {
                    totalBalance = totalBalance.add(Numeric.toBigInt(balance.getBalance()));
                }
            }

            currentBalance.setBalance(Numeric.toHexStringWithPrefix(totalBalance));
            return currentBalance;
        }

        return balanceDao.queryByWT(account, token);
    }

    //过滤trx token，并获取token余额
    private ArrayList<TokenBean> filterTrxTokenList(ObservableEmitter observableEmitter, Context context,
                                                    List<QWToken> list,
                                                    List<QWWalletToken> adjustTokenList,
                                                    QWAccount account,
                                                    QWTokenListOrder oderList,
                                                    QWBalanceDao balanceDao,
                                                    QWWalletTokenDao walletTokenDao) {
        ArrayList<TokenBean> result = new ArrayList<>();

        HttpService httpService = new HttpService(Constant.TRON_MAIN_NET_PATH + Constant.TRON_MAIN_NET_METHOD_TRIGGER_SMART, false);
        ArrayList<TokenBean> addList = new ArrayList<>();
        ArrayList<TokenBean> cloudList = new ArrayList<>();

        //获取所有trc10 balance
        String fromAddress = account.getAddress();
        Map<String, Long> map = getAllTrxTrc10TokenBalance(fromAddress);
        if (observableEmitter.isDisposed()) {
            return null;
        }

        //服务器获取的数据
        for (QWToken token : list) {
            if (observableEmitter.isDisposed()) {
                return null;
            }
            if (QWTokenDao.TRX_SYMBOL.equals(token.getSymbol())) {
                continue;
            }

            TokenBean bean = new TokenBean();
            bean.setToken(token);
            //自定义添加的Token数据
            if (adjustTokenList != null) {
                for (QWWalletToken qwWalletToken : adjustTokenList) {
                    if (TextUtils.equals(qwWalletToken.getTokenAddress(), token.getAddress())) {
                        token.setIsShow(1);
                        break;
                    }
                }
            }
            if (token.isShow()) {
                //Token为打开状态
                //获取balance
                QWBalance qwBalance = TronWalletClient.isTronAddressValid(token.getAddress()) ?
                        getTrxTrc20TokenBalance(httpService, account, token, balanceDao, fromAddress) :
                        getTrxTrc10TokenBalance(balanceDao, account, token, map);

                bean.setBalance(qwBalance);
                if (token.getIsAdd() == 1) {
                    addList.add(bean);
                } else {
                    cloudList.add(bean);
                }
            } else {
                //Token为关闭状态
                //判断是否从未添加过
                if (isNotOpenedToken(context, account.getAddress(), token.getAddress())) {
                    //获取balance
                    QWBalance qwBalance = TronWalletClient.isTronAddressValid(token.getAddress()) ?
                            getTrxTrc20TokenBalance(httpService, account, token, balanceDao, fromAddress) :
                            getTrxTrc10TokenBalance(balanceDao, account, token, map);
                    bean.setBalance(qwBalance);
                    //余额不为0
                    if (bean.getBalance() != null && !TextUtils.isEmpty(bean.getBalance().getBalance()) &&
                            !BigInteger.ZERO.equals(Numeric.toBigInt(bean.getBalance().getBalance()))) {
                        //打开token显示开关
                        QWWalletToken tokenBean = new QWWalletToken();
                        tokenBean.setTokenAddress(token.getAddress());
                        tokenBean.setAccountAddress(account.getAddress());
                        walletTokenDao.insert(tokenBean);

                        if (token.getIsAdd() == 1) {
                            addList.add(bean);
                        } else {
                            cloudList.add(bean);
                        }
                    }
                }
            }
        }
        //排序
        Collections.reverse(addList);
        //服务器数据排序过滤
        cloudList = orderTokenBeanList(cloudList, oderList);
        result.addAll(addList);
        result.addAll(cloudList);

        return result;
    }


    //过滤eth token，并获取token余额
    private ArrayList<TokenBean> filterEthTokenList(ObservableEmitter observableEmitter, Context context,
                                                    List<QWToken> list,
                                                    List<QWWalletToken> adjustTokenList,
                                                    QWAccount account,
                                                    QWTokenListOrder oderList,
                                                    QWBalanceDao balanceDao,
                                                    QWWalletTokenDao walletTokenDao) {
        ArrayList<TokenBean> result = new ArrayList<>();
        ArrayList<TokenBean> addList = new ArrayList<>();
        ArrayList<TokenBean> cloudList = new ArrayList<>();

        Web3j ethWeb3j = Web3jFactory.build(new HttpService(Constant.sEthNetworkPath, false));
        //服务器获取的数据
        for (QWToken token : list) {
            if (observableEmitter.isDisposed()) {
                return null;
            }

            if (QWTokenDao.ETH_SYMBOL.equals(token.getSymbol())) {
                continue;
            }
            if (token.getChainId() != Constant.sETHNetworkId && account.isEth()) {
                continue;
            }

            TokenBean bean = new TokenBean();
            bean.setToken(token);

            //自定义添加的Token数据
            if (adjustTokenList != null) {
                for (QWWalletToken qwWalletToken : adjustTokenList) {
                    if (TextUtils.equals(qwWalletToken.getTokenAddress(), token.getAddress())) {
                        token.setIsShow(1);
                        break;
                    }
                }
            }
            if (token.isShow()) {
                //Token为打开状态
                //获取balance
                QWBalance qwBalance = getAllEthBalance(account, token, balanceDao, ethWeb3j);
                bean.setBalance(qwBalance);
                if (token.getIsAdd() == 1) {
                    addList.add(bean);
                } else {
                    cloudList.add(bean);
                }
            } else {
                //Token为关闭状态
                //判断是否从未添加过
                if (isNotOpenedToken(context, account.getAddress(), token.getAddress())) {
                    //获取balance
                    QWBalance qwBalance = getAllEthBalance(account, token, balanceDao, ethWeb3j);
                    bean.setBalance(qwBalance);
                    //余额不为0
                    if (bean.getBalance() != null && !TextUtils.isEmpty(bean.getBalance().getBalance()) &&
                            !BigInteger.ZERO.equals(Numeric.toBigInt(bean.getBalance().getBalance()))) {
                        //打开token显示开关
                        QWWalletToken tokenBean = new QWWalletToken();
                        tokenBean.setTokenAddress(token.getAddress());
                        tokenBean.setAccountAddress(account.getAddress());
                        walletTokenDao.insert(tokenBean);

                        if (token.getIsAdd() == 1) {
                            addList.add(bean);
                        } else {
                            cloudList.add(bean);
                        }
                    }
                }
            }
        }
        //排序
        Collections.reverse(addList);
        //服务器数据排序过滤
        cloudList = orderTokenBeanList(cloudList, oderList);
        result.addAll(addList);
        result.addAll(cloudList);
        return result;
    }

    //过滤qkc token，并获取token余额
    private ArrayList<TokenBean> filterQkcTokenList(ObservableEmitter observableEmitter, Context context,
                                                    List<QWToken> list,
                                                    List<QWWalletToken> adjustTokenList,
                                                    QWAccount account,
                                                    QWTokenListOrder oderList,
                                                    QWBalanceDao balanceDao,
                                                    QWWalletTokenDao walletTokenDao) {

        ArrayList<TokenBean> result = new ArrayList<>();
        ArrayList<TokenBean> addList = new ArrayList<>();
        ArrayList<TokenBean> cloudList = new ArrayList<>();
        QWChainDao chainDao = new QWChainDao(context);
        QWShardDao shardDao = new QWShardDao(context);

        //获取Native Token余额
        TransactionRepository transactionRepository = new TransactionRepository(null);
        QKCGetAccountData.AccountData accountData = transactionRepository.getAccountData(account.getShardAddress()).blockingGet();

        String fromAddress = account.getAddress().substring(0, account.getAddress().length() - 8);
        //服务器获取的数据
        for (QWToken token : list) {
            if (observableEmitter.isDisposed()) {
                return null;
            }

            if (QWTokenDao.QKC_SYMBOL.equals(token.getSymbol())) {
                continue;
            }
            if (token.getChainId() != Constant.sNetworkId.intValue() && account.isQKC()) {
                continue;
            }


            if (adjustTokenList != null) {
                //自定义添加的Token数据
                for (QWWalletToken qwWalletToken : adjustTokenList) {
                    if (TextUtils.equals(qwWalletToken.getTokenAddress(), token.getAddress())) {
                        token.setIsShow(1);
                        break;
                    }
                }
            }

            if (token.isShow()) {
                TokenBean bean = new TokenBean();
                bean.setToken(token);
                //Token为打开状态
                //获取balance
                QWBalance qwBalance = token.isNative() ?
                        getQKCNativeTokenBalance(balanceDao, account, token, accountData, chainDao, shardDao) :
                        getQKCERC20TokenBalance(context, balanceDao, account, token, fromAddress);
                bean.setBalance(qwBalance);
                if (token.getIsAdd() == 1) {
                    addList.add(bean);
                } else {
                    cloudList.add(bean);
                }
            } else {
                //Token为关闭状态
                //判断是否从未添加过
                if (isNotOpenedToken(context, account.getAddress(), token.getAddress())) {
                    //获取balance
                    QWBalance qwBalance = token.isNative() ?
                            getQKCNativeTokenBalance(balanceDao, account, token, accountData, chainDao, shardDao) :
                            getQKCERC20TokenBalance(context, balanceDao, account, token, fromAddress);
                    //余额不为0 或者 native token打开开关
                    if (isNative(token) || (qwBalance != null && !TextUtils.isEmpty(qwBalance.getBalance()) &&
                            !BigInteger.ZERO.equals(Numeric.toBigInt(qwBalance.getBalance())))) {
                        TokenBean bean = new TokenBean();
                        bean.setToken(token);

                        bean.setBalance(qwBalance);
                        //打开token显示开关
                        QWWalletToken tokenBean = new QWWalletToken();
                        tokenBean.setTokenAddress(token.getAddress());
                        tokenBean.setAccountAddress(account.getAddress());
                        walletTokenDao.insert(tokenBean);

                        if (token.getIsAdd() == 1) {
                            addList.add(bean);
                        } else {
                            cloudList.add(bean);
                        }
                    }
                }
            }
        }
        //排序
        Collections.reverse(addList);
        //服务器数据排序过滤
        cloudList = orderTokenBeanList(cloudList, oderList);
        result.addAll(addList);
        result.addAll(cloudList);
        return result;
    }

    private boolean isNative(QWToken token) {
        //native token默认都显示
        return token.isNative() && token.getType() == Constant.ACCOUNT_TYPE_QKC;
    }

    private QWBalance getAllEthBalance(QWAccount account, QWToken token, QWBalanceDao balanceDao, Web3j ethWeb3j) {
        BigDecimal balance = null;
        try {
            balance = getETHBalance(ethWeb3j, account, token.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (balance != null) {
            String balanceStr = Numeric.toHexStringWithPrefix(balance.toBigInteger());
            //插入或者更新数据库balance
            balanceDao.insertBalance(account, token, balanceStr);
        }
        return balanceDao.queryByWT(account, token);
    }

    private QWBalance getQKCNativeTokenBalance(QWBalanceDao balanceDao, QWAccount account, QWToken token, QKCGetAccountData.AccountData data,
                                               QWChainDao chainDao, QWShardDao shardDao) {
        if (data != null) {
            insertNativeToken(balanceDao, account, data.getPrimary(), token, chainDao, shardDao);
            ArrayList<Account> list = data.getShards();
            if (list != null) {
                for (Account a : list) {
                    insertNativeToken(balanceDao, account, a, token, chainDao, shardDao);
                }
            }
        }
        return getQKCNativeBalance(balanceDao, account, token);
    }

    private void insertNativeToken(QWBalanceDao balanceDao, QWAccount account, Account data, QWToken token, QWChainDao chainDao, QWShardDao shardDao) {
        if (account != null) {
            ArrayList<Balance> list = data.getBalances();
            for (Balance balance : list) {
                if (TextUtils.equals(balance.getTokenId(), token.getAddress())) {
                    String chainStrId = data.getChainId();
                    QWChain chain = chainDao.queryChainByChain(chainStrId);
                    if (chain == null) {
                        chain = new QWChain(chainStrId);
                        chainDao.insert(chain);
                    }
                    String shardId = data.getShardId();
                    QWShard shard = shardDao.queryShardByShard(shardId);
                    if (shard == null) {
                        shard = new QWShard(shardId);
                        shardDao.insert(shard);
                    }

                    //插入或者更新数据库balance
                    String balanceStr = balance.getBalance();
                    QWBalance defaultBalance = balanceDao.queryByWTCS(account, token, chain, shard);
                    if (BigInteger.ZERO.equals(Numeric.toBigInt(balanceStr))) {
                        if (defaultBalance != null) {
                            balanceDao.delete(defaultBalance);
                        }
                        break;
                    }
                    balanceDao.insertQKCTokenBalance(account, token, balanceStr, chain, shard);
                    break;
                }
            }
        }
    }

    private QWBalance getQKCERC20TokenBalance(Context context, QWBalanceDao balanceDao, QWAccount account, QWToken token, String fromAddress) {
        BigDecimal balance = null;
        try {
            balance = getBalance(context, account.getAddress(), fromAddress, token);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (balance != null) {
            String balanceStr = Numeric.toHexStringWithPrefix(balance.toBigInteger());
            //插入或者更新数据库balance
            balanceDao.insertBalance(account, token, balanceStr);
        }
        return balanceDao.queryByWT(account, token);
    }

    //获取trc10 token余额
    private Map<String, Long> getAllTrxTrc10TokenBalance(String walletAddress) {
        byte[] address = TronWalletClient.decodeFromBase58Check(walletAddress);

        //获取balance
        Protocol.Account account = new TronWalletClient().queryAccount(address, false);
        return account.getAssetV2Map();
    }

    private QWBalance getTrxTrc10TokenBalance(QWBalanceDao balanceDao, QWAccount account, QWToken token, Map<String, Long> map) {
        Long value = 0L;
        if (map != null) {
            for (Map.Entry<String, Long> balance : map.entrySet()) {
                if (TextUtils.equals(balance.getKey(), token.getAddress())) {
                    value = balance.getValue();
                }
            }
        }
        if (value != 0) {
            String balanceStr = Numeric.toHexStringWithPrefix(new BigInteger(value + ""));
            //插入或者更新数据库balance
            balanceDao.insertBalance(account, token, balanceStr);
        }
        return balanceDao.queryByWT(account, token);
    }

    //获取Trx token balance
    private QWBalance getTrxTrc20TokenBalance(HttpService httpService, QWAccount account, QWToken token, QWBalanceDao balanceDao, String fromAddress) {
        BigDecimal balance = null;
        try {
            balance = getTrx20Balance(httpService, fromAddress, token.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (balance != null) {
            String balanceStr = Numeric.toHexStringWithPrefix(balance.toBigInteger());
            //插入或者更新数据库balance
            balanceDao.insertBalance(account, token, balanceStr);
        }
        return balanceDao.queryByWT(account, token);
    }

    //是否有打开显示过Token
    private boolean isNotOpenedToken(Context context, String accountAddress, String tokenAddress) {
        QWWalletTokenDao walletToken = new QWWalletTokenDao(context);
        return walletToken.queryByWalletAndToken(accountAddress, tokenAddress) == null;
    }

    private ArrayList<TokenBean> orderTokenBeanList(ArrayList<TokenBean> list, QWTokenListOrder order) {
        if (order == null || TextUtils.isEmpty(order.getTokenList())) {
            return list;
        }

        String json = order.getTokenList();
        ArrayList<TokenBean> cloudList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(json);
            int size = jsonArray.length();
            for (int i = 0; i < size; i++) {
                String address = jsonArray.getString(i);
                for (TokenBean token : list) {
                    if (TextUtils.equals(token.getToken().getAddress(), address)) {
                        cloudList.add(token);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return cloudList;
    }

    @Override
    public Single<TokenBean> getTokenBalance(Context context, QWAccount account, QWToken temp) {
        return Single.fromCallable(() -> {
            QWTokenDao dao = new QWTokenDao(context);
            QWToken token = dao.queryTokenByAddress(temp.getAddress());

            QWBalanceDao balanceDao = new QWBalanceDao(context);
            QWBalance balance = balanceDao.queryByWT(account, token);

            TokenBean bean = new TokenBean();
            bean.setToken(token);
            bean.setBalance(balance);

            //获取balance
            //balance Token是整个链上的总数，不区分分片
            BigDecimal balanceFloat;
            if (account.isEth()) {
                balanceFloat = getETHBalance(account, token.getAddress());
            } else if (account.isTRX()) {
                if (TronWalletClient.isTronErc10TokenAddressValid(temp.getAddress())) {
                    balanceFloat = getTrx10Balance(account.getAddress(), token.getAddress());
                } else {
                    HttpService httpService = new HttpService(Constant.TRON_MAIN_NET_PATH + Constant.TRON_MAIN_NET_METHOD_TRIGGER_SMART, false);
                    balanceFloat = getTrx20Balance(httpService, account.getAddress(), token.getAddress());
                }
            } else {
                String fromAddress = Numeric.parseAddressToEth(account.getAddress());
                balanceFloat = getBalance(context, account.getAddress(), fromAddress, token);
            }
            if (balanceFloat != null) {
                QWBalance qwBalance = new QWBalance();
                String balanceStr = Numeric.toHexStringWithPrefix(balanceFloat.toBigInteger());
                qwBalance.setBalance(balanceStr);
                bean.setBalance(qwBalance);

                //插入或者更新数据库balance
                balanceDao.insertBalance(account, token, balanceStr);
            }
            return bean;
        }).subscribeOn(Schedulers.newThread());
    }

    @Override
    public Observable<TokenBean> findTokenBalance(Context context, QWAccount account, QWToken temp) {
        return Observable.create(e -> {
            //获取数据库中token
            QWTokenDao dao = new QWTokenDao(context);
            QWToken token = dao.queryTokenByAddress(temp.getAddress());

            QWBalanceDao balanceDao = new QWBalanceDao(context);
            QWBalance balance = balanceDao.queryByWT(account, token);

            TokenBean bean = new TokenBean();
            bean.setToken(token);
            bean.setBalance(balance);
            e.onNext(bean);

            if (e.isDisposed()) {
                e.onComplete();
                return;
            }

            //获取balance
            //balance Token是整个链上的总数，不区分分片
            BigDecimal balanceFloat;
            if (account.isEth()) {
                balanceFloat = getETHBalance(account, token.getAddress());
            } else if (account.isTRX()) {
                if (TronWalletClient.isTronErc10TokenAddressValid(temp.getAddress())) {
                    balanceFloat = getTrx10Balance(account.getAddress(), token.getAddress());
                } else {
                    HttpService httpService = new HttpService(Constant.TRON_MAIN_NET_PATH + Constant.TRON_MAIN_NET_METHOD_TRIGGER_SMART, false);
                    balanceFloat = getTrx20Balance(httpService, account.getAddress(), token.getAddress());
                }
            } else {
                String fromAddress = Numeric.parseAddressToEth(account.getAddress());
                balanceFloat = getBalance(context, account.getAddress(), fromAddress, token);
            }
            if (balanceFloat != null) {
                QWBalance qwBalance = new QWBalance();
                String balanceStr = Numeric.toHexStringWithPrefix(balanceFloat.toBigInteger());
                qwBalance.setBalance(balanceStr);
                bean.setBalance(qwBalance);

                //插入或者更新数据库balance
                balanceDao.insertBalance(account, token, balanceStr);
            }
            e.onNext(bean);
        });
    }

    @Override
    public Single<TokenBean> findTokenSingleBalance(Context context, QWAccount account, QWToken temp) {
        return Single.fromCallable(() -> {
            QWTokenDao dao = new QWTokenDao(context);
            QWToken token = dao.queryTokenByAddress(temp.getAddress());

            TokenBean bean = new TokenBean();
            bean.setToken(token);

            QWBalanceDao balanceDao = new QWBalanceDao(context);
            //获取balance
            //balance Token是整个链上的总数，不区分分片
            BigDecimal balanceFloat;
            if (!account.isEth()) {
                String fromAddress = Numeric.parseAddressToEth(account.getAddress());
                balanceFloat = getBalance(context, account.getAddress(), fromAddress, token);
            } else {
                balanceFloat = getETHBalance(account, token.getAddress());
            }
            if (balanceFloat != null) {
                QWBalance qwBalance = new QWBalance();
                String balanceStr = Numeric.toHexStringWithPrefix(balanceFloat.toBigInteger());
                qwBalance.setBalance(balanceStr);
                bean.setBalance(qwBalance);

                //插入或者更新数据库balance
                balanceDao.insertBalance(account, token, balanceStr);
            } else {
                QWBalance qwBalance = balanceDao.queryByWT(account, token);
                bean.setBalance(qwBalance);
            }
            return bean;
        });
    }

    //获取ERC20 TOKEN
    @Override
    public Single<QWToken> fetchAddToken(Context context, String address, int accountType) {
        if (accountType == Constant.ACCOUNT_TYPE_ETH) {
            return searchETHToken(address);
        }

        if (accountType == Constant.ACCOUNT_TYPE_TRX) {
            return searchTRXToken(address);
        }

        return searchQKCToken(context, address);
    }

    private Single<QWToken> searchETHToken(String address) {
        return Single.fromCallable(() -> {
                    AVQuery<AVObject> avQuery = new AVQuery<>("FullTokenListS3");
                    avQuery.whereMatches("address", address, "gi");
                    avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_ETH)
                            .whereEqualTo("chainId", (int) Constant.sETHNetworkId);
                    try {
                        List<AVObject> list = avQuery.find();
                        if (list != null && !list.isEmpty()) {
                            return parseQWToken(list.get(0), Constant.ACCOUNT_TYPE_ETH);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    String name = getETHTokenName(address);
                    if (TextUtils.isEmpty(name)) {
                        return null;
                    }
                    String symbol = getETHTokenSymbol(address);
                    if (TextUtils.isEmpty(symbol)) {
                        return null;
                    }
                    BigDecimal decimals = getETHTokenDecimals(address);
                    if (decimals == null) {
                        return null;
                    }

                    QWToken token = new QWToken();
                    token.setAddress(Keys.toChecksumHDAddress(address));
                    token.setName(name);
                    token.setSymbol(symbol);
                    token.setDecimals(decimals.toPlainString());
                    token.setType(Constant.ACCOUNT_TYPE_ETH);
                    token.setChainId((int) Constant.sETHNetworkId);
                    return token;
                }
        );
    }

    private Single<QWToken> searchTRXToken(String address) {
        return Single.fromCallable(() -> {
                    AVQuery<AVObject> avQuery = new AVQuery<>("FullTokenListS3");
                    avQuery.whereMatches("address", address, "gi");
                    avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_TRX);
                    try {
                        List<AVObject> list = avQuery.find();
                        if (list != null && !list.isEmpty()) {
                            return parseQWToken(list.get(0), Constant.ACCOUNT_TYPE_TRX);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //trc10
                    QWToken token = searchTrc10(address);
                    if (token != null) {
                        return token;
                    }

                    //trc20
                    byte[] byteAddress = TronWalletClient.decodeFromBase58Check(address);
                    String name = getTrxTokenName(byteAddress);
                    if (TextUtils.isEmpty(name)) {
                        return null;
                    }
                    String symbol = getTrxTokenSymbol(byteAddress);
                    if (TextUtils.isEmpty(symbol)) {
                        return null;
                    }
                    BigDecimal decimals = getTrxTokenDecimals(byteAddress);
                    if (decimals == null) {
                        return null;
                    }
                    token = new QWToken();
                    token.setAddress(address);
                    token.setName(name);
                    token.setSymbol(symbol);
                    token.setDecimals(decimals.toPlainString());
                    token.setType(Constant.ACCOUNT_TYPE_TRX);
                    token.setChainId((int) Constant.sETHNetworkId);
                    return token;
                }
        );
    }

    private QWToken searchTrc10(String address) {
        Contract.AssetIssueContract contract = new TronWalletClient().getAssetIssueById(address);
        if (contract == null || TextUtils.isEmpty(contract.getAbbr().toStringUtf8())) {
            return null;
        }
        QWToken token = new QWToken();
        token.setAddress(address);
        token.setIsNative(1);
        token.setName(contract.getName().toStringUtf8());
        token.setSymbol(contract.getAbbr().toStringUtf8());
        token.setDecimals(String.valueOf(contract.getPrecision()));
        token.setType(Constant.ACCOUNT_TYPE_TRX);
        token.setChainId(Constant.TRX_MAIN_NETWORK);
        return token;
    }

    private Single<QWToken> searchQKCToken(Context context, String address) {
        return Single.fromCallable(() -> {
                    AVQuery<AVObject> avQuery = new AVQuery<>("FullTokenListS3");
                    avQuery.whereMatches("address", address, "gi");
                    avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_QKC)
                            .whereEqualTo("chainId", Constant.sNetworkId.intValue());
                    try {
                        List<AVObject> list = avQuery.find();
                        if (list != null && !list.isEmpty()) {
                            return parseQWToken(list.get(0), Constant.ACCOUNT_TYPE_ETH);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //Native Token
                    QWToken token = searchQKCNative(context, address);
                    if (token != null) {
                        return token;
                    }

                    //QRC20
                    String name = getTokenName(context, address);
                    if (TextUtils.isEmpty(name)) {
                        return null;
                    }
                    String symbol = getTokenSymbol(context, address);
                    if (TextUtils.isEmpty(symbol)) {
                        return null;
                    }
                    BigDecimal decimals = getTokenDecimals(context, address);
                    if (decimals == null) {
                        return null;
                    }
                    token = new QWToken();
                    token.setAddress(address);
                    token.setName(name);
                    token.setSymbol(symbol);
                    token.setDecimals(decimals.toPlainString());
                    token.setType(Constant.ACCOUNT_TYPE_QKC);
                    token.setChainId(Constant.sNetworkId.intValue());
                    return token;
                }
        );
    }

    //查询QKC Native Token
    private QWToken searchQKCNative(Context context, String tokenIdOrName) {
        //是否是合法的QKC native Token名字
        if (QWWalletUtils.isQKCNativeTokenName(tokenIdOrName)) {
            //转成Native id
            BigInteger tokenID = QWWalletUtils.convertTokenName2Num(tokenIdOrName);
            BigDecimal createTime = getNativeTokenInfo(context, tokenID);
            if (createTime == null || createTime.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            QWToken token = new QWToken();
            token.setAddress(Numeric.toHexStringWithPrefix(tokenID));
            token.setIsNative(1);
            token.setName(tokenIdOrName.toUpperCase());
            token.setSymbol(tokenIdOrName.toUpperCase());
            token.setDecimals(QWTokenDao.QKC_DECIMALS);
            token.setType(Constant.ACCOUNT_TYPE_QKC);
            token.setChainId(Constant.sNetworkId.intValue());
            return token;
        }
        return null;
    }

    //添加ERC20 TOKEN
    @Override
    public Single<String> addToken(Context context, String currentAddress, QWToken token) {
        return Single.fromCallable(() -> {
            QWTokenDao dao = new QWTokenDao(context);
            token.setIsAdd(1);
            token.setAddress(Keys.toChecksumHDAddress(token.getAddress()));

            QWToken temp = dao.queryTokenByAddress(token.getAddress());
            if (temp == null) {
                dao.insert(token);
            } else if (temp.isDelete()) {
                token.setAddress(temp.getAddress());
                temp.setIsDelete(0);
                dao.update(temp);
            } else {
                token.setAddress(temp.getAddress());
            }

            QWWalletTokenDao tokenDao = new QWWalletTokenDao(context);
            QWWalletToken walletToken = new QWWalletToken();
            walletToken.setTokenAddress(token.getAddress());
            walletToken.setAccountAddress(currentAddress);
            tokenDao.insert(walletToken);

            return token.getAddress();
        });
    }

    //从服务区获取最新token
    private QWTokenListOrder updateTokenInfoCache(Context context, QWAccount account) {
        int chainId = getChainId(account);
        List<AVObject> list = null;
        AVQuery<AVObject> avQuery = new AVQuery<>("Token");
        if (account.isEth()) {
            avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_ETH)
                    .whereEqualTo("chainId", (int) Constant.sETHNetworkId);
        } else if (account.isTRX()) {
            avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_TRX)
                    .whereEqualTo("chainId", chainId);
        } else {
            avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_QKC)
                    .whereEqualTo("chainId", chainId);
        }
        avQuery.whereEqualTo("approved", true);
        avQuery.orderByDescending("order");
        try {
            list = avQuery.find();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (list == null) {
            return null;
        }

        QWTokenDao dao = new QWTokenDao(context);
        //获取当前已经存在的token
        int accountType = getAccountType(account);
        List<QWToken> removeList = dao.queryAllTokenDefault(accountType, getChainId(account));
        JSONArray jsonArray = new JSONArray();
        for (AVObject object : list) {
            String address = object.getString("address");
            if (accountType == Constant.ACCOUNT_TYPE_ETH || accountType == Constant.ACCOUNT_TYPE_QKC) {
                address = Keys.toChecksumHDAddress(address);
            }
            QWToken token = dao.queryTokenByAddress(address);
            if (token == null) {
                token = parseQWToken(object, accountType);
                dao.insert(token);
            } else {
                //服务器新数据中还存在该Token
                removeList.remove(token);

                token.setIsDelete(0);
                token.setName(object.getString("name"));
                token.setChainId(object.getInt("chainId"));
                token.setIconPath(object.getString("iconURL"));
                token.setDescriptionCn(object.getString("descriptionCn"));
                token.setDescriptionEn(object.getString("descriptionEn"));
                dao.update(token);
            }
            jsonArray.put(address);
        }

        //服务器已不存在的数据，进行开关关闭
        if (removeList != null && !removeList.isEmpty()) {
            QWWalletTokenDao walletToken = new QWWalletTokenDao(context);
            for (QWToken token : removeList) {
                token.setIsDelete(1);
                //对Token做假删除
                dao.falseDelete(token);
                //开关关闭
                walletToken.closeToken(token);
            }
        }

        //更新顺序
        QWTokenListOrderDao listOrderDao = new QWTokenListOrderDao(context);
        QWTokenListOrder order = new QWTokenListOrder();
        order.setType(accountType);
        order.setChainId(getChainId(account));
        order.setTokenList(jsonArray.toString());
        listOrderDao.insert(order);
        return order;
    }

    private BigDecimal getPublicBalance(Context context, String fromAddress, String address) {
        String ethAddress = Numeric.parseAddressToEth(address);
        String responseValue = null;
        Function function = balanceOf(ethAddress);
        try {
            responseValue = callSmartContractFunction(function, context, fromAddress, address);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
        } else {
            return null;
        }
    }

    private BigDecimal getBalance(Context context, String fromAddress, String address, QWToken tokenInfo) {
        Function function = balanceOf(address);
        String responseValue = null;
        try {
            responseValue = callSmartContractFunction(function, context, fromAddress, tokenInfo.getAddress());
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
        } else {
            return null;
        }
    }

    private static Function balanceOf(String owner) {
        return new Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));
    }

    //获取erc20 Token代币balance
    private String callSmartContractFunction(Function function,
                                             Context context,
                                             String fromAddress, String contractAddress) throws Exception {
        String from = QWWalletUtils.changeChainShardToDes(context, fromAddress, contractAddress);
        String encodedFunction = FunctionEncoder.encode(function);
        QuarkCall response = Web3jFactory.build(new HttpService(Constant.sQKCNetworkPath, false))
                .call(new TokenBalance(from, contractAddress,
                        "0x0",
                        Numeric.toHexStringWithPrefix(new BigInteger(Constant.DEFAULT_GAS_TOKEN_LIMIT)),
                        encodedFunction))
                .send();
        return response.getValue();
    }

    //创建合约token data字段 转账Token
    public static byte[] createTokenTransferData(String to, BigInteger tokenAmount) {
        List<Type> params = Arrays.asList(new Address(to), new Uint256(tokenAmount));
        List<TypeReference<?>> returnTypes = Collections.singletonList(new TypeReference<Bool>() {
        });
        Function function = new Function("transfer", params, returnTypes);
        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    //创建合约token data字段 购买公募Token
    public static byte[] createBuyTokenTransferData() {
        Function function = new Function("buy", new ArrayList<>(), new ArrayList<>());
        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    //获取合约token name基本信息
    private String getTokenName(Context context, String address) {
        Function function = new Function(
                "name",
                new ArrayList<>(),
                Collections.singletonList(new TypeReference<Utf8String>() {
                }));
        String responseValue = null;
        try {
            responseValue = callSmartContractFunction(function, context, address, address);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return ((Utf8String) response.get(0)).getValue();
        } else {
            return null;
        }
    }

    //获取合约token symbol信息
    private String getTokenSymbol(Context context, String address) {
        Function function = new Function(
                "symbol",
                new ArrayList<>(),
                Collections.singletonList(new TypeReference<Utf8String>() {
                }));
        String responseValue = null;
        try {
            responseValue = callSmartContractFunction(function, context, address, address);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return ((Utf8String) response.get(0)).getValue();
        } else {
            return null;
        }
    }

    //获取合约token decimals信息
    private BigDecimal getTokenDecimals(Context context, String address) {
        Function function = new Function(
                "decimals",
                new ArrayList<>(),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));
        String responseValue = null;
        try {
            responseValue = callSmartContractFunction(function, context, address, address);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
//            return ((Uint256) response.get(0)).getValue();
        } else {
            return null;
        }
    }

    //查询native token是否存在
    private BigDecimal getNativeTokenInfo(Context context, BigInteger tokenID) {
        List<Type> params = Collections.singletonList(new Uint128(tokenID));
        List<TypeReference<?>> returnTypes = Arrays.asList(
                new TypeReference<Uint64>() {
                },//createTime
                new TypeReference<Address>() {
                },//owner
                new TypeReference<Uint256>() {
                });//totalSupply
        Function function = new Function(
                "getNativeTokenInfo",
                params,
                returnTypes);
        String responseValue = null;
        try {
            responseValue = callSmartContractFunction(function, context, Constant.QKC_NATIVE_TOKEN_INFO_ADDRESS, Constant.QKC_NATIVE_TOKEN_INFO_ADDRESS);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 3) {
            return new BigDecimal(((Uint64) response.get(0)).getValue());
        } else {
            return null;
        }
    }

    @Override
    public Single<Boolean> deleteToken(Context context, QWToken token) {
        return Single.fromCallable(() -> {
            QWTokenDao tokenDao = new QWTokenDao(context);
            QWToken newToken = tokenDao.queryTokenByAddress(token.getAddress());

            QWBalanceDao balanceDao = new QWBalanceDao(context);
            balanceDao.delete(newToken);

            QWTransactionDao transactionDao = new QWTransactionDao(context);
            transactionDao.delete(newToken);

            tokenDao.delete(newToken);
            return true;
        });
    }

    @Override
    public Observable<List<QWPublicScale>> fetchPublicSale(Context context, int accountType, boolean needRefresh) {
        return Observable.create(e -> {

            long time = System.currentTimeMillis();

            int chainId = getNetworkId(accountType);
            //获取数据库中publicScale
            QWPublicScaleDao dao = new QWPublicScaleDao(context);
            List<QWPublicScale> publicScaleList = dao.queryAll(accountType, chainId);
            List<QWPublicScale> checkList = new ArrayList<>();
            if (publicScaleList != null && !publicScaleList.isEmpty()) {
                checkList = checkPublicSaleData(publicScaleList, time);
            }
            e.onNext(checkList);

            String typeName = accountType == Constant.ACCOUNT_TYPE_ETH ?
                    Constant.HD_PATH_CODE_ETH + "" + (int) Constant.sETHNetworkId :
                    (accountType == Constant.ACCOUNT_TYPE_QKC ?
                            Constant.HD_PATH_CODE_QKC + "" + Constant.sNetworkId.toString() :
                            Constant.HD_PATH_CODE_TRX + "" + 0
                    );
            boolean refresh = needRefresh || ToolUtils.isLongDayTime(SharedPreferencesUtils.getPublicSaleListTime(context, typeName));
            if (!refresh && accountType == Constant.ACCOUNT_TYPE_ETH
                    && (Constant.sETHNetworkId == Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX || Constant.sETHNetworkId == Constant.ETH_PUBLIC_PATH_KOVAN_INDEX)) {
                refresh = true;
            }
            //刷新缓存
            if (ConnectionUtil.isInternetConnection(context)) {
                //是否从服务器拉取list
                if (refresh) {
                    //服务区拉取数据
                    AVQuery<AVObject> avQuery = new AVQuery<>("PublicSaleS3");
                    if (accountType == Constant.ACCOUNT_TYPE_ETH) {
                        avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_ETH)
                                .whereEqualTo("chainId", chainId)
                                .whereEqualTo("approved", true);
                    } else if (accountType == Constant.ACCOUNT_TYPE_TRX) {
                        avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_TRX)
                                .whereEqualTo("chainId", chainId)
                                .whereEqualTo("approved", true);
                    } else {
                        avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_QKC)
                                .whereEqualTo("chainId", chainId)
                                .whereEqualTo("approved", true);
                    }
                    avQuery.orderByDescending("order");
                    avQuery.include("token");
                    List<AVObject> list = avQuery.find();
                    if (list != null) {
                        dao.deleteAll(accountType, chainId);
                        QWTokenDao tokenDao = new QWTokenDao(context);
                        for (AVObject object : list) {
                            saveTokenAndSale(object, accountType, tokenDao, dao);
                        }
                        SharedPreferencesUtils.setPublicSaleListTime(context, typeName);
                    }

                    publicScaleList = dao.queryAll(accountType, chainId);
                }
                if (publicScaleList == null || e.isDisposed()) {
                    e.onNext(new ArrayList<>());
                    e.onComplete();
                    return;
                }

                //拉取balance
                Web3j web3j = Web3jFactory.build(new HttpService(Constant.sEthNetworkPath, false));
                for (QWPublicScale scale : publicScaleList) {
                    if (e.isDisposed()) {
                        e.onComplete();
                        return;
                    }
                    String address = scale.getToken().getAddress();
                    BigDecimal balance = null;
                    try {
                        balance = WalletUtils.isValidAddress(address) ? getEthPublicBalance(web3j, address) : getPublicBalance(context, address, address);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    if (balance != null) {
                        scale.setAvailability(Numeric.toHexStringWithPrefix(balance.toBigInteger()));
                        dao.update(scale);
                    }
                }

                //过滤
                e.onNext(checkPublicSaleData(publicScaleList, time));
            } else {
                e.onNext(checkList);
            }

            e.onComplete();
        });
    }

    private int getNetworkId(int accountType) {
        switch (accountType) {
            case Constant.ACCOUNT_TYPE_ETH:
                return (int) Constant.sETHNetworkId;
            case Constant.ACCOUNT_TYPE_TRX:
                return 0;
            default:
                return Constant.sNetworkId.intValue();
        }
    }

    private synchronized void saveTokenAndSale(AVObject object, int accountType, QWTokenDao tokenDao, QWPublicScaleDao dao) {
        //取出token
        AVObject tokenItem = object.getAVObject("token");
        String address = tokenItem.getString("address");
        QWToken token = tokenDao.queryTokenByAddress(address);
        if (token == null) {
            QWToken temp = parseQWToken(tokenItem, accountType);
            tokenDao.insert(temp);
            token = tokenDao.queryTokenByAddress(address);
        } else {
            token.setIsDelete(0);
            token.setName(tokenItem.getString("name"));
            token.setChainId(tokenItem.getInt("chainId"));
            token.setIconPath(tokenItem.getString("iconURL"));
            token.setDescriptionCn(tokenItem.getString("descriptionCn"));
            token.setDescriptionEn(tokenItem.getString("descriptionEn"));
            tokenDao.update(token);
        }

        if (token != null) {
            QWPublicScale publicScale = dao.queryByToken(token);
            if (publicScale == null) {
                publicScale = new QWPublicScale();
            }
            publicScale.setKey(token.getAddress());
            publicScale.setStartTime(object.getString("startTime"));
            publicScale.setEndTime(object.getString("endTime"));
            publicScale.setBuyRate(object.getString("buyRate"));
            publicScale.setBackgroundImageURL(object.getString("backgroundImageURL"));
            publicScale.setChainId(object.getInt("chainId"));
            publicScale.setType(accountType);
            publicScale.setOrder(object.getInt("order"));
            publicScale.setToken(token);
            dao.insert(publicScale);
        }
    }

    private List<QWPublicScale> checkPublicSaleData(List<QWPublicScale> list, long time) {
        ArrayList<QWPublicScale> newArrayList = new ArrayList<>();
        for (QWPublicScale scale : list) {
            long startTime = Long.parseLong(scale.getStartTime()) * 1000;
            long endTime = Long.parseLong(scale.getEndTime()) * 1000;
            if (startTime <= time && time <= endTime) {
                String a = scale.getAvailability();
                if (!TextUtils.isEmpty(a)) {
                    BigInteger availability = Numeric.toBigInt(scale.getAvailability());
                    if (BigInteger.ZERO.compareTo(availability) < 0) {
                        newArrayList.add(scale);
                    }
                }
            }
        }
        return newArrayList;
    }

    @Override
    public Single<QWBalance[]> findPublicSaleBalance(Context context, QWAccount account, QWToken token) {
        return Single.fromCallable(() -> {
            QWBalance[] list = new QWBalance[2];

            //获取balance
            //balance Token是整个链上的总数，不区分分片
            BigDecimal balanceFloat;
            if (account.isEth()) {
                balanceFloat = getETHBalance(account, token.getAddress());
            } else {
                String fromAddress = Numeric.parseAddressToEth(account.getAddress());
                balanceFloat = getBalance(context, account.getAddress(), fromAddress, token);
            }
            if (balanceFloat != null) {
                QWBalance qwBalance = new QWBalance();
                String balanceStr = Numeric.toHexStringWithPrefix(balanceFloat.toBigInteger());
                qwBalance.setBalance(balanceStr);
                list[0] = qwBalance;

                //插入或者更新数据库balance
                QWBalanceDao balanceDao = new QWBalanceDao(context);
                balanceDao.insertBalance(account, token, balanceStr);
            }

            String address = token.getAddress();
            BigDecimal balance;
            if (account.isEth()) {
                balance = getEthPublicBalance(address);
            } else {
                balance = getPublicBalance(context, account.getAddress(), address);
            }
            if (balance != null) {
                QWBalance qwBalance = new QWBalance();
                qwBalance.setBalance(Numeric.toHexStringWithPrefix(balance.toBigInteger()));
                list[1] = qwBalance;

                QWPublicScaleDao dao = new QWPublicScaleDao(context);
                QWPublicScale publicScale = dao.queryByToken(token);
                publicScale.setAvailability(Numeric.toHexStringWithPrefix(balance.toBigInteger()));
                dao.update(publicScale);
            }

            return list;
        });
    }

    @Override
    public Single<List<QWToken>> fetchTokenList(Context context, QWAccount account, int loadType) {
        return Single.fromCallable(() -> {
            List<QWToken> result = getTokenList(context, account);
            //只查询数据库
            if (loadType == Constant.TOKEN_LIST_TYPE_LOAD_DB) {
                return result;
            }

            //不为空时不查询
            if (loadType == Constant.TOKEN_LIST_TYPE_LOAD_NOT_NULL) {
                if (!result.isEmpty()) {
                    return result;
                }
            }

            //刷新缓存
            if (ConnectionUtil.isInternetConnection(context)) {
                //刷新缓存
                updateTokenInfoCache(context, account);
                //获取新数据
                result = getTokenList(context, account);
            }
            return result;
        });
    }

    private List<QWToken> getTokenList(Context context, QWAccount account) {
        ArrayList<QWToken> result = new ArrayList<>();

        //获取数据库中token
        int accountType = getAccountType(account);
        QWTokenDao dao = new QWTokenDao(context);
        List<QWToken> list = dao.queryAllTokenByType(accountType);
        if (list != null) {
            //显示顺序
            QWTokenListOrderDao listOrderDao = new QWTokenListOrderDao(context);
            QWTokenListOrder oderList = listOrderDao.queryTokenList(accountType, getChainId(account));

            //开关打开的token
            QWWalletTokenDao walletToken = new QWWalletTokenDao(context);
            List<QWWalletToken> adjustTokenList = walletToken.queryByWallet(account.getAddress());

            ArrayList<QWToken> addList = new ArrayList<>();
            ArrayList<QWToken> cloudList = new ArrayList<>();
            //服务器获取的数据
            for (QWToken token : list) {
                if (QWTokenDao.BTC_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
                if (QWTokenDao.ETH_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
                if (QWTokenDao.TRX_SYMBOL.equals(token.getSymbol())) {
                    continue;
                }
                if (QWTokenDao.QKC_SYMBOL.equals(token.getSymbol()) && !account.isEth()) {
                    continue;
                }
                if (accountType == Constant.ACCOUNT_TYPE_ETH && token.getChainId() != Constant.sETHNetworkId) {
                    continue;
                }
                if (accountType == Constant.ACCOUNT_TYPE_QKC && token.getChainId() != Constant.sNetworkId.intValue()) {
                    continue;
                }

                if (token.getIsAdd() == 1) {
                    //手动添加token
                    addList.add(token);
                } else {
                    //服务区获取token
                    cloudList.add(token);
                }

                //自定义添加的Token数据
                if (adjustTokenList != null) {
                    for (QWWalletToken qwWalletToken : adjustTokenList) {
                        if (TextUtils.equals(qwWalletToken.getTokenAddress(), token.getAddress())) {
                            token.setIsShow(1);
                            break;
                        }
                    }
                }
            }

            //手动添加token拍序
            Collections.reverse(addList);
            //服务区数据排序过滤
            cloudList = orderTokenList(cloudList, oderList);

            result.addAll(addList);
            result.addAll(cloudList);
        }
        return result;
    }

    private ArrayList<QWToken> orderTokenList(ArrayList<QWToken> list, QWTokenListOrder order) {
        if (order == null || TextUtils.isEmpty(order.getTokenList())) {
            return list;
        }

        String json = order.getTokenList();
        ArrayList<QWToken> cloudList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(json);
            int size = jsonArray.length();
            for (int i = 0; i < size; i++) {
                String address = jsonArray.getString(i);
                for (QWToken token : list) {
                    if (TextUtils.equals(token.getAddress(), address)) {
                        cloudList.add(token);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return cloudList;
    }

    private int getType(QWAccount account) {
        if (account.isEth()) {
            return Constant.ACCOUNT_TYPE_ETH;
        }
        if (account.isTRX()) {
            return Constant.ACCOUNT_TYPE_TRX;
        }

        return Constant.ACCOUNT_TYPE_QKC;
    }

    @Override
    public Single<List<QWToken>> searchTokenList(QWAccount account, String key) {
        return Single.fromCallable(() -> {
            List<QWToken> result = new ArrayList<>();
            String head = "(.*)(";
            String tail = ")(.*)";
            String match = head + key + tail;

            ArrayList<AVQuery<AVObject>> queryList = new ArrayList<>();
            AVQuery<AVObject> nameQuery = new AVQuery<>("FullTokenListS3");
            nameQuery.whereMatches("name", match, "gi");
            queryList.add(nameQuery);
            AVQuery<AVObject> symbolQuery = new AVQuery<>("FullTokenListS3");
            symbolQuery.whereMatches("symbol", match, "gi");
            queryList.add(symbolQuery);

            int chainId = getChainId(account);
            AVQuery<AVObject> avQuery = AVQuery.or(queryList);
            avQuery.limit(Constant.ERC_TOKEN_SEARCH_LIMIT);
            if (account.isEth()) {
                avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_ETH);
            } else if (account.isTRX()) {
                avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_TRX);
            } else {
                avQuery.whereEqualTo("coinType", Constant.HD_PATH_CODE_QKC);
            }
            avQuery.whereEqualTo("chainId", chainId);
            List<AVObject> list;
            try {
                list = avQuery.find();
            } catch (Exception e) {
                return result;
            }

            if (list != null) {
                int type = getType(account);
                for (AVObject object : list) {
                    QWToken token = parseQWToken(object, type);
                    result.add(token);
                }
            }
            return result;
        });
    }

    private QWToken parseQWToken(AVObject object, int type) {
        String address = object.getString("address");
        QWToken token = new QWToken();
        token.setName(object.getString("name"));
        token.setIconPath(object.getString("iconURL"));
        token.setAddress(Keys.toChecksumHDAddress(address));
        token.setSymbol(object.getString("symbol"));
        token.setTotalSupply(object.getString("totalSupply"));
        token.setDecimals(object.getString("decimals"));
        token.setUrl(object.getString("URL"));
        token.setDescriptionCn(object.getString("descriptionCn"));
        token.setDescriptionEn(object.getString("descriptionEn"));
        token.setType(type);
        token.setChainId(object.getInt("chainId"));
        token.setIsNative(object.getBoolean("native") ? 1 : 0);
        return token;
    }

    //******************eth************************
    //获取ETH Token Balance
    private BigDecimal getETHBalance(Web3j web3j, QWAccount account, String contractAddress) throws Exception {
        Function function = balanceOf(account.getAddress());
        String responseValue = callSmartContractFunction(web3j, function, contractAddress, account);

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
        } else {
            return null;
        }
    }

    private BigDecimal getETHBalance(QWAccount account, String contractAddress) throws Exception {
        Function function = balanceOf(account.getAddress());
        String responseValue = callSmartContractFunction(function, contractAddress, account);

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
        } else {
            return null;
        }
    }

    private BigDecimal getEthPublicBalance(Web3j web3j, String address) {
        String responseValue = null;
        Function function = balanceOf(address);
        try {
            responseValue = callSmartContractFunction(web3j, function, address, address);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
        } else {
            return null;
        }
    }

    private BigDecimal getEthPublicBalance(String address) {
        String responseValue = null;
        Function function = balanceOf(address);
        try {
            responseValue = callSmartContractFunction(function, address, address);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
        } else {
            return null;
        }
    }

    //获取合约token name基本信息
    private String getETHTokenName(String address) {
        Function function = new Function(
                "name",
                new ArrayList<>(),
                Collections.singletonList(new TypeReference<Utf8String>() {
                }));
        String responseValue = null;
        try {
            responseValue = callSmartContractFunction(function, address, address);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return ((Utf8String) response.get(0)).getValue();
        } else {
            return null;
        }
    }

    //获取合约token symbol信息
    private String getETHTokenSymbol(String address) {
        Function function = new Function(
                "symbol",
                new ArrayList<>(),
                Collections.singletonList(new TypeReference<Utf8String>() {
                }));
        String responseValue = null;
        try {
            responseValue = callSmartContractFunction(function, address, address);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return ((Utf8String) response.get(0)).getValue();
        } else {
            return null;
        }
    }

    //获取合约token decimals信息
    private BigDecimal getETHTokenDecimals(String address) {
        Function function = new Function(
                "decimals",
                new ArrayList<>(),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));
        String responseValue = null;
        try {
            responseValue = callSmartContractFunction(function, address, address);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
//            return ((Uint256) response.get(0)).getValue();
        } else {
            return null;
        }
    }

    private String callSmartContractFunction(Function function, String contractAddress, QWAccount account) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = Web3jFactory
                .build(new HttpService(Constant.sEthNetworkPath, false))
                .ethCall(Transaction.createEthCallTransaction(account.getAddress(), contractAddress, encodedFunction), DefaultBlockParameterName.LATEST)
                .send();
        return response.getValue();
    }

    private String callSmartContractFunction(Web3j web3j, Function function, String contractAddress, QWAccount account) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j
                .ethCall(Transaction.createEthCallTransaction(account.getAddress(), contractAddress, encodedFunction), DefaultBlockParameterName.LATEST)
                .send();
        return response.getValue();
    }

    private String callSmartContractFunction(Function function, String contractAddress, String address) throws Exception {
        return callSmartContractFunction(Web3jFactory.build(new HttpService(Constant.sEthNetworkPath, false)),
                function,
                contractAddress,
                address);
    }

    private String callSmartContractFunction(Web3j web3j, Function function, String contractAddress, String address) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j
                .ethCall(Transaction.createEthCallTransaction(address, contractAddress, encodedFunction), DefaultBlockParameterName.LATEST)
                .send();
        return response.getValue();
    }

    //******************trx******************
    private BigDecimal getTrx10Balance(String from, String contractAddress) {
        //获取banlance
        Map<String, Long> map = getAllTrxTrc10TokenBalance(from);
        for (Map.Entry<String, Long> balance : map.entrySet()) {
            if (TextUtils.equals(balance.getKey(), contractAddress)) {
                return new BigDecimal(balance.getValue());
            }
        }
        return BigDecimal.ZERO;
    }

    private static Function trxBalanceOf(String owner) {
        return new Function(
                "balanceOf",
                Collections.singletonList(new Uint168(Numeric.toBigInt(owner))),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));
    }

    private BigDecimal getTrx20Balance(HttpService httpService, String from, String contractAddress) throws Exception {
        String own = TronWalletClient.decodeBase58checkToHexString(from);
        String contract = TronWalletClient.decodeBase58checkToHexString(contractAddress);
        Function function = trxBalanceOf(own);
        String responseValue = triggerSmartContractFunction(httpService, function, contract, own);
        if (!TextUtils.isEmpty(responseValue)) {
            return new BigDecimal(Numeric.toBigInt(responseValue));
        } else {
            return null;
        }
    }

    private String triggerSmartContractFunction(HttpService httpService, Function function, String contractAddress, String address) throws Exception {
        String encodedFunction = FunctionEncoder.encodeConstructor(function.getInputParameters());
        TrxRequest request = TrxRequest.createContractTransaction(address, contractAddress, "balanceOf(address)",
                encodedFunction, BigInteger.ZERO, BigInteger.ZERO);
        GrpcAPI.TransactionExtention response = httpService.triggerSmartContractFunction(request);
        if (response.getConstantResultList() != null) {
            for (ByteString bytes : response.getConstantResultList()) {
                return bytes.toStringUtf8();
            }
        }
        return null;
    }


    private String getTrxTokenName(byte[] address) {
        Function function = new Function(
                "name",
                new ArrayList<>(),
                Collections.singletonList(new TypeReference<Utf8String>() {
                }));
        String encodedFunction = FunctionEncoder.encode(function);
        GrpcAPI.TransactionExtention extention = new TronWalletClient().triggerContract(address, address,
                0L,
                Hex.hexStringToByteArray(encodedFunction),
                0, null);
        if (extention != null && extention.getConstantResult(0) != null) {
            String value = extention.getConstantResult(0).toStringUtf8();
            return stringFilter(value);
        }
        return null;
    }

    private String getTrxTokenSymbol(byte[] address) {
        Function function = new Function(
                "symbol",
                new ArrayList<>(),
                Collections.singletonList(new TypeReference<Utf8String>() {
                }));
        String encodedFunction = FunctionEncoder.encode(function);
        GrpcAPI.TransactionExtention extention = new TronWalletClient().triggerContract(address, address,
                0L,
                Hex.hexStringToByteArray(encodedFunction),
                0, null);
        if (extention != null && extention.getConstantResult(0) != null) {
            String value = extention.getConstantResult(0).toStringUtf8();
            return stringFilter(value);
        }
        return null;
    }

    private BigDecimal getTrxTokenDecimals(byte[] address) {
        Function function = new Function(
                "decimals",
                new ArrayList<>(),
                Collections.singletonList(new TypeReference<Utf8String>() {
                }));
        String encodedFunction = FunctionEncoder.encode(function);
        GrpcAPI.TransactionExtention extention = new TronWalletClient().triggerContract(address, address,
                0L,
                Hex.hexStringToByteArray(encodedFunction),
                0, null);
        if (extention != null && extention.getConstantResult(0) != null) {
            byte[] value = extention.getConstantResult(0).toByteArray();
            return new BigDecimal(Numeric.toBigInt(value));
        }
        return null;
    }

    // 过滤特殊字符
    private static String stringFilter(String str) throws PatternSyntaxException {
        // 只允许字母和数字 // String regEx ="[^a-zA-Z0-9]";
        // 清除掉所有特殊字符
        String regEx = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }


    //查询native token是状态，获取铸币人address
    public NativeGasBean gasReserveToken(Context context, BigInteger tokenID, String contractAddress) {
        List<TypeReference<?>> returnTypes = Arrays.asList(
                new TypeReference<Address>() {
                },
                new TypeReference<Uint64>() {
                },
                new TypeReference<Uint128>() {
                },
                new TypeReference<Uint128>() {
                });
        Function function = new Function(
                "gasReserves",
                Collections.singletonList(new Uint128(tokenID)),
                returnTypes);
        String responseValue = null;
        try {
            responseValue = callSmartContractFunction(function, context, contractAddress, contractAddress);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 4) {
            NativeGasBean bean = new NativeGasBean();
            bean.setAddress(((Address) (response.get(0))).getValue());
            BigDecimal denominator = new BigDecimal(((Uint128) response.get(2)).getValue());
            BigDecimal numerator = new BigDecimal(((Uint128) response.get(3)).getValue());
            if (BigDecimal.ZERO.compareTo(denominator) >= 0) {
                bean.setRefundPercentage(numerator);
            } else {
                bean.setRefundPercentage(numerator.divide(denominator, Constant.QKC_DECIMAL_NUMBER, RoundingMode.CEILING));
            }
            return bean;
        } else {
            return null;
        }
    }

    //获取该token是否充值
    @Override
    public BigDecimal gasReserveTokenBalance(Context context, String address, BigInteger tokenID, String contractAddress) {
        Function function = new Function(
                "gasReserveBalance",
                Arrays.asList(
                        new Uint128(tokenID),
                        new Address(address)
                ),
                Collections.singletonList(new TypeReference<Uint256>() {
                }));
        String responseValue = null;
        try {
            responseValue = callSmartContractFunction(function, context, contractAddress, contractAddress);
        } catch (Exception e) {
            e.printStackTrace();
            //nothing
        }
        if (responseValue == null) {
            return null;
        }
        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
        } else {
            return null;
        }
    }
}
