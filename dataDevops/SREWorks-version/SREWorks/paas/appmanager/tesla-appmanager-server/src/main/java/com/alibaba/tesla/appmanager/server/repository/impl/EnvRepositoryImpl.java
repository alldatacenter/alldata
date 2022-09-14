package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.EnvRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.EnvQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.EnvDO;
import com.alibaba.tesla.appmanager.server.repository.domain.EnvDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.EnvMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class EnvRepositoryImpl implements EnvRepository {
    @Autowired
    private EnvMapper envMapper;

    @Override
    public long countByCondition(EnvQueryCondition condition) {
        return envMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(EnvQueryCondition condition) {
        return envMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(EnvDO record) {
        return envMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<EnvDO> selectByCondition(EnvQueryCondition condition) {
        EnvDOExample example = buildExample(condition);
        condition.doPagination();
        return envMapper.selectByExample(example);
    }

    @Override
    public int updateByCondition(EnvDO record, EnvQueryCondition condition) {
        return envMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private EnvDOExample buildExample(EnvQueryCondition condition) {
        EnvDOExample example = new EnvDOExample();
        EnvDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getNamespaceId())) {
            criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
        }
        if (StringUtils.isNotBlank(condition.getEnvId())) {
            criteria.andEnvIdEqualTo(condition.getEnvId());
        }
        if (StringUtils.isNotBlank(condition.getEnvName())) {
            criteria.andEnvNameEqualTo(condition.getEnvName());
        }
        if (StringUtils.isNotBlank(condition.getEnvCreator())) {
            criteria.andEnvCreatorEqualTo(condition.getEnvCreator());
        }
        if (StringUtils.isNotBlank(condition.getEnvModifier())) {
            criteria.andEnvModifierEqualTo(condition.getEnvModifier());
        }

        if (BooleanUtils.isTrue(condition.getProduction())) {
            criteria.andEnvIdIn(PRODUCTION);
        }

        if (BooleanUtils.isFalse(condition.getProduction())) {
            criteria.andEnvIdNotIn(PRODUCTION);
        }

        return example;
    }

    private EnvDO insertDate(EnvDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private EnvDO updateDate(EnvDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
