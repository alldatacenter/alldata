package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.RtComponentInstanceHistoryRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceHistoryDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.RtComponentInstanceHistoryDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class RtComponentInstanceHistoryRepositoryImpl implements RtComponentInstanceHistoryRepository {

    @Autowired
    private RtComponentInstanceHistoryDOMapper mapper;

    @Override
    public long countByCondition(RtComponentInstanceHistoryQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(RtComponentInstanceHistoryQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(RtComponentInstanceHistoryDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<RtComponentInstanceHistoryDO> selectByCondition(RtComponentInstanceHistoryQueryCondition condition) {
        condition.doPagination();
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public RtComponentInstanceHistoryDO getLatestByCondition(RtComponentInstanceHistoryQueryCondition condition) {
        condition.setPage(1);
        condition.setPageSize(1);
        List<RtComponentInstanceHistoryDO> records = selectByCondition(condition);
        if (records.size() == 0) {
            return null;
        } else {
            return records.get(0);
        }
    }

    @Override
    public int updateByCondition(RtComponentInstanceHistoryDO record, RtComponentInstanceHistoryQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int deleteExpiredRecords(String componentInstanceId, int instanceKeepDays) {
        return mapper.deleteExpiredRecords(componentInstanceId, instanceKeepDays);
    }

    private RtComponentInstanceHistoryDOExample buildExample(RtComponentInstanceHistoryQueryCondition condition) {
        RtComponentInstanceHistoryDOExample example = new RtComponentInstanceHistoryDOExample();
        RtComponentInstanceHistoryDOExample.Criteria criteria = example.createCriteria();
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

    private RtComponentInstanceHistoryDO insertDate(RtComponentInstanceHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private RtComponentInstanceHistoryDO updateDate(RtComponentInstanceHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
