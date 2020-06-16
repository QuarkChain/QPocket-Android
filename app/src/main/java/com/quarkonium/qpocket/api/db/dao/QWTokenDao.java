package com.quarkonium.qpocket.api.db.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.WalletDBHelper;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.crypto.Keys;
import com.quarkonium.qpocket.util.WalletIconUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * 操作钱包表的DAO类
 */
public class QWTokenDao {

    public static final String QKC_NAME = "QKC";
    public static final String QKC_SYMBOL = "qkc";//token符号
    private static final String QKC_TOTAL_SUPPLY = "10000000000";//总数量
    public static final String QKC_DECIMALS = "18";//有效小数
    public static final String QKC_ADDRESS = "0xEA26c4aC16D4a5A106820BC8AEE85fd0b7b2b664";
    private static final String QKC_DES_CN = "QuarkChain是一个基于分片技术来搭建的灵活、高拓展性且方便使用的区块链底层架构。它是世界上首个成功实现状态分片的公链之一。";
    private static final String QKC_DES_EN = "QuarkChain is a flexible, scalable, and user-oriented blockchain infrastructure by applying sharding technology.";

    public static final String TQKC_ADDRESS = "0x8bb0";

    public static final String ETH_NAME = "ETH";
    public static final String ETH_SYMBOL = "eth";//token符号
    private static final String ETH_TOTAL_SUPPLY = "10000000000";//总数量
    private static final String ETH_DECIMALS = "18";//有效小数
    private static final String ETH_DES_CN = "以太坊系统的基础货币。";
    private static final String ETH_DES_EN = "The crypto-fuel for Ethereum network.";

    public static final String TRX_NAME = "TRX";
    public static final String TRX_SYMBOL = "trx";//token符号
    private static final String TRX_TOTAL_SUPPLY = "100000000000";//总数量
    private static final String TRX_DECIMALS = "6";//有效小数
    private static final String TRX_DES_CN = "波场TRON是全球最大的区块链去中心化应用操作系统。";
    private static final String TRX_DES_EN = "TRON is one of the largest blockchain-based operating systems in the world.";

    public static final String BTC_NAME = "BTC";
    public static final String BTC_SYMBOL = "btc";//token符号
    private static final String BTC_TOTAL_SUPPLY = "21000000";//总数量
    private static final String BTC_DECIMALS = "8";//有效小数
    private static final String BTC_DES_CN = "一种点对点的电子现金系统";
    private static final String BTC_DES_EN = "A Peer-to-Peer Electronic Cash System";

    public static QWToken getETHERC20QKCToken() {
        QWToken token = new QWToken();
        token.setName(QKC_NAME);
        token.setSymbol(QKC_SYMBOL);
        token.setTotalSupply(QKC_TOTAL_SUPPLY);
        token.setDecimals(QKC_DECIMALS);
        token.setAddress(QKC_ADDRESS);
        token.setUrl("https://quarkchain.io/");
        token.setDescriptionCn(QKC_DES_CN);
        token.setDescriptionEn(QKC_DES_EN);
        token.setChainId(1);
        token.setIsAdd(1);
        token.setType(Constant.ACCOUNT_TYPE_ETH);

        token.setIconPath(WalletIconUtils.getResourcesUri(MainApplication.getContext(), R.drawable.token_qkc_icon));
        return token;
    }

    public static QWToken getDefaultETHToken() {
        QWToken token = new QWToken();
        token.setName(ETH_NAME);
        token.setSymbol(ETH_SYMBOL);
        token.setTotalSupply(ETH_TOTAL_SUPPLY);
        token.setDecimals(ETH_DECIMALS);
        token.setUrl("https://www.ethereum.org");
        token.setDescriptionCn(ETH_DES_CN);
        token.setDescriptionEn(ETH_DES_EN);
        token.setIconPath(WalletIconUtils.getResourcesUri(MainApplication.getContext(), R.drawable.token_eth_icon));
        token.setChainId(1);
        token.setIsAdd(1);
        token.setType(Constant.ACCOUNT_TYPE_ETH);

        token.setAddress("0x000000...00000000");
        return token;
    }

