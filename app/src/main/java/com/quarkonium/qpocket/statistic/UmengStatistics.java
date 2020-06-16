package com.quarkonium.qpocket.statistic;

import android.content.Context;
import android.text.TextUtils;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.Debug;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.ConstantLanguages;
import com.quarkonium.qpocket.util.ToolUtils;

import java.util.HashMap;
import java.util.Map;


public final class UmengStatistics {
    //======================================================================
    // private methods
    //======================================================================
    private static class UmengAgentWrapper {
        static void onEvent(Context context, String key) {
            if (Debug.UMENG_DEBUG_KEY) {
                key = key + "_debug";
            }
        }

        static void onEvent(Context context, String key, String value) {
            if (Debug.UMENG_DEBUG_KEY) {
                key = key + "_debug";
            }
        }

        static void onEvent(Context context, String key, Map<String, String> map) {
            if (Debug.UMENG_DEBUG_KEY) {
                key = key + "_debug";
            }
        }


        static void onEventValue(Context context, String key, Map<String, String> map, int i) {
            if (Debug.UMENG_DEBUG_KEY) {
                key = key + "_debug";
            }
        }
    }

    private static boolean isZH(Context context) {
        return ToolUtils.isZh(context);
    }

    private static boolean isKO(Context context) {
        return ToolUtils.isKo(context);
    }

    private static String appendLocale(Context context, String str) {
        String locale = isZH(context) ? "zh-Hans" : (isKO(context) ? "ko" : "en");
        return str + "_" + locale;
    }

    private static String getCoinType(String address) {
        if (WalletUtils.isQKCValidAddress(address)) {
            return Constant.HD_PATH_CODE_QKC + "";
        }
        if (TronWalletClient.isTronAddressValid(address)) {
            return Constant.HD_PATH_CODE_TRX + "";
        }
        return Constant.HD_PATH_CODE_ETH + "";
    }

    //**************COMMON**************
    public static void onPageStart(String page) {
    }

    public static void onPageEnd(String page) {
    }

    public static void onPause(Context context) {
    }

    public static void onResume(Context context) {
    }
    //**************COMMON**************


    //**************DApp***************
    //DApp点击次数
    public static void dAppListClickCount(Context context, String url, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? url : (url + " coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "DApp", value);
    }

    public static void dAppListClickCount(Context context, String url, String currentAddress, String category) {
        String value = TextUtils.isEmpty(currentAddress) ? url : (url + " coinType:" + getCoinType(currentAddress));
        value = "category:" + category + " " + value;
        UmengAgentWrapper.onEvent(context, "DApp", value);
    }
    //**************DApp***************

    //**************Banner***************
    //Banner DApp点击次数
    public static void bannerClickCount(Context context, String url, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? url : (url + " coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "Banner", value);
    }
    //**************Banner***************


    //**************首页底部菜单***************
    public static void mainDAppClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "DApps" : ("DApps coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "Tab", value);
    }

    public static void mainWalletClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "Wallet" : ("Wallet coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "Tab", value);
    }

    public static void mainMarketClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "Market" : ("Market coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "Tab", value);
    }

    public static void mainWealthClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "Wealth" : ("Wealth coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "Tab", value);
    }

    public static void mainSettingClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "Settings" : ("Settings coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "Tab", value);
    }
    //**************首页底部菜单***************


    //**************设置页***************
    public static void settingTouchFingerprintClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_fingerprint" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingLanguageClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_languages" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingCoinClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_coin" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingRedeemCodeClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_redeemCode" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingClearWebViewClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_clear_dApp_cache" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingHelpClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_help" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingTermsClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_terms_of_use" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingPrivacyClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_privacy_policy" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingAboutClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_about" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingUpdateClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_update_version" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingSwitchNetworkClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_switch_network" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingAddressBookClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_address_book" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingNotifyClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_notify_manager" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }

    public static void settingUnlockClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "settings_unlock" + value;
        UmengAgentWrapper.onEvent(context, "Settings", value);
    }
    //**************设置页***************


    //**************切换语言***************
    public static void changeLanguageClickCount(Context context, String language, String currentAddress) {
        if (ConstantLanguages.SIMPLIFIED_CHINESE.equals(language)) {
            language = "zh-Hans";
        } else if (ConstantLanguages.KOREA.equals(language)) {
            language = "ko";
        } else if (ConstantLanguages.ENGLISH.equals(language)) {
            language = "en";
        } else {
            language = "system";
        }
        String coinType = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "Language", language + coinType);
    }
    //**************切换语言***************


