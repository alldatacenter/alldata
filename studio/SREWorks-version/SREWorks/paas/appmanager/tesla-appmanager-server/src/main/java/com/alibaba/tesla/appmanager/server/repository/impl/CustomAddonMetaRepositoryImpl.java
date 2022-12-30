package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.CustomAddonMetaRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonMetaDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.CustomAddonMetaDOMapper;
import com.github.pagehelper.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: CustomAddonMetaRepositoryImpl
 * @Author: dyj
 * @DATE: 2020-11-25
 * @Description:
 **/
@Service
public class CustomAddonMetaRepositoryImpl implements CustomAddonMetaRepository {
    @Autowired
    private CustomAddonMetaDOMapper customAddonMetaDOMapper;

    @Override
    public long countByCondition(AddonMetaQueryCondition condition) {
        return customAddonMetaDOMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(AddonMetaQueryCondition condition) {
        return customAddonMetaDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return customAddonMetaDOMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(CustomAddonMetaDO record) {
        return customAddonMetaDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public CustomAddonMetaDO getByAddonId(String addonId, String addonVersion) {
        AddonMetaQueryCondition condition = AddonMetaQueryCondition.builder()
                .addonId(addonId)
                .addonVersion(addonVersion)
                .build();
        List<CustomAddonMetaDO> records = customAddonMetaDOMapper.selectByExampleWithBLOBs(buildExample(condition));
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }
        if (records.size() > 1) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("multiple addon found, invalid! addonId=%s, addonVersion=%s", addonId, addonVersion));
        }
        return records.get(0);
    }

    @Override
    public Page<CustomAddonMetaDO> selectByCondition(AddonMetaQueryCondition condition) {
        CustomAddonMetaDOExample example = buildExample(condition);
        condition.doPagination();
        return customAddonMetaDOMapper.selectByExampleWithBLOBs(example);
    }

    @Override
    public CustomAddonMetaDO selectByPrimaryKey(Long id) {
        return customAddonMetaDOMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByCondition(CustomAddonMetaDO record, AddonMetaQueryCondition condition) {
        return customAddonMetaDOMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(CustomAddonMetaDO record) {
        return customAddonMetaDOMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private CustomAddonMetaDOExample buildExample(AddonMetaQueryCondition condition) {
        CustomAddonMetaDOExample example = new CustomAddonMetaDOExample();
        CustomAddonMetaDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getAddonId())) {
            criteria.andAddonIdEqualTo(condition.getAddonId());
        }
        if (StringUtils.isNotBlank(condition.getAddonVersion())) {
            criteria.andAddonVersionEqualTo(condition.getAddonVersion());
        }
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(condition.getAddonTypeList())) {
            criteria.andAddonTypeIn(
                    condition.getAddonTypeList().stream().map(Enum::toString).collect(
                            Collectors.toList()));
        }
        return example;
    }

    private CustomAddonMetaDO insertDate(CustomAddonMetaDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private CustomAddonMetaDO updateDate(CustomAddonMetaDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
