package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.AppAddonDTO;
import com.alibaba.tesla.appmanager.domain.req.AppAddonCreateReq;
import com.alibaba.tesla.appmanager.domain.req.AppAddonQueryReq;
import com.alibaba.tesla.appmanager.domain.req.AppAddonUpdateReq;

/**
 * 应用 Addon 信息接口
 *
 * @author qianmo.zm@alibaba-inc.com
 */
public interface AppAddonProvider {

    /**
     * 分页查询应用 Addon 信息
     */
    Pagination<AppAddonDTO> list(AppAddonQueryReq request);

    /**
     * 通过 ID 查询 Addon 信息
     */
    AppAddonDTO get(Long id);

    /**
     * 通过 name 查询 Addon 信息
     */
    AppAddonDTO get(String appId, String namespaceId, String stageId, String name);

    /**
     * 通过 ID 删除 Addon 信息
     */
    boolean delete(String appId, String namespaceId, String stageId, String name);

    /**
     * 更新应用 Addon 绑定
     */
    boolean update(AppAddonUpdateReq request);

    /**
     * 新建应用 Addon 绑定
     */
    boolean create(AppAddonCreateReq request);
}
