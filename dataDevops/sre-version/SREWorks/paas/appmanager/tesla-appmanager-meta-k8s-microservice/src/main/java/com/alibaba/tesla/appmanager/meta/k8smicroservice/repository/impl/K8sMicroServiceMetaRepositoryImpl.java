package com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.K8sMicroServiceMetaRepository;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.condition.K8sMicroserviceMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDOExample;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.mapper.K8sMicroServiceMetaDOMapper;
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
public class K8sMicroServiceMetaRepositoryImpl implements K8sMicroServiceMetaRepository {

    @Autowired
    private K8sMicroServiceMetaDOMapper k8sMicroServiceMetaMapper;

    @Override
    public long countByCondition(K8sMicroserviceMetaQueryCondition condition) {
        return k8sMicroServiceMetaMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(K8sMicroserviceMetaQueryCondition condition) {
        return k8sMicroServiceMetaMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return k8sMicroServiceMetaMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(K8sMicroServiceMetaDO record) {
        return k8sMicroServiceMetaMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<K8sMicroServiceMetaDO> selectByCondition(K8sMicroserviceMetaQueryCondition condition) {
        K8sMicroServiceMetaDOExample example = buildExample(condition);
        condition.doPagination();
        if (condition.isWithBlobs()) {
            return k8sMicroServiceMetaMapper.selectByExampleWithBLOBs(example);
        } else {
            return k8sMicroServiceMetaMapper.selectByExample(example);
        }
    }

    @Override
    public K8sMicroServiceMetaDO selectByPrimaryKey(Long id) {
        return k8sMicroServiceMetaMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByCondition(K8sMicroServiceMetaDO record, K8sMicroserviceMetaQueryCondition condition) {
        return k8sMicroServiceMetaMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private K8sMicroServiceMetaDOExample buildExample(K8sMicroserviceMetaQueryCondition condition) {
        K8sMicroServiceMetaDOExample example = new K8sMicroServiceMetaDOExample();
        K8sMicroServiceMetaDOExample.Criteria criteria = example.createCriteria();
        if (Objects.nonNull(condition.getId())) {
            criteria.andIdEqualTo(condition.getId());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (condition.getNamespaceId() != null) {
            criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
        }
        if (condition.getStageId() != null) {
            criteria.andStageIdEqualTo(condition.getStageId());
        }
        if (StringUtils.isNotBlank(condition.getMicroServiceId())) {
            criteria.andMicroServiceIdEqualTo(condition.getMicroServiceId());
        }
        if (CollectionUtils.isNotEmpty(condition.getComponentTypeList())) {
            criteria.andComponentTypeIn(
                condition.getComponentTypeList().stream().map(Enum::toString).collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(condition.getNamespaceIdNotEqualTo())) {
            criteria.andNamespaceIdNotEqualTo(condition.getNamespaceIdNotEqualTo());
        }
        if (StringUtils.isNotEmpty(condition.getStageIdNotEqualTo())) {
            criteria.andStageIdNotEqualTo(condition.getStageIdNotEqualTo());
        }
        if (StringUtils.isNotEmpty(condition.getArch())) {
            criteria.andArchEqualTo(condition.getArch());
        }
        return example;
    }

    private K8sMicroServiceMetaDO insertDate(K8sMicroServiceMetaDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private K8sMicroServiceMetaDO updateDate(K8sMicroServiceMetaDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
