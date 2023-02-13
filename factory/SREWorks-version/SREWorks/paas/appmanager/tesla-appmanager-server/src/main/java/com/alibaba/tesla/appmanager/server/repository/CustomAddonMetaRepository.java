package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.AddonMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonMetaDO;
import com.github.pagehelper.Page;

/**
 * @author yangjie.dyj@alibaba-inc.com
 * @InterfaceName: CustomAddonMetaRepository
 * @DATE: 2020-11-25
 * @Description:
 **/
public interface CustomAddonMetaRepository {
    /**
     * 根据条件计数
     *
     * @param condition
     * @return
     */
    long countByCondition(AddonMetaQueryCondition condition);

    /**
     * 根据条件删除
     *
     * @param condition
     * @return
     */
    int deleteByCondition(AddonMetaQueryCondition condition);

    /**
     * 根据主键删除
     *
     * @param id
     * @return
     */
    int deleteByPrimaryKey(Long id);

    /**
     * 插入
     *
     * @param record
     * @return
     */
    int insert(CustomAddonMetaDO record);

    /**
     * 根据 addonId 查询
     *
     * @param addonId
     * @param addonVersion
     * @return
     */
    CustomAddonMetaDO getByAddonId(String addonId, String addonVersion);

    /**
     * 根据条件查询
     *
     * @param condition
     * @return
     */
    Page<CustomAddonMetaDO> selectByCondition(AddonMetaQueryCondition condition);

    /**
     * 根据主键查询
     *
     * @param id
     * @return
     */
    CustomAddonMetaDO selectByPrimaryKey(Long id);

    /**
     * 根据条件查询
     *
     * @param record
     * @param condition
     * @return
     */
    int updateByCondition(CustomAddonMetaDO record, AddonMetaQueryCondition condition);

    /**
     * 根据主键更新
     *
     * @param record
     * @return
     */
    int updateByPrimaryKey(CustomAddonMetaDO record);
}
