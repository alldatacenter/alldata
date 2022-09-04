package com.alibaba.tesla.appmanager.server.service.addon.impl;

import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.AddonMetaRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDO;
import com.alibaba.tesla.appmanager.server.service.addon.AddonMetaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class AddonMetaServiceImpl implements AddonMetaService {

    @Autowired
    private AddonMetaRepository addonMetaRepository;

    /**
     * 根据条件过滤 Addon 列表
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public Pagination<AddonMetaDO> list(AddonMetaQueryCondition condition) {
        List<AddonMetaDO> page = addonMetaRepository.selectByCondition(condition);
        return Pagination.valueOf(page, Function.identity());
    }

    /**
     * 根据主键 ID 获取 Addon
     *
     * @param id Addon 主键 ID
     * @return AddonMetaDO
     */
    @Override
    public AddonMetaDO get(Long id) {
        return addonMetaRepository.selectByPrimaryKey(id);
    }

    /**
     * 根据 addonType / addonId 获取 AddonMeta
     *
     * @param addonType Addon Type
     * @param addonId Addon ID
     * @return AddonMetaDO
     */
    @Override
    public AddonMetaDO get(ComponentTypeEnum addonType, String addonId) {
        return addonMetaRepository.get(addonType, addonId);
    }

    /**
     * 更新指定的 Addon 元信息
     *
     * @param record Addon Meta 记录
     */
    @Override
    public int update(AddonMetaDO record) {
        return addonMetaRepository.updateByPrimaryKey(record);
    }

    /**
     * 更新指定的 Addon 元信息
     *
     * @param record Addon Meta 记录
     */
    @Override
    public int create(AddonMetaDO record) {
        return addonMetaRepository.insert(record);
    }

    /**
     * 根据主键 ID 删除 Addon
     *
     * @param id Addon 主键 ID
     */
    @Override
    public int delete(Long id) {
        return addonMetaRepository.deleteByPrimaryKey(id);
    }
}
