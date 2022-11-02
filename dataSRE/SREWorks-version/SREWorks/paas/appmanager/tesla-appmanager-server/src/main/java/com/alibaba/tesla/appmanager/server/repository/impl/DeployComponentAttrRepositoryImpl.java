package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.DeployComponentAttrRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployComponentAttrQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentAttrDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentAttrDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.DeployComponentAttrDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class DeployComponentAttrRepositoryImpl implements DeployComponentAttrRepository {

    @Autowired
    private DeployComponentAttrDOMapper deployComponentAttrDOMapper;

    @Override
    public int deleteByCondition(DeployComponentAttrQueryCondition condition) {
        return deployComponentAttrDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(DeployComponentAttrDO record) {
        return deployComponentAttrDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<DeployComponentAttrDO> selectByCondition(DeployComponentAttrQueryCondition condition) {
        return deployComponentAttrDOMapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(DeployComponentAttrDO record) {
        return deployComponentAttrDOMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private DeployComponentAttrDOExample buildExample(DeployComponentAttrQueryCondition condition) {
        DeployComponentAttrDOExample example = new DeployComponentAttrDOExample();
        DeployComponentAttrDOExample.Criteria criteria = example.createCriteria();
        if (condition.getDeployComponentId() != null && condition.getDeployComponentId() > 0) {
            criteria.andDeployComponentIdEqualTo(condition.getDeployComponentId());
        }
        if (!CollectionUtils.isEmpty(condition.getDeployComponentIdList())) {
            criteria.andDeployComponentIdIn(condition.getDeployComponentIdList());
        }
        if (StringUtils.isNotBlank(condition.getAttrType())) {
            criteria.andAttrTypeEqualTo(condition.getAttrType());
        }
        return example;
    }

    private DeployComponentAttrDO insertDate(DeployComponentAttrDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private DeployComponentAttrDO updateDate(DeployComponentAttrDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
