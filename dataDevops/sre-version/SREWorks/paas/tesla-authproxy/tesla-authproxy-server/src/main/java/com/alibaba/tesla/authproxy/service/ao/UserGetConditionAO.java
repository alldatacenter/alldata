package com.alibaba.tesla.authproxy.service.ao;

import lombok.Builder;
import lombok.Data;

/**
 * 单个用户查询条件对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
public class UserGetConditionAO {

    /**
     * 必选。语言
     */
    private String locale;

    /**
     * 必选。租户 ID
     */
    private String tenantId;

    /**
     * 必选。应用 ID，用于用户相关信息的应用下过滤（比如用户在该 appId 下包含的 roles 信息）
     */
    private String appId;

    /**
     * 可选。用户唯一定位 ID，userId/empId 二选一
     */
    private String userId;

    /**
     * 可选。用户工号，userId/empId 二选一
     */
    private String empId;

    /**
     * 可选。从哪个 EmpId 切换身份过来
     */
    private String fromEmpId;

    /**
     * 可选。从哪个 AuthUser 切换身份过来
     */
    private String fromAuthUser;

    /**
     * 可选。从哪个 BucId 切换身份过来
     */
    private String fromBucId;
}
