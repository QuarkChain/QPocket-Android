package com.quarkonium.qpocket.api;

public final class Debug {
    public static final Boolean DEBUG = true;
    public static boolean PRE_RELEASE = false;

    //比特币测试环境
    public static final Boolean BIT_COIN_TEST = false;

    /**
     * 是否使用友盟的debug key
     */
    public static final boolean UMENG_DEBUG_KEY = false;

    /**
     * \
     * 性能测试窗口
     */
    public static final boolean PERFORMANCE_DEBUG = false;
    public static final int PERFORMANCE_REFRESH_TIME = 5 * 1000;

    /**
     * 日志单独提出
     */
    public static final boolean DEBUG_LOG = false;


    /**
     * 是否需要添加应用推荐项
     * 打包时注意
     */
    public static final boolean mIsNeedAddRecommdationItem = true;

    public static final int ENGIN_VERSION = 35;
    public static final String ENGIN_CRC32 = "480b1e1c";

    /**
     * MAA移动加速
     */
    public static final boolean MAA = false;

}
