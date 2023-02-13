package com.alibaba.tesla.appmanager.dynamicscript.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.dynamicscript.repository.DynamicScriptRepository;
import com.alibaba.tesla.appmanager.dynamicscript.repository.condition.DynamicScriptQueryCondition;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptDO;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptDOExample;
import com.alibaba.tesla.appmanager.dynamicscript.repository.mapper.DynamicScriptDOMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DynamicScriptRepositoryImpl implements DynamicScriptRepository {

    @Autowired
    private DynamicScriptDOMapper mapper;

    @Override
    public long countByCondition(DynamicScriptQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(DynamicScriptQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(DynamicScriptDO record) {
        return mapper.insert(insertDate(record));
    }

    @Override
    public List<DynamicScriptDO> selectByCondition(DynamicScriptQueryCondition condition) {
        if (condition.isWithBlobs()) {
            return mapper.selectByExampleWithBLOBs(buildExample(condition));
        } else {
            return mapper.selectByExample(buildExample(condition));
        }
    }

    @Override
    public DynamicScriptDO getByCondition(DynamicScriptQueryCondition condition) {
        List<DynamicScriptDO> records = selectByCondition(condition);
        if (records.size() == 0) {
            return null;
        } else if (records.size() > 1) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                "multiple dynamic script record found|%s", JSONObject.toJSONString(condition));
        } else {
            return records.get(0);
        }
    }

    @Override
    public int updateByCondition(DynamicScriptDO record, DynamicScriptQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(DynamicScriptDO record) {
        return mapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private DynamicScriptDOExample buildExample(DynamicScriptQueryCondition condition) {
        DynamicScriptDOExample example = new DynamicScriptDOExample();
        DynamicScriptDOExample.Criteria criteria = example.createCriteria();
        if (condition.getKind() != null) {
            criteria.andKindEqualTo(condition.getKind().toString());
        }
        if (StringUtils.isNotBlank(condition.getName())) {
            criteria.andNameEqualTo(condition.getName());
        }
        return example;
    }

    private DynamicScriptDO insertDate(DynamicScriptDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private DynamicScriptDO updateDate(DynamicScriptDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }    
}
