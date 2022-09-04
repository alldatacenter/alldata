package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.AppOptionRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppOptionQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppOptionDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppOptionDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.AppOptionDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AppOptionRepositoryImpl implements AppOptionRepository {

    @Autowired
    private AppOptionDOMapper appOptionMapper;

    @Override
    public long countByCondition(AppOptionQueryCondition condition) {
        return appOptionMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(AppOptionQueryCondition condition) {
        return appOptionMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(AppOptionDO record) {
        return appOptionMapper.insertSelective(insertDate(record));
    }

    @Override
    public int batchInsert(List<AppOptionDO> records) {
        return appOptionMapper.batchInsert(records.stream().map(this::insertDate).collect(Collectors.toList()));
    }

    @Override
    public List<AppOptionDO> selectByCondition(AppOptionQueryCondition condition) {
        condition.doPagination();
        return appOptionMapper.selectByExample(buildExample(condition));
    }

    @Override
    public AppOptionDO getByCondition(AppOptionQueryCondition condition) {
        List<AppOptionDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple app option found with query condition %s",
                            JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(AppOptionDO record, AppOptionQueryCondition condition) {
        return appOptionMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private AppOptionDOExample buildExample(AppOptionQueryCondition condition) {
        AppOptionDOExample example = new AppOptionDOExample();
        AppOptionDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (StringUtils.isNotBlank(condition.getKey())) {
            criteria.andKeyEqualTo(condition.getKey());
        }
        if (StringUtils.isNotBlank(condition.getValue())) {
            criteria.andValueEqualTo(condition.getValue());
        }
        return example;
    }

    private AppOptionDO insertDate(AppOptionDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private AppOptionDO updateDate(AppOptionDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
