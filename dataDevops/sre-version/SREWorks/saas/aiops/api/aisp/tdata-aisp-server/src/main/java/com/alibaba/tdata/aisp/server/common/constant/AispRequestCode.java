package com.alibaba.tdata.aisp.server.common.constant;

/**
 * @ClassName: AispRequestCodeEnum
 * @Author: dyj
 * @DATE: 2021-11-25
 * @Description:
 **/
public class AispRequestCode {

    /**
     * 成功
     */
    public static final int SUCCESS = 200;

    /**
     * Client error
     */
    public static final int CLIENT_ERROR = 400;

    /**
     * 用户参数填写错误
     */
    public static final int USER_ARG_ERROR = 400;

    /**
     * 用户无权限
     */
    public static final int NO_PERM = 401;

    /**
     * 非法操作
     */
    public static final int FORBIDDEN = 403;

    /**
     * 无效的资源
     */
    public static final int NOT_FOUND = 404;

    /**
     * 表单验证错误
     */
    public static final int VALIDATION_ERROR = 422;

    /**
     * Server generic error
     */
    public static final int SERVER_ERROR = 500;

    /**
     * 后台功能尚未实现
     */
    public static final int NOT_IMPLEMENTED = 501;

    /**
     * 后台服务返回数据异常
     */
    public static final int BAD_GATEWAY = 502;

    /**
     * 服务不可用
     */
    public static final int UNAVALIABLE = 503;

    /**
     * 后台功能尚未实现
     */
    public static final int TIMEOUT = 504;

    /**
     * Internal componet error
     *
     * 通道服务错误
     */
    public static final int CHANNEL_SERVICE_ERROR = 601;

    /**
     * 通知服务错误
     */
    public static final int NOTIFY_SERVICE_ERROR = 602;

    /**
     * CMDB、名字服务
     */
    public static final int CMDB_SERVICE_ERROR = 603;

    /**
     * External sys error
     *
     * 数据库错误
     */
    public static final int DB_ERROR = 701;

    /**
     * StarAgent 错误
     */
    public static final int STARAGENT_ERROR = 702;

    /**
     * Armory 错误
     */
    public static final int ARMORY_ERROR = 703;

    /**
     * Tianji 错误
     */
    public static final int TIANJI_ERROR = 704;

    /**
     * ChangeFree 错误
     */
    public static final int CHANGEFREE_ERROR = 705;

    /**
     * Buc 错误
     */
    public static final int BUC_ERROR = 706;

    /**
     * Acl 错误
     */
    public static final int ACL_ERROR = 707;

    /**
     * 业务异常 (兼容分歧)
     */
    public static final int BIZ_ERROR = 10000;
}