    public static QWToken getDefaultTRXToken() {
        QWToken token = new QWToken();
        token.setName(TRX_NAME);
        token.setSymbol(TRX_SYMBOL);
        token.setTotalSupply(TRX_TOTAL_SUPPLY);
        token.setDecimals(TRX_DECIMALS);
        token.setUrl("https://tron.network/");
        token.setDescriptionCn(TRX_DES_CN);
        token.setDescriptionEn(TRX_DES_EN);
        token.setIconPath(WalletIconUtils.getResourcesUri(MainApplication.getContext(), R.drawable.token_trx_icon));
        token.setChainId(1);
        token.setIsAdd(1);
        token.setType(Constant.ACCOUNT_TYPE_TRX);

        token.setAddress("1000000");
        return token;
    }

    public static QWToken getTQKCToken() {
        QWToken token = getETHERC20QKCToken();
        token.setAddress(TQKC_ADDRESS);
        token.setChainId(1);
        token.setIsNative(1);
        token.setType(Constant.ACCOUNT_TYPE_QKC);
        return token;
    }

    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<QWToken, Integer> dao;

    public QWTokenDao(Context context) {
        try {
            this.dao = WalletDBHelper.getInstance(context).getDao(QWToken.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertDefault() {
        //qkc
        QWToken tQKC = getTQKCToken();
        insert(tQKC);

        //ETH
        QWToken eth = getDefaultETHToken();
        insert(eth);
        //eth erc20 Token
        QWToken token = getETHERC20QKCToken();
        insert(token);

        //trx
        QWToken trx = getDefaultTRXToken();
        insert(trx);
    }

    // 添加数据
    public void insert(QWToken data) {
        try {
            dao.createOrUpdate(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 添加数据
    public void update(QWToken data) {
        try {
            dao.update(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除数据
    public void delete(QWToken data) {
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //做假删除
    public void falseDelete(QWToken data) {
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public QWToken queryTokenByName(String name) {
        try {
            List<QWToken> list = dao.queryForEq(QWToken.COLUMN_NAME_NAME, name);
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public QWToken queryTokenByAddress(String address) {
        try {
            QueryBuilder<QWToken, Integer> queryBuilder = dao.queryBuilder();
            queryBuilder.where()
                    .eq(QWToken.COLUMN_NAME_ADDRESS, address)
                    .or()
                    .eq(QWToken.COLUMN_NAME_ADDRESS, address.toLowerCase())
                    .or()
                    .eq(QWToken.COLUMN_NAME_ADDRESS, Keys.toChecksumHDAddress(address));
            return queryBuilder.queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWToken> queryAllTokenByType(int type) {
        try {
            return dao
                    .queryBuilder()
                    .where()
                    .eq(QWToken.COLUMN_NAME_IS_DELETE, 0)
                    .and()
                    .eq(QWToken.COLUMN_NAME_TYPE, type)
                    .or()
                    .eq(QWToken.COLUMN_NAME_TYPE, 0)
                    .query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWToken> queryAllTokenDefault(int accountType, int chainId) {
        try {
            return dao
                    .queryBuilder()
                    .where()
                    .eq(QWToken.COLUMN_NAME_TYPE, accountType)
                    .and()
                    .eq(QWToken.COLUMN_NAME_CHAIN_ID, chainId)
                    .and()
                    .eq(QWToken.COLUMN_NAME_IS_ADD, 0)
                    .query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QWToken> queryAllNativeTokenByType(int type) {
        try {
            return dao
                    .queryBuilder()
                    .where()
                    .eq(QWToken.COLUMN_NAME_IS_DELETE, 0)
                    .and()
                    .eq(QWToken.COLUMN_NAME_TYPE, type)
                    .and()
                    .eq(QWToken.COLUMN_NAME_NATIVE, 1)
                    .query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteQKCType() {
        try {
            DeleteBuilder<QWToken, Integer> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where()
                    .eq(QWToken.COLUMN_NAME_TYPE, Constant.ACCOUNT_TYPE_QKC);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
