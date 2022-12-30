package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ClusterRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ClusterQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ClusterDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ClusterDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ClusterDOMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ClusterRepositoryImpl implements ClusterRepository {

    @Autowired
    private ClusterDOMapper clusterDOMapper;

    @Override
    public long countByCondition(ClusterQueryCondition condition) {
        return clusterDOMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ClusterQueryCondition condition) {
        return clusterDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ClusterDO record) {
        return clusterDOMapper.insert(insertDate(record));
    }

    @Override
    public List<ClusterDO> selectByCondition(ClusterQueryCondition condition) {
        condition.doPagination();
        return clusterDOMapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByCondition(ClusterDO record, ClusterQueryCondition condition) {
        return clusterDOMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private ClusterDOExample buildExample(ClusterQueryCondition condition) {
        ClusterDOExample example = new ClusterDOExample();
        ClusterDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getClusterId())) {
            criteria.andClusterIdEqualTo(condition.getClusterId());
        }
        if (StringUtils.isNotEmpty(condition.getClusterName())) {
            criteria.andClusterNameEqualTo(condition.getClusterName());
        }
        if (StringUtils.isNotEmpty(condition.getClusterType())) {
            criteria.andClusterTypeEqualTo(condition.getClusterType());
        }
        return example;
    }

    private ClusterDO insertDate(ClusterDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ClusterDO updateDate(ClusterDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
