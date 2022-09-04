package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.AppPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTaskDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.AppPackageTaskDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AppPackageTaskRepositoryImpl implements AppPackageTaskRepository {

    @Autowired
    private AppPackageTaskDOMapper appPackageTaskMapper;

    @Override
    public long countByCondition(AppPackageTaskQueryCondition condition) {
        return appPackageTaskMapper.countByExample(buildExample(condition));
    }

    @Override
    public int insert(AppPackageTaskDO record) {
        return appPackageTaskMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<AppPackageTaskDO> selectByCondition(AppPackageTaskQueryCondition condition) {
        AppPackageTaskDOExample example = buildExample(condition);
        condition.doPagination();
        if (condition.isWithBlobs()) {
            return appPackageTaskMapper.selectByExampleWithBLOBs(example);
        } else {
            return appPackageTaskMapper.selectByExample(example);
        }
    }

    @Override
    public int updateByCondition(AppPackageTaskDO record, AppPackageTaskQueryCondition condition) {
        return appPackageTaskMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int deleteByCondition(AppPackageTaskQueryCondition condition) {
        return appPackageTaskMapper.deleteByExample(buildExample(condition));
    }

    private AppPackageTaskDOExample buildExample(AppPackageTaskQueryCondition condition) {
        AppPackageTaskDOExample example = new AppPackageTaskDOExample();
        AppPackageTaskDOExample.Criteria criteria = example.createCriteria();
        if (Objects.nonNull(condition.getId())) {
            criteria.andIdEqualTo(condition.getId());
        }

        if (CollectionUtils.isNotEmpty(condition.getIdList())) {
            criteria.andIdIn(condition.getIdList());
        }

        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }

        if (StringUtils.isNotBlank(condition.getOperator())) {
            criteria.andPackageCreatorEqualTo(condition.getOperator());
        }

        if (CollectionUtils.isNotEmpty(condition.getTaskStatusList())) {
            criteria.andTaskStatusIn(
                    condition.getTaskStatusList().stream().map(Enum::toString).collect(Collectors.toList()));
        }
        return example;
    }

    private AppPackageTaskDO insertDate(AppPackageTaskDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private AppPackageTaskDO updateDate(AppPackageTaskDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
