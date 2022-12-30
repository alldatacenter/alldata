package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.AddonMetaDTO;
import com.alibaba.tesla.appmanager.domain.req.AddonMetaQueryReq;
import com.alibaba.tesla.appmanager.domain.req.appaddon.AppAddonSyncReq;

/**
 * Addon 元信息 Provider
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
public interface AddonMetaProvider {

    Pagination<AddonMetaDTO> list(AddonMetaQueryReq request);

    AddonMetaDTO get(Long id);

    AddonMetaDTO get(String addonId, String addonVersion);

    AddonMetaDTO create(AddonMetaDTO record);

    void update(AddonMetaDTO record);

    void delete(Long id);

    /**
     * 同步当前所有 app addon 绑定关系到 deploy config 中
     */
    void sync(AppAddonSyncReq request);
}
