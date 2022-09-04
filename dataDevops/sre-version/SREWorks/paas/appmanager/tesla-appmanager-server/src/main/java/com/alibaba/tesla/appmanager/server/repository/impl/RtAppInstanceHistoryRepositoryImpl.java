package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.RtAppInstanceHistoryRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceHistoryDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.RtAppInstanceHistoryDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class RtAppInstanceHistoryRepositoryImpl implements RtAppInstanceHistoryRepository {

    @Autowired
    private RtAppInstanceHistoryDOMapper mapper;

    @Override
    public long countByCondition(RtAppInstanceHistoryQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(RtAppInstanceHistoryQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(RtAppInstanceHistoryDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<RtAppInstanceHistoryDO> selectByCondition(RtAppInstanceHistoryQueryCondition condition) {
        condition.doPagination();
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public RtAppInstanceHistoryDO getLatestByCondition(RtAppInstanceHistoryQueryCondition condition) {
        condition.setPage(1);
        condition.setPageSize(1);
        List<RtAppInstanceHistoryDO> records = selectByCondition(condition);
        if (records.size() == 0) {
            return null;
        } else {
            return records.get(0);
        }
    }

    @Override
    public int updateByCondition(RtAppInstanceHistoryDO record, RtAppInstanceHistoryQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int deleteExpiredRecords(String appInstanceId, int instanceKeepDays) {
        return mapper.deleteExpiredRecords(appInstanceId, instanceKeepDays);
    }

    private RtAppInstanceHistoryDOExample buildExample(RtAppInstanceHistoryQueryCondition condition) {
        RtAppInstanceHistoryDOExample example = new RtAppInstanceHistoryDOExample();
        RtAppInstanceHistoryDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getAppInstanceId())) {
            criteria.andAppInstanceIdEqualTo(condition.getAppInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getStatus())) {
            criteria.andStatusEqualTo(condition.getStatus());
        }
        return example;
    }

    private RtAppInstanceHistoryDO insertDate(RtAppInstanceHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private RtAppInstanceHistoryDO updateDate(RtAppInstanceHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