    //**************钱包管理***************
    //**************钱包管理***************
    public static void editWalletIconClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "manage_change_icon" + value;
        UmengAgentWrapper.onEvent(context, "WalletManagement", value);
    }

    public static void editWalletNameClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "manage_change_name" + value;
        UmengAgentWrapper.onEvent(context, "WalletManagement", value);
    }

    public static void editWalletExportPhraseClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "manage_export_phrase" + value;
        UmengAgentWrapper.onEvent(context, "WalletManagement", value);
    }

    public static void editWalletExportPrivateClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "manage_export_private_key" + value;
        UmengAgentWrapper.onEvent(context, "WalletManagement", value);
    }

    public static void editWalletExportKeystoreClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "manage_export_keystore" + value;
        UmengAgentWrapper.onEvent(context, "WalletManagement", value);
    }

    public static void editWalletDeleteClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "manage_export_delete_wallet" + value;
        UmengAgentWrapper.onEvent(context, "WalletManagement", value);
    }
    //**************钱包管理***************


    //**************public sale***************
    //点击公募token
    public static void publicSaleTokenClickCount(Context context, String tokenSymbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? tokenSymbol : (tokenSymbol + " coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "PublicSale", value);
    }
    //**************public sale***************


    //**************顶部bar按钮点击***************
    //**************顶部bar按钮点击***************
    public static void topBarDAppHomeClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "dAppBrowser_homeAction" + value;
        UmengAgentWrapper.onEvent(context, "Navigation", value);
    }

    public static void topBarDAppMenuClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "dAppBrowser_moreAction" + value;
        UmengAgentWrapper.onEvent(context, "Navigation", value);
    }

    public static void topBarDAppRefreshClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "dAppBrowser_reloadAction" + value;
        UmengAgentWrapper.onEvent(context, "Navigation", value);
    }

    public static void topBarLanguageSaveClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "language_saveAction" + value;
        UmengAgentWrapper.onEvent(context, "Navigation", value);
    }

    public static void topBarScanQRCodeClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "scanQRCode" + value;
        UmengAgentWrapper.onEvent(context, "Navigation", value);
    }

    public static void topBarShowQRCodeClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "showQRCode" + value;
        UmengAgentWrapper.onEvent(context, "Navigation", value);
    }

    public static void topBarCreateWalletSkipClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "createWallet_skipAction" + value;
        UmengAgentWrapper.onEvent(context, "Navigation", value);
    }

    public static void topBarTokenListAddTokenClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "tokenSearch_toAddTokenAction" + value;
        UmengAgentWrapper.onEvent(context, "Navigation", value);
    }

    public static void topBarAddTokenHelpClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "addToken_helpAction" + value;
        UmengAgentWrapper.onEvent(context, "Navigation", value);
    }

    public static void topBarAddWalletClickCount(Context context) {
        UmengAgentWrapper.onEvent(context, "Navigation", "walletList_addWalletAction");
    }
    //**************顶部bar按钮点击***************


    //**************添加Token***************
    public static void addTokenClickCount(Context context, String tokenSymbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "address_addTokenAction  symbol:" + tokenSymbol + value;
        UmengAgentWrapper.onEvent(context, "AddToken", value);
    }

    public static void openTokenClickCount(Context context, String tokenSymbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "search_addTokenAction  symbol:" + tokenSymbol + value;
        UmengAgentWrapper.onEvent(context, "AddToken", value);
    }
    //**************添加Token***************


    //**************钱包转账收款***************
    //详情
    public static void walletHomeDetailClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "main_detailAction" + value;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }

    //转账
    public static void walletHomeSendTokenClickCount(Context context, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "main_sendAction" + value;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }

    //详情页收款
    public static void walletDetailReceiveTokenClickCount(Context context, String tokenSymbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        String token = " token:" + tokenSymbol;
        value = "detail_receiveAction" + token + value;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }

    //详情页转账
    public static void walletDetailSendTokenClickCount(Context context, String tokenSymbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        String token = " token:" + tokenSymbol;
        value = "detail_sendAction" + token + value;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }

    //生成交易
    public static void walletCreateTokenTransactionClickCount(Context context, String tokenSymbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "generateTransactionAction tokenSymbol:" + tokenSymbol + value;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }

    //public Sale详情页buy按钮
    public static void walletPublicSaleBuyClickCount(Context context, String tokenSymbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        String token = " token:" + tokenSymbol;
        value = "publicSale_buyAction" + token + value;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }

    public static void walletPublicSaleCreateTransactionClickCount(Context context, String tokenSymbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        String token = " token:" + tokenSymbol;
        value = "publicSale_generateTransactionAction" + token + value;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }


    //发送Token交易hash
    public static void walletSendTokenTransactionClickCount(Context context, String tokenSymbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        value = "sendTransactionAction" + value;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }

    //购买页面发送交易hash
    public static void walletPublicSaleSendTransactionClickCount(Context context, String tokenSymbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        String token = " token:" + tokenSymbol;
        value = "publicSale_sendTransactionAction" + token + value;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }

    //DApp交易hash
    public static void walletDAppSendTransactionClickCount(Context context, String symbol, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? "" : (" coinType:" + getCoinType(currentAddress));
        String token = " token:" + symbol;
        value = "dApp_sendTransactionAction" + token + value;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }
    //**************钱包转账收款***************

    //**************trx 冻结解冻***************
    public static void walletTrxFreezeClickCount(Context context) {
        String value = "freezeTransactionAction coinType:" + Constant.HD_PATH_CODE_TRX;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }

    public static void walletTrxUnfreezeClickCount(Context context) {
        String value = "unfreezeTransactionAction coinType:" + Constant.HD_PATH_CODE_TRX;
        UmengAgentWrapper.onEvent(context, "Wallet", value);
    }


    //**************转账成功统计**************
    private static String getTag(int accountType, String symbol, String symbolAddress) {
        if (accountType == Constant.ACCOUNT_TYPE_ETH) {
            if (!TextUtils.isEmpty(symbol) && !QWTokenDao.ETH_SYMBOL.equals(symbol)) {
                return "erc20";
            } else {
                return "eth";
            }
        } else if (accountType == Constant.ACCOUNT_TYPE_QKC) {
            return "qkc";
        } else if (accountType == Constant.ACCOUNT_TYPE_TRX) {
            if (!TextUtils.isEmpty(symbolAddress) && !TextUtils.isEmpty(symbol) && !QWTokenDao.TRX_SYMBOL.equals(symbol)) {
                if (TronWalletClient.isTronAddressValid(symbolAddress)) {
                    return "trc20";
                } else {
                    return "trc10";
                }
            } else {
                return "trx";
            }
        }
        return "";
    }

    //转账成功统计
    public static void sendTranSuccessCount(Context context, int type, String symbol, String symbolAddress, int count) {
        String key = getTag(type, symbol, symbolAddress);
        if (TextUtils.isEmpty(key)) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        map.put(key, symbol);
        UmengAgentWrapper.onEventValue(context, "SendTransaction", map, count);
    }

    //DApp转账成功统计
    public static void dAppSendTranSuccessCount(Context context, int type, String symbol, String symbolAddress, int count) {
        String key = getTag(type, symbol, symbolAddress);
        if (TextUtils.isEmpty(key)) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        map.put(key, symbol);
        UmengAgentWrapper.onEventValue(context, "DAppSendTransaction", map, count);
    }
    //**************转账成功统计**************


    //****************bounty统计*********************
    public static void bountyLogin(Context context) {
        UmengAgentWrapper.onEvent(context, "Bounty", "Login");
    }

    public static void bountyLogout(Context context) {
        UmengAgentWrapper.onEvent(context, "Bounty", "Logout");
    }

    public static void bountySign(Context context) {
        UmengAgentWrapper.onEvent(context, "Bounty", "Register");
    }

    public static void bountyDownloadRedeem(Context context) {
        UmengAgentWrapper.onEvent(context, "Bounty", "Download Redeem");
    }

    public static void bountyTQKCRedeem(Context context) {
        UmengAgentWrapper.onEvent(context, "Bounty", "tQKC Redeem");
    }

    public static void bountyInvite(Context context) {
        UmengAgentWrapper.onEvent(context, "Bounty", "Invite");
    }

    public static void bountyCheckIn(Context context) {
        UmengAgentWrapper.onEvent(context, "Bounty", "Check-in");
    }

    public static void bountyMoreBounty(Context context) {
        UmengAgentWrapper.onEvent(context, "Bounty", "More task");
    }
    //****************bounty统计*********************

    //****************白名单统计*********************
    public static void whiteListOpen(Context context, String url, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? url : (url + " coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "Whitelist", value);
    }

    public static void whiteListTransaction(Context context, String url, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? url : (url + " coinType:" + getCoinType(currentAddress));
        UmengAgentWrapper.onEvent(context, "WhitelistTransaction", value);
    }
    //****************白名单统计*********************

    //****************DApp列表*********************
    public static void favoriteDAppClick(Context context, String url, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? url : (url + " coinType:" + getCoinType(currentAddress));
        value = value + " favorite";
        UmengAgentWrapper.onEvent(context, "Favorite", value);
    }

    public static void removeFavoriteDAppClick(Context context, String url, String currentAddress) {
        String value = TextUtils.isEmpty(currentAddress) ? url : (url + " coinType:" + getCoinType(currentAddress));
        value = value + " removeFavorite";
        UmengAgentWrapper.onEvent(context, "Favorite", value);
    }

    public static void favoriteEditClick(Context context) {
        UmengAgentWrapper.onEvent(context, "Favorite", "edit");
    }

    public static void favoriteDAppTotal(Context context, String currentAddress, int count) {
        Map<String, String> map = new HashMap<>();
        if (TextUtils.isEmpty(currentAddress)) {
            map.put("coinType", "195");
        } else {
            map.put("address", currentAddress);
            String coinType = getCoinType(currentAddress);
            map.put("coinType", coinType);
        }
        UmengAgentWrapper.onEventValue(context, "FavoriteTotal", map, count);
    }
    //****************DApp列表*********************

    //****************QR二维码*********************
    public static void qrMainSendTokenClick(Context context) {
        UmengAgentWrapper.onEvent(context, "QR", "sendToken main");
    }

    public static void qrSendTokenClick(Context context, String symbol) {
        String value = "sendToken symbol:" + symbol;
        UmengAgentWrapper.onEvent(context, "QR", value);
    }

    public static void qrMainReceiveTokenClick(Context context) {
        UmengAgentWrapper.onEvent(context, "QR", "receiveToken main");
    }

    public static void qrReceiveTokenClick(Context context, String symbol) {
        String value = "receiveToken symbol:" + symbol;
        UmengAgentWrapper.onEvent(context, "QR", value);
    }
    //****************QR二维码*********************


    //****************QuarkChain*********************
    public static void mergeToken(Context context, String symbol) {
        String value = "merge symbol:" + symbol;
        UmengAgentWrapper.onEvent(context, "QuarkChain", value);
    }

    public static void mergeSubmitToken(Context context, String symbol) {
        String value = "mergeSubmitTransaction symbol:" + symbol;
        UmengAgentWrapper.onEvent(context, "QuarkChain", value);
    }

    public static void requestTokenClick(Context context, String symbol) {
        String value = "requestToken symbol:" + symbol;
        UmengAgentWrapper.onEvent(context, "QuarkChain", value);
    }
    //****************QuarkChain*********************

    //****************公募购买*********************
    public static void payTokenClick(Context context, String address, String symbol) {
        String value = "pay address:" + address + " symbol:" + symbol;
        UmengAgentWrapper.onEvent(context, "PublicSale", value);
    }

    public static void transferTokenClick(Context context, String address, String symbol) {
        String value = "transfer address:" + address + " symbol:" + symbol;
        UmengAgentWrapper.onEvent(context, "PublicSale", value);
    }
    //****************公募购买*********************

}
