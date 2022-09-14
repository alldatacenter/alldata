package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.RtTraitInstanceHistoryRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.RtTraitInstanceHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtTraitInstanceHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtTraitInstanceHistoryDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.RtTraitInstanceHistoryDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class RtTraitInstanceHistoryRepositoryImpl implements RtTraitInstanceHistoryRepository {

    @Autowired
    private RtTraitInstanceHistoryDOMapper rtTraitInstanceHistoryDOMapper;

    @Override
    public long countByCondition(RtTraitInstanceHistoryQueryCondition condition) {
        return rtTraitInstanceHistoryDOMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(RtTraitInstanceHistoryQueryCondition condition) {
        return rtTraitInstanceHistoryDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(RtTraitInstanceHistoryDO record) {
        return rtTraitInstanceHistoryDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<RtTraitInstanceHistoryDO> selectByCondition(RtTraitInstanceHistoryQueryCondition condition) {
        condition.doPagination();
        return rtTraitInstanceHistoryDOMapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByCondition(RtTraitInstanceHistoryDO record, RtTraitInstanceHistoryQueryCondition condition) {
        return rtTraitInstanceHistoryDOMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private RtTraitInstanceHistoryDOExample buildExample(RtTraitInstanceHistoryQueryCondition condition) {
        RtTraitInstanceHistoryDOExample example = new RtTraitInstanceHistoryDOExample();
        RtTraitInstanceHistoryDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getTraitInstanceId())) {
            criteria.andTraitInstanceIdEqualTo(condition.getTraitInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getComponentInstanceId())) {
            criteria.andComponentInstanceIdEqualTo(condition.getComponentInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getAppInstanceId())) {
            criteria.andAppInstanceIdEqualTo(condition.getAppInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getStatus())) {
            criteria.andStatusEqualTo(condition.getStatus());
        }
        return example;
    }

    private RtTraitInstanceHistoryDO insertDate(RtTraitInstanceHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private RtTraitInstanceHistoryDO updateDate(RtTraitInstanceHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
