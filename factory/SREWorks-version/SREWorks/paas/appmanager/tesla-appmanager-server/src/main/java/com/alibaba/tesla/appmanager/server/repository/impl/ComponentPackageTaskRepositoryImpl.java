package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ComponentPackageTaskDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ComponentPackageTaskRepositoryImpl implements ComponentPackageTaskRepository {

    @Autowired
    private ComponentPackageTaskDOMapper componentPackageTaskDOMapper;

    @Override
    public long countByCondition(ComponentPackageTaskQueryCondition condition) {
        return componentPackageTaskDOMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ComponentPackageTaskQueryCondition condition) {
        return componentPackageTaskDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ComponentPackageTaskDO record) {
        return componentPackageTaskDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ComponentPackageTaskDO> selectByCondition(ComponentPackageTaskQueryCondition condition) {
        ComponentPackageTaskDOExample example = buildExample(condition);
        condition.doPagination();
        if (condition.isWithBlobs()) {
            return componentPackageTaskDOMapper.selectByExampleWithBLOBs(example);
        } else {
            return componentPackageTaskDOMapper.selectByExample(example);
        }
    }

    @Override
    public ComponentPackageTaskDO getByCondition(ComponentPackageTaskQueryCondition condition) {
        condition.setWithBlobs(true);
        List<ComponentPackageTaskDO> records = selectByCondition(condition);
        if (records.size() == 0) {
            return null;
        } else if (records.size() > 1) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    "multiple component package task record found|%s", JSONObject.toJSONString(condition));
        } else {
            return records.get(0);
        }
    }

    @Override
    public int updateByCondition(ComponentPackageTaskDO record, ComponentPackageTaskQueryCondition condition) {
        return componentPackageTaskDOMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private ComponentPackageTaskDOExample buildExample(ComponentPackageTaskQueryCondition condition) {
        ComponentPackageTaskDOExample example = new ComponentPackageTaskDOExample();
        ComponentPackageTaskDOExample.Criteria criteria = example.createCriteria();
        if (Objects.nonNull(condition.getId()) && condition.getId() > 0) {
            criteria.andIdEqualTo(condition.getId());
        }
        if (Objects.nonNull(condition.getIdList()) && condition.getIdList().size() > 0) {
            criteria.andIdIn(condition.getIdList());
        }
        if (Objects.nonNull(condition.getAppPackageTaskId()) && condition.getAppPackageTaskId() > 0) {
            criteria.andAppPackageTaskIdEqualTo(condition.getAppPackageTaskId());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (condition.getNamespaceId() != null) {
            criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
        }
        if (condition.getStageId() != null) {
            criteria.andStageIdEqualTo(condition.getStageId());
        }
        if (StringUtils.isNotBlank(condition.getComponentType())) {
            criteria.andComponentTypeEqualTo(condition.getComponentType());
        }
        if (StringUtils.isNotBlank(condition.getComponentName())) {
            criteria.andComponentNameEqualTo(condition.getComponentName());
        }
        if (StringUtils.isNotBlank(condition.getPackageCreator())) {
            criteria.andPackageCreatorEqualTo(condition.getPackageCreator());
        }
        if (StringUtils.isNotBlank(condition.getPackageVersion())) {
            criteria.andPackageVersionEqualTo(condition.getPackageVersion());
        }
        if (StringUtils.isNotBlank(condition.getTaskStatus())) {
            criteria.andTaskStatusEqualTo(condition.getTaskStatus());
        }
        if (StringUtils.isNotEmpty(condition.getNamespaceIdNotEqualTo())) {
            criteria.andNamespaceIdNotEqualTo(condition.getNamespaceIdNotEqualTo());
        }
        if (StringUtils.isNotEmpty(condition.getStageIdNotEqualTo())) {
            criteria.andStageIdNotEqualTo(condition.getStageIdNotEqualTo());
        }
        return example;
    }

    private ComponentPackageTaskDO insertDate(ComponentPackageTaskDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ComponentPackageTaskDO updateDate(ComponentPackageTaskDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
