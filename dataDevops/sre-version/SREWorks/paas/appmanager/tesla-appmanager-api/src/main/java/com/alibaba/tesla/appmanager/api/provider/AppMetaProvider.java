package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.AppMetaDTO;
import com.alibaba.tesla.appmanager.domain.req.AppMetaDeleteReq;
import com.alibaba.tesla.appmanager.domain.req.AppMetaQueryReq;
import com.alibaba.tesla.appmanager.domain.req.AppMetaUpdateReq;

/**
 * 应用元信息接口
 *
 * @author qianmo.zm@alibaba-inc.com
 */
public interface AppMetaProvider {

    /**
     * 分页查询应用元信息
     */
    Pagination<AppMetaDTO> list(AppMetaQueryReq request, String operator, boolean ignorePermission);

    /**
     * 通过应用 ID 查询应用元信息
     */
    AppMetaDTO get(String appId, String operator);

    /**
     * 保存应用元信息
     */
    AppMetaDTO save(AppMetaUpdateReq request, String operator);

    /**
     * 查询指定应用的前端版本
     *
     * @param appId    应用 ID
     * @param operator Operator
     * @return version
     */
    String getFrontendVersion(String appId, String operator);

    /**
     * 删除应用元信息
     */
    boolean delete(AppMetaDeleteReq request, String operator);
}
