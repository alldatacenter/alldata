package com.alibaba.tesla.appmanager.server.service.addon;

import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDO;

/**
 * Addon 元信息服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface AddonMetaService {

    /**
     * 根据条件过滤 Addon 列表
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<AddonMetaDO> list(AddonMetaQueryCondition condition);

    /**
     * 根据主键 ID 获取 Addon
     *
     * @param id Addon 主键 ID
     * @return AddonMetaDO
     */
    AddonMetaDO get(Long id);

    /**
     * 根据 addonType / addonId 获取 AddonMeta
     *
     * @param addonType Addon Type
     * @param addonId Addon ID
     * @return AddonMetaDO
     */
    AddonMetaDO get(ComponentTypeEnum addonType, String addonId);

    /**
     * 更新指定的 Addon 元信息
     *
     * @param record Addon Meta 记录
     */
    int update(AddonMetaDO record);

    /**
     * 创建指定的 Addon 元信息
     *
     * @param record Addon Meta 记录
     */
    int create(AddonMetaDO record);

    /**
     * 根据主键 ID 删除 Addon
     *
     * @param id Addon 主键 ID
     */
    int delete(Long id);
}
