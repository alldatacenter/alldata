package com.alibaba.tesla.authproxy.constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Tesla返回体结构中的code错误码字段定义
 *
 * 分短错误码和长错误码两个情况 1. 短错误码如下列类所定义, 三位整型 2. 长错误码为9位整形，前三位(Server generic error code) 中间三位(Internal component code)
 * 末尾三位(External sys error) 如: 503601701 表示 通道服务因为数据库错误暂时不可用
 *
 * TODO: 使用Enum，方便错误码和对应描述文本的映射
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class ErrorCode {

    /**
     * 空数据列表
     */
    public static final List<String> EMPTY_DATA = new ArrayList<>();

    /**
     * 空 Object
     */
    public static final Map<String, String> EMPTY_OBJ = new HashMap<>();

    /**
     * 成功
     */
    public static final int OK = 200;

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

    /**
     * 构造长错误码
     */
    public static int buildLongErrorCode(int genericErrorCode, int componentCode, int externalSysErrorCode) {
        String errCode = String.format("%3d%3d%3d", genericErrorCode, componentCode, externalSysErrorCode);
        return Integer.parseInt(errCode);
    }

    /**
     * 根据错误码自动解析得到错误原因
     */
    public static String getErrorMessage(int errCode) {
        String errMessage = "";
        String errCodeStr = String.format("%s", errCode);
        // 短错误码
        if (errCodeStr.length() == 3) {
            // TODO: 使用Enum，方便code和描述信息的对应关系
            errMessage = "";
        }
        // 长错误码
        if (errCodeStr.length() == 9) {
            // TODO: 拆分长错误为三部分
        } else {

        }
        return errMessage;
    }
}