package com.alibaba.tesla.appmanager.deployconfig.repository.impl;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.deployconfig.repository.DeployConfigHistoryRepository;
import com.alibaba.tesla.appmanager.deployconfig.repository.condition.DeployConfigHistoryQueryCondition;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigHistoryDO;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigHistoryDOExample;
import com.alibaba.tesla.appmanager.deployconfig.repository.mapper.DeployConfigHistoryDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class DeployConfigHistoryRepositoryImpl implements DeployConfigHistoryRepository {

    private final DeployConfigHistoryDOMapper mapper;

    public DeployConfigHistoryRepositoryImpl(DeployConfigHistoryDOMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long countByExample(DeployConfigHistoryQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByExample(DeployConfigHistoryQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insertSelective(DeployConfigHistoryDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<DeployConfigHistoryDO> selectByExample(DeployConfigHistoryQueryCondition condition) {
        condition.doPagination();
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByExampleSelective(DeployConfigHistoryDO record, DeployConfigHistoryQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private DeployConfigHistoryDOExample buildExample(DeployConfigHistoryQueryCondition condition) {
        DeployConfigHistoryDOExample example = new DeployConfigHistoryDOExample();
        DeployConfigHistoryDOExample.Criteria criteria = example.createCriteria();
        // 允许 appId/envId 为空
        if (condition.getAppId() != null) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (StringUtils.isNotBlank(condition.getTypeId())) {
            criteria.andTypeIdEqualTo(condition.getTypeId());
        }
        if (condition.getEnvId() != null) {
            criteria.andEnvIdEqualTo(condition.getEnvId());
        }
        if (StringUtils.isNotBlank(condition.getApiVersion())) {
            criteria.andApiVersionEqualTo(condition.getApiVersion());
        }
        if (condition.getRevision() != null && condition.getRevision() > 0) {
            criteria.andRevisionEqualTo(condition.getRevision());
        }
        if (condition.getInherit() != null) {
            criteria.andInheritEqualTo(condition.getInherit());
        }
        if (condition.getIsolateNamespaceId() != null) {
            criteria.andNamespaceIdEqualTo(condition.getIsolateNamespaceId());
        }
        if (condition.getIsolateStageId() != null) {
            criteria.andStageIdEqualTo(condition.getIsolateStageId());
        }
        return example;
    }

    private DeployConfigHistoryDO insertDate(DeployConfigHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private DeployConfigHistoryDO updateDate(DeployConfigHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
