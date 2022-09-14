package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.DeployComponentRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployComponentQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.DeployComponentDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeployComponentRepositoryImpl implements DeployComponentRepository {

    @Autowired
    private DeployComponentDOMapper deployComponentMapper;

    @Override
    public long countByCondition(DeployComponentQueryCondition condition) {
        return deployComponentMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(DeployComponentQueryCondition condition) {
        return deployComponentMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return deployComponentMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(DeployComponentDO record) {
        return deployComponentMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<DeployComponentDO> selectByCondition(DeployComponentQueryCondition condition) {
        condition.doPagination();
        return deployComponentMapper.selectByExample(buildExample(condition));
    }

    @Override
    public DeployComponentDO selectByPrimaryKey(Long id) {
        return deployComponentMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByConditionSelective(DeployComponentDO record, DeployComponentQueryCondition condition) {
        return deployComponentMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(DeployComponentDO record) {
        return deployComponentMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private DeployComponentDOExample buildExample(DeployComponentQueryCondition condition) {
        DeployComponentDOExample example = new DeployComponentDOExample();
        DeployComponentDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getClusterId())) {
            criteria.andClusterIdEqualTo(condition.getClusterId());
        }
        if (StringUtils.isNotBlank(condition.getNamespaceId())) {
            criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
        }
        if (StringUtils.isNotBlank(condition.getStageId())) {
            criteria.andStageIdEqualTo(condition.getStageId());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (condition.getDeployAppId() != null && condition.getDeployAppId() > 0) {
            criteria.andDeployIdEqualTo(condition.getDeployAppId());
        }
        if (StringUtils.isNotBlank(condition.getIdentifier())) {
            criteria.andIdentifierEqualTo(condition.getIdentifier());
        } else if (StringUtils.isNotBlank(condition.getIdentifierStartsWith())) {
            criteria.andIdentifierLike(String.format("%s%%", condition.getIdentifierStartsWith().trim()));
        }
        if (condition.getDeployStatus() != null) {
            criteria.andDeployStatusEqualTo(condition.getDeployStatus().name());
        }
        if (condition.getDeployStatusList() != null && condition.getDeployStatusList().size() > 0) {
            criteria.andDeployStatusIn(condition.getDeployStatusList().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList()));
        }
        if (condition.getDeployProcessId() != null && condition.getDeployProcessId() > 0) {
            criteria.andDeployProcessIdEqualTo(String.valueOf(condition.getDeployProcessId()));
        }
        return example;
    }

    private DeployComponentDO insertDate(DeployComponentDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private DeployComponentDO updateDate(DeployComponentDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
