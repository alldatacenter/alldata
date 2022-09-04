package com.alibaba.tesla.appmanager.dynamicscript.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.dynamicscript.repository.DynamicScriptHistoryRepository;
import com.alibaba.tesla.appmanager.dynamicscript.repository.condition.DynamicScriptHistoryQueryCondition;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptHistoryDO;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptHistoryDOExample;
import com.alibaba.tesla.appmanager.dynamicscript.repository.mapper.DynamicScriptHistoryDOMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DynamicScriptHistoryRepositoryImpl implements DynamicScriptHistoryRepository {

    @Autowired
    private DynamicScriptHistoryDOMapper mapper;

    @Override
    public long countByCondition(DynamicScriptHistoryQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(DynamicScriptHistoryQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(DynamicScriptHistoryDO record) {
        return mapper.insert(insertDate(record));
    }

    @Override
    public List<DynamicScriptHistoryDO> selectByConditionWithBLOBs(DynamicScriptHistoryQueryCondition condition) {
        return mapper.selectByExampleWithBLOBs(buildExample(condition));
    }

    @Override
    public List<DynamicScriptHistoryDO> selectByCondition(DynamicScriptHistoryQueryCondition condition) {
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByConditionWithBLOBs(DynamicScriptHistoryDO record, DynamicScriptHistoryQueryCondition condition) {
        return mapper.updateByExampleWithBLOBs(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByCondition(DynamicScriptHistoryDO record, DynamicScriptHistoryQueryCondition condition) {
        return mapper.updateByExample(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKeyWithBLOBs(DynamicScriptHistoryDO record) {
        return mapper.updateByPrimaryKeyWithBLOBs(updateDate(record));
    }

    @Override
    public int updateByPrimaryKey(DynamicScriptHistoryDO record) {
        return mapper.updateByPrimaryKey(updateDate(record));
    }

    private DynamicScriptHistoryDOExample buildExample(DynamicScriptHistoryQueryCondition condition) {
        DynamicScriptHistoryDOExample example = new DynamicScriptHistoryDOExample();
        DynamicScriptHistoryDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getName())) {
            criteria.andNameEqualTo(condition.getName());
        }
        if (condition.getRevision() != null && condition.getRevision() > 0) {
            criteria.andRevisionEqualTo(condition.getRevision());
        }
        return example;
    }

    private DynamicScriptHistoryDO insertDate(DynamicScriptHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private DynamicScriptHistoryDO updateDate(DynamicScriptHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
