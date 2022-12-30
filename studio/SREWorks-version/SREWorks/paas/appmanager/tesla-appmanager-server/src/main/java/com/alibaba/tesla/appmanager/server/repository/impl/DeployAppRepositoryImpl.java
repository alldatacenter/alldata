package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.DeployAppRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.DeployAppDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class DeployAppRepositoryImpl implements DeployAppRepository {

    @Autowired
    private DeployAppDOMapper deployAppMapper;

    @Override
    public long countByCondition(DeployAppQueryCondition condition) {
        return deployAppMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(DeployAppQueryCondition condition) {
        return deployAppMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return deployAppMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(DeployAppDO record) {
        return deployAppMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<DeployAppDO> selectByCondition(DeployAppQueryCondition condition) {
        condition.doPagination();
        String optionKey = condition.getOptionKey();
        String optionValue = condition.getOptionValue();
        if (StringUtils.isNotEmpty(optionKey) && StringUtils.isNotEmpty(optionValue)) {
            return deployAppMapper.selectByExampleAndOption(buildExample(condition), optionKey, optionValue);
        } else {
            return deployAppMapper.selectByExample(buildExample(condition));
        }
    }

    @Override
    public DeployAppDO selectByPrimaryKey(Long id) {
        return deployAppMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByConditionSelective(DeployAppDO record, DeployAppQueryCondition condition) {
        return deployAppMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(DeployAppDO record) {
        return deployAppMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private DeployAppDOExample buildExample(DeployAppQueryCondition condition) {
        DeployAppDOExample example = new DeployAppDOExample();
        DeployAppDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (StringUtils.isNotBlank(condition.getClusterId())) {
            criteria.andClusterIdEqualTo(condition.getClusterId());
        }
        if (condition.getNamespaceId() != null) {
            criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
        }
        if (condition.getStageId() != null) {
            criteria.andStageIdEqualTo(condition.getStageId());
        }
        if (condition.getDeployProcessId() != null && condition.getDeployProcessId() > 0) {
            criteria.andDeployProcessIdEqualTo(condition.getDeployProcessId());
        }
        if (condition.getAppPackageId() != null && condition.getAppPackageId() > 0) {
            criteria.andAppPackageIdEqualTo(condition.getAppPackageId());
        }
        if (condition.getDeployStatus() != null) {
            criteria.andDeployStatusEqualTo(condition.getDeployStatus().name());
        }
        if (StringUtils.isNotBlank(condition.getPackageVersion())) {
            criteria.andPackageVersionEqualTo(condition.getPackageVersion());
        }
        if (CollectionUtils.isNotEmpty(condition.getStageIdWhiteList())) {
            criteria.andStageIdIn(condition.getStageIdWhiteList());
        }
        if (CollectionUtils.isNotEmpty(condition.getStageIdBlackList())) {
            criteria.andStageIdNotIn(condition.getStageIdBlackList());
        }
        return example;
    }

    private DeployAppDO insertDate(DeployAppDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private DeployAppDO updateDate(DeployAppDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
