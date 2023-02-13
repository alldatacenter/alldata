package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.AddonMetaRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.AddonMetaMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AddonMetaRepositoryImpl implements AddonMetaRepository {

    @Autowired
    private AddonMetaMapper addonMetaMapper;

    @Override
    public long countByCondition(AddonMetaQueryCondition condition) {
        return addonMetaMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(AddonMetaQueryCondition condition) {
        return addonMetaMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return addonMetaMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(AddonMetaDO record) {
        return addonMetaMapper.insertSelective(insertDate(record));
    }

    @Override
    public AddonMetaDO get(ComponentTypeEnum addonType, String addonId) {
        AddonMetaQueryCondition condition = AddonMetaQueryCondition.builder()
                .addonTypeList(Collections.singletonList(addonType))
                .addonId(addonId)
                .build();
        List<AddonMetaDO> records = addonMetaMapper.selectByExample(buildExample(condition));
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }
        if (records.size() > 1) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("multiple addon found, invalid! addonId=%s", addonId));
        }
        return records.get(0);
    }

    @Override
    public List<AddonMetaDO> selectByCondition(AddonMetaQueryCondition condition) {
        AddonMetaDOExample example = buildExample(condition);
        condition.doPagination();
        return addonMetaMapper.selectByExample(example);
    }

    @Override
    public AddonMetaDO selectByPrimaryKey(Long id) {
        return addonMetaMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByCondition(AddonMetaDO record, AddonMetaQueryCondition condition) {
        return addonMetaMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(AddonMetaDO record) {
        return addonMetaMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private AddonMetaDOExample buildExample(AddonMetaQueryCondition condition) {
        AddonMetaDOExample example = new AddonMetaDOExample();
        AddonMetaDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getAddonId())) {
            criteria.andAddonIdEqualTo(condition.getAddonId());
        }
        if (StringUtils.isNotBlank(condition.getAddonVersion())) {
            criteria.andAddonVersionEqualTo(condition.getAddonVersion());
        }
        if (CollectionUtils.isNotEmpty(condition.getAddonTypeList())) {
            criteria.andAddonTypeIn(
                    condition.getAddonTypeList().stream().map(Enum::toString).collect(Collectors.toList()));
        }
        return example;
    }

    private AddonMetaDO insertDate(AddonMetaDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private AddonMetaDO updateDate(AddonMetaDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
