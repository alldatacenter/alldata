package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ComponentPackageDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ComponentPackageRepositoryImpl implements ComponentPackageRepository {

    @Autowired
    private ComponentPackageDOMapper componentPackageMapper;

    @Override
    public long countByCondition(ComponentPackageQueryCondition condition) {
        return componentPackageMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ComponentPackageQueryCondition condition) {
        return componentPackageMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return componentPackageMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(ComponentPackageDO record) {
        return componentPackageMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ComponentPackageDO> selectByCondition(ComponentPackageQueryCondition condition) {
        ComponentPackageDOExample example = buildExample(condition);
        condition.doPagination();
        if (condition.isWithBlobs()) {
            return componentPackageMapper.selectByExampleWithBLOBs(example);
        } else {
            return componentPackageMapper.selectByExample(example);
        }
    }

    @Override
    public ComponentPackageDO getByCondition(ComponentPackageQueryCondition condition) {
        List<ComponentPackageDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple component package found with query condition %s",
                            JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(ComponentPackageDO record, ComponentPackageQueryCondition condition) {
        return componentPackageMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKeySelective(ComponentPackageDO record) {
        return componentPackageMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private ComponentPackageDOExample buildExample(ComponentPackageQueryCondition condition) {
        ComponentPackageDOExample example = new ComponentPackageDOExample();
        ComponentPackageDOExample.Criteria criteria = example.createCriteria();
        if (Objects.nonNull(condition.getId())) {
            criteria.andIdEqualTo(condition.getId());
        }
        if (CollectionUtils.isNotEmpty(condition.getIdList())) {
            criteria.andIdIn(condition.getIdList());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
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
        return example;
    }

    private ComponentPackageDO insertDate(ComponentPackageDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ComponentPackageDO updateDate(ComponentPackageDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
