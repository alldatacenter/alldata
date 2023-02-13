package com.alibaba.tesla.appmanager.meta.helm.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.meta.helm.repository.HelmMetaRepository;
import com.alibaba.tesla.appmanager.meta.helm.repository.condition.HelmMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDO;
import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDOExample;
import com.alibaba.tesla.appmanager.meta.helm.repository.mapper.HelmMetaDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class HelmMetaRepositoryImpl implements HelmMetaRepository {

    @Autowired
    private HelmMetaDOMapper mapper;

    @Override
    public long countByCondition(HelmMetaQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(HelmMetaQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return mapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(HelmMetaDO record) {
        return mapper.insert(insertDate(record));
    }

    @Override
    public List<HelmMetaDO> selectByCondition(HelmMetaQueryCondition condition) {
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public HelmMetaDO selectByPrimaryKey(Long id) {
        return mapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByCondition(HelmMetaDO record, HelmMetaQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(HelmMetaDO record) {
        return mapper.updateByPrimaryKeySelective(record);
    }

    private HelmMetaDOExample buildExample(HelmMetaQueryCondition condition) {
        HelmMetaDOExample example = new HelmMetaDOExample();
        HelmMetaDOExample.Criteria criteria = example.createCriteria();
        if (Objects.nonNull(condition.getId())) {
            criteria.andIdEqualTo(condition.getId());
        }
        if (StringUtils.isNotBlank(condition.getName())) {
            criteria.andNameEqualTo(condition.getName());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (StringUtils.isNotBlank(condition.getHelmPackageId())){
            criteria.andHelmPackageIdEqualTo(condition.getHelmPackageId());
        }
        if (StringUtils.isNotEmpty(condition.getNamespaceIdNotEqualTo())) {
            criteria.andNamespaceIdNotEqualTo(condition.getNamespaceIdNotEqualTo());
        }
        if (StringUtils.isNotEmpty(condition.getStageIdNotEqualTo())) {
            criteria.andStageIdNotEqualTo(condition.getStageIdNotEqualTo());
        }
        return example;
    }

    private HelmMetaDO insertDate(HelmMetaDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private HelmMetaDO updateDate(HelmMetaDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
