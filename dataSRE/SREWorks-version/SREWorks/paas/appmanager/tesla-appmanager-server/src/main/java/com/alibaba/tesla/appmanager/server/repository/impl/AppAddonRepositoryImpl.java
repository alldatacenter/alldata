package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.AppAddonRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppAddonQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.AppAddonDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AppAddonRepositoryImpl implements AppAddonRepository {

    @Autowired
    private AppAddonDOMapper appAddonMapper;

    @Override
    public long countByCondition(AppAddonQueryCondition condition) {
        return appAddonMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(AppAddonQueryCondition condition) {
        return appAddonMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(AppAddonDO record) {
        return appAddonMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<AppAddonDO> selectByCondition(AppAddonQueryCondition condition) {
        AppAddonDOExample example = buildExample(condition);
        condition.doPagination();
        return appAddonMapper.selectByExample(example);
    }

    @Override
    public int updateByCondition(AppAddonDO record, AppAddonQueryCondition condition) {
        return appAddonMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private AppAddonDOExample buildExample(AppAddonQueryCondition condition) {
        AppAddonDOExample example = new AppAddonDOExample();
        AppAddonDOExample.Criteria criteria = example.createCriteria();
        if (Objects.nonNull(condition.getId())) {
            criteria.andIdEqualTo(condition.getId());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (CollectionUtils.isNotEmpty(condition.getAddonTypeList())) {
            criteria.andAddonTypeIn(
                    condition.getAddonTypeList().stream()
                            .map(Enum::toString)
                            .collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(condition.getAddonId())) {
            criteria.andAddonIdEqualTo(condition.getAddonId());
        }
        if (StringUtils.isNotEmpty(condition.getAddonName())) {
            criteria.andNameEqualTo(condition.getAddonName());
        }
        if (condition.getNamespaceId() != null) {
            criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
        }
        if (condition.getStageId() != null) {
            criteria.andStageIdEqualTo(condition.getStageId());
        }
        if (StringUtils.isNotEmpty(condition.getNamespaceIdNotEqualTo())) {
            criteria.andNamespaceIdNotEqualTo(condition.getNamespaceIdNotEqualTo());
        }
        if (StringUtils.isNotEmpty(condition.getStageIdNotEqualTo())) {
            criteria.andStageIdNotEqualTo(condition.getStageIdNotEqualTo());
        }
        return example;
    }

    private AppAddonDO insertDate(AppAddonDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private AppAddonDO updateDate(AppAddonDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
