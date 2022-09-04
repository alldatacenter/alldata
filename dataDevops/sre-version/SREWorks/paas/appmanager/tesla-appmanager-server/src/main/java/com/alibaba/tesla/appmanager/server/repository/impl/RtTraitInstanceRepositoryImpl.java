package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.RtTraitInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.RtTraitInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtTraitInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtTraitInstanceDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.RtTraitInstanceDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class RtTraitInstanceRepositoryImpl implements RtTraitInstanceRepository {

    @Autowired
    private RtTraitInstanceDOMapper rtTraitInstanceDOMapper;

    @Override
    public long countByCondition(RtTraitInstanceQueryCondition condition) {
        return rtTraitInstanceDOMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(RtTraitInstanceQueryCondition condition) {
        return rtTraitInstanceDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(RtTraitInstanceDO record) {
        return rtTraitInstanceDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<RtTraitInstanceDO> selectByCondition(RtTraitInstanceQueryCondition condition) {
        condition.doPagination();
        return rtTraitInstanceDOMapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByCondition(RtTraitInstanceDO record, RtTraitInstanceQueryCondition condition) {
        return rtTraitInstanceDOMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private RtTraitInstanceDOExample buildExample(RtTraitInstanceQueryCondition condition) {
        RtTraitInstanceDOExample example = new RtTraitInstanceDOExample();
        RtTraitInstanceDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getTraitInstanceId())) {
            criteria.andTraitInstanceIdEqualTo(condition.getTraitInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getComponentInstanceId())) {
            criteria.andComponentInstanceIdEqualTo(condition.getComponentInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getAppInstanceId())) {
            criteria.andAppInstanceIdEqualTo(condition.getAppInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getTraitName())) {
            criteria.andTraitNameEqualTo(condition.getTraitName());
        }
        if (StringUtils.isNotBlank(condition.getStatus())) {
            criteria.andStatusEqualTo(condition.getStatus());
        }
        return example;
    }

    private RtTraitInstanceDO insertDate(RtTraitInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private RtTraitInstanceDO updateDate(RtTraitInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
