package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.DeployAppAttrRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppAttrQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppAttrDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppAttrDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.DeployAppAttrDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class DeployAppAttrRepositoryImpl implements DeployAppAttrRepository {

    @Autowired
    private DeployAppAttrDOMapper deployAppAttrDOMapper;

    @Override
    public int deleteByCondition(DeployAppAttrQueryCondition condition) {
        return deployAppAttrDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(DeployAppAttrDO record) {
        return deployAppAttrDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<DeployAppAttrDO> selectByCondition(DeployAppAttrQueryCondition condition) {
        return deployAppAttrDOMapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(DeployAppAttrDO record) {
        return deployAppAttrDOMapper.updateByPrimaryKey(updateDate(record));
    }

    private DeployAppAttrDOExample buildExample(DeployAppAttrQueryCondition condition) {
        DeployAppAttrDOExample example = new DeployAppAttrDOExample();
        DeployAppAttrDOExample.Criteria criteria = example.createCriteria();
        if (condition.getDeployAppId() != null && condition.getDeployAppId() > 0) {
            criteria.andDeployAppIdEqualTo(condition.getDeployAppId());
        }
        if (!CollectionUtils.isEmpty(condition.getDeployAppIdList())) {
            criteria.andDeployAppIdIn(condition.getDeployAppIdList());
        }
        if (StringUtils.isNotBlank(condition.getAttrType())) {
            criteria.andAttrTypeEqualTo(condition.getAttrType());
        }
        return example;
    }

    private DeployAppAttrDO insertDate(DeployAppAttrDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private DeployAppAttrDO updateDate(DeployAppAttrDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
