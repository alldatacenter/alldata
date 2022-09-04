package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDO;

import java.util.List;

public interface AddonMetaRepository {
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
    int insert(AddonMetaDO record);

    /**
     * 根据 addonType / addonId 查询
     *
     * @param addonType Addon Type
     * @param addonId   Addon ID
     * @return
     */
    AddonMetaDO get(ComponentTypeEnum addonType, String addonId);

    /**
     * 根据条件查询
     *
     * @param condition
     * @return
     */
    List<AddonMetaDO> selectByCondition(AddonMetaQueryCondition condition);

    /**
     * 根据主键查询
     *
     * @param id
     * @return
     */
    AddonMetaDO selectByPrimaryKey(Long id);

    /**
     * 更新
     *
     * @param record
     * @param condition
     * @return
     */
    int updateByCondition(AddonMetaDO record, AddonMetaQueryCondition condition);

    /**
     * 根据主键更新
     *
     * @param record
     * @return
     */
    int updateByPrimaryKey(AddonMetaDO record);
}